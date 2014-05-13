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

import com.jaamsim.input.Input;
import com.jaamsim.input.KeywordIndex;

public class BooleanInput extends Input<Boolean> {

	private static final ArrayList<String> validOptions;

	static {
		validOptions = new ArrayList<String>();
		validOptions.add("TRUE");
		validOptions.add("FALSE");
	}

	/**
	 * Creates a new Boolean Input with the given keyword, category, units, and
	 * default value.
	 */
	public BooleanInput(String key, String cat, boolean def) {
		super(key, cat, Boolean.valueOf(def));
	}

	@Override
	public void parse(KeywordIndex kw)
	throws InputErrorException {
		Input.assertCount(kw, 1);
		value = Boolean.valueOf(Input.parseBoolean(kw.getArg(0)));
	}

	@Override
	public ArrayList<String> getValidOptions() {
		return validOptions;
	}

	@Override
	public String getDefaultString() {
		if (defValue == null)
			return NO_VALUE;

		if (defValue)
			return "TRUE";

		return "FALSE";
	}
}
