/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2013 Ausenco Engineering Canada Inc.
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
package com.jaamsim.MeshFiles;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.jaamsim.MeshFiles.DataBlock.Error;
import com.jaamsim.math.AABB;
import com.jaamsim.math.Color4d;
import com.jaamsim.math.ConvexHull;
import com.jaamsim.math.Mat4d;
import com.jaamsim.math.Vec2d;
import com.jaamsim.math.Vec2dInterner;
import com.jaamsim.math.Vec3d;
import com.jaamsim.math.Vec3dInterner;
import com.jaamsim.math.Vec4d;
import com.jaamsim.math.Vec4dInterner;
import com.jaamsim.render.Action;
import com.jaamsim.render.RenderException;
import com.jaamsim.render.RenderUtils;
import com.jaamsim.render.Renderer;

/**
 * MeshData represents all the data contained in a mesh 3d object. This data is not yet in video ram and can be written back to disk
 * This is not useful for rendering, for that a MeshProto is needed (which is owned by the Renderer)
 * @author matt.chudleigh
 *
 */
public class MeshData {

	private final static boolean LOG_DATA = false;

	public final static int MAX_HULL_ATTEMPTS = 5;
	public final static int MAX_HULL_POINTS = 100;
	public final static int MAX_SUBINST_HULL_POINTS = 30;

	public final static int NO_TRANS = 0;
	public final static int A_ONE_TRANS = 1;
	public final static int RGB_ZERO_TRANS = 2;
	public final static int DIFF_ALPHA_TRANS = 3;

	// Convenient key to track Mesh/Material combinations
	public static class MeshMatKey {
		public final int meshIndex;
		public final int matIndex;
		public MeshMatKey(int meshIndex, int matIndex) {
			this.meshIndex = meshIndex;
			this.matIndex = matIndex;
		}
		@Override
		public boolean equals(Object o) {
			if (!(o instanceof MeshMatKey)) {
				return false;
			}
			MeshMatKey other = (MeshMatKey)o;
			return this.meshIndex == other.meshIndex && this.matIndex == other.matIndex;
		}
		@Override
		public int hashCode() {
			return meshIndex * 21 + matIndex * 61;
		}
	}

	public static class Material {
		public Color4d diffuseColor;
		public Color4d ambientColor;
		public Color4d specColor;
		public double shininess;

		public URI colorTex;
		// TODO Properly relativize this one day
		public String relColorTex; // the 'relative' name for this texture, used by the binary exporter

		public int texIndex;
		public int transType;
		public Color4d transColour;

		public int numUses;

		public int getShaderID() {
			int ret = 0;
			if (colorTex != null) {
				ret += Renderer.DIFF_TEX_FLAG;
			}
			return ret;
		}
	}

	public static class SubMeshData {

		public ArrayList<Vec3d> verts;
		public ArrayList<Vec2d> texCoords;
		public ArrayList<Vec3d> normals;
		public int[] indices;

		public ConvexHull staticHull;
		public AABB localBounds;

		public boolean keepRuntimeData;
		public int numUses;

		public int firstVert;
		public int startInd;
	}

	public static class SubLineData {
		public ArrayList<Vec3d> verts = new ArrayList<>();
		public Color4d diffuseColor;

		public ConvexHull hull;
		public int numUses = 0;

		public int startVert; // Starting index in the full line vertex array
	}

	public static class StaticMeshInstance {
		public int subMeshIndex;
		public int materialIndex;
		public Mat4d transform;
		public Mat4d invTrans;
	}

	public static class StaticMeshBatch {
		public final MeshMatKey key;
		public ArrayList<Mat4d> transform = new ArrayList<>();
		public ArrayList<Mat4d> invTrans = new ArrayList<>();
		public StaticMeshBatch(MeshMatKey key) {
			this.key = key;
		}
	}

	private ArrayList<Vec3d> meshBatchPos;
	private ArrayList<Vec3d> meshBatchNor;
	private ArrayList<Vec2d> meshBatchTex;
	private ArrayList<Integer> meshBatchIndices;

	public static class StaticLineInstance {
		public int lineIndex;
		public Mat4d transform;
		public Mat4d invTrans;
	}

	public static class StaticLineBatch {
		public int lineIndex;
		public ArrayList<Mat4d> instTrans;
		public ArrayList<Color4d> instColor;
		public StaticLineBatch(int i) {
			lineIndex = i;
			instTrans = new ArrayList<>();
			instColor = new ArrayList<>();
		}
	}

	private ArrayList<Vec3d> lineBatchPos;
	private ArrayList<StaticLineBatch> lineBatches;

	public static class TransVal {
		Mat4d transform;
		Mat4d invTrans;
		public TransVal(Mat4d trans, Mat4d inverse) {
			transform = trans;
			invTrans = inverse;
		}
	}

	public static interface Trans {
		public abstract boolean isStatic();
		public abstract TransVal value(ArrayList<Action.Queue> actions);
		public abstract void accept(TransVisitor visitor);
	}

	public static class StaticTrans implements Trans{
		private final Mat4d matrix;
		private final Mat4d inverseMat;
		public StaticTrans(Mat4d mat) {
			matrix = mat;
			inverseMat = matrix.inverse();
		}
		@Override
		public boolean isStatic() {
			return true;
		}
		@Override
		public TransVal value(ArrayList<Action.Queue> actions) {
			return new TransVal(matrix, inverseMat);
		}
		@Override
		public void accept(TransVisitor visitor) {
			visitor.visitStatic(this);

		}
	}

	private static class Act {
		public Mat4d[] matrices;
		public Mat4d[] invMatrices;
		public double[] times;
		public String name;
	}

	public static class AnimTrans implements Trans{
		ArrayList<Act> actions = new ArrayList<>();
		public Mat4d staticMat;
		public Mat4d staticInv;

		public AnimTrans(ArrayList<Act> actions, Mat4d staticMat) {
			this.actions = actions;
			this.staticMat = staticMat;
			this.staticInv = staticMat.inverse();
		}

		public AnimTrans(double[][] times, Mat4d[][] mats, String[] actionNames, Mat4d staticMat) {

			this.staticMat = staticMat;
			staticInv = staticMat.inverse();

			assert(actionNames.length > 0);
			assert(times.length == actionNames.length);
			assert(mats.length == actionNames.length);

			for (int i = 0; i < actionNames.length; ++i) {
				// Build up the actions
				Act act = new Act();
				act.times = times[i];
				act.matrices = mats[i];
				act.name = actionNames[i];
				act.invMatrices = new Mat4d[act.matrices.length];
				for (int j = 0; j < act.matrices.length; ++j) {
					act.invMatrices[j] = act.matrices[j].inverse();
				}

				actions.add(act);
			}
		}

		@Override
		public boolean isStatic() {
			return false;
		}

		@Override
		public void accept(TransVisitor visitor) {
			visitor.visitAnim(this);
		}

		@Override
		public TransVal value(ArrayList<Action.Queue> aqs) {
			if (aqs == null || aqs.size() == 0) {
				return new TransVal(staticMat, staticInv);
			}

			// See which action is applicable
			// For now, if more than one action is applicable on any scene node, priority is determined by the
			// order of the action queues requested.

			Act act = null; // The applicable actions
			double time = 0;
			for (Action.Queue q : aqs) {
				for (Act a : actions) {
					if (q.name.equals(a.name)) {
						act = a;
						time = q.time;
						break;
					}
				}

				if (act != null) {
					break;
				}
			}
			if (act == null) {
				return new TransVal(staticMat, staticInv);
			}
			// The action applies, interpolate the value

			// Check if we are past the ends
			if (time <= act.times[0]) {
				return new TransVal(act.matrices[0], act.invMatrices[0]);
			}
			if (time >= act.times[act.times.length -1]) {
				return new TransVal(act.matrices[act.times.length-1], act.invMatrices[act.times.length-1]);
			}

			// Basic binary search for appropriate segment
			int start = 0;
			int end = act.times.length;
			while ((end - start) > 1) {
				int test = (start + end)/2;
				double samp = act.times[test];

				if (samp == time) { // perfect match
					return new TransVal(act.matrices[test], act.invMatrices[test]);
				}

				if (samp < time) {
					start = test;
				} else {
					end = test;
				}
			}

			assert(end - start == 1);

			// Linearly interpolate on the segment
			double t0 = act.times[start];
			double t1 = act.times[end];
			assert(time >= t0);
			assert(time <= t1);

			double endScale = (time-t0)/(t1-t0);
			double startScale = 1 - endScale;

			Mat4d temp = new Mat4d();

			Mat4d retTrans = new Mat4d(act.matrices[start]);
			retTrans.scale4(startScale);
			temp.set4(act.matrices[end]);
			temp.scale4(endScale);
			retTrans.add4(temp);

			Mat4d retInv = new Mat4d(act.invMatrices[start]);
			retInv.scale4(startScale);
			temp.set4(act.invMatrices[end]);
			temp.scale4(endScale);
			retInv.add4(temp);

//			Mat4d test = new Mat4d();
//			test.mult4(retTrans, retInv);
//			assert(test.nearIdentity());

			return new TransVal(retTrans, retInv);

		}

		// Scan through the list of keys and remove any that are redundant
		public int optimize() {
			int discarded = 0;

			for (Act act : actions) {

				ArrayList<Mat4d> keptMatrices = new ArrayList<>();
				ArrayList<Mat4d> keptInverses = new ArrayList<>();
				ArrayList<Double> keptTimes = new ArrayList<>();

				// Keep the first value
				keptMatrices.add(act.matrices[0]);
				keptInverses.add(act.invMatrices[0]);
				keptTimes.add(act.times[0]);

				for (int i = 0; i < act.matrices.length-2; ++i) {
					double t0 = act.times[i];
					double t1 = act.times[i+2];
					double checkTime = act.times[i+1];

					Mat4d m0 = act.matrices[i];
					Mat4d m1 = act.matrices[i+2];
					Mat4d checkMat = act.matrices[i+1];

					double s0 = (checkTime-t0)/(t1-t0);
					double s1 = 1 - s0;

					Mat4d val = new Mat4d(m0);
					val.scale4(s0);
					Mat4d temp = new Mat4d(m1);
					temp.scale4(s1);
					val.add4(temp);

					if (!val.near4(checkMat)) {
						// this matrix is different enough from the interpolated value that we need it
						keptMatrices.add(act.matrices[i+1]);
						keptInverses.add(act.invMatrices[i+1]);
						keptTimes.add(act.times[i+1]);
					} else {
						++discarded;
					}
				}
				// keep the last value
				keptMatrices.add(act.matrices[act.matrices.length-1]);
				keptInverses.add(act.invMatrices[act.invMatrices.length-1]);
				keptTimes.add(act.times[act.times.length-1]);

				// Finally rewrite the arrays
				act.matrices = new Mat4d[keptMatrices.size()];
				act.invMatrices = new Mat4d[keptMatrices.size()];
				act.times = new double[keptMatrices.size()];
				for (int i = 0; i < act.matrices.length; ++i) {
					act.matrices[i] = keptMatrices.get(i);
					act.invMatrices[i] = keptInverses.get(i);
					act.times[i] = keptTimes.get(i);
				}

			}

			return discarded;
		}
	}

	private interface TransVisitor {
		void visitStatic(StaticTrans trans);
		void visitAnim(AnimTrans trans);
	}

	public static class AnimMeshInstance {
		public AnimMeshInstance(int meshIndex, int materialIndex) {
			this.meshIndex = meshIndex;
			this.materialIndex = materialIndex;

		}
		public int meshIndex;
		public int materialIndex;
		public int nodeIndex;
	}
	public static class AnimLineInstance {
		public AnimLineInstance(int lineIndex) {
			this.lineIndex = lineIndex;

		}
		public int lineIndex;
		public int nodeIndex;
	}

	public static class TreeNode {
		public Trans trans;
		public ArrayList<TreeNode> children = new ArrayList<>();
		public ArrayList<AnimMeshInstance> meshInstances = new ArrayList<>();
		public ArrayList<AnimLineInstance> lineInstances = new ArrayList<>();
		public int nodeIndex;

		// Return a list of siblings that are equivalent to this being children of this node.
		// Part of constant coalescing.
		public ArrayList<TreeNode> getEquivSiblings() {
			if (!(trans.isStatic())) {
				return null;
			}
			ArrayList<TreeNode> ret = new ArrayList<>();
			Mat4d thisMat = trans.value(null).transform;
			Mat4d thisInvMat = trans.value(null).invTrans;
			for (TreeNode child : children) {
				if (child.trans.isStatic()) {
					Mat4d childMat = child.trans.value(null).transform;
					Mat4d sibMat = new Mat4d();
					sibMat.mult4(thisMat, childMat);
					child.trans = new StaticTrans(sibMat);
					ret.add(child);
				} else {
					// Merge the static parent into the animated child
					AnimTrans at = (AnimTrans)child.trans;
					for (Act act : at.actions) {
						for (int matInd = 0; matInd < act.matrices.length; ++matInd) {
							act.matrices[matInd].mult4(thisMat, act.matrices[matInd]);
							act.invMatrices[matInd].mult4(act.invMatrices[matInd], thisInvMat);
						}
					}

					at.staticMat.mult4(thisMat, at.staticMat);
					at.staticInv.mult4(at.staticInv, thisInvMat);
					ret.add(child);
				}
			}
			children.clear();
			return ret;
		}
	}

	private static class TreeWalker {
		public void onNode(Mat4d trans, Mat4d invTrans, TreeNode node) {}
		public void onMesh(Mat4d trans, Mat4d invTrans, TreeNode node, AnimMeshInstance inst) {}
		public void onLine(Mat4d trans, Mat4d invTrans, TreeNode node, AnimLineInstance inst) {}
	}

	public static class Pose {
		public Mat4d[] transforms;
		public Mat4d[] invTransforms;
	}

	public static class Texture {
		public URI texURI;
		public boolean withAlpha;
	}

	private final ArrayList<SubMeshData> _subMeshesData = new ArrayList<>();
	private final ArrayList<SubLineData> _subLinesData = new ArrayList<>();
	private final ArrayList<Material> _materials = new ArrayList<>();
	private final ArrayList<Texture> _textures = new ArrayList<>();

	private final ArrayList<StaticMeshInstance> _staticMeshInstances = new ArrayList<>();
	private final ArrayList<StaticLineInstance> _staticLineInstances = new ArrayList<>();

	private final HashMap<MeshMatKey, StaticMeshBatch> _staticBatches = new HashMap<>();

	private final ArrayList<AnimMeshInstance> _animMeshInstances = new ArrayList<>();
	private final ArrayList<AnimLineInstance> _animLineInstances = new ArrayList<>();

	private TreeNode treeRoot;
	private int numTreeNodes;

	private String source;

	private ConvexHull _staticHull;
	// The AABB of this mesh with no transform applied
	private AABB _defaultBounds;

	private boolean _anyTransparent = false;

	private ArrayList<Action.Description> _actionDesc;

	private Vec2dInterner v2Interner = new Vec2dInterner();
	private Vec3dInterner v3Interner = new Vec3dInterner();
	private Vec4dInterner v4Interner = new Vec4dInterner();

	public boolean keepRuntimeData;

	public MeshData(boolean keepRuntimeData) {
		this.keepRuntimeData = keepRuntimeData;
	}

	public void setSource(String source) {
		this.source = source;
	}

	public void addStaticMeshInstance(int meshIndex, int matIndex, Mat4d mat) {
		Mat4d trans = new Mat4d(mat);
		StaticMeshInstance inst = new StaticMeshInstance();
		inst.subMeshIndex = meshIndex;
		inst.materialIndex = matIndex;
		inst.transform = trans;

		Mat4d invTrans = trans.inverse();
		inst.invTrans = invTrans;
		_staticMeshInstances.add(inst);

		// Add to the batches as well if it is opaque
		Material material = _materials.get(matIndex);
		if (material.transType == NO_TRANS) {
			MeshMatKey key = new MeshMatKey(meshIndex, matIndex);
			StaticMeshBatch batch = _staticBatches.get(key);
			if (batch == null) {
				batch = new StaticMeshBatch(key);
				_staticBatches.put(key, batch);
			}
			batch.transform.add(trans);
			batch.invTrans.add(invTrans);
		}
	}

	public void setTree(TreeNode rootNode) {
		treeRoot = rootNode;
	}

	public void addStaticLineInstance(int lineIndex, Mat4d mat) {
		Mat4d trans = new Mat4d(mat);
		StaticLineInstance inst = new StaticLineInstance();
		inst.lineIndex = lineIndex;
		inst.transform = trans;
		inst.invTrans = trans.inverse();

		_subLinesData.get(lineIndex).numUses++;

		_staticLineInstances.add(inst);
	}

	public void addMaterial(URI colorTex,
	                        String relTexString,
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
		if (diffuseColor == null) {
			mat.diffuseColor = new Color4d(0.2, 0.2, 0.2, 1.0);
		}
		mat.ambientColor = ambientColor;
		mat.specColor = specColor;
		mat.shininess = shininess;
		mat.colorTex = colorTex;
		mat.relColorTex = relTexString;

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

		boolean isTrans = false;
		if (transType != NO_TRANS) {
			isTrans = true;
			_anyTransparent = true;
		}
		if (colorTex != null) {
			int texIndex = _textures.indexOf(colorTex);
			if (texIndex == -1) {
				Texture tex = new Texture();
				tex.texURI = colorTex;
				_textures.add(tex);
				texIndex = _textures.size()-1;
			}
			mat.texIndex = texIndex;
			Texture tex = _textures.get(texIndex);
			tex.withAlpha = tex.withAlpha || isTrans;
		} else {
			mat.texIndex = -1;
		}
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

		if (vertices.size() < 3) {
			vertices = new ArrayList<>();
			indices = new int[0];
		}

		SubMeshData sub = new SubMeshData();
		sub.keepRuntimeData = keepRuntimeData;
		_subMeshesData.add(sub);

		int[] goodIndices = removeDegenerateTriangles(vertices, indices);
		sub.indices = goodIndices;

		assert((sub.indices.length % 3) == 0);

		// Assume if there is one tex coordinate, there will be all of them
		boolean hasTexCoords = vertices.size() > 0 && vertices.get(0).getTexCoord() != null;

		sub.verts = new ArrayList<>(vertices.size());
		sub.normals = new ArrayList<>(vertices.size());

		if (hasTexCoords) {
			sub.texCoords = new ArrayList<>(vertices.size());
		}
		for (Vertex v : vertices) {
			sub.verts.add(v3Interner.intern(v.getPos()));
			sub.normals.add(v3Interner.intern(v.getNormal()));
			if (hasTexCoords) {
				sub.texCoords.add(v2Interner.intern(v.getTexCoord()));
			}
		}

		sub.staticHull = ConvexHull.TryBuildHull(sub.verts, MAX_HULL_ATTEMPTS, MAX_SUBINST_HULL_POINTS, v3Interner);
		sub.localBounds = sub.staticHull.getAABB(new Mat4d());
	}

	public void addSubLine(Vec3d[] vertices,
			Color4d diffuseColor) {

		SubLineData sub = new SubLineData();
		sub.diffuseColor = diffuseColor;
		if (sub.diffuseColor == null) {
			sub.diffuseColor = new Color4d(); // Default to black
		}
		_subLinesData.add(sub);

		assert((vertices.length % 2) == 0);

		for (Vec3d v : vertices) {
			sub.verts.add(v3Interner.intern(v));
		}

		sub.hull = ConvexHull.TryBuildHull(sub.verts, MAX_HULL_ATTEMPTS, MAX_SUBINST_HULL_POINTS, v3Interner);
	}

	public boolean hasTransparent() {
		return _anyTransparent;
	}

	private void scanTreeForStaticEntries(TreeNode node, Mat4d trans) {
		TransVal val = node.trans.value(null);

		Mat4d transform = new Mat4d(trans);
		transform.mult4(val.transform);

		for (AnimMeshInstance ti : node.meshInstances) {
			addStaticMeshInstance(ti.meshIndex, ti.materialIndex, transform);
		}
		node.meshInstances.clear();

		for (AnimLineInstance li : node.lineInstances) {
			addStaticLineInstance(li.lineIndex, transform);
		}
		node.lineInstances.clear();


		for (TreeNode child : node.children) {
			if (child.trans.isStatic()) {
				scanTreeForStaticEntries(child, transform);
			}
		}
		// Remove children that are now empty
		ArrayList<TreeNode> newChildren = new ArrayList<>();
		for (TreeNode child : node.children) {
			if (child.children.size() != 0 || !child.trans.isStatic()) {
				// Keep children that still have sub nodes or are not static
				newChildren.add(child);
			}
		}
		node.children = newChildren;
	}

	private void walkTree(TreeWalker walker, TreeNode node, Mat4d trans, Mat4d inverse, ArrayList<Action.Queue> actions) {
		Mat4d newTrans = new Mat4d();
		Mat4d newInv = new Mat4d();
		TransVal tv = node.trans.value(actions);
		newTrans.mult4(trans, tv.transform);
		newInv.mult4(tv.invTrans, inverse);

		walker.onNode(newTrans, newInv, node);

		for (AnimMeshInstance ti : node.meshInstances) {
			walker.onMesh(newTrans, newInv, node, ti);
		}

		for (AnimLineInstance li : node.lineInstances) {
			walker.onLine(newTrans, newInv, node, li);
		}
		for (TreeNode child : node.children) {
			walkTree(walker, child, newTrans, newInv, actions);
		}

	}

	private boolean optimizeTree(TreeNode node) {
		boolean changed = false;
		for (TreeNode child : node.children) {
			changed = changed || optimizeTree(child);
		}

		ArrayList<TreeNode> newChildren = new ArrayList<>();
		int i = 0;
		while (i < node.children.size()) {
			TreeNode child = node.children.get(i);
			ArrayList<TreeNode> newCs = child.getEquivSiblings();
			if (newCs != null && newCs.size() != 0) {
				changed = true;
				newChildren.addAll(newCs);
			}
			if (  child.children.size() == 0 &&
			      child.meshInstances.size() == 0 &&
			      child.lineInstances.size() == 0) {
				node.children.remove(i);
				changed = true;
			} else {
				i++;
			}
		}
		node.children.addAll(newChildren);

		// Now scan all children to see if there are any siblings with identical transforms that can be merged
		for (int indA = 0; indA < node.children.size()-1; ++indA) {
			int indB = indA+1;
			while (indB < node.children.size()) {
				TreeNode nodeA = node.children.get(indA);
				TreeNode nodeB = node.children.get(indB);
				if (nodeA.trans.isStatic() && nodeB.trans.isStatic()) {
					Mat4d aMat = nodeA.trans.value(null).transform;
					Mat4d bMat = nodeB.trans.value(null).transform;
					if (aMat.near4(bMat)) {
						nodeA.children.addAll(nodeB.children);
						nodeA.meshInstances.addAll(nodeB.meshInstances);
						nodeA.lineInstances.addAll(nodeB.lineInstances);

						node.children.remove(indB);
						changed = true;
						continue;
					}
				}
				++indB;
			}
		}
		return changed;
	}

	// Build up a batchable (via the DEBUB_BATCH shader) array of all static line information
	private void generateLineBatches() {
		// Build up a complete list of all line vertices
		lineBatchPos = new ArrayList<Vec3d>();
		for (SubLineData ld: _subLinesData) {
			ld.startVert = lineBatchPos.size();
			lineBatchPos.addAll(ld.verts);
		}

		lineBatches = new ArrayList<StaticLineBatch>();
		// Create batches
		for (int i = 0; i < _subLinesData.size(); ++i) {
			lineBatches.add(new StaticLineBatch(i));
		}
		for (int i = 0; i < _staticLineInstances.size(); ++i) {
			StaticLineInstance inst = _staticLineInstances.get(i);
			StaticLineBatch b = lineBatches.get(inst.lineIndex);
			b.instTrans.add(inst.transform);
			b.instColor.add(_subLinesData.get(inst.lineIndex).diffuseColor);
		}
	}

	private void generateMeshBatches() {
		meshBatchPos = new ArrayList<>();
		meshBatchNor = new ArrayList<>();
		meshBatchTex = new ArrayList<>();
		meshBatchIndices = new ArrayList<>();

		for (SubMeshData sm: _subMeshesData) {
			sm.firstVert = meshBatchPos.size();
			sm.startInd = meshBatchIndices.size();

			meshBatchPos.addAll(sm.verts);
			meshBatchNor.addAll(sm.normals);
			if (sm.texCoords != null) {
				meshBatchTex.addAll(sm.texCoords);
			} else {
				Vec2d defTexCoord = new Vec2d(0.0, 0.0);
				for (int i = 0; i < sm.verts.size(); ++i) {
					meshBatchTex.add(defTexCoord);
				}
			}

			for (int ind: sm.indices) {
				meshBatchIndices.add(ind);
			}
		}

	}

	/**
	 * Builds the convex hull of the current mesh based on all the existing sub meshes.
	 */
	public void finalizeData() {
		// Scan the tree to see if any animated transforms are effectively static
		class StaticWalker extends TreeWalker {
			public int numMatricesRemoved = 0;
			public int numMatricesRemaining = 0;
			@Override
			public void onNode(Mat4d trans, Mat4d invTrans, TreeNode node) {

				if (node.trans instanceof AnimTrans) {
					AnimTrans at = (AnimTrans)node.trans;
					numMatricesRemoved += at.optimize();
					for (Act act : at.actions) {
						numMatricesRemaining += act.matrices.length;
					}

					boolean isSame = true;
					for (Act act : at.actions) {
						for (int i = 0; i < act.matrices.length; ++i) {
							isSame = isSame && at.staticMat.near4(act.matrices[i]);
						}
						if (isSame) {
							// All values are effectively the same
							node.trans = new StaticTrans(at.staticMat);
						}
					}
				}
			}
		}
		StaticWalker staticWalker = new StaticWalker();

		long statStart = System.nanoTime();
		walkTree(staticWalker, treeRoot, new Mat4d(), new Mat4d(), null);
		long statEnd = System.nanoTime();
		double statMS = (statEnd - statStart) / 1000000.0;

		// Annoying way to disable debugging while avoiding compiler warnings
		boolean printDebug = false;
		if (printDebug) {
			System.out.printf("Matrices removed: %d remaining: %d in %fms\n", staticWalker.numMatricesRemoved, staticWalker.numMatricesRemaining, statMS);
		}
		// Pull all static information out of the root tree
		if (treeRoot != null && treeRoot.trans.isStatic()) {
			scanTreeForStaticEntries(treeRoot, new Mat4d());
		}

		final ArrayList<Vec3d> totalHullPoints = new ArrayList<>();
		// Collect all the points from the hulls of the individual sub meshes
		for (StaticMeshInstance subInst : _staticMeshInstances) {

			SubMeshData sm = _subMeshesData.get(subInst.subMeshIndex);
			List<Vec3d> pointsRef = sm.staticHull.getVertices();
			List<Vec3d> subPoints = RenderUtils.transformPointsWithTrans(subInst.transform, pointsRef);

			sm.numUses++;

			Material mat = _materials.get(subInst.materialIndex);
			mat.numUses++;

			totalHullPoints.addAll(subPoints);
		}
		// And the lines
		for (StaticLineInstance subInst : _staticLineInstances) {

			List<Vec3d> pointsRef = _subLinesData.get(subInst.lineIndex).hull.getVertices();
			List<Vec3d> subPoints = RenderUtils.transformPointsWithTrans(subInst.transform, pointsRef);

			totalHullPoints.addAll(subPoints);
		}

		generateMeshBatches();
		generateLineBatches();

		// Now scan the non-static part of the tree
		TreeWalker walker = new TreeWalker() {
			public int nextIndex = 0;

			@Override
			public void onNode(Mat4d trans, Mat4d InvTrans, TreeNode node) {
				node.nodeIndex = nextIndex++;

				numTreeNodes = nextIndex;
			}

			@Override
			public void onMesh(Mat4d trans, Mat4d InvTrans, TreeNode node, AnimMeshInstance inst) {
				List<Vec3d> pointsRef = _subMeshesData.get(inst.meshIndex).staticHull.getVertices();
				List<Vec3d> subPoints = RenderUtils.transformPointsWithTrans(trans, pointsRef);

				totalHullPoints.addAll(subPoints);

				inst.nodeIndex = node.nodeIndex;
				_animMeshInstances.add(inst);
			}

			@Override
			public void onLine(Mat4d trans, Mat4d InvTrans, TreeNode node, AnimLineInstance inst) {
				List<Vec3d> pointsRef = _subLinesData.get(inst.lineIndex).hull.getVertices();
				List<Vec3d> subPoints = RenderUtils.transformPointsWithTrans(trans, pointsRef);

				totalHullPoints.addAll(subPoints);
				inst.nodeIndex = node.nodeIndex;
				_animLineInstances.add(inst);
			}
		};

		long optStart = System.nanoTime();
		boolean changed = true;
		int numPasses = 0;
		while (changed) {
			changed = optimizeTree(treeRoot);
			++numPasses;
		}
		long optEnd = System.nanoTime();
		double optMS = (optEnd - optStart) / 1000000.0;

		walkTree(walker, treeRoot, new Mat4d(), new Mat4d(), null);
		int optimTreeNodes = numTreeNodes;

		if (printDebug) {
			System.out.printf("Tree optimization - nodes: %d, passes: %d in %fms\n", optimTreeNodes, numPasses, optMS);
		}

		_staticHull = ConvexHull.TryBuildHull(totalHullPoints, MAX_HULL_ATTEMPTS, MAX_HULL_POINTS, v3Interner);
		_defaultBounds = _staticHull.getAABB(new Mat4d());

		populateActionList();

		if (!keepRuntimeData) {
			v2Interner = null; // Drop ref to the interner to free memory
			v3Interner = null; // Drop ref to the interner to free memory
			v4Interner = null; // Drop ref to the interner to free memory
		}
		if (LOG_DATA) {
			logUses();
		}
	}

	private void addToHist(HashMap<Integer, Integer> hist, int val) {
		Integer histVal = hist.get(val);
		if (histVal == null) {
			histVal = new Integer(0);
		}
		histVal += 1;
		hist.put(val, histVal);

	}
	private void printHist(HashMap<Integer, Integer> hist, int maxVal) {
		for (int i = 0; i <= maxVal; ++i) {
			Integer histVal = hist.get(i);
			if (histVal != null && histVal != 0) {
				System.out.printf("%d -> %d\n", i, histVal);
			}
		}
	}

	private void logUses() {
		HashMap<Integer, Integer> instHist = new HashMap<>();
		int maxInst = 0;
		int totalInsts = 0;

		for(SubMeshData sm: _subMeshesData) {
			int numUses = sm.numUses;

			addToHist(instHist, numUses);
			totalInsts += numUses;

			maxInst = Math.max(maxInst, numUses);
		}
		String s = source != null ? source : "<unknown>";
		System.out.printf("Uses for source: %s\n", s);
		System.out.printf("Total meshes: %d\n", _subMeshesData.size());
		System.out.printf("Total insts: %d\n", totalInsts);

		printHist(instHist, maxInst);

		// Log the batch uses
		HashMap<Integer, Integer> batchHist = new HashMap<>();
		int maxBatch = 0;
		for (MeshMatKey k : _staticBatches.keySet()) {
			StaticMeshBatch b = _staticBatches.get(k);
			int batchSize = b.transform.size();

			addToHist(batchHist, batchSize);
			maxBatch = Math.max(maxInst, batchSize);

		}

		System.out.printf("BatchHistogram:\n");
		printHist(batchHist, maxBatch);

		if (_subLinesData.size() > 0) {
			System.out.printf("Lines:\n");

			HashMap<Integer, Integer> lineHist = new HashMap<>();
			int maxLineInst = 0;
			int totalLineInsts = 0;

			for(SubLineData sl: _subLinesData) {
				int numUses = sl.numUses;

				addToHist(lineHist, numUses);
				totalLineInsts += numUses;

				maxLineInst = Math.max(maxLineInst, numUses);
			}
			System.out.printf("Total lines: %d\n", _subLinesData.size());
			System.out.printf("Total insts: %d\n", totalLineInsts);

			printHist(lineHist, maxLineInst);
		}
	}

	private void populateActionList() {
		final HashMap<String, Double> actionMap = new HashMap<>();
		class ActionWalker extends TreeWalker {
			@Override
			public void onNode(Mat4d trans, Mat4d invTrans, TreeNode node) {

				if (node.trans instanceof AnimTrans) {
					AnimTrans at = (AnimTrans)node.trans;

					for (Act act : at.actions) {
						Double existingTime = actionMap.get(act.name);
						double lastTime = act.times[act.times.length-1];
						if (existingTime == null || lastTime > existingTime) {
							actionMap.put(act.name, lastTime);
						}
					}
				}
			}
		}

		ActionWalker actionWalker = new ActionWalker();
		walkTree(actionWalker, treeRoot, new Mat4d(), new Mat4d(), null);

		_actionDesc = new ArrayList<>();
		for (Map.Entry<String, Double> entry : actionMap.entrySet()) {
			Action.Description desc = new Action.Description();
			desc.name = entry.getKey();
			desc.duration = entry.getValue();
			_actionDesc.add(desc);
		}


	}

	public ConvexHull getHull(Pose pose) {
		if (pose == null)
			return _staticHull;

		// Otherwise, we need to calculate a new hull
		ArrayList<Vec3d> hullPoints = new ArrayList<>();

		for (StaticMeshInstance inst : _staticMeshInstances) {

			List<Vec3d> pointsRef = _subMeshesData.get(inst.subMeshIndex).staticHull.getVertices();
			List<Vec3d> subPoints = RenderUtils.transformPointsWithTrans(inst.transform, pointsRef);

			hullPoints.addAll(subPoints);
		}

		for (AnimMeshInstance inst : _animMeshInstances) {
			Mat4d animatedTransform = pose.transforms[inst.nodeIndex];

			List<Vec3d> pointsRef = _subMeshesData.get(inst.meshIndex).staticHull.getVertices();
			List<Vec3d> subPoints = RenderUtils.transformPointsWithTrans(animatedTransform, pointsRef);

			hullPoints.addAll(subPoints);

		}

		// And the lines
		for (StaticLineInstance inst : _staticLineInstances) {

			List<Vec3d> pointsRef = _subLinesData.get(inst.lineIndex).hull.getVertices();
			List<Vec3d> subPoints = RenderUtils.transformPointsWithTrans(inst.transform, pointsRef);

			hullPoints.addAll(subPoints);
		}

		for (AnimLineInstance inst : _animLineInstances) {

			Mat4d animatedTransform = pose.transforms[inst.nodeIndex];

			List<Vec3d> pointsRef = _subLinesData.get(inst.lineIndex).hull.getVertices();
			List<Vec3d> subPoints = RenderUtils.transformPointsWithTrans(animatedTransform, pointsRef);

			hullPoints.addAll(subPoints);
		}

		ConvexHull ret = ConvexHull.TryBuildHull(hullPoints, MAX_HULL_ATTEMPTS, MAX_HULL_POINTS, null);
		return ret;
	}

	public Pose getPose(ArrayList<Action.Queue> actions) {

		final Pose ret = new Pose();
		ret.transforms = new Mat4d[numTreeNodes];
		ret.invTransforms = new Mat4d[numTreeNodes];

		TreeWalker walker = new TreeWalker() {
			@Override
			public void onNode(Mat4d trans, Mat4d invTrans, TreeNode node) {
				ret.transforms[node.nodeIndex] = trans;
				ret.invTransforms[node.nodeIndex] = invTrans;

//				// Debug
//				Mat4d test = new Mat4d();
//				test.mult4(trans, invTrans);
//				assert(test.nearIdentityThresh3(0.01));
//
//				test.mult4(invTrans, trans);
//				assert(test.nearIdentityThresh3(0.01));

			}
		};

		walkTree(walker, treeRoot, new Mat4d(), new Mat4d(), actions);

		return ret;

	}

	public AABB getDefaultBounds() {
		return _defaultBounds;
	}

	public ArrayList<Action.Description> getActionDescriptions() {
		return _actionDesc;
	}

	public ArrayList<SubMeshData> getSubMeshData() {
		return _subMeshesData;
	}
	public ArrayList<SubLineData> getSubLineData() {
		return _subLinesData;
	}

	public ArrayList<StaticMeshInstance> getStaticMeshInstances() {
		return _staticMeshInstances;
	}
	public HashMap<MeshMatKey,StaticMeshBatch> getStaticMeshBatches() {
		return _staticBatches;
	}
	public ArrayList<StaticLineInstance> getStaticLineInstances() {
		return _staticLineInstances;
	}

	public ArrayList<AnimMeshInstance> getAnimMeshInstances() {
		return _animMeshInstances;
	}
	public ArrayList<AnimLineInstance> getAnimLineInstances() {
		return _animLineInstances;
	}
	public ArrayList<Texture> getTextures() {
		return _textures;
	}

	public ArrayList<Material> getMaterials() {
		return _materials;
	}

	public ArrayList<Vec3d> getLinePosArray() {
		return lineBatchPos;
	}

	public ArrayList<Vec3d> getMeshPosArray() {
		return meshBatchPos;
	}
	public ArrayList<Vec3d> getMeshNorArray() {
		return meshBatchNor;
	}
	public ArrayList<Vec2d> getMeshTexArray() {
		return meshBatchTex;
	}
	public ArrayList<Integer> getMeshIndexArray() {
		return meshBatchIndices;
	}

	public ArrayList<StaticLineBatch> getLineBatches() {
		return lineBatches;
	}

	public int getNumTriangles() {
		int numTriangles = 0;
		for (StaticMeshInstance inst : _staticMeshInstances) {
			SubMeshData data = _subMeshesData.get(inst.subMeshIndex);
			numTriangles += data.indices.length / 3;
		}
		for (AnimMeshInstance inst : _animMeshInstances) {
			SubMeshData data = _subMeshesData.get(inst.meshIndex);
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
		return _staticMeshInstances.size() + _staticLineInstances.size() +
				_animMeshInstances.size() + _animLineInstances.size();
	}
	public int getNumSubMeshes() {
		return _subMeshesData.size() + _subLinesData.size();
	}

	/**
	 * Write the color as 4 bytes, RGBA
	 * @param col
	 * @param b
	 */
	private void writeColorToBlock(Color4d col, DataBlock b) {
		if (col == null) {
			b.writeInt(0);
			return;
		}

		b.writeByte((byte)(col.r*255));
		b.writeByte((byte)(col.g*255));
		b.writeByte((byte)(col.b*255));
		b.writeByte((byte)(col.a*255));
	}

	private Color4d readColorFromBlock(DataBlock block) {
		int r = block.readByte() & 0x000000FF;
		int g = block.readByte() & 0x000000FF;
		int b = block.readByte() & 0x000000FF;
		int a = block.readByte() & 0x000000FF;
		return new Color4d(r/255.0, g/255.0, b/255.0, a/255.0);
	}

	/**
	 * Initialize a MeshData from a DataBlock, throws a RenderException if things go sideways.
	 * @param topBlock
	 */
	public MeshData(boolean keepRuntimeData, DataBlock topBlock, URL contextURL) {

		this.keepRuntimeData = keepRuntimeData;

		DataBlock vectorsBlock = topBlock.findChildByName("VectorLib");
		if (vectorsBlock == null) throw new RenderException("Binary MeshData: Missing Vectors Library");

		DataBlock v2s = vectorsBlock.findChildByName("Vec2ds");
		DataBlock v3s = vectorsBlock.findChildByName("Vec3ds");
		DataBlock v4s = vectorsBlock.findChildByName("Vec4ds");

		int vec2dSize = (v2s != null) ? v2s.getDataSize() / 16 : 0;
		Vec2d[] vec2ds = new Vec2d[vec2dSize];
		for (int i = 0; i < vec2dSize; ++i) {
			vec2ds[i] = new Vec2d(v2s.readDouble(), v2s.readDouble());
		}

		int vec3dSize = (v3s != null) ? v3s.getDataSize() / 24 : 0;
		Vec3d[] vec3ds = new Vec3d[vec3dSize];
		for (int i = 0; i < vec3dSize; ++i) {
			vec3ds[i] = new Vec3d(v3s.readDouble(), v3s.readDouble(), v3s.readDouble());
		}

		int vec4dSize = (v4s != null) ? v4s.getDataSize() / 32 : 0;
		Vec4d[] vec4ds = new Vec4d[vec4dSize];
		for (int i = 0; i < vec4dSize; ++i) {
			vec4ds[i] = new Vec4d(v4s.readDouble(), v4s.readDouble(), v4s.readDouble(), v4s.readDouble());
		}

		// Build up the sub mesh data
		DataBlock subMeshesBlock = topBlock.findChildByName("SubMeshes");
		for (DataBlock subMeshBlock : subMeshesBlock.getChildren()) {
			if (!subMeshBlock.getName().equals("SubMeshData")) {
				continue;
			}
			SubMeshData subData = new SubMeshData();
			subData.keepRuntimeData = keepRuntimeData;

			DataBlock vertBlock = subMeshBlock.findChildByName("Vertices");
			if (vertBlock == null) throw new RenderException("Missing vertices in submesh");
			subData.verts = new ArrayList<>(vertBlock.getDataSize() / 4);
			for (int i = 0; i < vertBlock.getDataSize() / 4; ++i) {
				int vertInd = vertBlock.readInt();
				subData.verts.add(vec3ds[vertInd]);
			}

			DataBlock normBlock = subMeshBlock.findChildByName("Normals");
			if (normBlock == null) throw new RenderException("Missing normals in submesh");
			subData.normals = new ArrayList<>(normBlock.getDataSize() / 4);
			for (int i = 0; i < normBlock.getDataSize() / 4; ++i) {
				int normInd = normBlock.readInt();
				subData.normals.add(vec3ds[normInd]);
			}

			DataBlock texCoordBlock = subMeshBlock.findChildByName("TexCoords");
			if (texCoordBlock != null) {
				subData.texCoords = new ArrayList<>(texCoordBlock.getDataSize() / 4);
				for (int i = 0; i < texCoordBlock.getDataSize() / 4; ++i) {
					int texInd = texCoordBlock.readInt();
					subData.texCoords.add(vec2ds[texInd]);
				}
			}

			DataBlock indicesBlock = subMeshBlock.findChildByName("Indices");
			if (indicesBlock == null) throw new RenderException("Missing indices in submesh");
			subData.indices = new int[indicesBlock.getDataSize() / 4];
			for (int i = 0; i < indicesBlock.getDataSize() / 4; ++i) {
				subData.indices[i] = indicesBlock.readInt();
			}

			DataBlock hullBlock = subMeshBlock.findChildByName("ConvexHull");
			if (hullBlock == null) throw new RenderException("Missing hull in submesh");
			subData.staticHull = ConvexHull.fromDataBlock(hullBlock, vec3ds);
			subData.localBounds = subData.staticHull.getAABB(new Mat4d());

			_subMeshesData.add(subData);
		}

		DataBlock subLinesBlock = topBlock.findChildByName("SubLines");
		for (DataBlock subLineBlock : subLinesBlock.getChildren()) {
			if (!subLineBlock.getName().equals("SubLineData")) {
				continue;
			}
			SubLineData subLine = new SubLineData();

			DataBlock vertBlock = subLineBlock.findChildByName("Vertices");
			if (vertBlock == null) throw new RenderException("Missing vertices in subline");
			subLine.verts = new ArrayList<>(vertBlock.getDataSize() / 4);
			for (int i = 0; i < vertBlock.getDataSize() / 4; ++i) {
				int vertInd = vertBlock.readInt();
				subLine.verts.add(vec3ds[vertInd]);
			}

			DataBlock colorBlock = subLineBlock.findChildByName("Color");
			if (colorBlock == null) throw new RenderException("Missing color in subline");
			subLine.diffuseColor = readColorFromBlock(colorBlock);

			subLine.hull = ConvexHull.TryBuildHull(subLine.verts, MAX_HULL_ATTEMPTS, MAX_HULL_POINTS, v3Interner);

			_subLinesData.add(subLine);
		}

		// Add Materials
		DataBlock matsBlock = topBlock.findChildByName("Materials");
		if (matsBlock == null) throw new RenderException("Missing materials block");
		for (DataBlock matBlock : matsBlock.getChildren()) {
			if (!matBlock.getName().equals("Material"))
				continue;

			DataBlock colorBlock = matBlock.findChildByName("DifAmbSpecShinTrans");
			if (colorBlock == null) throw new RenderException("Missing color block in material");

			Color4d diffuse = readColorFromBlock(colorBlock);
			Color4d ambient = readColorFromBlock(colorBlock);
			Color4d specular = readColorFromBlock(colorBlock);
			float shininess = colorBlock.readFloat();
			Color4d transColor = readColorFromBlock(colorBlock);
			int transType = colorBlock.readInt();

			URI texURI = null;
			String texString = null;
			DataBlock textureBlock = matBlock.findChildByName("DiffuseTexture");
			if (textureBlock != null) {
				texString = textureBlock.readString();
				try {
					texURI = new URL(contextURL, texString).toURI();
				} catch (MalformedURLException ex){
					throw new RenderException(String.format("Error with texture URL: %s", texString));
				} catch (URISyntaxException e) {
					throw new RenderException(String.format("Error with texture URL: %s", texString));
				}
			}

			addMaterial(texURI, texString, diffuse, ambient, specular, shininess, transType, transColor);
		}

		// Now for the instances
		DataBlock subMeshInstBlock = topBlock.findChildByName("SubMeshInstances");
		if (subMeshInstBlock == null) throw new RenderException("Missing mesh instance block");
		for (DataBlock subInstBlock : subMeshInstBlock.getChildren()) {
			if (!subInstBlock.getName().equals("StaticSubInstance"))
				continue;

			DataBlock indexBlock = subInstBlock.findChildByName("Indices");
			if (indexBlock == null) throw new RenderException("Missing sub instance indices");
			int meshIndex = indexBlock.readInt();
			int matIndex = indexBlock.readInt();

			DataBlock transBlock = subInstBlock.findChildByName("Transform");
			if (transBlock == null) throw new RenderException("Missing sub instance transform");
			double[] cmMat = new double[16];
			for (int i = 0; i < 16; ++i) {
				cmMat[i] = transBlock.readDouble();
			}
			Mat4d trans = new Mat4d(cmMat);
			trans.transpose4();

			addStaticMeshInstance(meshIndex, matIndex, trans);
		}

		DataBlock subLineInstBlock = topBlock.findChildByName("SubLineInstances");
		if (subLineInstBlock == null) throw new RenderException("Missing line instance block");
		for (DataBlock subInstBlock : subLineInstBlock.getChildren()) {

			DataBlock indexBlock = subInstBlock.findChildByName("Indices");
			if (indexBlock == null) throw new RenderException("Missing sub instance indices");
			int lineIndex = indexBlock.readInt();

			DataBlock transBlock = subInstBlock.findChildByName("Transform");
			if (transBlock == null) throw new RenderException("Missing sub instance transform");
			double[] cmMat = new double[16];
			for (int i = 0; i < 16; ++i) {
				cmMat[i] = transBlock.readDouble();
			}
			Mat4d trans = new Mat4d(cmMat);
			trans.transpose4();

			addStaticLineInstance(lineIndex, trans);
		}

		DataBlock animTreeBlock = topBlock.findChildByName("AnimTree");

		ArrayList<TreeNode> nodes = new ArrayList<>();
		if (animTreeBlock != null) {
			ArrayList<int[]> childIndices = new ArrayList<>(); // Store child indices until after all nodes have been created

			int nodeIndex = 0;
			for (DataBlock child : animTreeBlock.getChildren()) {
				int numChildren = child.readInt();
				int[] childInds = new int[numChildren];
				for (int i = 0; i < numChildren; ++i) {
					childInds[i] = child.readInt();
				}
				childIndices.add(childInds);

				TreeNode node = new TreeNode();
				node.nodeIndex = nodeIndex;
				nodes.add(node);
				int numSubMeshes = child.readInt();
				for (int i = 0; i < numSubMeshes; ++i) {
					int meshInd = child.readInt();
					int matInd = child.readInt();
					AnimMeshInstance ami = new AnimMeshInstance(meshInd, matInd);
					_animMeshInstances.add(ami);
					ami.nodeIndex = nodeIndex;
					node.meshInstances.add(ami);
				}

				int numSubLines = child.readInt();
				for (int i = 0; i < numSubLines; ++i) {
					int lineIndex = child.readInt();
					AnimLineInstance ali = new AnimLineInstance(lineIndex);
					_animLineInstances.add(ali);
					ali.nodeIndex = nodeIndex;
					node.lineInstances.add(ali);
				}

				DataBlock transBlock = child.findChildByName("StaticTrans");
				if (transBlock != null) {
					Mat4d transMat = transBlock.readMat4d();
					StaticTrans st = new StaticTrans(transMat);
					node.trans = st;
				} else {
					transBlock = child.findChildByName("AnimTrans");
					if (transBlock == null)
						throw new RenderException("TreeNode missing transform block");

					Mat4d staticTrans = transBlock.readMat4d();
					ArrayList<Act> actions = new ArrayList<>();
					for (DataBlock actBlock : transBlock.getChildren()) {
						if (!actBlock.getName().equals("Action"))
							continue;

						actions.add(readActionBlock(actBlock));
					}

					node.trans = new AnimTrans(actions, staticTrans);
				}

				nodeIndex++;
			}

			// Now that all nodes exist, populate child references from indices
			for (int i = 0; i < nodes.size(); ++i) {
				TreeNode node = nodes.get(i);
				int[] childInds = childIndices.get(i);
				for (int ci : childInds) {
					node.children.add(nodes.get(ci));
				}
			}
		}
		// Set the tree root node
		if (nodes.size() != 0) {
			treeRoot = nodes.get(0);
			numTreeNodes=  nodes.size();
		} else {
			treeRoot = new TreeNode();
			treeRoot.nodeIndex = 0;
			treeRoot.trans = new StaticTrans(new Mat4d());
			numTreeNodes = 1;
		}

		DataBlock hullBlock = topBlock.findChildByName("ConvexHull");
		if (hullBlock == null) throw new RenderException("Missing mesh convex hull");

		_staticHull = ConvexHull.fromDataBlock(hullBlock, vec3ds);
		_defaultBounds = _staticHull.getAABB(new Mat4d());

		if (!keepRuntimeData) {
			v2Interner = null; // Drop ref to the interner to free memory
			v3Interner = null; // Drop ref to the interner to free memory
			v4Interner = null; // Drop ref to the interner to free memory
		}

		populateActionList();
	}

	private class ExportTransVisitor implements TransVisitor {

		private DataBlock transBlock;
		@Override
		public void visitStatic(StaticTrans trans) {
			transBlock = new DataBlock("StaticTrans", 16*8);

			transBlock.writeMat4d(trans.matrix);
		}

		@Override
		public void visitAnim(AnimTrans trans) {
			transBlock = new DataBlock("AnimTrans", 16*8);
			transBlock.writeMat4d(trans.staticMat);

			for (Act act : trans.actions) {
				transBlock.addChildBlock(getActionBlock(act));
			}
		}

		public DataBlock getTransBlock() {
			return transBlock;
		}

	}

	private DataBlock getActionBlock(Act act) {

		assert act.times.length == act.matrices.length;

		byte[] nameBytes = null;
		try {
			nameBytes = act.name.getBytes("UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new Error(e.getMessage());
		}

		int blockSize = nameBytes.length + 1 + 4 + act.times.length*(8 + 16*8);
		DataBlock actBlock = new DataBlock("Action", blockSize);
		actBlock.writeString(act.name);
		actBlock.writeInt(act.times.length);
		for (double time : act.times) {
			actBlock.writeDouble(time);
		}
		for (Mat4d mat : act.matrices) {
			actBlock.writeMat4d(mat);
		}
		return actBlock;
	}

	private Act readActionBlock(DataBlock actBlock) {

		Act ret = new Act();
		ret.name = actBlock.readString();
		int numKeys = actBlock.readInt();
		ret.times = new double[numKeys];
		ret.matrices = new Mat4d[numKeys];
		ret.invMatrices = new Mat4d[numKeys];

		for (int i = 0; i < numKeys; ++i) {
			ret.times[i] = actBlock.readDouble();
		}

		for (int i = 0; i < numKeys; ++i) {
			ret.matrices[i] = actBlock.readMat4d();
			ret.invMatrices[i] = ret.matrices[i].inverse();
		}

		return ret;
	}

	private DataBlock getTreeNodeBlock(TreeNode node) {
		// The data segment consists of 3 lists of indices
		int dataSize = 0;
		dataSize += 4 + 4*node.children.size();
		dataSize += 4 + 8*node.meshInstances.size();
		dataSize += 4 + 4*node.lineInstances.size();

		DataBlock nodeBlock = new DataBlock("TreeNode", dataSize);
		ExportTransVisitor etv = new ExportTransVisitor();
		node.trans.accept(etv);
		nodeBlock.addChildBlock(etv.getTransBlock());

		nodeBlock.writeInt(node.children.size());
		for (TreeNode child : node.children) {
			nodeBlock.writeInt(child.nodeIndex);
		}

		nodeBlock.writeInt(node.meshInstances.size());
		for (AnimMeshInstance ami : node.meshInstances) {
			nodeBlock.writeInt(ami.meshIndex);
			nodeBlock.writeInt(ami.materialIndex);
		}

		nodeBlock.writeInt(node.lineInstances.size());
		for (AnimLineInstance ali : node.lineInstances) {
			nodeBlock.writeInt(ali.lineIndex);
		}

		return nodeBlock;
	}

	/**
	 * Build up a tree of 'DataBlock's and return it. This will return null if the runtime data needed as been discarded
	 * @return
	 */
	public DataBlock getDataAsBlock() {
		if (!keepRuntimeData) {
			return null;
		}

		DataBlock topBlock = new DataBlock("MeshData", 0);
		DataBlock vectorsBlock = new DataBlock("VectorLib", 0);
		topBlock.addChildBlock(vectorsBlock);

		// Write out all the vector data to be indexed later
		DataBlock vec2Block = new DataBlock("Vec2ds", v2Interner.getMaxIndex() * 16);
		vectorsBlock.addChildBlock(vec2Block);
		for (int i = 0; i < v2Interner.getMaxIndex(); ++i) {
			Vec2d val = v2Interner.getValueForIndex(i);
			vec2Block.writeDouble(val.x);
			vec2Block.writeDouble(val.y);
		}

		DataBlock vec3Block = new DataBlock("Vec3ds", v3Interner.getMaxIndex() * 24);
		vectorsBlock.addChildBlock(vec3Block);
		for (int i = 0; i < v3Interner.getMaxIndex(); ++i) {
			Vec3d val = v3Interner.getValueForIndex(i);
			vec3Block.writeDouble(val.x);
			vec3Block.writeDouble(val.y);
			vec3Block.writeDouble(val.z);
		}

		DataBlock vec4Block = new DataBlock("Vec4ds", v4Interner.getMaxIndex() * 32);
		vectorsBlock.addChildBlock(vec4Block);
		for (int i = 0; i < v4Interner.getMaxIndex(); ++i) {
			Vec4d val = v4Interner.getValueForIndex(i);
			vec4Block.writeDouble(val.x);
			vec4Block.writeDouble(val.y);
			vec4Block.writeDouble(val.z);
			vec4Block.writeDouble(val.w);
		}

		// Sub mesh data
		DataBlock subMeshes = new DataBlock("SubMeshes", 0);
		topBlock.addChildBlock(subMeshes);

		for (SubMeshData subData : _subMeshesData) {
			DataBlock subDataBlock = new DataBlock("SubMeshData", 0);
			subMeshes.addChildBlock(subDataBlock);

			DataBlock subVertsBlock = new DataBlock("Vertices", subData.verts.size() * 4);
			subDataBlock.addChildBlock(subVertsBlock);
			for (Vec3d v : subData.verts) {
				int vecInd = v3Interner.getIndexForValue(v);
				subVertsBlock.writeInt(vecInd);
			}

			DataBlock subNormBlock = new DataBlock("Normals", subData.normals.size() * 4);
			subDataBlock.addChildBlock(subNormBlock);
			for (Vec3d v : subData.normals) {
				int vecInd = v3Interner.getIndexForValue(v);
				subNormBlock.writeInt(vecInd);
			}

			if (subData.texCoords != null) {
				DataBlock subTexBlock = new DataBlock("TexCoords", subData.texCoords.size() * 4);
				subDataBlock.addChildBlock(subTexBlock);
				for (Vec2d v : subData.texCoords) {
					int vecInd = v2Interner.getIndexForValue(v);
					subTexBlock.writeInt(vecInd);
				}
			}

			DataBlock indicesBlock = new DataBlock("Indices", subData.indices.length * 4);
			subDataBlock.addChildBlock(indicesBlock);
			for (int ind : subData.indices) {
				indicesBlock.writeInt(ind);
			}

			DataBlock hullBlock = subData.staticHull.toDataBlock(v3Interner);
			subDataBlock.addChildBlock(hullBlock);
		}

		// Sub line data
		DataBlock subLines = new DataBlock("SubLines", 0);
		topBlock.addChildBlock(subLines);

		for (SubLineData subData : _subLinesData) {
			DataBlock subDataBlock = new DataBlock("SubLineData", 0);
			subLines.addChildBlock(subDataBlock);

			DataBlock subVertsBlock = new DataBlock("Vertices", subData.verts.size() * 4);
			subDataBlock.addChildBlock(subVertsBlock);
			for (Vec3d v : subData.verts) {
				int vecInd = v3Interner.getIndexForValue(v);
				subVertsBlock.writeInt(vecInd);
			}

			DataBlock colorBlock = new DataBlock("Color", 4);
			subDataBlock.addChildBlock(colorBlock);
			writeColorToBlock(subData.diffuseColor, colorBlock);
		}

		DataBlock subMInsts = new DataBlock("SubMeshInstances", 0);
		topBlock.addChildBlock(subMInsts);
		for (StaticMeshInstance subInst : _staticMeshInstances) {
			DataBlock staticSubInst = new DataBlock("StaticSubInstance", 0);
			subMInsts.addChildBlock(staticSubInst);

			DataBlock indices = new DataBlock("Indices", 8);
			staticSubInst.addChildBlock(indices);
			indices.writeInt(subInst.subMeshIndex);
			indices.writeInt(subInst.materialIndex);

			DataBlock transBlock = new DataBlock("Transform", 128);
			staticSubInst.addChildBlock(transBlock);
			double[] transCMData = subInst.transform.toCMDataArray();
			for (int i = 0; i < 16; ++i) {
				transBlock.writeDouble(transCMData[i]);
			}
		}

		DataBlock subLInsts = new DataBlock("SubLineInstances", 0);
		topBlock.addChildBlock(subLInsts);
		for (StaticLineInstance subInst : _staticLineInstances) {
			DataBlock subLineInst = new DataBlock("SubLineInstance", 0);
			subLInsts.addChildBlock(subLineInst);

			DataBlock indices = new DataBlock("Indices", 4);
			subLineInst.addChildBlock(indices);
			indices.writeInt(subInst.lineIndex);

			DataBlock transBlock = new DataBlock("Transform", 128);
			subLineInst.addChildBlock(transBlock);
			double[] transCMData = subInst.transform.toCMDataArray();
			for (int i = 0; i < 16; ++i) {
				transBlock.writeDouble(transCMData[i]);
			}
		}

		final DataBlock animTreeBlock = new DataBlock("AnimTree", 0);
		topBlock.addChildBlock(animTreeBlock);

		class ExportWalker extends TreeWalker {
			@Override
			public void onNode(Mat4d trans, Mat4d invTrans, TreeNode node) {
				DataBlock nodeBlock = getTreeNodeBlock(node);
				animTreeBlock.addChildBlock(nodeBlock);
			}
		}
		// Walk the tree in order, so that the nodes are exported in index order
		walkTree(new ExportWalker(), treeRoot, new Mat4d(), new Mat4d(), null);


		// Materials
		DataBlock matsBlock = new DataBlock("Materials", 0);
		topBlock.addChildBlock(matsBlock);
		for (Material mat : _materials) {
			DataBlock matBlock = new DataBlock("Material", 0);
			matsBlock.addChildBlock(matBlock);

			DataBlock colors = new DataBlock("DifAmbSpecShinTrans", 24);
			matBlock.addChildBlock(colors);
			writeColorToBlock(mat.diffuseColor, colors);
			writeColorToBlock(mat.ambientColor, colors);
			writeColorToBlock(mat.specColor, colors);
			colors.writeFloat((float)mat.shininess);
			writeColorToBlock(mat.transColour, colors);
			colors.writeInt(mat.transType);


			if (mat.colorTex != null) {
				String texString = mat.relColorTex;
				DataBlock texBlock = new DataBlock("DiffuseTexture", texString.length()*4 + 1); // Worst case size
				matBlock.addChildBlock(texBlock);
				texBlock.writeString(texString);
			}
		}

		DataBlock hullBlock = _staticHull.toDataBlock(v3Interner);
		topBlock.addChildBlock(hullBlock);

		return topBlock;
	}

	/**
	 * Returns an array of all the used shaders for this MeshData
	 * @return
	 */
	public int[] getUsedMeshShaders(boolean canBatch) {
		ArrayList<Integer> shaderIDs = new ArrayList<>();
		for (StaticMeshInstance smi : _staticMeshInstances) {
			Material mat = _materials.get(smi.materialIndex);
			int shaderID = mat.getShaderID();

			if (canBatch && mat.transType == NO_TRANS) {
				shaderID = shaderID | Renderer.STATIC_BATCH_FLAG;
			}

			if (!shaderIDs.contains(shaderID))
				shaderIDs.add(shaderID);
		}

		for (AnimMeshInstance ami : _animMeshInstances) {
			int shaderID = _materials.get(ami.materialIndex).getShaderID();

			if (!shaderIDs.contains(shaderID))
				shaderIDs.add(shaderID);
		}

		int[] ret = new int[shaderIDs.size()];
		for (int i = 0; i < shaderIDs.size(); ++i) {
			ret[i] = shaderIDs.get(i);
		}
		return ret;
	}
}
