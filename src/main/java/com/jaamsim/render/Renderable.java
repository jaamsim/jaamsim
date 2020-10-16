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

import com.jaamsim.math.AABB;
import com.jaamsim.math.Ray;

public interface Renderable {

/**
 * The actual render call, called after collect for any object that returned true, although
 * possibly in a different order than collected.
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
	 * @param cam - the distance this object is from the camera. This is used for LOD renderables
	 * @return - a boolean indicating this renderable is visible on that window
 */
public boolean renderForView(int viewID, Camera cam);

/**
 * Test for collision with a ray in global space, return the distance to a collision,
 * a negative return value indicates no collision. There is two modes for determining collision, set by 'precise'
 * a precise collision will check down to the individual renderable elements (lines, triangles, etc) and can be quite slow
 * @param r
 */
public double getCollisionDist(Ray r, boolean precise);

} // Interface Renderable
