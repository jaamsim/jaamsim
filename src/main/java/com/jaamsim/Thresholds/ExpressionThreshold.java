/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2014 Ausenco Engineering Canada Inc.
 * Copyright (C) 2016-2020 JaamSim Software Inc.
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
package com.jaamsim.Thresholds;

import java.util.ArrayList;

import com.jaamsim.DisplayModels.ShapeModel;
import com.jaamsim.basicsim.EntityTarget;
import com.jaamsim.basicsim.ErrorException;
import com.jaamsim.basicsim.ObserverEntity;
import com.jaamsim.basicsim.SubjectEntity;
import com.jaamsim.events.Conditional;
import com.jaamsim.events.EventHandle;
import com.jaamsim.events.EventManager;
import com.jaamsim.events.ProcessTarget;
import com.jaamsim.input.BooleanInput;
import com.jaamsim.input.ColourInput;
import com.jaamsim.input.ExpError;
import com.jaamsim.input.ExpEvaluator;
import com.jaamsim.input.ExpResType;
import com.jaamsim.input.ExpressionInput;
import com.jaamsim.input.Input;
import com.jaamsim.input.InterfaceEntityListInput;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.Output;
import com.jaamsim.math.Color4d;
import com.jaamsim.units.DimensionlessUnit;
import com.jaamsim.units.RateUnit;

public class ExpressionThreshold extends Threshold implements ObserverEntity {

	@Keyword(description = "The logical condition for the ExpressionThreshold to open.",
	         exampleList = { "'[Queue1].QueueLength > 3'" })
	private final ExpressionInput openCondition;

	@Keyword(description = "The logical condition for the ExpressionThreshold to close.\n"
	                     + "If not specified, the CloseCondition defaults to the opposite of the "
	                     + "OpenCondition. If the OpenCondition and CloseCondition are both TRUE, "
	                     + "then the ExpressionThreshold is set to open.",
	         exampleList = { "'[Queue1].QueueLength < 2'" })
	private final ExpressionInput closeCondition;

	@Keyword(description = "The initial state for the ExpressionThreshold: "
	                     + "TRUE = Open, FALSE = Closed.\n"
	                     + "This input is only relevant when the CloseCondition input is used "
	                     + "and both the OpenCondition and CloseCondition are FALSE at the "
	                     + "start of the simulation run. Otherwise, the initial state is "
	                     + "determined explicitly by the OpenCondition and CloseCondition.",
	         exampleList = { "TRUE" })
	private final BooleanInput initialOpenValue;

	@Keyword(description = "The colour of the ExpressionThreshold graphic when the threshold "
	                     + "condition is open, but the gate is still closed.",
	         exampleList = { "yellow" })
	private final ColourInput pendingOpenColour;

	@Keyword(description = "The colour of the ExpressionThreshold graphic when the threshold "
	                     + "condition is closed, but the gate is still open.",
	         exampleList = { "magenta" })
	private final ColourInput pendingClosedColour;

	@Keyword(description = "A Boolean value. If TRUE, the ExpressionThreshold displays the "
	                     + "pending open and pending closed states.",
	         exampleList = { "FALSE" })
	private final BooleanInput showPendingStates;

	@Keyword(description = "An optional list of objects to monitor.\n\n"
	                     + "If the WatchList input is provided, the ExpressionThreshold evaluates "
	                     + "its OpenCondition and CloseCondition expression inputs and set its "
	                     + "open/closed state ONLY when triggered by an object in its WatchList. "
	                     + "This is much more efficient than the default behaviour which "
	                     + "evalautes these expressions at every event time and whenever its "
	                     + "state is queried by another object.\n\n"
	                     + "Care must be taken to ensure that the WatchList input includes every "
	                     + "object that can trigger the OpenCondition or CloseCondition expressions. "
	                     + "Normally, the WatchList should include every object that is referenced "
	                     + "directly or indirectly by these expressions. "
	                     + "The VerfiyWatchList input can be used to ensure that the WatchList "
	                     + "includes all the necessary objects.",
	         exampleList = {"Object1  Object2"})
	protected final InterfaceEntityListInput<SubjectEntity> watchList;

	@Keyword(description = "Allows the user to verify that the input to the 'WatchList' keyword "
	                     + "includes all the objects that affect the ExpressionThreshold's state. "
	                     + "When set to TRUE, the ExpressionThreshold uses both the normal logic "
	                     + "and the WatchList logic to set its state. "
	                     + "An error message is generated if the threshold changes state without "
	                     + "being triggered by a WatchList object.",
	         exampleList = { "TRUE" })
	private final BooleanInput verifyWatchList;

	private boolean lastOpenValue; // state of the threshold that was calculated on-demand
	private boolean useLastValue;
	private long numCalls;
	private long numEvals;

	{
		attributeDefinitionList.setHidden(false);

		openCondition = new ExpressionInput("OpenCondition", KEY_INPUTS, null);
		openCondition.setUnitType(DimensionlessUnit.class);
		openCondition.setResultType(ExpResType.NUMBER);
		openCondition.setRequired(true);
		this.addInput(openCondition);

		closeCondition = new ExpressionInput("CloseCondition", KEY_INPUTS, null);
		closeCondition.setUnitType(DimensionlessUnit.class);
		closeCondition.setResultType(ExpResType.NUMBER);
		this.addInput(closeCondition);

		initialOpenValue = new BooleanInput("InitialOpenValue", KEY_INPUTS, false);
		this.addInput(initialOpenValue);

		pendingOpenColour = new ColourInput("PendingOpenColour", FORMAT, ColourInput.YELLOW);
		this.addInput(pendingOpenColour);
		this.addSynonym(pendingOpenColour, "PendingOpenColor");

		pendingClosedColour = new ColourInput("PendingClosedColour", FORMAT, ColourInput.PURPLE);
		this.addInput(pendingClosedColour);
		this.addSynonym(pendingClosedColour, "PendingClosedColor");

		showPendingStates = new BooleanInput("ShowPendingStates", FORMAT, true);
		this.addInput(showPendingStates);

		watchList = new InterfaceEntityListInput<>(SubjectEntity.class, "WatchList", KEY_INPUTS, new ArrayList<>());
		watchList.setIncludeSelf(false);
		watchList.setUnique(true);
		this.addInput(watchList);

		verifyWatchList = new BooleanInput("VerifyWatchList", KEY_INPUTS, false);
		this.addInput(verifyWatchList);
	}

	public ExpressionThreshold() {}

	@Override
	public void validate() {
		super.validate();
		ObserverEntity.validate(this);
	}

	@Override
	public void earlyInit() {
		super.earlyInit();
		lastOpenValue = initialOpenValue.getValue();
		lastOpenValue = this.getOpenConditionValue(0.0);
		useLastValue = false;
		numCalls = 0L;
		numEvals = 0L;
	}

	@Override
	public void lateInit() {
		super.lateInit();
		ObserverEntity.registerWithSubjects(this, getWatchList());
	}

	@Override
	public void updateForInput(Input<?> in) {
		super.updateForInput(in);

		if (in == openCondition || in == closeCondition || in == initialOpenValue) {
			lastOpenValue = initialOpenValue.getValue();
			this.setInitialOpenValue(this.getOpenConditionValue(0.0));
			return;
		}
	}

	@Override
	public void startUp() {
		super.startUp();

		// If there is no WatchList, the open/close expressions are tested after every event
		if (!isWatchList() || isVerifyWatchList())
			doOpenClose();
	}

	@Override
	public ArrayList<SubjectEntity> getWatchList() {
		return watchList.getValue();
	}

	public boolean isVerifyWatchList() {
		return verifyWatchList.getValue();
	}

	public boolean isWatchList() {
		return !getWatchList().isEmpty();
	}

	/**
	 * Loops from one state change to the next.
	 */
	void doOpenClose() {
		// Set the present state
		setOpen(this.getOpenConditionValue(this.getSimTime()));

		// Wait until the state is ready to change
		EventManager.scheduleUntil(doOpenCloseTarget, openChangedConditional, null);
	}

	private final Conditional openChangedConditional = new Conditional() {
		@Override
		public boolean evaluate() {
			return getOpenConditionValue(getSimTime()) != ExpressionThreshold.super.isOpen();
		}
	};

	private final ProcessTarget doOpenCloseTarget = new EntityTarget<ExpressionThreshold>(this, "doOpenClose") {
		@Override
		public void process() {
			if (isVerifyWatchList())
				error(ERR_WATCHLIST);
			doOpenClose();
		}
	};

	/**
	 * Returns the state implied by the present values for the OpenCondition
	 * and CloseCondition expressions.
	 * @param simTime - present simulation time.
	 * @return state implied by the OpenCondition and CloseCondition expressions.
	 */
	private boolean getOpenConditionValue(double simTime) {
		try {
			if (openCondition.getValue() == null)
				return super.isOpen();

			// Evaluate the open condition (0 = false, non-zero = true)
			boolean openCond = ExpEvaluator.evaluateExpression(openCondition.getValue(),
					simTime).value != 0;

			// If the open condition is satisfied or there is no close condition, then we are done
			boolean ret;
			if (openCond || closeCondition.getValue() == null) {
				ret = openCond;
			}

			// The open condition is false
			else {

				// If the close condition is satisfied, then the threshold is closed
				boolean closeCond = ExpEvaluator.evaluateExpression(closeCondition.getValue(),
						simTime).value != 0;
				if (closeCond) {
					ret = false;
				}

				// If the open and close conditions are both false, then the state is unchanged
				else {
					ret = lastOpenValue;
				}
			}

			// Save the threshold's last state (unless called by the UI thread)
			if (EventManager.hasCurrent()) {
				lastOpenValue = ret;
				numCalls++;
				numEvals++;
			}
			return ret;
		}
		catch (ExpError e) {
			throw new ErrorException(this, e);
		}
	}

	@Override
	public boolean isOpen() {

		// If called from the user interface or if a Controller has been specified,
		// then return the saved state
		if (!EventManager.hasCurrent())
			return super.isOpen();

		if (useLastValue && isWatchList()) {
			numCalls++;
			return super.isOpen();
		}

		// Determine the state implied by the OpenCondition and CloseCondition expressions
		boolean ret = this.getOpenConditionValue(getSimTime());

		// If necessary, schedule an event to change the saved state
		if (ret != super.isOpen())
			performSetOpen();

		// Return the value calculated on demand
		return ret;
	}

	private void performSetOpen() {
		// The event is scheduled LIFO so it is performed as soon as possible, before the condition
		// can change again.
		if (!setOpenHandle.isScheduled()) {
			if (isTraceFlag()) trace(0, "performSetOpen()");
			this.scheduleProcessTicks(0L, 2, false, setOpenTarget, setOpenHandle);  // LIFO
		}
	}

	private final EventHandle setOpenHandle = new EventHandle();
	private final ProcessTarget setOpenTarget = new EntityTarget<ExpressionThreshold>(this, "setOpen") {
		@Override
		public void process() {
			boolean bool = getOpenConditionValue(getSimTime());
			if (isTraceFlag()) trace(0, "setOpen(%s)", bool);
			setOpen(bool);
			useLastValue = true;
		}
	};

	@Override
	public void observerUpdate(SubjectEntity subj) {
		useLastValue = false;
		if (observerUpdateHandle.isScheduled())
			return;
		// Priority set to 99 to ensure that this event executed just before the conditional events
		this.scheduleProcessTicks(0L, 99, false, setOpenTarget, observerUpdateHandle);  // LIFO
	}

	private final EventHandle observerUpdateHandle = new EventHandle();

	@Override
	public void updateGraphics(double simTime) {
		super.updateGraphics(simTime);

		// Trap the pending cases
		if (!showPendingStates.getValue())
			return;

		boolean threshOpen = super.isOpen();
		try {
			if (getOpenConditionValue(simTime) == threshOpen)
				return;
		}
		catch (Throwable t) {
			return;
		}


		// Select the colour
		Color4d col;
		if (threshOpen)
			col = pendingClosedColour.getValue();
		else
			col = pendingOpenColour.getValue();

		// Display the threshold icon
		setTagVisibility(ShapeModel.TAG_CONTENTS, true);
		setTagVisibility(ShapeModel.TAG_OUTLINES, true);
		setTagColour(ShapeModel.TAG_CONTENTS, col);
		setTagColour(ShapeModel.TAG_OUTLINES, ColourInput.BLACK);
	}

	@Output(name = "Open",
	 description = "If open, then return TRUE.  Otherwise, return FALSE.",
	    unitType = DimensionlessUnit.class,
	    sequence = 1)
	@Override
	public Boolean getOpen(double simTime) {
		return this.getOpenConditionValue(simTime);
	}

	@Output(name = "FracEval",
	 description = "Fraction of times that the threshold expression was evaluated out of the "
	             + "total number of times the threshold state was obtained.",
	    unitType = DimensionlessUnit.class,
	    sequence = 2)
	public double getFracEval(double simTime) {
		return (double) numEvals / numCalls;
	}

	@Output(name = "EvalRate",
	 description = "Number of times that the threshold expression is evaluated per unit "
	             + "simulation time.",
	    unitType = RateUnit.class,
	    sequence = 3)
	public double getEvalRate(double simTime) {
		return numEvals / simTime;
	}

}
