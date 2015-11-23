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
package com.jaamsim.Samples;

import com.jaamsim.basicsim.Entity;
import com.jaamsim.basicsim.ObjectType;
import com.jaamsim.input.ExpError;
import com.jaamsim.input.ExpEvaluator;
import com.jaamsim.input.ExpParser;
import com.jaamsim.input.ExpResult;
import com.jaamsim.units.Unit;

public class SampleExpression implements SampleProvider {
	private final ExpParser.Expression exp;
	private final Entity thisEnt;
	private final Class<? extends Unit> unitType;

	public SampleExpression(ExpParser.Expression e, Entity ent, Class<? extends Unit> ut) {
		exp = e;
		thisEnt = ent;
		unitType = ut;
	}

	@Override
	public Class<? extends Unit> getUnitType() {
		return unitType;
	}

	@Override
	public double getNextSample(double simTime) {
		double ret = 0.0;
		try {
			ExpResult res = ExpEvaluator.evaluateExpression(exp, simTime, thisEnt);
			if (res.unitType != unitType)
				thisEnt.error("Invalid unit returned by an expression: '%s'%n"
						+ "Received: %s, expected: %s",
						exp, ObjectType.getObjectTypeForClass(res.unitType),
						ObjectType.getObjectTypeForClass(unitType));

			ret = res.value;
		}
		catch(ExpError e) {
			thisEnt.error("%s", e.getMessage());
		}
		return ret;
	}

	@Override
	public double getMeanValue(double simTime) {
		return 0;
	}

	@Override
	public double getMinValue() {
		return Double.NEGATIVE_INFINITY;
	}

	@Override
	public double getMaxValue() {
		return Double.POSITIVE_INFINITY;
	}

	@Override
	public String toString() {
		return exp.toString();
	}

}
