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

import java.net.URI;
import java.nio.FloatBuffer;
import java.util.HashMap;

import com.jaamsim.math.Ray;
import com.jaamsim.math.Vec2d;
import com.jogamp.opengl.GL2GL3;

public class OverlayTexture implements OverlayRenderable {

	// Position and size, specified in pixels, origin at the bottom left
	private int _x, _y;
	private int _width, _height;

	private URI _imageURI;

	private boolean _isTransparent;
	private boolean _isCompressed;
	private long _pickingID;

	static private boolean staticInit = false;
	static private int vertBuff;
	static private int texCoordBuff;
	static private HashMap<Integer, Integer> VAOMap = new HashMap<>();

	static private int progHandle;

	static private int offsetVar;
	static private int sizeVar;
	static private int texVar;
	static private int hasTexVar;

	private boolean _alignRight, _alignBottom;
	private VisibilityInfo _visInfo;

	public OverlayTexture(int x, int y, int width, int height, URI imageURI, boolean transparent, boolean compressed,
	                      boolean alignRight, boolean alignBottom, VisibilityInfo visInfo, long pickingID) {
		_x = x; _y = y;
		_width = width; _height = height;
		_imageURI = imageURI;
		_isTransparent = transparent; _isCompressed = compressed;
		_alignRight = alignRight; _alignBottom = alignBottom;
		_visInfo = visInfo;
		_pickingID = pickingID;
	}

	private static void initStaticBuffers(Renderer r) {
		GL2GL3 gl = r.getGL();

		int[] buffs = new int[2];
		gl.glGenBuffers(2, buffs, 0);
		vertBuff = buffs[0];
		texCoordBuff = buffs[1];

		FloatBuffer verts = FloatBuffer.allocate(6*3); // 2 triangles * 3 coordinates
		verts.put(0.0f); verts.put(0.0f); verts.put(0.0f);
		verts.put(1.0f); verts.put(0.0f); verts.put(0.0f);
		verts.put(1.0f); verts.put(1.0f); verts.put(0.0f);

		verts.put(0.0f); verts.put(0.0f); verts.put(0.0f);
		verts.put(1.0f); verts.put(1.0f); verts.put(0.0f);
		verts.put(0.0f); verts.put(1.0f); verts.put(0.0f);

		verts.flip();
		gl.glBindBuffer(GL2GL3.GL_ARRAY_BUFFER, vertBuff);
		gl.glBufferData(GL2GL3.GL_ARRAY_BUFFER, 6*3*4, verts, GL2GL3.GL_STATIC_DRAW);

		FloatBuffer texCoords = FloatBuffer.allocate(6*2); // 2 triangles * 2 coordinates

		texCoords.put(0.0f); texCoords.put(0.0f);
		texCoords.put(1.0f); texCoords.put(0.0f);
		texCoords.put(1.0f); texCoords.put(1.0f);

		texCoords.put(0.0f); texCoords.put(0.0f);
		texCoords.put(1.0f); texCoords.put(1.0f);
		texCoords.put(0.0f); texCoords.put(1.0f);

		texCoords.flip();
		gl.glBindBuffer(GL2GL3.GL_ARRAY_BUFFER, texCoordBuff);
		gl.glBufferData(GL2GL3.GL_ARRAY_BUFFER, 6*2*4, texCoords, GL2GL3.GL_STATIC_DRAW);

		// Initialize the shader variables
		progHandle = r.getShader(Renderer.ShaderHandle.OVERLAY_FLAT).getProgramHandle();

		texVar = gl.glGetUniformLocation(progHandle, "tex");
		hasTexVar = gl.glGetUniformLocation(progHandle, "useTex");
		sizeVar = gl.glGetUniformLocation(progHandle, "size");
		offsetVar = gl.glGetUniformLocation(progHandle, "offset");

		staticInit = true;
	}

	private void setupVAO(int contextID, Renderer renderer) {
		GL2GL3 gl = renderer.getGL();

		int vao = renderer.generateVAO(contextID, gl);
		VAOMap.put(contextID, vao);

		gl.glBindVertexArray(vao);


		// Position
		int posVar = gl.glGetAttribLocation(progHandle, "position");
		gl.glEnableVertexAttribArray(posVar);

		gl.glBindBuffer(GL2GL3.GL_ARRAY_BUFFER, vertBuff);
		gl.glVertexAttribPointer(posVar, 3, GL2GL3.GL_FLOAT, false, 0, 0);

		// TexCoords
		int texCoordVar = gl.glGetAttribLocation(progHandle, "texCoordVert");
		gl.glEnableVertexAttribArray(texCoordVar);

		gl.glBindBuffer(GL2GL3.GL_ARRAY_BUFFER, texCoordBuff);
		gl.glVertexAttribPointer(texCoordVar, 2, GL2GL3.GL_FLOAT, false, 0, 0);

		gl.glBindVertexArray(0);
	}


	@Override
	public void render(int contextID, Renderer renderer,
			double windowWidth, double windowHeight, Camera cam, Ray pickRay) {

		if (!staticInit) {
			initStaticBuffers(renderer);
		}

		double x = _x;
		double y = _y;
		if (_alignRight) {
			x = windowWidth - _x - _width;
		}
		if (!_alignBottom) {
			y = windowHeight - _y - _height;
		}

		GL2GL3 gl = renderer.getGL();

		int textureID = renderer.getTexCache().getTexID(gl, _imageURI, _isTransparent, _isCompressed, false);

		if (textureID == TexCache.LOADING_TEX_ID) {
			return; // This texture is not ready yet
		}

		if (!VAOMap.containsKey(contextID)) {
			setupVAO(contextID, renderer);
		}

		int vao = VAOMap.get(contextID);
		gl.glBindVertexArray(vao);

		gl.glUseProgram(progHandle);

		gl.glUniform1i(hasTexVar, 1);

		gl.glActiveTexture(GL2GL3.GL_TEXTURE0);
		gl.glBindTexture(GL2GL3.GL_TEXTURE_2D, textureID);
		gl.glTexParameteri(GL2GL3.GL_TEXTURE_2D, GL2GL3.GL_TEXTURE_WRAP_S, GL2GL3.GL_CLAMP_TO_EDGE);
		gl.glTexParameteri(GL2GL3.GL_TEXTURE_2D, GL2GL3.GL_TEXTURE_WRAP_T, GL2GL3.GL_CLAMP_TO_EDGE);
		gl.glUniform1i(texVar, 0);

		if (_isTransparent) {
			gl.glEnable(GL2GL3.GL_BLEND);
			gl.glBlendEquationSeparate(GL2GL3.GL_FUNC_ADD, GL2GL3.GL_MAX);
			gl.glBlendFuncSeparate(GL2GL3.GL_SRC_ALPHA, GL2GL3.GL_ONE_MINUS_SRC_ALPHA, GL2GL3.GL_ONE, GL2GL3.GL_ONE);
		}

		// Set the size and scale in normalized (-1, 1) coordinates
		double normX = x / (0.5 *  windowWidth) - 1;
		double normY = y / (0.5 * windowHeight) - 1;

		double normWidth  =  _width / (0.5 *  windowWidth);
		double normHeight = _height / (0.5 * windowHeight);

		gl.glUniform2f(offsetVar, (float)normX, (float)normY);
		gl.glUniform2f(sizeVar, (float)normWidth, (float)normHeight);

		// Draw
		gl.glDisable(GL2GL3.GL_CULL_FACE);
		gl.glDrawArrays(GL2GL3.GL_TRIANGLES, 0, 6);
		gl.glEnable(GL2GL3.GL_CULL_FACE);

		if (_isTransparent) {
			gl.glDisable(GL2GL3.GL_BLEND);
		}

	}

	@Override
	public boolean renderForView(int viewID, Camera cam) {
		return _visInfo.isVisible(viewID);
	}

	@Override
	public long getPickingID() {
		return _pickingID;
	}

	@Override
	public boolean collides(Vec2d coords, double windowWidth, double windowHeight, Camera cam) {

		double x = _x;
		double y = _y;
		if (_alignRight) {
			x = windowWidth - _x - _width;
		}
		if (!_alignBottom) {
			y = windowHeight - _y - _height;
		}

		boolean inX = (coords.x > x) && (coords.x < x + _width);
		boolean inY = (coords.y > y) && (coords.y < y + _height);

		return inX && inY;
	}
}
