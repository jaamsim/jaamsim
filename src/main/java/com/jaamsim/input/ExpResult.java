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
package com.jaamsim.input;

import com.jaamsim.basicsim.Entity;
import com.jaamsim.units.Unit;

public class ExpResult {
	public final ExpResType type;

	public final double value;
	public final Class<? extends Unit> unitType;

	public final String stringVal;
	public final Entity entVal;

	public static ExpResult makeNumResult(double val, Class<? extends Unit> ut) {
		return new ExpResult(ExpResType.NUMBER, val, ut, null, null);
	}

	public static ExpResult makeStringResult(String str) {
		return new ExpResult(ExpResType.STRING, 0, null, str, null);
	}

	public static ExpResult makeEntityResult(Entity ent) {
		return new ExpResult(ExpResType.ENTITY, 0, null, null, ent);
	}

	private ExpResult(ExpResType type, double val, Class<? extends Unit> ut, String str, Entity ent) {
		this.type = type;
		value = val;
		unitType = ut;

		stringVal = str;
		entVal = ent;
	}
}
