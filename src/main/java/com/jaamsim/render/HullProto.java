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
import java.nio.IntBuffer;
import java.util.List;
import java.util.Map;

import javax.media.opengl.GL2GL3;

import com.jaamsim.math.ConvexHull;
import com.jaamsim.math.Matrix4d;
import com.jaamsim.math.Vector4d;

/**
 * A renderable prototype for a convex hull
 * @author Matt.Chudleigh
 *
 */
public class HullProto {

	private ConvexHull _hull;
	boolean _isLoadedGPU = false;

	private int _vertexBuffer;
	private int _indexBuffer;
	private int _numIndices;

	private int _progHandle;
	private int _modelViewMatVar;
	private int _projMatVar;

	private int _assetID;

	public HullProto(ConvexHull hull) {
		_hull = hull;
	}

	public void loadGPUAssets(GL2GL3 gl, Renderer renderer) {
		assert(!_isLoadedGPU);

		_assetID = Renderer.getAssetID();

		Shader s = renderer.getShader(Renderer.ShaderHandle.HULL);

		_progHandle = s.getProgramHandle();
		gl.glUseProgram(_progHandle);

		_modelViewMatVar = gl.glGetUniformLocation(_progHandle, "modelViewMat");
		_projMatVar = gl.glGetUniformLocation(_progHandle, "projMat");

		int[] is = new int[2];
		gl.glGenBuffers(2, is, 0);
		_vertexBuffer = is[0];
		_indexBuffer = is[1];

		List<Vector4d> verts = _hull.getVertices();
		// Generate the vertex buffer
		FloatBuffer fb = FloatBuffer.allocate(verts.size() * 3); //
		for (Vector4d v : verts) {
			fb.put(v.toFloats(), 0, 3);
		}
		fb.flip();

		gl.glBindBuffer(GL2GL3.GL_ARRAY_BUFFER, _vertexBuffer);
		gl.glBufferData(GL2GL3.GL_ARRAY_BUFFER, verts.size() * 3 * 4, fb, GL2GL3.GL_STATIC_DRAW);

		gl.glBindBuffer(GL2GL3.GL_ARRAY_BUFFER, 0);

		// Generate the index buffer
		List<ConvexHull.HullFace> faces = _hull.getFaces();

		_numIndices = faces.size() * 3;

		IntBuffer ib = IntBuffer.allocate(faces.size() * 3); //
		for (ConvexHull.HullFace f : faces) {
			ib.put(f.indices, 0 ,3);
		}

		ib.flip();

		gl.glBindBuffer(GL2GL3.GL_ELEMENT_ARRAY_BUFFER, _indexBuffer);
		gl.glBufferData(GL2GL3.GL_ELEMENT_ARRAY_BUFFER, faces.size() * 3 * 4, ib, GL2GL3.GL_STATIC_DRAW);

		gl.glBindBuffer(GL2GL3.GL_ELEMENT_ARRAY_BUFFER, 0);

		_isLoadedGPU = true;
	}

	private void setupVAO(Map<Integer, Integer> vaoMap, Renderer renderer) {
		GL2GL3 gl = renderer.getGL();

		int[] vaos = new int[1];
		gl.glGenVertexArrays(1, vaos, 0);
		int vao = vaos[0];
		vaoMap.put(_assetID, vao);
		gl.glBindVertexArray(vao);

		gl.glUseProgram(_progHandle);

		int posVar = gl.glGetAttribLocation(_progHandle, "position");
		gl.glEnableVertexAttribArray(posVar);

		gl.glBindBuffer(GL2GL3.GL_ARRAY_BUFFER, _vertexBuffer);
		gl.glVertexAttribPointer(posVar, 3, GL2GL3.GL_FLOAT, false, 0, 0);

		gl.glBindBuffer(GL2GL3.GL_ELEMENT_ARRAY_BUFFER, _indexBuffer);

		gl.glBindVertexArray(0);

	}

	public void render(Map<Integer, Integer> vaoMap, Renderer renderer,
            Matrix4d modelViewMat,
            Camera cam) {

		GL2GL3 gl = renderer.getGL();

		if (!vaoMap.containsKey(_assetID)) {
			setupVAO(vaoMap, renderer);
		}

		int vao = vaoMap.get(_assetID);
		gl.glBindVertexArray(vao);

		gl.glUseProgram(_progHandle);

		// Setup uniforms for this object
		Matrix4d projMat = cam.getProjMatRef();

		gl.glUniformMatrix4fv(_modelViewMatVar, 1, false, modelViewMat.toFloats(), 0);
		gl.glUniformMatrix4fv(_projMatVar, 1, false, projMat.toFloats(), 0);

		// Actually draw it

		gl.glEnable(GL2GL3.GL_BLEND);
		gl.glEnable(GL2GL3.GL_CULL_FACE);
		gl.glCullFace(GL2GL3.GL_BACK);

		gl.glBlendFunc(GL2GL3.GL_ONE, GL2GL3.GL_ONE_MINUS_SRC_ALPHA);
		gl.glBlendEquation(GL2GL3.GL_FUNC_ADD);

		gl.glDisable(GL2GL3.GL_DEPTH_TEST);

		//gl.glPolygonMode(GL2GL3.GL_FRONT_AND_BACK, GL2GL3.GL_LINE);

		gl.glDrawElements(GL2GL3.GL_TRIANGLES, _numIndices, GL2GL3.GL_UNSIGNED_INT, 0);

		//gl.glPolygonMode(GL2GL3.GL_FRONT_AND_BACK, GL2GL3.GL_FILL);

		gl.glEnable(GL2GL3.GL_DEPTH_TEST);

		gl.glDisable(GL2GL3.GL_CULL_FACE);
		gl.glCullFace(GL2GL3.GL_BACK);

		gl.glDisable(GL2GL3.GL_BLEND);

		gl.glBindVertexArray(0);

	}

	public boolean isLoadedGPU() {
		return _isLoadedGPU;
	}
}
