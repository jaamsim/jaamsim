/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2015 Ausenco Engineering Canada Inc.
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
import java.util.HashMap;

import com.jaamsim.basicsim.Entity;

public class StringKeyInput<T extends Entity> extends Input<HashMap<String,T>> {

	private Class<T> entClass;

	public StringKeyInput(Class<T> klass, String keyword, String cat) {
		super(keyword, cat, null);
		entClass = klass;
	}

	@Override
	public void parse(Entity thisEnt, KeywordIndex kw) throws InputErrorException {
		HashMap<String,T> hashMap = new HashMap<>();
		ArrayList<KeywordIndex> subArgs = kw.getSubArgs();
		for (int i = 0; i < subArgs.size(); i++) {
			KeywordIndex subArg = subArgs.get(i);
			Input.assertCount(subArg, 2);
			T ent = Input.tryParseEntity(subArg.getArg(1), entClass );
			hashMap.put(subArg.getArg(0), ent);
		}
		value = hashMap;
	}

	public T getValueFor(String str) {
		if (value == null)
			return null;
		return value.get(str);
	}

}
