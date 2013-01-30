package com.jaamsim.render;

import java.util.ArrayList;

import com.jaamsim.math.Color4d;
import com.jaamsim.math.Vec3d;

public interface HasScreenPoints {

	public ArrayList<Vec3d> getScreenPoints();
	public Color4d getDisplayColour();

	/**
	 * Returns the screen width of the line in pixels
	 * @return
	 */
	public int getWidth();

	public boolean selectable();

}
