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
	 */
	public abstract void collectRenderables(Renderer r, ArrayList<Renderable> outList);

	/**
	 * Collect the renderables that will draw directly to the window overlay (window space)
	 * @param r
	 * @param outList
	 */
	public abstract void collectOverlayRenderables(Renderer r, ArrayList<OverlayRenderable> outList);
}
