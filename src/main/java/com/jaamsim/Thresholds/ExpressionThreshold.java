/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2014 Ausenco Engineering Canada Inc.
 * Copyright (C) 2016-2018 JaamSim Software Inc.
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

import com.jaamsim.DisplayModels.ShapeModel;
import com.jaamsim.basicsim.EntityTarget;
import com.jaamsim.basicsim.ErrorException;
import com.jaamsim.events.Conditional;
import com.jaamsim.events.EventHandle;
import com.jaamsim.events.EventManager;
import com.jaamsim.events.ProcessTarget;
import com.jaamsim.input.BooleanInput;
import com.jaamsim.input.ColourInput;
import com.jaamsim.input.ExpError;
import com.jaamsim.input.ExpEvaluator;
import com.jaamsim.input.ExpressionInput;
import com.jaamsim.input.Input;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.Output;
import com.jaamsim.math.Color4d;
import com.jaamsim.units.DimensionlessUnit;

public class ExpressionThreshold extends Threshold {

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

	private boolean lastOpenValue; // state of the threshold that was calculated on-demand

	{
		attributeDefinitionList.setHidden(false);

		openCondition = new ExpressionInput("OpenCondition", KEY_INPUTS, null);
		openCondition.setEntity(this);
		openCondition.setUnitType(DimensionlessUnit.class);
		openCondition.setRequired(true);
		this.addInput(openCondition);

		closeCondition = new ExpressionInput("CloseCondition", KEY_INPUTS, null);
		closeCondition.setEntity(this);
		closeCondition.setUnitType(DimensionlessUnit.class);
		this.addInput(closeCondition);

		initialOpenValue = new BooleanInput("InitialOpenValue", KEY_INPUTS, false);
		this.addInput(initialOpenValue);

		pendingOpenColour = new ColourInput("PendingOpenColour", GRAPHICS, ColourInput.YELLOW);
		this.addInput(pendingOpenColour);
		this.addSynonym(pendingOpenColour, "PendingOpenColor");

		pendingClosedColour = new ColourInput("PendingClosedColour", GRAPHICS, ColourInput.PURPLE);
		this.addInput(pendingClosedColour);
		this.addSynonym(pendingClosedColour, "PendingClosedColor");

		showPendingStates = new BooleanInput("ShowPendingStates", GRAPHICS, true);
		this.addInput(showPendingStates);
	}

	public ExpressionThreshold() {}

	@Override
	public void earlyInit() {
		super.earlyInit();
		lastOpenValue = initialOpenValue.getValue();
		lastOpenValue = this.getOpenConditionValue(0.0);
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

		doOpenClose();
	}

	/**
	 * Loops from one state change to the next.
	 */
	void doOpenClose() {
		// Set the present state
		setOpen(this.getOpenConditionValue(this.getSimTime()));

		// Wait until the state is ready to change
		EventManager.scheduleUntil(doOpenClose, openChanged, null);
	}

	/**
	 * Returns true if the saved state differs from the state implied by the OpenCondition
	 * and CloseCondition
	 * @return true if the state has changed
	 */
	boolean openStateChanged() {
		return getOpenConditionValue(getSimTime()) != super.isOpen();
	}

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
			if (EventManager.hasCurrent())
				lastOpenValue = ret;
			return ret;
		}
		catch (ExpError e) {
			throw new ErrorException(this, e);
		}
	}

	@Override
	public boolean isOpen() {

		// If called from the user interface, return the saved state
		if (!EventManager.hasCurrent())
			return super.isOpen();

		// Determine the state implied by the OpenCondition and CloseCondition expressions
		boolean ret = this.getOpenConditionValue(getSimTime());

		// If necessary, schedule an event to change the saved state
		if (ret != super.isOpen() && !setOpenHandle.isScheduled()) {
			if (isTraceFlag()) trace(0, "isOpen()=%s, super.isOpen()=%s", ret, super.isOpen());
			this.scheduleProcessTicks(0L, 2, true, setOpenTarget, setOpenHandle);  // FIFO
		}

		// Return the value calculated on demand
		return ret;
	}

	/**
	 * Conditional that tests whether the state has changed
	 */
	class OpenChangedConditional extends Conditional {
		@Override
		public boolean evaluate() {
			return ExpressionThreshold.this.openStateChanged();
		}
	}
	private final Conditional openChanged = new OpenChangedConditional();

	/**
	 * ProcessTarget the executes the doOpenClose() method
	 */
	class DoOpenCloseTarget extends ProcessTarget {
		@Override
		public String getDescription() {
			return ExpressionThreshold.this.getName() + ".doOpenClose";
		}

		@Override
		public void process() {
			doOpenClose();
		}
	}
	private final ProcessTarget doOpenClose = new DoOpenCloseTarget();

	private static class SetOpenTarget extends EntityTarget<ExpressionThreshold> {
		SetOpenTarget(ExpressionThreshold thresh) {
			super(thresh, "setOpen");
		}

		@Override
		public void process() {
			boolean bool = ent.getOpenConditionValue(ent.getSimTime());
			if (ent.isTraceFlag()) ent.trace(0, "setOpen(%s)", bool);
			ent.setOpen(bool);
		}
	}
	private final SetOpenTarget setOpenTarget = new SetOpenTarget(this);
	private final EventHandle setOpenHandle = new EventHandle();

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
	    unitType = DimensionlessUnit.class)
	@Override
	public Boolean getOpen(double simTime) {
		return this.getOpenConditionValue(simTime);
	}

}
