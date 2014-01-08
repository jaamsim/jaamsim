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

import com.jaamsim.math.Ray;


/**
 * OverlayRenderable is for renderable objects that render to a screen space overlay
 * instead of 3D space. This is for UI, HUDs, etc.
 * @author Matt.Chudleigh
 *
 */
public interface OverlayRenderable {

	/**
	 * Render in screen space
	 * @param vaoMap
	 * @param renderer
	 * @param windowWidth - the width of the current window
	 * @param windowHeight - the height of the current window
	 * @param cam - the camera object for the current view
	 * @param pickRay - the ray representing the current mouse projected into the 3D scene, may be null
	 */
	public void render(int contextID, Renderer renderer,
	                   double windowWidth, double windowHeight, Camera cam, Ray pickRay);

	/**
	 * Returns if this renderable should be rendered for the listed window, most implementations will hard code this to true
	 * @param windowID - the ID of the window be queried about
	 * @return - a boolean indicating this renderable is visible on that window
	 */
	public boolean renderForView(int windowID, Camera cam);

}
