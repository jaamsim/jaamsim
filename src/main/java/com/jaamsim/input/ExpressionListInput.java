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
package com.jaamsim.input;

import java.util.ArrayList;

import com.jaamsim.basicsim.Entity;
import com.jaamsim.basicsim.ErrorException;
import com.jaamsim.input.ExpEvaluator.EntityParseContext;
import com.jaamsim.input.ExpParser.Expression;
import com.jaamsim.units.DimensionlessUnit;
import com.jaamsim.units.Unit;

public class ExpressionListInput extends ArrayListInput<Expression> {
	private Class<? extends Unit> unitType;
	private ArrayList<EntityParseContext> parseContextList;
	private ExpResType resType;

	public ExpressionListInput(String key, String cat, ArrayList<Expression> def) {
		super(key, cat, def);
	}

	public void setUnitType(Class<? extends Unit> u) {
		if (u == unitType) {
			return;
		}
		if (!isDef)
			setValid(false);
		unitType = u;
	}

	public void setResultType(ExpResType type) {
		resType = type;
	}

	@Override
	public Class<? extends Unit> getUnitType() {
		return unitType;
	}

	@Override
	public String applyConditioning(String str) {
		return Parser.addSubstringQuotesIfNeeded(str);
	}

	@Override
	public void parse(Entity thisEnt, KeywordIndex kw) throws InputErrorException {
		ArrayList<KeywordIndex> subArgs = kw.getSubArgs();
		ArrayList<Expression> temp = new ArrayList<>(subArgs.size());
		ArrayList<EntityParseContext> tempPCList = new ArrayList<>(subArgs.size());
		for (int i = 0; i < subArgs.size(); i++) {
			KeywordIndex subArg = subArgs.get(i);
			Input.assertCount(subArg, 1);
			try {
				String expString = subArg.getArg(0);

				ExpEvaluator.EntityParseContext pc = ExpEvaluator.getParseContext(thisEnt, expString);
				Expression exp = ExpParser.parseExpression(pc, expString);
				ExpParser.assertUnitType(exp, unitType);
				if (resType != null)
					ExpParser.assertResultType(exp, resType);
				tempPCList.add(pc);
				temp.add(exp);
			}
			catch (ExpError e) {
				String msg = e.getMessage();
				if (subArg.numArgs() > 1)
					msg = String.format(INP_ERR_ELEMENT, i + 1, e.getMessage());
				throw new InputErrorException(e.pos, e.source, msg, e);
			}
		}
		parseContextList = tempPCList;
		value = temp;
		this.setValid(true);
	}

	@Override
	public String getValidInputDesc() {

		if (resType == ExpResType.NUMBER) {
			if (unitType == DimensionlessUnit.class)
				return VALID_EXP_LIST_DIMLESS;
			else
				return VALID_EXP_LIST_NUM;
		}

		if (resType == ExpResType.STRING)
			return VALID_EXP_LIST_STR;

		if (resType == ExpResType.ENTITY)
			return VALID_EXP_LIST_ENT;

		if (resType == ExpResType.COLLECTION)
			return VALID_EXP_LIST_COL;

		return VALID_EXP_LIST;
	}

	@Override
	public void getValueTokens(ArrayList<String> toks) {
		if (value == null || isDef)
			return;
		for (int i = 0; i < value.size(); i++) {
			toks.add("{");
			toks.add(parseContextList.get(i).getUpdatedSource());
			toks.add("}");
		}
	}

	@Override
	public void appendEntityReferences(ArrayList<Entity> list) {
		if (value == null)
			return;
		for (int i = 0; i < value.size(); i++) {
			try {
				ExpParser.appendEntityReferences(value.get(i), list);
			}
			catch (ExpError e) {}
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
		for (int i = 0; i < value.size(); i++) {
			if (i > 0)
				sb.append(BRACE_SEPARATOR);
			sb.append("{").append(BRACE_SEPARATOR);
			try {
				ExpResult res = ExpEvaluator.evaluateExpression(value.get(i), thisEnt, simTime);
				sb.append(res.toString());
			}
			catch (ExpError e) {
				throw new ErrorException(thisEnt, getKeyword(), i, e);
			}
			sb.append(BRACE_SEPARATOR).append("}");
		}
		return sb.toString();
	}

	public ExpResult getNextResult(int i, Entity thisEnt, double simTime) {
		try {
			ExpResult ret = ExpEvaluator.evaluateExpression(getValue().get(i), thisEnt, simTime);
			if (ret.type != resType)
				throw new ExpError(parseContextList.get(i).getUpdatedSource(), 0, EXP_ERR_RESULT_TYPE,
						ret.type, resType);
			if (ret.type == ExpResType.NUMBER && ret.unitType != unitType)
				throw new ExpError(parseContextList.get(i).getUpdatedSource(), 0, EXP_ERR_UNIT,
						ret.unitType.getSimpleName(), unitType.getSimpleName());
			return ret;
		}
		catch (ExpError e) {
			throw new ErrorException(thisEnt, getKeyword(), i + 1, e);
		}
	}

	public ArrayList<ExpResult> getNextResults(Entity thisEnt, double simTime) {
		ArrayList<ExpResult> ret = new ArrayList<>(getValue().size());
		for (int i = 0; i < getValue().size(); i++) {
			ret.add(getNextResult(i, thisEnt, simTime));
		}
		return ret;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <V> V getValue(Entity thisEnt, double simTime, Class<V> klass) {
		if (getValue() == null)
			return null;
		return (V) getNextResults(thisEnt, simTime);
	}

	@Override
	public Class<?> getReturnType() {
		return ArrayList.class;
	}

}
