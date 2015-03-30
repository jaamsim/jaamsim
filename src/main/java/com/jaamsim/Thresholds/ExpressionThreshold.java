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
package com.jaamsim.Thresholds;

import com.jaamsim.Graphics.DisplayModelCompat;
import com.jaamsim.basicsim.EntityTarget;
import com.jaamsim.events.Conditional;
import com.jaamsim.events.EventManager;
import com.jaamsim.events.ProcessTarget;
import com.jaamsim.input.BooleanInput;
import com.jaamsim.input.ColourInput;
import com.jaamsim.input.ExpError;
import com.jaamsim.input.ExpEvaluator;
import com.jaamsim.input.ExpressionInput;
import com.jaamsim.input.InputErrorException;
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
		openCondition = new ExpressionInput("OpenCondition", "Key Inputs", null);
		openCondition.setEntity(this);
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
	public void validate() throws InputErrorException {
		super.validate();

		if (openCondition == null)
			throw new InputErrorException( "The keyword OpenCondition must be set." );
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
		setTagVisibility(DisplayModelCompat.TAG_CONTENTS, true);
		setTagVisibility(DisplayModelCompat.TAG_OUTLINES, true);
		setTagColour(DisplayModelCompat.TAG_CONTENTS, col);
		setTagColour(DisplayModelCompat.TAG_OUTLINES, ColourInput.BLACK);
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
