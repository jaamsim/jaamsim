/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2014 Ausenco Engineering Canada Inc.
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
package com.jaamsim.font;

import java.util.HashMap;

import com.jaamsim.math.Color4d;
import com.jaamsim.math.Mat4d;
import com.jaamsim.math.Ray;
import com.jaamsim.math.Vec2d;
import com.jaamsim.math.Vec3d;
import com.jaamsim.math.Vec4d;
import com.jaamsim.render.Camera;
import com.jaamsim.render.OverlayRenderable;
import com.jaamsim.render.RenderUtils;
import com.jaamsim.render.Renderer;
import com.jaamsim.render.Shader;
import com.jaamsim.render.VisibilityInfo;
import com.jogamp.opengl.GL2GL3;

public class BillboardString implements OverlayRenderable {

	private final TessFont _font;
	private final String _contents;

	private final float[] _color;

	private final double _height;
	private final Vec3d _pos;
	private final double _xOffset, _yOffset;
	private final VisibilityInfo _visInfo;
	private final long _pickingID;

	private final Mat4d tempViewMat = new Mat4d();
	private final Vec4d tempPos = new Vec4d();

	private static HashMap<Integer, Integer> VAOMap = new HashMap<>();

	public BillboardString(TessFont font, String contents, Color4d color,
            double height, Vec3d pos, double xOffset, double yOffset, VisibilityInfo visInfo, long pickingID) {
		_font = font;
		_contents = contents;
		_color = color.toFloats();
		_height = height;
		_xOffset = xOffset;
		_yOffset = yOffset;
		_pos = pos;
		_visInfo = visInfo;
		_pickingID = pickingID;
	}

	/**
	 * Note: render() and renderForView() are mutually non-reentrant due to shared temporaries. This should be fine
	 * because neither should ever be called by any thread other than the render thread.
	 */
	@Override
	public void render(int contextID, Renderer renderer, double windowWidth,
			double windowHeight, Camera cam, Ray pickRay) {

		GL2GL3 gl = renderer.getGL();

		if (!VAOMap.containsKey(contextID)) {
			setupVAO(contextID, renderer);
		}

		int vao = VAOMap.get(contextID);
		gl.glBindVertexArray(vao);

		// Render the string
		Shader s = renderer.getShader(Renderer.ShaderHandle.OVERLAY_FONT);

		s.useShader(gl);
		int prog = s.getProgramHandle();

		// Work out the billboard position
		cam.getViewMat4d(tempViewMat);
		// Build up the projection*view matrix
		tempViewMat.mult4(cam.getProjMat4d(), tempViewMat);

		tempPos.x = _pos.x;
		tempPos.y = _pos.y;
		tempPos.z = _pos.z;
		tempPos.w = 1.0;

		tempPos.mult4(tempViewMat, tempPos);
		tempPos.x /= tempPos.w;
		tempPos.y /= tempPos.w;
		// TempPos x and y are now in normalized coordinate space (after the projection)

		int colorVar = gl.glGetUniformLocation(prog, "color");
		gl.glUniform4fv(colorVar, 1, _color, 0);

		int posVar = gl.glGetAttribLocation(prog, "position");
		gl.glEnableVertexAttribArray(posVar);

		gl.glBindBuffer(GL2GL3.GL_ARRAY_BUFFER, _font.getGLBuffer(gl));
		gl.glVertexAttribPointer(posVar, 2, GL2GL3.GL_FLOAT, false, 0, 0);
		gl.glBindBuffer(GL2GL3.GL_ARRAY_BUFFER, 0);

		int offsetVar = gl.glGetUniformLocation(prog, "offset");

		float scaleY = (float)(2 * _height / (windowHeight * _font.getNominalHeight()));
		float scaleX = scaleY * (float)(windowHeight/windowWidth);

		int scaleVar = gl.glGetUniformLocation(prog, "scale");
		gl.glUniform2f(scaleVar, scaleX, scaleY);

		float offsetX = (float)tempPos.x;
		float offsetY = (float)tempPos.y;

		offsetX += _xOffset*2.0/windowWidth;
		offsetY += _yOffset*2.0/windowHeight;

		gl.glDisable(GL2GL3.GL_CULL_FACE);

		for (int cp : RenderUtils.stringToCodePoints(_contents)) {
			TessChar tc = _font.getTessChar(cp);
			if (tc == null) {
				assert(false);
				continue;
			}

			gl.glUniform2f(offsetVar, offsetX, offsetY);

			gl.glDrawArrays(GL2GL3.GL_TRIANGLES, tc.getStartIndex(), tc.getNumVerts());

			offsetX += tc.getAdvance()*scaleX;
		}

		gl.glEnable(GL2GL3.GL_CULL_FACE);

	}

	private void setupVAO(int contextID, Renderer renderer) {
		GL2GL3 gl = renderer.getGL();

		int vao = renderer.generateVAO(contextID, gl);
		VAOMap.put(contextID, vao);
	}


	@Override
	public boolean renderForView(int viewID, Camera cam) {
		// Check the view
		if (!_visInfo.isVisible(viewID)) {
			return false;
		}

		// Render if the billboard is in front of the camera
		cam.getViewMat4d(tempViewMat);

		// tempPos is now the billboard in normalized coordinate space
		tempPos.multAndTrans3(tempViewMat, _pos);

		return tempPos.z < 0;
	}

	@Override
	public long getPickingID() {
		return _pickingID;
	}

	@Override
	public boolean collides(Vec2d coords, double windowWidth, double windowHeight, Camera cam) {

		// Work out the billboard position
		cam.getViewMat4d(tempViewMat);
		// Build up the projection*view matrix
		tempViewMat.mult4(cam.getProjMat4d(), tempViewMat);

		tempPos.x = _pos.x;
		tempPos.y = _pos.y;
		tempPos.z = _pos.z;
		tempPos.w = 1.0;

		tempPos.mult4(tempViewMat, tempPos);
		tempPos.x /= tempPos.w;
		tempPos.y /= tempPos.w;
		// TempPos x and y are now in normalized coordinate space (after the projection)

		// x and y in [0,1]
		double x = (tempPos.x + 1.0)/2.0;
		double y = (tempPos.y + 1.0)/2.0;

		// x and y in pixels
		x *= windowWidth;
		y *= windowHeight;

		Vec3d renderedSize = _font.getStringSize(_height, _contents);

		boolean inX = (coords.x > x) && (coords.x < x + renderedSize.x);
		boolean inY = (coords.y > y) && (coords.y < y + renderedSize.y);

		return inX && inY;

	}
}
