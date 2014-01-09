/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2014 Ausenco Engineering Canada Inc.
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

import java.util.HashMap;

import javax.media.opengl.GL2GL3;

import com.jaamsim.math.Color4d;
import com.jaamsim.math.Mat4d;
import com.jaamsim.math.Ray;
import com.jaamsim.math.Vec3d;
import com.jaamsim.math.Vec4d;
import com.jaamsim.render.Camera;
import com.jaamsim.render.OverlayRenderable;
import com.jaamsim.render.RenderUtils;
import com.jaamsim.render.Renderer;
import com.jaamsim.render.Shader;
import com.jaamsim.render.VisibilityInfo;

public class BillboardString implements OverlayRenderable {

	private final TessFont _font;
	private final String _contents;

	private final float[] _color;

	private final double _height;
	private final Vec3d _pos;
	private final double _xOffset, _yOffset;
	private final VisibilityInfo _visInfo;

	private final Mat4d tempViewMat = new Mat4d();
	private final Vec4d tempPos = new Vec4d();

	private static HashMap<Integer, Integer> VAOMap = new HashMap<Integer, Integer>();

	public BillboardString(TessFont font, String contents, Color4d color,
            double height, Vec3d pos, double xOffset, double yOffset, VisibilityInfo visInfo) {
		_font = font;
		_contents = contents;
		_color = color.toFloats();
		_height = height;
		_xOffset = xOffset;
		_yOffset = yOffset;
		_pos = pos;
		_visInfo = visInfo;
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

}
