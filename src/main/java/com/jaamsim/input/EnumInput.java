/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2012 Ausenco Engineering Canada Inc.
 * Copyright (C) 2023-2026 JaamSim Software Inc.
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


public class EnumInput<T extends Enum<T>> extends Input<T> {
	private final Class<T> type;

	public EnumInput(Class<T> atype, String key, String cat, T def) {
		super(key, cat, def);
		type = atype;
	}

	@Override
	public void parse(Entity thisEnt, KeywordIndex kw)
	throws InputErrorException {
		Input.assertCount(kw, 1);
		value = Input.parseEnum(type, kw.getArg(0));
	}

	@Override
	public ArrayList<String> getValidOptions(Entity ent) {
		ArrayList<String> tmp = new ArrayList<>();
		for (T each : type.getEnumConstants())
			tmp.add(each.name());
		return tmp;
	}

	@Override
	public String[] getExamples() {
		String[] ret = new String[1];
		T[] array = type.getEnumConstants();
		if (array.length > 0)
			ret[0] = array[0].name();
		return ret;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <V> V getValue(Entity thisEnt, double simTime, Class<V> klass) {
		if (getValue() == null)
			return (V) "";
		return (V) getValue().toString();
	}

	@Override
	public Class<?> getReturnType() {
		return String.class;
	}

}
