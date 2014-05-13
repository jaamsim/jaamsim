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
import com.jaamsim.input.InputAgent;

public class CompatInput extends Input<String> {
	Entity target;
	private boolean appendable;

	public CompatInput(Entity target, String key, String cat, String def) {
		super(key, cat, def);
		this.target = target;

		appendable = false;
	}

	@Override
	public void parse(StringVector input)
	throws InputErrorException {
		if (isAppendable()) {
			ArrayList<StringVector> split = InputAgent.splitStringVectorByBraces(input);
			for (StringVector each : split)
				this.innerParse(each);
		}
		else {
			innerParse(input);
		}
	}

	private void innerParse(StringVector input) {
		target.readData_ForKeyword(input, this.getKeyword());
	}

	void setAppendable(boolean bool) {
		appendable = bool;
	}

	boolean isAppendable() {
		return appendable;
	}
}
