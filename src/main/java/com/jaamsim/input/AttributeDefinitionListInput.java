/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2014 Ausenco Engineering Canada Inc.
 * Copyright (C) 2016-2025 JaamSim Software Inc.
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
import com.jaamsim.basicsim.ErrorException;
import com.jaamsim.input.ExpEvaluator.EntityParseContext;
import com.jaamsim.units.DimensionlessUnit;
import com.jaamsim.units.Unit;

/**
 * AttributeDefinitionListInput is an object for parsing inputs consisting of a list of
 * Attribute definitions using the syntax:
 * Entity AttributeDefinitionList { { AttibuteName1 Value1 } { AttibuteName2 Value2 } ... }
 * @author Harry King
 */
public class AttributeDefinitionListInput extends ArrayListInput<NamedExpression> {

	public AttributeDefinitionListInput(String key, String cat, ArrayList<NamedExpression> def) {
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
		ArrayList<NamedExpression> temp = new ArrayList<>(subArgs.size());

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

				Class<? extends Unit> unitType = DimensionlessUnit.class;

				// Parse the expression
				String expString = subArg.getArg(1);
				EntityParseContext pc = ExpEvaluator.getParseContext(thisEnt, expString);
				ExpParser.Expression exp = ExpParser.parseExpression(pc, expString);

				// Save the data for this attribute
				NamedExpression ne = new NamedExpression(name, pc, exp, unitType);
				temp.add(ne);

			} catch (ExpError e) {
				throw new InputErrorException(e);
			} catch (InputErrorException e) {
				throw new InputErrorException(INP_ERR_ELEMENT, i+1, e.getMessage());
			}
		}

		// Save the data for each attribute
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
			NamedExpression ne = value.get(i);
			toks.add("{");
			toks.add(ne.getName());
			toks.add(ne.getParseContext().getUpdatedSource());
			toks.add("}");
		}
	}

	@Override
	public boolean useExpressionBuilder() {
		return true;
	}

	@Override
	public String getStubDefinition() {
		StringBuilder sb = new StringBuilder();
		boolean first = true;
		for (NamedExpression ne : value) {
			if (first) {
				first = false;
			}
			else {
				sb.append(Input.BRACE_SEPARATOR);
			}
			sb.append(ne.getStubDefinition());
		}
		return sb.toString();
	}

	@Override
	public String getPresentValueString(Entity thisEnt, double simTime) {
		if (value == null || isDef)
			return "";

		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < value.size(); i++) {
			NamedExpression ne = value.get(i);
			if (i > 0)
				sb.append(BRACE_SEPARATOR);

			// Opening brace and attribute name
			sb.append("{").append(BRACE_SEPARATOR);
			sb.append(ne.getName()).append(SEPARATOR);

			// Present value
			try {
				ExpResult res = ExpEvaluator.evaluateExpression(ne.getExpression(), thisEnt, simTime);
				sb.append(res.toString());
			}
			catch (ExpError e) {
				throw new ErrorException(thisEnt, getKeyword(), i + 1, e);
			}

			// Closing brace
			sb.append(BRACE_SEPARATOR).append("}");
		}
		return sb.toString();
	}

}
