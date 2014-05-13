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

import com.sandwell.JavaSimulation.InputErrorException;
import com.sandwell.JavaSimulation.StringInput;
import com.sandwell.JavaSimulation.StringVector;

public class FormatInput extends StringInput {
	public FormatInput(String key, String cat, String def) {
		super(key, cat, def);
	}

	@Override
	public void parse(StringVector input) throws InputErrorException {
		Input.assertCount(input, 1);
		String temp = input.get(0);
		try {
			String.format(temp, 0.0d);
		}
		catch (Throwable e) {
			throw new InputErrorException("Invalid Java format string: %s", temp);
		}

		value = temp;
	}
}
