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

import java.net.URL;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.media.opengl.GL2GL3;

import com.jaamsim.math.AABB;
import com.jaamsim.math.ConvexHull;
import com.jaamsim.math.Matrix4d;
import com.jaamsim.math.Vector4d;

/**
 * A basic wrapper around mesh data and the OpenGL calls that go with it
 * Will maintain a vertex buffer for vertex data, texture coordinates and normal data
 * The first pass is non-indexed but will be updated in the future
 * @author Matt Chudleigh
 *
 */
public class MeshProto {

public final static int NO_TRANS = 0;
public final static int A_ONE_TRANS = 1;
public final static int RGB_ZERO_TRANS = 2;

/**
 * The sub mesh data is a CPU space representation of the data,
 * this will be pushed to the GPU when loadGPUAssets() is called
 * @author Matt.Chudleigh
 *
 */
private static class SubMeshData {

	public ArrayList<Vector4d> verts = new ArrayList<Vector4d>();
	public ArrayList<Vector4d> texCoords = new ArrayList<Vector4d>();
	public ArrayList<Vector4d> normals = new ArrayList<Vector4d>();
	public Vector4d diffuseColor;
	public URL colorTex;

	public int numVerts;
	public ConvexHull hull;
	public int transType;
	public Vector4d transColour;
}

private static class SubMeshInstance {
	int subMeshIndex;
	Matrix4d transform;
	Matrix4d normalTrans;
}

private static class SubLineData {
	public ArrayList<Vector4d> verts = new ArrayList<Vector4d>();
	public Vector4d diffuseColor;

	public int numVerts;
	public ConvexHull hull;
}
private static class SubLineInstance {
	int subLineIndex;
	Matrix4d transform;
}

/** Sortable entry for a single transparent sub mesh before rendering
 *
 * @author matt.chudleigh
 *
 */
private static class TransSortable implements Comparable<TransSortable> {
	public SubMesh subMesh;
	public double dist;
	public Matrix4d modelViewMat;
	public Matrix4d normalMat;

	@Override
	public int compareTo(TransSortable o) {
		// Sort such that largest distance sorts to front of list
		// by reversing argument order in compare.
		return Double.compare(o.dist, this.dist);
	}
}

private ArrayList<SubMeshData> _subMeshesData;
private ArrayList<SubMeshInstance> _subMeshInstances;
private ArrayList<SubLineData> _subLinesData;
private ArrayList<SubLineInstance> _subLineInstances;
private ConvexHull _hull;
private boolean _anyTransparent = false;

@SuppressWarnings("unused")
private int _numVerts = 0;

private class SubMesh {

	SubMesh() {
		_id = Renderer.getAssetID();
	}

	public int _vertexBuffer;
	public int _texCoordBuffer;
	public int _normalBuffer;

	public int _texHandle;
	public Vector4d _diffuseColor;

	public int _progHandle;

	public int _modelViewProjMatVar;
	public int _normalMatVar;
	public int _lightDirVar;
	public int _texVar;
	public int _colorVar;
	public int _useTexVar;

	public int _transType;
	public Vector4d _transColour;

	public Vector4d _center;

	public int _numVerts;
	public int _id; // The system wide asset ID

	public ConvexHull _hull;
}

private class SubLine {

	SubLine() {
		_id = Renderer.getAssetID();
	}

	public int _vertexBuffer;
	public Vector4d _diffuseColor;

	public int _progHandle;

	public int _modelViewMatVar;
	public int _projMatVar;
	public int _colorVar;

	public ConvexHull _hull;

	public int _numVerts;
	public int _id; // The system wide asset ID
}

private ArrayList<SubMesh> _subMeshes;
private ArrayList<SubLine> _subLines;

/**
 * The maximum distance a vertex is from the origin
 */
private double _radius;
private boolean _isLoadedGPU = false;

public MeshProto() {
	_subMeshes = new ArrayList<SubMesh>();
	_subMeshesData = new ArrayList<SubMeshData>();
	_subMeshInstances = new ArrayList<SubMeshInstance>();
	_subLines = new ArrayList<SubLine>();
	_subLinesData = new ArrayList<SubLineData>();
	_subLineInstances = new ArrayList<SubLineInstance>();
	_radius = 0;
}

public void addSubMeshInstance(int meshIndex, Matrix4d trans, Matrix4d normalTrans) {
	SubMeshInstance inst = new SubMeshInstance();
	inst.subMeshIndex = meshIndex;
	inst.transform = trans;
	inst.normalTrans = normalTrans;

	_subMeshInstances.add(inst);
	_numVerts += _subMeshesData.get(meshIndex).verts.size();

}

public void addSubLineInstance(int lineIndex, Matrix4d trans, Matrix4d normalTrans) {
	SubLineInstance inst = new SubLineInstance();
	inst.subLineIndex = lineIndex;
	inst.transform = trans;

	_subLineInstances.add(inst);
}

public void addSubMesh(Vector4d[] vertices,
        Vector4d[] normals,
        Vector4d[] texCoords,
        URL colorTex,
        Vector4d diffuseColor,
        int transType,
        Vector4d transColour) {


	if (colorTex == null) {
		assert diffuseColor != null;
	}

	boolean hasTex = colorTex != null;

	SubMeshData sub = new SubMeshData();
	sub.colorTex = colorTex;
	sub.diffuseColor = diffuseColor;
	sub.transType = transType;
	sub.transColour = transColour;
	_subMeshesData.add(sub);

	if (transType != 0) {
		_anyTransparent = true;
	}

		// This is a new sub mesh (or one with a unique color or texture)

	sub.numVerts += vertices.length;
	//_numVerts += vertices.length;

	assert((sub.numVerts % 3) == 0);

	assert(normals.length == vertices.length);

	if (hasTex) {
		assert(texCoords.length == vertices.length);

		sub.texCoords.addAll(Arrays.asList(texCoords));
	}

	sub.verts.addAll(Arrays.asList(vertices));
	sub.normals.addAll(Arrays.asList(normals));

	sub.hull = ConvexHull.TryBuildHull(sub.verts, 5);
}

public void addSubLine(Vector4d[] vertices,
        Vector4d diffuseColor) {

	SubLineData sub = new SubLineData();
	sub.diffuseColor = diffuseColor;
	if (sub.diffuseColor == null) {
		sub.diffuseColor = new Vector4d(); // Default to black
	}
	_subLinesData.add(sub);

		// This is a new sub mesh (or one with a unique color or texture)

	sub.numVerts += vertices.length;

	assert((sub.numVerts % 2) == 0);

	sub.verts.addAll(Arrays.asList(vertices));

	sub.hull = ConvexHull.TryBuildHull(sub.verts, 5);
}

public boolean hasTransparent() {
	return _anyTransparent;
}

public void render(Map<Integer, Integer> vaoMap, Renderer renderer,
                   Matrix4d modelMat,
                   Matrix4d normalMat,
                   Camera cam) {

	assert(_isLoadedGPU);

	Matrix4d viewMat = new Matrix4d();
	cam.getViewMatrix(viewMat);

	Matrix4d modelViewMat = new Matrix4d();
	viewMat.mult(modelMat, modelViewMat);

	Matrix4d subModelViewMat = new Matrix4d();
	Matrix4d subModelMat = new Matrix4d();
	Matrix4d subNormalMat = new Matrix4d();


	for (SubMeshInstance subInst : _subMeshInstances) {
		SubMesh subMesh = _subMeshes.get(subInst.subMeshIndex);
		if (subMesh._transType != NO_TRANS) {
			continue; // Render transparent submeshes after
		}

		modelViewMat.mult(subInst.transform, subModelViewMat);

		modelMat.mult(subInst.transform, subModelMat);

		AABB instBounds = subMesh._hull.getAABB(subModelMat);
		if (!cam.collides(instBounds)) {
			continue;
		}
		// Work out distance to the camera
		Vector4d dist = instBounds.getCenter();
		dist.subLocal3(cam.getTransformRef().getTransRef());

		double apparentSize = 2 * instBounds.getRadius().mag3() / dist.mag3();
		if (apparentSize < 0.001) {
			// This object is too small to draw
			continue;
		}

		normalMat.mult(subInst.normalTrans, subNormalMat);

		renderSubMesh(subMesh, vaoMap, renderer, subModelViewMat, subNormalMat, cam);
	}

	for (SubLineInstance subInst : _subLineInstances) {

		modelMat.mult(subInst.transform, subModelMat);

		SubLine subLine = _subLines.get(subInst.subLineIndex);

		AABB instBounds = subLine._hull.getAABB(subModelMat);
		if (!cam.collides(instBounds)) {
			continue;
		}

		modelViewMat.mult(subInst.transform, subModelViewMat);

		renderSubLine(subLine, vaoMap, renderer, subModelViewMat, cam);
	}

}

public void renderTransparent(Map<Integer, Integer> vaoMap, Renderer renderer,
        Matrix4d modelMat,
        Matrix4d normalMat,
        Camera cam) {


	Matrix4d viewMat = new Matrix4d();
	cam.getViewMatrix(viewMat);

	Matrix4d modelViewMat = new Matrix4d();
	viewMat.mult(modelMat, modelViewMat);

	ArrayList<TransSortable> transparents = new ArrayList<TransSortable>();
	for (SubMeshInstance subInst : _subMeshInstances) {

		Matrix4d subModelView = new Matrix4d();
		Matrix4d subNormalMat = new Matrix4d();
		Matrix4d subModelMat = new Matrix4d();

		SubMesh subMesh = _subMeshes.get(subInst.subMeshIndex);
		if (subMesh._transType == NO_TRANS) {
			continue; // Opaque sub meshes have been rendered
		}

		modelMat.mult(subInst.transform, subModelMat);

		AABB instBounds = subMesh._hull.getAABB(subModelMat);
		if (!cam.collides(instBounds)) {
			continue;
		}

		modelViewMat.mult(subInst.transform, subModelView);

		normalMat.mult(subInst.normalTrans, subNormalMat);

		Vector4d eyeCenter = new Vector4d();
		subModelView.mult(subMesh._center, eyeCenter);

		TransSortable ts = new TransSortable();
		ts.subMesh = subMesh;
		ts.modelViewMat = subModelView;
		ts.normalMat = subNormalMat;
		ts.dist = eyeCenter.z();
		transparents.add(ts);
	}

	Collections.sort(transparents);

	for (TransSortable ts : transparents) {
		renderSubMesh(ts.subMesh, vaoMap, renderer, ts.modelViewMat, ts.normalMat, cam);
	}
}

private void setupVAOForSubMesh(Map<Integer, Integer> vaoMap, SubMesh sub, Renderer renderer) {
	GL2GL3 gl = renderer.getGL();

	int[] vaos = new int[1];
	gl.glGenVertexArrays(1, vaos, 0);
	int vao = vaos[0];
	vaoMap.put(sub._id, vao);
	gl.glBindVertexArray(vao);

	int prog = sub._progHandle;
	gl.glUseProgram(prog);

	if (sub._texHandle != 0) {
		// Texture coordinates
		int texCoordVar = gl.glGetAttribLocation(prog, "texCoord");
		gl.glEnableVertexAttribArray(texCoordVar);

		gl.glBindBuffer(GL2GL3.GL_ARRAY_BUFFER, sub._texCoordBuffer);
		gl.glVertexAttribPointer(texCoordVar, 2, GL2GL3.GL_FLOAT, false, 0, 0);
	}

	int posVar = gl.glGetAttribLocation(prog, "position");
	gl.glEnableVertexAttribArray(posVar);

	gl.glBindBuffer(GL2GL3.GL_ARRAY_BUFFER, sub._vertexBuffer);
	gl.glVertexAttribPointer(posVar, 3, GL2GL3.GL_FLOAT, false, 0, 0);

	// Normals
	int normalVar = gl.glGetAttribLocation(prog, "normal");
	gl.glEnableVertexAttribArray(normalVar);

	gl.glBindBuffer(GL2GL3.GL_ARRAY_BUFFER, sub._normalBuffer);
	gl.glVertexAttribPointer(normalVar, 3, GL2GL3.GL_FLOAT, false, 0, 0);

	gl.glBindVertexArray(0);

}

private void renderSubMesh(SubMesh sub, Map<Integer, Integer> vaoMap,
                           Renderer renderer, Matrix4d modelViewMat,
                           Matrix4d normalMat, Camera cam) {

	GL2GL3 gl = renderer.getGL();

	if (!vaoMap.containsKey(sub._id)) {
		setupVAOForSubMesh(vaoMap, sub, renderer);
	}

	int vao = vaoMap.get(sub._id);
	gl.glBindVertexArray(vao);

	int prog = sub._progHandle;
	gl.glUseProgram(prog);

	// Setup uniforms for this object
	Matrix4d modelViewProjMat = new Matrix4d(modelViewMat);

	Matrix4d projMat = cam.getProjMatRef();
	projMat.mult(modelViewProjMat, modelViewProjMat);

	gl.glUniformMatrix4fv(sub._modelViewProjMatVar, 1, false, modelViewProjMat.toFloats(), 0);
	gl.glUniformMatrix4fv(sub._normalMatVar, 1, false, normalMat.toFloats(), 0);

	Vector4d lightVect = new Vector4d(-0.5f,  -0.2f, -0.5,  0);
	lightVect.normalizeLocal3();

	gl.glUniform4fv(sub._lightDirVar, 1, lightVect.toFloats(), 0);

	gl.glUniform1i(sub._useTexVar, (sub._texHandle != 0) ? 1 : 0);

	if (sub._texHandle != 0) {
		gl.glActiveTexture(GL2GL3.GL_TEXTURE0);
		gl.glBindTexture(GL2GL3.GL_TEXTURE_2D, sub._texHandle);
		gl.glUniform1i(sub._texVar, 0);
	} else {
		gl.glUniform4fv(sub._colorVar, 1, sub._diffuseColor.toFloats(), 0);
	}

	if (sub._transType != NO_TRANS) {
		gl.glEnable(GL2GL3.GL_BLEND);
		gl.glBlendEquation(GL2GL3.GL_FUNC_ADD);
		gl.glDepthMask(false);

		gl.glBlendColor((float)sub._transColour.x(),
		                (float)sub._transColour.y(),
		                (float)sub._transColour.z(),
		                (float)sub._transColour.w());

		if (sub._transType == A_ONE_TRANS) {
			gl.glBlendFunc(GL2GL3.GL_CONSTANT_ALPHA, GL2GL3.GL_ONE_MINUS_CONSTANT_ALPHA);
		} else if (sub._transType == RGB_ZERO_TRANS) {
			gl.glBlendFunc(GL2GL3.GL_ONE_MINUS_CONSTANT_COLOR, GL2GL3.GL_CONSTANT_COLOR);
		} else {
			assert(false); // Unknown transparency type
		}
	} else {
		// Just in case this was missed somewhere
		gl.glDisable(GL2GL3.GL_BLEND);
	}

	// Actually draw it
	//gl.glPolygonMode(GL2GL3.GL_FRONT_AND_BACK, GL2GL3.GL_LINE);
	gl.glDisable(GL2GL3.GL_CULL_FACE);

	gl.glDrawArrays(GL2GL3.GL_TRIANGLES, 0, sub._numVerts);
	gl.glEnable(GL2GL3.GL_CULL_FACE);

	if (sub._transType != NO_TRANS) {
		gl.glDisable(GL2GL3.GL_BLEND);
		gl.glDepthMask(true);
	}

	gl.glBindVertexArray(0);

}

private void setupVAOForSubLine(Map<Integer, Integer> vaoMap, SubLine sub, Renderer renderer) {
	GL2GL3 gl = renderer.getGL();

	int[] vaos = new int[1];
	gl.glGenVertexArrays(1, vaos, 0);
	int vao = vaos[0];
	vaoMap.put(sub._id, vao);
	gl.glBindVertexArray(vao);

	int prog = sub._progHandle;
	gl.glUseProgram(prog);

	int posVar = gl.glGetAttribLocation(prog, "position");
	gl.glEnableVertexAttribArray(posVar);

	gl.glBindBuffer(GL2GL3.GL_ARRAY_BUFFER, sub._vertexBuffer);
	gl.glVertexAttribPointer(posVar, 3, GL2GL3.GL_FLOAT, false, 0, 0);

	gl.glBindVertexArray(0);

}

private void renderSubLine(SubLine sub, Map<Integer, Integer> vaoMap,
        Renderer renderer, Matrix4d modelViewMat, Camera cam) {

	GL2GL3 gl = renderer.getGL();

	if (!vaoMap.containsKey(sub._id)) {
		setupVAOForSubLine(vaoMap, sub, renderer);
	}

	int vao = vaoMap.get(sub._id);
	gl.glBindVertexArray(vao);

	int prog = sub._progHandle;
	gl.glUseProgram(prog);

	Matrix4d projMat = cam.getProjMatRef();

	gl.glUniformMatrix4fv(sub._modelViewMatVar, 1, false, modelViewMat.toFloats(), 0);
	gl.glUniformMatrix4fv(sub._projMatVar, 1, false, projMat.toFloats(), 0);

	gl.glUniform4fv(sub._colorVar, 1, sub._diffuseColor.toFloats(), 0);

	gl.glLineWidth(1);

	// Actually draw it
	gl.glDrawArrays(GL2GL3.GL_LINES, 0, sub._numVerts);

	gl.glBindVertexArray(0);

}

/**
 * Push all the data to the GPU and free up CPU side ram
 * @param gl
 */
public void loadGPUAssets(GL2GL3 gl, Renderer renderer) {
	assert(!_isLoadedGPU);

	for (SubMeshData data : _subMeshesData) {
		loadGPUSubMesh(gl, renderer, data);
	}
	for (SubLineData data : _subLinesData) {
		loadGPUSubLine(gl, renderer, data);
	}

	_isLoadedGPU = true;
}

private void loadGPUSubMesh(GL2GL3 gl, Renderer renderer, SubMeshData data) {

	boolean hasTex = data.colorTex != null;

	SubMesh sub = new SubMesh();
	sub._progHandle = renderer.getShader(Renderer.ShaderHandle.MESH).getProgramHandle();

	if (hasTex) {
		int[] is = new int[3];
		gl.glGenBuffers(3, is, 0);
		sub._vertexBuffer = is[0];
		sub._normalBuffer = is[1];
		sub._texCoordBuffer = is[2];
	} else {
		int[] is = new int[2];
		gl.glGenBuffers(2, is, 0);
		sub._vertexBuffer = is[0];
		sub._normalBuffer = is[1];
		sub._texCoordBuffer = 0;
	}

	sub._transType = data.transType;
	sub._transColour = data.transColour;

	sub._hull = data.hull;

	sub._center = data.hull.getAABB(Matrix4d.IDENT).getCenter();

	sub._numVerts = data.verts.size();

	// Init vertices
	FloatBuffer fb = FloatBuffer.allocate(data.verts.size() * 3); //
	for (Vector4d v : data.verts) {
		fb.put(v.toFloats(), 0, 3);
	}
	fb.flip();

	gl.glBindBuffer(GL2GL3.GL_ARRAY_BUFFER, sub._vertexBuffer);
	gl.glBufferData(GL2GL3.GL_ARRAY_BUFFER, sub._numVerts * 3 * 4, fb, GL2GL3.GL_STATIC_DRAW);

	// Bind the shader variables
	sub._modelViewProjMatVar = gl.glGetUniformLocation(sub._progHandle, "modelViewProjMat");
	sub._normalMatVar = gl.glGetUniformLocation(sub._progHandle, "normalMat");
	sub._lightDirVar = gl.glGetUniformLocation(sub._progHandle, "lightDir");
	sub._colorVar = gl.glGetUniformLocation(sub._progHandle, "diffuseColor");
	sub._texVar = gl.glGetUniformLocation(sub._progHandle, "tex");
	sub._useTexVar = gl.glGetUniformLocation(sub._progHandle, "useTex");

	// Init textureCoords
	if (hasTex) {

		fb = FloatBuffer.allocate(sub._numVerts * 2); //
		for (Vector4d v : data.texCoords) {
			float[] fs = v.toFloats();
			fb.put(fs[0]); // x
			fb.put(fs[1]); // y
		}
		fb.flip();

		gl.glBindBuffer(GL2GL3.GL_ARRAY_BUFFER, sub._texCoordBuffer);
		gl.glBufferData(GL2GL3.GL_ARRAY_BUFFER, sub._numVerts * 2 * 4, fb, GL2GL3.GL_STATIC_DRAW);

		sub._texHandle = renderer.getTexCache().getTexID(gl, data.colorTex, (data.transType != NO_TRANS), false, true);
	}
	else
	{
		sub._texHandle = 0;
		sub._diffuseColor = new Vector4d(data.diffuseColor);
	}

	// Init normals
	fb = FloatBuffer.allocate(sub._numVerts * 3); //
	for (Vector4d v : data.normals) {
		fb.put(v.toFloats(), 0, 3);
	}
	fb.flip();

	gl.glBindBuffer(GL2GL3.GL_ARRAY_BUFFER, sub._normalBuffer);
	gl.glBufferData(GL2GL3.GL_ARRAY_BUFFER, sub._numVerts * 3 * 4, fb, GL2GL3.GL_STATIC_DRAW);

	_subMeshes.add(sub);
}

private void loadGPUSubLine(GL2GL3 gl, Renderer renderer, SubLineData data) {

	Shader s = renderer.getShader(Renderer.ShaderHandle.DEBUG);

	assert (s.isGood());

	SubLine sub = new SubLine();
	sub._progHandle = s.getProgramHandle();

	int[] is = new int[1];
	gl.glGenBuffers(1, is, 0);
	sub._vertexBuffer = is[0];

	sub._numVerts = data.verts.size();

	sub._hull = data.hull;

	// Init vertices
	FloatBuffer fb = FloatBuffer.allocate(data.verts.size() * 3); //
	for (Vector4d v : data.verts) {
		fb.put(v.toFloats(), 0, 3);
	}
	fb.flip();

	gl.glBindBuffer(GL2GL3.GL_ARRAY_BUFFER, sub._vertexBuffer);
	gl.glBufferData(GL2GL3.GL_ARRAY_BUFFER, sub._numVerts * 3 * 4, fb, GL2GL3.GL_STATIC_DRAW);

	// Bind the shader variables
	sub._modelViewMatVar = gl.glGetUniformLocation(sub._progHandle, "modelViewMat");
	sub._projMatVar = gl.glGetUniformLocation(sub._progHandle, "projMat");

	sub._diffuseColor = new Vector4d(data.diffuseColor);
	sub._colorVar = gl.glGetUniformLocation(sub._progHandle, "color");

	_subLines.add(sub);
}

/**
 * Has this mesh been loaded into GPU memory?
 * @return
 */
public boolean isLoadedGPU() {
	return _isLoadedGPU;
}

public void freeResources(GL2GL3 gl) {

	for (SubMesh sub : _subMeshes) {
		int[] bufs = new int[3];
		bufs[0] = sub._vertexBuffer;
		bufs[1] = sub._normalBuffer;
		bufs[2] = sub._texCoordBuffer;

		gl.glDeleteBuffers(3, bufs, 0);
	}

	_subMeshes.clear();

}

public double getRadius() {
	return _radius;
}

public ConvexHull getHull() {
	return _hull;
}

/**
 * Builds the convex hull of the current mesh based on all the existing sub meshes.
 */
public void generateHull() {
	ArrayList<Vector4d> totalHullPoints = new ArrayList<Vector4d>();
	// Collect all the points from the hulls of the individual sub meshes
	for (SubMeshInstance subInst : _subMeshInstances) {

		List<Vector4d> pointsRef = _subMeshesData.get(subInst.subMeshIndex).hull.getVertices();
		List<Vector4d> subPoints = RenderUtils.transformPoints(subInst.transform, pointsRef);

		totalHullPoints.addAll(subPoints);
	}
	// And the lines
	for (SubLineInstance subInst : _subLineInstances) {

		List<Vector4d> pointsRef = _subLinesData.get(subInst.subLineIndex).hull.getVertices();
		List<Vector4d> subPoints = RenderUtils.transformPoints(subInst.transform, pointsRef);

		totalHullPoints.addAll(subPoints);
	}

	_hull = ConvexHull.TryBuildHull(totalHullPoints, 5);

	_radius = _hull.getRadius();
}

} // class MeshProto
