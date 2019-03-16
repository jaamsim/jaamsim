/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2017-2019 JaamSim Software Inc.
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
package com.jaamsim.EntityProviders;

import java.util.ArrayList;
import java.util.Collections;

import com.jaamsim.Graphics.DisplayEntity;
import com.jaamsim.basicsim.Entity;
import com.jaamsim.input.Input;
import com.jaamsim.input.InputErrorException;
import com.jaamsim.input.KeywordIndex;
import com.jaamsim.input.Parser;

public class EntityProvInput<T extends Entity> extends Input<EntityProvider<T>> {

	private Entity thisEnt;
	private Class<T> entClass;
	private ArrayList<Class<? extends Entity>> invalidClasses;

	public EntityProvInput(Class<T> aClass, String key, String cat, EntityProvider<T> def) {
		super(key, cat, def);
		entClass = aClass;
		thisEnt = null;
		invalidClasses = new ArrayList<>();
	}

	public void setEntity(Entity ent) {
		thisEnt = ent;
	}

	@Override
	public void copyFrom(Input<?> in) {
		super.copyFrom(in);

		// An expression input must be re-parsed to reset the entity referred to by "this"
		if (value instanceof EntityProvExpression<?>) {
			parseFrom(in);
		}
	}

	@Override
	public String applyConditioning(String str) {
		return Parser.addQuotesIfNeeded(str);
	}

	@Override
	public void parse(KeywordIndex kw) throws InputErrorException {
		value = Input.parseEntityProvider(kw, thisEnt, entClass);
		this.setValid(true);
	}

	@Override
	public String getValidInputDesc() {
		if (entClass == DisplayEntity.class) {
			return Input.VALID_ENTITY_PROV;
		}
		return String.format(Input.VALID_ENTITY_PROV_TYPE, entClass.getSimpleName());
	}

	@Override
	public ArrayList<String> getValidOptions(Entity ent) {
		ArrayList<String> list = new ArrayList<>();

		for (T each: thisEnt.getJaamSimModel().getClonesOfIterator(entClass)) {
			if (each.testFlag(Entity.FLAG_GENERATED))
				continue;

			if (!isValid(each))
				continue;

			list.add(each.getName());
		}
		Collections.sort(list, Input.uiSortOrder);
		return list;
	}

	public void addInvalidClass(Class<? extends Entity> aClass) {
		invalidClasses.add(aClass);
	}

	private boolean isValid(T ent) {

		for (Class<? extends Entity> cls : invalidClasses) {
			if (cls.isAssignableFrom(ent.getClass())) {
				return false;
			}
		}

		return true;
	}

	@Override
	public void getValueTokens(ArrayList<String> toks) {
		if (value == null)
			return;

		toks.add(value.toString());
	}

	@Override
	public boolean removeReferences(Entity ent) {
		if (value instanceof EntityProvConstant) {
			EntityProvConstant<T> epc = (EntityProvConstant<T>) value;
			if (epc.getEntity() == ent) {
				this.reset();
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean useExpressionBuilder() {
		return true;
	}

	@Override
	public String getPresentValueString(double simTime) {
		if (value == null)
			return "";

		return String.format("[%s]", value.getNextEntity(simTime));
	}

}
