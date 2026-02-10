/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2014 Ausenco Engineering Canada Inc.
 * Copyright (C) 2021-2025 JaamSim Software Inc.
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

public class AttributeHandle extends ValueHandle {
	private final String attributeName;
	private final Expression expression;
	private ExpResult value;
	private Class<? extends Unit> unitType;

	public AttributeHandle(Entity e, String name, Expression exp, ExpResult val, Class<? extends Unit> ut) {
		super(e);
		attributeName = name;
		expression = exp;
		value = val;
		unitType = ut;
	}

	@Override
	public Class<? extends Unit> getUnitType() {
		if (value == null) {
			try {
				ExpResult res = ExpEvaluator.evaluateExpression(expression, ent, 0.0d);
				return res.unitType;
			}
			catch (ExpError e) {
				throw new ErrorException(ent, e);
			}
		}
		return unitType;
	}

	public Expression getExpression() {
		return expression;
	}

	public void setValue(ExpResult val) {
		value = val;
		unitType = val.unitType;
	}

	@Override
	public <T> T getValue(double simTime, Class<T> klass) {
		if (value == null) {
			try {
				ExpResult res = ExpEvaluator.evaluateExpression(expression, ent, simTime);
				return res.getValue(klass);
			}
			catch (ExpError e) {
				throw new ErrorException(ent, e);
			}
		}
		return value.getValue(klass);
	}

	public <T> T getValue(Class<T> klass) {
		return getValue(0.0d, klass);
	}

	public ExpResult copyValue() {
		if (value == null) {
			return null;
		}
		return value.getCopy();
	}

	@Override
	public double getValueAsDouble(double simTime, double def) {
		if (value.type == ExpResType.NUMBER)
			return value.value;
		else
			return def;
	}

	@Override
	public Class<?> getReturnType() {
		return ExpResult.class;
	}
	@Override
	public String getDescription() {
		return String.format("Value for the user-defined attribute '%s'.", attributeName);
	}

	@Override
	public String getTitle() {
		return "User-Defined Attributes";
	}

	@Override
	public String getName() {
		return attributeName;
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
