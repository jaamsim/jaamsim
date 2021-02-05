/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2018-2021 JaamSim Software Inc.
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
package com.jaamsim.SubModels;

import com.jaamsim.units.Unit;

public class PassThroughData {

	private final String name;
	private final Class<? extends Unit> unitType;

	public PassThroughData(String nm, Class<? extends Unit> ut) {
		name = nm;
		unitType = ut;
	}

	public String getName() {
		return name;
	}

	public Class<? extends Unit> getUnitType() {
		return unitType;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null)
			return false;

		if (!PassThroughData.class.isAssignableFrom(obj.getClass()))
			return false;

		PassThroughData data = (PassThroughData) obj;
		return name.equals(data.name) && unitType.equals(data.unitType);
	}

	@Override
	public String toString() {
		return String.format("(%s, %s)", name, unitType.getSimpleName());
	}

}
