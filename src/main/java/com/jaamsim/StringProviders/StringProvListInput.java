/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2015 Ausenco Engineering Canada Inc.
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
package com.jaamsim.StringProviders;

import java.util.ArrayList;
import java.util.Collections;

import com.jaamsim.Samples.SampleProvider;
import com.jaamsim.basicsim.Entity;
import com.jaamsim.basicsim.JaamSimModel;
import com.jaamsim.input.Input;
import com.jaamsim.input.InputErrorException;
import com.jaamsim.input.KeywordIndex;
import com.jaamsim.input.ListInput;
import com.jaamsim.units.DimensionlessUnit;

public class StringProvListInput extends ListInput<ArrayList<StringProvider>> {

	public StringProvListInput(String key, String cat, ArrayList<StringProvider> def) {
		super(key, cat, def);
	}

	@Override
	public int getListSize() {
		if (value == null)
			return 0;
		else
			return value.size();
	}

	@Override
	public void copyFrom(Entity thisEnt, Input<?> in) {
		super.copyFrom(thisEnt, in);

		// An expression input must be re-parsed to reset the entity referred to by "this"
		parseFrom(thisEnt, in);
	}

	@Override
	public void parse(Entity thisEnt, KeywordIndex kw) throws InputErrorException {
		ArrayList<KeywordIndex> subArgs = kw.getSubArgs();
		ArrayList<StringProvider> temp = new ArrayList<>(subArgs.size());
		for (int i = 0; i < subArgs.size(); i++) {
			KeywordIndex subArg = subArgs.get(i);
			try {
				StringProvider sp = Input.parseStringProvider(subArg, thisEnt, DimensionlessUnit.class);
				temp.add(sp);
			}
			catch (InputErrorException e) {
				if (subArgs.size() == 1)
					throw new InputErrorException(e.getMessage());
				else
					throw new InputErrorException(INP_ERR_ELEMENT, i+1, e.getMessage());
			}
		}
		value = temp;
		this.setValid(true);
	}

	@Override
	public String getValidInputDesc() {
		return Input.VALID_STRING_PROV_LIST;
	}

	@Override
	public ArrayList<String> getValidOptions(Entity ent) {
		ArrayList<String> list = new ArrayList<>();
		JaamSimModel simModel = ent.getJaamSimModel();
		for (Entity each : simModel.getClonesOfIterator(Entity.class, SampleProvider.class)) {
			SampleProvider samp = (SampleProvider)each;
			if (samp.getUnitType() == DimensionlessUnit.class)
				list.add(each.getName());
		}
		Collections.sort(list, Input.uiSortOrder);
		return list;
	}

	@Override
	public void getValueTokens(ArrayList<String> toks) {
		if (value == null) return;

		for (int i = 0; i < value.size(); i++) {
			toks.add("{");
			toks.add(value.get(i).toString());
			toks.add("}");
		}
	}

	@Override
	public String getDefaultString() {
		if (defValue == null || defValue.isEmpty()) {
			return "";
		}

		StringBuilder tmp = new StringBuilder();
		for (int i = 0; i < defValue.size(); i++) {
			if (i > 0)
				tmp.append(SEPARATOR);

			tmp.append("{ ");
			tmp.append(defValue.get(i));
			tmp.append(" }");
		}

		return tmp.toString();
	}

	@Override
	public boolean removeReferences(Entity ent) {
		if (value == null)
			return false;

		ArrayList<StringProvider> list = new ArrayList<>();
		for (StringProvider samp : value) {
			if (samp instanceof StringProvSample) {
				StringProvSample spsamp = (StringProvSample) samp;
				if (spsamp.getSampleProvider() == ent) {
					list.add(samp);
				}
			}
		}
		boolean ret = value.removeAll(list);
		return ret;
	}

	@Override
	public boolean useExpressionBuilder() {
		return true;
	}

	@Override
	public String getPresentValueString(double simTime) {
		if (value == null)
			return "";

		StringBuilder sb = new StringBuilder();
		boolean first = true;
		for (int i = 0; i < value.size(); i++) {
			StringProvider prov = value.get(i);
			if (!first) {
				first = false;
			}
			else {
				sb.append(Input.SEPARATOR);
			}
			sb.append("{").append(Input.BRACE_SEPARATOR);
			sb.append(prov.getNextString(simTime));
			sb.append(Input.BRACE_SEPARATOR).append("}");
		}
		return sb.toString();
	}

}
