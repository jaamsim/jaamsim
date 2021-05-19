/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2017-2021 JaamSim Software Inc.
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
package com.jaamsim.EntityProviders;

import java.util.ArrayList;

import com.jaamsim.basicsim.Entity;
import com.jaamsim.basicsim.ErrorException;
import com.jaamsim.input.ExpError;
import com.jaamsim.input.ExpEvaluator;
import com.jaamsim.input.ExpParser;
import com.jaamsim.input.ExpParser.Expression;
import com.jaamsim.input.ExpResType;
import com.jaamsim.input.ExpResult;

public class EntityProvExpression<T extends Entity> implements EntityProvider<T> {

	private final Expression exp;
	private final Entity thisEnt;
	private final ExpEvaluator.EntityParseContext parseContext;
	private final Class<T> entClass;

	private final static String BAD_RESULT_TYPE = "Incorrect result type returned by expression: '%s'%n"
			+ "Received: %s, expected: %s";
	private final static String BAD_RESULT_CLASS = "Incorrect class returned by expression: '%s'%n"
			+ "Received: %s, expected: %s";

	public EntityProvExpression(String expString, Entity ent, Class<T> aClass) throws ExpError {
		thisEnt = ent;
		parseContext = ExpEvaluator.getParseContext(thisEnt, expString);
		exp = ExpParser.parseExpression(parseContext, expString);
		ExpParser.assertResultType(exp, ExpResType.ENTITY);
		entClass = aClass;
	}

	@SuppressWarnings("unchecked")
	@Override
	public T getNextEntity(double simTime) {
		T ret = null;
		try {
			ExpResult result = ExpEvaluator.evaluateExpression(exp, simTime);

			if (result.type != ExpResType.ENTITY) {
				thisEnt.error(BAD_RESULT_TYPE, exp.source, result.type, "ENTITY");
			}

			if (!entClass.isAssignableFrom(result.entVal.getClass())) {
				thisEnt.error(BAD_RESULT_CLASS, exp.source,
						result.entVal.getClass().getSimpleName(), entClass.getSimpleName());
			}

			ret = (T)result.entVal;
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
