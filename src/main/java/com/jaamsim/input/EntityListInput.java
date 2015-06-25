/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2010-2011 Ausenco Engineering Canada Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 */
package com.jaamsim.input;

import java.util.ArrayList;
import java.util.Collections;

import com.jaamsim.basicsim.Entity;

public class EntityListInput<T extends Entity> extends ListInput<ArrayList<T>> {
	private Class<T> entClass;
	private boolean unique; // flag to determine if list must be unique or not
	private boolean even;  // flag to determine if there must be an even number of entries
	private boolean includeSubclasses;  // flag to determine if subclasses are valid
	private boolean includeSelf; // flag to determine whether to include the calling entity in the entityList
	private ArrayList<Class<? extends Entity>> validClasses; // list of valid classes (including subclasses).  if empty, then all classes are valid

	public EntityListInput(Class<T> aClass, String key, String cat, ArrayList<T> def) {
		super(key, cat, def);
		entClass = aClass;
		unique = true;
		even = false;
		includeSubclasses = true;
		includeSelf = true;
		validClasses = new ArrayList<>();
	}

	@Override
	public void parse(KeywordIndex kw)
	throws InputErrorException {
		Input.assertCountRange(kw, minCount, maxCount);
		if( even )
			Input.assertCountEven(kw);

		value = Input.parseEntityList(kw, entClass, unique);
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
	public void setIncludeSubclasses(boolean bool) {
		this.includeSubclasses = bool;
	}

	public void setIncludeSelf(boolean bool) {
		this.includeSelf = bool;
	}

	public void setValidClasses(ArrayList<Class<? extends Entity>> classes ) {
		validClasses = classes;
	}

	@Override
	public ArrayList<String> getValidOptions() {
		ArrayList<String> list = new ArrayList<>();
		for(T each: Entity.getClonesOfIterator(entClass) ) {
			if(each.testFlag(Entity.FLAG_GENERATED))
				continue;

			if( ! isValidClass( each ))
				continue;

			if(! includeSubclasses) {
				if( each.getClass() != entClass ) {
					continue;
				}
			}

			if(each.getEditableInputs().contains( this ) && ! includeSelf ) {
				continue;
			}

			list.add(each.getName());
		}
		Collections.sort(list);
		return list;
	}

	@Override
	public void getValueTokens(ArrayList<String> toks) {
		if (value == null) return;

		for (int i = 0; i < value.size(); i++)
			toks.add(value.get(i).getName());
	}

	private String getInputString(ArrayList<T> val) {

		if (val.size() == 0)
			return "";

		StringBuilder tmp = new StringBuilder();
		tmp.append(val.get(0).getName());
		for (int i = 1; i < val.size(); i++) {
			tmp.append(SEPARATOR);
			tmp.append(val.get(i).getName());
		}
		return tmp.toString();
	}


	@Override
	public String getDefaultString() {
		if (defValue == null || defValue.size() == 0)
			return "";
		return this.getInputString(defValue);
	}

	public boolean isValidClass( Entity ent ) {
		if( validClasses.size() == 0 )
			return true;

		for( Class<? extends Entity> c : validClasses ) {
			if( c.isAssignableFrom( ent.getClass() ) ) {
				return true;
			}
		}

		return false;
	}
}
