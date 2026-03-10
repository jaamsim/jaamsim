/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2026 JaamSim Software Inc.
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
package com.jaamsim.Vec3dProviders;

import java.util.ArrayList;

import com.jaamsim.basicsim.Entity;
import com.jaamsim.basicsim.ErrorException;
import com.jaamsim.input.ExpError;
import com.jaamsim.input.ExpEvaluator;
import com.jaamsim.input.ExpParser;
import com.jaamsim.input.ExpResType;
import com.jaamsim.input.ExpResult;
import com.jaamsim.input.Input;
import com.jaamsim.math.Vec3d;
import com.jaamsim.units.DimensionlessUnit;
import com.jaamsim.units.Unit;

public class Vec3dProvExpression implements Vec3dProvider {
	private final ExpParser.Expression exp;
	private final Class<? extends Unit> unitType;
	private final ExpEvaluator.EntityParseContext parseContext;

	public Vec3dProvExpression(String expString, Entity thisEnt, Class<? extends Unit> ut) throws ExpError {
		unitType = ut;
		parseContext = ExpEvaluator.getParseContext(thisEnt, expString);
		exp = ExpParser.parseExpression(parseContext, expString);
		ExpParser.assertResultType(exp, ExpResType.COLLECTION);
	}

	@Override
	public Vec3d getNextVec3d(Entity thisEnt, double simTime) {
		try {
			ExpResult result = ExpEvaluator.evaluateExpression(exp, thisEnt, simTime);

			if (result.type != ExpResType.COLLECTION)
				throw new ExpError(exp.source, 0, Input.EXP_ERR_RESULT_TYPE,
						result.type, "COLLECTION");

			ExpResult.Collection col = result.colVal;
			if (col.getSize() != 3 ) {
				String colStr = col.getOutputString(thisEnt.getJaamSimModel());
				throw new ExpError(exp.source, 0, Input.INP_ERR_BADVEC3D, colStr);
			}

			ExpResult xRes = col.index(ExpResult.makeNumResult(1, DimensionlessUnit.class));
			ExpResult yRes = col.index(ExpResult.makeNumResult(2, DimensionlessUnit.class));
			ExpResult zRes = col.index(ExpResult.makeNumResult(3, DimensionlessUnit.class));

			if (xRes.type != ExpResType.NUMBER)
				throw new ExpError(exp.source, 0, Input.EXP_ERR_RESULT_TYPE, xRes.type, "NUMBER");
			if (yRes.type != ExpResType.NUMBER)
				throw new ExpError(exp.source, 0, Input.EXP_ERR_RESULT_TYPE, yRes.type, "NUMBER");
			if (zRes.type != ExpResType.NUMBER)
				throw new ExpError(exp.source, 0, Input.EXP_ERR_RESULT_TYPE, zRes.type, "NUMBER");

			if (xRes.unitType != unitType)
				throw new ExpError(exp.source, 0, Input.EXP_ERR_UNIT,
						thisEnt.getJaamSimModel().getObjectTypeForClass(xRes.unitType),
						thisEnt.getJaamSimModel().getObjectTypeForClass(unitType));
			if (yRes.unitType != unitType)
				throw new ExpError(exp.source, 0, Input.EXP_ERR_UNIT,
						thisEnt.getJaamSimModel().getObjectTypeForClass(yRes.unitType),
						thisEnt.getJaamSimModel().getObjectTypeForClass(unitType));
			if (zRes.unitType != unitType)
				throw new ExpError(exp.source, 0, Input.EXP_ERR_UNIT,
						thisEnt.getJaamSimModel().getObjectTypeForClass(zRes.unitType),
						thisEnt.getJaamSimModel().getObjectTypeForClass(unitType));

			return new Vec3d(xRes.value, yRes.value, zRes.value);
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
