/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2014 Ausenco Engineering Canada Inc.
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;

import com.jaamsim.Graphics.DisplayEntity;
import com.jaamsim.basicsim.Entity;
import com.jaamsim.basicsim.FileEntity;
import com.jaamsim.events.EventManager;
import com.jaamsim.input.BooleanInput;
import com.jaamsim.input.InputAgent;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.Output;
import com.jaamsim.input.StringKeyInput;
import com.jaamsim.input.StringListInput;
import com.jaamsim.units.DimensionlessUnit;
import com.jaamsim.units.TimeUnit;

public class StateEntity extends DisplayEntity {

	@Keyword(description = "A list of state/DisplayEntity pairs. For each state, the graphics "
	                     + "will be changed to those for the corresponding DisplayEntity.",
	         exampleList = {"{ idle DisplayEntity1 } { working DisplayEntity2 }"})
	protected final StringKeyInput<DisplayEntity> stateGraphics;

	@Keyword(description = "If TRUE, a log file (.trc) will be printed with the time of every state change during the run.",
	         exampleList = {"TRUE"})
	private final BooleanInput traceState;

	@Keyword(description = "A list of states for which the entity is considered working.",
		     exampleList = "'Transit - Seg1L' 'Transit - Seg1B'")
	protected final StringListInput workingStateListInput;

	private StateRecord presentState; // The present state of the entity
	private final HashMap<String, StateRecord> states;
	private final ArrayList<StateEntityListener> stateListeners;

	private long lastStateCollectionTick;
	private long workingTicks;

	protected FileEntity stateReportFile;        // The file to store the state information

	{
		stateGraphics = new StringKeyInput<>(DisplayEntity.class, "StateGraphics", KEY_INPUTS);
		stateGraphics.setHidden(true);
		this.addInput(stateGraphics);

		traceState = new BooleanInput("TraceState", KEY_INPUTS, false);
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

		if (testFlag(FLAG_GENERATED))
			return;

		// Create state trace file if required
		if (traceState.getValue()) {
			String fileName = InputAgent.getReportFileName(InputAgent.getRunName() + "-" + this.getName() + ".trc");
			stateReportFile = new FileEntity( fileName);
		}
	}

	@Override
	public void lateInit() {
		super.lateInit();

		stateListeners.clear();
		for (Entity ent : Entity.getClonesOfIterator(Entity.class, StateEntityListener.class)) {
			StateEntityListener sel = (StateEntityListener)ent;
			if (sel.isWatching(this))
				stateListeners.add(sel);
		}
	}

	private void initStateData() {
		lastStateCollectionTick = 0;
		if (EventManager.hasCurrent())
			lastStateCollectionTick = getSimTicks();
		workingTicks = 0;
		states.clear();

		String initState = getInitialState().intern();
		StateRecord init = new StateRecord(initState, isValidWorkingState(initState));
		init.startTick = lastStateCollectionTick;
		presentState = init;
		states.put(init.name, init);

		this.setGraphicsForState(initState);
	}

	public ArrayList<StateEntityListener> getStateListeners() {
		return stateListeners;
	}

	/**
	 * Get the name of the initial state this Entity will be initialized with.
	 * @return
	 */
	public String getInitialState() {
		return "Idle";
	}

	/**
	 * Tests the given state name to see if it is valid for this Entity.
	 * @param state
	 * @return
	 */
	public boolean isValidState(String state) {
		return "Idle".equals(state) || "Working".equals(state);
	}

	/**
	 * Tests the given state name to see if it is counted as working hours when in
	 * that state..
	 * @param state
	 * @return
	 */
	public boolean isValidWorkingState(String state) {

		if( workingStateListInput.getValue().size() > 0 )
			return workingStateListInput.getValue().contains( state );

		return "Working".equals(state);
	}

	/**
	 * Sets the state of this Entity to the given state.
	 */
	public final void setPresentState( String state ) {
		if (presentState == null)
			this.initStateData();

		if (presentState.name.equals(state))
			return;

		StateRecord nextState = states.get(state);
		if (nextState == null) {
			if (!isValidState(state))
				error("Specified state: %s is not valid", state);

			String intState = state.intern();
			nextState = new StateRecord(intState, isValidWorkingState(intState));
			states.put(nextState.name, nextState);
		}

		this.setGraphicsForState(state);

		updateStateStats();
		nextState.startTick = lastStateCollectionTick;

		StateRecord prev = presentState;
		presentState = nextState;
		stateChanged(prev, presentState);
	}

	private void setGraphicsForState(String state) {

		if (stateGraphics.getValue() == null)
			return;

		DisplayEntity ent = stateGraphics.getValueFor(state);
		if (ent == null) {
			this.resetGraphics();
			return;
		}

		this.setDisplayModelList(ent.getDisplayModelList());
		this.setSize(ent.getSize());
		this.setOrientation(ent.getOrientation());
		this.setAlignment(ent.getAlignment());
	}

	/**
	 * A callback subclasses can override that is called on each state transition.
	 *
	 * The state has already been updated when this is called so presentState == next
	 * @param prev the state this Entity was in previously
	 * @param next the state this Entity is currently in
	 */
	public void stateChanged(StateRecord prev, StateRecord next) {

		if (traceState.getValue()) {
			long curTick = EventManager.simTicks();
			EventManager evt = EventManager.current();
			double duration = evt.ticksToSeconds(curTick - prev.getStartTick());
			double timeOfPrevStart = evt.ticksToSeconds(prev.getStartTick());
			stateReportFile.format("%.5f  %s.setState( \"%s\" ) dt = %g\n",
			                       timeOfPrevStart, this.getName(),
			                       prev.name, duration);
			stateReportFile.flush();
		}

		for (StateEntityListener each : stateListeners) {
			each.updateForStateChange(this, prev, next);
		}
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

		presentState.totalTicks += durTicks;
		presentState.currentCycleTicks += durTicks;
		if (presentState.working)
			workingTicks += durTicks;
	}

	/**
	 * Runs after initialization period
	 */
	public void collectInitializationStats() {
		updateStateStats();

		for (StateRecord each : states.values()) {
			each.initTicks = each.totalTicks;
			each.totalTicks = 0;
			each.completedCycleTicks = 0;
		}
	}

	/**
	 * Runs after each report interval
	 */
	public void clearReportStats() {
		updateStateStats();

		// clear totalHours for each state record
		for (StateRecord each : states.values()) {
			each.totalTicks = 0;
			each.completedCycleTicks = 0;
		}
	}

	/**
	 * Clear the current cycle hours, also reset the start of cycle time
	 */
	public void clearCurrentCycleStats() {
		updateStateStats();

		// clear current cycle hours for each state record
		for (StateRecord each : states.values()) {
			each.currentCycleTicks = 0;
		}
	}

	/**
	 * Runs when cycle is finished
	 */
	public void collectCycleStats() {
		updateStateStats();

		// finalize cycle for each state record
		for (StateRecord each : states.values()) {
			each.completedCycleTicks += each.currentCycleTicks;
			each.currentCycleTicks = 0;
		}
	}

	public void addState(String str) {
		if (states.get(str) != null)
			return;
		if (!isValidState(str))
			error("Specified state: %s is not valid", str);

		String state = str.intern();
		StateRecord stateRec = new StateRecord(state, isValidWorkingState(state));
		states.put(stateRec.name, stateRec);
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
			return sr1.name.compareTo(sr2.name);
		}
	}

	public ArrayList<StateRecord> getStateRecs() {
		ArrayList<StateRecord> recs = new ArrayList<>(states.size());
		for (StateRecord rec : states.values())
			recs.add(rec);
		Collections.sort(recs, new StateRecSort());
		return recs;
	}

	/**
	 * Return true if the entity is working
	 */
	public boolean isWorking() {
		return presentState.working;
	}

	/**
	 * A helper used to implement some of the state-based outputs, likely not
	 * useful for model code.
	 * @param simTicks
	 * @param state
	 * @return
	 */
	public long getTicksInState(long simTicks, StateRecord state) {
		if (state == null)
			return 0;

		long ticks = state.totalTicks;
		if (getState() == state)
			ticks += (simTicks - lastStateCollectionTick);
		return ticks;
	}

	public long getTicksInState(StateRecord state) {
		if (state == null)
			return 0;

		long ticks = state.totalTicks;
		if (getState() == state)
			ticks += (getSimTicks() - lastStateCollectionTick);
		return ticks;
	}

	public long getCurrentCycleTicks(StateRecord state) {
		if (state == null)
			return 0;

		long ticks = state.currentCycleTicks;
		if (getState() == state)
			ticks += (getSimTicks() - lastStateCollectionTick);
		return ticks;
	}

	public long getCompletedCycleTicks(StateRecord state) {
		if (state == null)
			return 0;

		return state.completedCycleTicks;
	}
	public long getInitTicks(StateRecord state) {
		if (state == null)
			return 0;

		return state.initTicks;
	}

	private long getWorkingTicks(long simTicks) {
		long ticks = workingTicks;
		if (presentState.working)
			ticks += (simTicks - lastStateCollectionTick);

		return ticks;
	}

	/**
	 * Returns the number of seconds that the entity has been in use.
	 */
	public double getWorkingTime() {
		long ticks = getWorkingTicks(getSimTicks());
		return EventManager.ticksToSecs(ticks);
	}

	public void setPresentState() {}

	/**
	 * Returns the elapsed time in seconds after the completion of the initialisation period
	 * that the entity has been in the specified state.
	 * @param simTime - present simulation time
	 * @param state - string representing the state
	 * @return
	 */
	public double getTimeInState(double simTime, String state) {
		long simTicks = EventManager.secsToNearestTick(simTime);
		StateRecord rec = states.get(state.intern());
		if (rec == null)
			return 0.0;
		long ticks = getTicksInState(simTicks, rec);
		return EventManager.ticksToSecs(ticks);
	}

	@Output(name = "State",
	 description = "The present state for the object.",
	    unitType = DimensionlessUnit.class,
	    sequence = 0)
	public String getPresentState(double simTime) {
		if (presentState == null) {
			return this.getInitialState();
		}
		return presentState.name;
	}

	@Output(name = "WorkingState",
	 description = "Returns TRUE if the present state is one of the working states.",
	    sequence = 1)
	public boolean isWorking(double simTime) {
		if (presentState == null) {
			return this.isValidWorkingState(this.getInitialState());
		}
		return presentState.working;
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
		long simTicks = EventManager.secsToNearestTick(simTime);
		long ticks = getWorkingTicks(simTicks);
		return EventManager.ticksToSecs(ticks);
	}

	@Output(name = "StateTimes",
	 description = "The total time recorded for each state after the completion of "
	             + "the initialisation period.",
	    unitType = TimeUnit.class,
	  reportable = true,
	    sequence = 3)
	public LinkedHashMap<String, Double> getStateTimes(double simTime) {
		long simTicks = EventManager.secsToNearestTick(simTime);
		LinkedHashMap<String, Double> ret = new LinkedHashMap<>(states.size());
		for (StateRecord stateRec : this.getStateRecs()) {
			long ticks = getTicksInState(simTicks, stateRec);
			Double t = EventManager.ticksToSecs(ticks);
			ret.put(stateRec.name, t);
		}
		return ret;
	}

}
