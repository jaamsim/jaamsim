/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2016-2020 JaamSim Software Inc.
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
import com.jaamsim.basicsim.JaamSimModel;

public class InterfaceEntityInput<T> extends Input<T> {

	private Class<T> entClass;

	public InterfaceEntityInput(Class<T> aClass, String key, String cat, T def) {
		super(key, cat, def);
		entClass = aClass;
	}

	@Override
	public void parse(Entity thisEnt, KeywordIndex kw) throws InputErrorException {
		Input.assertCount(kw, 1);
		T tmp = Input.parseInterfaceEntity(thisEnt.getJaamSimModel(), kw.getArg(0), entClass);
		value = tmp;
	}

	@Override
	public String getValidInputDesc() {
		return String.format(Input.VALID_INTERFACE_ENTITY, entClass.getSimpleName());
	}

	@Override
	public ArrayList<String> getValidOptions(Entity ent) {
		ArrayList<String> list = new ArrayList<>();
		JaamSimModel simModel = ent.getJaamSimModel();
		for (Entity each: simModel.getClonesOfIterator(Entity.class, entClass)) {
			if (!each.isRegistered())
				continue;

			list.add(each.getName());
		}
		Collections.sort(list, Input.uiSortOrder);
		return list;
	}

	@Override
	public void getValueTokens(ArrayList<String> toks) {
		if (value == null) return;

		toks.add(value.toString());
	}

	@Override
	public boolean removeReferences(Entity ent) {
		if (value == ent) {
			this.reset();
			return true;
		}
		return false;
	}

}
