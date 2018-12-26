/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2018 JaamSim Software Inc.
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

import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.List;

import com.jaamsim.math.Color4d;
import com.jaamsim.math.Ray;
import com.jaamsim.math.Vec2d;
import com.jogamp.opengl.GL2GL3;

public class OverlayLine implements OverlayRenderable {

	private final FloatBuffer lineBuffer;
	private final Color4d color;
	private final double lineWidth;
	private final VisibilityInfo visInfo;
	private final boolean originTop;
	private final boolean originRight;
	private final long pickingID;

	static private boolean staticInit = false;

	static private int progHandle;

	static private int posVar;
	static private int colorVar;
	static private int offsetVar;
	static private int sizeVar;
	static private int hasTexVar;
	static private int lineGLBuff;

	static private double COLL_DIST = 3; // The collision distance in pixels

	private static HashMap<Integer, Integer> VAOMap = new HashMap<>();

	public OverlayLine(  List<Vec2d> lineSegments, Color4d color,
	                     boolean originTop, boolean originRight,
	                     double lineWidth, VisibilityInfo visInfo,
	                     long pickingID) {
		this.color = color;
		this.lineWidth = lineWidth;
		this.pickingID = pickingID;
		this.visInfo = visInfo;
		this.originTop = originTop;
		this.originRight = originRight;

		lineBuffer = FloatBuffer.allocate(2 * lineSegments.size());
		for (Vec2d vert : lineSegments) {
			RenderUtils.putPointXY(lineBuffer, vert);
		}
		lineBuffer.flip();

	}

	private static void initStaticData(Renderer r) {
		GL2GL3 gl = r.getGL();

		// Initialize the shader variables
		progHandle = r.getShader(Renderer.ShaderHandle.OVERLAY_FLAT).getProgramHandle();

		int[] is = new int[1];
		gl.glGenBuffers(1, is, 0);
		lineGLBuff = is[0];

		hasTexVar = gl.glGetUniformLocation(progHandle, "useTex");
		sizeVar = gl.glGetUniformLocation(progHandle, "size");
		offsetVar = gl.glGetUniformLocation(progHandle, "offset");
		colorVar = gl.glGetUniformLocation(progHandle, "color");

		posVar = gl.glGetAttribLocation(progHandle, "position");
		staticInit = true;
	}



	@Override
	public void render(int contextID, Renderer renderer,
		double windowWidth, double windowHeight, Camera cam, Ray pickRay) {

		if (!staticInit) {
			initStaticData(renderer);
		}

		GL2GL3 gl = renderer.getGL();

		if (!VAOMap.containsKey(contextID)) {
			setupVAO(contextID, renderer);
		}

		int vao = VAOMap.get(contextID);
		gl.glBindVertexArray(vao);

		gl.glUseProgram(progHandle);

		gl.glUniform4fv(colorVar, 1, color.toFloats(), 0);

		gl.glUniform1i(hasTexVar, 0);


		// Set the size and offset in normalized (-1, 1) coordinates
		double scaleX = 2.0 / windowWidth;
		double scaleY = 2.0 / windowHeight;

		double pixelWidth = 2.0/windowWidth;
		double pixelHeight = 2.0/windowHeight;

		double xOffset = -1.0 + 0.5*pixelWidth;
		double yOffset = -1.0 + 0.5*pixelHeight;

		if (originTop) {
			scaleY *= -1.0;
			yOffset = 1.0 - 0.5*pixelHeight;
		}

		if (originRight) {
			scaleX *= -1.0;
			xOffset = 1.0 - 0.5*pixelWidth;
		}

		gl.glUniform2f(offsetVar, (float)xOffset, (float)yOffset);
		gl.glUniform2f(sizeVar, (float)scaleX, (float)scaleY);

		gl.glEnableVertexAttribArray(posVar);

		gl.glDisable(GL2GL3.GL_CULL_FACE);

		if (!gl.isGLcore())
			gl.glLineWidth((float)lineWidth);
		else
			gl.glLineWidth(1.0f);

		// Build up a float buffer to pass to GL

		gl.glBindBuffer(GL2GL3.GL_ARRAY_BUFFER, lineGLBuff);
		gl.glBufferData(GL2GL3.GL_ARRAY_BUFFER, lineBuffer.limit() * 4, lineBuffer, GL2GL3.GL_STATIC_DRAW);

		gl.glVertexAttribPointer(posVar, 2, GL2GL3.GL_FLOAT, false, 0, 0);

		gl.glBindBuffer(GL2GL3.GL_ARRAY_BUFFER, 0);

		gl.glDrawArrays(GL2GL3.GL_LINES, 0, lineBuffer.limit() / 2);

		gl.glLineWidth(1.0f);

		gl.glEnable(GL2GL3.GL_CULL_FACE);

		gl.glBindVertexArray(0);
	}

	private void setupVAO(int contextID, Renderer renderer) {
		GL2GL3 gl = renderer.getGL();

		int vao = renderer.generateVAO(contextID, gl);
		VAOMap.put(contextID, vao);
	}

	@Override
	public boolean renderForView(int viewID, Camera cam) {
		return visInfo.isVisible(viewID);
	}

	@Override
	public long getPickingID() {
		return pickingID;
	}

	@Override
	public boolean collides(Vec2d coords, double windowWidth, double windowHeight, Camera cam) {

		float[] floats = lineBuffer.array();
		for (int i = 0; i < floats.length; i+=4) {
			Vec2d l0 = new Vec2d(floats[i+0], floats[i+1]);
			Vec2d l1 = new Vec2d(floats[i+2], floats[i+3]);
			Vec2d dir = new Vec2d();
			dir.sub2(l1, l0);

			dir.normalize2();
			Vec2d locPoint = new Vec2d(coords);
			locPoint.sub2(l0);

			double dot = locPoint.dot2(dir);
			Vec2d parallel = new Vec2d(dir);
			parallel.scale2(dot);
			Vec2d diff = new Vec2d();
			diff.sub2(locPoint, parallel);

			if (diff.magSquare2() < (COLL_DIST+lineWidth)*(COLL_DIST+lineWidth))
				return true;
		}

		return false;
	}

}
