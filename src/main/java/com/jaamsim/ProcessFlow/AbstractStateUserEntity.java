/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2020-2022 JaamSim Software Inc.
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
package com.jaamsim.ProcessFlow;

import com.jaamsim.events.Conditional;
import com.jaamsim.events.EventManager;
import com.jaamsim.events.ProcessTarget;
import com.jaamsim.input.BooleanInput;
import com.jaamsim.input.ColourInput;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.Output;
import com.jaamsim.math.Color4d;
import com.jaamsim.states.StateEntity;

public abstract class AbstractStateUserEntity extends StateEntity {

	@Keyword(description = "If TRUE, the object's state will be recalculated at each event time "
	                     + "to verify that it has been set correctly by the program. "
	                     + "An error message will be generated if the state is not correct.",
	         exampleList = {"TRUE"})
	private final BooleanInput verifyState;

	{
		verifyState = new BooleanInput("VerifyState", OPTIONS, false);
		verifyState.setHidden(true);
		this.addInput(verifyState);
	}

	public static final String STATE_MAINTENANCE = "Maintenance";
	public static final String STATE_BREAKDOWN = "Breakdown";
	public static final String STATE_STOPPED = "Stopped";
	public static final String STATE_BLOCKED = "Blocked";
	public static final String STATE_SETUP = "Setup";
	public static final String STATE_SETDOWN = "Setdown";

	protected static final Color4d COL_MAINTENANCE = ColourInput.RED;
	protected static final Color4d COL_BREAKDOWN = ColourInput.RED;
	protected static final Color4d COL_STOPPED = ColourInput.getColorWithName("gray25");
	protected static final Color4d COL_BLOCKED = ColourInput.getColorWithName("gray25");
	protected static final Color4d COL_SETUP = ColourInput.getColorWithName("gray25");
	protected static final Color4d COL_SETDOWN = ColourInput.getColorWithName("gray25");

	public AbstractStateUserEntity() {}

	@Override
	public void startUp() {
		super.startUp();

		// Track any state changes
		if (verifyState.getValue())
			doStateVerification();
	}

	@Override
	public boolean isValidState(String state) {
		return true;
	}

	/**
	 * Returns whether the entity is working.
	 * @return true if working
	 */
	public abstract boolean isBusy();

	/**
	 * Returns whether set up is being performed.
	 * @return true if undergoing set up
	 */
	public abstract boolean isSetup();

	/**
	 * Returns whether set down is being performed.
	 * @return true if undergoing set down
	 */
	public abstract boolean isSetdown();

	/**
	 * Returns whether scheduled maintenance is being performed.
	 * @return true if undergoing maintenance
	 */
	public abstract boolean isMaintenance();

	/**
	 * Returns whether a breakdown is being repaired.
	 * @return true if being repaired
	 */
	public abstract boolean isBreakdown();

	/**
	 * Returns whether an operational limit prevents work.
	 * @return true if an operational limit is exceeded
	 */
	public abstract boolean isStopped();

	/**
	 * Returns whether the entity is able to work.
	 * @return true if available for work
	 */
	public boolean isAvailable() {
		return !isStopped() && !isMaintenance() && !isBreakdown() && isActive();
	}

	/**
	 * Returns whether the entity is not working because it has no tasks to perform.
	 * @return true if idle
	 */
	public boolean isIdle() {
		return !isBusy() && isAvailable() && !isSetup() && !isSetdown();
	}

	/**
	 * Returns whether something is not working because something is preventing it from working.
	 * @return true if unable to work
	 */
	public boolean isUnableToWork() {
		return !isBusy() && !isAvailable() && !isSetup() && !isSetdown();
	}

	public void setPresentState() {

		// Inactive
		if (!this.isActive()) {
			this.setPresentState(STATE_INACTIVE);
			return;
		}

		// Working (Busy)
		if (this.isBusy()) {
			this.setPresentState(STATE_WORKING);
			return;
		}

		// Not working because of maintenance or a closure (UnableToWork)
		if (this.isMaintenance()) {
			this.setPresentState(STATE_MAINTENANCE);
			return;
		}
		if (this.isBreakdown()) {
			this.setPresentState(STATE_BREAKDOWN);
			return;
		}
		if (this.isStopped()) {
			this.setPresentState(STATE_STOPPED);
			return;
		}

		// Setup
		if (this.isSetup()) {
			this.setPresentState(STATE_SETUP);
			return;
		}

		// Setdown
		if (this.isSetdown()) {
			this.setPresentState(STATE_SETDOWN);
			return;
		}

		// Not working because there is nothing to do (Idle)
		this.setPresentState(STATE_IDLE);
		return;
	}

	protected Color4d getColourForPresentState() {

		// Inactive
		if (!this.isActive()) {
			return COL_INACTIVE;
		}

		// Working (Busy)
		if (this.isBusy()) {
			return COL_WORKING;
		}

		// Not working because of maintenance or a closure (UnableToWork)
		if (this.isMaintenance()) {
			return COL_MAINTENANCE;
		}
		if (this.isBreakdown()) {
			return COL_BREAKDOWN;
		}
		if (this.isStopped()) {
			return COL_STOPPED;
		}

		// Setup
		if (this.isSetup()) {
			return COL_SETUP;
		}

		// Setdown
		if (this.isSetdown()) {
			return COL_SETDOWN;
		}

		// Not working because there is nothing to do (Idle)
		return COL_IDLE;
	}

	public double getTimeInState_Idle(double simTime) {
		return getTimeInState(simTime, STATE_IDLE);
	}

	public double getTimeInState_Working(double simTime) {
		return getTimeInState(simTime, STATE_WORKING);
	}

	public double getTimeInState_Maintenance(double simTime) {
		return getTimeInState(simTime, STATE_MAINTENANCE);
	}

	public double getTimeInState_Breakdown(double simTime) {
		return getTimeInState(simTime, STATE_BREAKDOWN);
	}

	public double getTimeInState_Stopped(double simTime) {
		return getTimeInState(simTime, STATE_STOPPED);
	}

	/**
	 * Loops from one state verification to the next.
	 */
	void doStateVerification() {
		EventManager.scheduleUntil(verifyStateTarget, verifyStateConditional, null);
	}

	// Perform state verification at each event time
	class VerifyStateConditional extends Conditional {
		@Override
		public boolean evaluate() {
			String state = getState().getName();
			setPresentState();
			String correctState = getState().getName();
			if (!correctState.equals(state))
				error("Present state is incorrect: presentState=%s, correctState=%s",
					state, correctState);
			return false;
		}
	}
	private final Conditional verifyStateConditional = new VerifyStateConditional();

	// Unused target for VerifyStateConditional
	class VerifyStateTarget extends ProcessTarget {
		@Override
		public String getDescription() {
			return AbstractStateUserEntity.this.getName() + ".verifyState";
		}

		@Override
		public void process() {
			// error message has already been generated in the 'evaluate' method
			doStateVerification();
		}
	}
	private final ProcessTarget verifyStateTarget = new VerifyStateTarget();

	@Output(name = "Idle",
	 description = "Returns TRUE if able to work but there is no work to perform. "
	             + "For an EntitySystem, TRUE is returned if all the entities in the system are idle.",
	    sequence = 1)
	public boolean isIdle(double simTime) {
		return isIdle();
	}

	@Output(name = "Working",
	 description = "Returns TRUE if work is being performed. "
	             + "For an EntitySystem, TRUE is returned if any of the entities in the system "
	             + "are working.",
	    sequence = 2)
	public boolean isBusy(double simTime) {
		return isBusy();
	}

	@Output(name = "Setup",
	 description = "Returns TRUE if setup is being performed. "
	             + "For an EntitySystem, TRUE is returned if setup is being performed on any of "
	             + "the entities in the system.",
	    sequence = 3)
	public boolean isSetup(double simTime) {
		return isSetup();
	}

	@Output(name = "Setdown",
	 description = "Returns TRUE if setdown is being performed. "
	             + "For an EntitySystem, TRUE is returned if setdown is being performed on any of "
	             + "the entities in the system.",
	    sequence = 4)
	public boolean isSetdown(double simTime) {
		return isSetdown();
	}

	@Output(name = "Maintenance",
	 description = "Returns TRUE if maintenance is being performed. "
	             + "For an EntitySystem, TRUE is returned if maintenance is being performed on "
	             + "any of the entities in the system.",
	    sequence = 5)
	public boolean isMaintenance(double simTime) {
		return isMaintenance();
	}

	@Output(name = "Breakdown",
	 description = "Returns TRUE if a breakdown is being repaired. "
	             + "For an EntitySystem, TRUE is returned if a breakdown is being repaired on "
	             + "any of the entities in the system.",
	    sequence = 6)
	public boolean isBreakdown(double simTime) {
		return isBreakdown();
	}

	@Output(name = "Stopped",
	 description = "Returns TRUE if an operating limit prevents work from being performed. "
	             + "For an EntitySystem, TRUE is returned if an operating limit prevents work "
	             + "from being performed on any of the entities in the system.",
	    sequence = 7)
	public boolean isStopped(double simTime) {
		return isStopped();
	}

	@Output(name = "Utilisation",
	 description = "The fraction of calendar time (excluding the initialisation period) that "
	             + "this object is in the Working state. Includes any completed cycles.",
	  reportable = true,
	    sequence = 8)
	public double getUtilisation(double simTime) {
		double total = this.getTotalTime(simTime);
		double working = getTimeInState_Working(simTime);
		return working/total;
	}

	@Output(name = "Commitment",
	 description = "The fraction of calendar time (excluding the initialisation period) that "
	             + "this object is in any state other than Idle. Includes any completed cycles.",
	  reportable = true,
	    sequence = 9)
	public double getCommitment(double simTime) {
		double total = this.getTotalTime(simTime);
		double idle = getTimeInState_Idle(simTime);
		return 1.0d - idle/total;
	}

	@Output(name = "Availability",
	 description = "The fraction of calendar time (excluding the initialisation period) that "
	             + "this object is in any state other than Maintenance or Breakdown. "
	             + "Includes any completed cycles.",
	  reportable = true,
	    sequence = 10)
	public double getAvailability(double simTime) {
		double total = this.getTotalTime(simTime);
		double maintenance = getTimeInState_Maintenance(simTime);
		double breakdown = getTimeInState_Breakdown(simTime);
		return 1.0d - (maintenance + breakdown)/total;
	}

	@Output(name = "Reliability",
	 description = "The ratio of Working time to the sum of Working time and Breakdown time. "
	             + "All times exclude the initialisation period and include any completed cycles.",
	  reportable = true,
	    sequence = 11)
	public double getReliability(double simTime) {
		double working = getTimeInState_Working(simTime);
		double breakdown = getTimeInState_Breakdown(simTime);
		return working / (working + breakdown);
	}

}
