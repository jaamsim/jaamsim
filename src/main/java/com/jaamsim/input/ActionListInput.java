/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2013 Ausenco Engineering Canada Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 */
package com.jaamsim.input;

import java.util.ArrayList;

import com.jaamsim.render.Action;
import com.sandwell.JavaSimulation.InputErrorException;
import com.sandwell.JavaSimulation.ListInput;
import com.sandwell.JavaSimulation.StringVector;

public class ActionListInput extends ListInput<ArrayList<Action.Binding>>{

	public ActionListInput(String key, String cat, ArrayList<Action.Binding> def) {
		super(key, cat, def);
	}

	@Override
	public void parse(StringVector input) throws InputErrorException {
		ArrayList<StringVector> splitData = InputAgent.splitStringVectorByBraces(input);
		ArrayList<Action.Binding> bindings = new ArrayList<Action.Binding>(splitData.size());
		for (int i = 0; i < splitData.size(); i++) {
			try {
				bindings.add(parseBinding(splitData.get(i)));
			} catch (InputErrorException e) {
				throw new InputErrorException(INP_ERR_ELEMENT, i, e.getMessage());
			}
		}
		value = bindings;
	}

	private Action.Binding parseBinding(StringVector tokens) throws InputErrorException {
		Input.assertCount(tokens, 2);
		Action.Binding binding = new Action.Binding();
		binding.actionName = tokens.get(0);
		binding.outputName = tokens.get(1);
		return binding;
	}

	@Override
	public String getValueString() {
		if (value == null)
			return "";

		StringBuilder tmp = new StringBuilder();
		for (int i = 0; i < value.size(); i++) {
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

	@Override
	public String getDefaultString() {
		if (defValue == null || defValue.isEmpty())
			return NO_VALUE;

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
