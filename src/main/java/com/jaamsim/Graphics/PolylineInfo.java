/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2015 Ausenco Engineering Canada Inc.
 * Copyright (C) 2019-2020 JaamSim Software Inc.
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
package com.jaamsim.Graphics;

import java.util.ArrayList;
import java.util.Arrays;

import com.jaamsim.basicsim.ErrorException;
import com.jaamsim.math.Color4d;
import com.jaamsim.math.MathUtils;
import com.jaamsim.math.Plane;
import com.jaamsim.math.Vec3d;

public class PolylineInfo {
	public static enum CurveType {
		LINEAR,
		BEZIER,
		SPLINE,
		CIRCULAR_ARC,
	}

	private final ArrayList<Vec3d> curvePoints;
	private final Color4d color;
	private final int width; // Line width in pixels

	public PolylineInfo(ArrayList<Vec3d> pts, Color4d col, int w) {
		color = col;
		width = w;
		curvePoints = pts;
	}

	@Override
	public boolean equals(Object o) {
		if (o == this) return true;
		if (!(o instanceof PolylineInfo)) return false;

		PolylineInfo pi = (PolylineInfo)o;

		return curvePoints != null && curvePoints.equals(pi.curvePoints) &&
		       color != null && color.equals(pi.color) &&
		       width == pi.width;
	}

	public ArrayList<Vec3d> getCurvePoints() {
		return curvePoints;
	}

	public Color4d getColor() {
		return color;
	}

	public int getWidth() {
		return width;
	}

	@Override
	public String toString() {
		return curvePoints.toString();
	}

	public static ArrayList<Vec3d> getBezierPoints(ArrayList<Vec3d> ps) {
		ArrayList<Vec3d> ret = new ArrayList<>();

		// The total number of segments in this curve
		final int NUM_POINTS = 32;

		double tInc = 1.0/NUM_POINTS;
		for (int i = 0; i < NUM_POINTS; ++i) {
			ret.add(solveBezier(i*tInc, 0, ps.size()-1, ps));
		}
		ret.add(ps.get(ps.size()-1));
		return ret;
	}

	public static ArrayList<Vec3d> getSplinePoints(ArrayList<Vec3d> ps) {
		// This spline fitting algorithm is a bit of a custom creation. It is loosely based on the finte differences method described
		// in this article: https://en.wikipedia.org/wiki/Cubic_Hermite_spline (assuming it hasn't changed since).
		// The major changes are:
		// * All segments are solved in Bezier form (this is more stylist than a change in algorithm)
		// * The end two segments are quadratic Bezier curves (not cubic) and as such the end tangent is un-constrained.
		// * The internal control points are scaled down proportional to segment length to avoid kinks and self intersection within a segment

		// If there's too few points, just return the existing line
		if (ps.size() <= 2) {
			return ps;
		}

		// The number of segments between each control point
		final int NUM_SEGMENTS = 16;

		Vec3d temp = new Vec3d();

		// Generate tangents for internal points only
		ArrayList<Vec3d> tangents = new ArrayList<>();
		for (int i = 1; i < ps.size()-1; ++i) {
			Vec3d pMin = ps.get(i-1);
			Vec3d p = ps.get(i);
			Vec3d pPlus = ps.get(i+1);

			temp.sub3(p, pMin);
			double l0 = temp.mag3();

			temp.sub3(p, pPlus);
			double l1 = temp.mag3();

			Vec3d tan = new Vec3d();
			tan.sub3(pPlus, pMin);
			tan.scale3(1.0/(l0 + l1));
			tangents.add(tan);
		}

		double tInc = 1.0/NUM_SEGMENTS;

		ArrayList<Vec3d> ret = new ArrayList<>();

		Vec3d scaledTanTemp = new Vec3d();
		Vec3d segDiffTemp = new Vec3d();

		// Start with a quadratic segment
		{
			Vec3d p0 = ps.get(0);
			Vec3d p1 = ps.get(1);

			segDiffTemp.sub3(p0, p1);
			double segLength = segDiffTemp.mag3();

			Vec3d c = new Vec3d(p1);
			scaledTanTemp.scale3(segLength/2.0, tangents.get(0));
			c.sub3(scaledTanTemp);

			for (int t = 0; t < NUM_SEGMENTS; ++t) {
				Vec3d curvePoint = solveQuadraticBezier(t*tInc, p0, p1, c);
				ret.add(curvePoint);
			}
		}

		// Internal segments are cubic
		for (int i = 2; i < ps.size()-1; ++i) {
			Vec3d p0 = ps.get(i-1);
			Vec3d p1 = ps.get(i);

			segDiffTemp.sub3(p0, p1);
			double segLength = segDiffTemp.mag3();

			Vec3d c0 = new Vec3d(p0);
			scaledTanTemp.scale3(segLength/3.0, tangents.get(i-2));
			c0.add3(scaledTanTemp);

			Vec3d c1 = new Vec3d(p1);
			scaledTanTemp.scale3(segLength/3.0, tangents.get(i-1));
			c1.sub3(scaledTanTemp);

			for (int t = 0; t < NUM_SEGMENTS; ++t) {
				Vec3d curvePoint = solveCubicBezier(t*tInc, p0, p1, c0, c1);
				ret.add(curvePoint);
			}
		}

		// End with another quadratic segment
		{
			Vec3d p0 = ps.get(ps.size()-2);
			Vec3d p1 = ps.get(ps.size()-1);

			segDiffTemp.sub3(p0, p1);
			double segLength = segDiffTemp.mag3();

			Vec3d c = new Vec3d(p0);
			scaledTanTemp.scale3(segLength/2.0, tangents.get(tangents.size()-1));
			c.add3(scaledTanTemp);

			for (int t = 0; t < NUM_SEGMENTS; ++t) {
				Vec3d curvePoint = solveQuadraticBezier(t*tInc, p0, p1, c);
				ret.add(curvePoint);
			}
		}

		ret.add(ps.get(ps.size()-1));

		return ret;
	}

	public static ArrayList<Vec3d> getCircularArcPoints(ArrayList<Vec3d> ps) {
		if (ps.size() <= 2) {
			return ps;
		}

		Vec3d start = new Vec3d(ps.get(0));
		Vec3d mid = new Vec3d(ps.get(1));
		Vec3d end = new Vec3d(ps.get(ps.size()-1));

		Plane circlePlane = new Plane(start, mid, end);
		Plane midPlane0 = MathUtils.getMidpointPlane(start, mid);
		Plane midPlane1 = MathUtils.getMidpointPlane(mid, end);
		Vec3d center = MathUtils.collidePlanes(circlePlane, midPlane0, midPlane1);
		if (center == null) {
			return ps;
		}

		Vec3d temp = new Vec3d(center);
		temp.sub3(start);
		double radius = temp.mag3();

		// Create the two orthogonal components of our circle (the equivalent of x and y)
		Vec3d xComp = new Vec3d(start);
		xComp.sub3(center);

		Vec3d yComp = new Vec3d();
		yComp.cross3(circlePlane.normal, xComp);
		yComp.normalize3();
		yComp.scale3(radius);

		// Find the end arc value
		temp.sub3(end, center);
		double xVal = temp.dot3(xComp);
		double yVal = temp.dot3(yComp);
		double arcAngle = Math.atan2(yVal, xVal);
		if (arcAngle < 0) {
			arcAngle += 2*Math.PI;
		}

		// Find the number of segments to draw, 60 for a full circle
		int numSegments = (int)(arcAngle*60/(2*Math.PI));
		if (numSegments < 2)
			numSegments = 2;

		// Build the arc
		ArrayList<Vec3d> ret = new ArrayList<>();
		for (int i = 0; i <= numSegments; ++i) {
			double theta = arcAngle*i/numSegments;

			Vec3d point = new Vec3d(center);

			temp.scale3(Math.cos(theta), xComp);
			point.add3(temp);

			temp.scale3(Math.sin(theta), yComp);
			point.add3(temp);

			ret.add(point);
		}
		return ret;
	}


	// Solve a generic bezier with arbitrary control points
	private static Vec3d solveBezier(double t, int start, int end, ArrayList<Vec3d> controls) {
		// Termination case
		if (start == end) {
			return new Vec3d(controls.get(start));
		}
		Vec3d a = solveBezier(t, start, end-1, controls);
		Vec3d b = solveBezier(t, start+1, end, controls);

		a.scale3(1-t);
		b.scale3(t);
		a.add3(b);

		return a;
	}

	// Solve a cubic bezier with explicit control points
	private static Vec3d solveCubicBezier(double s, Vec3d p0, Vec3d p1, Vec3d c0, Vec3d c1) {

		double oneMinS = 1 - s;
		double coeffP0 = oneMinS*oneMinS*oneMinS;
		double coeffC0 = 3*s*oneMinS*oneMinS;
		double coeffC1 = 3*s*s*oneMinS;
		double coeffP1 = s*s*s;

		Vec3d lp0 = new Vec3d(p0);
		lp0.scale3(coeffP0);

		Vec3d lp1 = new Vec3d(p1);
		lp1.scale3(coeffP1);

		Vec3d lc0 = new Vec3d(c0);
		lc0.scale3(coeffC0);

		Vec3d lc1 = new Vec3d(c1);
		lc1.scale3(coeffC1);

		Vec3d ret = new Vec3d();
		ret.add3(lp0);
		ret.add3(lp1);
		ret.add3(lc0);
		ret.add3(lc1);

		return ret;
	}

	// Solve a quadratic bezier with an explicit control point
	private static Vec3d solveQuadraticBezier(double s, Vec3d p0, Vec3d p1, Vec3d c) {

		double oneMinS = 1 - s;

		double coeffP0 = oneMinS*oneMinS;
		double coeffC = 2*s*oneMinS;
		double coeffP1 = s*s;

		Vec3d lp0 = new Vec3d(p0);
		lp0.scale3(coeffP0);

		Vec3d lp1 = new Vec3d(p1);
		lp1.scale3(coeffP1);

		Vec3d lc = new Vec3d(c);
		lc.scale3(coeffC);

		Vec3d ret = new Vec3d();
		ret.add3(lp0);
		ret.add3(lp1);
		ret.add3(lc);

		return ret;
	}

	/**
	 * Returns the local coordinates for a specified fractional distance along a polyline.
	 * @param pts - points for the polyline
	 * @param frac - fraction of the total graphical length of the polyline
	 * @return local coordinates for the specified position
	 */
	public static Vec3d getPositionOnPolyline(ArrayList<Vec3d> pts, double frac) {

		if (pts.isEmpty())
			return null;

		// Calculate the cumulative graphical lengths along the polyline
		double[] cumLengthList = PolylineInfo.getCumulativeLengths(pts);

		// Find the insertion point by binary search
		double dist = frac * cumLengthList[cumLengthList.length-1];
		int k = Arrays.binarySearch(cumLengthList, dist);

		// Exact match
		if (k >= 0)
			return pts.get(k);

		// Error condition
		if (k == -1)
			return new Vec3d();

		// Insertion index = -k-1
		int index = -k - 1;

		// Interpolate the final position between the two points
		if (index == cumLengthList.length) {
			return new Vec3d(pts.get(index-1));
		}
		double fracInSegment = (dist - cumLengthList[index-1]) /
				(cumLengthList[index] - cumLengthList[index-1]);
		Vec3d vec = new Vec3d();
		vec.interpolate3(pts.get(index-1),
				pts.get(index),
				fracInSegment);
		return vec;
	}

	public static double getAngleOnPolyline(ArrayList<Vec3d> pts, double frac) {

		if (pts.isEmpty())
			return 0.0d;

		// Calculate the cumulative graphical lengths along the polyline
		double[] cumLengthList = PolylineInfo.getCumulativeLengths(pts);

		// Find the insertion point by binary search
		double dist = frac * cumLengthList[cumLengthList.length-1];
		int k = Arrays.binarySearch(cumLengthList, dist);

		// Error condition
		if (k == -1)
			return 0.0d;

		// Insertion index
		int index = k;
		if (k < 0)
			index = -k - 1;
		index = Math.max(index, 1);
		index = Math.min(index, pts.size() - 1);

		// Return the angle
		Vec3d vec = new Vec3d(pts.get(index));
		vec.sub3(pts.get(index - 1));
		vec.normalize3();
		return Math.atan2(vec.y, vec.x);
	}

	/**
	 * Returns the local coordinates for a sub-section of the polyline specified by a first and
	 * last fractional distance.
	 * @param pts - points for the polyline
	 * @param frac0 - fractional distance for the start of the sub-polyline
	 * @param frac1 - fractional distance for the end of the sub-polyline
	 * @return array of local coordinates for the sub-polyline
	 */
	public static ArrayList<Vec3d> getSubPolyline(ArrayList<Vec3d> pts, double frac0, double frac1) {

		ArrayList<Vec3d> ret = new ArrayList<>();
		if (pts.isEmpty())
			return ret;

		// Calculate the cumulative graphical lengths along the polyline
		double[] cumLengthList = PolylineInfo.getCumulativeLengths(pts);

		// Find the insertion point for the first distance using binary search
		double dist0 = frac0 * cumLengthList[cumLengthList.length-1];
		int k = Arrays.binarySearch(cumLengthList, dist0);
		if (k == -1)
			throw new ErrorException("Unable to find position in polyline using binary search.");

		Vec3d lastPt = pts.get(pts.size()-1);

		// Interpolate the position of the first node
		int index;
		if (k >= 0) {
			ret.add(pts.get(k));
			index = k + 1;
			if (index == cumLengthList.length) {
				ret.add(lastPt);
				return ret;
			}
		}
		else {
			index = -k - 1;
			if (index == cumLengthList.length) {
				ret.add(lastPt);
				ret.add(lastPt);
				return ret;
			}
			double fracInSegment = (dist0 - cumLengthList[index-1]) /
					(cumLengthList[index] - cumLengthList[index-1]);
			Vec3d vec = new Vec3d();
			vec.interpolate3(pts.get(index-1),
					pts.get(index),
					fracInSegment);
			ret.add(vec);
		}

		// Loop through the indices following the insertion point
		double dist1 = frac1 * cumLengthList[cumLengthList.length-1];
		while (index < cumLengthList.length && cumLengthList[index] < dist1) {
			ret.add(pts.get(index));
			index++;
		}
		if (index == cumLengthList.length) {
			ret.add(lastPt);
			return ret;
		}

		// Interpolate the position of the last node
		Vec3d vec = new Vec3d();
		double fracInSegment = (dist1 - cumLengthList[index-1]) /
                (cumLengthList[index] - cumLengthList[index-1]);
		vec.interpolate3(pts.get(index-1),
                 pts.get(index),
                 fracInSegment);
		ret.add(vec);
		return ret;
	}

	/**
	 * Returns the cumulative graphics lengths for the nodes along the polyline.
	 * @param pts - points for the polyline
	 * @return array of cumulative graphical lengths
	 */
	public static double[] getCumulativeLengths(ArrayList<Vec3d> pts) {
		double[] cumLengthList = new double[pts.size()];
		cumLengthList[0] = 0.0;
		for (int i = 1; i < pts.size(); i++) {
			Vec3d vec = new Vec3d();
			vec.sub3(pts.get(i), pts.get(i-1));
			cumLengthList[i] = cumLengthList[i-1] + vec.mag3();
		}
		return cumLengthList;
	}

	/**
	 * Returns the total graphics length for a polyline.
	 * @param pts - points for the polyline
	 * @return total length
	 */
	public static double getLength(ArrayList<Vec3d> pts) {
		double ret = 0.0d;
		for (int i = 1; i < pts.size(); i++) {
			Vec3d vec = new Vec3d();
			vec.sub3(pts.get(i), pts.get(i-1));
			ret += vec.mag3();
		}
		return ret;
	}

	/**
	 * Returns the fractional position along a polyline for the location that is closest to the
	 * specified point.
	 * @param pts - coordinates for the polyline's nodes
	 * @param point - specified point
	 * @return fraction of the polyline's length
	 */
	public static double getNearestPosition(ArrayList<Vec3d> pts, Vec3d point) {

		// Distance to the first node in the polyline
		Vec3d vec0 = new Vec3d(point);  // vector from the node to the point
		vec0.sub3(pts.get(0));
		double dist = vec0.mag3();
		double totalLength = 0.0;
		double pos = 0.0d;

		// Loop through each segment of the polyline
		for (int i = 1; i < pts.size(); i++) {
			Vec3d vec1 = new Vec3d(pts.get(i));  // vector along the segment
			vec1.sub3(pts.get(i - 1));
			double length = vec1.mag3();  // length of the segment

			// Is there an intermediate point that is closest?
			double subLength = vec0.dot3(vec1)/length;
			if (subLength > 0.0d && subLength < length) {
				Vec3d pt = new Vec3d();
				pt.interpolate3(pts.get(i - 1), pts.get(i), subLength/length);
				pt.sub3(point);
				double newDist = pt.mag3();
				if (newDist < dist) {
					dist = newDist;
					pos = totalLength + subLength;
				}
			}

			// Try the node at end of the segment
			totalLength += length;
			vec0 = new Vec3d(point);
			vec0.sub3(pts.get(i));
			double newDist = vec0.mag3();
			if (newDist < dist) {
				dist = newDist;
				pos = totalLength;
			}
		}
		return pos/totalLength;
	}

}
