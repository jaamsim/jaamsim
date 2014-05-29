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
package com.jaamsim.input;

import com.jaamsim.input.ExpParser.Expression;
import com.sandwell.JavaSimulation.Entity;
import com.sandwell.JavaSimulation.InputErrorException;
import com.sandwell.JavaSimulation.StringVector;

public class ExpressionInput extends Input<ExpParser.Expression> {
	private Entity thisEnt;

	public ExpressionInput(String key, String cat, ExpParser.Expression def) {
		super(key, cat, def);
	}

	public void setEntity(Entity ent) {
		thisEnt = ent;
	}

	@Override
	public void parse(StringVector input)
	throws InputErrorException {

		try {
			Input.assertCount(input, 1);
			Expression exp = ExpParser.parseExpression(input.get(0));

			// Test whether the expression can be evaluated
			try {
				@SuppressWarnings("unused")
				double x = ExpEvaluator.evaluateExpression(exp, 0.0, thisEnt, null).value;
			} catch (ExpEvaluator.Error e) {
				throw new InputErrorException(e.toString());
			}

			// Save the expression
			value = exp;

		} catch (ExpParser.Error e) {
			throw new InputErrorException(e.toString());
		}
	}

}
