/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2023 JaamSim Software Inc.
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

public class EntityProvListInput<T extends Entity> extends ArrayListInput<EntityProvider<T>> {

	private Class<T> entClass;

	public EntityProvListInput(Class<T> aClass, String key, String cat, ArrayList<EntityProvider<T>> def) {
		super(key, cat, def);
		entClass = aClass;
	}

	@Override
	public void parse(Entity thisEnt, KeywordIndex kw)
	throws InputErrorException {
		ArrayList<KeywordIndex> subArgs = kw.getSubArgs();

		// Simple format without inner braces
		if (subArgs.size() == 1) {
			KeywordIndex subArg = subArgs.get(0);
			ArrayList<EntityProvider<T>> temp = new ArrayList<>(subArg.numArgs());
			for (int i = 0; i < subArg.numArgs(); i++) {
				KeywordIndex argKw = new KeywordIndex(subArg, i, i + 1);
				try {
					EntityProvider<T> ep = Input.parseEntityProvider(argKw, thisEnt, entClass);
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
		ArrayList<EntityProvider<T>> temp = new ArrayList<>(subArgs.size());
		for (int i = 0; i < subArgs.size(); i++) {
			KeywordIndex subArg = subArgs.get(i);
			try {
				EntityProvider<T> ep = Input.parseEntityProvider(subArg, thisEnt, entClass);
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
			EntityProvider<T> ep = value.get(i);
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
		for (EntityProvider<T> ep : value) {
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
		boolean first = true;
		for (EntityProvider<T> ep : value) {
			if (!first) {
				first = false;
			}
			else {
				sb.append(Input.SEPARATOR);
			}
			//sb.append("{").append(Input.BRACE_SEPARATOR);
			//sb.append(String.format("[%s]", ep.getNextEntity(thisEnt, simTime)));
			sb.append(ep.getNextEntity(thisEnt, simTime));
			//sb.append(Input.BRACE_SEPARATOR).append("}");
		}
		return sb.toString();
	}

	public T getNextEntity(int i, Entity thisEnt, double simTime) {
		try {
			return getValue().get(i).getNextEntity(thisEnt, simTime);
		}
		catch (ErrorException e) {
			e.keyword = getKeyword();
			e.index = i + 1;
			throw e;
		}
		catch (Exception e) {
			throw new ErrorException("", -1, thisEnt.getName(), getKeyword(), i + 1,
					e.getMessage(), e);
		}
	}

	public ArrayList<T> getNextEntityList(Entity thisEnt, double simTime) {
		ArrayList<T> ret = new ArrayList<>();
		for (int i = 0; i < getListSize(); i++) {
			try {
				ret.add(getValue().get(i).getNextEntity(thisEnt, simTime));
			}
			catch (ErrorException e) {
				e.keyword = getKeyword();
				e.index = i + 1;
				throw e;
			}
			catch (Exception e) {
				throw new ErrorException("", -1, thisEnt.getName(), getKeyword(), i + 1,
						e.getMessage(), e);
			}
		}
		return ret;
	}

}
