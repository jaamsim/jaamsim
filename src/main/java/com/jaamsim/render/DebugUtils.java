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

import java.nio.FloatBuffer;
import java.util.List;
import java.util.Map;

import javax.media.opengl.GL2GL3;

import com.jaamsim.math.AABB;
import com.jaamsim.math.Matrix4d;
import com.jaamsim.math.Transform;
import com.jaamsim.math.Vector4d;
import com.jaamsim.render.Renderer.ShaderHandle;

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

	private static int _debugVAOKey;


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

		_debugVAOKey = Renderer.getAssetID();

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

	public static void renderAABB(Map<Integer, Integer> vaoMap, Renderer renderer,
	                              AABB aabb, Vector4d color, Camera cam) {

		if (aabb.isEmpty()) {
			return;
		}

		GL2GL3 gl = renderer.getGL();

		if (!vaoMap.containsKey(_debugVAOKey)) {
			setupDebugVAO(vaoMap, renderer);
		}

		int vao = vaoMap.get(_debugVAOKey);
		gl.glBindVertexArray(vao);

		gl.glUseProgram(_debugProgHandle);

		// Setup uniforms for this object
		Matrix4d projMat = cam.getProjMatRef();
		Matrix4d modelViewMat = new Matrix4d();
		cam.getViewMatrix(modelViewMat);

		modelViewMat.mult(Matrix4d.TranslationMatrix(aabb.getCenter()), modelViewMat);
		modelViewMat.mult(Matrix4d.ScaleMatrix(aabb.getRadius()), modelViewMat);

		gl.glUniformMatrix4fv(_modelViewMatVar, 1, false, modelViewMat.toFloats(), 0);
		gl.glUniformMatrix4fv(_projMatVar, 1, false, projMat.toFloats(), 0);

		gl.glUniform4fv(_colorVar, 1, color.toFloats(), 0);

		gl.glBindBuffer(GL2GL3.GL_ARRAY_BUFFER, _aabbVertBuffer);
		gl.glVertexAttribPointer(_posVar, 3, GL2GL3.GL_FLOAT, false, 0, 0);

		gl.glDrawArrays(GL2GL3.GL_LINES, 0, 12 * 2);

		gl.glBindVertexArray(0);

	}

	// Render a 1*1 box in the XY plane centered at the origin
	public static void renderBox(Map<Integer, Integer> vaoMap, Renderer renderer,
            Transform modelTrans, Vector4d scale, Vector4d color, Camera cam) {

		GL2GL3 gl = renderer.getGL();

		if (!vaoMap.containsKey(_debugVAOKey)) {
			setupDebugVAO(vaoMap, renderer);
		}

		int vao = vaoMap.get(_debugVAOKey);
		gl.glBindVertexArray(vao);

		gl.glUseProgram(_debugProgHandle);

		// Setup uniforms for this object
		Matrix4d projMat = cam.getProjMatRef();
		Matrix4d modelViewMat = new Matrix4d();
		cam.getViewMatrix(modelViewMat);

		modelViewMat.mult(modelTrans.getMatrixRef(), modelViewMat);
		modelViewMat.mult(Matrix4d.ScaleMatrix(scale), modelViewMat);


		gl.glUniformMatrix4fv(_modelViewMatVar, 1, false, modelViewMat.toFloats(), 0);
		gl.glUniformMatrix4fv(_projMatVar, 1, false, projMat.toFloats(), 0);

		gl.glUniform4fv(_colorVar, 1, color.toFloats(), 0);

		gl.glBindBuffer(GL2GL3.GL_ARRAY_BUFFER, _boxVertBuffer);
		gl.glVertexAttribPointer(_posVar, 3, GL2GL3.GL_FLOAT, false, 0, 0);

		gl.glDrawArrays(GL2GL3.GL_LINES, 0, 4 * 2);

		gl.glBindVertexArray(0);
	}

	/**
	 * Render a number of lines segments from the points provided
	 * @param vaoMap
	 * @param renderer
	 * @param lineSegments - pairs of discontinuous line start and end points
	 * @param color
	 * @param cam
	 */
	public static void renderLine(Map<Integer, Integer> vaoMap, Renderer renderer,
            List<Vector4d> lineSegments, float[] color, double lineWidth, Camera cam) {

		GL2GL3 gl = renderer.getGL();

		if (!vaoMap.containsKey(_debugVAOKey)) {
			setupDebugVAO(vaoMap, renderer);
		}

		int vao = vaoMap.get(_debugVAOKey);
		gl.glBindVertexArray(vao);

		gl.glUseProgram(_debugProgHandle);

		// Setup uniforms for this object
		Matrix4d projMat = cam.getProjMatRef();
		Matrix4d modelViewMat = new Matrix4d();
		cam.getViewMatrix(modelViewMat);


		gl.glUniformMatrix4fv(_modelViewMatVar, 1, false, modelViewMat.toFloats(), 0);
		gl.glUniformMatrix4fv(_projMatVar, 1, false, projMat.toFloats(), 0);

		gl.glUniform4fv(_colorVar, 1, color, 0);

		gl.glLineWidth((float)lineWidth);

		// Build up a float buffer to pass to GL

		FloatBuffer fb = FloatBuffer.allocate(3 * lineSegments.size());
		for (Vector4d vert : lineSegments) {
			fb.put(vert.toFloats3());
		}
		fb.flip();

		gl.glBindBuffer(GL2GL3.GL_ARRAY_BUFFER, _lineVertBuffer);
		gl.glBufferData(GL2GL3.GL_ARRAY_BUFFER, 3 * lineSegments.size() * 4, fb, GL2GL3.GL_STATIC_DRAW);

		gl.glVertexAttribPointer(_posVar, 3, GL2GL3.GL_FLOAT, false, 0, 0);

		gl.glBindBuffer(GL2GL3.GL_ARRAY_BUFFER, 0);

		gl.glDrawArrays(GL2GL3.GL_LINES, 0, lineSegments.size());

		gl.glLineWidth(1.0f);

		gl.glBindVertexArray(0);
	}

	/**
	 * Render a list of points
	 * @param vaoMap
	 * @param renderer
	 * @param points
	 * @param color
	 * @param pointWidth
	 * @param cam
	 */
	public static void renderPoints(Map<Integer, Integer> vaoMap, Renderer renderer,
            List<Vector4d> points, float[] color, double pointWidth, Camera cam) {

		GL2GL3 gl = renderer.getGL();

		if (!vaoMap.containsKey(_debugVAOKey)) {
			setupDebugVAO(vaoMap, renderer);
		}

		int vao = vaoMap.get(_debugVAOKey);
		gl.glBindVertexArray(vao);

		gl.glUseProgram(_debugProgHandle);

		// Setup uniforms for this object
		Matrix4d projMat = cam.getProjMatRef();
		Matrix4d modelViewMat = new Matrix4d();
		cam.getViewMatrix(modelViewMat);


		gl.glUniformMatrix4fv(_modelViewMatVar, 1, false, modelViewMat.toFloats(), 0);
		gl.glUniformMatrix4fv(_projMatVar, 1, false, projMat.toFloats(), 0);

		gl.glUniform4fv(_colorVar, 1, color, 0);

		gl.glPointSize((float)pointWidth);

		// Build up a float buffer to pass to GL

		FloatBuffer fb = FloatBuffer.allocate(3 * points.size());
		for (Vector4d point : points) {
			fb.put(point.toFloats3());
		}
		fb.flip();

		gl.glBindBuffer(GL2GL3.GL_ARRAY_BUFFER, _lineVertBuffer);
		gl.glBufferData(GL2GL3.GL_ARRAY_BUFFER, 3 * points.size() * 4, fb, GL2GL3.GL_STATIC_DRAW);

		gl.glVertexAttribPointer(_posVar, 3, GL2GL3.GL_FLOAT, false, 0, 0);

		gl.glBindBuffer(GL2GL3.GL_ARRAY_BUFFER, 0);

		gl.glDrawArrays(GL2GL3.GL_POINTS, 0, points.size());

		gl.glPointSize(1.0f);

		gl.glBindVertexArray(0);
	}

	private static void setupDebugVAO(Map<Integer, Integer> vaoMap, Renderer renderer) {
		GL2GL3 gl = renderer.getGL();

		int[] vaos = new int[1];
		gl.glGenVertexArrays(1, vaos, 0);
		int vao = vaos[0];
		vaoMap.put(_debugVAOKey, vao);
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
