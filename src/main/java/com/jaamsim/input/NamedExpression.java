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

import com.jaamsim.input.ExpParser.Expression;
import com.jaamsim.units.Unit;

public class NamedExpression {

	private final String name;
	private final Expression exp;
	private final Class<? extends Unit> unitType;

	public NamedExpression(String name, Expression exp, Class<? extends Unit> unitType) {
		this.name = name;
		this.exp = exp;
		this.unitType = unitType;
	}

	public String getName() {
		return name;
	}
	public Expression getExpression() {
		return exp;
	}

	public Class<? extends Unit> getUnitType() {
		return unitType;
	}
}
