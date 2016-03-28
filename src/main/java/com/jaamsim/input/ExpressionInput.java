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
			Expression exp = ExpParser.parseExpression(ExpEvaluator.getParseContext(thisEnt), kw.getArg(0));

			// Save the expression
			value = exp;

		} catch (ExpError e) {
			LogBox.logException(e);
			throw new InputErrorException(e.toString());
		}
	}

}
