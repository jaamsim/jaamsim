package com.jaamsim.render;

import com.jaamsim.Graphics.PolylineInfo;

public interface HasScreenPoints {

	public static class PointsInfo extends PolylineInfo {}

	public PointsInfo[] getScreenPoints();

	public boolean selectable();

}
