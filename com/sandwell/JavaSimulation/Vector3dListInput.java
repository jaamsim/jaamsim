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

import javax.vecmath.Vector3d;

public class Vector3dListInput extends ListInput<ArrayList<Vector3d>> {

	public Vector3dListInput(String key, String cat, ArrayList<Vector3d> def) {
		super(key, cat, def);
	}

	public void parse(StringVector input)
	throws InputErrorException {

		// Check if number of outer lists violate minCount or maxCount
		ArrayList<StringVector> splitData = Util.splitStringVectorByBraces(input);
		if (splitData.size() < minCount || splitData.size() > maxCount)
			throw new InputErrorException(INP_ERR_RANGECOUNT, minCount, maxCount, input.toString());

		ArrayList<Vector3d> tempValue = new ArrayList<Vector3d>();

		for( StringVector innerInput : splitData ) {
			Vector3d temp = Input.parseVector3d(innerInput);
			tempValue.add(temp);
		}

		value = tempValue;
	}
}
