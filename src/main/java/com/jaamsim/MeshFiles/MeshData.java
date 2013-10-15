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
import com.jaamsim.math.Vec2d;
import com.jaamsim.math.Vec3d;
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
	public final static int MAX_SUBINST_HULL_POINTS = 30;

	public final static int NO_TRANS = 0;
	public final static int A_ONE_TRANS = 1;
	public final static int RGB_ZERO_TRANS = 2;

	public static class Material {
		public Color4d diffuseColor;
		public Color4d ambientColor;
		public Color4d specColor;
		public double shininess;

		public URL colorTex;
		boolean useDiffuseTex;

		public int transType;
		public Color4d transColour;
	}

	public static class SubMeshData {

		public ArrayList<Vec3d> verts;
		public ArrayList<Vec2d> texCoords;
		public ArrayList<Vec3d> normals;
		public int[] indices;

		public ConvexHull staticHull;

		public ArrayList<Vec4d> boneIndices;
		public ArrayList<Vec4d> boneWeights;

		public ArrayList<ConvexHull> boneHulls;
		public ConvexHull bonelessHull;
	}

	public static class SubLineData {
		public ArrayList<Vec3d> verts = new ArrayList<Vec3d>();
		public Color4d diffuseColor;

		public int numVerts;
		public ConvexHull hull;
	}

	public static class SubMeshInstance {
		public int subMeshIndex;
		public int materialIndex;
		public int armatureIndex;
		private Mat4d transform;
		private Mat4d normalTrans;
		public int[] boneMapper;
		public String[] boneNames;
		public ArrayList<Action> actions;

		public Mat4d getAnimatedTransform(ArrayList<Action.Queue> aqs) {
			if (aqs == null || aqs.size() == 0 || this.actions == null)
				return transform;

			// Find the first action in aqs that matches one in this.actions
			for (Action.Queue aq : aqs) {
				// Find this action
				for (Action act : this.actions) {
					if (aq.name.equals(act.name)) {
						assert(act.channels.size() == 1);
						return Action.getChannelMatAtTime(act.channels.get(0), aq.time);
					}
				}
			}
			// None of the actions apply to this sub instance
			return transform;
		}

		public Mat4d getAnimatedNormalTransform(ArrayList<Action.Queue> aqs) {
			if (aqs == null || aqs.size() == 0 || this.actions == null)
				return normalTrans;

			// Find the first action in aqs that matches one in this.actions
			for (Action.Queue aq : aqs) {
				// Find this action
				for (Action act : this.actions) {
					if (aq.name.equals(act.name)) {
						assert(act.channels.size() == 1);
						Mat4d trans = Action.getChannelMatAtTime(act.channels.get(0), aq.time);
						Mat4d ret = trans.inverse();
						ret.transpose4();
						return ret;
					}
				}
			}

			return normalTrans;
		}
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

	private ConvexHull _staticHull;
	private double _radius;
	// The AABB of this mesh with no transform applied
	private AABB _defaultBounds;

	private boolean _anyTransparent = false;

	private ArrayList<Action.Description> _actionDesc;

	public void addSubMeshInstance(int meshIndex, int matIndex, int armIndex, Mat4d mat, String[] boneNames, ArrayList<Action> actions) {
		Mat4d trans = new Mat4d(mat);
		SubMeshInstance inst = new SubMeshInstance();
		inst.subMeshIndex = meshIndex;
		inst.materialIndex = matIndex;
		inst.armatureIndex = armIndex;
		inst.transform = trans;
		inst.boneNames = boneNames;
		inst.actions = actions;

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
	                        Color4d ambientColor,
	                        Color4d specColor,
	                        double shininess,
	                        int transType,
	                        Color4d transColour) {

		Material mat = new Material();
		if (colorTex == null) {
			assert diffuseColor != null;
		}


		mat.diffuseColor = diffuseColor;
		mat.ambientColor = ambientColor;
		mat.specColor = specColor;
		mat.shininess = shininess;
		mat.colorTex = colorTex;

		if (mat.ambientColor == null) mat.ambientColor = new Color4d();
		if (mat.specColor == null) mat.specColor = new Color4d();

		if (mat.specColor.r == 0.0 &&
		    mat.specColor.g == 0.0 &&
		    mat.specColor.b == 0.0) {

			// A black spec color means that the shininess has no effect
			// set it to one as the shader uses shininess < 2 to fast path
			// the spec calculations
			mat.shininess = 1;
		}

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

	// Returns a new index list with any zero area triangles removed
	private int[] removeDegenerateTriangles(ArrayList<Vertex> vertices, int[] indices) {
		assert(indices.length % 3 == 0);
		int[] goodIndices = new int[indices.length];
		int goodWritePos = 0;

		for (int triInd = 0; triInd < indices.length/3; ++triInd) {
			int ind0 = indices[triInd * 3 + 0];
			int ind1 = indices[triInd * 3 + 1];
			int ind2 = indices[triInd * 3 + 2];
			Vec3d pos0 = vertices.get(ind0).getPos();
			Vec3d pos1 = vertices.get(ind1).getPos();
			Vec3d pos2 = vertices.get(ind2).getPos();

			if (ind0 == ind1 || ind1 == ind2 || ind2 == ind0) {
				continue;
			}
			if (pos0.equals3(pos1) || pos1.equals3(pos2) || pos2.equals3(pos0)) {
				continue;
			}
			goodIndices[goodWritePos++] = ind0;
			goodIndices[goodWritePos++] = ind1;
			goodIndices[goodWritePos++] = ind2;
		}
		// Finally rebuild the index list
		if (goodIndices.length == indices.length) {
			return goodIndices; // No degenerates found
		}
		int[] ret = new int[goodWritePos];
		for (int i = 0; i < goodWritePos; ++i) {
			ret[i] = goodIndices[i];
		}
		return ret;
	}

	public void addSubMesh(ArrayList<Vertex> vertices,
	                       int[] indices) {

		assert(vertices.size() >= 3);

		SubMeshData sub = new SubMeshData();
		_subMeshesData.add(sub);

		boolean hasBoneInfo = vertices.get(0).getBoneIndices() != null;

		if (!hasBoneInfo) {
			// If this mesh can not be animated, do an extra check and remove zero area triangles
			// (for animated meshes, this is not safe as the triangles may not alway be zero area)
			int[] goodIndices = removeDegenerateTriangles(vertices, indices);
			sub.indices = goodIndices;
		} else {
			sub.indices = indices;
		}

		assert((sub.indices.length % 3) == 0);

		// Assume if there is one tex coordinate, there will be all of them
		boolean hasTexCoords = vertices.get(0).getTexCoord() != null;

		sub.verts = new ArrayList<Vec3d>(vertices.size());
		sub.normals = new ArrayList<Vec3d>(vertices.size());

		if (hasTexCoords) {
			sub.texCoords = new ArrayList<Vec2d>(vertices.size());
		}
		if (hasBoneInfo) {
			sub.boneIndices = new ArrayList<Vec4d>(vertices.size());
			sub.boneWeights = new ArrayList<Vec4d>(vertices.size());
		}
		int maxBoneIndex = -1;
		for (Vertex v : vertices) {
			sub.verts.add(v.getPos());
			sub.normals.add(v.getNormal());
			if (hasTexCoords) {
				sub.texCoords.add(v.getTexCoord());
			}
			if (hasBoneInfo) {
				Vec4d boneIndices = v.getBoneIndices();
				Vec4d boneWeights = v.getBoneWeights();
				sub.boneIndices.add(boneIndices);
				sub.boneWeights.add(boneWeights);

				if (boneWeights.x > 0 && (int)boneIndices.x > maxBoneIndex)
					maxBoneIndex = (int)boneIndices.x;
				if (boneWeights.y > 0 && (int)boneIndices.y > maxBoneIndex)
					maxBoneIndex = (int)boneIndices.y;
				if (boneWeights.z > 0 && (int)boneIndices.z > maxBoneIndex)
					maxBoneIndex = (int)boneIndices.z;
				if (boneWeights.w > 0 && (int)boneIndices.w > maxBoneIndex)
					maxBoneIndex = (int)boneIndices.w;
			}
		}

		if (hasBoneInfo) {
			// Generate the per-bone convex hulls
			sub.boneHulls = new ArrayList<ConvexHull>(maxBoneIndex + 1);
			for(int i = 0; i < maxBoneIndex + 1; ++i) {
				ArrayList<Vec3d> boneVerts = new ArrayList<Vec3d>();
				// Scan all vertices, and if it is influenced by this bone, add it to the hull
				for (Vertex v : vertices) {
					Vec4d boneIndices = v.getBoneIndices();
					Vec4d boneWeights = v.getBoneWeights();
					boolean isInfluenced = false;
					if (boneWeights.x > 0 && (int)boneIndices.x == i)
						isInfluenced = true;
					if (boneWeights.y > 0 && (int)boneIndices.y == i)
						isInfluenced = true;
					if (boneWeights.z > 0 && (int)boneIndices.z == i)
						isInfluenced = true;
					if (boneWeights.w > 0 && (int)boneIndices.w == i)
						isInfluenced = true;
					if (isInfluenced) {
						boneVerts.add(v.getPos());
					}
				}

				ConvexHull boneHull = ConvexHull.TryBuildHull(boneVerts, MAX_HULL_ATTEMPTS, MAX_HULL_POINTS);
				sub.boneHulls.add(boneHull);
			}
			// Lastly, make a convex hull of any vertices that are influenced by no bones
			ArrayList<Vec3d> bonelessVerts = new ArrayList<Vec3d>();
			for (Vertex v : vertices) {
				Vec4d boneIndices = v.getBoneIndices();
				if (boneIndices.x == -1) {
					bonelessVerts.add(v.getPos());
				}
			}
			sub.bonelessHull = ConvexHull.TryBuildHull(bonelessVerts, MAX_HULL_ATTEMPTS, MAX_HULL_POINTS);
		}

		sub.staticHull = ConvexHull.TryBuildHull(sub.verts, MAX_HULL_ATTEMPTS, MAX_HULL_POINTS);
	}

	public void addSubLine(Vec3d[] vertices,
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
	public void finalizeData() {
		ArrayList<Vec3d> totalHullPoints = new ArrayList<Vec3d>();
		// Collect all the points from the hulls of the individual sub meshes
		for (SubMeshInstance subInst : _subMeshInstances) {

			List<Vec3d> pointsRef = _subMeshesData.get(subInst.subMeshIndex).staticHull.getVertices();
			List<Vec3d> subPoints = RenderUtils.transformPointsWithTrans(subInst.transform, pointsRef);

			totalHullPoints.addAll(subPoints);
		}
		// And the lines
		for (SubLineInstance subInst : _subLineInstances) {

			List<Vec3d> pointsRef = _subLinesData.get(subInst.subLineIndex).hull.getVertices();
			List<Vec3d> subPoints = RenderUtils.transformPointsWithTrans(subInst.transform, pointsRef);

			totalHullPoints.addAll(subPoints);
		}

		_staticHull = ConvexHull.TryBuildHull(totalHullPoints, MAX_HULL_ATTEMPTS, MAX_HULL_POINTS);
		_defaultBounds = _staticHull.getAABB(new Mat4d());

		_radius = _staticHull.getRadius();

		_actionDesc = new ArrayList<Action.Description>();
		// Add all the actions found in the armatures
		for (Armature arm : _armatures) {
			for (Action act : arm.getActions()) {
				Action.Description desc = new Action.Description();
				desc.name = act.name;
				desc.duration = act.duration;
				_actionDesc.add(desc);
			}
		}
		// And sub meshes
		for (SubMeshInstance subInst : _subMeshInstances) {
			if (subInst.actions != null) {
				for (Action act : subInst.actions) {
					Action.Description desc = new Action.Description();
					desc.name = act.name;
					desc.duration = act.duration;
					_actionDesc.add(desc);
				}
			}
		}
	}

	public double getRadius() {
		return _radius;
	}

	public ConvexHull getHull(ArrayList<Action.Queue> actions) {
		if (actions == null || actions.size() == 0)
			return _staticHull;

		ArrayList<ConvexHull> subInstHulls = new ArrayList<ConvexHull>(_subMeshInstances.size());
		for (SubMeshInstance subInst : _subMeshInstances) {
			ConvexHull subHull = getSubInstHull(subInst, actions);
			subInstHulls.add(subHull);
		}
		return getHull(actions, subInstHulls);
	}

	// This is an optimization. Mesh needs a list of sub mesh hulls based on a pose. As it already has it calculated,
	// it can be passed back to MeshData and save the effort of recalculating them. The other getHull() will do that
	// calculation if it's needed.
	public ConvexHull getHull(ArrayList<Action.Queue> actions, ArrayList<ConvexHull> subInstHulls) {
		if (actions == null || actions.size() == 0)
			return _staticHull;

		// Otherwise, we need to calculate a new hull
		ArrayList<Vec3d> hullPoints = new ArrayList<Vec3d>();

		for (ConvexHull subHull : subInstHulls) {
			hullPoints.addAll(subHull.getVertices());
		}

		// And the lines
		for (SubLineInstance subInst : _subLineInstances) {

			List<Vec3d> pointsRef = _subLinesData.get(subInst.subLineIndex).hull.getVertices();
			List<Vec3d> subPoints = RenderUtils.transformPointsWithTrans(subInst.transform, pointsRef);

			hullPoints.addAll(subPoints);
		}

		ConvexHull ret = ConvexHull.TryBuildHull(hullPoints, MAX_HULL_ATTEMPTS, MAX_HULL_POINTS);
		return ret;
	}

	public AABB getDefaultBounds() {
		return _defaultBounds;
	}

	public ArrayList<Action.Description> getActionDescriptions() {
		return _actionDesc;
	}

	private ConvexHull getSubInstHull(SubMeshInstance subInst, ArrayList<Action.Queue> actions) {

		ArrayList<Vec3d> hullPoints = new ArrayList<Vec3d>();
		Mat4d animatedTransform = subInst.getAnimatedTransform(actions);

		if (actions == null || actions.size() == 0 || subInst.armatureIndex == -1) {
			// This is an unanimated sub instance, just add the normal points
			List<Vec3d> pointsRef = _subMeshesData.get(subInst.subMeshIndex).staticHull.getVertices();
			List<Vec3d> subPoints = RenderUtils.transformPointsWithTrans(animatedTransform, pointsRef);
			hullPoints.addAll(subPoints);
		} else {
			// We need to add each bone in it's animated position
			Armature arm = _armatures.get(subInst.armatureIndex);
			SubMeshData subMesh = _subMeshesData.get(subInst.subMeshIndex);
			ArrayList<Mat4d> pose = arm.getPose(actions);
			for (int bInstInd = 0; bInstInd < subMesh.boneHulls.size(); ++bInstInd) {
				ConvexHull boneHull = subMesh.boneHulls.get(bInstInd);
				Mat4d boneMat = null;

				if (bInstInd < subInst.boneMapper.length)
					boneMat = pose.get(subInst.boneMapper[bInstInd]);

				for (Vec3d hullVect : boneHull.getVertices()) {
					Vec3d temp = new Vec3d(hullVect);
					temp.multAndTrans3(animatedTransform, temp);
					if (boneMat != null)
						temp.multAndTrans3(boneMat, temp);
					hullPoints.add(temp);
				}
			}
			// Add the boneless vertices
			List<Vec3d> pointsRef = _subMeshesData.get(subInst.subMeshIndex).bonelessHull.getVertices();
			List<Vec3d> subPoints = RenderUtils.transformPointsWithTrans(animatedTransform, pointsRef);
			hullPoints.addAll(subPoints);
		}

		return ConvexHull.TryBuildHull(hullPoints, MAX_HULL_ATTEMPTS, MAX_SUBINST_HULL_POINTS);
	}

	public ArrayList<ConvexHull> getSubHulls(ArrayList<Action.Queue> actions) {

		ArrayList<ConvexHull> ret = new ArrayList<ConvexHull>(_subMeshInstances.size());

		for (SubMeshInstance subInst : _subMeshInstances) {

			ConvexHull instHull = getSubInstHull(subInst, actions);
			ret.add(instHull);
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

	public int getNumTriangles() {
		int numTriangles = 0;
		for (SubMeshInstance inst : _subMeshInstances) {
			SubMeshData data = _subMeshesData.get(inst.subMeshIndex);
			numTriangles += data.indices.length / 3;
		}
		return numTriangles;
	}

	public int getNumVertices() {
		int numVerts = 0;
		for (SubMeshData data : _subMeshesData) {
			numVerts += data.verts.size();
		}
		return numVerts;
	}

	public int getNumSubInstances() {
		return _subMeshInstances.size() + _subLineInstances.size();
	}
	public int getNumSubMeshes() {
		return _subMeshesData.size() + _subLinesData.size();
	}

}
