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

import java.util.ArrayList;

import com.jaamsim.MeshFiles.MeshData;
import com.jaamsim.math.AABB;
import com.jaamsim.math.ConvexHull;
import com.jaamsim.math.Mat4d;
import com.jaamsim.math.MathUtils;
import com.jaamsim.math.Ray;
import com.jaamsim.math.Transform;
import com.jaamsim.math.Vec3d;
import com.jaamsim.math.Vec4d;

public class Mesh implements Renderable {

private final MeshProto _proto;
private final Transform _trans;
private final Vec3d _scale; // Allow for a non-uniform scale

private final long _pickingID;
private final VisibilityInfo _visInfo;

private final ConvexHull _hull;
private final AABB _bounds;

private final Mat4d _modelMat;
private final Mat4d _invModelMat;
//private final ArrayList<Action.Queue> _actions;
private final MeshData.Pose pose;
private HullProto debugHull = null;

public Mesh(MeshProto proto, Transform trans, Vec3d scale,
            ArrayList<Action.Queue> actions, VisibilityInfo visInfo, long pickingID) {

	_trans = new Transform(trans);
	_proto = proto;
	_scale = new Vec3d(scale);
	_visInfo = visInfo;

	_modelMat = RenderUtils.mergeTransAndScale(_trans, _scale);

	_invModelMat = RenderUtils.getInverseWithScale(_trans, _scale);

	pose = _proto.getPose(actions);
	_hull = _proto.getHull(pose);
	_bounds = _hull.getAABB(_modelMat);

	_pickingID = pickingID;
}

@Override
public AABB getBoundsRef() {
	return _bounds;
}

@Override
public void render(int contextID, Renderer renderer, Camera cam, Ray pickRay) {

	_proto.render(contextID, renderer, _modelMat, _invModelMat, cam, pose);

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
	for (MeshData.StaticMeshInstance subInst : data.getStaticMeshInstances()) {

		MeshData.SubMeshData subData = data.getSubMeshData().get(subInst.subMeshIndex);

		Mat4d invModelMat = new Mat4d();
		invModelMat.mult4(subInst.invTrans, _invModelMat);

		Mat4d fullModelMat = new Mat4d();
		fullModelMat.mult4(_modelMat, subInst.transform);

		double subDist = collideSubMesh(subData, r, fullModelMat, invModelMat);
		if (subDist < shortDistance) {
			shortDistance = subDist;
		}
	}
	for (MeshData.AnimMeshInstance animInst : data.getAnimMeshInstances()) {

		MeshData.SubMeshData subData = data.getSubMeshData().get(animInst.meshIndex);

		Mat4d invModelMat = new Mat4d();
		invModelMat.mult4(pose.invTransforms[animInst.nodeIndex], _invModelMat);

		Mat4d fullModelMat = new Mat4d();
		fullModelMat.mult4(_modelMat, pose.transforms[animInst.nodeIndex]);


		double subDist = collideSubMesh(subData, r, fullModelMat, invModelMat);
		if (subDist < shortDistance) {
			shortDistance = subDist;
		}
	}

	// Now check against line components
	for (MeshData.StaticLineInstance subInst : data.getStaticLineInstances()) {

		MeshData.SubLineData subData = data.getSubLineData().get(subInst.lineIndex);

		Mat4d invModelMat = new Mat4d();
		invModelMat.mult4(subInst.invTrans, _invModelMat);

		Mat4d fullModelMat = new Mat4d();
		fullModelMat.mult4(_modelMat, subInst.transform);

		double lineDist = collideSubLine(subData, r, fullModelMat, invModelMat);

		if (lineDist > 0 && lineDist < shortDistance) {
			shortDistance = lineDist;
		}
	}

	for (MeshData.AnimLineInstance animInst : data.getAnimLineInstances()) {

		MeshData.SubLineData subData = data.getSubLineData().get(animInst.lineIndex);

		Mat4d invModelMat = new Mat4d();
		invModelMat.mult4(pose.invTransforms[animInst.nodeIndex], _invModelMat);

		Mat4d fullModelMat = new Mat4d();
		fullModelMat.mult4(_modelMat, pose.transforms[animInst.nodeIndex]);

		double lineDist = collideSubLine(subData, r, fullModelMat, invModelMat);

		if (lineDist > 0 && lineDist < shortDistance) {
			shortDistance = lineDist;
		}
	}

	if (shortDistance == Double.POSITIVE_INFINITY) {
		return -1; // We did not actually collide with anything
	}

	return shortDistance;

}

private double collideSubMesh(MeshData.SubMeshData subData, Ray r, Mat4d modelMat, Mat4d invModelMat) {

	Ray localRay = r.transform(invModelMat);

	// Rough collision to AABB
	if (subData.localBounds.collisionDist(localRay) < 0) {
		return Double.POSITIVE_INFINITY;
	}

	ConvexHull subDataHull = subData.staticHull;
	double subDist = subDataHull.collisionDistanceByMatrix(r, modelMat, invModelMat);
	if (subDist < 0) {
		return Double.POSITIVE_INFINITY;
	}

	// We have hit both the AABB and the convex hull for this sub instance, now do individual triangle collision
	Vec3d[] triVecs = new Vec3d[3];

	double shortDist = Double.POSITIVE_INFINITY;

	for (int triInd = 0; triInd < subData.indices.length / 3; ++triInd) {
		triVecs[0] = subData.verts.get(subData.indices[triInd*3+0]);
		triVecs[1] = subData.verts.get(subData.indices[triInd*3+1]);
		triVecs[2] = subData.verts.get(subData.indices[triInd*3+2]);
		if ( triVecs[0].equals3(triVecs[1]) ||
		     triVecs[1].equals3(triVecs[2]) ||
		     triVecs[2].equals3(triVecs[0])) {
			continue;
		}
		double triDist = MathUtils.collisionDistPoly(localRay, triVecs);
		if (triDist > 0) {
			// We have collided, now we need to figure out the distance in original ray space, not the transformed ray space
			Vec3d temp = localRay.getPointAtDist(triDist);
			temp.multAndTrans3(modelMat, temp); // Temp is the collision point in world space
			temp.sub3(temp, r.getStartRef());

			double newDist = temp.mag3();

			if (newDist < shortDist) {
				shortDist = newDist;
			}
		}
	}

	return shortDist;
}

private double collideSubLine(MeshData.SubLineData subData, Ray r, Mat4d modelMat, Mat4d invModelMat) {

	double subDist = subData.hull.collisionDistanceByMatrix(r, modelMat, invModelMat);
	if (subDist < 0) {
		return Double.POSITIVE_INFINITY;
	}

	Mat4d rayMat = MathUtils.RaySpace(r);
	Vec4d[] lineVerts = new Vec4d[subData.verts.size()];
	for (int i = 0; i < lineVerts.length; ++i) {
		lineVerts[i] = new Vec4d();
		lineVerts[i].multAndTrans3(modelMat, subData.verts.get(i));
	}

	return MathUtils.collisionDistLines(rayMat, lineVerts, 0.01309); // Angle is 0.75 deg in radians
}

@Override
public boolean hasTransparent() {
	return _proto.hasTransparent() || Renderer.debugDrawHulls();
}

@Override
public void renderTransparent(int contextID, Renderer renderer, Camera cam, Ray pickRay) {

	_proto.renderTransparent(contextID, renderer, _modelMat, _invModelMat, cam, pose);

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
