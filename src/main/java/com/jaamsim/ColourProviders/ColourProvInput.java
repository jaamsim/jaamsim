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
package com.jaamsim.ColourProviders;

import java.util.ArrayList;

import com.jaamsim.basicsim.Entity;
import com.jaamsim.basicsim.ErrorException;
import com.jaamsim.input.ColourInput;
import com.jaamsim.input.Input;
import com.jaamsim.input.InputErrorException;
import com.jaamsim.input.KeywordIndex;
import com.jaamsim.input.Parser;
import com.jaamsim.math.Color4d;

public class ColourProvInput extends Input<ColourProvider> {

	public ColourProvInput(String key, String cat, ColourProvider def) {
		super(key, cat, def);
	}

	public ColourProvInput(String key, String cat, Color4d def) {
		this(key, cat, new ColourProvConstant(def));
	}

	@Override
	public String applyConditioning(String str) {

		// No changes required if the input is a constant colour value
		ArrayList<String> tokens = new ArrayList<>();
		Parser.tokenize(tokens, str, true);
		if (tokens.size() == 1 && ColourInput.getColorWithName(tokens.get(0)) != null)
			return str;
		if (tokens.size() == 2 && ColourInput.getColorWithName(tokens.get(0)) != null
				&& isDouble(tokens.get(1)))
			return str;
		if (tokens.size() == 3 && isDouble(tokens.get(0)) && isDouble(tokens.get(1))
				&& isDouble(tokens.get(2)))
			return str;
		if (tokens.size() == 4 && isDouble(tokens.get(0)) && isDouble(tokens.get(1))
				&& isDouble(tokens.get(2)) && isDouble(tokens.get(3)))
			return str;

		return Parser.addQuotesIfNeeded(str);
	}

	@Override
	public void parse(Entity thisEnt, KeywordIndex kw) throws InputErrorException {
		value = Input.parseColourProvider(kw, thisEnt);
		this.setValid(true);
	}

	@Override
	public String getValidInputDesc() {
		return Input.VALID_COLOUR_PROV;
	}

	@Override
	public String[] getExamples() {
		return Input.EXAMPLE_COLOUR_PROV;
	}

	@Override
	public void getValueTokens(ArrayList<String> toks) {
		if (value == null || isDef)
			return;

		// Preserve the exact text for a constant value input
		if (isConstant()) {
			super.getValueTokens(toks);
			return;
		}

		// All other inputs can be built from scratch
		toks.add(value.toString());
	}

	@Override
	public void appendEntityReferences(ArrayList<Entity> list) {
		value.appendEntityReferences(list);
	}

	@Override
	public boolean useExpressionBuilder() {
		return true;
	}

	@Override
	public String getPresentValueString(Entity thisEnt, double simTime) {
		if (value == null)
			return "";
		return value.getNextColour(thisEnt, simTime).toString();
	}

	public Color4d getNextColour(Entity thisEnt, double simTime) {
		try {
			return getValue().getNextColour(thisEnt, simTime);
		}
		catch (ErrorException e) {
			e.keyword = getKeyword();
			throw e;
		}
	}

	public boolean isConstant() {
		return (value instanceof ColourProvConstant);
	}

}
