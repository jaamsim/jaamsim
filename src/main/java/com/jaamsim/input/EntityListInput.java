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

import com.jaamsim.basicsim.Entity;
import com.jaamsim.basicsim.JaamSimModel;

public class EntityListInput<T extends Entity> extends ListInput<ArrayList<T>> {
	private Class<T> entClass;
	private boolean unique; // flag to determine if list must be unique or not
	private boolean even;  // flag to determine if there must be an even number of entries
	private boolean includeSubclasses;  // flag to determine if subclasses are valid
	private boolean includeSelf; // flag to determine whether to include the calling entity in the entityList
	private ArrayList<Class<? extends Entity>> validClasses; // list of valid classes (including subclasses).  if empty, then all classes are valid
	private ArrayList<Class<? extends Entity>> invalidClasses; // list of invalid classes (including subclasses).

	public EntityListInput(Class<T> aClass, String key, String cat, ArrayList<T> def) {
		super(key, cat, def);
		entClass = aClass;
		unique = true;
		even = false;
		includeSubclasses = true;
		includeSelf = true;
		validClasses = new ArrayList<>();
		invalidClasses = new ArrayList<>();
	}

	@Override
	public void parse(Entity thisEnt, KeywordIndex kw)
	throws InputErrorException {

		// If adding to the list
		if( kw.getArg( 0 ).equals( "++" ) ) {
			KeywordIndex subKw = new KeywordIndex(kw, 1);

			ArrayList<T> newValue;
			if( value == null )
				newValue = new ArrayList<>();
			else
				newValue = new ArrayList<>( value );

			Input.assertCountRange(subKw, 0, maxCount - newValue.size());
			if( even ) {
				if ((kw.numArgs() % 2) == 0)
					throw new InputErrorException(INP_ERR_EVENCOUNT, kw.argString());
			}

			ArrayList<T> addedValues = Input.parseEntityList(thisEnt.getJaamSimModel(), subKw, entClass, unique);
			for( T val : addedValues ) {
				if( unique && newValue.contains( val ) )
					throw new InputErrorException(INP_ERR_NOTUNIQUE, val.getName());
				newValue.add( val );
			}
			value = newValue;
			return;
		}

		// If removing from the list
		if( kw.getArg( 0 ).equals( "--" ) ) {
			KeywordIndex subKw = new KeywordIndex(kw, 1);

			Input.assertCountRange(subKw, 0, value.size() - minCount );
			if( even ) {
				if ((kw.numArgs() % 2) == 0)
					throw new InputErrorException(INP_ERR_EVENCOUNT, kw.argString());
			}

			ArrayList<T> newValue = new ArrayList<>( value );
			ArrayList<T> removedValues = Input.parseEntityList(thisEnt.getJaamSimModel(), subKw, entClass, unique);
			for( T val : removedValues ) {
				if( ! newValue.contains( val ) )
					InputAgent.logWarning(thisEnt.getJaamSimModel(),
							"Could not remove " + val + " from " + this.getKeyword() );
				newValue.remove( val );
			}
			value = newValue;
			return;
		}

		// Otherwise, just set the list normally
		Input.assertCountRange(kw, minCount, maxCount);
		if( even )
			Input.assertCountEven(kw);

		value = Input.parseEntityList(thisEnt.getJaamSimModel(), kw, entClass, unique);
	}

	@Override
	public String getValidInputDesc() {
		return Input.VALID_ENTITY_LIST;
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

	@Override
	public ArrayList<String> getValidOptions(Entity ent) {
		ArrayList<String> list = new ArrayList<>();
		for(T each: ent.getJaamSimModel().getClonesOfIterator(entClass) ) {
			if(!each.isRegistered())
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

		// Include the default values
		if (getDefaultValue() != null) {
			for (Entity def : getDefaultValue()) {
				String name = def.getName();
				if (list.contains(name))
					continue;
				list.add(name);
			}
		}

		Collections.sort(list, Input.uiSortOrder);
		return list;
	}

	@Override
	public void getValueTokens(ArrayList<String> toks) {
		if (value == null || isDefault())
			return;

		for (int i = 0; i < value.size(); i++)
			toks.add(value.get(i).getName());
	}

	@Override
	public void setTokens(KeywordIndex kw) {
		isDef = false;

		String[] args = kw.getArgArray();
		if (args.length > 0) {

			// Consider the following input case:
			// Object1 Keyword1 { ++ Entity1 ...
			if (args[0].equals( "++" )) {
				this.addTokens(args);
				return;
			}

			// Consider the following input case:
			// Object1 Keyword1 { -- Entity1 ...
			if (args[0].equals( "--" )) {
				if (this.removeTokens(args))
					return;
			}
		}

		valueTokens = args;
	}

	@Override
	public String getDefaultString(JaamSimModel simModel) {
		if (defValue == null || defValue.isEmpty())
			return "";

		StringBuilder tmp = new StringBuilder();
		tmp.append(defValue.get(0).getName());
		for (int i = 1; i < defValue.size(); i++) {
			tmp.append(SEPARATOR);
			tmp.append(defValue.get(i).getName());
		}
		return tmp.toString();
	}

	public boolean isValidClass( Entity ent ) {

		for (Class<? extends Entity> c : invalidClasses) {
			if (c.isAssignableFrom( ent.getClass() )) {
				return false;
			}
		}

		if( validClasses.size() == 0 )
			return true;

		for( Class<? extends Entity> c : validClasses ) {
			if( c.isAssignableFrom( ent.getClass() ) ) {
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
		for (Entity ent : value) {
			if (ent == null || list.contains(ent))
				continue;
			list.add(ent);
		}
	}

}
