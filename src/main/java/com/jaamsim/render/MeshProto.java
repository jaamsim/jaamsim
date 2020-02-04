/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2012 Ausenco Engineering Canada Inc.
 * Copyright (C) 2015 JaamSim Software Inc.
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

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import com.jaamsim.MeshFiles.MeshData;
import com.jaamsim.math.AABB;
import com.jaamsim.math.Color4d;
import com.jaamsim.math.ConvexHull;
import com.jaamsim.math.Mat4d;
import com.jaamsim.math.Vec2d;
import com.jaamsim.math.Vec3d;
import com.jaamsim.math.Vec4d;
import com.jaamsim.render.Renderer.ShaderHandle;
import com.jaamsim.ui.LogBox;
import com.jogamp.opengl.GL2GL3;
import com.jogamp.opengl.GL4;
import com.jogamp.opengl.GLException;

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

	public int matIndex;
	public Mat4d transform;
	public Mat4d invTrans;

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

	private int lightDirVar;
	private int lightIntVar;
	private int numLightsVar;

	private int cVar;
	private int fcVar;

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

	public Vec3d _center;

	public int _numVerts;
	public int[] usedShaders;
	public final HashMap<Integer, Integer>[] vaoMaps;

	@SuppressWarnings("unchecked")  // initializing the vaoMaps array
	SubMesh(int[] usedShaders) {
		this.usedShaders = usedShaders;
		vaoMaps = new HashMap[Renderer.NUM_MESH_SHADERS];
		for (int i = 0; i < usedShaders.length; ++i) {
			int shaderID = usedShaders[i];
			vaoMaps[shaderID] = new HashMap<>();
		}
	}
}

private static class MeshBatch {
	public int meshIndex;
	public int matIndex;
	public int transBuffer;
	public int normMatBuffer;

	public int numInstances;

	private final int[] usedShaders;

	public final MeshData.StaticMeshBatch data;

	// TODO: bounds

	public final HashMap<Integer, Integer>[] vaoMaps;

	@SuppressWarnings("unchecked")
	MeshBatch(MeshData.StaticMeshBatch data, int[] usedShaders) {
		this.meshIndex = data.key.meshIndex;
		this.matIndex = data.key.matIndex;
		this.usedShaders = usedShaders;
		this.data = data;
		vaoMaps = new HashMap[Renderer.NUM_MESH_SHADERS];
		for (int i = 0; i < usedShaders.length; ++i) {
			int shaderID = usedShaders[i];
			vaoMaps[shaderID] = new HashMap<>();
		}
	}
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
	public HashMap<Integer, Integer> vaoMap = new HashMap<>();
}

private static class LineBatch {
	public int startVert;
	public int numVerts;
	public int baseInst;
	public int numInsts;
}

private final MeshData data;
private final ArrayList<SubMesh> _subMeshes;
private final ArrayList<SubLine> _subLines;
private final ArrayList<Material> _materials;
private final ArrayList<MeshBatch> _batches;
private final ArrayList<LineBatch> _lineBatches;
private final HashMap<Integer, Integer> _lineVAOs;

private int linePosBuffer;
private int lineColorBuffer;
private int lineTransBuffer;


private final int[] usedShaders;

/**
 * The maximum distance a vertex is from the origin
 */
private boolean _isLoadedGPU = false;

private final boolean flattenBuffers;
private final boolean canBatch;

public MeshProto(MeshData data, boolean flattenBuffers, boolean canBatch) {
	this.flattenBuffers = flattenBuffers;
	this.canBatch = canBatch && !flattenBuffers;
	this.data = data;
	_subMeshes = new ArrayList<>();
	_subLines = new ArrayList<>();
	_materials = new ArrayList<>();
	_batches = new ArrayList<>();
	_lineBatches = new ArrayList<>();
	_lineVAOs = new HashMap<>();

	usedShaders = data.getUsedMeshShaders(this.canBatch);
}

public void render(int contextID, Renderer renderer,
                   Mat4d modelMat,
                   Mat4d invModelMat,
                   Camera cam,
                   MeshData.Pose pose) {

	assert(_isLoadedGPU);

	Mat4d viewMat = new Mat4d();
	cam.getViewMat4d(viewMat);

	Mat4d normalMat = new Mat4d(invModelMat);
	normalMat.transpose4();

	Mat4d finalNorMat = new Mat4d(); // The normal matrix in eye space
	cam.getRotMat4d(finalNorMat);
	finalNorMat.mult4(finalNorMat, normalMat);

	Mat4d modelViewMat = new Mat4d();
	modelViewMat.mult4(viewMat, modelMat);

	initUniforms(renderer, modelViewMat, cam.getProjMat4d(), viewMat, finalNorMat);

	if (renderer.isGL4Supported() && canBatch) {
		// Batch render static meshes
		for (MeshBatch batch : _batches) {
			renderBatch(contextID, renderer, batch);
		}
	} else {
		// Render individual instances of static meshes
		for (MeshData.StaticMeshInstance subInst : data.getStaticMeshInstances()) {
			renderOpaqueSubMesh(contextID, renderer, subInst.subMeshIndex, subInst.materialIndex, cam,
			                    modelMat, invModelMat, modelViewMat,
			                    subInst.transform, subInst.invTrans);
		}
	}

	for (MeshData.AnimMeshInstance subInst : data.getAnimMeshInstances()) {
		renderOpaqueSubMesh(contextID, renderer, subInst.meshIndex, subInst.materialIndex, cam,
                modelMat, invModelMat, modelViewMat,
                pose.transforms[subInst.nodeIndex], pose.invTransforms[subInst.nodeIndex]);
	}

	if (renderer.isGL4Supported() && canBatch) {
		renderStaticLines(contextID, renderer, modelViewMat, cam);
	} else {
		for (MeshData.StaticLineInstance subInst : data.getStaticLineInstances()) {

			SubLine subLine = _subLines.get(subInst.lineIndex);
			renderSubLine(subLine, contextID, renderer, modelMat, modelViewMat, subInst.transform, cam);
		}
	}

	for (MeshData.AnimLineInstance subInst : data.getAnimLineInstances()) {

		SubLine subLine = _subLines.get(subInst.lineIndex);
		renderSubLine(subLine, contextID, renderer, modelMat, modelViewMat, pose.transforms[subInst.nodeIndex], cam);
	}

}

private void renderBatch(int contextID, Renderer renderer,
        MeshBatch batch) {

	// TODO bounds
	// TODO size culling


	Material mat = _materials.get(batch.matIndex);
	SubMesh subMesh = _subMeshes.get(batch.meshIndex);

	int shaderID = mat.shaderID | Renderer.STATIC_BATCH_FLAG;

	GL2GL3 gl = renderer.getGL();
	GL4 gl4 = renderer.getGL4();

	if (!batch.vaoMaps[shaderID].containsKey(contextID)) {
		setupVAOForBatch(contextID, batch, renderer);
	}

	int vao = batch.vaoMaps[shaderID].get(contextID);
	gl.glBindVertexArray(vao);

	ShaderInfo si = sInfos[shaderID];

	gl.glUseProgram(si.meshProgHandle);

	// Setup uniforms for this object
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

	// Actually draw it
	gl.glDisable(GL2GL3.GL_CULL_FACE);

	//gl.glDrawElements(GL2GL3.GL_TRIANGLES, subMesh._numVerts, GL2GL3.GL_UNSIGNED_INT, 0);
	gl4.glDrawElementsInstanced(GL2GL3.GL_TRIANGLES, subMesh._numVerts, GL2GL3.GL_UNSIGNED_INT, 0, batch.numInstances);

	gl.glEnable(GL2GL3.GL_CULL_FACE);

	gl.glBindVertexArray(0);

}

private void renderOpaqueSubMesh(int contextID, Renderer renderer,
                                int meshIndex, int matIndex,
                                Camera cam,
                                Mat4d modelMat, Mat4d invModelMat, Mat4d modelViewMat,
                                Mat4d subTrans, Mat4d invSubTrans) {
	Mat4d fullInvMat = new Mat4d();
	fullInvMat.mult4(invSubTrans, invModelMat);

	SubMesh subMesh = _subMeshes.get(meshIndex);
	Material mat = _materials.get(matIndex);
	if (mat._transType != MeshData.NO_TRANS) {
		return; // Render transparent submeshes after
	}

	Mat4d camNormal = new Mat4d();
	camNormal.mult4(modelMat, subTrans);
	camNormal.transpose4();

	MeshData.SubMeshData subData = data.getSubMeshData().get(meshIndex);
	if (!cam.collides(subData.localBounds, fullInvMat, camNormal)) {
		// Not in the view frustum
		return;
	}

	Mat4d fullModelViewMat = new Mat4d();
	fullModelViewMat.mult4(modelViewMat, subTrans);

	Vec3d boundsMax = new Vec3d();
	Vec3d boundsMin = new Vec3d();
	Vec3d diff = new Vec3d();

	boundsMax.multAndTrans3(fullModelViewMat, subData.localBounds.maxPt);
	boundsMin.multAndTrans3(fullModelViewMat, subData.localBounds.minPt);

	diff.sub3(boundsMax, boundsMin);

	double radius = diff.mag3();
	double dist = Math.abs(boundsMax.z + boundsMin.z)/2.0;

	double apparentSize = radius / dist;
	if (apparentSize < 0.001) {
		// This object is too small to draw
		return;
	}

	renderSubMesh(subMesh, matIndex, subTrans, invSubTrans, contextID, renderer);

}

public void renderTransparent(int contextID, Renderer renderer,
        Mat4d modelMat,
        Mat4d invModelMat,
        Camera cam,
        MeshData.Pose pose) {


	Mat4d viewMat = new Mat4d();
	cam.getViewMat4d(viewMat);

	Mat4d normalMat = new Mat4d(invModelMat);
	normalMat.transpose4();

	Mat4d finalNorMat = new Mat4d(); // The normal matrix in eye space
	cam.getRotMat4d(finalNorMat);
	finalNorMat.mult4(finalNorMat, normalMat);

	Mat4d modelViewMat = new Mat4d();
	modelViewMat.mult4(viewMat, modelMat);

	Mat4d subModelView = new Mat4d();

	ArrayList<TransSortable> transparents = new ArrayList<>();
	for (MeshData.StaticMeshInstance subInst : data.getStaticMeshInstances()) {

		SubMesh subMesh = _subMeshes.get(subInst.subMeshIndex);
		Material mat = _materials.get(subInst.materialIndex);

		if (mat._transType == MeshData.NO_TRANS) {
			continue; // Opaque sub meshes have been rendered
		}

		Mat4d fullInvMat = new Mat4d();
		fullInvMat.mult4(subInst.invTrans, invModelMat);
		Mat4d camNormal = new Mat4d();
		camNormal.mult4(modelMat, subInst.transform);
		camNormal.transpose4();


		MeshData.SubMeshData subData = data.getSubMeshData().get(subInst.subMeshIndex);
		if (!cam.collides(subData.localBounds, fullInvMat, camNormal)) {
			continue;
		}

		subModelView.mult4(modelViewMat, subInst.transform);

		Vec3d eyeCenter = new Vec3d();
		eyeCenter.multAndTrans3(subModelView, subMesh._center);

		TransSortable ts = new TransSortable();
		ts.subMesh = subMesh;
		ts.dist = eyeCenter.z;
		ts.matIndex = subInst.materialIndex;
		ts.transform = subInst.transform;
		ts.invTrans = subInst.invTrans;
		transparents.add(ts);
	}

	for (MeshData.AnimMeshInstance subInst : data.getAnimMeshInstances()) {

		SubMesh subMesh = _subMeshes.get(subInst.meshIndex);
		Material mat = _materials.get(subInst.materialIndex);

		if (mat._transType == MeshData.NO_TRANS) {
			continue; // Opaque sub meshes have been rendered
		}

		Mat4d subTrans = pose.transforms[subInst.nodeIndex];
		Mat4d invSubTrans = pose.invTransforms[subInst.nodeIndex];

		Mat4d fullInvMat = new Mat4d();
		fullInvMat.mult4(invSubTrans, invModelMat);
		Mat4d camNormal = new Mat4d();
		camNormal.mult4(modelMat, subTrans);
		camNormal.transpose4();

		MeshData.SubMeshData subData = data.getSubMeshData().get(subInst.meshIndex);
		if (!cam.collides(subData.localBounds, fullInvMat, camNormal)) {
			continue;
		}

		subModelView.mult4(modelViewMat, subTrans);

		Vec3d eyeCenter = new Vec3d();
		eyeCenter.multAndTrans3(subModelView, subMesh._center);

		TransSortable ts = new TransSortable();
		ts.subMesh = subMesh;
		ts.dist = eyeCenter.z;
		ts.matIndex = subInst.materialIndex;
		ts.transform = subTrans;
		ts.invTrans = invSubTrans;
		transparents.add(ts);
	}

	initUniforms(renderer, modelViewMat, cam.getProjMat4d(), viewMat, finalNorMat);

	Collections.sort(transparents);

	for (TransSortable ts : transparents) {

		Mat4d subInstNorm = new Mat4d(ts.invTrans);
		subInstNorm.transpose4();
		renderSubMesh(ts.subMesh, ts.matIndex, ts.transform,
		              subInstNorm, contextID, renderer);
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
	for (int shaderID : sub.usedShaders) {
		if ((shaderID & Renderer.STATIC_BATCH_FLAG) == 0)
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

private void setupVAOForBatch(int contextID, MeshBatch batch, Renderer renderer) {
	for (int shaderID : batch.usedShaders) {
		if ((shaderID & Renderer.STATIC_BATCH_FLAG) != 0)
			setupVAOForBatchImp(contextID, shaderID, batch, renderer);
	}
}

private void setupVAOForBatchImp(int contextID, int shaderID, MeshBatch batch, Renderer renderer) {
	GL4 gl = renderer.getGL4();

	SubMesh sub = _subMeshes.get(batch.meshIndex);

	int vao = renderer.generateVAO(contextID, gl);
	batch.vaoMaps[shaderID].put(contextID, vao);
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

	int bindSpaceMatVar = gl.glGetAttribLocation(progHandle, "bindSpaceMat");
	gl.glBindBuffer(GL2GL3.GL_ARRAY_BUFFER, batch.transBuffer);

	for (int i = 0; i < 4; ++i) {
		// Enable 4 variables because this is a matrix
		int varInd = bindSpaceMatVar + i;

		gl.glEnableVertexAttribArray(varInd);

		gl.glVertexAttribPointer(varInd, 4, GL2GL3.GL_FLOAT, false, 16*4, i*4*4);
		gl.glVertexAttribDivisor(varInd, 1); // Per instance

	}

	int bindSpaceNormMatVar = gl.glGetAttribLocation(progHandle, "bindSpaceNorMat");
	gl.glBindBuffer(GL2GL3.GL_ARRAY_BUFFER, batch.normMatBuffer);

	for (int i = 0; i < 4; ++i) {
		// Enable 4 variables because this is a matrix
		int varInd = bindSpaceNormMatVar + i;

		gl.glEnableVertexAttribArray(varInd);

		gl.glVertexAttribPointer(varInd, 4, GL2GL3.GL_FLOAT, false, 16*4, i*4*4);
		gl.glVertexAttribDivisor(varInd, 1); // Per instance

	}

	gl.glBindVertexArray(0);

}


private void renderSubMesh(SubMesh subMesh, int materialIndex,
                           Mat4d subInstTrans,
                           Mat4d subInstInvTrans,
                           int contextID,
                           Renderer renderer) {

	Material mat = _materials.get(materialIndex);
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
	Mat4d subInstNorm = new Mat4d(subInstInvTrans);
	subInstNorm.transpose4();

	gl.glUniformMatrix4fv(si.bindSpaceMatVar, 1, false, RenderUtils.MarshalMat4d(subInstTrans), 0);
	gl.glUniformMatrix4fv(si.bindSpaceNorMatVar, 1, false, RenderUtils.MarshalMat4d(subInstNorm), 0);

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
		gl.glBlendEquationSeparate(GL2GL3.GL_FUNC_ADD, GL2GL3.GL_MAX);

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
	}

	// Actually draw it
	//gl.glPolygonMode(GL2GL3.GL_FRONT_AND_BACK, GL2GL3.GL_LINE);
	gl.glDisable(GL2GL3.GL_CULL_FACE);

	if (flattenBuffers) {
		gl.glDrawArrays(GL2GL3.GL_TRIANGLES, 0, subMesh._numVerts);
	} else {
		gl.glDrawElements(GL2GL3.GL_TRIANGLES, subMesh._numVerts, GL2GL3.GL_UNSIGNED_INT, 0);
	}
	gl.glEnable(GL2GL3.GL_CULL_FACE);

	// Reset the blend state
	if (mat._transType != MeshData.NO_TRANS) {
		gl.glBlendEquationSeparate(GL2GL3.GL_FUNC_ADD, GL2GL3.GL_MAX);
		gl.glBlendFuncSeparate(GL2GL3.GL_SRC_ALPHA, GL2GL3.GL_ONE_MINUS_SRC_ALPHA, GL2GL3.GL_ONE, GL2GL3.GL_ONE);
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

private void renderSubLine(SubLine sub, int contextID, Renderer renderer,
                           Mat4d modelMat, Mat4d modelViewMat, Mat4d subInstTrans, Camera cam) {

	Mat4d subModelViewMat = new Mat4d();
	Mat4d subModelMat = new Mat4d();

	subModelMat.mult4(modelMat, subInstTrans);

	AABB instBounds = sub._hull.getAABB(subModelMat);
	if (!cam.collides(instBounds)) {
		return;
	}

	subModelViewMat.mult4(modelViewMat, subInstTrans);


	GL2GL3 gl = renderer.getGL();

	if (!sub.vaoMap.containsKey(contextID)) {
		setupVAOForSubLine(contextID, sub, renderer);
	}

	int vao = sub.vaoMap.get(contextID);
	gl.glBindVertexArray(vao);

	int prog = sub._progHandle;
	gl.glUseProgram(prog);

	Mat4d projMat = cam.getProjMat4d();

	gl.glUniformMatrix4fv(sub._modelViewMatVar, 1, false, RenderUtils.MarshalMat4d(subModelViewMat), 0);
	gl.glUniformMatrix4fv(sub._projMatVar, 1, false, RenderUtils.MarshalMat4d(projMat), 0);

	gl.glUniform4fv(sub._colorVar, 1, sub._diffuseColor.toFloats(), 0);

	gl.glUniform1f(sub._cVar, Camera.C);
	gl.glUniform1f(sub._fcVar, Camera.FC);

	gl.glLineWidth(1);

	// Actually draw it
	gl.glDrawArrays(GL2GL3.GL_LINES, 0, sub._numVerts);

	gl.glBindVertexArray(0);

}

private void setupVAOForStaticLines(int contextID, Renderer renderer) {
	GL2GL3 gl = renderer.getGL();

	int vao = renderer.generateVAO(contextID, gl);
	_lineVAOs.put(contextID, vao);
	gl.glBindVertexArray(vao);

	Shader s= renderer.getShader(ShaderHandle.DEBUG_BATCH);
	assert(s.isGood());
	int prog = s.getProgramHandle();
	gl.glUseProgram(prog);

	int posVar = gl.glGetAttribLocation(prog, "position");
	gl.glEnableVertexAttribArray(posVar);

	gl.glBindBuffer(GL2GL3.GL_ARRAY_BUFFER, linePosBuffer);
	gl.glVertexAttribPointer(posVar, 3, GL2GL3.GL_FLOAT, false, 0, 0);

	int colVar = gl.glGetAttribLocation(prog, "vertColor");
	gl.glEnableVertexAttribArray(colVar);

	gl.glBindBuffer(GL2GL3.GL_ARRAY_BUFFER, lineColorBuffer);
	gl.glVertexAttribPointer(colVar, 4, GL2GL3. GL_UNSIGNED_BYTE, true, 0, 0);

	int instMatVar = gl.glGetAttribLocation(prog, "instMat");
	gl.glBindBuffer(GL2GL3.GL_ARRAY_BUFFER, lineTransBuffer);

	for (int i = 0; i < 4; ++i) {
		// Enable 4 variables because this is a matrix
		int varInd = instMatVar + i;

		gl.glEnableVertexAttribArray(varInd);

		gl.glVertexAttribPointer(varInd, 4, GL2GL3.GL_FLOAT, false, 16*4, i*4*4);
		gl.glVertexAttribDivisor(varInd, 1); // Per instance
	}

	gl.glBindVertexArray(0);

}

private void renderStaticLines(int contextID, Renderer renderer, Mat4d modelViewMat, Camera cam) {

	GL2GL3 gl = renderer.getGL();
	GL4 gl4 = renderer.getGL4();

	if (!_lineVAOs.containsKey(contextID)) {
		setupVAOForStaticLines(contextID, renderer);
	}

	int vao = _lineVAOs.get(contextID);
	gl.glBindVertexArray(vao);

	Shader s = renderer.getShader(ShaderHandle.DEBUG_BATCH);
	int progHandle = s.getProgramHandle();
	gl.glUseProgram(progHandle);

	Mat4d projMat = cam.getProjMat4d();

	int modelViewMatVar = gl.glGetUniformLocation(progHandle, "modelViewMat");
	int projMatVar = gl.glGetUniformLocation(progHandle, "projMat");

	int cVar = gl.glGetUniformLocation(progHandle, "C");
	int fcVar = gl.glGetUniformLocation(progHandle, "FC");

	gl.glUniformMatrix4fv(modelViewMatVar, 1, false, RenderUtils.MarshalMat4d(modelViewMat), 0);
	gl.glUniformMatrix4fv(projMatVar, 1, false, RenderUtils.MarshalMat4d(projMat), 0);

	gl.glUniform1f(cVar, Camera.C);
	gl.glUniform1f(fcVar, Camera.FC);

	gl.glLineWidth(1);

	// Actually draw it
	for (LineBatch batch : _lineBatches) {
		gl4.glDrawArraysInstancedBaseInstance(GL2GL3.GL_LINES, batch.startVert, batch.numVerts, batch.numInsts, batch.baseInst);
	}
	//gl.glDrawArrays(GL2GL3.GL_LINES, 0, sub._numVerts);

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
		if (canBatch) {
			for (MeshData.StaticMeshBatch batch : data.getStaticMeshBatches().values()) {
				loadGPUBatch(gl, renderer, batch);
			}
			loadGPULineBatches(gl, renderer);
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

private void loadGPUBatch(GL2GL3 gl, Renderer renderer, MeshData.StaticMeshBatch batch) {

	MeshBatch b = new MeshBatch(batch, usedShaders);

	b.numInstances = batch.transform.size();

	int[] is = new int[2];
	gl.glGenBuffers(2, is, 0);
	b.transBuffer = is[0];
	b.normMatBuffer = is[1];

	// Populate buffers

	assert(b.data.transform.size() == b.data.invTrans.size());

	// Init transform list
	FloatBuffer fb = FloatBuffer.allocate(batch.transform.size() * 16); //
	for (Mat4d m : batch.transform) {
		RenderUtils.putMat4dCM(fb, m);
	}
	fb.flip();

	gl.glBindBuffer(GL2GL3.GL_ARRAY_BUFFER, b.transBuffer);
	gl.glBufferData(GL2GL3.GL_ARRAY_BUFFER, batch.transform.size() * 16 * 4, fb, GL2GL3.GL_STATIC_DRAW);
	renderer.usingVRAM(batch.transform.size() * 16 * 4);

	// Init inverse transform list
	fb = FloatBuffer.allocate(batch.invTrans.size() * 16); //
	for (Mat4d m : batch.invTrans) {
		// Convert the inverse transform into a normal matrix
		Mat4d normMat = new Mat4d(m);
		normMat.transpose4();
		RenderUtils.putMat4dCM(fb, normMat);
	}
	fb.flip();

	gl.glBindBuffer(GL2GL3.GL_ARRAY_BUFFER, b.normMatBuffer);
	gl.glBufferData(GL2GL3.GL_ARRAY_BUFFER, batch.invTrans.size() * 16 * 4, fb, GL2GL3.GL_STATIC_DRAW);
	renderer.usingVRAM(batch.invTrans.size() * 16 * 4);

	_batches.add(b);
}

public void loadGPULineBatches(GL2GL3 gl, Renderer renderer) {
	ArrayList<Vec3d> linePos = data.getLinePosArray();
	ArrayList<Color4d> lineCols = data.getLineColorArray();

	ArrayList<MeshData.StaticLineBatch> lineBatches = data.getLineBatches();
	ArrayList<MeshData.SubLineData> subLines = data.getSubLineData();

	int[] is = new int[2];
	gl.glGenBuffers(2, is, 0);
	linePosBuffer = is[0];
	lineColorBuffer = is[1];

	// Populate buffers

	assert(linePos.size() == lineCols.size());

	// Init pos buffer
	FloatBuffer fb = FloatBuffer.allocate(linePos.size() * 3); //
	for (Vec3d pos : linePos) {
		RenderUtils.putPointXYZ(fb, pos);
	}
	fb.flip();

	gl.glBindBuffer(GL2GL3.GL_ARRAY_BUFFER, linePosBuffer);
	gl.glBufferData(GL2GL3.GL_ARRAY_BUFFER, linePos.size()*3*4, fb, GL2GL3.GL_STATIC_DRAW);
	renderer.usingVRAM(linePos.size()*3*4);

	// Init color buffer
	ByteBuffer bb = ByteBuffer.allocate(lineCols.size() * 4); //
	for (Color4d col : lineCols) {
		RenderUtils.putColor4b(bb, col);
	}
	bb.flip();

	gl.glBindBuffer(GL2GL3.GL_ARRAY_BUFFER, lineColorBuffer);
	gl.glBufferData(GL2GL3.GL_ARRAY_BUFFER, lineCols.size()*4, bb, GL2GL3.GL_STATIC_DRAW);
	renderer.usingVRAM(lineCols.size()*4);

	// Populate per-instance transform buffer
	int numInsts = 0;
	assert(subLines.size() == lineBatches.size());
	for (int i = 0; i < subLines.size(); ++i) {
		MeshData.SubLineData subLine = subLines.get(i);
		MeshData.StaticLineBatch batchData = lineBatches.get(i);

		LineBatch batch = new LineBatch();
		batch.startVert = subLine.startVert;
		batch.numVerts = subLine.verts.size();
		batch.baseInst = numInsts;//TODO
		batch.numInsts = batchData.instTrans.size();

		numInsts += batch.numInsts;
		_lineBatches.add(batch);
	}

	int[] buffs = new int[1];
	gl.glGenBuffers(1, buffs, 0);
	lineTransBuffer = buffs[0];

	fb = FloatBuffer.allocate(numInsts * 16); //
	for (MeshData.StaticLineBatch batchData: lineBatches) {
		for (Mat4d m : batchData.instTrans) {
			RenderUtils.putMat4dCM(fb, m);
		}
	}

	fb.flip();

	gl.glBindBuffer(GL2GL3.GL_ARRAY_BUFFER, lineTransBuffer);
	gl.glBufferData(GL2GL3.GL_ARRAY_BUFFER, numInsts * 16 * 4, fb, GL2GL3.GL_STATIC_DRAW);
	renderer.usingVRAM(numInsts * 16 * 4);

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
		si.normalMatVar = gl.glGetUniformLocation(si.meshProgHandle, "normalMat");
		si.diffuseColorVar = gl.glGetUniformLocation(si.meshProgHandle, "diffuseColor");
		si.ambientColorVar = gl.glGetUniformLocation(si.meshProgHandle, "ambientColor");
		si.specColorVar = gl.glGetUniformLocation(si.meshProgHandle, "specColor");
		si.shininessVar = gl.glGetUniformLocation(si.meshProgHandle, "shininess");
		si.texVar = gl.glGetUniformLocation(si.meshProgHandle, "diffuseTex");

		si.lightDirVar = gl.glGetUniformLocation(si.meshProgHandle, "lightDir");
		si.lightIntVar = gl.glGetUniformLocation(si.meshProgHandle, "lightIntensity");
		si.numLightsVar = gl.glGetUniformLocation(si.meshProgHandle, "numLights");

		si.cVar = gl.glGetUniformLocation(si.meshProgHandle, "C");
		si.fcVar = gl.glGetUniformLocation(si.meshProgHandle, "FC");

		if ((i & Renderer.STATIC_BATCH_FLAG) == 0) {
			// We do not use the uniform bind space variables when batch rendering
			si.bindSpaceMatVar = gl.glGetUniformLocation(si.meshProgHandle, "bindSpaceMat");
			si.bindSpaceNorMatVar = gl.glGetUniformLocation(si.meshProgHandle, "bindSpaceNorMat");
		}

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

private void loadGPUSubMesh(GL2GL3 gl, Renderer renderer, MeshData.SubMeshData data) {

	boolean hasTex = data.texCoords != null;

	SubMesh sub = new SubMesh(usedShaders);

	int[] is = new int[3];
	gl.glGenBuffers(3, is, 0);
	sub._vertexBuffer = is[0];
	sub._normalBuffer = is[1];
	sub._indexBuffer = is[2];

	if (hasTex) {
		gl.glGenBuffers(1, is, 0);
		sub._texCoordBuffer = is[0];
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
		bufs[3] = sub._indexBuffer;

		gl.glDeleteBuffers(4, bufs, 0);

	}

	for (SubLine sub : _subLines) {
		int[] bufs = new int[1];
		bufs[0] = sub._vertexBuffer;
		gl.glDeleteBuffers(1, bufs, 0);
	}

	for (MeshBatch b : _batches) {
		int[] bufs = new int[2];
		bufs[0] = b.transBuffer;
		bufs[1] = b.normMatBuffer;
		gl.glDeleteBuffers(2, bufs, 0);
	}

	_subMeshes.clear();

}

public MeshData.Pose getPose(ArrayList<Action.Queue> actions) {
	return data.getPose(actions);
}

public ConvexHull getHull(MeshData.Pose pose) {
	return data.getHull(pose);
}

public boolean hasTransparent() {
	return data.hasTransparent();
}

public MeshData getRawData() {
	return data;
}

} // class MeshProto
