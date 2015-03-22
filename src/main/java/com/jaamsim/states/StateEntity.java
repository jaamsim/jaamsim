/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2014 Ausenco Engineering Canada Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 */
package com.jaamsim.states;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

import com.jaamsim.Graphics.DisplayEntity;
import com.jaamsim.basicsim.Entity;
import com.jaamsim.basicsim.FileEntity;
import com.jaamsim.basicsim.Simulation;
import com.jaamsim.input.BooleanInput;
import com.jaamsim.input.InputAgent;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.Output;
import com.jaamsim.input.StringKeyInput;
import com.jaamsim.units.DimensionlessUnit;

public class StateEntity extends DisplayEntity {

	@Keyword(description = "A list of state/DisplayEntity pairs. For each state," +
			" the graphics will be changed to those for the corresponding DisplayEntity.",
	         example = "Object1  StateGraphics { { idle DisplayEntity1 } { working DisplayEntity2 }")
	protected final StringKeyInput<DisplayEntity> stateGraphics;

	@Keyword(description = "If TRUE, a log file (.trc) will be printed with the time of every state change during the run.",
	         example = "Object1  TraceState { TRUE }")
	private final BooleanInput traceState;

	private StateRecord presentState; // The present state of the entity
	private final HashMap<String, StateRecord> states;
	private final ArrayList<StateEntityListener> stateListeners;

	private long lastStateCollectionTick;
	private long workingTicks;

	protected FileEntity stateReportFile;        // The file to store the state information

	{
		stateGraphics = new StringKeyInput<>(DisplayEntity.class, "StateGraphics", "Key Inputs");
		stateGraphics.setHidden(true);
		this.addInput(stateGraphics);

		traceState = new BooleanInput("TraceState", "Key Inputs", false);
		traceState.setHidden(true);
		this.addInput(traceState);
	}

	public StateEntity() {
		states = new HashMap<>();
		stateListeners = new ArrayList<>();
	}

	@Override
	public void earlyInit() {
		super.earlyInit();

		lastStateCollectionTick = getSimTicks();

		workingTicks = 0;
		states.clear();

		String initState = getInitialState().intern();
		StateRecord init = new StateRecord(initState, isValidWorkingState(initState));
		init.startTick = lastStateCollectionTick;
		presentState = init;
		states.put(init.name, init);
		this.setGraphicsForState(initState);

		if (testFlag(FLAG_GENERATED))
			return;

		stateListeners.clear();
		for (Entity ent : Entity.getClonesOfIterator(Entity.class, StateEntityListener.class)) {
			StateEntityListener sel = (StateEntityListener)ent;
			if (sel.isWatching(this))
				stateListeners.add(sel);
		}

		// Create state trace file if required
		if (traceState.getValue()) {
			String fileName = InputAgent.getReportFileName(InputAgent.getRunName() + "-" + this.getName() + ".trc");
			stateReportFile = new FileEntity( fileName);
		}
	}

	public ArrayList<StateEntityListener> getStateListeners() {
		return stateListeners;
	}

	@Override
	public void printReport(FileEntity file, double simTime) {
		super.printReport(file, simTime);

		long totalTicks = 0;
		long workingTicks = 0;

		// Loop through the states
		for (StateRecord st : this.getStateRecs()) {
			long ticks = this.getTicksInState(st);
			if (ticks == 0)
				continue;

			double hours = ticks / Simulation.getSimTimeFactor();
			file.format("%s\tStateTime[%s, h]\t%f\n", this.getName(), st.name, hours);

			totalTicks += ticks;
			if (st.working)
				workingTicks += ticks;
		}

		file.format("%s\tStateTime[%s, h]\t%f\n", this.getName(), "TotalTime", totalTicks / Simulation.getSimTimeFactor());
		file.format("%s\tStateTime[%s, h]\t%f\n", this.getName(), "WorkingTime", workingTicks / Simulation.getSimTimeFactor());
		file.format("%n");
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
		return "Working".equals(state);
	}

	/**
	 * Sets the state of this Entity to the given state.
	 */
	public final void setPresentState( String state ) {
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
		for (StateEntityListener each : stateListeners) {
			each.updateForStateChange(this, prev, next);
		}

		if (traceState.getValue()) {
			long curTick = getSimTicks();
			double duration = (curTick - prev.getStartTick()) / Simulation.getSimTimeFactor();
			double timeOfPrevStart = prev.getStartTick() / Simulation.getSimTimeFactor();
			stateReportFile.format("%.5f  %s.setState( \"%s\" ) dt = %g\n",
			                       timeOfPrevStart, this.getName(),
			                       prev.name, duration);
			stateReportFile.flush();
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

	@Output(name = "State",
	        description = "The present model state of the object",
	        unitType = DimensionlessUnit.class)
	public String getPresentState(double time) {
		if (presentState == null) {
			return "";
		}
		return presentState.name;
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

	public long getWorkingTicks() {
		long ticks = workingTicks;
		if (presentState.working)
			ticks += (getSimTicks() - lastStateCollectionTick);

		return ticks;
	}

}
