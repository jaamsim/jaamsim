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
import java.util.Arrays;

import com.jaamsim.ui.View;

public class VisibilityInfo {
	private static final int[] ALL_VIEWS = new int[0];

	private final int[] viewIDs;
	private final double minDist;
	private final double maxDist;

	public VisibilityInfo(ArrayList<View> views, double minDist, double maxDist) {
		if (views == null || views.size() == 0) {
			viewIDs = ALL_VIEWS;
		}
		else {
			viewIDs = new int[views.size()];
			for (int i = 0; i < views.size(); i++)
				viewIDs[i] = views.get(i).getID();
		}

		this.minDist = minDist;
		this.maxDist = maxDist;
	}

	public final boolean isVisible(int viewID, double dist) {
		// Test the distance is in the visible range
		if (dist < minDist || dist > maxDist)
			return false;

		return isVisible(viewID);
	}

	public final boolean isVisible(int viewID) {
		// If no limitation on views, we must be visible
		if (viewIDs.length == 0)
			return true;

		for (int i = 0; i < viewIDs.length; i++) {
			if (viewIDs[i] == viewID)
				return true;
		}
		return false;
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof VisibilityInfo)) {
			return false;
		}

		VisibilityInfo vi = (VisibilityInfo)o;
		return Arrays.equals(vi.viewIDs, viewIDs) && vi.minDist == minDist && vi.maxDist == maxDist;
	}
}
