/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2013 Ausenco Engineering Canada Inc.
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
import java.util.HashMap;

import javax.media.opengl.GL2GL3;

import com.jaamsim.math.Mat4d;

public class Skybox {

	private URL textureURL;

	private static HashMap<Integer, Integer> VAOMap = new HashMap<Integer, Integer>();
	private static int vertBuff;
	private static int progHandle;

	private static int projMatVar;
	private static int invViewMatVar;
	private static int texVar;

	private static boolean isLoaded;

	public void setTexture(URL texURL) {
		textureURL = texURL;
	}

	public static void loadGPUAssets(Renderer r) {

		GL2GL3 gl = r.getGL();

		int[] buffs = new int[1];
		gl.glGenBuffers(1, buffs, 0);
		vertBuff = buffs[0];

		progHandle = r.getShader(Renderer.ShaderHandle.SKYBOX).getProgramHandle();

		projMatVar = gl.glGetUniformLocation(progHandle, "projMat");
		invViewMatVar = gl.glGetUniformLocation(progHandle, "invViewMat");
		texVar = gl.glGetUniformLocation(progHandle, "tex");

		FloatBuffer verts = FloatBuffer.allocate(6*3); // 2 triangles * 3 coordinates
		verts.put(-0.5f); verts.put(-0.5f); verts.put(-0.5f);
		verts.put( 0.5f); verts.put(-0.5f); verts.put(-0.5f);
		verts.put( 0.5f); verts.put( 0.5f); verts.put(-0.5f);

		verts.put(-0.5f); verts.put(-0.5f); verts.put(-0.5f);
		verts.put( 0.5f); verts.put( 0.5f); verts.put(-0.5f);
		verts.put(-0.5f); verts.put( 0.5f); verts.put(-0.5f);

		verts.flip();
		gl.glBindBuffer(GL2GL3.GL_ARRAY_BUFFER, vertBuff);
		gl.glBufferData(GL2GL3.GL_ARRAY_BUFFER, 6*3*4, verts, GL2GL3.GL_STATIC_DRAW);

		gl.glBindBuffer(GL2GL3.GL_ARRAY_BUFFER, 0);

		isLoaded = true;
	}

	public void render(int contextID, Renderer renderer, Camera cam) {

		if (textureURL == null) {
			return;
		}

		if (!isLoaded) {
			loadGPUAssets(renderer);
		}
		if (!VAOMap.containsKey(contextID)) {
			setupVAO(contextID, renderer);
		}

		GL2GL3 gl = renderer.getGL();

		int textureID = renderer.getTexCache().getTexID(gl, textureURL, false, false, false);
		if (textureID == TexCache.LOADING_TEX_ID) {
			// Sky box is not ready yet, get it next time
			return;
		}

		int vao = VAOMap.get(contextID);
		gl.glBindVertexArray(vao);

		gl.glUseProgram(progHandle);

		gl.glUniformMatrix4fv(projMatVar, 1, false, RenderUtils.MarshalMat4d(cam.getProjMat4d()), 0);

		Mat4d invViewMat = new Mat4d();
		invViewMat.setRot4(cam.getTransformRef().getRotRef());
		gl.glUniformMatrix4fv(invViewMatVar, 1, false, RenderUtils.MarshalMat4d(invViewMat), 0);

		gl.glActiveTexture(GL2GL3.GL_TEXTURE0);
		gl.glBindTexture(GL2GL3.GL_TEXTURE_2D, textureID);
		// Disable mipmaps for skyboxes as the discontinuity screws them up
		gl.glTexParameteri(GL2GL3.GL_TEXTURE_2D, GL2GL3.GL_TEXTURE_MIN_FILTER, GL2GL3.GL_LINEAR );

		gl.glUniform1i(texVar, 0);

		gl.glDepthMask(false);
		gl.glDrawArrays(GL2GL3.GL_TRIANGLES, 0, 6);
		gl.glDepthMask(true);

		gl.glTexParameteri(GL2GL3.GL_TEXTURE_2D, GL2GL3.GL_TEXTURE_MIN_FILTER, GL2GL3.GL_LINEAR_MIPMAP_LINEAR );

		gl.glBindVertexArray(0);

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

		gl.glBindBuffer(GL2GL3.GL_ARRAY_BUFFER, 0);

		gl.glBindVertexArray(0);

	}

}
