/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2023 JaamSim Software Inc.
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
package com.jaamsim.ColourProviders;

import java.util.ArrayList;

import com.jaamsim.basicsim.Entity;
import com.jaamsim.basicsim.ErrorException;
import com.jaamsim.input.ColourInput;
import com.jaamsim.input.ExpError;
import com.jaamsim.input.ExpEvaluator;
import com.jaamsim.input.ExpParser;
import com.jaamsim.input.ExpResType;
import com.jaamsim.input.ExpResult;
import com.jaamsim.input.Input;
import com.jaamsim.math.Color4d;
import com.jaamsim.units.DimensionlessUnit;

public class ColourProvExpression implements ColourProvider {
	private final ExpParser.Expression exp;
	private final ExpEvaluator.EntityParseContext parseContext;

	public ColourProvExpression(String expString, Entity thisEnt) throws ExpError {
		parseContext = ExpEvaluator.getParseContext(thisEnt, expString);
		exp = ExpParser.parseExpression(parseContext, expString);
	}

	@Override
	public Color4d getNextColour(Entity thisEnt, double simTime) {
		try {
			ExpResult result = ExpEvaluator.evaluateExpression(exp, thisEnt, simTime);

			if (result.type == ExpResType.STRING) {
				Color4d ret = ColourInput.getColorWithName(result.stringVal);
				if (ret == null)
					throw new ExpError(exp.source, 0, Input.INP_ERR_BADCOLOUR, result.stringVal);
				return ret;
			}

			else if (result.type == ExpResType.COLLECTION) {
				ExpResult.Collection col = result.colVal;
				if (col.getSize() < 3 || col.getSize() > 4) {
					String colStr = col.getOutputString(thisEnt.getJaamSimModel());
					throw new ExpError(exp.source, 0, Input.INP_ERR_BADCOLOUR, colStr);
				}

				double r = col.index(ExpResult.makeNumResult(1, DimensionlessUnit.class)).value;
				double g = col.index(ExpResult.makeNumResult(2, DimensionlessUnit.class)).value;
				double b = col.index(ExpResult.makeNumResult(3, DimensionlessUnit.class)).value;
				if (r > 1.0f || g > 1.0f || b > 1.0f) {
					r /= 255.0d;
					g /= 255.0d;
					b /= 255.0d;
				}

				double a = 1.0d;
				if (col.getSize() == 4) {
					a = col.index(ExpResult.makeNumResult(4, DimensionlessUnit.class)).value;
					if (a > 1.0f)
						a /= 255.0d;
				}

				return new Color4d(r, g, b, a);
			}

			else {
				throw new ExpError(exp.source, 0, Input.EXP_ERR_RESULT_TYPE,
						result.type, "STRING or COLLECTION");
			}
		}
		catch (ExpError e) {
			throw new ErrorException(thisEnt, e);
		}
	}

	@Override
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
