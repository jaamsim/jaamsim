/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2016 KMA Technologies
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

public class ExpressionHandle extends OutputHandle {

	private final Expression exp;
	private final String name;

	public ExpressionHandle(Entity ent, Expression exp, String name) {
		super(ent);
		this.exp = exp;
		this.name = name;
	}

	@Override
	public <T> T getValue(double simTime, Class<T> klass) {
		if (!double.class.equals(klass) || Double.class.equals(klass)) {
			return null;
		}
		return klass.cast(evaluateExp(simTime));
	}

	@Override
	public double getValueAsDouble(double simTime, double def) {
		return evaluateExp(simTime);
	}

	private double evaluateExp(double simTime) {
		try {
			ExpResult er = ExpEvaluator.evaluateExpression(exp, simTime);
			if (er.unitType != unitType) {
				throw new ErrorException(String.format("Unit Type mismatch in custom output. Entity: %s Expression: '%s' Exected %s, got %s.",
						ent.getName(), exp.source, unitType.getSimpleName(), er.unitType.getSimpleName()));
			}
			return er.value;
		}
		catch (ExpError ex) {
			throw new ErrorException(ex);
		}
	}

	@Override
	public Class<?> getReturnType() {
		return double.class;
	}

	@Override
	public Class<?> getDeclaringClass() {
		return Entity.class;
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

