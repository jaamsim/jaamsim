/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2014 Ausenco Engineering Canada Inc.
 * Copyright (C) 2018-2026 JaamSim Software Inc.
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

public class InterfaceEntityListInput<T> extends ArrayListInput<T> {
	private Class<T> entClass;
	private boolean unique; // flag to determine if list must be unique or not
	private boolean even;  // flag to determine if there must be an even number of entries
	private boolean includeSelf; // flag to determine whether to include the calling entity in the entityList
	private ArrayList<Class<? extends Entity>> validClasses; // list of valid classes (including subclasses).  if empty, then all classes are valid
	private ArrayList<Class<? extends Entity>> invalidClasses; // list of invalid classes (including subclasses).

	public InterfaceEntityListInput(Class<T> aClass, String key, String cat, ArrayList<T> def) {
		super(key, cat, def);
		entClass = aClass;
		unique = true;
		even = false;
		includeSelf = true;
		validClasses = new ArrayList<>();
		invalidClasses = new ArrayList<>();
	}

	@Override
	public void parse(Entity thisEnt, KeywordIndex kw)
	throws InputErrorException {
		Input.assertCountRange(kw, minCount, maxCount);
		if( even )
			Input.assertCountEven(kw);

		value = Input.parseInterfaceEntityList(thisEnt.getJaamSimModel(), kw, entClass, unique);
	}

	public void setUnique(boolean unique) {
		this.unique = unique;
	}
	public void setEven(boolean bool) {
		this.even = bool;
	}

	public void setIncludeSelf(boolean bool) {
		this.includeSelf = bool;
	}

	@Override
	public ArrayList<String> getValidOptions(Entity ent) {
		ArrayList<String> list = new ArrayList<>();
		JaamSimModel simModel = ent.getJaamSimModel();
		for(Entity each: simModel.getClonesOfIterator(Entity.class, entClass) ) {
			if(!each.isRegistered())
				continue;

			if (!isValidClass(each))
				continue;

			if (each.getEditableInputs().contains(this) && !includeSelf)
				continue;

			list.add(each.getName());
		}
		Collections.sort(list, Input.uiSortOrder);
		return list;
	}

	@Override
	public String getDefaultString(JaamSimModel simModel) {
		if (defValue == null || defValue.isEmpty())
			return "";

		StringBuilder tmp = new StringBuilder();
		tmp.append(((Entity)defValue.get(0)).getName());
		for (int i = 1; i < defValue.size(); i++) {
			tmp.append(SEPARATOR);
			tmp.append(((Entity)defValue.get(i)).getName());
		}
		return tmp.toString();
	}

	public boolean isValidClass(Entity ent) {
		for (Class<? extends Entity> c : invalidClasses) {
			if (c.isAssignableFrom(ent.getClass())) {
				return false;
			}
		}

		if (validClasses.isEmpty())
			return true;
		for (Class<? extends Entity> c : validClasses) {
			if (c.isAssignableFrom(ent.getClass())) {
				return true;
			}
		}
		return false;
	}

	public void addValidClass(Class<? extends Entity> aClass ) {
		invalidClasses.remove(aClass);
		validClasses.add(aClass);
	}

	public void addInvalidClass(Class<? extends Entity> aClass ) {
		validClasses.remove(aClass);
		invalidClasses.add(aClass);
	}

	public void clearValidClasses() {
		validClasses.clear();
		invalidClasses.clear();
	}

	@Override
	public void getValueTokens(ArrayList<String> toks) {
		if (value == null || isDef)
			return;

		for (int i = 0; i < value.size(); i++) {
			toks.add(((Entity)value.get(i)).getName());
		}
	}

	@Override
	public boolean removeReferences(Entity ent) {
		if (value == null)
			return false;
		boolean ret = value.removeAll(Collections.singleton(ent));
		return ret;
	}

	@Override
	public void appendEntityReferences(ArrayList<Entity> list) {
		if (value == null)
			return;
		for (T ent : value) {
			if (ent == null || list.contains(ent))
				continue;
			list.add((Entity) ent);
		}
	}

	@Override
	public Class<?> getReturnType() {
		return ArrayList.class;
	}

}
