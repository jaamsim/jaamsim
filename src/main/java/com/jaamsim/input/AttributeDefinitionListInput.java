/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2014 Ausenco Engineering Canada Inc.
 * Copyright (C) 2016-2019 JaamSim Software Inc.
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

import com.jaamsim.basicsim.Entity;
import com.jaamsim.units.DimensionlessUnit;
import com.jaamsim.units.Unit;

/**
 * AttributeDefinitionListInput is an object for parsing inputs consisting of a list of
 * Attribute definitions using the syntax:
 * Entity AttributeDefinitionList { { AttibuteName1 Value1 Unit1 } { AttibuteName2 Value2 Unit2 } ... }
 * @author Harry King
 */
public class AttributeDefinitionListInput extends ListInput<ArrayList<AttributeHandle>> {
	public AttributeDefinitionListInput(String key, String cat, ArrayList<AttributeHandle> def) {
		super(key, cat, def);
	}

	@Override
	public String applyConditioning(String str) {
		return Parser.addEnclosure("{", str, "}");
	}

	@Override
	public void parse(Entity thisEnt, KeywordIndex kw) throws InputErrorException {

		// Divide up the inputs by the inner braces
		ArrayList<KeywordIndex> subArgs = kw.getSubArgs();
		ArrayList<AttributeHandle> temp = new ArrayList<>(subArgs.size());

		// Parse the inputs within each inner brace
		for (int i = 0; i < subArgs.size(); i++) {
			KeywordIndex subArg = subArgs.get(i);
			Input.assertCount(subArg, 2, 3);
			try {
				// Parse the attribute name
				String name = subArg.getArg(0);
				if (OutputHandle.hasOutput(thisEnt.getClass(), name)) {
					throw new InputErrorException("Attribute name is the same as existing output name: %s", name);
				}

				ExpResult expVal;
				Class<? extends Unit> unitType = DimensionlessUnit.class;

				if (subArg.numArgs() == 2) {
					// parse this as an expression
					String expString = subArg.getArg(1);
					ExpParser.Expression exp = ExpParser.parseExpression(ExpEvaluator.getParseContext(thisEnt, expString), expString);
					expVal = ExpEvaluator.evaluateExpression(exp, 0);
					if (expVal.type == ExpResType.NUMBER) {
						unitType = expVal.unitType;
					}
				} else {
					// Parse the unit type
					double factor = 1.0;
					if (subArg.numArgs() == 3) {
						String unitName = Parser.removeEnclosure("[", subArg.getArg(2), "]");
						Unit unit = Input.parseUnit(thisEnt.getJaamSimModel(), unitName);
						unitType = unit.getClass();
						factor = unit.getConversionFactorToSI();
					}

					// Parse the initial value
					double val;
					try {
						val = factor * Double.valueOf(subArg.getArg(1));
					} catch (Exception e) {
						throw new InputErrorException(INP_ERR_DOUBLE, subArg.getArg(1));
					}
					expVal = ExpResult.makeNumResult(val, unitType);
				}

				// Save the data for this attribute
				AttributeHandle h = (AttributeHandle) thisEnt.getOutputHandle(name);
				if (h == null)
					h = new AttributeHandle(thisEnt, name);
				h.setUnitType(unitType);
				h.setInitialValue(expVal);
				h.setValue(expVal);
				temp.add(h);

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
	public void copyFrom(Entity thisEnt, Input<?> in) {
		super.copyFrom(thisEnt, in);
		value = new ArrayList<>();
		@SuppressWarnings("unchecked")
		ArrayList<AttributeHandle> inValue = (ArrayList<AttributeHandle>) (in.value);
		for (AttributeHandle h : inValue) {
			AttributeHandle hNew = new AttributeHandle(thisEnt, h.getName());
			hNew.setUnitType(h.getUnitType());
			hNew.setInitialValue(h.getInitialValue());
			hNew.setValue(h.getValue(0.0d, ExpResult.class));
			value.add(hNew);
		}
	}

	@Override
	public int getListSize() {
		if (value == null)
			return 0;
		else
			return value.size();
	}

	@Override
	public String getDefaultString() {
		if (defValue == null || defValue.isEmpty())
			return "";

		return defValue.toString();
	}

	@Override
	public boolean useExpressionBuilder() {
		return true;
	}

}
