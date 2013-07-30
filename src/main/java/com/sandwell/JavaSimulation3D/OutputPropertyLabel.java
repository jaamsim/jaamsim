/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2013 Ausenco Engineering Canada Inc.
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
package com.sandwell.JavaSimulation3D;

import com.jaamsim.input.OutputHandle;
import com.sandwell.JavaSimulation.Input;
import com.sandwell.JavaSimulation.IntegerInput;
import com.sandwell.JavaSimulation.Keyword;
import com.sandwell.JavaSimulation.StringInput;

public class OutputPropertyLabel extends TextLabel {

	@Keyword(description = "The number of decimal places displayed by the label when displaying floating point values.",
	         example = "Label Precision { 1 }")
	private final IntegerInput precision;

	@Keyword(description = "The text to use if this text label can not be properly resolved (for example there is a null in the chain)",
	         example = "Label FailureText { 'N/A' }")
	private final StringInput failureText;

	private String doubleFormat = "%.0f";

	{
		outputName.setHidden(false);

		precision = new IntegerInput("Precision", "Key Inputs", 0);
		precision.setValidRange(0, Integer.MAX_VALUE);
		this.addInput(precision, true);

		failureText = new StringInput("FailureText", "Key Inputs", "");
		this.addInput(failureText, true);
	}

	// TODO: validate the output list is sane (check types of outputs)

	@Override
	public void updateForInput( Input<?> in ) {
		super.updateForInput(in);
		if (in == precision) {
			doubleFormat = String.format("%%.%df", precision.getValue());
		}
	}

	@Override
	public String getRenderText(double simTime) {

		if( outputName.getValue() == null )
			return formatText.getValue();

		OutputHandle out = outputName.getOutputHandle(simTime);
		if( out == null )
			return "Invalid entry for keyword OutputName";
		String ret = out.getValueAsString(simTime, 1.0, doubleFormat);
		if( ret == null )
			return "Invalid entry for keyword Format";
		return ret;
	}
}
