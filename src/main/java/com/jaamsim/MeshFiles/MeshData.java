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
package com.jaamsim.MeshFiles;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.jaamsim.math.AABB;
import com.jaamsim.math.Color4d;
import com.jaamsim.math.ConvexHull;
import com.jaamsim.math.Mat4d;
import com.jaamsim.math.Vec4d;
import com.jaamsim.render.RenderUtils;

/**
 * MeshData represents all the data contained in a mesh 3d object. This data is not yet in video ram and can be written back to disk
 * This is not useful for rendering, for that a MeshProto is needed (which is owned by the Renderer)
 * @author matt.chudleigh
 *
 */
public class MeshData {

	public final static int NO_TRANS = 0;
	public final static int A_ONE_TRANS = 1;
	public final static int RGB_ZERO_TRANS = 2;

	public static class Material {
		public Color4d diffuseColor;
		public URL colorTex;
		boolean useDiffuseTex;

		public int transType;
		public Color4d transColour;
	}

	public static class SubMeshData {

		public ArrayList<Vec4d> verts = new ArrayList<Vec4d>();
		public ArrayList<Vec4d> texCoords = new ArrayList<Vec4d>();
		public ArrayList<Vec4d> normals = new ArrayList<Vec4d>();

		public int numVerts;
		public ConvexHull hull;
	}

	public static class SubLineData {
		public ArrayList<Vec4d> verts = new ArrayList<Vec4d>();
		public Color4d diffuseColor;

		public int numVerts;
		public ConvexHull hull;
	}

	public static class SubMeshInstance {
		public int subMeshIndex;
		public int materialIndex;
		public Mat4d transform;
		public Mat4d normalTrans;
	}

	public static class SubLineInstance {
		public int subLineIndex;
		public Mat4d transform;
	}

	private ArrayList<SubMeshData> _subMeshesData = new ArrayList<SubMeshData>();
	private ArrayList<SubLineData> _subLinesData = new ArrayList<SubLineData>();
	private ArrayList<Material> _materials = new ArrayList<Material>();
	private ArrayList<SubMeshInstance> _subMeshInstances = new ArrayList<SubMeshInstance>();
	private ArrayList<SubLineInstance> _subLineInstances = new ArrayList<SubLineInstance>();

	private ConvexHull _hull;
	private double _radius;

	private boolean _anyTransparent = false;

	public void addSubMeshInstance(int meshIndex, int matIndex, Mat4d mat) {
		Mat4d trans = new Mat4d(mat);
		SubMeshInstance inst = new SubMeshInstance();
		inst.subMeshIndex = meshIndex;
		inst.materialIndex = matIndex;
		inst.transform = trans;

		Mat4d normalMat = trans.inverse();
		normalMat.transpose4();
		inst.normalTrans = normalMat;
		_subMeshInstances.add(inst);
	}

	public void addSubLineInstance(int lineIndex, Mat4d mat) {
		Mat4d trans = new Mat4d(mat);
		SubLineInstance inst = new SubLineInstance();
		inst.subLineIndex = lineIndex;
		inst.transform = trans;

		_subLineInstances.add(inst);
	}

	public void addMaterial(URL colorTex,
	                        Color4d diffuseColor,
	                        int transType,
	                        Color4d transColour) {

		Material mat = new Material();
		if (colorTex == null) {
			assert diffuseColor != null;
		}

		mat.diffuseColor = diffuseColor;
		mat.colorTex = colorTex;

		mat.transType = transType;
		mat.transColour = transColour;
		_materials.add(mat);

		if (transType != NO_TRANS) {
			_anyTransparent = true;
		}
	}

	public void addSubMesh(Vec4d[] vertices,
			Vec4d[] normals,
			Vec4d[] texCoords) {


		SubMeshData sub = new SubMeshData();
		_subMeshesData.add(sub);

		// This is a new sub mesh (or one with a unique color or texture)

		sub.numVerts += vertices.length;
		//_numVerts += vertices.length;

		assert((sub.numVerts % 3) == 0);

		assert(normals.length == vertices.length);

		if (texCoords != null) {
			assert(texCoords.length == vertices.length);

			sub.texCoords.addAll(Arrays.asList(texCoords));
		}

		sub.verts.addAll(Arrays.asList(vertices));
		sub.normals.addAll(Arrays.asList(normals));

		sub.hull = ConvexHull.TryBuildHull(sub.verts, 5);
	}

	public void addSubLine(Vec4d[] vertices,
			Color4d diffuseColor) {

		SubLineData sub = new SubLineData();
		sub.diffuseColor = diffuseColor;
		if (sub.diffuseColor == null) {
			sub.diffuseColor = new Color4d(); // Default to black
		}
		_subLinesData.add(sub);

		sub.numVerts += vertices.length;

		assert((sub.numVerts % 2) == 0);

		sub.verts.addAll(Arrays.asList(vertices));

		sub.hull = ConvexHull.TryBuildHull(sub.verts, 5);
	}

	public boolean hasTransparent() {
		return _anyTransparent;
	}

	/**
	 * Builds the convex hull of the current mesh based on all the existing sub meshes.
	 */
	public void generateHull() {
		ArrayList<Vec4d> totalHullPoints = new ArrayList<Vec4d>();
		// Collect all the points from the hulls of the individual sub meshes
		for (SubMeshInstance subInst : _subMeshInstances) {

			List<Vec4d> pointsRef = _subMeshesData.get(subInst.subMeshIndex).hull.getVertices();
			List<Vec4d> subPoints = RenderUtils.transformPoints(subInst.transform, pointsRef, 0);

			totalHullPoints.addAll(subPoints);
		}
		// And the lines
		for (SubLineInstance subInst : _subLineInstances) {

			List<Vec4d> pointsRef = _subLinesData.get(subInst.subLineIndex).hull.getVertices();
			List<Vec4d> subPoints = RenderUtils.transformPoints(subInst.transform, pointsRef, 0);

			totalHullPoints.addAll(subPoints);
		}

		_hull = ConvexHull.TryBuildHull(totalHullPoints, 5);

		_radius = _hull.getRadius();
	}

	public double getRadius() {
		return _radius;
	}

	public ConvexHull getHull() {
		return _hull;
	}

	public ArrayList<AABB> getSubBounds(Mat4d modelMat) {

		Mat4d subModelMat = new Mat4d();

		ArrayList<AABB> ret = new ArrayList<AABB>(_subMeshInstances.size());

		for (SubMeshInstance subInst : _subMeshInstances) {

			SubMeshData subMesh = _subMeshesData.get(subInst.subMeshIndex);
			subModelMat.mult4(modelMat, subInst.transform);
			AABB instBounds = subMesh.hull.getAABB(subModelMat);
			ret.add(instBounds);
		}

		return ret;
	}

	public ArrayList<SubMeshData> getSubMeshData() {
		return _subMeshesData;
	}
	public ArrayList<SubLineData> getSubLineData() {
		return _subLinesData;
	}

	public ArrayList<SubMeshInstance> getSubMeshInstances() {
		return _subMeshInstances;
	}
	public ArrayList<SubLineInstance> getSubLineInstances() {
		return _subLineInstances;
	}

	public ArrayList<Material> getMaterials() {
		return _materials;
	}

}
