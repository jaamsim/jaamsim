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

import com.jaamsim.math.AABB;
import com.jaamsim.math.Ray;

public interface Renderable {

/**
 * The actual render call, called after collect for any object that returned true, although
 * possibly in a different order than collected.
 * @param toEyeSpace - Transform from model space to eye space
 * @param cam - the camera used to render
 */
public void render(int contextID, Renderer renderer, Camera cam, Ray pickRay);
public void renderTransparent(int contextID, Renderer renderer, Camera cam, Ray pickRay);

public long getPickingID();

public AABB getBoundsRef();

public boolean hasTransparent();

/**
 * Returns if this renderable should be rendered for the listed window, most implementations will hard code this to true
	 * @param viewID - the ID of the window be queried about
	 * @param dist - the distance this object is from the camera. This is used for LOD renderables
	 * @return - a boolean indicating this renderable is visible on that window
 */
public boolean renderForView(int viewID, Camera cam);

/**
 * Test for collision with a ray in global space, return the distance to a collision,
 * a negative return value indicates no collision. There is two modes for determining collision, set by 'precise'
 * a precise collision will check down to the individual renderable elements (lines, triangles, etc) and can be quite slow
 * @param r
 * @return
 */
public double getCollisionDist(Ray r, boolean precise);

} // Interface Renderable
