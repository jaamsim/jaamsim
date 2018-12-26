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
package com.jaamsim.font;

import java.util.HashMap;

import com.jaamsim.math.Color4d;
import com.jaamsim.math.Ray;
import com.jaamsim.math.Vec2d;
import com.jaamsim.math.Vec3d;
import com.jaamsim.render.Camera;
import com.jaamsim.render.OverlayRenderable;
import com.jaamsim.render.RenderUtils;
import com.jaamsim.render.Renderer;
import com.jaamsim.render.Shader;
import com.jaamsim.render.VisibilityInfo;
import com.jogamp.opengl.GL2GL3;

public class OverlayString implements OverlayRenderable {

	private TessFont _font;
	private String _contents;

	private final float[] _color;

	private double _height;
	private double _x, _y;
	private boolean _alignRight, _alignBottom;
	private VisibilityInfo _visInfo;
	private final long _pickingID;

	private static HashMap<Integer, Integer> VAOMap = new HashMap<>();

	public OverlayString(TessFont font, String contents, Color4d color,
	                     double height, double x, double y,
	                     boolean alignRight, boolean alignBottom, VisibilityInfo visInfo, long pickingID) {
		_font = font;
		_contents = contents;
		if (_contents == null) {
			_contents = "";
		}
		_color = color.toFloats();
		_height = height;
		_x = x; _y = y;
		_alignRight = alignRight; _alignBottom = alignBottom;
		_visInfo = visInfo;
		_pickingID = pickingID;
	}

	@Override
	public void render(int contextID, Renderer renderer,
		double windowWidth, double windowHeight, Camera cam, Ray pickRay) {


		Vec3d renderedSize = _font.getStringSize(_height, _contents);
		double x = _x;
		double y = _y;
		if (_alignRight) {
			x = windowWidth - _x - renderedSize.x;
		}
		if (!_alignBottom) {
			y = windowHeight - _y - renderedSize.y;
		}


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

		float offsetX = (float)(2*x/windowWidth - 1);
		float offsetY = (float)(2*y/windowHeight - 1);

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
		return _visInfo.isVisible(viewID);
	}

	@Override
	public long getPickingID() {
		return _pickingID;
	}

	@Override
	public boolean collides(Vec2d coords, double windowWidth, double windowHeight, Camera cam) {

		Vec3d renderedSize = _font.getStringSize(_height, _contents);
		double x = _x;
		double y = _y;
		if (_alignRight) {
			x = windowWidth - _x - renderedSize.x;
		}
		if (!_alignBottom) {
			y = windowHeight - _y - renderedSize.y;
		}

		boolean inX = (coords.x > x) && (coords.x < x+renderedSize.x);
		boolean inY = (coords.y > y) && (coords.y < y+renderedSize.y);

		return inX && inY;
	}
}
