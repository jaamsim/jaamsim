package com.jaamsim.render;

import java.util.ArrayList;

import com.jaamsim.math.Color4d;
import com.jaamsim.math.Vec3d;

public interface HasScreenPoints {

	public static class PointsInfo {
		public ArrayList<Vec3d> points;
		public Color4d color;
		public int width; // Line width in pixels

		@Override
		public boolean equals(Object o) {
			if (o == this) return true;
			if (!(o instanceof PointsInfo)) return false;

			PointsInfo pi = (PointsInfo)o;

			return points != null && points.equals(pi.points) &&
			       color != null && color.equals(pi.color) &&
			       width == pi.width;
		}
	}

	public PointsInfo[] getScreenPoints();

	public boolean selectable();

}
