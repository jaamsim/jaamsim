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
import com.sandwell.JavaSimulation.EntityInput;
import com.sandwell.JavaSimulation.Input;
import com.sandwell.JavaSimulation.IntegerInput;
import com.sandwell.JavaSimulation.Keyword;
import com.sandwell.JavaSimulation.StringInput;

public class OutputPropertyLabel extends TextLabel {

	@Keyword(desc = "The name of the output to display",
	         example = "Label OutputName { 'Contents' }")
	private final StringInput outputName;

	@Keyword(desc = "The entity to read the output from",
	         example = "Label Entity { StockPile2 }")
	private final EntityInput<Entity> entity;

	@Keyword(desc = "The number of decimal places displayed by the label when displaying floating point values.",
	         example = "Label Precision { 1 }")
	private final IntegerInput precision;

	private String doubleFormat = "%.0f";

	{
		entity = new EntityInput<Entity>(Entity.class, "Entity", "Variable Text", null);
		this.addInput(entity, true);

		outputName = new StringInput("OutputName", "Variable Text", null);
		this.addInput(outputName, true);

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
	public String getRenderText(double simTime) {

		Entity ent = entity.getValue();
		String name = outputName.getValue();

		if (name == null || ent == null || !ent.hasOutput(name, true)) {
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

		String val = entity.getValue().getOutputAsString(outputName.getValue(), simTime);

		if (val == null) {
			return "";
		}
		return val;
	}
}
