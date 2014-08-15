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

import com.jaamsim.basicsim.ErrorException;
import com.jaamsim.input.Output;
import com.jaamsim.units.DimensionlessUnit;
import com.sandwell.JavaSimulation.Entity;
import com.sandwell.JavaSimulation3D.DisplayEntity;

public class StateEntity extends DisplayEntity {
	private StateRecord presentState; // The present state of the entity
	private final ArrayList<StateRecord> states;
	private final ArrayList<StateEntityListener> stateListeners;

	private long lastStateCollectionTick;
	private long workingTicks;

	public StateEntity() {
		states = new ArrayList<StateRecord>();
		stateListeners = new ArrayList<StateEntityListener>();
	}

	@Override
	public void earlyInit() {
		super.earlyInit();

		lastStateCollectionTick = getSimTicks();

		workingTicks = 0;
		states.clear();

		String initState = getInitialState();
		StateRecord init = new StateRecord(initState, isValidWorkingState(initState));
		init.startTick = lastStateCollectionTick;
		presentState = init;
		states.add(init);

		stateListeners.clear();
		for (Entity ent : Entity.getClonesOfIterator(Entity.class, StateEntityListener.class)) {
			StateEntityListener sel = (StateEntityListener)ent;
			if (sel.isWatching(this))
				stateListeners.add(sel);
		}
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

		int stateIdx = this.stateIdx(state);
		StateRecord nextState;
		if (stateIdx < 0) {
			if (!isValidState(state))
				throw new ErrorException("Specified state: %s is not valid for Entity: %s",
				                         state, this.getName());

			nextState = new StateRecord(state, isValidWorkingState(state));
			states.add(-stateIdx - 1, nextState);
		}
		else {
			nextState = states.get(stateIdx);
		}

		updateStateStats();
		nextState.startTick = lastStateCollectionTick;

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

		for (StateRecord each : getStateRecs()) {
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
		for (StateRecord each : getStateRecs()) {
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
		for (StateRecord each : getStateRecs()) {
			each.currentCycleTicks = 0;
		}
	}

	/**
	 * Runs when cycle is finished
	 */
	public void collectCycleStats() {
		updateStateStats();

		// finalize cycle for each state record
		for (StateRecord each : getStateRecs()) {
			each.completedCycleTicks += each.currentCycleTicks;
			each.currentCycleTicks = 0;
		}
	}

	@Output(name = "State",
	        description = "The present model state of the object",
	        unitType = DimensionlessUnit.class)
	public String getPresentState(double time) {
		return presentState.name;
	}

	/**
	 * Return the index of the given state, or (-insertionPoint - 1), see Arrays.binarySearach
	 * @param state
	 * @return
	 */
	private int stateIdx(String state) {
		int lowIdx = 0;
		int highIdx = states.size() - 1;

		while (lowIdx <= highIdx) {
			final int testIdx = (lowIdx + highIdx) >>> 1; // use unsigned shift to avoid overflow
			final int comp = states.get(testIdx).name.compareTo(state);
			if (comp < 0) {
				lowIdx = testIdx + 1;
			}
			else if (comp > 0) {
				highIdx = testIdx - 1;
			}
			else {
				return testIdx;
			}
		}

		return -(lowIdx + 1);
	}

	public StateRecord getState(String state) {
		int idx = stateIdx(state);
		if (idx < 0)
			return null;

		return states.get(idx);
	}

	public StateRecord getState() {
		return presentState;
	}

	public ArrayList<StateRecord> getStateRecs() {
		return states;
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
