package com.jaamsim.render;

import java.util.ArrayList;

import com.jaamsim.math.Color4d;
import com.jaamsim.math.Vec3d;

public interface HasScreenPoints {

	public static class PointsInfo {
		public ArrayList<Vec3d> points;
		public Color4d color;
		public int width; // Line width in pixels
	}

	public PointsInfo[] getScreenPoints();

	public boolean selectable();

}
