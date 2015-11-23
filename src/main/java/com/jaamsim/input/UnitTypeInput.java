/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2013 Ausenco Engineering Canada Inc.
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
import java.util.Collections;

import com.jaamsim.basicsim.Entity;
import com.jaamsim.basicsim.ObjectType;
import com.jaamsim.units.Unit;

public class UnitTypeInput extends Input<ObjectType> {
	private Class<? extends Unit> unitType;
	private Class<? extends Unit> defaultUnitType;

	public UnitTypeInput(String key, String cat, Class<? extends Unit> ut) {
		super(key, cat, ObjectType.getObjectTypeForClass(ut));
		unitType = ut;
		defaultUnitType = ut;
	}

	public void setDefaultValue(Class<? extends Unit> ut) {
		this.setDefaultValue(ObjectType.getObjectTypeForClass(ut));
		unitType = ut;
		defaultUnitType = ut;
	}

	@Override
	public void reset() {
		super.reset();
		unitType = defaultUnitType;
	}

	@Override
	public void copyFrom(Input<?> in) {
		super.copyFrom(in);
		UnitTypeInput inp = (UnitTypeInput) in;
		unitType = inp.unitType;
	}

	@Override
	public void parse(KeywordIndex kw)
	throws InputErrorException {
		Input.assertCount(kw, 1);
		ObjectType t = Input.parseEntity(kw.getArg(0), ObjectType.class);
		Class<? extends Unit> type = Input.checkCast(t.getJavaClass(), Unit.class);

		value = t;
		unitType = type;
	}

	@Override
	public ArrayList<String> getValidOptions() {
		ArrayList<String> list = new ArrayList<>();
		for (ObjectType each: Entity.getClonesOfIterator(ObjectType.class)) {
			Class<? extends Entity> klass = each.getJavaClass();
			if (klass == null)
				continue;

			if (Unit.class.isAssignableFrom(klass))
				list.add(each.getName());
		}
		Collections.sort(list);
		return list;
	}

	public Class<? extends Unit> getUnitType() {
		return unitType;
	}
}
