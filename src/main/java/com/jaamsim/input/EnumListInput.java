/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2017 JaamSim Software Inc.
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

public class EnumListInput<T extends Enum<T>> extends ListInput<ArrayList<T>> {

	private final Class<T> type;

	public EnumListInput(Class<T> atype, String key, String cat, ArrayList<T> def) {
		super(key, cat, def);
		type = atype;
	}

	@Override
	public void parse(Entity thisEnt, KeywordIndex kw)
	throws InputErrorException {
		value = Input.parseEnumList(type, kw);
	}

	@Override
	public int getListSize() {
		if (value == null)
			return 0;
		else
			return value.size();
	}

	@Override
	public ArrayList<String> getValidOptions(Entity ent) {
		ArrayList<String> tmp = new ArrayList<>();
		for (T each : type.getEnumConstants())
			tmp.add(each.name());
		return tmp;
	}

}
