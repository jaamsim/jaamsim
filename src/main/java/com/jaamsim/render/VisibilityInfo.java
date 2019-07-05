/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2012 Ausenco Engineering Canada Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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

		if (viewID < 0)
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

	public double getMinDist() {
		return minDist;
	}
	public double getMaxDist() {
		return maxDist;
	}
}
