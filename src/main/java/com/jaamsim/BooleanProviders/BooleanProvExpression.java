/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2022-2023 JaamSim Software Inc.
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
package com.jaamsim.BooleanProviders;

import java.util.ArrayList;

import com.jaamsim.basicsim.Entity;
import com.jaamsim.basicsim.ErrorException;
import com.jaamsim.input.ExpError;
import com.jaamsim.input.ExpEvaluator;
import com.jaamsim.input.ExpParser;
import com.jaamsim.input.ExpResult;
import com.jaamsim.input.Input;
import com.jaamsim.units.DimensionlessUnit;
import com.jaamsim.input.ExpParser.Expression;
import com.jaamsim.input.ExpResType;

public class BooleanProvExpression implements BooleanProvider {

	private final Expression exp;
	private final ExpEvaluator.EntityParseContext parseContext;

	public BooleanProvExpression(String expString, Entity thisEnt) throws ExpError {
		parseContext = ExpEvaluator.getParseContext(thisEnt, expString);
		exp = ExpParser.parseExpression(parseContext, expString);
		ExpParser.assertResultType(exp, ExpResType.NUMBER);
	}

	@Override
	public boolean getNextBoolean(Entity thisEnt, double simTime) {
		boolean ret;
		try {
			ExpResult res = ExpEvaluator.evaluateExpression(exp, thisEnt, simTime);

			if (res.type != ExpResType.NUMBER)
				throw new ExpError(exp.source, 0, Input.EXP_ERR_RESULT_TYPE,
						res.type, ExpResType.NUMBER);

			if (res.unitType != DimensionlessUnit.class)
				throw new ExpError(exp.source, 0, Input.EXP_ERR_UNIT,
						thisEnt.getJaamSimModel().getObjectTypeForClass(res.unitType),
						thisEnt.getJaamSimModel().getObjectTypeForClass(DimensionlessUnit.class));

			ret = res.value != 0;
		}
		catch(ExpError e) {
			throw new ErrorException(thisEnt, e);
		}
		return ret;
	}

	public void appendEntityReferences(ArrayList<Entity> list) {
		try {
			ExpParser.appendEntityReferences(exp, list);
		}
		catch (ExpError e) {}
	}

	@Override
	public String toString() {
		return parseContext.getUpdatedSource();
	}

}
