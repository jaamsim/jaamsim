/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2014 Ausenco Engineering Canada Inc.
 * Copyright (C) 2016-2026 JaamSim Software Inc.
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

public class ExpressionInput extends Input<Expression> {
	private Class<? extends Unit> unitType;
	private EntityParseContext parseContext;
	private ExpResType resType;

	public ExpressionInput(String key, String cat, Expression def) {
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
		return Parser.addQuotesIfNeeded(str);
	}

	@Override
	public void parse(Entity thisEnt, KeywordIndex kw)
	throws InputErrorException {
		Input.assertCount(kw, 1);
		try {
			String expString = kw.getArg(0);

			ExpEvaluator.EntityParseContext pc = ExpEvaluator.getParseContext(thisEnt, expString);
			Expression exp = ExpParser.parseExpression(pc, expString);
			ExpParser.assertUnitType(exp, unitType);
			if (resType != null)
				ExpParser.assertResultType(exp, resType);

			// Save the expression
			parseContext = pc;
			value = exp;
			this.setValid(true);

		} catch (ExpError e) {
			throw new InputErrorException(e);
		}
	}

	@Override
	public String getValidInputDesc() {

		if (resType == ExpResType.NUMBER) {
			if (unitType == DimensionlessUnit.class)
				return VALID_EXP_DIMLESS;
			else
				return VALID_EXP_NUM;
		}

		if (resType == ExpResType.STRING)
			return VALID_EXP_STR;

		if (resType == ExpResType.ENTITY)
			return VALID_EXP_ENT;

		if (resType == ExpResType.COLLECTION)
			return VALID_EXP_COL;

		return VALID_EXP;
	}

	@Override
	public void getValueTokens(ArrayList<String> toks) {
		if (value == null || isDef)
			return;
		toks.add(parseContext.getUpdatedSource());
	}

	@Override
	public void appendEntityReferences(ArrayList<Entity> list) {
		if (value == null)
			return;
		try {
			ExpParser.appendEntityReferences(value, list);
		}
		catch (ExpError e) {}
	}

	@Override
	public boolean useExpressionBuilder() {
		return true;
	}

	@Override
	public String getPresentValueString(Entity thisEnt, double simTime) {
		if (value == null)
			return "";

		try {
			ExpResult res = ExpEvaluator.evaluateExpression(value, thisEnt, simTime);
			return res.toString();
		}
		catch (ExpError e) {
			return getValueString();
		}
	}

	public ExpResult getNextResult(Entity thisEnt, double simTime) {
		try {
			ExpResult ret = ExpEvaluator.evaluateExpression(getValue(), thisEnt, simTime);
			if (ret.type != resType)
				throw new ExpError(parseContext.getUpdatedSource(), 0, EXP_ERR_RESULT_TYPE,
						ret.type, resType);
			if (ret.type == ExpResType.NUMBER && ret.unitType != unitType)
				throw new ExpError(parseContext.getUpdatedSource(), 0, EXP_ERR_UNIT,
						ret.unitType.getSimpleName(), unitType.getSimpleName());
			return ret;
		}
		catch (ExpError e) {
			throw new ErrorException(thisEnt, getKeyword(), e);
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public <V> V getValue(Entity thisEnt, double simTime, Class<V> klass) {
		if (getValue() == null)
			return null;
		return (V) getNextResult(thisEnt, simTime);
	}

	@Override
	public Class<?> getReturnType() {
		return ExpResult.class;
	}

}
