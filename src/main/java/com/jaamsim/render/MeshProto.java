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

	public MeshData.SubMeshInstance subInst;

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
	public int _indexBuffer;

	public int _boneIndicesBuffer;
	public int _boneWeightsBuffer;

	public int _progHandle;

	public int _modelViewProjMatVar;
	public int _normalMatVar;
	public int _lightDirVar;
	public int _texVar;
	public int _colorVar;
	public int _useTexVar;
	public int _maxNumBonesVar;

	public int _boneMatricesVar;

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

private final boolean flattenBuffers;

public MeshProto(MeshData data, boolean flattenBuffers) {
	this.flattenBuffers = flattenBuffers;
	this.data = data;
	_subMeshes = new ArrayList<SubMesh>();
	_subLines = new ArrayList<SubLine>();
	_materials = new ArrayList<Material>();
}

public void render(Map<Integer, Integer> vaoMap, Renderer renderer,
                   Mat4d modelMat,
                   Mat4d normalMat,
                   Camera cam,
                   ArrayList<Action.Queue> actions,
                   ArrayList<AABB> subInstBounds) {

	assert(_isLoadedGPU);

	Mat4d viewMat = new Mat4d();
	cam.getViewMat4d(viewMat);

	Mat4d modelViewMat = new Mat4d();
	modelViewMat.mult4(viewMat, modelMat);

	Mat4d subModelViewMat = new Mat4d();
	Mat4d subModelMat = new Mat4d();
	Mat4d subNormalMat = new Mat4d();

	ArrayList<ArrayList<Mat4d>> poses = null;
	if (actions != null) {
		ArrayList<Armature> arms = data.getArmatures();
		// Run the actions through all armatures attached to this mesh
		poses = new ArrayList<ArrayList<Mat4d>>(arms.size());
		for (int i = 0; i < arms.size(); ++i) {
			Armature arm = arms.get(i);
			ArrayList<Mat4d> pose = arm.getPose(actions);
			poses.add(pose);
		}
	}

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

		ArrayList<Mat4d> pose = null;
		if (subInst.armatureIndex != -1 && poses != null) {
			pose = poses.get(subInst.armatureIndex);
		}

		subNormalMat.mult4(normalMat, subInst.normalTrans);

		renderSubMesh(subMesh, mat, vaoMap, renderer, subModelViewMat, subNormalMat,
		              pose ,subInst.boneMapper, cam);
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
        Camera cam,
        ArrayList<Action.Queue> actions,
        ArrayList<AABB> subInstBounds) {


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
		ts.subInst = subInst;
		transparents.add(ts);
	}

	ArrayList<ArrayList<Mat4d>> poses = null;
	if (actions != null) {
		ArrayList<Armature> arms = data.getArmatures();
		// Run the actions through all armatures attached to this mesh
		poses = new ArrayList<ArrayList<Mat4d>>(arms.size());
		for (int i = 0; i < arms.size(); ++i) {
			Armature arm = arms.get(i);
			ArrayList<Mat4d> pose = arm.getPose(actions);
			poses.add(pose);
		}
	}

	Collections.sort(transparents);

	for (TransSortable ts : transparents) {
		ArrayList<Mat4d> pose = null;
		if (ts.subInst.armatureIndex != -1 && poses != null) {
			pose = poses.get(ts.subInst.armatureIndex);
		}

		renderSubMesh(ts.subMesh, ts.mat, vaoMap, renderer, ts.modelViewMat, ts.normalMat, pose, ts.subInst.boneMapper, cam);
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

	if (sub._boneIndicesBuffer != 0) {
		// Indices
		int boneIndicesVar = gl.glGetAttribLocation(prog, "boneIndices");
		gl.glEnableVertexAttribArray(boneIndicesVar);

		gl.glBindBuffer(GL2GL3.GL_ARRAY_BUFFER, sub._boneIndicesBuffer);
		gl.glVertexAttribPointer(boneIndicesVar, 4, GL2GL3.GL_FLOAT, false, 0, 0);

		// Weights
		int boneWeightsVar = gl.glGetAttribLocation(prog, "boneWeights");
		gl.glEnableVertexAttribArray(boneWeightsVar);

		gl.glBindBuffer(GL2GL3.GL_ARRAY_BUFFER, sub._boneWeightsBuffer);
		gl.glVertexAttribPointer(boneWeightsVar, 4, GL2GL3.GL_FLOAT, false, 0, 0);
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

	if (!flattenBuffers) {
		gl.glBindBuffer(GL2GL3.GL_ELEMENT_ARRAY_BUFFER, sub._indexBuffer);
	}

	gl.glBindVertexArray(0);

}

private void renderSubMesh(SubMesh sub, Material mat, Map<Integer, Integer> vaoMap,
                           Renderer renderer, Mat4d modelViewMat,
                           Mat4d normalMat,
                           ArrayList<Mat4d> pose,
                           int[] boneMap, Camera cam) {

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

	// Build up the pose matrices

	if (pose != null) {
		float[] poseMatrices = new float[16*pose.size()];
		for (int i = 0; i < pose.size(); ++i) {
			int poseIndex = boneMap[i];
			RenderUtils.MarshalMat4dToArray(pose.get(poseIndex), poseMatrices, i*16);
		}
		gl.glUniformMatrix4fv(sub._boneMatricesVar, pose.size(), false, poseMatrices, 0);
	}

	gl.glUniform1i(sub._maxNumBonesVar, (pose == null) ? 0 : 4);

	// Actually draw it
	//gl.glPolygonMode(GL2GL3.GL_FRONT_AND_BACK, GL2GL3.GL_LINE);
	gl.glDisable(GL2GL3.GL_CULL_FACE);

	if (flattenBuffers) {
		gl.glDrawArrays(GL2GL3.GL_TRIANGLES, 0, sub._numVerts);
	} else {
		gl.glDrawElements(GL2GL3.GL_TRIANGLES, sub._numVerts, GL2GL3.GL_UNSIGNED_INT, 0);
	}
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
	boolean hasBoneInfo = data.boneIndices != null;

	SubMesh sub = new SubMesh();
	sub._progHandle = renderer.getShader(Renderer.ShaderHandle.MESH).getProgramHandle();

	int[] is = new int[3];
	gl.glGenBuffers(3, is, 0);
	sub._vertexBuffer = is[0];
	sub._normalBuffer = is[1];
	sub._indexBuffer = is[2];

	if (hasTex) {
		gl.glGenBuffers(1, is, 0);
		sub._texCoordBuffer = is[0];
	}

	if (hasBoneInfo) {
		gl.glGenBuffers(2, is, 0);
		sub._boneIndicesBuffer = is[0];
		sub._boneWeightsBuffer = is[1];
	}

	sub._center = data.hull.getAABB(Mat4d.IDENTITY).getCenter();

	sub._numVerts = data.indices.length;

	if (flattenBuffers) {
		FloatBuffer fb = FloatBuffer.allocate(data.indices.length * 3); //
		for (int ind : data.indices) {
			RenderUtils.putPointXYZ(fb, data.verts.get(ind));
		}
		fb.flip();

		gl.glBindBuffer(GL2GL3.GL_ARRAY_BUFFER, sub._vertexBuffer);
		gl.glBufferData(GL2GL3.GL_ARRAY_BUFFER, data.indices.length * 3 * 4, fb, GL2GL3.GL_STATIC_DRAW);
	} else
	{
		// Init vertices
		FloatBuffer fb = FloatBuffer.allocate(data.verts.size() * 3); //
		for (Vec4d v : data.verts) {
			RenderUtils.putPointXYZ(fb, v);
		}
		fb.flip();

		gl.glBindBuffer(GL2GL3.GL_ARRAY_BUFFER, sub._vertexBuffer);
		gl.glBufferData(GL2GL3.GL_ARRAY_BUFFER, data.verts.size() * 3 * 4, fb, GL2GL3.GL_STATIC_DRAW);
	}

	// Bind the shader variables
	sub._modelViewProjMatVar = gl.glGetUniformLocation(sub._progHandle, "modelViewProjMat");
	sub._normalMatVar = gl.glGetUniformLocation(sub._progHandle, "normalMat");
	sub._lightDirVar = gl.glGetUniformLocation(sub._progHandle, "lightDir");
	sub._colorVar = gl.glGetUniformLocation(sub._progHandle, "diffuseColor");
	sub._texVar = gl.glGetUniformLocation(sub._progHandle, "tex");
	sub._useTexVar = gl.glGetUniformLocation(sub._progHandle, "useTex");
	sub._maxNumBonesVar = gl.glGetUniformLocation(sub._progHandle, "maxNumBones");

	// Init textureCoords
	if (hasTex) {

		if (flattenBuffers) {
			FloatBuffer fb = FloatBuffer.allocate(data.indices.length * 2); //
			for (int ind : data.indices) {
				RenderUtils.putPointXY(fb, data.texCoords.get(ind));
			}
			fb.flip();

			gl.glBindBuffer(GL2GL3.GL_ARRAY_BUFFER, sub._texCoordBuffer);
			gl.glBufferData(GL2GL3.GL_ARRAY_BUFFER, data.indices.length * 2 * 4, fb, GL2GL3.GL_STATIC_DRAW);

		} else
		{
			FloatBuffer fb = FloatBuffer.allocate(data.texCoords.size() * 2); //
			for (Vec4d v : data.texCoords) {
				RenderUtils.putPointXY(fb, v);
			}
			fb.flip();

			gl.glBindBuffer(GL2GL3.GL_ARRAY_BUFFER, sub._texCoordBuffer);
			gl.glBufferData(GL2GL3.GL_ARRAY_BUFFER, data.texCoords.size() * 2 * 4, fb, GL2GL3.GL_STATIC_DRAW);
		}
	}

	if (hasBoneInfo) {
		if (flattenBuffers) {
			FloatBuffer fb = FloatBuffer.allocate(data.indices.length * 4); //
			for (int ind : data.indices) {
				RenderUtils.putPointXYZW(fb, data.boneIndices.get(ind));
			}
			fb.flip();

			gl.glBindBuffer(GL2GL3.GL_ARRAY_BUFFER, sub._boneIndicesBuffer);
			gl.glBufferData(GL2GL3.GL_ARRAY_BUFFER, data.indices.length * 4 * 4, fb, GL2GL3.GL_STATIC_DRAW);

			fb = FloatBuffer.allocate(data.indices.length * 4); //
			for (int ind : data.indices) {
				RenderUtils.putPointXYZW(fb, data.boneWeights.get(ind));
			}
			fb.flip();

			gl.glBindBuffer(GL2GL3.GL_ARRAY_BUFFER, sub._boneWeightsBuffer);
			gl.glBufferData(GL2GL3.GL_ARRAY_BUFFER, data.indices.length * 4 * 4, fb, GL2GL3.GL_STATIC_DRAW);
		}
		else {
			sub._boneMatricesVar = gl.glGetUniformLocation(sub._progHandle, "boneMatrices");

			// Indices
			FloatBuffer fb = FloatBuffer.allocate(data.boneIndices.size() * 4); //
			for (Vec4d v : data.boneIndices) {
				RenderUtils.putPointXYZW(fb, v);
			}
			fb.flip();

			gl.glBindBuffer(GL2GL3.GL_ARRAY_BUFFER, sub._boneIndicesBuffer);
			gl.glBufferData(GL2GL3.GL_ARRAY_BUFFER, data.boneIndices.size() * 4 * 4, fb, GL2GL3.GL_STATIC_DRAW);

			// Weights
			fb = FloatBuffer.allocate(data.boneWeights.size() * 4); //
			for (Vec4d v : data.boneWeights) {
				RenderUtils.putPointXYZW(fb, v);
			}
			fb.flip();

			gl.glBindBuffer(GL2GL3.GL_ARRAY_BUFFER, sub._boneWeightsBuffer);
			gl.glBufferData(GL2GL3.GL_ARRAY_BUFFER, data.boneWeights.size() * 4 * 4, fb, GL2GL3.GL_STATIC_DRAW);
		}
	}

	if (flattenBuffers) {
		FloatBuffer fb = FloatBuffer.allocate(data.indices.length * 3); //
		for (int ind : data.indices) {
			RenderUtils.putPointXYZ(fb, data.normals.get(ind));
		}
		fb.flip();

		gl.glBindBuffer(GL2GL3.GL_ARRAY_BUFFER, sub._normalBuffer);
		gl.glBufferData(GL2GL3.GL_ARRAY_BUFFER, data.indices.length * 3 * 4, fb, GL2GL3.GL_STATIC_DRAW);
	} else
	{
		// Init normals
		FloatBuffer fb = FloatBuffer.allocate(data.normals.size() * 3);
		for (Vec4d v : data.normals) {
			RenderUtils.putPointXYZ(fb, v);
		}
		fb.flip();

		gl.glBindBuffer(GL2GL3.GL_ARRAY_BUFFER, sub._normalBuffer);
		gl.glBufferData(GL2GL3.GL_ARRAY_BUFFER, data.normals.size() * 3 * 4, fb, GL2GL3.GL_STATIC_DRAW);
	}


	if (flattenBuffers) {
		is[0] = sub._indexBuffer;
		// Clear the unneeded buffer object
		gl.glDeleteBuffers(1, is, 0);
	} else
	{
		IntBuffer indexBuffer = IntBuffer.wrap(data.indices);
		gl.glBindBuffer(GL2GL3.GL_ELEMENT_ARRAY_BUFFER, sub._indexBuffer);
		gl.glBufferData(GL2GL3.GL_ELEMENT_ARRAY_BUFFER, sub._numVerts * 4, indexBuffer, GL2GL3.GL_STATIC_DRAW);

	}

	gl.glBindBuffer(GL2GL3.GL_ARRAY_BUFFER, 0);
	gl.glBindBuffer(GL2GL3.GL_ELEMENT_ARRAY_BUFFER, 0);

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
		int[] bufs = new int[6];
		bufs[0] = sub._vertexBuffer;
		bufs[1] = sub._normalBuffer;
		bufs[2] = sub._texCoordBuffer;
		bufs[3] = sub._boneIndicesBuffer;
		bufs[4] = sub._boneWeightsBuffer;
		bufs[5] = sub._indexBuffer;

		gl.glDeleteBuffers(6, bufs, 0);

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

public MeshData getRawData() {
	return data;
}

} // class MeshProto
