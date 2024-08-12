/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2014 Ausenco Engineering Canada Inc.
 * Copyright (C) 2016-2024 JaamSim Software Inc.
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
import java.util.HashSet;

import com.jaamsim.basicsim.Entity;
import com.jaamsim.input.ExpEvaluator.EntityParseContext;
import com.jaamsim.units.DimensionlessUnit;
import com.jaamsim.units.Unit;

/**
 * AttributeDefinitionListInput is an object for parsing inputs consisting of a list of
 * Attribute definitions using the syntax:
 * Entity AttributeDefinitionList { { AttibuteName1 Value1 } { AttibuteName2 Value2 } ... }
 * @author Harry King
 */
public class AttributeDefinitionListInput extends ArrayListInput<AttributeHandle> {

	private ArrayList<ExpEvaluator.EntityParseContext> parseContextList;

	public AttributeDefinitionListInput(String key, String cat, ArrayList<AttributeHandle> def) {
		super(key, cat, def);
	}

	@Override
	public String applyConditioning(String str) {
		return Parser.addQuotesIfNeededToDefinitions(str);
	}

	@Override
	public void parse(Entity thisEnt, KeywordIndex kw) throws InputErrorException {

		// Divide up the inputs by the inner braces
		ArrayList<KeywordIndex> subArgs = kw.getSubArgs();
		ArrayList<AttributeHandle> temp = new ArrayList<>(subArgs.size());
		ArrayList<ExpEvaluator.EntityParseContext> pcList = new ArrayList<>(subArgs.size());

		// Ensure that no attribute names are repeated
		HashSet<String> nameSet = new HashSet<>();
		for (KeywordIndex subArg : subArgs) {
			if (subArg.numArgs() == 0)
				continue;
			String name = subArg.getArg(0);
			if (nameSet.contains(name))
				throw new InputErrorException("Duplicate attribute name: %s", name);
			nameSet.add(name);
		}

		// Parse the inputs within each inner brace
		for (int i = 0; i < subArgs.size(); i++) {
			KeywordIndex subArg = subArgs.get(i);
			Input.assertCount(subArg, 2);
			try {
				// Parse the attribute name
				String name = subArg.getArg(0);
				ValueHandle vh = thisEnt.getOutputHandle(name);
				if (vh != null && !(vh instanceof AttributeHandle)) {
					throw new InputErrorException("Attribute name is the same as existing output name: %s", name);
				}

				ExpResult expVal;
				Class<? extends Unit> unitType = DimensionlessUnit.class;

				// Parse the expression
				String expString = subArg.getArg(1);
				EntityParseContext pc = ExpEvaluator.getParseContext(thisEnt, expString);
				ExpParser.Expression exp = ExpParser.parseExpression(pc, expString);
				expVal = ExpEvaluator.evaluateExpression(exp, thisEnt, 0);
				if (expVal.type == ExpResType.NUMBER) {
					unitType = expVal.unitType;
				}

				// Save the data for this attribute
				AttributeHandle h = new AttributeHandle(thisEnt, name, expVal, expVal, unitType);
				temp.add(h);
				pcList.add(pc);

			} catch (ExpError e) {
				throw new InputErrorException(e);
			} catch (InputErrorException e) {
				throw new InputErrorException(INP_ERR_ELEMENT, i+1, e.getMessage());
			}
		}

		// Save the data for each attribute
		parseContextList = pcList;
		value = temp;
	}

	@Override
	public String getValidInputDesc() {
		return Input.VALID_ATTRIB_DEF;
	}

	@Override
	public String[] getExamples() {
		return Input.EXAMPLE_ATTRIB_DEF;
	}

	@Override
	public void getValueTokens(ArrayList<String> toks) {
		if (value == null || isDef)
			return;
		for (int i = 0; i < value.size(); i++) {
			AttributeHandle h = value.get(i);
			toks.add("{");
			toks.add(h.getName());
			toks.add(parseContextList.get(i).getUpdatedSource());
			toks.add("}");
		}
	}

	@Override
	public boolean useExpressionBuilder() {
		return true;
	}

}
