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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;

import javax.media.opengl.GL2GL3;

import com.jaamsim.MeshFiles.MeshData;
import com.jaamsim.math.AABB;
import com.jaamsim.math.Color4d;
import com.jaamsim.math.ConvexHull;
import com.jaamsim.math.Mat4d;
import com.jaamsim.math.Vec4d;

/**
 * A basic wrapper around mesh data and the OpenGL calls that go with it
 * Will maintain a vertex buffer for vertex data, texture coordinates and normal data
 * The first pass is non-indexed but will be updated in the future
 * @author Matt Chudleigh
 *
 */
public class MeshProto {

/** Sortable entry for a single transparent sub mesh before rendering
 *
 * @author matt.chudleigh
 *
 */
private static class TransSortable implements Comparable<TransSortable> {
	public SubMesh subMesh;
	public Material mat;
	public double dist;
	public Mat4d modelViewMat;
	public Mat4d normalMat;

	@Override
	public int compareTo(TransSortable o) {
		// Sort such that largest distance sorts to front of list
		// by reversing argument order in compare.
		return Double.compare(o.dist, this.dist);
	}
}

private class SubMesh {

	SubMesh() {
		_id = Renderer.getAssetID();
	}

	public int _vertexBuffer;
	public int _texCoordBuffer;
	public int _normalBuffer;

	public int _progHandle;

	public int _modelViewProjMatVar;
	public int _normalMatVar;
	public int _lightDirVar;
	public int _texVar;
	public int _colorVar;
	public int _useTexVar;


	public Vec4d _center;

	public int _numVerts;
	public int _id; // The system wide asset ID

}

private class Material {
	public int _texHandle;
	public Color4d _diffuseColor;

	public int _transType;
	public Color4d _transColour;
}

private class SubLine {

	SubLine() {
		_id = Renderer.getAssetID();
	}

	public int _vertexBuffer;
	public Color4d _diffuseColor;

	public int _progHandle;

	public int _modelViewMatVar;
	public int _projMatVar;
	public int _colorVar;

	public ConvexHull _hull;

	public int _numVerts;
	public int _id; // The system wide asset ID
}

private MeshData data;
private ArrayList<SubMesh> _subMeshes;
private ArrayList<SubLine> _subLines;
private ArrayList<Material> _materials;

/**
 * The maximum distance a vertex is from the origin
 */
private boolean _isLoadedGPU = false;

public MeshProto(MeshData data) {
	this.data = data;
	_subMeshes = new ArrayList<SubMesh>();
	_subLines = new ArrayList<SubLine>();
	_materials = new ArrayList<Material>();
}

public void render(Map<Integer, Integer> vaoMap, Renderer renderer,
                   Mat4d modelMat,
                   Mat4d normalMat,
                   Camera cam, ArrayList<AABB> subInstBounds) {

	assert(_isLoadedGPU);

	Mat4d viewMat = new Mat4d();
	cam.getViewMat4d(viewMat);

	Mat4d modelViewMat = new Mat4d();
	modelViewMat.mult4(viewMat, modelMat);

	Mat4d subModelViewMat = new Mat4d();
	Mat4d subModelMat = new Mat4d();
	Mat4d subNormalMat = new Mat4d();


	Vec4d dist = new Vec4d(0.0d, 0.0d, 0.0d, 1.0d);

	for (int i = 0; i < data.getSubMeshInstances().size(); ++i) {
		MeshData.SubMeshInstance subInst = data.getSubMeshInstances().get(i);

		SubMesh subMesh = _subMeshes.get(subInst.subMeshIndex);
		Material mat = _materials.get(subInst.materialIndex);
		if (mat._transType != MeshData.NO_TRANS) {
			continue; // Render transparent submeshes after
		}

		subModelViewMat.mult4(modelViewMat, subInst.transform);

		subModelMat.mult4(modelMat, subInst.transform);

		AABB instBounds = subInstBounds.get(i);
		if (!cam.collides(instBounds)) {
			continue;
		}

		// Work out distance to the camera
		dist.set4(instBounds.getCenter());
		dist.sub3(cam.getTransformRef().getTransRef());

		double apparentSize = 2 * instBounds.getRadius().mag3() / dist.mag3();
		if (apparentSize < 0.001) {
			// This object is too small to draw
			continue;
		}

		subNormalMat.mult4(normalMat, subInst.normalTrans);

		renderSubMesh(subMesh, mat, vaoMap, renderer, subModelViewMat, subNormalMat, cam);
	}

	for (MeshData.SubLineInstance subInst : data.getSubLineInstances()) {

		subModelMat.mult4(modelMat, subInst.transform);

		SubLine subLine = _subLines.get(subInst.subLineIndex);

		AABB instBounds = subLine._hull.getAABB(subModelMat);
		if (!cam.collides(instBounds)) {
			continue;
		}

		subModelViewMat.mult4(modelViewMat, subInst.transform);

		renderSubLine(subLine, vaoMap, renderer, subModelViewMat, cam);
	}

}

public void renderTransparent(Map<Integer, Integer> vaoMap, Renderer renderer,
        Mat4d modelMat,
        Mat4d normalMat,
        Camera cam, ArrayList<AABB> subInstBounds) {


	Mat4d viewMat = new Mat4d();
	cam.getViewMat4d(viewMat);

	Mat4d modelViewMat = new Mat4d();
	modelViewMat.mult4(viewMat, modelMat);

	ArrayList<TransSortable> transparents = new ArrayList<TransSortable>();
	for (int i = 0; i < data.getSubMeshInstances().size(); ++i) {
		MeshData.SubMeshInstance subInst = data.getSubMeshInstances().get(i);

		Mat4d subModelView = new Mat4d();
		Mat4d subNormalMat = new Mat4d();
		Mat4d subModelMat = new Mat4d();

		SubMesh subMesh = _subMeshes.get(subInst.subMeshIndex);
		Material mat = _materials.get(subInst.materialIndex);

		if (mat._transType == MeshData.NO_TRANS) {
			continue; // Opaque sub meshes have been rendered
		}

		subModelMat.mult4(modelMat, subInst.transform);

		AABB instBounds = subInstBounds.get(i);
		if (!cam.collides(instBounds)) {
			continue;
		}

		subModelView.mult4(modelViewMat, subInst.transform);

		subNormalMat.mult4(normalMat, subInst.normalTrans);

		Vec4d eyeCenter = new Vec4d(0.0d, 0.0d, 0.0d, 1.0d);
		eyeCenter.mult4(subModelView, subMesh._center);

		TransSortable ts = new TransSortable();
		ts.subMesh = subMesh;
		ts.mat = mat;
		ts.modelViewMat = subModelView;
		ts.normalMat = subNormalMat;
		ts.dist = eyeCenter.z;
		transparents.add(ts);
	}

	Collections.sort(transparents);

	for (TransSortable ts : transparents) {
		renderSubMesh(ts.subMesh, ts.mat, vaoMap, renderer, ts.modelViewMat, ts.normalMat, cam);
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

	if (sub._texCoordBuffer != 0) {
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

private void renderSubMesh(SubMesh sub, Material mat, Map<Integer, Integer> vaoMap,
                           Renderer renderer, Mat4d modelViewMat,
                           Mat4d normalMat, Camera cam) {

	GL2GL3 gl = renderer.getGL();

	if (!vaoMap.containsKey(sub._id)) {
		setupVAOForSubMesh(vaoMap, sub, renderer);
	}

	int vao = vaoMap.get(sub._id);
	gl.glBindVertexArray(vao);

	int prog = sub._progHandle;
	gl.glUseProgram(prog);

	// Setup uniforms for this object
	Mat4d modelViewProjMat = new Mat4d(modelViewMat);

	Mat4d projMat = cam.getProjMat4d();
	modelViewProjMat.mult4(projMat, modelViewProjMat);

	gl.glUniformMatrix4fv(sub._modelViewProjMatVar, 1, false, RenderUtils.MarshalMat4d(modelViewProjMat), 0);
	gl.glUniformMatrix4fv(sub._normalMatVar, 1, false, RenderUtils.MarshalMat4d(normalMat), 0);

	Vec4d lightVect = new Vec4d(-0.5f,  -0.2f, -0.5,  0);
	lightVect.normalize3();

	gl.glUniform4f(sub._lightDirVar, (float)lightVect.x, (float)lightVect.y, (float)lightVect.z, (float)lightVect.w);

	gl.glUniform1i(sub._useTexVar, (mat._texHandle != 0) ? 1 : 0);

	if (mat._texHandle != 0) {
		gl.glActiveTexture(GL2GL3.GL_TEXTURE0);
		gl.glBindTexture(GL2GL3.GL_TEXTURE_2D, mat._texHandle);
		gl.glUniform1i(sub._texVar, 0);
	} else {
		gl.glUniform4fv(sub._colorVar, 1, mat._diffuseColor.toFloats(), 0);
	}

	if (mat._transType != MeshData.NO_TRANS) {
		gl.glEnable(GL2GL3.GL_BLEND);
		gl.glBlendEquationSeparate(GL2GL3.GL_FUNC_ADD, GL2GL3.GL_MAX);
		gl.glDepthMask(false);

		gl.glBlendColor((float)mat._transColour.r,
		                (float)mat._transColour.g,
		                (float)mat._transColour.b,
		                (float)mat._transColour.a);

		if (mat._transType == MeshData.A_ONE_TRANS) {
			gl.glBlendFuncSeparate(GL2GL3.GL_CONSTANT_ALPHA, GL2GL3.GL_ONE_MINUS_CONSTANT_ALPHA, GL2GL3.GL_ONE, GL2GL3.GL_ONE);
		} else if (mat._transType == MeshData.RGB_ZERO_TRANS) {
			gl.glBlendFuncSeparate(GL2GL3.GL_ONE_MINUS_CONSTANT_COLOR, GL2GL3.GL_CONSTANT_COLOR, GL2GL3.GL_ONE, GL2GL3.GL_ONE);
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

	if (mat._transType != MeshData.NO_TRANS) {
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
        Renderer renderer, Mat4d modelViewMat, Camera cam) {

	GL2GL3 gl = renderer.getGL();

	if (!vaoMap.containsKey(sub._id)) {
		setupVAOForSubLine(vaoMap, sub, renderer);
	}

	int vao = vaoMap.get(sub._id);
	gl.glBindVertexArray(vao);

	int prog = sub._progHandle;
	gl.glUseProgram(prog);

	Mat4d projMat = cam.getProjMat4d();

	gl.glUniformMatrix4fv(sub._modelViewMatVar, 1, false, RenderUtils.MarshalMat4d(modelViewMat), 0);
	gl.glUniformMatrix4fv(sub._projMatVar, 1, false, RenderUtils.MarshalMat4d(projMat), 0);

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

	for (MeshData.SubMeshData subData : data.getSubMeshData()) {
		loadGPUSubMesh(gl, renderer, subData);
	}
	for (MeshData.SubLineData subData : data.getSubLineData()) {
		loadGPUSubLine(gl, renderer, subData);
	}
	for (MeshData.Material mat : data.getMaterials()) {
		loadGPUMaterial(gl, renderer, mat);
	}

	_isLoadedGPU = true;
}

private void loadGPUMaterial(GL2GL3 gl, Renderer renderer, MeshData.Material dataMat) {

	boolean hasTex = dataMat.colorTex != null;

	Material mat = new Material();
	mat._transType = dataMat.transType;
	mat._transColour = dataMat.transColour;

	if (hasTex) {
		mat._texHandle = renderer.getTexCache().getTexID(gl, dataMat.colorTex, (dataMat.transType != MeshData.NO_TRANS), false, true);
	} else {
		mat._texHandle = 0;
		mat._diffuseColor = new Color4d(dataMat.diffuseColor);
	}
	_materials.add(mat);
}

private void loadGPUSubMesh(GL2GL3 gl, Renderer renderer, MeshData.SubMeshData data) {

	boolean hasTex = data.texCoords != null;

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

	sub._center = data.hull.getAABB(Mat4d.IDENTITY).getCenter();

	sub._numVerts = data.verts.size();

	// Init vertices
	FloatBuffer fb = FloatBuffer.allocate(data.verts.size() * 3); //
	for (Vec4d v : data.verts) {
		RenderUtils.putPointXYZ(fb, v);
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
		for (Vec4d v : data.texCoords) {
			RenderUtils.putPointXY(fb, v);
		}
		fb.flip();

		gl.glBindBuffer(GL2GL3.GL_ARRAY_BUFFER, sub._texCoordBuffer);
		gl.glBufferData(GL2GL3.GL_ARRAY_BUFFER, sub._numVerts * 2 * 4, fb, GL2GL3.GL_STATIC_DRAW);
	}

	// Init normals
	fb = FloatBuffer.allocate(sub._numVerts * 3); //
	for (Vec4d v : data.normals) {
		RenderUtils.putPointXYZ(fb, v);
	}
	fb.flip();

	gl.glBindBuffer(GL2GL3.GL_ARRAY_BUFFER, sub._normalBuffer);
	gl.glBufferData(GL2GL3.GL_ARRAY_BUFFER, sub._numVerts * 3 * 4, fb, GL2GL3.GL_STATIC_DRAW);

	_subMeshes.add(sub);
}

private void loadGPUSubLine(GL2GL3 gl, Renderer renderer, MeshData.SubLineData data) {

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
	for (Vec4d v : data.verts) {
		RenderUtils.putPointXYZ(fb, v);
	}
	fb.flip();

	gl.glBindBuffer(GL2GL3.GL_ARRAY_BUFFER, sub._vertexBuffer);
	gl.glBufferData(GL2GL3.GL_ARRAY_BUFFER, sub._numVerts * 3 * 4, fb, GL2GL3.GL_STATIC_DRAW);

	// Bind the shader variables
	sub._modelViewMatVar = gl.glGetUniformLocation(sub._progHandle, "modelViewMat");
	sub._projMatVar = gl.glGetUniformLocation(sub._progHandle, "projMat");

	sub._diffuseColor = new Color4d(data.diffuseColor);
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

public ConvexHull getHull() {
	return data.getHull();
}

public boolean hasTransparent() {
	return data.hasTransparent();
}

public ArrayList<AABB> getSubBounds(Mat4d modelMat) {
	return data.getSubBounds(modelMat);
}

} // class MeshProto
