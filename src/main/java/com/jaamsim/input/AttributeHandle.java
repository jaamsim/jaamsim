/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2014 Ausenco Engineering Canada Inc.
 * Copyright (C) 2021-2024 JaamSim Software Inc.
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

public class AttributeHandle extends ValueHandle {
	private final String attributeName;
	private final ExpResult initialValue;
	private ExpResult value;
	private final Class<? extends Unit> unitType;

	public AttributeHandle(Entity e, String name, ExpResult initVal, ExpResult val, Class<? extends Unit> ut) {
		super(e);
		attributeName = name;
		initialValue = initVal;
		value = val;
		unitType = ut;
	}

	@Override
	public Class<? extends Unit> getUnitType() {
		return unitType;
	}

	public ExpResult getInitialValue() {
		return initialValue.getCopy();
	}

	public void setValue(ExpResult val) {
		value = val;
	}

	@Override
	public <T> T getValue(double simTime, Class<T> klass) {
		return getValue(klass);
	}

	public <T> T getValue(Class<T> klass) {
		if (value == null) {
			return null;
		}
		return value.getValue(klass);
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
