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
		ArrayList<String> strings = new ArrayList<String>(input.size());
		for (String s : input) {
			strings.add(s);
		}

		value = new ArrayList<Action.Binding>();

		ArrayList<ArrayList<String>> bindings = InputAgent.splitForNestedBraces(strings);
		for( ArrayList<String> b : bindings) {
			Action.Binding binding = parseBinding(b);
			value.add(binding);
		}
	}

	public Action.Binding parseBinding(ArrayList<String> tokens) {
		if (tokens.size() != 4|| !tokens.get(0).equals("{") || !tokens.get(3).equals("}")) {
			throw new InputErrorException("Malformed binding entry: %s", tokens.toString());
		}

		Action.Binding binding = new Action.Binding();
		binding.actionName = tokens.get(1);
		binding.outputName = tokens.get(2);
		return binding;
	}
}
