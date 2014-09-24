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

import java.net.URI;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.HashMap;

import javax.media.opengl.GL2GL3;

import com.jaamsim.math.AABB;
import com.jaamsim.math.Mat4d;
import com.jaamsim.math.Ray;
import com.jaamsim.math.Transform;
import com.jaamsim.math.Vec3d;
import com.jaamsim.math.Vec4d;


/**
 * TextureView is a simple renderable that allows a rectangular image to be easily displayed in 3-space
 * @author Matt.Chudleigh
 *
 */
public class TextureView implements Renderable {

	private URI _imageURI;
	private Transform _trans;
	private Vec3d _scale;
	private long _pickingID;

	private AABB _bounds;

	private boolean _isTransparent;
	private boolean _isCompressed;

	private VisibilityInfo _visInfo;

	// Initialize the very simple buffers needed to render this image
	static private boolean staticInit = false;
	static private int vertBuff;
	static private int texCoordBuff;
	static private int normalBuff;
	static private HashMap<Integer, Integer> VAOMap = new HashMap<Integer, Integer>();

	static private int boneBuff;

	static private int progHandle;
	static private int projMatVar;
	static private int modelViewMatVar;
	static private int normalMatVar;
	static private int bindSpaceMatVar;
	static private int bindSpaceNorMatVar;
	static private int texVar;

	static private int lightDirVar;
	static private int lightIntVar;
	static private int numLightsVar;

	static private int specColorVar;
	static private int ambientColorVar;
	static private int shininessVar;

	static private int maxNumBonesVar;

	static private int cVar;
	static private int fcVar;

	static private float[] lightDir = new float[3];
	static private float[] lightInt = new float[1];

	// a column-major identity matrix
	static private float[] identMat = new float[16];

	static {
		identMat[ 0] = 1.0f; identMat[ 4] = 0.0f; identMat[ 8] = 0.0f; identMat[12] = 0.0f;
		identMat[ 1] = 0.0f; identMat[ 5] = 1.0f; identMat[ 9] = 0.0f; identMat[13] = 0.0f;
		identMat[ 2] = 0.0f; identMat[ 6] = 0.0f; identMat[10] = 1.0f; identMat[14] = 0.0f;
		identMat[ 3] = 0.0f; identMat[ 7] = 0.0f; identMat[11] = 0.0f; identMat[15] = 1.0f;
	}

	public TextureView(URI imageURI, Transform trans, Vec3d scale, boolean isTransparent, boolean isCompressed,
	                   VisibilityInfo visInfo, long pickingID) {
		_imageURI = imageURI;
		_trans = trans;
		_scale = scale;
		_scale.z = 1; // This object can only be scaled in X, Y
		_pickingID = pickingID;
		_isTransparent = isTransparent;
		_isCompressed = isCompressed;
		_visInfo = visInfo;


		Mat4d modelMat = RenderUtils.mergeTransAndScale(_trans, _scale);

		ArrayList<Vec4d> vs = new ArrayList<Vec4d>(4);
		vs.add(new Vec4d( 0.5,  0.5, 0, 1.0d));
		vs.add(new Vec4d(-0.5,  0.5, 0, 1.0d));
		vs.add(new Vec4d(-0.5, -0.5, 0, 1.0d));
		vs.add(new Vec4d( 0.5, -0.5, 0, 1.0d));

		_bounds = new AABB(vs, modelMat);

	}

	private static void initStaticBuffers(Renderer r) {
		GL2GL3 gl = r.getGL();

		int[] buffs = new int[4];
		gl.glGenBuffers(4, buffs, 0);
		vertBuff = buffs[0];
		texCoordBuff = buffs[1];
		normalBuff = buffs[2];
		boneBuff = buffs[3];

		FloatBuffer verts = FloatBuffer.allocate(6*3); // 2 triangles * 3 coordinates
		verts.put(-0.5f); verts.put(-0.5f); verts.put(0.0f);
		verts.put( 0.5f); verts.put(-0.5f); verts.put(0.0f);
		verts.put( 0.5f); verts.put( 0.5f); verts.put(0.0f);

		verts.put(-0.5f); verts.put(-0.5f); verts.put(0.0f);
		verts.put( 0.5f); verts.put( 0.5f); verts.put(0.0f);
		verts.put(-0.5f); verts.put( 0.5f); verts.put(0.0f);

		verts.flip();
		gl.glBindBuffer(GL2GL3.GL_ARRAY_BUFFER, vertBuff);
		gl.glBufferData(GL2GL3.GL_ARRAY_BUFFER, 6*3*4, verts, GL2GL3.GL_STATIC_DRAW);

		FloatBuffer texCoords = FloatBuffer.allocate(6*2); // 2 triangles * 2 coordinates

		texCoords.put(0.0f); texCoords.put(0.0f);
		texCoords.put(1.0f); texCoords.put(0.0f);
		texCoords.put(1.0f); texCoords.put(1.0f);

		texCoords.put(0.0f); texCoords.put(0.0f);
		texCoords.put(1.0f); texCoords.put(1.0f);
		texCoords.put(0.0f); texCoords.put(1.0f);

		texCoords.flip();
		gl.glBindBuffer(GL2GL3.GL_ARRAY_BUFFER, texCoordBuff);
		gl.glBufferData(GL2GL3.GL_ARRAY_BUFFER, 6*2*4, texCoords, GL2GL3.GL_STATIC_DRAW);

		FloatBuffer normals = FloatBuffer.allocate(6*3); // 2 triangles * 3 coordinates
		for (int i = 0; i < 6; ++i) {
			normals.put(0.0f); normals.put(0.0f); normals.put(1.0f);
		}

		normals.flip();
		gl.glBindBuffer(GL2GL3.GL_ARRAY_BUFFER, normalBuff);
		gl.glBufferData(GL2GL3.GL_ARRAY_BUFFER, 6*3*4, normals, GL2GL3.GL_STATIC_DRAW);

		FloatBuffer bones = FloatBuffer.allocate(6*4);
		for (int i = 0; i < 6*4; ++i) {
			bones.put(0.0f);
		}

		bones.flip();
		gl.glBindBuffer(GL2GL3.GL_ARRAY_BUFFER, boneBuff);
		gl.glBufferData(GL2GL3.GL_ARRAY_BUFFER, 6*4*4, bones, GL2GL3.GL_STATIC_DRAW);

		gl.glBindBuffer(GL2GL3.GL_ARRAY_BUFFER, 0);

		// Initialize the shader variables
		progHandle = r.getMeshShader(Renderer.DIFF_TEX_FLAG).getProgramHandle();

		modelViewMatVar = gl.glGetUniformLocation(progHandle, "modelViewMat");
		projMatVar = gl.glGetUniformLocation(progHandle, "projMat");
		normalMatVar = gl.glGetUniformLocation(progHandle, "normalMat");
		bindSpaceMatVar = gl.glGetUniformLocation(progHandle, "bindSpaceMat");
		bindSpaceNorMatVar = gl.glGetUniformLocation(progHandle, "bindSpaceNorMat");
		texVar = gl.glGetUniformLocation(progHandle, "diffuseTex");

		lightDirVar = gl.glGetUniformLocation(progHandle, "lightDir");
		lightIntVar = gl.glGetUniformLocation(progHandle, "lightIntensity");
		numLightsVar = gl.glGetUniformLocation(progHandle, "numLights");

		specColorVar = gl.glGetUniformLocation(progHandle, "specColor");
		ambientColorVar = gl.glGetUniformLocation(progHandle, "ambientColor");
		shininessVar = gl.glGetUniformLocation(progHandle, "shininess");

		maxNumBonesVar = gl.glGetUniformLocation(progHandle, "maxNumBones");

		cVar = gl.glGetUniformLocation(progHandle, "C");
		fcVar = gl.glGetUniformLocation(progHandle, "FC");

		lightDir[0] = 0;
		lightDir[1] = 0;
		lightDir[2] = -1;

		lightInt[0] = 1;

		staticInit = true;
	}

	private void setupVAO(int contextID, Renderer renderer) {
		GL2GL3 gl = renderer.getGL();

		int vao = renderer.generateVAO(contextID, gl);
		VAOMap.put(contextID, vao);

		gl.glBindVertexArray(vao);


		// Position
		int posVar = gl.glGetAttribLocation(progHandle, "position");
		gl.glEnableVertexAttribArray(posVar);

		gl.glBindBuffer(GL2GL3.GL_ARRAY_BUFFER, vertBuff);
		gl.glVertexAttribPointer(posVar, 3, GL2GL3.GL_FLOAT, false, 0, 0);

		// Normals
		int normalVar = gl.glGetAttribLocation(progHandle, "normal");
		gl.glEnableVertexAttribArray(normalVar);

		gl.glBindBuffer(GL2GL3.GL_ARRAY_BUFFER, normalBuff);
		gl.glVertexAttribPointer(normalVar, 3, GL2GL3.GL_FLOAT, false, 0, 0);

		// TexCoords
		int texCoordVar = gl.glGetAttribLocation(progHandle, "texCoord");
		gl.glEnableVertexAttribArray(texCoordVar);

		gl.glBindBuffer(GL2GL3.GL_ARRAY_BUFFER, texCoordBuff);
		gl.glVertexAttribPointer(texCoordVar, 2, GL2GL3.GL_FLOAT, false, 0, 0);

		gl.glBindBuffer(GL2GL3.GL_ARRAY_BUFFER, boneBuff);
		int boneIndicesVar = gl.glGetAttribLocation(progHandle, "boneIndices");
		gl.glEnableVertexAttribArray(boneIndicesVar);
		gl.glVertexAttribPointer(boneIndicesVar, 4, GL2GL3.GL_FLOAT, false, 0, 0);

		int boneWeightsVar = gl.glGetAttribLocation(progHandle, "boneWeights");
		gl.glEnableVertexAttribArray(boneWeightsVar);
		gl.glVertexAttribPointer(boneWeightsVar, 4, GL2GL3.GL_FLOAT, false, 0, 0);

		gl.glBindBuffer(GL2GL3.GL_ARRAY_BUFFER, 0);

		gl.glBindVertexArray(0);

	}


	@Override
	public void render(int contextID, Renderer renderer, Camera cam, Ray pickRay) {
		if (_isTransparent) {
			// Return, this will be handled in the transparent render phase
			return;
		}

		renderImp(contextID, renderer, cam, pickRay);
	}

	private void renderImp(int contextID, Renderer renderer, Camera cam, Ray pickRay) {

		if (!staticInit) {
			initStaticBuffers(renderer);
		}

		GL2GL3 gl = renderer.getGL();

		int textureID = renderer.getTexCache().getTexID(gl, _imageURI, _isTransparent, _isCompressed, false);

		if (textureID == TexCache.LOADING_TEX_ID) {
			return; // This texture is not ready yet
		}

		if (!VAOMap.containsKey(contextID)) {
			setupVAO(contextID, renderer);
		}

		int vao = VAOMap.get(contextID);
		gl.glBindVertexArray(vao);


		Mat4d modelViewMat = new Mat4d();

		cam.getViewMat4d(modelViewMat);
		modelViewMat.mult4(_trans.getMat4dRef());
		modelViewMat.scaleCols3(_scale);

		Mat4d normalMat = RenderUtils.getInverseWithScale(_trans, _scale);
		normalMat.transpose4();

		gl.glUseProgram(progHandle);

		gl.glUniformMatrix4fv(modelViewMatVar, 1, false, RenderUtils.MarshalMat4d(modelViewMat), 0);
		gl.glUniformMatrix4fv(projMatVar, 1, false, RenderUtils.MarshalMat4d(cam.getProjMat4d()), 0);
		gl.glUniformMatrix4fv(normalMatVar, 1, false, RenderUtils.MarshalMat4d(normalMat), 0);
		gl.glUniformMatrix4fv(bindSpaceMatVar, 1, false, identMat, 0);
		gl.glUniformMatrix4fv(bindSpaceNorMatVar, 1, false, identMat, 0);

		gl.glUniform1i(maxNumBonesVar, 0);

		gl.glUniform1f(cVar, Camera.C);
		gl.glUniform1f(fcVar, Camera.FC);

		gl.glUniform1i(numLightsVar, 1);
		gl.glUniform3fv(lightDirVar, 1, lightDir, 0);
		gl.glUniform1fv(lightIntVar, 1, lightInt, 0);

		gl.glUniform3f(ambientColorVar, 0.0f, 0.0f, 0.0f);
		gl.glUniform3f(specColorVar, 0.0f, 0.0f, 0.0f);
		gl.glUniform1f(shininessVar, 1.0f);

		gl.glActiveTexture(GL2GL3.GL_TEXTURE0);
		gl.glBindTexture(GL2GL3.GL_TEXTURE_2D, textureID);
		gl.glUniform1i(texVar, 0);
		gl.glTexParameteri(GL2GL3.GL_TEXTURE_2D, GL2GL3.GL_TEXTURE_WRAP_S, GL2GL3.GL_CLAMP_TO_EDGE);
		gl.glTexParameteri(GL2GL3.GL_TEXTURE_2D, GL2GL3.GL_TEXTURE_WRAP_T, GL2GL3.GL_CLAMP_TO_EDGE);


		if (_isTransparent) {
			gl.glEnable(GL2GL3.GL_BLEND);
			gl.glBlendEquationSeparate(GL2GL3.GL_FUNC_ADD, GL2GL3.GL_MAX);
			gl.glBlendFuncSeparate(GL2GL3.GL_SRC_ALPHA, GL2GL3.GL_ONE_MINUS_SRC_ALPHA, GL2GL3.GL_ONE, GL2GL3.GL_ONE);
		}

		// Draw
		gl.glDisable(GL2GL3.GL_CULL_FACE);
		gl.glDrawArrays(GL2GL3.GL_TRIANGLES, 0, 6);
		gl.glEnable(GL2GL3.GL_CULL_FACE);

		if (_isTransparent) {
			gl.glDisable(GL2GL3.GL_BLEND);
		}

		gl.glBindVertexArray(0);

	}

	@Override
	public long getPickingID() {
		return _pickingID;
	}

	@Override
	public AABB getBoundsRef() {
		return _bounds;
	}

	@Override
	public double getCollisionDist(Ray r, boolean precise)
	{
		return _bounds.collisionDist(r);

		// TODO: precise
	}

	@Override
	public boolean hasTransparent() {
		return _isTransparent;
	}

	@Override
	public void renderTransparent(int contextID, Renderer renderer, Camera cam, Ray pickRay) {
		renderImp(contextID, renderer, cam, pickRay);
	}

	@Override
	public boolean renderForView(int viewID, Camera cam) {
		double dist = cam.distToBounds(getBoundsRef());
		return _visInfo.isVisible(viewID, dist);
	}
}
