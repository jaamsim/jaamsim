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
import java.util.List;
import java.util.Map;

import javax.media.opengl.GL2GL3;

import com.jaamsim.math.AABB;
import com.jaamsim.math.Color4d;
import com.jaamsim.math.Matrix4d;
import com.jaamsim.math.Plane;
import com.jaamsim.math.Ray;
import com.jaamsim.math.Transform;
import com.jaamsim.math.Vector4d;
import com.jaamsim.render.Renderer.ShaderHandle;

/**
 * A Renderable that draws convex (or relatively convex) polygons. Specifically it draw a triangle fan through all the
 * provided points centered at the median point
 *
 * @author matt.chudleigh
 *
 */
public class Polygon implements Renderable {

	private static int _vertBuffer;
	private static int _progHandle; // The flat, unshaded program handle
	private static int _modelViewMatVar;
	private static int _projMatVar;
	private static int _colorVar;
	private static int _posVar;

	private static int _VAOKey;

	private static boolean _hasInitialized;

	private List<Vector4d> _points;

	// Leave these public for now, it's just easier
	private final float[] colour;
	private final float[] hoverColour;
	public boolean isOutline;
	public double lineWidth; // only meaningful if (isOutline)
	public long pickingID;

	private Transform trans;

	private AABB _bounds;

	public Polygon(List<Vector4d> points, Transform trans, Vector4d scale, Color4d colour, Color4d hoverColour, long pickingID) {
		this.colour = colour.toFloats();
		this.hoverColour = hoverColour.toFloats();
		this.isOutline = false;
		this.lineWidth = 1;
		this.pickingID = pickingID;
		this.trans = trans;

		// Points includes the scale, but not the transform
		_points = RenderUtils.transformPoints(Matrix4d.ScaleMatrix(scale), points);

		List<Vector4d> boundsPoints = RenderUtils.transformPoints(trans, _points);

		_bounds = new AABB(boundsPoints);
	}

	@Override
	public void render(Map<Integer, Integer> vaoMap, Renderer renderer,
			Camera cam, Ray pickRay) {
		assert(_hasInitialized);

		GL2GL3 gl = renderer.getGL();
		if (_points.size() < 3) {
			return; // Can't actually draw this polygon
		}

		float[] renderColour = colour;
		if (pickRay != null && getCollisionDist(pickRay) > 0)
			renderColour = hoverColour;

		if (!vaoMap.containsKey(_VAOKey)) {
			setupVAO(vaoMap, renderer);
		}

		int vao = vaoMap.get(_VAOKey);
		gl.glBindVertexArray(vao);

		gl.glUseProgram(_progHandle);

		// Setup uniforms for this object
		Matrix4d projMat = cam.getProjMatRef();
		Matrix4d modelViewMat = new Matrix4d();
		cam.getViewMatrix(modelViewMat);

		modelViewMat.mult(trans.getMatrixRef(), modelViewMat);


		gl.glUniformMatrix4fv(_modelViewMatVar, 1, false, modelViewMat.toFloats(), 0);
		gl.glUniformMatrix4fv(_projMatVar, 1, false, projMat.toFloats(), 0);

		gl.glUniform4fv(_colorVar, 1, renderColour, 0);

		if (isOutline) {
			renderOutline(gl);
		} else {
			renderFill(gl);
		}

		gl.glBindVertexArray(0);

	}

	private void renderOutline(GL2GL3 gl) {
		// The vertex list is just the closed loop of points
		if (lineWidth == 0) {
			return;
		}

		int buffSize = 3 * _points.size();
		FloatBuffer fb = FloatBuffer.allocate(buffSize);
		for (Vector4d vert : _points) {
			fb.put(vert.toFloats3());
		}

		fb.flip();

		gl.glBindBuffer(GL2GL3.GL_ARRAY_BUFFER, _vertBuffer);
		gl.glBufferData(GL2GL3.GL_ARRAY_BUFFER, buffSize * 4, fb, GL2GL3.GL_STATIC_DRAW);

		gl.glVertexAttribPointer(_posVar, 3, GL2GL3.GL_FLOAT, false, 0, 0);

		gl.glBindBuffer(GL2GL3.GL_ARRAY_BUFFER, 0);

		gl.glLineWidth((float)lineWidth);
		gl.glDrawArrays(GL2GL3.GL_LINE_LOOP, 0, _points.size());

		gl.glLineWidth(1.0f);

	}

	private void renderFill(GL2GL3 gl) {

		Vector4d center = new Vector4d();
		for (Vector4d vert : _points) {
			center.addLocal3(vert);
		}
		center.scaleLocal3(1.0/_points.size());

		// The vertex list is just the closed loop of points
		int buffSize = 3 * (_points.size() + 2);
		FloatBuffer fb = FloatBuffer.allocate(buffSize);
		// Put the center to start the triangle fan
		fb.put(center.toFloats3());

		for (Vector4d vert : _points) {
			fb.put(vert.toFloats3());
		}
		fb.put(_points.get(0).toFloats3()); // Add the first point again to close the loop

		fb.flip();

		gl.glBindBuffer(GL2GL3.GL_ARRAY_BUFFER, _vertBuffer);
		gl.glBufferData(GL2GL3.GL_ARRAY_BUFFER, buffSize * 4, fb, GL2GL3.GL_STATIC_DRAW);

		gl.glVertexAttribPointer(_posVar, 3, GL2GL3.GL_FLOAT, false, 0, 0);

		gl.glBindBuffer(GL2GL3.GL_ARRAY_BUFFER, 0);

		gl.glDrawArrays(GL2GL3.GL_TRIANGLE_FAN, 0, _points.size() + 2);

	}

	@Override
	public long getPickingID() {
		return pickingID;
	}

	@Override
	public AABB getBoundsRef() {
		return _bounds;
	}

	@Override
	public double getCollisionDist(Ray r) {
		double dist =  _bounds.collisionDist(r);

		if (dist < 0) { return dist; } // Does not collide with the bounds

		// Create a ray in the points coordinate space
		Transform invTrans = new Transform();
		trans.inverse(invTrans);
		Ray localRay = r.transform(invTrans);

		// Check that this is actually inside the polygon, this assumes the points are co-planar
		Plane p = new Plane(_points.get(0), _points.get(1), _points.get(2));
		dist = p.collisionDist(localRay);

		if (dist < 0) { return dist; } // Behind the start of the ray

		// This is the potential collision point, if it's inside the polygon
		Vector4d collisionPoint = localRay.getPointAtDist(dist);

		Vector4d a = new Vector4d();
		Vector4d b = new Vector4d();
		Vector4d cross = new Vector4d();
		boolean posSign = true;

		for (int i = 0; i < _points.size(); ++i) {
			// Check that the collision point is on the same winding side of all the
			Vector4d p0 = _points.get(i);
			Vector4d p1 = _points.get((i + 1) % _points.size() );
			p0.sub3(collisionPoint, a);
			p1.sub3(p0, b);
			a.cross(b, cross); // cross is the

			double triple = cross.dot3(r.getDirRef());
			// This point is inside the polygon if all triple products have the same sign
			if (i == 0 && triple < 0) {
				// First iteration sets the sign
				posSign = false;
			}
			if (posSign && triple < 0) {
				return -1;
			} else if (!posSign && triple > 0) {
				return -1;
			}
		}
		return dist; // This must be valid then
	}

	// This should be called from the renderer at initialization
	public static void init(Renderer r, GL2GL3 gl) {
		int[] is = new int[1];
		gl.glGenBuffers(1, is, 0);
		_vertBuffer = is[0];

		Shader s = r.getShader(ShaderHandle.DEBUG);
		_progHandle = s.getProgramHandle();
		gl.glUseProgram(_progHandle);

		_modelViewMatVar = gl.glGetUniformLocation(_progHandle, "modelViewMat");
		_projMatVar = gl.glGetUniformLocation(_progHandle, "projMat");
		_colorVar = gl.glGetUniformLocation(_progHandle, "color");

		_posVar = gl.glGetAttribLocation(_progHandle, "position");

		_VAOKey = Renderer.getAssetID();

		_hasInitialized = true;
	}

	private static void setupVAO(Map<Integer, Integer> vaoMap, Renderer renderer) {
		GL2GL3 gl = renderer.getGL();

		int[] vaos = new int[1];
		gl.glGenVertexArrays(1, vaos, 0);
		int vao = vaos[0];
		vaoMap.put(_VAOKey, vao);
		gl.glBindVertexArray(vao);

		gl.glUseProgram(_progHandle);

		gl.glEnableVertexAttribArray(_posVar);

		gl.glBindVertexArray(0);

	}

	@Override
	public boolean hasTransparent() {
		return false;
	}

	@Override
	public void renderTransparent(Map<Integer, Integer> vaoMap, Renderer renderer, Camera cam, Ray pickRay) {
	}

	@Override
	public boolean renderForView(int windowID) {
		return true;
	}

}
