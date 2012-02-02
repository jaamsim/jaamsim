/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2010-2011 Ausenco Engineering Canada Inc.
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
package com.sandwell.JavaSimulation;

import java.util.ArrayList;

public class StringChoiceInput extends IntegerInput {
	private ArrayList<String> choices;

	{
		choices = new ArrayList<String>();
	}

	public StringChoiceInput(String key, String cat, Integer def) {
		super(key, cat, def);
	}

	public void parse(StringVector input)
	throws InputErrorException {
		Input.assertCount(input, 1);
		String temp = Input.parseString(input.get(0), choices);
		value = choices.indexOf( temp );
		this.updateEditingFlags();
	}

	public void addChoice(String choice) {
		if (!choices.contains(choice))
			choices.add(choice);
	}

	public String getChoice() {
		return choices.get(value);
	}

	public void setChoices(ArrayList<String> list) {
		choices = list;
	}
}
