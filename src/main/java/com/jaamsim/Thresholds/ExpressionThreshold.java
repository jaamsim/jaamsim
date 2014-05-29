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

import com.jaamsim.input.ExpEvaluator;
import com.jaamsim.input.ExpressionInput;
import com.jaamsim.input.Keyword;
import com.sandwell.JavaSimulation.InputErrorException;

public class ExpressionThreshold extends Threshold {

	@Keyword(description = "The logical condition for which the Threshold is open.",
	         example = "ExpressionThreshold1  OpenCondition { '[Queue1].QueueLength < 3' }")
	private final ExpressionInput openCondition;

	{
		openCondition = new ExpressionInput("OpenCondition", "Key Inputs", null);
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

	private void doOpenClose() {

		// Set the present state
		setOpen(this.getOpenConditionValue(this.getSimTime()));

		// Loop endlessly
		while (true) {

			// Wait until the state has changed
			while( this.getOpenConditionValue(this.getSimTime()) == isOpen() ) {
				waitUntil();
			}
			waitUntilEnded();

			// Set the present state
			setOpen(this.getOpenConditionValue(this.getSimTime()));
		}
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

}
