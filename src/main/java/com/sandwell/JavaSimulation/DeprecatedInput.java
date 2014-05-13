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

import com.jaamsim.input.Input;
import com.jaamsim.input.InputAgent;

public class DeprecatedInput extends Input<String> {
	private boolean fatal;

	public DeprecatedInput(String key, String msg) {
		super(key, "", "");
		value = msg;
		fatal = true;
	}

	public void setFatal(boolean fatal) {
		this.fatal = fatal;
	}

	@Override
	public void parse(StringVector input)
	throws InputErrorException {
		if (fatal)
			throw new InputErrorException(value);
		else
			InputAgent.logWarning("%s - %s", this.getKeyword(), value);
	}
}
