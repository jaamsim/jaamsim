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

import com.jaamsim.basicsim.Entity;
import com.jaamsim.input.ExpParser.Expression;
import com.jaamsim.ui.LogBox;

public class ExpressionInput extends Input<ExpParser.Expression> {
	private Entity thisEnt;

	public ExpressionInput(String key, String cat, ExpParser.Expression def) {
		super(key, cat, def);
	}

	public void setEntity(Entity ent) {
		thisEnt = ent;
	}

	@Override
	public void parse(KeywordIndex kw)
	throws InputErrorException {
		Input.assertCount(kw, 1);
		try {
			Expression exp = ExpParser.parseExpression(ExpEvaluator.getParseContext(), kw.getArg(0));

			// Test whether the expression can be evaluated
			ExpValidator.validateExpression(exp, thisEnt, null);

			// Save the expression
			value = exp;

		} catch (ExpError e) {
			LogBox.logException(e);
			throw new InputErrorException(e.toString());
		}
	}

}
