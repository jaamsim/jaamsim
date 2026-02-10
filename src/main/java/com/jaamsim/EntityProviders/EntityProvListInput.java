/*
 * JaamSim Discrete Event Simulation
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
package com.jaamsim.EntityProviders;

import java.util.ArrayList;
import java.util.Collections;

import com.jaamsim.Graphics.DisplayEntity;
import com.jaamsim.basicsim.Entity;
import com.jaamsim.basicsim.ErrorException;
import com.jaamsim.basicsim.JaamSimModel;
import com.jaamsim.input.ArrayListInput;
import com.jaamsim.input.Input;
import com.jaamsim.input.InputErrorException;
import com.jaamsim.input.KeywordIndex;
import com.jaamsim.input.Parser;

public class EntityProvListInput<T extends Entity> extends ArrayListInput<EntityListProvider<T>> {

	private Class<T> entClass;
	private boolean unique; // flag to determine if list must be unique or not

	public EntityProvListInput(Class<T> aClass, String key, String cat, ArrayList<EntityListProvider<T>> def) {
		super(key, cat, def);
		entClass = aClass;
		unique = true;
	}

	@Override
	public String applyConditioning(String str) {
		if (!str.contains("{")) {
			return str;
		}
		return Parser.addSubstringQuotesIfNeeded(str);
	}

	@Override
	public void parse(Entity thisEnt, KeywordIndex kw)
	throws InputErrorException {
		ArrayList<KeywordIndex> subArgs = kw.getSubArgs();

		// Simple format without inner braces
		if (subArgs.size() == 1) {
			KeywordIndex subArg = subArgs.get(0);
			ArrayList<EntityListProvider<T>> temp = new ArrayList<>(subArg.numArgs());
			for (int i = 0; i < subArg.numArgs(); i++) {
				KeywordIndex argKw = new KeywordIndex(subArg, i, i + 1);
				try {
					EntityListProvider<T> ep = Input.parseEntityListProvider(argKw, thisEnt, entClass);
					temp.add(ep);
				}
				catch (InputErrorException e) {
					String msg = e.getMessage();
					if (subArg.numArgs() > 1)
						msg = String.format(INP_ERR_ELEMENT, i + 1, e.getMessage());
					throw new InputErrorException(e.position, e.source, msg, e);
				}
			}
			value = temp;
			this.setValid(true);
			return;
		}

		// Normal format with inner braces
		ArrayList<EntityListProvider<T>> temp = new ArrayList<>(subArgs.size());
		for (int i = 0; i < subArgs.size(); i++) {
			KeywordIndex subArg = subArgs.get(i);
			try {
				EntityListProvider<T> ep = Input.parseEntityListProvider(subArg, thisEnt, entClass);
				temp.add(ep);
			}
			catch (InputErrorException e) {
				String msg = e.getMessage();
				if (subArg.numArgs() > 1)
					msg = String.format(INP_ERR_ELEMENT, i + 1, e.getMessage());
				throw new InputErrorException(e.position, e.source, msg, e);
			}
		}
		value = temp;
		this.setValid(true);
	}

	@Override
	public String getValidInputDesc() {
		if (entClass == DisplayEntity.class) {
			return Input.VALID_ENTITY_PROV_LIST;
		}
		return String.format(Input.VALID_ENTITY_PROV_LIST_TYPE, entClass.getSimpleName());
	}

	@Override
	public String[] getExamples() {
		String name = entClass.getSimpleName();
		if (entClass == DisplayEntity.class) {
			name = "Entity";
		}
		return new String[]{name+"1 "+name+"2",
				"{ "+name+"1 } { "+name+"2 }",
				"{ this.attrib1 } { this.attrib2 }"};
	}

	public void setUnique(boolean unique) {
		this.unique = unique;
	}

	@Override
	public ArrayList<String> getValidOptions(Entity ent) {
		ArrayList<String> list = new ArrayList<>();
		JaamSimModel simModel = ent.getJaamSimModel();
		for (Entity each : simModel.getClonesOfIterator(entClass)) {
			list.add(each.getName());
		}
		Collections.sort(list, Input.uiSortOrder);
		return list;
	}

	@Override
	public void getValueTokens(ArrayList<String> toks) {
		if (value == null || valueTokens == null || isDef)
			return;

		boolean braces = valueTokens[0].equals("{");
		for (int i = 0; i < value.size(); i++) {
			if (braces) {
				toks.add("{");
			}
			toks.add(value.get(i).toString());
			if (braces) {
				toks.add("}");
			}
		}
	}

	@Override
	public boolean removeReferences(Entity ent) {
		if (value == null)
			return false;
		boolean ret = false;
		for (int i = value.size() - 1; i >= 0; i--) {
			EntityListProvider<T> ep = value.get(i);
			if (ep instanceof EntityProvConstant
					&& ((EntityProvConstant<T>) ep).getEntity() == ent) {
				value.remove(i);
				ret = true;
			}
		}
		return ret;
	}

	@Override
	public void appendEntityReferences(ArrayList<Entity> list) {
		if (value == null)
			return;
		for (EntityListProvider<T> ep : value) {
			if (ep instanceof EntityProvConstant) {
				Entity ent = ((EntityProvConstant<T>) ep).getEntity();
				if (ent == null || list.contains(ent))
					continue;
				list.add(ent);
			}

			else if (ep instanceof EntityProvExpression) {
				((EntityProvExpression<T>) ep).appendEntityReferences(list);
			}
		}
	}

	@Override
	public boolean useExpressionBuilder() {
		return true;
	}

	@Override
	public String getPresentValueString(Entity thisEnt, double simTime) {
		if (value == null)
			return "";

		StringBuilder sb = new StringBuilder();
		sb.append("{").append(BRACE_SEPARATOR);
		boolean first = true;
		for (T ent : getNextEntityList(thisEnt, simTime)) {
			if (!first) {
				sb.append(", ");
			}
			first = false;
			sb.append(String.format("[%s]", ent));
		}
		sb.append(BRACE_SEPARATOR).append("}");
		return sb.toString();
	}

	public ArrayList<T> getNextEntityList(Entity thisEnt, double simTime) {
		ArrayList<T> ret = new ArrayList<>();
		for (int i = 0; i < getListSize(); i++) {
			try {
				getValue().get(i).getNextEntityList(thisEnt, simTime, ret, unique);
			}
			catch (ErrorException e) {
				e.keyword = getKeyword();
				e.index = i + 1;
				throw e;
			}
			catch (Exception e) {
				throw new ErrorException(thisEnt, getKeyword(), i + 1, e);
			}
		}
		return ret;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <V> V getValue(Entity thisEnt, double simTime, Class<V> klass) {
		return (V) getNextEntityList(thisEnt, simTime);
	}

	@Override
	public Class<?> getReturnType() {
		return ArrayList.class;
	}

}
