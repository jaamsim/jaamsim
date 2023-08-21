/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2022-2023 JaamSim Software Inc.
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
package com.jaamsim.BooleanProviders;

import java.util.ArrayList;

import com.jaamsim.basicsim.Entity;
import com.jaamsim.basicsim.ErrorException;
import com.jaamsim.input.BooleanInput;
import com.jaamsim.input.Input;
import com.jaamsim.input.InputErrorException;
import com.jaamsim.input.KeywordIndex;
import com.jaamsim.input.Parser;

public class BooleanProvInput extends Input<BooleanProvider> {

	public BooleanProvInput(String key, String cat, BooleanProvider def) {
		super(key, cat, def);
	}

	public BooleanProvInput(String key, String cat, boolean def) {
		this(key, cat, new BooleanProvConstant(def));
	}

	public void setDefaultValue(boolean def) {
		setDefaultValue(new BooleanProvConstant(def));
	}

	@Override
	public String applyConditioning(String str) {
		if (str.equals("t") || str.equals("T") || str.equals("1"))
			return BooleanInput.TRUE;
		if (str.equals("f") || str.equals("F") || str.equals("0"))
			return BooleanInput.FALSE;
		return Parser.addQuotesIfNeeded(str);
	}

	@Override
	public void parse(Entity thisEnt, KeywordIndex kw) throws InputErrorException {
		value = Input.parseBooleanProvider(kw, thisEnt);
		this.setValid(true);
	}

	@Override
	public String getValidInputDesc() {
		return Input.VALID_BOOLEAN_PROV;
	}

	@Override
	public String[] getExamples() {
		return Input.EXAMPLE_BOOLEAN_PROV;
	}

	@Override
	public ArrayList<String> getValidOptions(Entity ent) {
		return new ArrayList<>(BooleanInput.validOptions);
	}

	@Override
	public void getValueTokens(ArrayList<String> toks) {
		if (value == null || isDef)
			return;
		toks.add(value.toString());
	}

	@Override
	public void appendEntityReferences(ArrayList<Entity> list) {
		if (value == null)
			return;
		if (value instanceof BooleanProvExpression) {
			((BooleanProvExpression) value).appendEntityReferences(list);
			return;
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
		return Boolean.toString(value.getNextBoolean(thisEnt, simTime));
	}

	public boolean getNextBoolean(Entity thisEnt, double simTime) {
		try {
			return getValue().getNextBoolean(thisEnt, simTime);
		}
		catch (ErrorException e) {
			e.keyword = getKeyword();
			throw e;
		}
	}

	public boolean isConstant() {
		return (value instanceof BooleanProvConstant);
	}

}
