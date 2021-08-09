/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2010-2011 Ausenco Engineering Canada Inc.
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
package com.jaamsim.input;

import java.util.ArrayList;
import java.util.Collections;

import com.jaamsim.Graphics.DisplayEntity;
import com.jaamsim.basicsim.Entity;

public class EntityInput<T extends Entity> extends Input<T> {

	private Class<T> entClass;
	private Class<? extends T> entSubClass;  // a particular sub-class that can be set at runtime
	private boolean includeSubclasses;  // flag to determine if subclasses are valid
	private ArrayList<Class<? extends Entity>> invalidClasses; // list of invalid classes (including subclasses).  if empty, then all classes are valid

	public EntityInput(Class<T> aClass, String key, String cat, T def) {
		super(key, cat, def);
		entClass = aClass;
		entSubClass = aClass;
		includeSubclasses = true;
		invalidClasses = new ArrayList<>();
	}

	public void setSubClass(Class<? extends T> aClass) {
		if (aClass != entSubClass)
			this.reset();
		entSubClass = aClass;
	}

	@Override
	public void parse(Entity thisEnt, KeywordIndex kw)
	throws InputErrorException {
		Input.assertCount(kw, 1);
		T tmp = Input.parseEntity(thisEnt.getJaamSimModel(), kw.getArg(0), entClass);
		if (!isValid(tmp))
			throw new InputErrorException("%s is not a valid entity", tmp.getName());
		value = tmp;
	}

	@Override
	public String getValidInputDesc() {
		if (entClass == DisplayEntity.class) {
			return Input.VALID_ENTITY;
		}
		return String.format(Input.VALID_ENTITY_TYPE, entClass.getSimpleName());
	}

	@Override
	public ArrayList<String> getValidOptions(Entity ent) {
		ArrayList<String> list = new ArrayList<>();
		if (entSubClass == null)
			return list;

		for (T each: ent.getJaamSimModel().getClonesOfIterator(entSubClass)) {
			if (!each.isRegistered())
				continue;

			if (!isValid(each))
				continue;

			list.add(each.getName());
		}
		Collections.sort(list, Input.uiSortOrder);
		return list;
	}

	@Override
	public void getValueTokens(ArrayList<String> toks) {
		if (value == null || isDefault())
			return;

		toks.add(value.getName());
	}

	private boolean isValid(T ent) {
		if(! includeSubclasses) {
			if( ent.getClass() != entClass ) {
				return false;
			}
		}

		for( Class<? extends Entity> c : invalidClasses ) {
			if( c.isAssignableFrom( ent.getClass() ) ) {
				return false;
			}
		}

		return true;
	}

	public void setIncludeSubclasses(boolean bool) {
		this.includeSubclasses = bool;
	}

	public void addInvalidClass(Class<? extends Entity> aClass ) {
		invalidClasses.add(aClass);
	}

	@Override
	public boolean removeReferences(Entity ent) {
		if (value == ent) {
			this.reset();
			return true;
		}
		return false;
	}

	@Override
	public void appendEntityReferences(ArrayList<Entity> list) {
		if (value == null || list.contains(value))
			return;
		list.add(value);
	}

}
