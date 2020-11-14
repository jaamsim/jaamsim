/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2016-2019 JaamSim Software Inc.
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
package com.jaamsim.StringProviders;

import com.jaamsim.basicsim.Entity;
import com.jaamsim.basicsim.ErrorException;
import com.jaamsim.basicsim.JaamSimModel;
import com.jaamsim.input.ExpError;
import com.jaamsim.input.ExpEvaluator;
import com.jaamsim.input.ExpParser;
import com.jaamsim.input.ExpResult;
import com.jaamsim.input.ExpParser.Expression;
import com.jaamsim.units.Unit;

public class StringProvExpression implements StringProvider {

	private final Expression exp;
	private final Entity thisEnt;
	private final Class<? extends Unit> unitType;
	private final ExpEvaluator.EntityParseContext parseContext;

	public StringProvExpression(String expString, Entity ent, Class<? extends Unit> ut) throws ExpError {
		thisEnt = ent;
		unitType = ut;
		parseContext = ExpEvaluator.getParseContext(thisEnt, expString);
		exp = ExpParser.parseExpression(parseContext, expString);
	}

	@Override
	public String getNextString(double simTime) {
		return getNextString(simTime, 1.0d, false);
	}

	@Override
	public String getNextString(double simTime, double siFactor) {
		return getNextString(simTime, siFactor, false);
	}

	@Override
	public String getNextString(double simTime, double siFactor, boolean integerValue) {
		String ret = "";
		try {
			ExpResult result = ExpEvaluator.evaluateExpression(exp, simTime);
			switch (result.type) {
			case STRING:
				ret = result.stringVal;
				break;
			case ENTITY:
				ret = "null";
				if (result.entVal != null)
					ret = result.entVal.getName();
				break;
			case NUMBER:
				if (result.unitType != unitType) {
					ret = result.toString();
					break;
				}
				if (integerValue) {
					ret = Double.toString((int)(result.value/siFactor));
				}
				else {
					ret = Double.toString(result.value/siFactor);
				}
				break;
			case COLLECTION:
				JaamSimModel simModel = thisEnt.getJaamSimModel();
				ret = result.colVal.getOutputString(simModel);
				break;
			default:
				assert(false);
				ret = "???";
				break;
			}
		}
		catch(ExpError e) {
			throw new ErrorException(thisEnt, e);
		}
		return ret;
	}

	@Override
	public String getNextString(double simTime, String fmt, double siFactor) {
		String ret = "";
		try {
			ExpResult result = ExpEvaluator.evaluateExpression(exp, simTime);
			switch (result.type) {
			case STRING:
				ret = String.format(fmt, result.stringVal);
				break;
			case ENTITY:
				ret = String.format(fmt, result.entVal.getName());
				break;
			case NUMBER:
				if (result.unitType != unitType) {
					thisEnt.error("Invalid unit returned by an expression: '%s'%n"
							+ "Received: %s, expected: %s",
							exp, thisEnt.getJaamSimModel().getObjectTypeForClass(result.unitType),
							thisEnt.getJaamSimModel().getObjectTypeForClass(unitType));
				}
				ret = String.format(fmt, result.value/siFactor);
				break;
			case COLLECTION:
				JaamSimModel simModel = thisEnt.getJaamSimModel();
				ret = String.format(fmt, result.colVal.getOutputString(simModel));
				break;
			default:
				assert(false);
				ret = String.format(fmt, "???");
				break;
			}
		}
		catch(ExpError e) {
			throw new ErrorException(thisEnt, e);
		}
		return ret;
	}

	@Override
	public String toString() {
		return parseContext.getUpdatedSource();
	}

}
