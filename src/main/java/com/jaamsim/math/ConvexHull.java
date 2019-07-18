/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2012 Ausenco Engineering Canada Inc.
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
package com.jaamsim.math;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import com.jaamsim.MeshFiles.DataBlock;
import com.jaamsim.render.RenderException;
import com.jaamsim.render.RenderUtils;

/**
 * A convex hull that is initialized by a set of points
 * @author Matt.Chudleigh
 *
 */
public class ConvexHull {
	private ArrayList<Vec3d> _verts;

	private boolean _isDegenerate = false;

	private ArrayList<HullFace> _faces = new ArrayList<>();

	public static ConvexHull TryBuildHull(ArrayList<Vec3d> verts, int numAttempts, int maxNumPoints, Vec3dInterner interner) {

		ArrayList<Vec3d> baseVerts = removeDoubles(verts);

		assert(numAttempts > 0);

		// bad input indicates a hull that will naturally be degenerate, just return the first attempt
		boolean badInput = baseVerts.size() < 3;

		ConvexHull ret = null;
		for (int i = 0; i < numAttempts; ++i) {
			int seed = (int)((double)i/(double)numAttempts);

			ret = new ConvexHull(baseVerts, seed, maxNumPoints, interner);
			if (!ret._isDegenerate || badInput) {
				return ret;
			}
			// The mesh was degenerate, loop and try again
		}

		// We fell through, so return the last degenerate attempt
		return ret;
	}

	/**
	 * Initialize this hull from the vertices provided. This is an implementation of the QuickHull algorithm (or close enough to it)
	 * @param verts
	 */
	public ConvexHull(ArrayList<Vec3d> baseVerts, int seed, int maxNumPoints, Vec3dInterner interner) {

		assert(seed >= 0);
		assert(seed < 1);

		if (baseVerts.size() < 3) {
			// This mesh is too small, so just create an empty Hull... or should we throw?
			makeDegenerate(baseVerts);
			return;
		}

		// Start by finding 3 points to build the original faces, this may have a problem if there is only
		// 3 points and all are in a line
		ArrayList<TempHullFace> tempFaces = new ArrayList<>();
		List<Integer> unclaimedPoints = new ArrayList<>();

		// Create two starting faces (both use the same verts but are wound backwards to face in both directions)

		int ind0 = baseVerts.size() * seed;
		Vec3d v0 = baseVerts.get(0);
		double bestDist = 0;
		int ind1 = 0;
		Vec3d temp = new Vec3d();
		for (int i = 0; i < baseVerts.size(); ++i) {
			if (i == ind0) continue;

			// Ind1 is the furthest vertex from ind0
			temp.sub3(v0, baseVerts.get(i));
			double dist = temp.mag3();
			if (dist > bestDist) {
				bestDist = dist;
				ind1 = i;
			}
		}
		// Now ind2 is the vertex furthest from the line of the above two
		bestDist = 0;
		Vec3d dir = new Vec3d();
		dir.sub3(v0, baseVerts.get(ind1));
		dir.normalize3();
		int ind2 = 0;
		for (int i = 1; i < baseVerts.size(); ++i) {
			if (i == ind0) continue;
			if (i == ind1) continue;

			temp.sub3(v0, baseVerts.get(i));
			temp.cross3(dir, temp);
			double dist = temp.mag3();
			if (dist > bestDist) {
				bestDist = dist;
				ind2 = i;
			}
		}

		if (ind1 == ind0 ||
		    ind2 == ind0 ||
		    ind1 == ind2) {
			makeDegenerate(baseVerts);
			return;
		}

		TempHullFace f0 = new TempHullFace(ind0, ind1, ind2, baseVerts);
		TempHullFace f1 = new TempHullFace(ind0, ind2, ind1, baseVerts);
		tempFaces.add(f0);
		tempFaces.add(f1);

		// Make sure the planes do not face each other
		assert(f0.plane.normal.dot3(f1.plane.normal) < 0.9999 );

		boolean planar = true;

		// Assign all the remaining points to either of the faces if that face can 'see' the vertex
		for (int i = 0; i < baseVerts.size(); ++i) {
			double dist = f0.plane.getNormalDist(baseVerts.get(i));

			if (dist > 0.000001) {
				f0.addPoint(i, baseVerts);
				planar = false;
			} else if (dist < -0.000001){
				f1.addPoint(i, baseVerts);
				planar = false;
			} else {
				unclaimedPoints.add(i);
			}
		}

		if (planar) {
			// Damn... for now fall back to a degenerate hull
			makeDegenerate(baseVerts);
			return;
		}

		int numPoints = 3; // We start with 3 points

		// Initialization is complete, start the core loop
		while (true) {
			// Find any faces with points assigned to it
			TempHullFace f = null;
			bestDist = 0.001; // A non zero value to quick out if the closest points aren't that far
			for (TempHullFace ft : tempFaces) {
				if (ft.points.size() != 0 && ft.furthestDist > bestDist) {
					f = ft;
					bestDist = ft.furthestDist;
				}
			}
			if (f == null) {
				// There's no remaining points unassigned, we're done.
				break;
			}

			// Find the point assigned to this face that is the furthest away
			int farInd = f.furthestInd;

			// Remove any faces that can see this point and orphan any points owned by these faces
			Vec3d farVert = baseVerts.get(farInd);

			ArrayList<TempHullFace> deadFaces = new ArrayList<>();

			for (Iterator<TempHullFace> it = tempFaces.iterator(); it.hasNext(); ) {
				TempHullFace tempFace = it.next();

				if (tempFace.plane.getNormalDist(farVert) > -0.000000001) { // Non zero to allow a bit of floating point round off and avoid degenerate faces
					// This face can see this point, and is therefore not part of the hull
					deadFaces.add(tempFace);
					it.remove();
				}

			}

			// The points that are no longer associated with a face
			ArrayList<Integer> orphanedPoints = new ArrayList<>();
			orphanedPoints.addAll(unclaimedPoints);
			unclaimedPoints.clear();

			// Find all the open edges left by removing these faces
			ArrayList<HullEdge> edges = new ArrayList<>();
			for (TempHullFace df : deadFaces) {
				orphanedPoints.addAll(df.points);

				edges.add(new HullEdge(df.indices[0], df.indices[1]));
				edges.add(new HullEdge(df.indices[1], df.indices[2]));
				edges.add(new HullEdge(df.indices[2], df.indices[0]));
			}

			// Remove double edges (to make sure we have a single loop)
			pruneEdges(edges);

			// Scan the loop to make sure it's a single loop
//			if (!scanEdges(edges)) {
//				// This hull diverged
//				makeDegenerate(baseVerts);
//				return;
//			}

			ArrayList<TempHullFace> newFaces = new ArrayList<>();

			// Build new faces from the remaining edges and assign all remaining points
			for (HullEdge e : edges) {
				TempHullFace newFace = new TempHullFace(e.ind0, e.ind1, farInd, baseVerts);
				tempFaces.add(newFace);

				newFaces.add(newFace);

			} // end of building new faces

			// Add each orphaned point to the new face it is the furthest away from (by normal distance)
			int deadPoints = 0;
			for (int ind : orphanedPoints) {

				TempHullFace bestFace = null;
				bestDist = -1;

				for (TempHullFace tf : newFaces) {
					double dist = tf.plane.getNormalDist(baseVerts.get(ind));
					if (dist > 0.000001 && dist > bestDist) {
						bestFace = tf;
						bestDist = dist;
					}
				}
				if (bestFace != null) {
					bestFace.addPoint(ind, baseVerts);
				} else {
					++deadPoints;
				}
			}

			// This is the end of the main loop, check that at least one point has been claimed

			if (deadPoints == 0) {
				// We have run out of points, so let's just call this good enough
				break;
			}
			if (++numPoints > maxNumPoints && maxNumPoints > 0) {
				// We've looped and built up a hull of the maximum number of points
				break;
			}

		} // End of main loop

		// Now that we have all the faces we can create a real subset of points we care about
		ArrayList<Vec3d> realVerts = new ArrayList<>();
		for (TempHullFace tf : tempFaces) {
			HullFace realFace = new HullFace();
			for (int i = 0; i < 3; ++i) {

				Vec3d oldVert = baseVerts.get(tf.indices[i]);

				int newInd = realVerts.size();

				for (int j = 0; j < realVerts.size(); ++j) {
					if (oldVert.equals3(realVerts.get(j))) {
						// This vertex has already be included in the final list
						newInd = j;
					}
				}
				if (newInd == realVerts.size()) {
					// This vertex isn't in the new list, so add it and update the radius
					if (interner != null)
						realVerts.add(interner.intern(oldVert));
					else
						realVerts.add(oldVert);
				}
				realFace.indices[i] = newInd;
			}

			_faces.add(realFace);
		}

		// swap out our vertex list to the real one
		_verts = realVerts;
	} // End of ConvexHull() Constructor

	private ConvexHull() {

	}

	private static final Comparator<Vec3d> COMP = new Comparator<Vec3d>() {
		@Override
		public int compare(Vec3d v0, Vec3d v1) {
			int comp;
			comp = Double.compare(v0.x, v1.x);
			if (comp != 0)
				return comp;

			comp = Double.compare(v0.y, v1.y);
			if (comp != 0)
				return comp;

			return Double.compare(v0.z, v1.z);

		}
	};

	/**
	 * Remove any doubles from the list and return a new, possibly shorter list
	 * @param orig
	 * @return
	 */
	private static ArrayList<Vec3d> removeDoubles(List<Vec3d> orig) {
		ArrayList<Vec3d> ret = new ArrayList<>();
		if (orig.size() == 0) {
			return ret;
		}

		final ArrayList<Vec3d> copy = new ArrayList<>(orig);

		Collections.sort(copy, COMP);

		ret.add(copy.get(0));
		int outIndex = 0; // An updated index of the last element of the returned set, this may be
		for (int index = 1;index < copy.size(); ++index) {
			if (!copy.get(index).near3(ret.get(outIndex))) {
				// We have not seen this vector before
				ret.add(copy.get(index));
				++outIndex;
			}
		}

		return ret;
	}

	// Remove any edge that is back tracked over, alter 'edges' in place
	private void pruneEdges(ArrayList<HullEdge> edges) {
		for (int i = 0; i < edges.size(); ) {

			HullEdge e0 = edges.get(i);
			boolean keepEdge = true;

			for (int j = i;  j < edges.size(); ++j) {

				HullEdge e1 = edges.get(j);

				if (e0.ind0 == e1.ind1 && e0.ind1 == e1.ind0) {
					keepEdge = false;
					edges.remove(j); // Remove j, which is always higher than i so this should be safe
					break;
				}
			}

			if (keepEdge) {
				++i;
			} else {
				edges.remove(i);
			}
		}
	}

	// This is used for sanity checking the generation code, but it too slow to leave in
	@SuppressWarnings("unused")
	private boolean scanEdges(ArrayList<HullEdge> edges) {
		int[] seenIndices = new int[edges.size()*2];
		int numSeen = 0;

		//ArrayList<Integer> seenIndices = new ArrayList<Integer>();

		for (HullEdge e : edges) {
			boolean seen = false;
			for (int i = 0; i < numSeen; ++i) { if (seenIndices[i] == e.ind0) seen = true; }
			if (!seen)
				seenIndices[numSeen++] = e.ind0;

			for (int i = 0; i < numSeen; ++i) { if (seenIndices[i] == e.ind1) seen = true; }
			if (!seen)
				seenIndices[numSeen++] = e.ind1;
		}


		//assert(seenIndices.size() == edges.size());
		return numSeen == edges.size();

	}

	/**
	 * Return the list of faces (a list of triplets of indices into the vertices) for this mesh
	 * @return
	 */
	public List<HullFace> getFaces() {
		return _faces;
	}

	public List<Vec3d> getVertices() {

		return _verts;
	}

	public boolean collides(Vec4d point, Transform trans) {
		// Simply use the AABB formed from the points for the degenerate cases
		if (_isDegenerate) {
			AABB aabb = getAABB(trans.getMat4dRef());
			return aabb.collides(point);
		}

		Transform inv = new Transform();
		trans.inverse(inv);

		// P is the point in hull space
		Vec3d p = new Vec3d();
		inv.multAndTrans(point, p);

		Plane plane = new Plane();
		for (HullFace f : _faces) {
			this.faceToPlane(f, plane);
			double dist = plane.getNormalDist(p);
			if (dist > 0) {
				// This point is outside at least one plane, so there is not intersection
				return false;
			}
		}
		return true;
	}

	private static final Vec3d ONES = new Vec3d(1.0d, 1.0d, 1.0d);
	public double collisionDistance(Ray r, Transform trans) {
		return collisionDistance(r, trans, ONES);
	}

	/**
	 * Check for collision with the provided ray, will return the distance to a collision, with a negative number being returned for no collsion
	 * @param r
	 * @param trans
	 * @return - distance to collision, or -1 if no collision
	 */
	public double collisionDistance(Ray r, Transform trans, Vec3d scale) {

		return collisionDistanceByMatrix(r,
				RenderUtils.mergeTransAndScale(trans, scale),
				RenderUtils.getInverseWithScale(trans, scale));
	}

	public double collisionDistanceByMatrix(Ray r, Mat4d mat, Mat4d invMat) {

		if (_isDegenerate) {
			AABB aabb = getAABB(mat);
			return aabb.collisionDist(r);
		}

		// hullRay is the point in hull space
		Ray hullRay = r.transform(invMat);

		double back = Double.MAX_VALUE;
		double front = -Double.MAX_VALUE;

		Plane plane = new Plane();
		for (HullFace f : _faces) {
			this.faceToPlane(f, plane);

			boolean bBackFace = plane.backFaceCollision(hullRay);

			double dist = plane.collisionDist(hullRay);

			if (Double.isInfinite(dist)) {
				continue; // Parallel ray and plane
			}

			if ( bBackFace && dist <  back) {  back = dist; }
			if (!bBackFace && dist > front) { front = dist; }
		}

		// We now know the extreme points and can figure out if this collides or not
		if (back < front) { return -1.0; } // No collision

		if (back < 0 && front < 0) { return -1.0; } // Collision behind the start of the ray

		if (front < 0) {
			// The ray starts from inside the object
			return 0.0;
		}

		// Scale the distance back to global coords
		Vec3d collisionPoint = hullRay.getPointAtDist(front);
		// Convert to global space
		collisionPoint.multAndTrans3(mat, collisionPoint);
		Vec3d diff = new Vec3d();
		diff.sub3(r.getStartRef(), collisionPoint);

		return diff.mag3();
	}

	/**
	 * A class that represents a single face of the hull during hull construction. The 'points' member is the list of external
	 * points not yet encompasses by the hull and are visible by this temporary face
	 * @author Matt.Chudleigh
	 *
	 */
	private static class TempHullFace {
		public final int[] indices = new int[3];
		public final Plane plane;
		public double furthestDist = 0;
		public int furthestInd = 0;
		public final ArrayList<Integer> points = new ArrayList<>();

		public TempHullFace(int i0, int i1, int i2, ArrayList<Vec3d> verts) {
			indices[0] = i0;
			indices[1] = i1;
			indices[2] = i2;
			plane = new Plane(verts.get(indices[0]),
			                  verts.get(indices[1]),
			                  verts.get(indices[2]));

		}
		public void addPoint(int ind, ArrayList<Vec3d> verts) {
			Vec3d v = verts.get(ind);
			double dist = plane.getNormalDist(v);
			if (dist >= furthestDist) {
				furthestDist = dist;
				furthestInd = ind;
			}
			assert(dist > -0.000001);
			points.add(ind);
		}

	}

	/**
	 * The main hull face storage class, simply a list of indices
	 * @author Matt.Chudleigh
	 *
	 */
	public static class HullFace {
		public final int[] indices = new int[3];
	}

	// A really dumb class to store edges
	private static class HullEdge {
		final int ind0;
		final int ind1;
		HullEdge(int i0, int i1) {
			ind0 = i0; ind1 = i1;
		}
	}

	private void faceToPlane(HullFace f, Plane p) {
		p.set(_verts.get(f.indices[0]),
		      _verts.get(f.indices[1]),
		      _verts.get(f.indices[2]));
	}

	private void makeDegenerate(ArrayList<Vec3d> vs) {
		_isDegenerate = true;
		_verts = vs;
		_faces = new ArrayList<>();
		// Figure out a radius
	}

	public boolean isDegenerate() {
		return _isDegenerate;
	}

	/**
	 * Get an world space AABB if this hull were transformed by 't'
	 * @param t
	 * @return
	 */
	public AABB getAABB(Mat4d mat) {
		return new AABB(_verts, mat);
	}

	public Vec3d getAABBCenter() {
		AABB tmp = new AABB(_verts);
		return new Vec3d(tmp.center);
	}

	public DataBlock toDataBlock(Vec3dInterner interner) {
		DataBlock topBlock = new DataBlock("ConvexHull", 0);

		DataBlock vertsBlock = new DataBlock("Vertices", _verts.size() * 4);
		for (Vec3d v : _verts) {
			vertsBlock.writeInt(interner.getIndexForValue(v));
		}

		DataBlock facesBlock = new DataBlock("Faces", _faces.size() * 4*3);
		for (HullFace f : _faces) {
			facesBlock.writeInt(f.indices[0]);
			facesBlock.writeInt(f.indices[1]);
			facesBlock.writeInt(f.indices[2]);
		}

		topBlock.addChildBlock(vertsBlock);
		topBlock.addChildBlock(facesBlock);
		return topBlock;
	}

	public static ConvexHull fromDataBlock(DataBlock topBlock, Vec3d[] vecs) {
		if (!topBlock.getName().equals("ConvexHull")) {
			throw new RenderException("ConvexHull block not found");
		}

		ConvexHull ret = new ConvexHull();

		DataBlock vertsBlock = topBlock.findChildByName("Vertices");
		DataBlock facesBlock = topBlock.findChildByName("Faces");

		if (vertsBlock == null) throw new RenderException("Missing vertices in ConvexHull");
		if (facesBlock == null) throw new RenderException("Missing faces in ConvexHull");

		int numVerts = vertsBlock.getDataSize() / 4;
		ret._verts = new ArrayList<>(numVerts);
		for (int i = 0; i < numVerts; ++i) {
			int index = vertsBlock.readInt();
			ret._verts.add(vecs[index]);
		}

		int numFaces = facesBlock.getDataSize() / (4*3);
		ret._faces = new ArrayList<>(numFaces);
		for (int i = 0; i < numFaces; ++i) {
			HullFace f = new HullFace();
			f.indices[0] = facesBlock.readInt();
			f.indices[1] = facesBlock.readInt();
			f.indices[2] = facesBlock.readInt();
			ret._faces.add(f);
		}

		ret._isDegenerate = (numFaces == 0);

		return ret;
	}
}
