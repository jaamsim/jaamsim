/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2021 JaamSim Software Inc.
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

public abstract class ValueHandle {
	public final Entity ent;

	ValueHandle(Entity e) {
		ent = e;
	}

	abstract public <T> T getValue(double simTime, Class<T> klass);

	abstract public double getValueAsDouble(double simTime, double def);

	abstract public Class<? extends Unit> getUnitType();

	abstract public Class<?> getReturnType();

	abstract public String getDescription();

	abstract public String getName();

	abstract public boolean isReportable();

	abstract public int getSequence();

	abstract public boolean canCache();

	public Class<?> getDeclaringClass() {
		return ent.getClass();
	}

	public boolean isNumericValue() {
		return ValueHandle.isNumericType(this.getReturnType());
	}

	public boolean isIntegerValue() {
		return ValueHandle.isIntegerType(this.getReturnType());
	}

	public static boolean isNumericType(Class<?> rtype) {

		if (rtype == double.class) return true;
		if (rtype == int.class) return true;
		if (rtype == long.class) return true;
		if (rtype == float.class) return true;
		if (rtype == short.class) return true;
		if (rtype == char.class) return true;

		if (rtype == Double.class) return true;
		if (rtype == Integer.class) return true;
		if (rtype == Long.class) return true;
		if (rtype == Float.class) return true;
		if (rtype == Short.class) return true;
		if (rtype == Character.class) return true;

		return false;
	}

	public static boolean isIntegerType(Class<?> rtype) {

		if (rtype == int.class) return true;
		if (rtype == long.class) return true;

		if (rtype == Integer.class) return true;
		if (rtype == Long.class) return true;

		return false;
	}
}
