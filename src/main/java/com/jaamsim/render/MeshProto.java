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
import java.util.HashMap;

import javax.media.opengl.GL2GL3;
import javax.media.opengl.GLException;

import com.jaamsim.MeshFiles.MeshData;
import com.jaamsim.math.AABB;
import com.jaamsim.math.Color4d;
import com.jaamsim.math.ConvexHull;
import com.jaamsim.math.Mat4d;
import com.jaamsim.math.Vec2d;
import com.jaamsim.math.Vec3d;
import com.jaamsim.math.Vec4d;
import com.jaamsim.ui.LogBox;

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
	public double dist;

	public MeshData.SubMeshInstance subInst;

	@Override
	public int compareTo(TransSortable o) {
		// Sort such that largest distance sorts to front of list
		// by reversing argument order in compare.
		return Double.compare(o.dist, this.dist);
	}
}

private static class ShaderInfo {
	private int meshProgHandle;

	private int modelViewMatVar;
	private int projMatVar;
	private int bindSpaceMatVar;
	private int bindSpaceNorMatVar;
	private int normalMatVar;
	private int texVar;
	private int diffuseColorVar;
	private int specColorVar;
	private int ambientColorVar;
	private int shininessVar;
	private int maxNumBonesVar;

	private int lightDirVar;
	private int lightIntVar;
	private int numLightsVar;

	private int cVar;
	private int fcVar;

	private int boneMatricesVar;
}

private static ShaderInfo[] sInfos = new ShaderInfo[Renderer.NUM_MESH_SHADERS];

private static Vec4d[] lightsDir = new Vec4d[2];
private static Vec4d[] lightsDirScratch = new Vec4d[2];
private static float[] lightsDirFloats = new float[6];
private static float[] lightsInt = new float[2];
private static int numLights;

private static class SubMesh {

	public int _vertexBuffer;
	public int _texCoordBuffer;
	public int _normalBuffer;
	public int _indexBuffer;

	public int _boneIndicesBuffer;
	public int _boneWeightsBuffer;


	public Vec3d _center;

	public int _numVerts;
	public HashMap<Integer, Integer>[] vaoMaps;
}

private static class Material {
	public int _texHandle;
	public Color4d _diffuseColor;
	public Color4d _specColor  = new Color4d();
	public Color4d _ambientColor = new Color4d();
	public double _shininess;

	public int _transType;
	public Color4d _transColour;

	int shaderID;
}

private static class SubLine {

	public int _vertexBuffer;
	public Color4d _diffuseColor;

	public int _progHandle;

	public int _modelViewMatVar;
	public int _projMatVar;
	public int _colorVar;

	public int _cVar;
	public int _fcVar;

	public ConvexHull _hull;

	public int _numVerts;
	public HashMap<Integer, Integer> vaoMap = new HashMap<Integer, Integer>();
}

private MeshData data;
private ArrayList<SubMesh> _subMeshes;
private ArrayList<SubLine> _subLines;
private ArrayList<Material> _materials;

private int[] usedShaders;

/**
 * The maximum distance a vertex is from the origin
 */
private boolean _isLoadedGPU = false;

private static int zeroBuffer = 0;
private static int zeroBufferSize; // The size of the zero buffer in vertices

private final boolean flattenBuffers;
private final boolean useZeroBuffer;

public MeshProto(MeshData data, boolean flattenBuffers, boolean useZeroBuffer) {
	this.flattenBuffers = flattenBuffers;
	this.useZeroBuffer = useZeroBuffer;
	this.data = data;
	_subMeshes = new ArrayList<SubMesh>();
	_subLines = new ArrayList<SubLine>();
	_materials = new ArrayList<Material>();

	usedShaders = data.getUsedMeshShaders();
}

public void render(int contextID, Renderer renderer,
                   Mat4d modelMat,
                   Mat4d normalMat,
                   Camera cam,
                   ArrayList<Action.Queue> actions,
                   ArrayList<AABB> subInstBounds) {

	assert(_isLoadedGPU);

	Mat4d viewMat = new Mat4d();
	cam.getViewMat4d(viewMat);

	Mat4d finalNorMat = new Mat4d(); // The normal matrix in eye space
	cam.getRotMat4d(finalNorMat);
	finalNorMat.mult4(finalNorMat, normalMat);

	Mat4d modelViewMat = new Mat4d();
	modelViewMat.mult4(viewMat, modelMat);

	initUniforms(renderer, modelViewMat, cam.getProjMat4d(), viewMat, finalNorMat);

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

	Vec3d dist = new Vec3d();

	for (int i = 0; i < data.getSubMeshInstances().size(); ++i) {
		MeshData.SubMeshInstance subInst = data.getSubMeshInstances().get(i);

		SubMesh subMesh = _subMeshes.get(subInst.subMeshIndex);
		Material mat = _materials.get(subInst.materialIndex);
		if (mat._transType != MeshData.NO_TRANS) {
			continue; // Render transparent submeshes after
		}

		AABB instBounds = subInstBounds.get(i);

		if (!cam.collides(instBounds)) {
			continue;
		}

		// Work out distance to the camera
		dist.set3(instBounds.center);
		dist.sub3(cam.getTransformRef().getTransRef());

		double apparentSize = 2 * instBounds.radius.mag3() / dist.mag3();
		if (apparentSize < 0.001) {
			// This object is too small to draw
			continue;
		}

		ArrayList<Mat4d> pose = null;
		if (subInst.armatureIndex != -1 && poses != null) {
			pose = poses.get(subInst.armatureIndex);
		}

		renderSubMesh(subMesh, subInst, contextID, renderer, pose, actions);
	}

	Mat4d subModelViewMat = new Mat4d();
	Mat4d subModelMat = new Mat4d();

	for (MeshData.SubLineInstance subInst : data.getSubLineInstances()) {

		subModelMat.mult4(modelMat, subInst.transform);

		SubLine subLine = _subLines.get(subInst.subLineIndex);

		AABB instBounds = subLine._hull.getAABB(subModelMat);
		if (!cam.collides(instBounds)) {
			continue;
		}

		subModelViewMat.mult4(modelViewMat, subInst.transform);

		renderSubLine(subLine, contextID, renderer, subModelViewMat, cam);
	}

}

public void renderTransparent(int contextID, Renderer renderer,
        Mat4d modelMat,
        Mat4d normalMat,
        Camera cam,
        ArrayList<Action.Queue> actions,
        ArrayList<AABB> subInstBounds) {


	Mat4d viewMat = new Mat4d();
	cam.getViewMat4d(viewMat);

	Mat4d finalNorMat = new Mat4d(); // The normal matrix in eye space
	cam.getRotMat4d(finalNorMat);
	finalNorMat.mult4(finalNorMat, normalMat);

	Mat4d modelViewMat = new Mat4d();
	modelViewMat.mult4(viewMat, modelMat);

	Mat4d subModelView = new Mat4d();

	ArrayList<TransSortable> transparents = new ArrayList<TransSortable>();
	for (int i = 0; i < data.getSubMeshInstances().size(); ++i) {
		MeshData.SubMeshInstance subInst = data.getSubMeshInstances().get(i);

		SubMesh subMesh = _subMeshes.get(subInst.subMeshIndex);
		Material mat = _materials.get(subInst.materialIndex);

		if (mat._transType == MeshData.NO_TRANS) {
			continue; // Opaque sub meshes have been rendered
		}

		AABB instBounds = subInstBounds.get(i);
		if (!cam.collides(instBounds)) {
			continue;
		}

		subModelView.mult4(modelViewMat, subInst.getAnimatedTransform(actions));

		Vec3d eyeCenter = new Vec3d();
		eyeCenter.multAndTrans3(subModelView, subMesh._center);

		TransSortable ts = new TransSortable();
		ts.subMesh = subMesh;
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

	initUniforms(renderer, modelViewMat, cam.getProjMat4d(), viewMat, finalNorMat);

	Collections.sort(transparents);

	for (TransSortable ts : transparents) {
		ArrayList<Mat4d> pose = null;
		if (ts.subInst.armatureIndex != -1 && poses != null) {
			pose = poses.get(ts.subInst.armatureIndex);
		}

		renderSubMesh(ts.subMesh, ts.subInst, contextID, renderer, pose, actions);
	}
}

private void initUniforms(Renderer renderer, Mat4d modelViewMat, Mat4d projMat, Mat4d viewMat, Mat4d normalMat) {
	GL2GL3 gl = renderer.getGL();

	lightsDirScratch[0].mult4(viewMat, lightsDir[0]);
	lightsDirScratch[1].mult4(viewMat, lightsDir[1]);

	lightsDirFloats[0] = (float)lightsDirScratch[0].x;
	lightsDirFloats[1] = (float)lightsDirScratch[0].y;
	lightsDirFloats[2] = (float)lightsDirScratch[0].z;

	lightsDirFloats[3] = (float)lightsDirScratch[1].x;
	lightsDirFloats[4] = (float)lightsDirScratch[1].y;
	lightsDirFloats[5] = (float)lightsDirScratch[1].z;


	for (int i = 0; i < usedShaders.length; ++i) {
		int shaderID = usedShaders[i];
		ShaderInfo si = sInfos[shaderID];

		gl.glUseProgram(si.meshProgHandle);

		gl.glUniformMatrix4fv(si.modelViewMatVar, 1, false, RenderUtils.MarshalMat4d(modelViewMat), 0);
		gl.glUniformMatrix4fv(si.projMatVar, 1, false, RenderUtils.MarshalMat4d(projMat), 0);
		gl.glUniformMatrix4fv(si.normalMatVar, 1, false, RenderUtils.MarshalMat4d(normalMat), 0);

		gl.glUniform3fv(si.lightDirVar, 2, lightsDirFloats, 0);
		gl.glUniform1fv(si.lightIntVar, 2, lightsInt, 0);
		gl.glUniform1i(si.numLightsVar, numLights);

		gl.glUniform1f(si.cVar, Camera.C);
		gl.glUniform1f(si.fcVar, Camera.FC);

	}
}

private void setupVAOForSubMesh(int contextID, SubMesh sub, Renderer renderer) {
	// Setup a VAO for each used shader for this mesh (this could be optimized for only the sub mesh...
	for (int shaderID : usedShaders) {
		setupVAOForSubMeshImp(contextID, shaderID, sub, renderer);
	}
}

private void setupVAOForSubMeshImp(int contextID, int shaderID, SubMesh sub, Renderer renderer) {
	GL2GL3 gl = renderer.getGL();

	int vao = renderer.generateVAO(contextID, gl);
	sub.vaoMaps[shaderID].put(contextID, vao);
	gl.glBindVertexArray(vao);

	int progHandle = sInfos[shaderID].meshProgHandle;
	gl.glUseProgram(progHandle);

	int texCoordVar = gl.glGetAttribLocation(progHandle, "texCoord");

	// For some shaders the texCoordVar may be optimized away
	if (texCoordVar != -1) {
		if (sub._texCoordBuffer != 0) {
			// Texture coordinates
			gl.glEnableVertexAttribArray(texCoordVar);

			gl.glBindBuffer(GL2GL3.GL_ARRAY_BUFFER, sub._texCoordBuffer);
			gl.glVertexAttribPointer(texCoordVar, 2, GL2GL3.GL_FLOAT, false, 0, 0);
		} else {
			gl.glVertexAttrib2f(texCoordVar, 0, 0);
		}
	}

	int boneIndicesVar = gl.glGetAttribLocation(progHandle, "boneIndices");
	int boneWeightsVar = gl.glGetAttribLocation(progHandle, "boneWeights");

	if (sub._boneIndicesBuffer != 0) {
		// Indices
		gl.glEnableVertexAttribArray(boneIndicesVar);

		gl.glBindBuffer(GL2GL3.GL_ARRAY_BUFFER, sub._boneIndicesBuffer);
		gl.glVertexAttribPointer(boneIndicesVar, 4, GL2GL3.GL_FLOAT, false, 0, 0);

		// Weights
		gl.glEnableVertexAttribArray(boneWeightsVar);

		gl.glBindBuffer(GL2GL3.GL_ARRAY_BUFFER, sub._boneWeightsBuffer);
		gl.glVertexAttribPointer(boneWeightsVar, 4, GL2GL3.GL_FLOAT, false, 0, 0);
	} else {
		gl.glVertexAttrib4f(boneIndicesVar, 0, 0, 0, 0);
		gl.glVertexAttrib4f(boneWeightsVar, 0, 0, 0, 0);
	}

	int posVar = gl.glGetAttribLocation(progHandle, "position");
	gl.glEnableVertexAttribArray(posVar);

	gl.glBindBuffer(GL2GL3.GL_ARRAY_BUFFER, sub._vertexBuffer);
	gl.glVertexAttribPointer(posVar, 3, GL2GL3.GL_FLOAT, false, 0, 0);

	// Normals
	int normalVar = gl.glGetAttribLocation(progHandle, "normal");
	gl.glEnableVertexAttribArray(normalVar);

	gl.glBindBuffer(GL2GL3.GL_ARRAY_BUFFER, sub._normalBuffer);
	gl.glVertexAttribPointer(normalVar, 3, GL2GL3.GL_FLOAT, false, 0, 0);

	if (!flattenBuffers) {
		gl.glBindBuffer(GL2GL3.GL_ELEMENT_ARRAY_BUFFER, sub._indexBuffer);
	}

	gl.glBindVertexArray(0);

}

private void renderSubMesh(SubMesh subMesh, MeshData.SubMeshInstance subInst, int contextID,
                           Renderer renderer, ArrayList<Mat4d> pose, ArrayList<Action.Queue> actions) {

	Material mat = _materials.get(subInst.materialIndex);
	int shaderID = mat.shaderID;

	GL2GL3 gl = renderer.getGL();

	if (!subMesh.vaoMaps[shaderID].containsKey(contextID)) {
		setupVAOForSubMesh(contextID, subMesh, renderer);
	}

	int vao = subMesh.vaoMaps[shaderID].get(contextID);
	gl.glBindVertexArray(vao);

	ShaderInfo si = sInfos[shaderID];

	gl.glUseProgram(si.meshProgHandle);

	// Setup uniforms for this object

	gl.glUniformMatrix4fv(si.bindSpaceMatVar, 1, false, RenderUtils.MarshalMat4d(subInst.getAnimatedTransform(actions)), 0);
	gl.glUniformMatrix4fv(si.bindSpaceNorMatVar, 1, false, RenderUtils.MarshalMat4d(subInst.getAnimatedNormalTransform(actions)), 0);

	if (mat._texHandle != 0) {
		gl.glActiveTexture(GL2GL3.GL_TEXTURE0);
		gl.glBindTexture(GL2GL3.GL_TEXTURE_2D, mat._texHandle);
		gl.glTexParameteri(GL2GL3.GL_TEXTURE_2D, GL2GL3.GL_TEXTURE_WRAP_S, GL2GL3.GL_REPEAT);
		gl.glTexParameteri(GL2GL3.GL_TEXTURE_2D, GL2GL3.GL_TEXTURE_WRAP_T, GL2GL3.GL_REPEAT);
		gl.glUniform1i(si.texVar, 0);
	} else {
		gl.glUniform4fv(si.diffuseColorVar, 1, mat._diffuseColor.toFloats(), 0);
	}

	gl.glUniform3fv(si.ambientColorVar, 1, mat._ambientColor.toFloats(), 0);
	gl.glUniform3fv(si.specColorVar, 1, mat._specColor.toFloats(), 0);
	gl.glUniform1f(si.shininessVar, (float)mat._shininess);

	if (mat._transType != MeshData.NO_TRANS) {
		gl.glEnable(GL2GL3.GL_BLEND);
		gl.glBlendEquationSeparate(GL2GL3.GL_FUNC_ADD, GL2GL3.GL_MAX);
		gl.glDepthMask(false);

		if (mat._transType != MeshData.DIFF_ALPHA_TRANS) {
			gl.glBlendColor((float)mat._transColour.r,
			                (float)mat._transColour.g,
			                (float)mat._transColour.b,
			                (float)mat._transColour.a);
		}

		if (mat._transType == MeshData.A_ONE_TRANS) {
			gl.glBlendFuncSeparate(GL2GL3.GL_CONSTANT_ALPHA, GL2GL3.GL_ONE_MINUS_CONSTANT_ALPHA, GL2GL3.GL_ONE, GL2GL3.GL_ZERO);
		} else if (mat._transType == MeshData.RGB_ZERO_TRANS) {
			gl.glBlendFuncSeparate(GL2GL3.GL_ONE_MINUS_CONSTANT_COLOR, GL2GL3.GL_CONSTANT_COLOR, GL2GL3.GL_ONE, GL2GL3.GL_ZERO);
		} else if (mat._transType == MeshData.DIFF_ALPHA_TRANS) {
			gl.glBlendFuncSeparate(GL2GL3.GL_SRC_ALPHA, GL2GL3.GL_ONE_MINUS_SRC_ALPHA, GL2GL3.GL_ONE, GL2GL3.GL_ZERO);
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
			int poseIndex = subInst.boneMapper[i];
			RenderUtils.MarshalMat4dToArray(pose.get(poseIndex), poseMatrices, i*16);
		}
		gl.glUniformMatrix4fv(si.boneMatricesVar, pose.size(), false, poseMatrices, 0);
	}

	gl.glUniform1i(si.maxNumBonesVar, (pose == null) ? 0 : 4);

	// Actually draw it
	//gl.glPolygonMode(GL2GL3.GL_FRONT_AND_BACK, GL2GL3.GL_LINE);
	gl.glDisable(GL2GL3.GL_CULL_FACE);

	if (flattenBuffers) {
		gl.glDrawArrays(GL2GL3.GL_TRIANGLES, 0, subMesh._numVerts);
	} else {
		gl.glDrawElements(GL2GL3.GL_TRIANGLES, subMesh._numVerts, GL2GL3.GL_UNSIGNED_INT, 0);
	}
	gl.glEnable(GL2GL3.GL_CULL_FACE);

	if (mat._transType != MeshData.NO_TRANS) {
		gl.glDisable(GL2GL3.GL_BLEND);
		gl.glDepthMask(true);
	}

	gl.glBindVertexArray(0);

}

private void setupVAOForSubLine(int contextID, SubLine sub, Renderer renderer) {
	GL2GL3 gl = renderer.getGL();

	int vao = renderer.generateVAO(contextID, gl);
	sub.vaoMap.put(contextID, vao);
	gl.glBindVertexArray(vao);

	int prog = sub._progHandle;
	gl.glUseProgram(prog);

	int posVar = gl.glGetAttribLocation(prog, "position");
	gl.glEnableVertexAttribArray(posVar);

	gl.glBindBuffer(GL2GL3.GL_ARRAY_BUFFER, sub._vertexBuffer);
	gl.glVertexAttribPointer(posVar, 3, GL2GL3.GL_FLOAT, false, 0, 0);

	gl.glBindVertexArray(0);

}

private void renderSubLine(SubLine sub, int contextID,
        Renderer renderer, Mat4d modelViewMat, Camera cam) {

	GL2GL3 gl = renderer.getGL();

	if (!sub.vaoMap.containsKey(contextID)) {
		setupVAOForSubLine(contextID, sub, renderer);
	}

	int vao = sub.vaoMap.get(contextID);
	gl.glBindVertexArray(vao);

	int prog = sub._progHandle;
	gl.glUseProgram(prog);

	Mat4d projMat = cam.getProjMat4d();

	gl.glUniformMatrix4fv(sub._modelViewMatVar, 1, false, RenderUtils.MarshalMat4d(modelViewMat), 0);
	gl.glUniformMatrix4fv(sub._projMatVar, 1, false, RenderUtils.MarshalMat4d(projMat), 0);

	gl.glUniform4fv(sub._colorVar, 1, sub._diffuseColor.toFloats(), 0);

	gl.glUniform1f(sub._cVar, Camera.C);
	gl.glUniform1f(sub._fcVar, Camera.FC);

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

	try {
		for (MeshData.SubMeshData subData : data.getSubMeshData()) {
			loadGPUSubMesh(gl, renderer, subData);
		}
		for (MeshData.SubLineData subData : data.getSubLineData()) {
			loadGPUSubLine(gl, renderer, subData);
		}
		for (MeshData.Material mat : data.getMaterials()) {
			loadGPUMaterial(gl, renderer, mat);
		}
	} catch (GLException ex) {
		LogBox.renderLogException(ex);
		return; // The loader will detect that this did not load cleanly
	}

	_isLoadedGPU = true;
}

private void loadGPUMaterial(GL2GL3 gl, Renderer renderer, MeshData.Material dataMat) {

	boolean hasTex = dataMat.colorTex != null;

	Material mat = new Material();
	mat.shaderID = dataMat.getShaderID();
	mat._transType = dataMat.transType;
	mat._transColour = dataMat.transColour;

	if (hasTex) {
		mat._texHandle = renderer.getTexCache().getTexID(gl, dataMat.colorTex, (dataMat.transType != MeshData.NO_TRANS), false, true);
	} else {
		mat._texHandle = 0;
		mat._diffuseColor = new Color4d(dataMat.diffuseColor);
	}

	mat._ambientColor = new Color4d(dataMat.ambientColor);
	mat._specColor = new Color4d(dataMat.specColor);
	mat._shininess = dataMat.shininess;

	_materials.add(mat);
}

public static void init(Renderer r, GL2GL3 gl) {
	for (int i = 0; i < Renderer.NUM_MESH_SHADERS; ++i) {
		ShaderInfo si = new ShaderInfo();
		sInfos[i] = si;

		si.meshProgHandle = r.getMeshShader(i).getProgramHandle();

		gl.glUseProgram(si.meshProgHandle);

		// Bind the shader variables
		si.modelViewMatVar = gl.glGetUniformLocation(si.meshProgHandle, "modelViewMat");
		si.projMatVar = gl.glGetUniformLocation(si.meshProgHandle, "projMat");
		si.bindSpaceMatVar = gl.glGetUniformLocation(si.meshProgHandle, "bindSpaceMat");
		si.bindSpaceNorMatVar = gl.glGetUniformLocation(si.meshProgHandle, "bindSpaceNorMat");
		si.normalMatVar = gl.glGetUniformLocation(si.meshProgHandle, "normalMat");
		si.diffuseColorVar = gl.glGetUniformLocation(si.meshProgHandle, "diffuseColor");
		si.ambientColorVar = gl.glGetUniformLocation(si.meshProgHandle, "ambientColor");
		si.specColorVar = gl.glGetUniformLocation(si.meshProgHandle, "specColor");
		si.shininessVar = gl.glGetUniformLocation(si.meshProgHandle, "shininess");
		si.texVar = gl.glGetUniformLocation(si.meshProgHandle, "diffuseTex");
		si.maxNumBonesVar = gl.glGetUniformLocation(si.meshProgHandle, "maxNumBones");
		si.boneMatricesVar = gl.glGetUniformLocation(si.meshProgHandle, "boneMatrices");

		si.lightDirVar = gl.glGetUniformLocation(si.meshProgHandle, "lightDir");
		si.lightIntVar = gl.glGetUniformLocation(si.meshProgHandle, "lightIntensity");
		si.numLightsVar = gl.glGetUniformLocation(si.meshProgHandle, "numLights");

		si.cVar = gl.glGetUniformLocation(si.meshProgHandle, "C");
		si.fcVar = gl.glGetUniformLocation(si.meshProgHandle, "FC");
	}

	numLights = 2;


	lightsDir[0] = new Vec4d(-0.3, -0.2, -0.5, 0.0);
	lightsDir[1] = new Vec4d( 0.5, 1.0, -0.1, 0.0);

	lightsDir[0].normalize3();
	lightsDir[1].normalize3();

	lightsInt[0] = 1f;
	lightsInt[1] = 0.5f;

	lightsDirScratch[0] = new Vec4d();
	lightsDirScratch[1] = new Vec4d();

}

@SuppressWarnings("unchecked") // Suppresses a warning due to the array of hash maps
private void loadGPUSubMesh(GL2GL3 gl, Renderer renderer, MeshData.SubMeshData data) {

	boolean hasTex = data.texCoords != null;
	boolean hasBoneInfo = data.boneIndices != null;

	SubMesh sub = new SubMesh();
	sub.vaoMaps = new HashMap[Renderer.NUM_MESH_SHADERS]; // This would generate a warning
	for (int i = 0; i < usedShaders.length; ++i) {
		int shaderID = usedShaders[i];
		sub.vaoMaps[shaderID] = new HashMap<Integer, Integer>();
	}

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
	else {
		if (useZeroBuffer) {
			int numEntries = data.verts.size();
			if (flattenBuffers) {
				numEntries = data.indices.length;
			}
			setupZeroBuffer(numEntries, gl);

			sub._boneIndicesBuffer = zeroBuffer;
			sub._boneWeightsBuffer = zeroBuffer;
		}
	}

	sub._center = data.staticHull.getAABBCenter();

	sub._numVerts = data.indices.length;

	if (flattenBuffers) {
		FloatBuffer fb = FloatBuffer.allocate(data.indices.length * 3); //
		for (int ind : data.indices) {
			RenderUtils.putPointXYZ(fb, data.verts.get(ind));
		}
		fb.flip();

		gl.glBindBuffer(GL2GL3.GL_ARRAY_BUFFER, sub._vertexBuffer);
		gl.glBufferData(GL2GL3.GL_ARRAY_BUFFER, data.indices.length * 3 * 4, fb, GL2GL3.GL_STATIC_DRAW);
		renderer.usingVRAM(data.indices.length * 3 * 4);
	} else
	{
		// Init vertices
		FloatBuffer fb = FloatBuffer.allocate(data.verts.size() * 3); //
		for (Vec3d v : data.verts) {
			RenderUtils.putPointXYZ(fb, v);
		}
		fb.flip();

		gl.glBindBuffer(GL2GL3.GL_ARRAY_BUFFER, sub._vertexBuffer);
		gl.glBufferData(GL2GL3.GL_ARRAY_BUFFER, data.verts.size() * 3 * 4, fb, GL2GL3.GL_STATIC_DRAW);
		renderer.usingVRAM(data.verts.size() * 3 * 4);
	}

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
			renderer.usingVRAM(data.indices.length * 2 * 4);

		} else
		{
			FloatBuffer fb = FloatBuffer.allocate(data.texCoords.size() * 2); //
			for (Vec2d v : data.texCoords) {
				RenderUtils.putPointXY(fb, v);
			}
			fb.flip();

			gl.glBindBuffer(GL2GL3.GL_ARRAY_BUFFER, sub._texCoordBuffer);
			gl.glBufferData(GL2GL3.GL_ARRAY_BUFFER, data.texCoords.size() * 2 * 4, fb, GL2GL3.GL_STATIC_DRAW);
			renderer.usingVRAM(data.texCoords.size() * 2 * 4);
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
			renderer.usingVRAM(data.indices.length * 4 * 4);

			fb = FloatBuffer.allocate(data.indices.length * 4); //
			for (int ind : data.indices) {
				RenderUtils.putPointXYZW(fb, data.boneWeights.get(ind));
			}
			fb.flip();

			gl.glBindBuffer(GL2GL3.GL_ARRAY_BUFFER, sub._boneWeightsBuffer);
			gl.glBufferData(GL2GL3.GL_ARRAY_BUFFER, data.indices.length * 4 * 4, fb, GL2GL3.GL_STATIC_DRAW);
			renderer.usingVRAM(data.indices.length * 4 * 4);
		}
		else {
			// Indices
			FloatBuffer fb = FloatBuffer.allocate(data.boneIndices.size() * 4); //
			for (Vec4d v : data.boneIndices) {
				RenderUtils.putPointXYZW(fb, v);
			}
			fb.flip();

			gl.glBindBuffer(GL2GL3.GL_ARRAY_BUFFER, sub._boneIndicesBuffer);
			gl.glBufferData(GL2GL3.GL_ARRAY_BUFFER, data.boneIndices.size() * 4 * 4, fb, GL2GL3.GL_STATIC_DRAW);
			renderer.usingVRAM(data.boneIndices.size() * 4 * 4);

			// Weights
			fb = FloatBuffer.allocate(data.boneWeights.size() * 4); //
			for (Vec4d v : data.boneWeights) {
				RenderUtils.putPointXYZW(fb, v);
			}
			fb.flip();

			gl.glBindBuffer(GL2GL3.GL_ARRAY_BUFFER, sub._boneWeightsBuffer);
			gl.glBufferData(GL2GL3.GL_ARRAY_BUFFER, data.boneWeights.size() * 4 * 4, fb, GL2GL3.GL_STATIC_DRAW);
			renderer.usingVRAM(data.boneWeights.size() * 4 * 4);
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
		renderer.usingVRAM(data.indices.length * 3 * 4);
	} else
	{
		// Init normals
		FloatBuffer fb = FloatBuffer.allocate(data.normals.size() * 3);
		for (Vec3d v : data.normals) {
			RenderUtils.putPointXYZ(fb, v);
		}
		fb.flip();

		gl.glBindBuffer(GL2GL3.GL_ARRAY_BUFFER, sub._normalBuffer);
		gl.glBufferData(GL2GL3.GL_ARRAY_BUFFER, data.normals.size() * 3 * 4, fb, GL2GL3.GL_STATIC_DRAW);
		renderer.usingVRAM(data.normals.size() * 3 * 4);
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
		renderer.usingVRAM(sub._numVerts * 4);

	}

	gl.glBindBuffer(GL2GL3.GL_ARRAY_BUFFER, 0);
	gl.glBindBuffer(GL2GL3.GL_ELEMENT_ARRAY_BUFFER, 0);

	_subMeshes.add(sub);

	// These will never be needed again, so let's just get rid of them
	if (!data.keepRuntimeData) {
		if (data.texCoords != null)
			data.texCoords.clear();
		if (data.normals != null)
			data.normals.clear();
	}
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
	for (Vec3d v : data.verts) {
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

	sub._cVar = gl.glGetUniformLocation(sub._progHandle, "C");
	sub._fcVar = gl.glGetUniformLocation(sub._progHandle, "FC");

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

	for (SubLine sub : _subLines) {
		int[] bufs = new int[1];
		bufs[0] = sub._vertexBuffer;
		gl.glDeleteBuffers(6, bufs, 0);
	}

	_subMeshes.clear();

}

public ConvexHull getHull(ArrayList<Action.Queue> actions, ArrayList<ConvexHull> subInstHulls) {
	return data.getHull(actions, subInstHulls);
}

public boolean hasTransparent() {
	return data.hasTransparent();
}

public ArrayList<ConvexHull> getSubHulls(ArrayList<Action.Queue> actions) {
	return data.getSubHulls(actions);
}

public MeshData getRawData() {
	return data;
}

private void setupZeroBuffer(int numEntries, GL2GL3 gl) {
	if (numEntries <= zeroBufferSize) {
		return; // The buffer is setup and large enough
	}
	if (zeroBuffer == 0) {
		int[] is = new int[1];
		gl.glGenBuffers(1, is, 0);
		zeroBuffer = is[0];
	}

	// allocate a big block of zero floats
	FloatBuffer fb = FloatBuffer.allocate(numEntries * 4);

	for (int i = 0; i < numEntries * 4; ++i) {
		fb.put(0.0f);
	}
	fb.flip();

	gl.glBindBuffer(GL2GL3.GL_ARRAY_BUFFER, zeroBuffer);
	gl.glBufferData(GL2GL3.GL_ARRAY_BUFFER, numEntries * 4 * 4, fb, GL2GL3.GL_STATIC_DRAW);

	gl.glBindBuffer(GL2GL3.GL_ARRAY_BUFFER, 0);

}

} // class MeshProto
