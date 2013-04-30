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

	private final ArrayList<Integer> viewIDs;
	private final double minDist;
	private final double maxDist;

	public VisibilityInfo(ArrayList<Integer> viewIDs, double minDist, double maxDist) {
		this.viewIDs = viewIDs;
		this.minDist = minDist;
		this.maxDist = maxDist;
	}

	public final boolean isVisible(int viewID, double dist) {
		// Test the distance is in the visible range
		if (dist < minDist || dist > maxDist)
			return false;

		// If no limitation on views, we must be visible
		if (viewIDs == null || viewIDs.size() == 0)
			return true;

		return viewIDs.contains(viewID);
	}

	public final boolean isVisible(int viewID) {
		// If no limitation on views, we must be visible
		if (viewIDs == null || viewIDs.size() == 0)
			return true;

		return viewIDs.contains(viewID);
	}
}
