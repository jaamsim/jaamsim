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
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.List;

import com.jaamsim.math.ConvexHull;
import com.jaamsim.math.Mat4d;
import com.jaamsim.math.Vec3d;
import com.jogamp.opengl.GL2GL3;

/**
 * A renderable prototype for a convex hull
 * @author Matt.Chudleigh
 *
 */
public class HullProto {

	private ConvexHull _hull;
	boolean _isLoadedGPU = false;

	private HashMap<Integer, Integer> _vaoMap = new HashMap<>();

	public HullProto(ConvexHull hull) {
		_hull = hull;
	}

	private void setupVAO(int contextID, Renderer renderer, int progHandle, int vertexBuffer, int indexBuffer) {
		GL2GL3 gl = renderer.getGL();

		int vao = renderer.generateVAO(contextID, gl);

		_vaoMap.put(contextID, vao);
		gl.glBindVertexArray(vao);

		gl.glUseProgram(progHandle);

		int posVar = gl.glGetAttribLocation(progHandle, "position");
		gl.glEnableVertexAttribArray(posVar);

		gl.glBindBuffer(GL2GL3.GL_ARRAY_BUFFER, vertexBuffer);
		gl.glVertexAttribPointer(posVar, 3, GL2GL3.GL_FLOAT, false, 0, 0);

		gl.glBindBuffer(GL2GL3.GL_ELEMENT_ARRAY_BUFFER, indexBuffer);

		gl.glBindVertexArray(0);

	}

	public void render(int contextID, Renderer renderer,
            Mat4d modelViewMat,
            Camera cam) {

		GL2GL3 gl = renderer.getGL();

		Shader s = renderer.getShader(Renderer.ShaderHandle.HULL);

		int progHandle = s.getProgramHandle();
		gl.glUseProgram(progHandle);

		int modelViewMatVar = gl.glGetUniformLocation(progHandle, "modelViewMat");
		int projMatVar = gl.glGetUniformLocation(progHandle, "projMat");

		int cVar = gl.glGetUniformLocation(progHandle, "C");
		int fcVar = gl.glGetUniformLocation(progHandle, "FC");

		int[] is = new int[2];
		gl.glGenBuffers(2, is, 0);
		int vertexBuffer = is[0];
		int indexBuffer = is[1];

		List<Vec3d> verts = _hull.getVertices();
		// Generate the vertex buffer
		FloatBuffer fb = FloatBuffer.allocate(verts.size() * 3); //
		for (Vec3d v : verts) {
			RenderUtils.putPointXYZ(fb, v);
		}
		fb.flip();

		gl.glBindBuffer(GL2GL3.GL_ARRAY_BUFFER, vertexBuffer);
		gl.glBufferData(GL2GL3.GL_ARRAY_BUFFER, verts.size() * 3 * 4, fb, GL2GL3.GL_STATIC_DRAW);

		gl.glBindBuffer(GL2GL3.GL_ARRAY_BUFFER, 0);

		// Generate the index buffer
		List<ConvexHull.HullFace> faces = _hull.getFaces();

		int numIndices = faces.size() * 3;

		IntBuffer ib = IntBuffer.allocate(faces.size() * 3); //
		for (ConvexHull.HullFace f : faces) {
			ib.put(f.indices, 0 ,3);
		}

		ib.flip();

		gl.glBindBuffer(GL2GL3.GL_ELEMENT_ARRAY_BUFFER, indexBuffer);
		gl.glBufferData(GL2GL3.GL_ELEMENT_ARRAY_BUFFER, faces.size() * 3 * 4, ib, GL2GL3.GL_STATIC_DRAW);

		gl.glBindBuffer(GL2GL3.GL_ELEMENT_ARRAY_BUFFER, 0);

		if (!_vaoMap.containsKey(contextID)) {
			setupVAO(contextID, renderer, progHandle, vertexBuffer, indexBuffer);
		}

		int vao = _vaoMap.get(contextID);
		gl.glBindVertexArray(vao);

		gl.glUseProgram(progHandle);

		// Setup uniforms for this object
		Mat4d projMat = cam.getProjMat4d();

		gl.glUniformMatrix4fv(modelViewMatVar, 1, false, RenderUtils.MarshalMat4d(modelViewMat), 0);
		gl.glUniformMatrix4fv(projMatVar, 1, false, RenderUtils.MarshalMat4d(projMat), 0);

		gl.glUniform1f(cVar, Camera.C);
		gl.glUniform1f(fcVar, Camera.FC);

		// Actually draw it

		gl.glEnable(GL2GL3.GL_BLEND);
		gl.glEnable(GL2GL3.GL_CULL_FACE);
		gl.glCullFace(GL2GL3.GL_BACK);

		gl.glBlendFunc(GL2GL3.GL_ONE, GL2GL3.GL_ONE_MINUS_SRC_ALPHA);
		gl.glBlendEquation(GL2GL3.GL_FUNC_ADD);

		gl.glDisable(GL2GL3.GL_DEPTH_TEST);

		//gl.glPolygonMode(GL2GL3.GL_FRONT_AND_BACK, GL2GL3.GL_LINE);

		gl.glDrawElements(GL2GL3.GL_TRIANGLES, numIndices, GL2GL3.GL_UNSIGNED_INT, 0);

		//gl.glPolygonMode(GL2GL3.GL_FRONT_AND_BACK, GL2GL3.GL_FILL);

		gl.glEnable(GL2GL3.GL_DEPTH_TEST);

		gl.glDisable(GL2GL3.GL_CULL_FACE);
		gl.glCullFace(GL2GL3.GL_BACK);

		gl.glDisable(GL2GL3.GL_BLEND);

		gl.glBindVertexArray(0);

		gl.glDeleteBuffers(2, is, 0);

	}

	public boolean isLoadedGPU() {
		return _isLoadedGPU;
	}
}
