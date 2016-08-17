/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2015 Ausenco Engineering Canada Inc.
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
package com.jaamsim.Graphics;

import java.util.ArrayList;

import com.jaamsim.math.Color4d;
import com.jaamsim.math.Vec3d;

public class PolylineInfo {
	private final ArrayList<Vec3d> points;
	private final Color4d color;
	private final int width; // Line width in pixels

	public PolylineInfo(ArrayList<Vec3d> pts, Color4d col, int w) {
		points = pts;
		color = col;
		width = w;
	}

	@Override
	public boolean equals(Object o) {
		if (o == this) return true;
		if (!(o instanceof PolylineInfo)) return false;

		PolylineInfo pi = (PolylineInfo)o;

		return points != null && points.equals(pi.points) &&
		       color != null && color.equals(pi.color) &&
		       width == pi.width;
	}

	public ArrayList<Vec3d> getPoints() {
		return points;
	}

	public Color4d getColor() {
		return color;
	}

	public int getWidth() {
		return width;
	}

	@Override
	public String toString() {
		return points.toString();
	}

}
