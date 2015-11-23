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
package com.jaamsim.Thresholds;

import com.jaamsim.DisplayModels.ShapeModel;
import com.jaamsim.basicsim.EntityTarget;
import com.jaamsim.events.Conditional;
import com.jaamsim.events.EventManager;
import com.jaamsim.events.ProcessTarget;
import com.jaamsim.input.BooleanInput;
import com.jaamsim.input.ColourInput;
import com.jaamsim.input.ExpError;
import com.jaamsim.input.ExpEvaluator;
import com.jaamsim.input.ExpressionInput;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.Output;
import com.jaamsim.math.Color4d;
import com.jaamsim.units.DimensionlessUnit;

public class ExpressionThreshold extends Threshold {

	@Keyword(description = "The logical condition for which the Threshold is open.",
	         example = "ExpressionThreshold1  OpenCondition { '[Queue1].QueueLength &gt 3' }")
	private final ExpressionInput openCondition;

	@Keyword(description = "The colour of the threshold graphic when the threshold condition is open, but the gate is still closed.",
	         example = "ExpressionThreshold1  PendingOpenColour { yellow }")
	private final ColourInput pendingOpenColour;

	@Keyword(description = "The colour of the threshold graphic when the threshold condition is closed, but the gate is still open.",
	         example = "ExpressionThreshold1  PendingClosedColour { yellow }")
	private final ColourInput pendingClosedColour;

	@Keyword(description = "A Boolean value.  If TRUE, the threshold displayed distinguishes the pending open and pending closed states.",
	         example = "Threshold1 ShowPendingStates { FALSE }")
	private final BooleanInput showPendingStates;

	{
		attributeDefinitionList.setHidden(false);

		openCondition = new ExpressionInput("OpenCondition", "Key Inputs", null);
		openCondition.setEntity(this);
		openCondition.setRequired(true);
		this.addInput(openCondition);

		pendingOpenColour = new ColourInput("PendingOpenColour", "Graphics", ColourInput.YELLOW);
		this.addInput(pendingOpenColour);
		this.addSynonym(pendingOpenColour, "PendingOpenColor");

		pendingClosedColour = new ColourInput("PendingClosedColour", "Graphics", ColourInput.PURPLE);
		this.addInput(pendingClosedColour);
		this.addSynonym(pendingClosedColour, "PendingClosedColor");

		showPendingStates = new BooleanInput("ShowPendingStates", "Graphics", true);
		this.addInput(showPendingStates);
	}

	@Override
    public void startUp() {
		super.startUp();

		doOpenClose();
	}

	class OpenChangedConditional extends Conditional {
		@Override
		public boolean evaluate() {
			return ExpressionThreshold.this.openStateChanged();
		}
	}
	private final Conditional openChanged = new OpenChangedConditional();

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

	void doOpenClose() {
		// Set the present state
		setOpen(this.getOpenConditionValue(this.getSimTime()));
		EventManager.scheduleUntil(doOpenClose, openChanged, null);
	}

	private boolean getOpenConditionValue(double simTime) {
		try {
			// Evaluate the condition (0 = false, non-zero = true)
			boolean ret = ExpEvaluator.evaluateExpression(openCondition.getValue(), simTime, this).value != 0;
			return ret;
		} catch(ExpError e) {
			error("%s", e.getMessage());
			return false; //never hit, error() will throw
		}
	}

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

	@Override
	public boolean isOpen() {
		boolean ret = this.getOpenConditionValue(getSimTime());
		if (ret != super.isOpen())
			this.scheduleProcessTicks(0, 2, setOpenTarget);
		return ret;
	}

	private final SetOpenTarget setOpenTarget = new SetOpenTarget(this);
	private static class SetOpenTarget extends EntityTarget<ExpressionThreshold> {
		SetOpenTarget(ExpressionThreshold thresh) {
			super(thresh, "setOpen");
		}

		@Override
		public void process() {
			ent.setOpen(ent.getOpenConditionValue(ent.getSimTime()));
		}
	}

	boolean openStateChanged() {
		return getOpenConditionValue(getSimTime()) != super.isOpen();
	}

	@Output(name = "Open",
	 description = "If open, then return TRUE.  Otherwise, return FALSE.",
	    unitType = DimensionlessUnit.class)
	@Override
	public Boolean getOpen(double simTime) {
		return this.getOpenConditionValue(simTime);
	}

}
