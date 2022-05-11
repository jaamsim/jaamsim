/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2016-2022 JaamSim Software Inc.
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
import com.jaamsim.basicsim.ErrorException;
import com.jaamsim.input.ExpParser.Expression;
import com.jaamsim.units.Unit;

public class ExpressionHandle extends ValueHandle {

	private final Expression exp;
	private final String name;
	private final Class<? extends Unit> unitType;

	public ExpressionHandle(Entity ent, Expression exp, String name, Class<? extends Unit> unitType) {
		super(ent);
		this.exp = exp;
		this.name = name;
		this.unitType = unitType;
	}

	@Override
	public Class<? extends Unit> getUnitType() {
		return unitType;
	}

	@Override
	public <T> T getValue(double simTime, Class<T> klass) {
		// Make a best effort to return the type
		ExpResult res = evaluateExp(simTime);

		return res.getValue(klass);
	}

	@Override
	public double getValueAsDouble(double simTime, double def) {
		ExpResult res = evaluateExp(simTime);
		if (res.type == ExpResType.NUMBER)
			return res.value;

		return def;
	}

	private ExpResult evaluateExp(double simTime) {
		try {
			ExpResult er = ExpEvaluator.evaluateExpression(exp, ent, simTime);
			if (er.type == ExpResType.NUMBER && er.unitType != unitType) {
				throw new ExpError(exp.source, 0, Input.EXP_ERR_UNIT,
						er.unitType.getSimpleName(), unitType.getSimpleName());
			}
			return er;
		}
		catch (ExpError e) {
			throw new ErrorException(ent, e);
		}
	}

	@Override
	public Class<?> getReturnType() {
		return ExpResult.class;
	}

	@Override
	public String getDescription() {
		return "User defined attribute";
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public boolean isReportable() {
		return true;
	}

	@Override
	public int getSequence() {
		return Integer.MAX_VALUE;
	}
	@Override
	public boolean canCache() {
		return false;
	}

}

