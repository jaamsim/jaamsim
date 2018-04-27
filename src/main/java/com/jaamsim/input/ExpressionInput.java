/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2014 Ausenco Engineering Canada Inc.
 * Copyright (C) 2016-2018 JaamSim Software Inc.
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
import com.jaamsim.input.ExpParser.Expression;

public class ExpressionInput extends Input<ExpParser.Expression> {
	private Entity thisEnt;
	private ExpEvaluator.EntityParseContext parseContext;

	public ExpressionInput(String key, String cat, ExpParser.Expression def) {
		super(key, cat, def);
	}

	public void setEntity(Entity ent) {
		thisEnt = ent;
	}

	@Override
	public void copyFrom(Input<?> in) {
		super.copyFrom(in);

		// An expression input must be re-parsed to reset the entity referred to by "this"
		parseFrom(in);
	}

	@Override
	public String applyConditioning(String str) {
		return Parser.addQuotesIfNeeded(str);
	}

	@Override
	public void parse(KeywordIndex kw)
	throws InputErrorException {
		Input.assertCount(kw, 1);
		try {
			String expString = kw.getArg(0);

			ExpEvaluator.EntityParseContext pc = ExpEvaluator.getParseContext(thisEnt, expString);
			Expression exp = ExpParser.parseExpression(pc, expString);

			// Save the expression
			parseContext = pc;
			value = exp;

		} catch (ExpError e) {
			throw new InputErrorException(e);
		}
	}

	@Override
	public String getValidInputDesc() {
		return Input.VALID_EXP;
	}

	@Override
	public void getValueTokens(ArrayList<String> toks) {
		if (value == null) return;
		toks.add(parseContext.getUpdatedSource());
	}

	@Override
	public boolean useExpressionBuilder() {
		return true;
	}

}
