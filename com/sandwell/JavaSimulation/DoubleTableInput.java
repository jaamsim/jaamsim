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

public class DoubleTableInput extends Input<ArrayList<DoubleVector>> {

	public DoubleTableInput(String key, String cat, ArrayList<DoubleVector> def) {
		super(key, cat, def);
	}

	public void parse(StringVector input) throws InputErrorException {
		ArrayList<StringVector> temp = Util.splitStringVectorByBraces(input);

		for (StringVector each : temp) {
			value.add(Input.parseDoubleVector(each, 0.0d, Double.POSITIVE_INFINITY));
		}
		this.updateEditingFlags();
	}
}
