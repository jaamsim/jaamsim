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

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.HashMap;

import com.jaamsim.math.AABB;
import com.jaamsim.math.Color4d;
import com.jaamsim.math.Mat4d;
import com.jaamsim.math.Transform;
import com.jaamsim.math.Vec4d;
import com.jaamsim.render.Renderer.ShaderHandle;
import com.jogamp.opengl.GL2GL3;

/**
 * Miscellaneous debug tools
 * @author Matt.Chudleigh
 *
 */
public class DebugUtils {

	// A vertex buffer of vec3s that draw the lines of a 2*2*2 box at the origin
	private static int _aabbVertBuffer;
	private static int _boxVertBuffer;
	private static int _lineVertBuffer;

	private static int _debugProgHandle;
	private static int _modelViewMatVar;
	private static int _projMatVar;
	private static int _colorVar;
	private static int _posVar;

	private static int _cVar;
	private static int _fcVar;

	private static HashMap<Integer, Integer> _debugVAOMap = new HashMap<>();


	/**
	 * Initialize the GL assets needed, this should be called with the shared GL context
	 * @param gl
	 */
	public static void init(Renderer r, GL2GL3 gl) {
		int[] is = new int[3];
		gl.glGenBuffers(3, is, 0);
		_aabbVertBuffer = is[0];
		_boxVertBuffer = is[1];
		_lineVertBuffer = is[2];

		Shader s = r.getShader(ShaderHandle.DEBUG);
		_debugProgHandle = s.getProgramHandle();
		gl.glUseProgram(_debugProgHandle);

		_modelViewMatVar = gl.glGetUniformLocation(_debugProgHandle, "modelViewMat");
		_projMatVar = gl.glGetUniformLocation(_debugProgHandle, "projMat");
		_colorVar = gl.glGetUniformLocation(_debugProgHandle, "color");

		_posVar = gl.glGetAttribLocation(_debugProgHandle, "position");

		_cVar = gl.glGetAttribLocation(_debugProgHandle, "C");
		_fcVar = gl.glGetAttribLocation(_debugProgHandle, "FC");

		// Build up a buffer of vertices for lines in a box
		gl.glBindBuffer(GL2GL3.GL_ARRAY_BUFFER, _aabbVertBuffer);

		FloatBuffer fb = FloatBuffer.allocate(3 * 2 * 12); // 12 line segments
		// Top lines
		app( 1,  1,  1, fb);
		app( 1, -1,  1, fb);

		app( 1, -1,  1, fb);
		app(-1, -1,  1, fb);

		app(-1, -1,  1, fb);
		app(-1,  1,  1, fb);

		app(-1,  1,  1, fb);
		app( 1,  1,  1, fb);

		// Bottom lines
		app( 1,  1, -1, fb);
		app( 1, -1, -1, fb);

		app( 1, -1, -1, fb);
		app(-1, -1, -1, fb);

		app(-1, -1, -1, fb);
		app(-1,  1, -1, fb);

		app(-1,  1, -1, fb);
		app( 1,  1, -1, fb);

		// Side lines
		app( 1,  1,  1, fb);
		app( 1,  1, -1, fb);

		app(-1,  1,  1, fb);
		app(-1,  1, -1, fb);

		app( 1, -1,  1, fb);
		app( 1, -1, -1, fb);

		app(-1, -1,  1, fb);
		app(-1, -1, -1, fb);

		fb.flip();

		gl.glBufferData(GL2GL3.GL_ARRAY_BUFFER, 3 * 2 * 12 * 4, fb, GL2GL3.GL_STATIC_DRAW);
		gl.glBindBuffer(GL2GL3.GL_ARRAY_BUFFER, 0);

		// Create a buffer for drawing rectangles
		// Build up a buffer of vertices for lines in a box
		gl.glBindBuffer(GL2GL3.GL_ARRAY_BUFFER, _boxVertBuffer);

		fb = FloatBuffer.allocate(3 * 2 * 4); // 4 line segments

		// lines
		app( 0.5f,  0.5f,  0, fb);
		app( 0.5f, -0.5f,  0, fb);

		app( 0.5f, -0.5f,  0, fb);
		app(-0.5f, -0.5f,  0, fb);

		app(-0.5f, -0.5f,  0, fb);
		app(-0.5f,  0.5f,  0, fb);

		app(-0.5f,  0.5f,  0, fb);
		app( 0.5f,  0.5f,  0, fb);

		fb.flip();

		gl.glBufferData(GL2GL3.GL_ARRAY_BUFFER, 3 * 2 * 4 * 4, fb, GL2GL3.GL_STATIC_DRAW);
		gl.glBindBuffer(GL2GL3.GL_ARRAY_BUFFER, 0);

	}

	public static void renderArmature(int contextID, Renderer renderer, Mat4d modelViewMat,
	                                  Armature arm, ArrayList<Mat4d> pose, Color4d color, Camera cam) {

		GL2GL3 gl = renderer.getGL();

		if (!_debugVAOMap.containsKey(contextID)) {
			setupDebugVAO(contextID, renderer);
		}

		int vao = _debugVAOMap.get(contextID);
		gl.glBindVertexArray(vao);

		gl.glUseProgram(_debugProgHandle);

		// Setup uniforms for this object
		Mat4d projMat = cam.getProjMat4d();

		gl.glUniformMatrix4fv(_modelViewMatVar, 1, false, RenderUtils.MarshalMat4d(modelViewMat), 0);
		gl.glUniformMatrix4fv(_projMatVar, 1, false, RenderUtils.MarshalMat4d(projMat), 0);

		gl.glUniform4fv(_colorVar, 1, color.toFloats(), 0);

		gl.glUniform1f(_cVar, Camera.C);
		gl.glUniform1f(_fcVar, Camera.FC);

		ArrayList<Armature.Bone> bones = arm.getAllBones();
		//Build up the list of bone vertices
		Vec4d[] vects = new Vec4d[bones.size() * 2];
		for (int i = 0; i < bones.size(); ++i) {
			Armature.Bone b = bones.get(i);

			Vec4d boneStart = new Vec4d(0, 0, 0, 1);
			boneStart.mult4(b.getMatrix(), boneStart);

			Vec4d boneEnd = new Vec4d(0, b.getLength(), 0, 1);
			boneEnd.mult4(b.getMatrix(), boneEnd);

			if (pose != null) {
				// Adjust the bone by the current pose
				Mat4d poseMat = pose.get(i);

				boneStart.mult4(poseMat, boneStart);
				boneEnd.mult4(poseMat, boneEnd);
			}

			vects[2*i + 0] = boneStart;
			vects[2*i + 1] = boneEnd;
		}

		// Now push it to the card
		FloatBuffer fb = FloatBuffer.allocate(vects.length * 3);
		for (Vec4d v : vects) {
			RenderUtils.putPointXYZ(fb, v);
		}
		fb.flip();

		gl.glBindBuffer(GL2GL3.GL_ARRAY_BUFFER, _lineVertBuffer);
		gl.glBufferData(GL2GL3.GL_ARRAY_BUFFER, fb.limit() * 4, fb, GL2GL3.GL_STATIC_DRAW);

		gl.glVertexAttribPointer(_posVar, 3, GL2GL3.GL_FLOAT, false, 0, 0);

		gl.glBindBuffer(GL2GL3.GL_ARRAY_BUFFER, 0);

		gl.glDisable(GL2GL3.GL_DEPTH_TEST);
		gl.glDrawArrays(GL2GL3.GL_LINES, 0, fb.limit() / 3);
		gl.glEnable(GL2GL3.GL_DEPTH_TEST);

		gl.glLineWidth(1.0f);

		gl.glBindVertexArray(0);

	}

	public static void renderAABB(int contextID, Renderer renderer,
	                              AABB aabb, Color4d color, Camera cam) {

		if (aabb.isEmpty()) {
			return;
		}

		GL2GL3 gl = renderer.getGL();

		if (!_debugVAOMap.containsKey(contextID)) {
			setupDebugVAO(contextID, renderer);
		}

		int vao = _debugVAOMap.get(contextID);
		gl.glBindVertexArray(vao);

		gl.glUseProgram(_debugProgHandle);

		// Setup uniforms for this object
		Mat4d projMat = cam.getProjMat4d();
		Mat4d modelViewMat = new Mat4d();
		cam.getViewMat4d(modelViewMat);

		Mat4d aabbCenterMat = new Mat4d();
		aabbCenterMat.setTranslate3(aabb.center);
		modelViewMat.mult4(aabbCenterMat);
		modelViewMat.scaleCols3(aabb.radius);

		gl.glUniformMatrix4fv(_modelViewMatVar, 1, false, RenderUtils.MarshalMat4d(modelViewMat), 0);
		gl.glUniformMatrix4fv(_projMatVar, 1, false, RenderUtils.MarshalMat4d(projMat), 0);

		gl.glUniform4fv(_colorVar, 1, color.toFloats(), 0);

		gl.glUniform1f(_cVar, Camera.C);
		gl.glUniform1f(_fcVar, Camera.FC);

		gl.glBindBuffer(GL2GL3.GL_ARRAY_BUFFER, _aabbVertBuffer);
		gl.glVertexAttribPointer(_posVar, 3, GL2GL3.GL_FLOAT, false, 0, 0);

		gl.glDrawArrays(GL2GL3.GL_LINES, 0, 12 * 2);

		gl.glBindVertexArray(0);

	}

	// Render a 1*1 box in the XY plane centered at the origin
	public static void renderBox(int contextID, Renderer renderer,
            Transform modelTrans, Vec4d scale, Color4d color, Camera cam) {

		GL2GL3 gl = renderer.getGL();

		if (!_debugVAOMap.containsKey(contextID)) {
			setupDebugVAO(contextID, renderer);
		}

		int vao = _debugVAOMap.get(contextID);
		gl.glBindVertexArray(vao);

		gl.glUseProgram(_debugProgHandle);

		gl.glUniform1f(_cVar, Camera.C);
		gl.glUniform1f(_fcVar, Camera.FC);

		// Setup uniforms for this object
		Mat4d projMat = cam.getProjMat4d();
		Mat4d modelViewMat = new Mat4d();
		cam.getViewMat4d(modelViewMat);

		modelViewMat.mult4(modelTrans.getMat4dRef());
		modelViewMat.scaleCols3(scale);

		gl.glUniformMatrix4fv(_modelViewMatVar, 1, false, RenderUtils.MarshalMat4d(modelViewMat), 0);
		gl.glUniformMatrix4fv(_projMatVar, 1, false, RenderUtils.MarshalMat4d(projMat), 0);

		gl.glUniform4fv(_colorVar, 1, color.toFloats(), 0);

		gl.glBindBuffer(GL2GL3.GL_ARRAY_BUFFER, _boxVertBuffer);
		gl.glVertexAttribPointer(_posVar, 3, GL2GL3.GL_FLOAT, false, 0, 0);

		gl.glDrawArrays(GL2GL3.GL_LINES, 0, 4 * 2);

		gl.glBindVertexArray(0);
	}

	/**
	 * Render a number of lines segments from the points provided
	 * @param lineSegments - pairs of discontinuous line start and end points
	 */
	public static void renderLine(int contextID, Renderer renderer,
            FloatBuffer lineSegments, float[] color, double lineWidth, Camera cam) {

		GL2GL3 gl = renderer.getGL();

		if (!_debugVAOMap.containsKey(contextID)) {
			setupDebugVAO(contextID, renderer);
		}

		int vao = _debugVAOMap.get(contextID);
		gl.glBindVertexArray(vao);

		gl.glUseProgram(_debugProgHandle);

		// Setup uniforms for this object
		Mat4d projMat = cam.getProjMat4d();
		Mat4d modelViewMat = new Mat4d();
		cam.getViewMat4d(modelViewMat);


		gl.glUniformMatrix4fv(_modelViewMatVar, 1, false, RenderUtils.MarshalMat4d(modelViewMat), 0);
		gl.glUniformMatrix4fv(_projMatVar, 1, false, RenderUtils.MarshalMat4d(projMat), 0);

		gl.glUniform4fv(_colorVar, 1, color, 0);

		gl.glUniform1f(_cVar, Camera.C);
		gl.glUniform1f(_fcVar, Camera.FC);

		if (!gl.isGLcore())
			gl.glLineWidth((float)lineWidth);
		else
			gl.glLineWidth(1.0f);

		// Build up a float buffer to pass to GL

		gl.glBindBuffer(GL2GL3.GL_ARRAY_BUFFER, _lineVertBuffer);
		gl.glBufferData(GL2GL3.GL_ARRAY_BUFFER, lineSegments.limit() * 4, lineSegments, GL2GL3.GL_STATIC_DRAW);

		gl.glVertexAttribPointer(_posVar, 3, GL2GL3.GL_FLOAT, false, 0, 0);

		gl.glBindBuffer(GL2GL3.GL_ARRAY_BUFFER, 0);

		gl.glDrawArrays(GL2GL3.GL_LINES, 0, lineSegments.limit() / 3);

		gl.glLineWidth(1.0f);

		gl.glBindVertexArray(0);
	}

	/**
	 * Render a list of points
	 */
	public static void renderPoints(int contextID, Renderer renderer,
            FloatBuffer points, float[] color, double pointWidth, Camera cam) {

		GL2GL3 gl = renderer.getGL();

		if (!_debugVAOMap.containsKey(contextID)) {
			setupDebugVAO(contextID, renderer);
		}

		int vao = _debugVAOMap.get(contextID);
		gl.glBindVertexArray(vao);

		gl.glUseProgram(_debugProgHandle);

		// Setup uniforms for this object
		Mat4d projMat = cam.getProjMat4d();
		Mat4d modelViewMat = new Mat4d();
		cam.getViewMat4d(modelViewMat);


		gl.glUniformMatrix4fv(_modelViewMatVar, 1, false, RenderUtils.MarshalMat4d(modelViewMat), 0);
		gl.glUniformMatrix4fv(_projMatVar, 1, false, RenderUtils.MarshalMat4d(projMat), 0);

		gl.glUniform4fv(_colorVar, 1, color, 0);

		gl.glUniform1f(_cVar, Camera.C);
		gl.glUniform1f(_fcVar, Camera.FC);

		gl.glPointSize((float)pointWidth);

		gl.glBindBuffer(GL2GL3.GL_ARRAY_BUFFER, _lineVertBuffer);
		gl.glBufferData(GL2GL3.GL_ARRAY_BUFFER, points.limit() * 4, points, GL2GL3.GL_STATIC_DRAW);

		gl.glVertexAttribPointer(_posVar, 3, GL2GL3.GL_FLOAT, false, 0, 0);

		gl.glBindBuffer(GL2GL3.GL_ARRAY_BUFFER, 0);

		gl.glDrawArrays(GL2GL3.GL_POINTS, 0, points.limit() / 3);

		gl.glPointSize(1.0f);

		gl.glBindVertexArray(0);
	}

	private static void setupDebugVAO(int contextID, Renderer renderer) {
		GL2GL3 gl = renderer.getGL();

		int vao = renderer.generateVAO(contextID, gl);

		_debugVAOMap.put(contextID, vao);
		gl.glBindVertexArray(vao);

		gl.glUseProgram(_debugProgHandle);

		gl.glEnableVertexAttribArray(_posVar);

		gl.glBindVertexArray(0);

	}


	/**
	 * Dummy helper to make init less ugly
	 * @param f0
	 * @param f1
	 * @param f2
	 * @param fb
	 */
	private static void app(float f0, float f1, float f2, FloatBuffer fb) {
		fb.put(f0); fb.put(f1); fb.put(f2);
	}
}
