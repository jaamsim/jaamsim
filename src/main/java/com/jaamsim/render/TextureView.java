/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2012 Ausenco Engineering Canada Inc.
 * Copyright (C) 2025 JaamSim Software Inc.
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
import com.jaamsim.math.Mat4d;
import com.jaamsim.math.Ray;
import com.jaamsim.math.Transform;
import com.jaamsim.math.Vec2d;
import com.jaamsim.math.Vec3d;
import com.jaamsim.math.Vec4d;
import com.jogamp.opengl.GL2GL3;


/**
 * TextureView is a simple renderable that allows a rectangular image to be easily displayed in 3-space
 * @author Matt.Chudleigh
 *
 */
public class TextureView implements Renderable {

	private final TexLoader _texLoader;
	private final Transform _trans;
	private final Vec3d _scale;
	private final long _pickingID;
	private ArrayList<Vec2d> _texCoords;

	private final AABB _bounds;

	private final VisibilityInfo _visInfo;
	private final float[] lightDir = new float[3];

	// Initialize the very simple buffers needed to render this image
	static private boolean staticInit = false;
	static private int vertBuff;
	static private int normalBuff;
	static private HashMap<Integer, Integer> VAOMap = new HashMap<>();

	// The texture coordinates to use when none are provided
	static private int defaultTexCoordBuff;

	// A buffer handle for when custom texture coordinates are provided
	static private int customTexCoordBuff;

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

	static private int cVar;
	static private int fcVar;

	static private float[] lightInt = new float[1];

	// a column-major identity matrix
	static private float[] identMat = new float[16];

	static {
		identMat[ 0] = 1.0f; identMat[ 4] = 0.0f; identMat[ 8] = 0.0f; identMat[12] = 0.0f;
		identMat[ 1] = 0.0f; identMat[ 5] = 1.0f; identMat[ 9] = 0.0f; identMat[13] = 0.0f;
		identMat[ 2] = 0.0f; identMat[ 6] = 0.0f; identMat[10] = 1.0f; identMat[14] = 0.0f;
		identMat[ 3] = 0.0f; identMat[ 7] = 0.0f; identMat[11] = 0.0f; identMat[15] = 1.0f;
	}

	public TextureView(TexLoader loader, ArrayList<Vec2d> texCoords, Transform trans, Vec3d scale,
            VisibilityInfo visInfo, long pickingID) {
		_texLoader = loader;
		_trans = trans;
		_scale = scale;
		_scale.z = 1; // This object can only be scaled in X, Y
		_pickingID = pickingID;
		_visInfo = visInfo;

		_texCoords = texCoords;

		Mat4d modelMat = RenderUtils.mergeTransAndScale(_trans, _scale);

		ArrayList<Vec4d> vs = new ArrayList<>(4);
		vs.add(new Vec4d( 0.5,  0.5, 0, 1.0d));
		vs.add(new Vec4d(-0.5,  0.5, 0, 1.0d));
		vs.add(new Vec4d(-0.5, -0.5, 0, 1.0d));
		vs.add(new Vec4d( 0.5, -0.5, 0, 1.0d));

		_bounds = new AABB(vs, modelMat);

		// Lighting direction
		Vec4d vec = new Vec4d(0, 0, -1, 0);
		_trans.apply(vec, vec);
		vec.scale3(1.0d/_trans.getScale());
		lightDir[0] = (float) vec.x;
		lightDir[1] = (float) vec.y;
		lightDir[2] = (float) vec.z;
	}

	private static void initStaticBuffers(Renderer r) {
		GL2GL3 gl = r.getGL();

		int[] buffs = new int[4];
		gl.glGenBuffers(4, buffs, 0);
		vertBuff = buffs[0];
		defaultTexCoordBuff = buffs[1];
		normalBuff = buffs[2];
		customTexCoordBuff = buffs[3];

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
		gl.glBindBuffer(GL2GL3.GL_ARRAY_BUFFER, defaultTexCoordBuff);
		gl.glBufferData(GL2GL3.GL_ARRAY_BUFFER, 6*2*4, texCoords, GL2GL3.GL_STATIC_DRAW);

		FloatBuffer normals = FloatBuffer.allocate(6*3); // 2 triangles * 3 coordinates
		for (int i = 0; i < 6; ++i) {
			normals.put(0.0f); normals.put(0.0f); normals.put(1.0f);
		}

		normals.flip();
		gl.glBindBuffer(GL2GL3.GL_ARRAY_BUFFER, normalBuff);
		gl.glBufferData(GL2GL3.GL_ARRAY_BUFFER, 6*3*4, normals, GL2GL3.GL_STATIC_DRAW);

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

		cVar = gl.glGetUniformLocation(progHandle, "C");
		fcVar = gl.glGetUniformLocation(progHandle, "FC");

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

		gl.glBindVertexArray(0);

	}

	private void bindDefaultTexCoords(Renderer renderer) {
		GL2GL3 gl = renderer.getGL();
		int texCoordVar = gl.glGetAttribLocation(progHandle, "texCoord");
		gl.glEnableVertexAttribArray(texCoordVar);

		gl.glBindBuffer(GL2GL3.GL_ARRAY_BUFFER, defaultTexCoordBuff);
		gl.glVertexAttribPointer(texCoordVar, 2, GL2GL3.GL_FLOAT, false, 0, 0);
	}

	private void updateCustomTexCoordBuffer(Renderer renderer) {
		GL2GL3 gl = renderer.getGL();
		int texCoordBuffSize = _texCoords.size()*2*4;

		FloatBuffer texData = FloatBuffer.allocate(_texCoords.size()*2);

		for (Vec2d v : _texCoords) {
			texData.put((float)v.x); texData.put((float)v.y);
		}
		texData.flip();
		gl.glBindBuffer(GL2GL3.GL_ARRAY_BUFFER, customTexCoordBuff);
		gl.glBufferData(GL2GL3.GL_ARRAY_BUFFER, texCoordBuffSize, texData, GL2GL3.GL_STATIC_DRAW);

		int texCoordVar = gl.glGetAttribLocation(progHandle, "texCoord");
		gl.glEnableVertexAttribArray(texCoordVar);

		gl.glBindBuffer(GL2GL3.GL_ARRAY_BUFFER, customTexCoordBuff);
		gl.glVertexAttribPointer(texCoordVar, 2, GL2GL3.GL_FLOAT, false, 0, 0);

	}

	@Override
	public void render(int contextID, Renderer renderer, Camera cam, Ray pickRay) {
		if (_texLoader.hasTransparent()) {
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

		int textureID = _texLoader.getTexID(renderer);

		if (textureID == TexCache.LOADING_TEX_ID) {
			return; // This texture is not ready yet
		}

		if (!VAOMap.containsKey(contextID)) {
			setupVAO(contextID, renderer);
		}

		int vao = VAOMap.get(contextID);
		gl.glBindVertexArray(vao);

		if (_texCoords == null) {
			bindDefaultTexCoords(renderer);
		} else {
			updateCustomTexCoordBuffer(renderer);
		}

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

		// Draw
		gl.glDisable(GL2GL3.GL_CULL_FACE);
		gl.glDrawArrays(GL2GL3.GL_TRIANGLES, 0, 6);
		gl.glEnable(GL2GL3.GL_CULL_FACE);

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
		return _texLoader.hasTransparent();
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
