package com.jaamsim.render;

import java.util.ArrayList;

import javax.vecmath.Vector3d;

import com.jaamsim.math.Color4d;

public interface HasScreenPoints {

	public ArrayList<Vector3d> getScreenPoints();
	public Color4d getDisplayColour();

	/**
	 * Returns the screen width of the line in pixels
	 * @return
	 */
	public int getWidth();

	public boolean selectable();

}
