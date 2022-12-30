/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2013 Ausenco Engineering Canada Inc.
 * Copyright (C) 2021-2022 JaamSim Software Inc.
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
import com.jaamsim.basicsim.JaamSimModel;
import com.jaamsim.render.Action;

public class ActionListInput extends ArrayListInput<Action.Binding>{

	public ActionListInput(String key, String cat, ArrayList<Action.Binding> def) {
		super(key, cat, def);
	}

	@Override
	public void parse(Entity thisEnt, KeywordIndex kw) throws InputErrorException {
		ArrayList<KeywordIndex> subArgs = kw.getSubArgs();
		ArrayList<Action.Binding> bindings = new ArrayList<>(subArgs.size());
		for (int i = 0; i < subArgs.size(); i++) {
			try {
				bindings.add(parseBinding(subArgs.get(i)));
			} catch (InputErrorException e) {
				throw new InputErrorException(INP_ERR_ELEMENT, i, e.getMessage());
			}
		}
		value = bindings;
	}

	private Action.Binding parseBinding(KeywordIndex kw) throws InputErrorException {
		Input.assertCount(kw, 2);
		Action.Binding binding = new Action.Binding();
		binding.actionName = kw.getArg(0);
		binding.outputName = kw.getArg(1);
		return binding;
	}

	@Override
	public String getValidInputDesc() {
		return Input.VALID_ACTION;
	}

	@Override
	public void getValueTokens(ArrayList<String> toks) {
		if (value == null || isDef)
			return;

		for (int i = 0; i < value.size(); i++) {
			Action.Binding b = value.get(i);
			toks.add("{");
			toks.add(b.actionName);
			toks.add(b.outputName);
			toks.add("}");
		}
	}

	@Override
	public boolean useExpressionBuilder() {
		return true;
	}

	@Override
	public String getDefaultString(JaamSimModel simModel) {
		if (defValue == null || defValue.isEmpty())
			return "";

		StringBuilder tmp = new StringBuilder();
		for (int i = 0; i < defValue.size(); i++) {
			// Separate each action
			if (i > 0) tmp.append(SEPARATOR);

			Action.Binding b = value.get(i);
			tmp.append("{ ");
			tmp.append(b.actionName);
			tmp.append(SEPARATOR);
			tmp.append(b.outputName);
			tmp.append(" }");
		}
		return tmp.toString();
	}
}
