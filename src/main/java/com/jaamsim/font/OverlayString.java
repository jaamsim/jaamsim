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
package com.jaamsim.font;

import java.util.Map;

import javax.media.opengl.GL2GL3;

import com.jaamsim.math.Color4d;
import com.jaamsim.math.Vec3d;
import com.jaamsim.render.OverlayRenderable;
import com.jaamsim.render.Renderer;
import com.jaamsim.render.Shader;
import com.jaamsim.render.VisibilityInfo;

public class OverlayString implements OverlayRenderable {

	private TessFont _font;
	private String _contents;

	private final float[] _color;

	private double _height;
	private double _x, _y;
	private boolean _alignRight, _alignBottom;
	private VisibilityInfo _visInfo;


	public OverlayString(TessFont font, String contents, Color4d color,
	                     double height, double x, double y,
	                     boolean alignRight, boolean alignBottom, VisibilityInfo visInfo) {
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
	}

	@Override
	public void render(Map<Integer, Integer> vaoMap, Renderer renderer,
		double windowWidth, double windowHeight) {


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

		int fontID = _font.getAssetID();
		if (!vaoMap.containsKey(fontID)) {
			setupVAO(vaoMap, fontID, renderer);
		}

		int vao = vaoMap.get(fontID);
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
		gl.glDisable(GL2GL3.GL_DEPTH_TEST);

		for (int i = 0; i < _contents.length(); ++i) {
			TessChar tc = _font.getTessChar(_contents.charAt(i));
			if (tc == null) {
				assert(false);
				continue;
			}

			gl.glUniform2f(offsetVar, offsetX, offsetY);

			gl.glDrawArrays(GL2GL3.GL_TRIANGLES, tc.getStartIndex(), tc.getNumVerts());

			offsetX += tc.getAdvance()*scaleX;
		}

		gl.glEnable(GL2GL3.GL_CULL_FACE);
		gl.glEnable(GL2GL3.GL_DEPTH_TEST);

	}


	private void setupVAO(Map<Integer, Integer> vaoMap, int id, Renderer renderer) {
		GL2GL3 gl = renderer.getGL();

		int[] vaos = new int[1];
		gl.glGenVertexArrays(1, vaos, 0);
		vaoMap.put(id, vaos[0]);

	}

	@Override
	public boolean renderForView(int viewID) {
		return _visInfo.isVisible(viewID);
	}
}
