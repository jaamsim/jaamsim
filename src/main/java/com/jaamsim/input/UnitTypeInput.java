/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2013 Ausenco Engineering Canada Inc.
 * Copyright (C) 2019 JaamSim Software Inc.
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

import java.util.ArrayList;

import com.jaamsim.basicsim.Entity;
import com.jaamsim.basicsim.ObjectType;
import com.jaamsim.units.Unit;

public class UnitTypeInput extends Input<ObjectType> {
	private Class<? extends Unit> unitType;
	private Class<? extends Unit> defaultUnitType;

	public UnitTypeInput(String key, String cat, Class<? extends Unit> ut) {
		super(key, cat, null);
		setDefaultValue(ut);
	}

	public void setDefaultValue(Class<? extends Unit> ut) {
		super.setDefaultValue(null);  // getValue is never used
		unitType = ut;
		defaultUnitType = ut;
	}

	@Override
	public void reset() {
		super.reset();
		unitType = defaultUnitType;
	}

	@Override
	public void copyFrom(Entity thisEnt, Input<?> in) {
		super.copyFrom(thisEnt, in);
		UnitTypeInput inp = (UnitTypeInput) in;
		unitType = inp.unitType;
	}

	@Override
	public void parse(Entity thisEnt, KeywordIndex kw)
	throws InputErrorException {
		Input.assertCount(kw, 1);
		ObjectType t = Input.parseEntity(thisEnt.getJaamSimModel(), kw.getArg(0), ObjectType.class);
		Class<? extends Unit> type = Input.checkCast(t.getJavaClass(), Unit.class);

		value = t;
		unitType = type;
	}

	@Override
	public ArrayList<String> getValidOptions(Entity ent) {
		return Unit.getUnitTypeList(ent.getJaamSimModel());
	}

	public Class<? extends Unit> getUnitType() {
		return unitType;
	}

}
