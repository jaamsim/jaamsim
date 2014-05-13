/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2011 Ausenco Engineering Canada Inc.
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

import com.jaamsim.input.Input;

public class StringListInput extends ListInput<StringVector> {
	private ArrayList<String> validOptions;

	 // If true convert all the the items to uppercase
	private boolean caseSensitive;

	public StringListInput(String key, String cat, StringVector def) {
		super(key, cat, def);
		validOptions = null;
		caseSensitive = true;
	}

	@Override
	public void parse(StringVector input)
	throws InputErrorException {
		Input.assertCountRange(input, minCount, maxCount);
		value = input;
	}

	public void setValidOptions(ArrayList<String> list) {
		validOptions = list;
	}

	public void setCaseSensitive(boolean bool) {
		caseSensitive = bool;
	}

	public boolean getCaseSensitive() {
		return caseSensitive;
	}

	@Override
	public ArrayList<String> getValidOptions() {
		return validOptions;
	}

	@Override
	public String getDefaultString() {
		if (defValue == null)
			return NO_VALUE;

		if (defValue.size() == 0)
			return NO_VALUE;

		StringBuilder tmp = new StringBuilder(defValue.get(0));
		for (int i = 1; i < defValue.size(); i++) {
			tmp.append(SEPARATOR);
			tmp.append(defValue.get(i));
		}

		return tmp.toString();
	}
}