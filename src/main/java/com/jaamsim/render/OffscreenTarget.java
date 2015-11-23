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

import java.nio.IntBuffer;

/**
 * The OffscreenTarget is an optional resource handle for offscreen rendering. Using an offscreen target prevents the
 * renderer from needing to allocate render buffer information
 * @author matt.chudleigh
 *
 */
public class OffscreenTarget {

	private int _width;
	private int _height;

	private boolean _loaded;

	// openGL handles to the frame buffer object, colour texture and depth render buffer respectively
	private int _drawFBO;
	private int _drawTex;
	private int _depthHandle;

	// A frame buffer and texture that are allocated single sample to blit to when done
	private int _blitFBO;
	private int _blitTex;

	private IntBuffer _pixelBuffer;

	/**
	 * Builds an OffScreenTarget of the specified size, the resources are not allocated right away, but will be before
	 * this can be used. As such it is safe to use this target immediately after being created.
	 *
	 * @param width
	 * @param height
	 */
	public OffscreenTarget(int width, int height) {
		assert(width > 0);
		assert(height > 0);

		_width = width;
		_height = height;
		_loaded = false;
	}

	public void load(int drawFBO, int drawTex, int depthBuff, int blitFBO, int blitTex) {
		assert !_loaded;

		_loaded = true;
		_drawFBO = drawFBO;
		_drawTex = drawTex;
		_depthHandle = depthBuff;
		_blitFBO = blitFBO;
		_blitTex = blitTex;
		_pixelBuffer = IntBuffer.allocate(_width * _height);

	}

	public int getDrawFBO() {
		assert _loaded;
		return _drawFBO;
	}

	public int getDrawTex() {
		assert _loaded;
		return _drawTex;
	}

	public int getDepthBuffer() {
		assert _loaded;
		return _depthHandle;
	}

	public int getBlitFBO() {
		assert _loaded;
		return _blitFBO;
	}

	public int getBlitTex() {
		assert _loaded;
		return _blitTex;
	}

	public boolean isLoaded() {
		return _loaded;
	}

	public void free() {
		_loaded = false;
		_pixelBuffer = null;
	}

	public int getWidth() {
		return _width;
	}

	public int getHeight() {
		return _height;
	}

	public IntBuffer getPixelBuffer() {
		return _pixelBuffer;
	}
}
