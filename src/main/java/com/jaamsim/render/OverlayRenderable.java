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

import com.jaamsim.math.Ray;
import com.jaamsim.math.Vec2d;


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

	public long getPickingID();

	public boolean collides(Vec2d coords, double windowWidth, double windowHeight, Camera cam);
}
