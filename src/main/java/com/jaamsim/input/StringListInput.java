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
package com.jaamsim.input;

import java.util.ArrayList;


public class StringListInput extends ListInput<ArrayList<String>> {
	private ArrayList<String> validOptions;

	 // If true convert all the the items to uppercase
	private boolean caseSensitive;

	public StringListInput(String key, String cat, ArrayList<String> def) {
		super(key, cat, def);
		validOptions = null;
		caseSensitive = true;
	}

	@Override
	public void parse(KeywordIndex kw)
	throws InputErrorException {
		Input.assertCountRange(kw, minCount, maxCount);
		if (validOptions != null) {
			value = Input.parseStrings(kw, validOptions, caseSensitive);
			return;
		}

		ArrayList<String> tmp = new ArrayList<String>(kw.numArgs());
		for (int i = 0; i < kw.numArgs(); i++) {
			tmp.add(kw.getArg(i));
		}
		value = tmp;
	}

	@Override
	public int getListSize() {
		if (value == null)
			return 0;
		else
			return value.size();
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