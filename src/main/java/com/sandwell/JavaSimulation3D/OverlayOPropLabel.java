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

import com.sandwell.JavaSimulation.Entity;
import com.sandwell.JavaSimulation.Input;
import com.sandwell.JavaSimulation.IntegerInput;
import com.sandwell.JavaSimulation.Keyword;
import com.sandwell.JavaSimulation.StringListInput;
import com.sandwell.JavaSimulation.StringVector;

public class OverlayOPropLabel extends OverlayTextLabel {

	@Keyword(desc = "The name of a root entity, and then an output value chain. If more than one output value is " +
	         "given, all outputs but the last should point to an entity output to query for the next output. The " +
	         "example gets the name of the brand in a tank",
	         example = "Label Output { Tank1 Product Name }")
	private final StringListInput outputValue;

	@Keyword(desc = "The number of decimal places displayed by the label when displaying floating point values.",
	         example = "Label Precision { 1 }")
	private final IntegerInput precision;

	private String doubleFormat = "%.0f";

	{
		outputValue = new StringListInput("OutputName", "Variable Text", null);
		this.addInput(outputValue, true);

		precision = new IntegerInput("Precision", "Variable Text", 0);
		precision.setValidRange(0, Integer.MAX_VALUE);
		this.addInput(precision, true);

	}

	@Override
	public void updateForInput( Input<?> in ) {
		super.updateForInput(in);
		if (in == precision) {
			doubleFormat = String.format("%%.%df", precision.getValue());
		}
	}
	@Override
	public String getText(double simTime) {

		StringVector outputs = outputValue.getValue();
		if (outputs.size() < 2) {
			return "";
		}
		Entity ent = Entity.getNamedEntity(outputs.get(0));

		// For any intermediate values (not the first or last), follow the entity-output chain
		for (int i = 1; i < outputs.size() - 1; ++i) {
			String outputName = outputs.get(i);
			if (ent == null || !ent.hasOutput(outputName, true)) {
				return "";
			}
			ent = ent.getOutputValue(outputName, simTime, Entity.class);
		}

		// Now get the last output, and take it's value from the current entity
		String name = outputs.get(outputs.size() - 1);

		if (ent == null || !ent.hasOutput(name, true)) {
			return "";
		}

		Class<?> retType = ent.getOutputType(name);
		if (retType == Double.class ||
		    retType == double.class) {
			double val = 0;
			if (retType == Double.class) {
				val = ent.getOutputValue(name, simTime, Double.class);
			} else {
				val = ent.getOutputValue(name, simTime, double.class);
			}
			return String.format(doubleFormat, val);
		}

		String val = ent.getOutputAsString(name, simTime);

		if (val == null) {
			return "";
		}
		return val;
	}

}
