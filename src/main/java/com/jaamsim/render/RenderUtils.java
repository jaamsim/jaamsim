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

import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import javax.vecmath.Vector3d;

import com.jaamsim.math.Matrix4d;
import com.jaamsim.math.Plane;
import com.jaamsim.math.Ray;
import com.jaamsim.math.Transform;
import com.jaamsim.math.Vector4d;

/**
 * A big pile of static methods that currently don't have a better place to live. All Rendering specific
 * @author matt.chudleigh
 *
 */
public class RenderUtils {

	public static List<Vector4d> CIRCLE_POINTS;
	public static List<Vector4d> RECT_POINTS;
	public static List<Vector4d> TRIANGLE_POINTS;

	// Transform the list of points in place
	public static void transformPointsLocal(Transform trans, List<Vector4d> points) {
		for (Vector4d p : points) {
			trans.apply(p, p);
		}
	}

	// Transform into a new list
	public static List<Vector4d> transformPoints(Transform trans, List<Vector4d> points) {
		List<Vector4d> ret = new ArrayList<Vector4d>();
		for (Vector4d p : points) {
			Vector4d v = new Vector4d();
			trans.apply(p, v);
			ret.add(v);
		}
		return ret;
	}

	// As above, but with a matrix
	public static void transformPointsLocal(Matrix4d mat, List<Vector4d> points) {
		for (Vector4d p : points) {
			mat.mult(p,  p);
		}
	}

	public static List<Vector4d> transformPoints(Matrix4d mat, List<Vector4d> points) {
		List<Vector4d> ret = new ArrayList<Vector4d>();
		for (Vector4d p : points) {
			Vector4d v = new Vector4d();
			mat.mult(p, v);
			ret.add(v);
		}
		return ret;
	}

	/**
	 * Returns a list of points for a circle in the XY plane at the origin
	 * @return
	 */
	public static ArrayList<Vector4d> getCirclePoints(int numSegments) {
		if (numSegments < 3) {
			return null;
		}
		ArrayList<Vector4d> ret = new ArrayList<Vector4d>();

		double thetaStep = 2 * Math.PI / numSegments;
		for (int i = 0; i < numSegments + 1; ++i) {
			double theta = i * thetaStep;

			ret.add(new Vector4d(Math.cos(theta), Math.sin(theta), 0));
		}

		return ret;
	}

	public static void init() {
		CIRCLE_POINTS = getCirclePoints(32);
		// Scale the points down (as JaamSim uses a 1x1 box [-0.5, 0.5] not [-1, 1]
		transformPointsLocal(Matrix4d.ScaleMatrix(0.5), CIRCLE_POINTS);

		RECT_POINTS = new ArrayList<Vector4d>();
		RECT_POINTS.add(new Vector4d( 0.5,  0.5, 0));
		RECT_POINTS.add(new Vector4d(-0.5,  0.5, 0));
		RECT_POINTS.add(new Vector4d(-0.5, -0.5, 0));
		RECT_POINTS.add(new Vector4d( 0.5, -0.5, 0));

		TRIANGLE_POINTS = new ArrayList<Vector4d>();
		TRIANGLE_POINTS.add(new Vector4d( 0.5, -0.5, 0));
		TRIANGLE_POINTS.add(new Vector4d( 0.5,  0.5, 0));
		TRIANGLE_POINTS.add(new Vector4d(-0.5,  0.0, 0));

	}

	/**
	 * Build up a rounded rectangle (similar to the existing stockpiles). Assumes rounding width-wise
	 * @param width
	 * @param height
	 * @return
	 */
	public static ArrayList<Vector4d> getRoundedRectPoints(double width, double height, int numSegments) {
		ArrayList<Vector4d> ret = new ArrayList<Vector4d>();


		// Create semi circles on the ends
		double xScale = 1;
		double radius = height/2;
		double fociiPoint = width/2 - radius;

		// If the width is too small, the focii are at 0, and we scale in the x component of the curvature
		if (width < height) {
			xScale = width/height;
			fociiPoint = 0;
		}

		double thetaStep = 2 * Math.PI / numSegments;
		// +X cap
		for (int i = 0; i < numSegments/2 + 1; ++i) {
			double theta = i * thetaStep;
			ret.add(new Vector4d(xScale*(radius*Math.sin(theta) + fociiPoint), -radius*Math.cos(theta), 0));
		}
		// -X cap
		for (int i = 0; i < numSegments/2 + 1; ++i) {
			double theta = i * thetaStep;
			ret.add(new Vector4d(xScale*(-radius*Math.sin(theta) - fociiPoint), radius*Math.cos(theta), 0));
		}


		return ret;
	}

	/**
	 * Return a number of points that can draw an arc. This returns pairs for lines (unlike the getCirclePoints() which
	 * returns points for a line-strip).
	 * @param radius
	 * @param center
	 * @param startAngle
	 * @param endAngle
	 * @param numSegments
	 * @return
	 */
	public static ArrayList<Vector4d> getArcPoints(double radius, Vector4d center, double startAngle, double endAngle, int numSegments) {
		if (numSegments < 3) {
			return null;
		}

		ArrayList<Vector4d> ret = new ArrayList<Vector4d>();

		double thetaStep = (startAngle - endAngle) / numSegments;
		for (int i = 0; i < numSegments; ++i) {
			double theta0 = i * thetaStep + startAngle;
			double theta1 = (i+1) * thetaStep + startAngle;

			ret.add(new Vector4d(radius * Math.cos(theta0) + center.x(), radius * Math.sin(theta0) + center.y(), 0));
			ret.add(new Vector4d(radius * Math.cos(theta1) + center.x(), radius * Math.sin(theta1) + center.y(), 0));
		}

		return ret;

	}

	/**
	 *
	 * @param cameraInfo
	 * @param x - x coord in window space
	 * @param y - y coord in window space
	 * @param width - window width
	 * @param height - window height
	 * @return
	 */
	public static Ray getPickRayForPosition(CameraInfo cameraInfo, int x, int y, int width, int height) {

		double aspectRatio = (double)width / (double)height;
		double normX = 2.0*((double)x / (double)width) - 1.0;
		double normY = 1.0 - 2.0*((double)y / (double)height); // In openGL space, y is -1 at the bottom

		return RenderUtils.getViewRay(cameraInfo, aspectRatio, normX, normY);
	}

	/**
	 * Get a Ray representing a line starting at the camera position and projecting through the current mouse pointer
	 * in this window
	 * @param mouseInfo
	 * @return
	 */
	public static Ray getPickRay(Renderer.WindowMouseInfo mouseInfo) {

		if (mouseInfo == null || !mouseInfo.mouseInWindow) {
			return null;
		}

		return getPickRayForPosition(mouseInfo.cameraInfo,
		                             mouseInfo.x,
		                             mouseInfo.y,
		                             mouseInfo.width,
		                             mouseInfo.height);
	}

	/**
	 * Get a ray from the camera's point of view
	 * @param camInfo
	 * @param aspectRatio
	 * @param x - normalized [-1. 1] screen x coord
	 * @param y - normalized [-1. 1] screen y coord
	 * @return
	 */
	public static Ray getViewRay(CameraInfo camInfo, double aspectRatio, double x, double y) {

		double yScale, xScale;
		if (aspectRatio > 1) {
			xScale = Math.tan(camInfo.FOV/2);
			yScale = xScale / aspectRatio;
		} else {
			yScale = Math.tan(camInfo.FOV/2);
			xScale = yScale * aspectRatio;
		}
		Vector4d dir = new Vector4d(x * xScale, y * yScale, -1, 0); // This will be normalized by Ray()
		Vector4d start = new Vector4d(0, 0, 0);

		// Temp is the ray in eye-space
		Ray temp = new Ray(start, dir);

		// Transform by the camera transform to get to global space
		return temp.transform(camInfo.trans);

	}

	/**
	 * Return a matrix that is the combination of the transform and non-uniform scale
	 * @param trans
	 * @param scale
	 * @return
	 */
	public static Matrix4d mergeTransAndScale(Transform trans, Vector4d scale) {
		Matrix4d ret = new Matrix4d();
		trans.getMatrix(ret);
		ret.mult(Matrix4d.ScaleMatrix(scale), ret);

		return ret;
	}

	/**
	 * Get the inverse (in Matrix4d form) of the combined Transform and non-uniform scale factors
	 * @param trans
	 * @param scale
	 * @return
	 */
	public static Matrix4d getInverseWithScale(Transform trans, Vector4d scale) {
		Transform t = new Transform(trans);
		t.inverse(t);

		Matrix4d ret = new Matrix4d();
		t.getMatrix(ret);
		Vector4d s = new Vector4d(scale);
		// Prevent dividing by zero
		if (s.data[0] == 0) { s.data[0] = 1; }
		if (s.data[1] == 0) { s.data[1] = 1; }
		if (s.data[2] == 0) { s.data[2] = 1; }
		Matrix4d.ScaleMatrix(1/s.x(), 1/s.y(), 1/s.z()).mult(ret, ret);

		return ret;

	}

	/**
	 * Scale an awt BufferedImage to a given resolution
	 * @param img
	 * @param newWidth
	 * @param newHeight
	 * @return a new BufferedImage of the appropriate size
	 */
	public static BufferedImage scaleToRes(BufferedImage img, int newWidth, int newHeight) {
		int oldWidth = img.getWidth();
		int oldHeight = img.getHeight();
		BufferedImage ret = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_ARGB);
		AffineTransform at = new AffineTransform();
		at.scale((double)newWidth/(double)oldWidth, (double)newHeight/(double)oldHeight);
		AffineTransformOp scaleOp =
		   new AffineTransformOp(at, AffineTransformOp.TYPE_BILINEAR);
		ret = scaleOp.filter(img, ret);
		return ret;
	}

	// Get the closest point in a line segment to a ray
	public static Vector4d rayClosePoint(Matrix4d rayMatrix, Vector4d worldA, Vector4d worldB) {

		// Create vectors for a and b in ray space
		Vector4d a = new Vector4d();
		rayMatrix.mult(worldA, a);

		Vector4d b = new Vector4d();
		rayMatrix.mult(worldB, b);

		Vector4d ab = new Vector4d(); // The line A to B
		Vector4d negA = new Vector4d(); // -1 * A

		b.sub3(a, ab);
		Vector4d.ORIGIN.sub3(a, negA);

		double dot = negA.dot2(ab)/ab.magSquared2();
		if (dot < 0) {
			// The closest point is the A point
			return new Vector4d(worldA);
		} else if (dot >= 1) {
			// B is closest
			return new Vector4d(worldB);
		} else {
			// An intermediate point is closest
			Vector4d worldAB = new Vector4d();
			worldB.sub3(worldA, worldAB);

			Vector4d ret = new Vector4d();
			worldAB.scale3(dot, ret);

			ret.addLocal3(worldA);

			return ret;
		}
	}

	// Get the angle (in rads) this point is off the ray, this is useful for collision cones
	// This will return an negative angle for points behind the start of the ray
	public static double angleToRay(Matrix4d rayMatrix, Vector4d worldP) {

		Vector4d p = new Vector4d();
		rayMatrix.mult(worldP, p);

		return Math.atan(p.mag2() / p.z());
	}

	public static Vector4d getGeometricMedian(ArrayList<Vector3d> points) {
		assert(points.size() > 0);

		double minX = points.get(0).x;
		double maxX = points.get(0).x;
		double minY = points.get(0).y;
		double maxY = points.get(0).y;
		double minZ = points.get(0).z;
		double maxZ = points.get(0).z;

		for (Vector3d p : points) {
			if (p.x < minX) minX = p.x;
			if (p.x > maxX) maxX = p.x;

			if (p.y < minY) minY = p.y;
			if (p.y > maxY) maxY = p.y;

			if (p.z < minZ) minZ = p.z;
			if (p.z > maxZ) maxZ = p.z;
		}

		return new Vector4d((minX+maxX)/2, (minY+maxY)/2,  (minZ+maxZ)/2);
	}

	public static Vector4d getPlaneCollisionDiff(Plane p, Ray r0, Ray r1) {
		double r0Dist = p.collisionDist(r0);
		double r1Dist = p.collisionDist(r1);

		if (r0Dist < 0 || r0Dist == Double.POSITIVE_INFINITY ||
		       r1Dist < 0 ||    r1Dist == Double.POSITIVE_INFINITY)
		{
			// The plane is parallel or behind one of the rays...
			return new Vector4d(); // Just ignore it for now...
		}

		// The points where the previous pick ended and current position. Collision is with the entity's XY plane
		Vector4d r0Point = r0.getPointAtDist(r0Dist);
		Vector4d r1Point = r1.getPointAtDist(r1Dist);

		Vector4d ret = new Vector4d();
		r0Point.sub3(r1Point, ret);
		return ret;
	}
}
