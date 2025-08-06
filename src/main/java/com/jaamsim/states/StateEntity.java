/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2014 Ausenco Engineering Canada Inc.
 * Copyright (C) 2018-2024 JaamSim Software Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jaamsim.states;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map.Entry;

import com.jaamsim.BooleanProviders.BooleanProvInput;
import com.jaamsim.Graphics.DisplayEntity;
import com.jaamsim.basicsim.Entity;
import com.jaamsim.basicsim.FileEntity;
import com.jaamsim.basicsim.JaamSimModel;
import com.jaamsim.basicsim.SubjectEntity;
import com.jaamsim.events.EventManager;
import com.jaamsim.input.ColourInput;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.Output;
import com.jaamsim.input.StringKeyInput;
import com.jaamsim.input.StringListInput;
import com.jaamsim.math.Color4d;
import com.jaamsim.units.DimensionlessUnit;
import com.jaamsim.units.TimeUnit;

public abstract class StateEntity extends DisplayEntity implements StateUser {

	@Keyword(description = "A list of state/DisplayEntity pairs. For each state, the graphics "
	                     + "will be changed to those for the corresponding DisplayEntity.",
	         exampleList = {"{ idle DisplayEntity1 } { working DisplayEntity2 }"})
	protected final StringKeyInput<DisplayEntity> stateGraphics;

	@Keyword(description = "If TRUE, a log file (.trc) will be printed with the time of every state change during the run.")
	private final BooleanProvInput traceState;

	@Keyword(description = "A list of states for which the entity is considered working.",
		     exampleList = "'Transit - Seg1L' 'Transit - Seg1B'")
	protected final StringListInput workingStateListInput;

	private StateRecord presentState; // The present state of the entity
	private final HashMap<String, StateRecord> states;
	private final ArrayList<StateEntityListener> stateListeners;

	private long lastStateCollectionTick;
	private long workingTicks;
	private boolean useCurrentCycle;

	protected FileEntity stateReportFile;        // The file to store the state information

	protected static final String STATE_IDLE = "Idle";
	protected static final String STATE_WORKING = "Working";
	protected static final String STATE_INACTIVE = "Inactive";

	protected static final Color4d COL_IDLE = ColourInput.LIGHT_GREY;
	protected static final Color4d COL_WORKING = ColourInput.GREEN;
	protected static final Color4d COL_INACTIVE = ColourInput.WHITE;

	{
		stateGraphics = new StringKeyInput<>(DisplayEntity.class, "StateGraphics", FORMAT);
		stateGraphics.setHidden(true);
		this.addInput(stateGraphics);

		traceState = new BooleanProvInput("TraceState", KEY_INPUTS, false);
		traceState.setHidden(true);
		this.addInput(traceState);

		workingStateListInput = new StringListInput("WorkingStateList", MAINTENANCE, new ArrayList<String>(0));
		this.addInput(workingStateListInput);
	}

	public StateEntity() {
		states = new HashMap<>();
		stateListeners = new ArrayList<>();
	}

	@Override
	public void earlyInit() {
		super.earlyInit();

		this.initStateData();

		if (this.isGenerated())
			return;

		// Create state trace file if required
		if (isTraceState()) {
			JaamSimModel simModel = getJaamSimModel();
			String fileName = simModel.getReportFileName("-" + this.getName() + ".trc");
			if (fileName == null)
				error("Cannot create the trace file");
			File f = new File(fileName);
			if (f.exists() && !f.delete())
				error("Cannot delete the existing trace file %s", f);
			stateReportFile = new FileEntity(simModel, f);
		}
	}

	@Override
	public void lateInit() {
		super.lateInit();

		stateListeners.clear();

		if (!isRegistered())
			return;

		for (Entity ent : getJaamSimModel().getClonesOfIterator(Entity.class, StateEntityListener.class)) {
			StateEntityListener sel = (StateEntityListener)ent;
			if (sel.isWatching(this))
				addStateListener(sel);
		}
	}

	@Override
	public void doEnd() {
		super.doEnd();
		if (stateReportFile == null)
			return;
		stateReportFile.flush();

		// Close the state trace file
		if (getJaamSimModel().isLastRun()) {
			stateReportFile.close();
			stateReportFile = null;
		}
	}

	@Override
	public void close() {
		super.close();
		if (stateReportFile == null)
			return;
		stateReportFile.flush();
		stateReportFile.close();
		stateReportFile = null;
	}

	public boolean isTraceState() {
		return traceState.getNextBoolean(this, 0.0d);
	}

	private void initStateData() {
		lastStateCollectionTick = 0;
		if (EventManager.hasCurrent())
			lastStateCollectionTick = getSimTicks();
		workingTicks = 0;
		states.clear();
		useCurrentCycle = false;

		StateRecord init = this.createRecord(getInitialState());
		init.setStartTick(lastStateCollectionTick);
		presentState = init;
		states.put(init.getName(), init);
	}

	public void addStateListener(StateEntityListener listener) {
		stateListeners.add(listener);
	}

	public ArrayList<StateEntityListener> getStateListeners() {
		return stateListeners;
	}

	/**
	 * Get the name of the initial state this Entity will be initialized with.
	 */
	public String getInitialState() {
		return isActive() ? STATE_IDLE : STATE_INACTIVE;
	}

	/**
	 * Tests the given state name to see if it is valid for this Entity.
	 * @param state
	 */
	public boolean isValidState(String state) {
		return STATE_IDLE.equals(state) || STATE_WORKING.equals(state)
				|| STATE_INACTIVE.equals(state);
	}

	/**
	 * Tests the given state name to see if it is counted as working hours when in
	 * that state..
	 * @param state
	 */
	public boolean isValidWorkingState(String state) {

		if( workingStateListInput.getValue().size() > 0 )
			return workingStateListInput.getValue().contains( state );

		return STATE_WORKING.equals(state);
	}

	@Override
	public final void setPresentState( String state ) {
		if (presentState == null)
			this.initStateData();

		if (presentState.getName().equals(state))
			return;

		StateRecord nextState = states.get(state);
		if (nextState == null) {
			if (!isValidState(state))
				error("Specified state: %s is not valid", state);

			nextState = this.createRecord(state);
			states.put(nextState.getName(), nextState);
		}

		updateStateStats();
		nextState.setStartTick(lastStateCollectionTick);

		StateRecord prev = presentState;
		presentState = nextState;
		stateChanged(prev, presentState);
	}

	/**
	 * A callback subclasses can override that is called on each state transition.
	 *
	 * The state has already been updated when this is called so presentState == next
	 * @param prev the state this Entity was in previously
	 * @param next the state this Entity is currently in
	 */
	public void stateChanged(StateRecord prev, StateRecord next) {

		if (isTraceState()) {
			long curTick = EventManager.simTicks();
			EventManager evt = EventManager.current();
			double duration = evt.ticksToSeconds(curTick - prev.getStartTick());
			double timeOfPrevStart = evt.ticksToSeconds(prev.getStartTick());
			stateReportFile.format("%.5f  %s.setState( \"%s\" ) dt = %g\n",
			                       timeOfPrevStart, this.getName(),
			                       prev.getName(), duration);
			stateReportFile.flush();
		}

		// Notify the state listeners
		for (StateEntityListener each : stateListeners) {
			each.updateForStateChange(this, prev, next);
		}

		// Notify the observers
		if (this instanceof SubjectEntity)
			((SubjectEntity) this).notifyObservers();
	}

	/**
	 * Update the statistics kept for ticks in the presentState
	 */
	private void updateStateStats() {
		long curTick = getSimTicks();
		if (curTick == lastStateCollectionTick)
			return;

		long durTicks = curTick - lastStateCollectionTick;
		lastStateCollectionTick = curTick;

		presentState.addTicks(durTicks);
		if (presentState.isWorking())
			workingTicks += durTicks;
	}

	/**
	 * Runs after initialization period
	 */
	public void collectInitializationStats() {
		updateStateStats();
		for (StateRecord each : states.values()) {
			each.finishWarmUp();
		}
	}

	/**
	 * Runs after each report interval
	 */
	public void clearReportStats() {
		updateStateStats();
		for (StateRecord each : states.values()) {
			each.clearStats();
		}
	}

	/**
	 * Clear the current cycle hours, also reset the start of cycle time
	 */
	public void clearCurrentCycleStats() {
		updateStateStats();
		for (StateRecord each : states.values()) {
			each.clearCurrentCycleStats();
		}
	}

	/**
	 * Runs when cycle is finished
	 */
	public void collectCycleStats() {
		updateStateStats();
		useCurrentCycle = true;
		for (StateRecord each : states.values()) {
			each.finishCycle();
		}
	}

	private StateRecord createRecord(String state) {
		return new StateRecord(state, isValidWorkingState(state));
	}

	public void addState(String str) {
		if (states.get(str) != null)
			return;
		if (!isValidState(str))
			error("Specified state: %s is not valid", str);

		StateRecord stateRec = this.createRecord(str);
		states.put(stateRec.getName(), stateRec);
	}

	public StateRecord getState(String state) {
		return states.get(state);
	}

	public StateRecord getState() {
		return presentState;
	}

	private static class StateRecSort implements Comparator<StateRecord> {
		@Override
		public int compare(StateRecord sr1, StateRecord sr2) {
			return sr1.getName().compareTo(sr2.getName());
		}
	}

	public ArrayList<StateRecord> getStateRecs() {
		ArrayList<StateRecord> recs = new ArrayList<>(states.size());
		for (StateRecord rec : states.values())
			recs.add(rec);
		Collections.sort(recs, new StateRecSort());
		return recs;
	}

	@Override
	public boolean isWorkingState() {
		return presentState.isWorking();
	}

	/**
	 * A helper used to implement some of the state-based outputs, likely not
	 * useful for model code.
	 * @param simTicks
	 * @param state
	 */
	public long getTicksInState(long simTicks, StateRecord state) {
		if (state == null)
			return 0;

		long ticks = state.getTotalTicks();
		if (getState() == state)
			ticks += (simTicks - lastStateCollectionTick);
		return ticks;
	}

	public long getTicksInState(StateRecord state) {
		return getTicksInState(getSimTicks(), state);
	}

	public long getCurrentCycleTicks(long simTicks, StateRecord state) {
		if (state == null)
			return 0;

		long ticks = state.getCurrentCycleTicks();
		if (getState() == state)
			ticks += (simTicks - lastStateCollectionTick);
		return ticks;
	}

	public long getCurrentCycleTicks(StateRecord state) {
		return getCurrentCycleTicks(getSimTicks(), state);
	}

	public long getCompletedCycleTicks(StateRecord state) {
		if (state == null)
			return 0;

		return state.getCompletedCycleTicks();
	}

	public long getInitTicks(StateRecord state) {
		if (state == null)
			return 0;

		return state.getInitTicks();
	}

	private long getWorkingTicks(long simTicks) {
		long ticks = workingTicks;
		if (presentState.isWorking())
			ticks += (simTicks - lastStateCollectionTick);

		return ticks;
	}

	/**
	 * Returns the number of seconds that the entity has been in use.
	 */
	public double getWorkingTime() {
		long ticks = getWorkingTicks(getSimTicks());
		return EventManager.current().ticksToSeconds(ticks);
	}

	/**
	 * Returns the elapsed time in seconds after the completion of the initialisation period
	 * that the entity has been in the specified state.
	 * @param simTime - present simulation time
	 * @param state - string representing the state
	 */
	public double getTimeInState(double simTime, String state) {
		EventManager evt = this.getJaamSimModel().getEventManager();
		long simTicks = evt.secondsToNearestTick(simTime);
		StateRecord rec = states.get(state);
		if (rec == null)
			return 0.0;
		long ticks = getTicksInState(simTicks, rec);
		return evt.ticksToSeconds(ticks);
	}

	/**
	 * Returns the total elapsed time in seconds after the completion of the initialisation period
	 * during which the entity has been in a state that has been labelled as 'working'.
	 * @param simTime - present simulation time
	 * @return total time in a 'working' state
	 */
	public double getTimeInWorkingState(double simTime) {
		EventManager evt = this.getJaamSimModel().getEventManager();
		long simTicks = evt.secondsToNearestTick(simTime);
		long ticks = 0L;
		for (StateRecord rec : states.values()) {
			if (!rec.isWorking())
				continue;
			ticks += getTicksInState(simTicks, rec);
		}
		return evt.ticksToSeconds(ticks);
	}

	/**
	 * Returns the total elapsed time in seconds after the completion of the initialisation period
	 * the entity has been in any state that ends in the specified string.
	 * @param simTime - present simulation time
	 * @param state - string representing the specified type of state
	 * @return total time
	 */
	public double getTotalTimeInState(double simTime, String state) {
		EventManager evt = this.getJaamSimModel().getEventManager();
		long simTicks = evt.secondsToNearestTick(simTime);
		long ticks = 0L;
		Iterator<Entry<String, StateRecord>> itr = states.entrySet().iterator();
		while (itr.hasNext()) {
			Entry<String, StateRecord> pair = itr.next();
			if (pair.getKey().endsWith(state)) {
				ticks += getTicksInState(simTicks, pair.getValue());
			}
		}
		return evt.ticksToSeconds(ticks);
	}

	/**
	 * Returns the total elapsed time in seconds after the completion of the initialisation period.
	 * Includes the time in any completed cycles.
	 * @param simTime - present simulation time
	 * @return total time in any state
	 */
	public double getTotalTime(double simTime) {
		EventManager evt = this.getJaamSimModel().getEventManager();
		long simTicks = evt.secondsToNearestTick(simTime);
		long ticks = 0L;
		for (StateRecord rec : states.values()) {
			ticks += getTicksInState(simTicks, rec);
		}
		return evt.ticksToSeconds(ticks);
	}

	@Override
	public void updateGraphics(double simTime) {
		super.updateGraphics(simTime);

		if (stateGraphics.getValue() == null || presentState == null)
			return;

		DisplayEntity ent = stateGraphics.getValueFor(presentState.getName());
		if (ent == null) {
			setDisplayModelList(displayModelListInput.getValue());
			setSize(sizeInput.getValue());
			setOrientation(orientationInput.getValue());
			setAlignment(alignmentInput.getValue());
			return;
		}

		this.setDisplayModelList(ent.getDisplayModelList());
		this.setSize(ent.getSize());
		this.setOrientation(ent.getOrientation());
		this.setAlignment(ent.getAlignment());
	}

	@Output(name = "State",
	 description = "The present state for the object.",
	    unitType = DimensionlessUnit.class,
	    sequence = 0)
	public String getPresentState(double simTime) {
		if (presentState == null) {
			return this.getInitialState();
		}
		return presentState.getName();
	}

	@Output(name = "WorkingState",
	 description = "Returns TRUE if the present state is one of the 'working' states.",
	    sequence = 1)
	public boolean isWorkingState(double simTime) {
		if (presentState == null) {
			return this.isValidWorkingState(this.getInitialState());
		}
		return presentState.isWorking();
	}

	@Output(name = "WorkingTime",
	 description = "The total time recorded for the working states, including the "
	             + "initialisation period. Breakdown events can be triggered by elapsed "
	             + "working time instead of calendar time.",
	    unitType = TimeUnit.class,
	    sequence = 2)
	public double getWorkingTime(double simTime) {
		if (presentState == null) {
			return 0.0;
		}
		EventManager evt = this.getJaamSimModel().getEventManager();
		long simTicks = evt.secondsToNearestTick(simTime);
		long ticks = getWorkingTicks(simTicks);
		return evt.ticksToSeconds(ticks);
	}

	@Output(name = "StateTimes",
	 description = "The total time recorded for each state after the completion of "
	             + "the initialisation period. Includes only the present cycle, if applicable.",
	    unitType = TimeUnit.class,
	  reportable = true,
	    sequence = 3)
	public LinkedHashMap<String, Double> getStateTimes(double simTime) {
		EventManager evt = this.getJaamSimModel().getEventManager();
		long simTicks = evt.secondsToNearestTick(simTime);
		LinkedHashMap<String, Double> ret = new LinkedHashMap<>(states.size());
		for (StateRecord stateRec : this.getStateRecs()) {
			long ticks = getTicksInState(simTicks, stateRec);
			if (useCurrentCycle)
				ticks = getCurrentCycleTicks(simTicks, stateRec);
			Double t = evt.ticksToSeconds(ticks);
			ret.put(stateRec.getName(), t);
		}
		return ret;
	}

	@Output(name = "TotalTime",
	 description = "The total time the entity has spent in the model after the completion of "
	             + "the initialisation period. It is equal to the sum of the state times. "
	             + "Includes only the present cycle, if applicable.",
	    unitType = TimeUnit.class,
	  reportable = true,
	    sequence = 4)
	public double getTotalTimeInCycle(double simTime) {
		EventManager evt = this.getJaamSimModel().getEventManager();
		long simTicks = evt.secondsToNearestTick(simTime);
		long ticks = 0L;
		for (StateRecord stateRec : states.values()) {
			if (useCurrentCycle) {
				ticks += getCurrentCycleTicks(simTicks, stateRec);
			}
			else {
				ticks += getTicksInState(simTicks, stateRec);
			}
		}
		return evt.ticksToSeconds(ticks);
	}

}
