/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2012 Ausenco Engineering Canada Inc.
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
package com.jaamsim.render;

import java.util.ArrayList;

public class VisibilityInfo {

	public static VisibilityInfo ALWAYS = new VisibilityInfo(new ArrayList<Integer>(), Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);

	public final ArrayList<Integer> viewIDs;
	public final double minDist;
	public final double maxDist;

	public VisibilityInfo(ArrayList<Integer> viewIDs, double minDist, double maxDist) {
		this.viewIDs = viewIDs;
		this.minDist = minDist;
		this.maxDist = maxDist;
	}
}
