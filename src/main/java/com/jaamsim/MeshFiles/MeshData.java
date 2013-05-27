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
import com.jaamsim.render.Action;
import com.jaamsim.render.Armature;
import com.jaamsim.render.RenderUtils;

/**
 * MeshData represents all the data contained in a mesh 3d object. This data is not yet in video ram and can be written back to disk
 * This is not useful for rendering, for that a MeshProto is needed (which is owned by the Renderer)
 * @author matt.chudleigh
 *
 */
public class MeshData {

	public final static int MAX_HULL_ATTEMPTS = 5;
	public final static int MAX_HULL_POINTS = 100;

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

		public ArrayList<Vec4d> verts;
		public ArrayList<Vec4d> texCoords;
		public ArrayList<Vec4d> normals;
		public int[] indices;

		public ConvexHull hull;

		public ArrayList<Vec4d> boneIndices;
		public ArrayList<Vec4d> boneWeights;
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
		public int armatureIndex;
		public Mat4d transform;
		public Mat4d normalTrans;
		public int[] boneMapper;
		public String[] boneNames;
	}

	public static class SubLineInstance {
		public int subLineIndex;
		public Mat4d transform;
	}

	private ArrayList<SubMeshData> _subMeshesData = new ArrayList<SubMeshData>();
	private ArrayList<SubLineData> _subLinesData = new ArrayList<SubLineData>();
	private ArrayList<Material> _materials = new ArrayList<Material>();

	private ArrayList<Armature> _armatures = new ArrayList<Armature>();

	private ArrayList<SubMeshInstance> _subMeshInstances = new ArrayList<SubMeshInstance>();
	private ArrayList<SubLineInstance> _subLineInstances = new ArrayList<SubLineInstance>();

	private ConvexHull _hull;
	private double _radius;
	// The AABB of this mesh with no transform applied
	private AABB _defaultBounds;

	private boolean _anyTransparent = false;

	private ArrayList<Action.Description> _actionDesc;

	public void addSubMeshInstance(int meshIndex, int matIndex, int armIndex, Mat4d mat, String[] boneNames) {
		Mat4d trans = new Mat4d(mat);
		SubMeshInstance inst = new SubMeshInstance();
		inst.subMeshIndex = meshIndex;
		inst.materialIndex = matIndex;
		inst.armatureIndex = armIndex;
		inst.transform = trans;
		inst.boneNames = boneNames;

		if (boneNames != null) {
			assert(armIndex != -1);
			// Build up the mapping of armature bone indices into mesh bone indices
			// (these will often be the same, but this is part of how blender binds meshes to armatures so let's just make sure)
			inst.boneMapper = new int[boneNames.length];
			Armature arm = _armatures.get(armIndex);
			ArrayList<Armature.Bone> bones = arm.getAllBones();
			for (int i = 0; i < boneNames.length; ++i) {
				// Find this bone in the armature
				boolean boneFound = false;
				for (int j = 0; j < bones.size(); ++j) {
					if (bones.get(j).getName().equals(boneNames[i])) {
						inst.boneMapper[i] = j;
						boneFound = true;
						break;
					}
				}
				assert(boneFound);
			}
		}

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

	public void addArmature(Armature arm) {
		_armatures.add(arm);
	}

	public void addSubMesh(ArrayList<Vertex> vertices,
	                       int[] indices) {

		assert(vertices.size() >= 3);

		SubMeshData sub = new SubMeshData();
		_subMeshesData.add(sub);

		sub.indices = indices;

		assert((sub.indices.length % 3) == 0);

		// Assume if there is one tex coordinate, there will be all of them
		boolean hasTexCoords = vertices.get(0).getTexCoord() != null;

		boolean hasBoneInfo = vertices.get(0).getBoneIndices() != null;

		sub.verts = new ArrayList<Vec4d>(vertices.size());
		sub.normals = new ArrayList<Vec4d>(vertices.size());

		if (hasTexCoords) {
			sub.texCoords = new ArrayList<Vec4d>(vertices.size());
		}
		if (hasBoneInfo) {
			sub.boneIndices = new ArrayList<Vec4d>(vertices.size());
			sub.boneWeights = new ArrayList<Vec4d>(vertices.size());
		}
		for (Vertex v : vertices) {
			sub.verts.add(v.getPos());
			sub.normals.add(v.getNormal());
			if (hasTexCoords) {
				sub.texCoords.add(v.getTexCoord());
			}
			if (hasBoneInfo) {
				sub.boneIndices.add(v.getBoneIndices());
				sub.boneWeights.add(v.getBoneWeights());
			}
		}

		sub.hull = ConvexHull.TryBuildHull(sub.verts, MAX_HULL_ATTEMPTS, MAX_HULL_POINTS);
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

		sub.hull = ConvexHull.TryBuildHull(sub.verts, MAX_HULL_ATTEMPTS, MAX_HULL_POINTS);
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

		_hull = ConvexHull.TryBuildHull(totalHullPoints, MAX_HULL_ATTEMPTS, MAX_HULL_POINTS);
		_defaultBounds = _hull.getAABB(new Mat4d());

		_radius = _hull.getRadius();

		_actionDesc = new ArrayList<Action.Description>();
		for (Armature arm : _armatures) {
			for (Armature.ArmAction act : arm.getActions()) {
				Action.Description desc = new Action.Description();
				desc.name = act.name;
				desc.duration = act.duration;
				_actionDesc.add(desc);
			}
		}
	}

	public double getRadius() {
		return _radius;
	}

	public ConvexHull getHull() {
		return _hull;
	}

	public AABB getDefaultBounds() {
		return _defaultBounds;
	}

	public ArrayList<Action.Description> getActionDescriptions() {
		return _actionDesc;
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

	public ArrayList<Armature> getArmatures() {
		return _armatures;
	}

}
