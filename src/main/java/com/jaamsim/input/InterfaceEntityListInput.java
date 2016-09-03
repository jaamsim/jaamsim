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

import java.util.ArrayList;
import java.util.Collections;

import com.jaamsim.basicsim.Entity;

public class InterfaceEntityListInput<T> extends ListInput<ArrayList<T>> {
	private Class<T> entClass;
	private boolean unique; // flag to determine if list must be unique or not
	private boolean even;  // flag to determine if there must be an even number of entries

	public InterfaceEntityListInput(Class<T> aClass, String key, String cat, ArrayList<T> def) {
		super(key, cat, def);
		entClass = aClass;
		unique = true;
		even = false;
	}

	@Override
	public void parse(KeywordIndex kw)
	throws InputErrorException {
		Input.assertCountRange(kw, minCount, maxCount);
		if( even )
			Input.assertCountEven(kw);

		value = Input.parseInterfaceEntityList(kw, entClass, unique);
	}

	@Override
	public int getListSize() {
		if (value == null)
			return 0;
		else
			return value.size();
	}

	public void setUnique(boolean unique) {
		this.unique = unique;
	}
	public void setEven(boolean bool) {
		this.even = bool;
	}

	@Override
	public ArrayList<String> getValidOptions() {
		ArrayList<String> list = new ArrayList<>();
		for(Entity each: Entity.getClonesOfIterator(Entity.class, entClass) ) {
			if(each.testFlag(Entity.FLAG_GENERATED))
				continue;

			list.add(each.getName());
		}
		Collections.sort(list, Input.uiSortOrder);
		return list;
	}

	private String getInputString(ArrayList<T> val) {

		if (val.size() == 0)
			return "";

		StringBuilder tmp = new StringBuilder();
		tmp.append(((Entity)val.get(0)).getName());
		for (int i = 1; i < val.size(); i++) {
			tmp.append(SEPARATOR);
			tmp.append(((Entity)val.get(i)).getName());
		}
		return tmp.toString();
	}


	@Override
	public String getDefaultString() {
		if (defValue == null || defValue.size() == 0)
			return "";
		return this.getInputString(defValue);
	}

	@Override
	public void getValueTokens(ArrayList<String> toks) {
		if (value == null) return;

		for (int i = 0; i < value.size(); i++) {
			toks.add(((Entity)value.get(i)).getName());
		}
	}

	@Override
	public void removeReferences(Entity ent) {
		if (value == null)
			return;
		value.removeAll(Collections.singleton(ent));
	}

}
