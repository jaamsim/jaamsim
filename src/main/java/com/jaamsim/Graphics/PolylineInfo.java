/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2015 Ausenco Engineering Canada Inc.
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
package com.jaamsim.Graphics;

import java.util.ArrayList;

import com.jaamsim.math.Color4d;
import com.jaamsim.math.Vec3d;
import com.jaamsim.render.HasScreenPoints.PointsInfo;

public class PolylineInfo {
	public ArrayList<Vec3d> points;
	public Color4d color;
	public int width; // Line width in pixels

	public PolylineInfo() {}

	public PolylineInfo(ArrayList<Vec3d> pts, Color4d col, int w) {
		points = pts;
		color = col;
		width = w;
	}

	@Override
	public boolean equals(Object o) {
		if (o == this) return true;
		if (!(o instanceof PointsInfo)) return false;

		PointsInfo pi = (PointsInfo)o;

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

}
