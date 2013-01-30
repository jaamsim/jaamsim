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

import java.util.ArrayList;

/**
 * A RenderProxy is a representation of a renderable object that has no reliance on the
 * actual render state (eg: any kind of OpenGL buffer objects or textures). However,
 * a RenderProxy has enough information to be rendered. The key method is collectRenderables(Renderer, ArrayList<Renderables>), which
 * appends new renderables to a growing scene list.
 * @author Matt.Chudleigh
 *
 */
public interface RenderProxy {

	/**
	 * Create a Renderable object with the help of the renderer. This should only be called in the
	 * render thread.
	 * @param r
	 * @param outList
	 * @return
	 */
	public abstract void collectRenderables(Renderer r, ArrayList<Renderable> outList);

	/**
	 * Collect the renderables that will draw directly to the window overlay (window space)
	 * @param r
	 * @param outList
	 */
	public abstract void collectOverlayRenderables(Renderer r, ArrayList<OverlayRenderable> outList);
}
