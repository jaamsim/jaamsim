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

import java.util.ArrayList;

import com.jaamsim.MeshFiles.MeshData;
import com.jaamsim.math.AABB;
import com.jaamsim.math.Color4d;
import com.jaamsim.math.ConvexHull;
import com.jaamsim.math.Mat4d;
import com.jaamsim.math.MathUtils;
import com.jaamsim.math.Ray;
import com.jaamsim.math.Transform;
import com.jaamsim.math.Vec3d;
import com.jaamsim.math.Vec4d;

//import javax.media.opengl.*;

public class Mesh implements Renderable {

private MeshProto _proto;
private Transform _trans;
private Vec3d _scale; // Allow for a non-uniform scale

private long _pickingID;
private VisibilityInfo _visInfo;

private ConvexHull _hull;
private AABB _bounds;

private Mat4d _modelMat;
private Mat4d _normalMat;
private ArrayList<ConvexHull> _subMeshHulls;
private ArrayList<AABB> _subMeshBounds;
private ArrayList<Action.Queue> _actions;
private HullProto debugHull = null;

public Mesh(MeshProto proto, Transform trans, Vec3d scale,
            ArrayList<Action.Queue> actions, VisibilityInfo visInfo, long pickingID) {

	_trans = new Transform(trans);
	_proto = proto;
	_scale = new Vec3d(scale);
	_visInfo = visInfo;
	_actions = actions;

	_modelMat = RenderUtils.mergeTransAndScale(_trans, _scale);

	_normalMat = RenderUtils.getInverseWithScale(_trans, _scale);
	_normalMat.transpose4();

	_subMeshHulls = _proto.getSubHulls(actions);

	_hull = _proto.getHull(_actions, _subMeshHulls);
	_bounds = _hull.getAABB(_modelMat);

	_subMeshBounds = new ArrayList<AABB>(_subMeshHulls.size());
	for (ConvexHull subHull : _subMeshHulls) {
		AABB subBounds = subHull.getAABB(_modelMat);
		_subMeshBounds.add(subBounds);
	}

	_pickingID = pickingID;
}

@Override
public AABB getBoundsRef() {
	return _bounds;
}

@Override
public void render(int contextID, Renderer renderer, Camera cam, Ray pickRay) {

	_proto.render(contextID, renderer, _modelMat, _normalMat, cam, _actions, _subMeshBounds);

	if (Renderer.debugDrawArmatures()) {
		Mat4d modelViewMat = new Mat4d();
		cam.getViewMat4d(modelViewMat);
		modelViewMat.mult4(_modelMat);

		MeshData md = _proto.getRawData();
		for (Armature arm : md.getArmatures()) {
			ArrayList<Mat4d> pose = null;
			if (_actions != null) {
				pose = arm.getPose(_actions);
			}
			DebugUtils.renderArmature(contextID, renderer, modelViewMat, arm, pose, new Color4d(1, 0, 0), cam);
		}
	}
}

@Override
public long getPickingID() {
	return _pickingID;
}

@Override
public double getCollisionDist(Ray r, boolean precise)
{
	double boundsDist = _bounds.collisionDist(r);
	if (boundsDist < 0) {
		return boundsDist;
	}

	double roughCollision = _hull.collisionDistance(r, _trans, _scale);
	if (!precise || roughCollision < 0) {
		// This is either a rough collision, or we missed outright
		return roughCollision;
	}

	double shortDistance = Double.POSITIVE_INFINITY;

	MeshData data = _proto.getRawData();
	// Check against all sub meshes
	for (int instInd = 0; instInd < data.getSubMeshInstances().size(); ++instInd) {
		AABB subBounds = _subMeshBounds.get(instInd);
		// Rough collision to AABB
		if (subBounds.collisionDist(r) < 0) {
			continue;
		}

		MeshData.SubMeshInstance subInst = data.getSubMeshInstances().get(instInd);

		MeshData.SubMeshData subData = data.getSubMeshData().get(subInst.subMeshIndex);

		Mat4d objMat = RenderUtils.mergeTransAndScale(_trans, _scale);
		Mat4d invObjMat = objMat.inverse();

		ConvexHull subInstHull = _subMeshHulls.get(instInd);
		double subDist = subInstHull.collisionDistanceByMatrix(r, objMat, invObjMat);
		if (subDist < 0) {
			continue;
		}
		// We have hit both the AABB and the convex hull for this sub instance, now do individual triangle collision

		Mat4d animatedTransform = subInst.getAnimatedTransform(_actions);

		Mat4d subMat = RenderUtils.mergeTransAndScale(_trans, _scale);
		subMat.mult4(animatedTransform);

		Mat4d invMat = subMat.inverse();

		ArrayList<Vec3d> vertices = null;
		if (_actions == null || _actions.size() == 0 || subInst.armatureIndex == -1) {
			// Not animated, just take the static vertices
			vertices = subData.verts;
		} else {
			// This mesh is being animated by an armature, we need to work out the
			// new vertex positions
			ArrayList<Mat4d> pose = data.getArmatures().get(subInst.armatureIndex).getPose(_actions);

			// Just renaming the matrix to make this code easier to read
			Mat4d bindMat = animatedTransform;
			Mat4d invBindMat = bindMat.inverse();

			double[] weights = new double[4];
			int[] indices = new int[4];

			Vec3d bindSpaceVert = new Vec3d();
			Vec3d temp = new Vec3d();

			vertices = new ArrayList<Vec3d>(subData.verts.size());
			for (int i = 0; i < subData.verts.size(); ++i) {
				Vec3d vert = subData.verts.get(i);
				bindSpaceVert.multAndTrans3(bindMat, vert);

				Vec4d rawWeights = subData.boneWeights.get(i);
				Vec4d rawIndices = subData.boneIndices.get(i);
				weights[0] = rawWeights.x; weights[1] = rawWeights.y; weights[2] = rawWeights.z; weights[3] = rawWeights.w;
				indices[0] = (int)rawIndices.x; indices[1] = (int)rawIndices.y; indices[2] = (int)rawIndices.z; indices[3] = (int)rawIndices.w;

				if (indices[0] == -1) {
					// This vertex is not influenced by any bone
					vertices.add(vert);
					continue;
				}
				Vec4d animVert = new Vec4d();
				animVert.w = 1;
				for (int j = 0; j < 4; ++j) {
					if (weights[j] == 0) continue;

					// Add the influence of all the bones
					Mat4d boneMat = pose.get(indices[j]);
					temp.multAndTrans3(boneMat, bindSpaceVert);
					temp.scale3(weights[j]);
					animVert.add3(temp);
				}
				// Now convert this vertex back to instance space to be equivalent to the non-animated case
				animVert.mult4(invBindMat, animVert);
				vertices.add(animVert);
			}
		}

		Ray localRay = r.transform(invMat);
		Vec3d[] triVecs = new Vec3d[3];

		for (int triInd = 0; triInd < subData.indices.length / 3; ++triInd) {
			triVecs[0] = vertices.get(subData.indices[triInd*3+0]);
			triVecs[1] = vertices.get(subData.indices[triInd*3+1]);
			triVecs[2] = vertices.get(subData.indices[triInd*3+2]);
			if ( triVecs[0].equals3(triVecs[1]) ||
			     triVecs[1].equals3(triVecs[2]) ||
			     triVecs[2].equals3(triVecs[0])) {
				continue;
			}
			double triDist = MathUtils.collisionDistPoly(localRay, triVecs);
			if (triDist > 0) {
				// We have collided, now we need to figure out the distance in original ray space, not the transformed ray space
				Vec3d temp = localRay.getPointAtDist(triDist);
				temp.multAndTrans3(subMat, temp); // Temp is the collision point in world space
				temp.sub3(temp, r.getStartRef());

				double newDist = temp.mag3();

				if (newDist < shortDistance) {
					shortDistance = newDist;
				}
			}
		}
	}

	// Now check against line components
	for (int instInd = 0; instInd < data.getSubLineInstances().size(); ++instInd) {
		MeshData.SubLineInstance subInst = data.getSubLineInstances().get(instInd);

		MeshData.SubLineData subData = data.getSubLineData().get(subInst.subLineIndex);

		Mat4d subMat = RenderUtils.mergeTransAndScale(_trans, _scale);
		subMat.mult4(subInst.transform);

		Mat4d invMat = subMat.inverse();
		double subDist = subData.hull.collisionDistanceByMatrix(r, subMat, invMat);
		if (subDist < 0) {
			continue;
		}

		Mat4d rayMat = MathUtils.RaySpace(r);
		Vec4d[] lineVerts = new Vec4d[subData.verts.size()];
		for (int i = 0; i < lineVerts.length; ++i) {
			lineVerts[i] = new Vec4d();
			lineVerts[i].multAndTrans3(subMat, subData.verts.get(i));
		}

		double lineDist = MathUtils.collisionDistLines(rayMat, lineVerts, 0.01309); // Angle is 0.75 deg in radians

		if (lineDist > 0 && lineDist < shortDistance) {
			shortDistance = lineDist;
		}
	}

	if (shortDistance == Double.POSITIVE_INFINITY) {
		return -1; // We did not actually collide with anything
	}

	return shortDistance;

}

@Override
public boolean hasTransparent() {
	return _proto.hasTransparent() || Renderer.debugDrawHulls();
}

@Override
public void renderTransparent(int contextID, Renderer renderer, Camera cam, Ray pickRay) {

	_proto.renderTransparent(contextID, renderer, _modelMat, _normalMat, cam, _actions, _subMeshBounds);

	// Debug render of the convex hull
	if (Renderer.debugDrawHulls()) {

		Mat4d modelViewMat = new Mat4d();
		cam.getViewMat4d(modelViewMat);
		modelViewMat.mult4(_modelMat);

		if (debugHull == null) {
			debugHull = new HullProto(_hull);
		}
		debugHull.render(contextID, renderer, modelViewMat, cam);
	}
}

@Override
public boolean renderForView(int viewID, Camera cam) {
	double dist = cam.distToBounds(getBoundsRef());
	return _visInfo.isVisible(viewID, dist);
}
}
