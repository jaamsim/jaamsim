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

import com.jaamsim.events.Conditional;
import com.jaamsim.events.EventManager;
import com.jaamsim.events.ProcessTarget;
import com.jaamsim.input.ExpEvaluator;
import com.jaamsim.input.ExpressionInput;
import com.jaamsim.input.InputErrorException;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.Output;
import com.jaamsim.units.DimensionlessUnit;

public class ExpressionThreshold extends Threshold {

	@Keyword(description = "The logical condition for which the Threshold is open.",
	         example = "ExpressionThreshold1  OpenCondition { '[Queue1].QueueLength < 3' }")
	private final ExpressionInput openCondition;

	{
		openCondition = new ExpressionInput("OpenCondition", "Key Inputs", null);
		openCondition.setEntity(this);
		this.addInput(openCondition);
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
			return ExpressionThreshold.this.getInputName() + ".doOpenClose";
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
			boolean ret = ExpEvaluator.evaluateExpression(openCondition.getValue(), simTime, this, null).value != 0;
			return ret;
		} catch(ExpEvaluator.Error e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public boolean isOpen() {
		return this.getOpenConditionValue(getSimTime());
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
