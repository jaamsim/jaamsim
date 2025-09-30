/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2012 Ausenco Engineering Canada Inc.
 * Copyright (C) 2020-2024 JaamSim Software Inc.
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

import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.jaamsim.basicsim.Log;
import com.jaamsim.math.Color4d;
import com.jaamsim.math.Mat4d;
import com.jaamsim.math.Plane;
import com.jaamsim.math.Ray;
import com.jaamsim.math.Transform;
import com.jaamsim.math.Vec2d;
import com.jaamsim.math.Vec3d;
import com.jaamsim.math.Vec4d;
import com.jogamp.opengl.GL2GL3;

/**
 * A big pile of static methods that currently don't have a better place to live. All Rendering specific
 * @author matt.chudleigh
 *
 */
public class RenderUtils {

	public static final List<Vec4d> CIRCLE_POINTS;
	public static final List<Vec4d> RECT_POINTS;
	public static final List<Vec4d> TRIANGLE_POINTS;
	public static final List<Vec4d> PENTAGON_POINTS;
	public static final List<Vec4d> HEXAGON_POINTS;
	public static final List<Vec4d> OCTAGON_POINTS;
	public static final List<Vec4d> PENTAGRAM_POINTS;
	public static final List<Vec4d> HEPTAGRAM_POINTS;
	public static final List<Vec4d> OCTAGRAM_POINTS;
	public static final List<Vec4d> ARROW2D_POINTS;

	static {
		CIRCLE_POINTS = getCirclePoints(32);
		// Scale the points down (as JaamSim uses a 1x1 box [-0.5, 0.5] not [-1, 1]
		for (int i = 0; i < CIRCLE_POINTS.size(); ++i) {
			CIRCLE_POINTS.get(i).scale3(0.5);
		}

		RECT_POINTS = new ArrayList<>();
		RECT_POINTS.add(new Vec4d( 0.5,  0.5, 0, 1.0d));
		RECT_POINTS.add(new Vec4d(-0.5,  0.5, 0, 1.0d));
		RECT_POINTS.add(new Vec4d(-0.5, -0.5, 0, 1.0d));
		RECT_POINTS.add(new Vec4d( 0.5, -0.5, 0, 1.0d));

		TRIANGLE_POINTS = new ArrayList<>();
		TRIANGLE_POINTS.add(new Vec4d( 0.5, -0.5, 0, 1.0d));
		TRIANGLE_POINTS.add(new Vec4d( 0.5,  0.5, 0, 1.0d));
		TRIANGLE_POINTS.add(new Vec4d(-0.5,  0.0, 0, 1.0d));

		PENTAGON_POINTS = getStarPoints(0.5, 5, 1);
		HEXAGON_POINTS = getStarPoints(0.5, 6, 1);
		OCTAGON_POINTS = getStarPoints(0.5, 8, 1);

		PENTAGRAM_POINTS = getStarPoints(0.5, 5, 2);
		HEPTAGRAM_POINTS = getStarPoints(0.5, 7, 3);
		OCTAGRAM_POINTS = getStarPoints(0.5, 8, 3);

		ARROW2D_POINTS = new ArrayList<>(7);
		ARROW2D_POINTS.add(new Vec4d( 0.5,  0.0, 0.0, 1.0d));
		ARROW2D_POINTS.add(new Vec4d( 0.1,  0.5, 0.0, 1.0d));
		ARROW2D_POINTS.add(new Vec4d( 0.1,  0.2, 0.0, 1.0d));
		ARROW2D_POINTS.add(new Vec4d(-0.5,  0.2, 0.0, 1.0d));
		ARROW2D_POINTS.add(new Vec4d(-0.5, -0.2, 0.0, 1.0d));
		ARROW2D_POINTS.add(new Vec4d( 0.1, -0.2, 0.0, 1.0d));
		ARROW2D_POINTS.add(new Vec4d( 0.1, -0.5, 0.0, 1.0d));
	}

	// Transform the list of points in place
	public static void transformPointsLocal(Transform trans, List<Vec4d> points, int dummy) {
		for (Vec4d p : points) {
			trans.apply(p, p);
		}
	}

	public static List<Vec4d> transformPoints(Mat4d mat, List<Vec4d> points, int dummy) {
		List<Vec4d> ret = new ArrayList<>();
		for (Vec4d p : points) {
			Vec4d v = new Vec4d(0.0d, 0.0d, 0.0d, 1.0d);
			v.mult4(mat, p);
			ret.add(v);
		}
		return ret;
	}

	public static List<Vec3d> transformPointsWithTrans(Mat4d mat, List<Vec3d> points) {
		List<Vec3d> ret = new ArrayList<>();
		for (Vec3d p : points) {
			Vec3d v = new Vec3d();
			v.multAndTrans3(mat, p);
			ret.add(v);
		}
		return ret;
	}

static void putPointXY(FloatBuffer fb, Vec2d v) {
	fb.put((float)v.x);
	fb.put((float)v.y);
}

static void putPointXYZ(FloatBuffer fb, Vec3d v) {
	fb.put((float)v.x);
	fb.put((float)v.y);
	fb.put((float)v.z);
}

static void putPointXYZW(FloatBuffer fb, Vec4d v) {
	fb.put((float)v.x);
	fb.put((float)v.y);
	fb.put((float)v.z);
	fb.put((float)v.w);
}

static void putColor4b(ByteBuffer bb, Color4d col) {
	bb.put((byte)(col.r * 255.0));
	bb.put((byte)(col.g * 255.0));
	bb.put((byte)(col.b * 255.0));
	bb.put((byte)(col.a * 255.0));
}

static void putMat4dCM(FloatBuffer fb, Mat4d mat) {
	float[] floats = MarshalMat4d(mat);
	fb.put(floats);
}

// Load a float buffer into the openGL buffer given
// Assumes the buffer has not been flipped
static void nioBuffToGL(GL2GL3 gl, Renderer r, int bufferHandle, int itemSize, Buffer buff) {

	buff.flip();
	int size = buff.limit() * itemSize;
	gl.glBindBuffer(GL2GL3.GL_ARRAY_BUFFER, bufferHandle);
	gl.glBufferData(GL2GL3.GL_ARRAY_BUFFER, size, buff, GL2GL3.GL_STATIC_DRAW);
	gl.glBindBuffer(GL2GL3.GL_ARRAY_BUFFER, 0);
	r.usingVRAM(size);

}

	/**
	 * Returns a list of points for a circle in the XY plane at the origin
	 */
	public static ArrayList<Vec4d> getCirclePoints(int numSegments) {
		if (numSegments < 3) {
			return null;
		}
		ArrayList<Vec4d> ret = new ArrayList<>();

		double thetaStep = 2 * Math.PI / numSegments;
		for (int i = 0; i < numSegments + 1; ++i) {
			double theta = i * thetaStep;

			ret.add(new Vec4d(Math.cos(theta), Math.sin(theta), 0, 1.0d));
		}

		return ret;
	}

	/**
	 * Returns a list of points for a polygon or star.
	 * @param radius - distance from the centre of the polygon or star to each vertex
	 * @param n - number of vertices
	 * @param m - number of vertices spanned by each line
	 * @return list of coordinates for the vertices
	 */
	public static ArrayList<Vec4d> getStarPoints(double radius, int n, int m) {
		ArrayList<Vec4d> ret = new ArrayList<>(n + 1);
		double angle = 2 * Math.PI / n * m;
		for (int i = 0; i <= n; i++) {
			double x = -radius * Math.sin(angle * i);
			double y = radius * Math.cos(angle * i);
			ret.add(new Vec4d(x, y, 0, 1));
		}
		return ret;
	}

	/**
	 * Build up a rounded rectangle (similar to the existing stockpiles). Assumes rounding width-wise
	 * @param width
	 * @param height
	 */
	public static ArrayList<Vec4d> getRoundedRectPoints(double width, double height, int numSegments) {
		ArrayList<Vec4d> ret = new ArrayList<>();


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
			ret.add(new Vec4d(xScale*(radius*Math.sin(theta) + fociiPoint), -radius*Math.cos(theta), 0, 1.0d));
		}
		// -X cap
		for (int i = 0; i < numSegments/2 + 1; ++i) {
			double theta = i * thetaStep;
			ret.add(new Vec4d(xScale*(-radius*Math.sin(theta) - fociiPoint), radius*Math.cos(theta), 0, 1.0d));
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
	 */
	public static ArrayList<Vec4d> getArcPoints(double radius, Vec4d center, double startAngle, double endAngle, int numSegments) {
		if (numSegments < 3) {
			return null;
		}

		ArrayList<Vec4d> ret = new ArrayList<>();

		double thetaStep = (startAngle - endAngle) / numSegments;
		for (int i = 0; i < numSegments; ++i) {
			double theta0 = i * thetaStep + startAngle;
			double theta1 = (i+1) * thetaStep + startAngle;

			ret.add(new Vec4d(radius * Math.cos(theta0) + center.x, radius * Math.sin(theta0) + center.y, center.z, 1.0d));
			ret.add(new Vec4d(radius * Math.cos(theta1) + center.x, radius * Math.sin(theta1) + center.y, center.z, 1.0d));
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
	 */
	public static Ray getPickRayForPosition(Renderer.WindowMouseInfo info, int x, int y) {
		CameraInfo cameraInfo = info.cameraInfo;
		int width = info.width;
		int height = info.height;

		double scaledX = Math.round((double)x / info.scaleX);
		double scaledY = Math.round((double)y / info.scaleY);
		double aspectRatio = (double)width / (double)height;
		double normX = 2.0*(scaledX / width) - 1.0;
		double normY = 1.0 - 2.0*(scaledY / height); // In openGL space, y is -1 at the bottom

		return RenderUtils.getViewRay(cameraInfo, aspectRatio, normX, normY);
	}

	/**
	 * Get a Ray representing a line starting at the camera position and projecting through the current mouse pointer
	 * in this window
	 * @param mouseInfo
	 */
	public static Ray getPickRay(Renderer.WindowMouseInfo mouseInfo) {

		if (mouseInfo == null || !mouseInfo.mouseInWindow) {
			return null;
		}

		return getPickRayForPosition(mouseInfo, mouseInfo.x, mouseInfo.y);
	}

	/**
	 * Get a ray from the camera's point of view
	 * @param camInfo
	 * @param aspectRatio
	 * @param x - normalized [-1. 1] screen x coord
	 * @param y - normalized [-1. 1] screen y coord
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
		Vec4d dir = new Vec4d(x * xScale, y * yScale, -1, 0); // This will be normalized by Ray()
		Vec4d start = new Vec4d(0, 0, 0, 1.0d);

		// Temp is the ray in eye-space
		Ray temp = new Ray(start, dir);

		// Transform by the camera transform to get to global space
		return temp.transform(camInfo.trans);

	}

	/**
	 * Return a matrix that is the combination of the transform and non-uniform scale
	 * @param trans
	 * @param scale
	 */
	public static Mat4d mergeTransAndScale(Transform trans, Vec3d scale) {
		Mat4d ret = new Mat4d();
		trans.getMat4d(ret);
		ret.scaleCols3(scale);

		return ret;
	}

	/**
	 * Get the inverse (in Matrix4d form) of the combined Transform and non-uniform scale factors
	 * @param trans
	 * @param scale
	 */
	public static Mat4d getInverseWithScale(Transform trans, Vec3d scale) {
		Transform t = new Transform(trans);
		t.inverse(t);

		Mat4d ret = new Mat4d();
		t.getMat4d(ret);
		Vec3d s = new Vec3d(scale);
		// Prevent dividing by zero
		if (s.x == 0) { s.x = 1; }
		if (s.y == 0) { s.y = 1; }
		if (s.z == 0) { s.z = 1; }
		ret.scaleRows3(new Vec3d(1/s.x, 1/s.y, 1/s.z));

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
	public static Vec4d rayClosePoint(Mat4d rayMatrix, Vec4d worldA, Vec4d worldB) {

		// Create vectors for a and b in ray space
		Vec4d a = new Vec4d(0.0d, 0.0d, 0.0d, 1.0d);
		a.mult4(rayMatrix, worldA);

		Vec4d b = new Vec4d(0.0d, 0.0d, 0.0d, 1.0d);
		b.mult4(rayMatrix, worldB);

		Vec4d ab = new Vec4d(0.0d, 0.0d, 0.0d, 1.0d); // The line A to B

		Vec4d negA = new Vec4d(0.0d, 0.0d, 0.0d, 1.0d); // -1 * A
		negA.sub3(a);

		ab.sub3(b, a);

		double dot = negA.dot2(ab)/ab.magSquare2();
		if (dot < 0) {
			// The closest point is the A point
			return new Vec4d(worldA);
		} else if (dot >= 1) {
			// B is closest
			return new Vec4d(worldB);
		} else {
			// An intermediate point is closest
			Vec4d worldAB = new Vec4d(0.0d, 0.0d, 0.0d, 1.0d);
			worldAB.sub3(worldB, worldA);

			Vec4d ret = new Vec4d(0.0d, 0.0d, 0.0d, 1.0d);
			ret.scale3(dot, worldAB);
			ret.add3(worldA);

			return ret;
		}
	}

	// Get the angle (in rads) this point is off the ray, this is useful for collision cones
	// This will return an negative angle for points behind the start of the ray
	public static double angleToRay(Mat4d rayMatrix, Vec4d worldP) {

		Vec4d p = new Vec4d(0.0d, 0.0d, 0.0d, 1.0d);
		p.mult4(rayMatrix, worldP);

		return Math.atan(p.mag2() / p.z);
	}

	public static Vec4d getGeometricMedian(ArrayList<Vec3d> points) {
		assert(points.size() > 0);

		double minX = points.get(0).x;
		double maxX = points.get(0).x;
		double minY = points.get(0).y;
		double maxY = points.get(0).y;
		double minZ = points.get(0).z;
		double maxZ = points.get(0).z;

		for (Vec3d p : points) {
			if (p.x < minX) minX = p.x;
			if (p.x > maxX) maxX = p.x;

			if (p.y < minY) minY = p.y;
			if (p.y > maxY) maxY = p.y;

			if (p.z < minZ) minZ = p.z;
			if (p.z > maxZ) maxZ = p.z;
		}

		return new Vec4d((minX+maxX)/2, (minY+maxY)/2,  (minZ+maxZ)/2, 1.0d);
	}

	public static Vec3d getPlaneCollisionDiff(Plane p, Ray r0, Ray r1) {
		double r0Dist = p.collisionDist(r0);
		double r1Dist = p.collisionDist(r1);

		if (r0Dist < 0 || r0Dist == Double.POSITIVE_INFINITY ||
		       r1Dist < 0 ||    r1Dist == Double.POSITIVE_INFINITY)
		{
			// The plane is parallel or behind one of the rays...
			return new Vec4d(0.0d, 0.0d, 0.0d, 1.0d); // Just ignore it for now...
		}

		// The points where the previous pick ended and current position. Collision is with the entity's XY plane
		Vec3d r0Point = r0.getPointAtDist(r0Dist);
		Vec3d r1Point = r1.getPointAtDist(r1Dist);

		Vec3d ret = new Vec3d();
		ret.sub3(r0Point, r1Point);
		return ret;
	}

	/**
	 * This is scratch space for matrix marshaling
	 */
	private static final float[] MAT_MARSHAL = new float[16];
	/**
	 * Marshal a Mat4d into a static scratch space, this should only be called by the render thread, but I'm not
	 * putting a guard here for performance reasons. This returns a colum major float array
	 * @param mat
	 */
	public static float[] MarshalMat4d(Mat4d mat) {
		MAT_MARSHAL[ 0] = (float)mat.d00;
		MAT_MARSHAL[ 1] = (float)mat.d10;
		MAT_MARSHAL[ 2] = (float)mat.d20;
		MAT_MARSHAL[ 3] = (float)mat.d30;

		MAT_MARSHAL[ 4] = (float)mat.d01;
		MAT_MARSHAL[ 5] = (float)mat.d11;
		MAT_MARSHAL[ 6] = (float)mat.d21;
		MAT_MARSHAL[ 7] = (float)mat.d31;

		MAT_MARSHAL[ 8] = (float)mat.d02;
		MAT_MARSHAL[ 9] = (float)mat.d12;
		MAT_MARSHAL[10] = (float)mat.d22;
		MAT_MARSHAL[11] = (float)mat.d32;

		MAT_MARSHAL[12] = (float)mat.d03;
		MAT_MARSHAL[13] = (float)mat.d13;
		MAT_MARSHAL[14] = (float)mat.d23;
		MAT_MARSHAL[15] = (float)mat.d33;
		return MAT_MARSHAL;

	}

	/**
	 * Marshal a 4x4 matrix into 'array', filling the 16 values starting at 'offset'
	 * @param mat
	 * @param array
	 * @param offset
	 */
	public static void MarshalMat4dToArray(Mat4d mat, float[] array, int offset) {
		array[ 0 + offset] = (float)mat.d00;
		array[ 1 + offset] = (float)mat.d10;
		array[ 2 + offset] = (float)mat.d20;
		array[ 3 + offset] = (float)mat.d30;

		array[ 4 + offset] = (float)mat.d01;
		array[ 5 + offset] = (float)mat.d11;
		array[ 6 + offset] = (float)mat.d21;
		array[ 7 + offset] = (float)mat.d31;

		array[ 8 + offset] = (float)mat.d02;
		array[ 9 + offset] = (float)mat.d12;
		array[10 + offset] = (float)mat.d22;
		array[11 + offset] = (float)mat.d32;

		array[12 + offset] = (float)mat.d03;
		array[13 + offset] = (float)mat.d13;
		array[14 + offset] = (float)mat.d23;
		array[15 + offset] = (float)mat.d33;

	}

	private static final double SMALL_SCALE = 0.000001;

	public static Vec3d fixupScale(Vec3d inScale) {
		Vec3d ret = new Vec3d(inScale);

		if (ret.x <= 0.0) ret.x = SMALL_SCALE;
		if (ret.y <= 0.0) ret.y = SMALL_SCALE;
		if (ret.z <= 0.0) ret.z = SMALL_SCALE;

		return ret;
	}

	private static boolean isValidExtension(String fileName) {
		int idx = fileName.lastIndexOf('.');
		if (idx < 0)
			return false;

		String ext = fileName.substring(idx + 1).trim().toUpperCase();
		if (ext.equals("DAE") || ext.equals("OBJ") || ext.equals("JSM") || ext.equals("JSB"))
			return true;

		return false;
	}

	public static MeshProtoKey FileNameToMeshProtoKey(URI fileURI) {
		try {
			URL meshURL = fileURI.toURL();

			String fileString = fileURI.toString();
			String ext = fileString.substring(fileString.length() - 4,
					fileString.length());

			if (ext.toUpperCase().equals(".ZIP")) {
				// This is a zip, use a zip stream to actually pull out
				// the .dae file
				try (ZipInputStream zipIS = new ZipInputStream(meshURL.openStream())) {
					// Loop through zipEntries
					for (ZipEntry ze; (ze = zipIS.getNextEntry()) != null;) {

						String entryName = ze.getName();
						if (!isValidExtension(entryName))
							continue;

						// This zipEntry is a collada file, no need to look
						// any further

						// Abuse URI a bit to URI-encode the filename
						URI encEntryURI = new URI(null, entryName, null);
						String encEntry = encEntryURI.getRawSchemeSpecificPart();

						meshURL = new URL("jar:" + meshURL + "!/"
								+ encEntry);
						break;
					}
				}
			}

			return new MeshProtoKey(meshURL.toURI());
		} catch (MalformedURLException e) {
			Log.logException(e);
			assert (false);
		} catch (IOException e) {
			assert (false);
		} catch (URISyntaxException e) {
			Log.logException(e);
			assert (false);
		}
		return null;
	}

	/**
	 * Get the difference in Z height from projecting the two rays onto the vertical plane
	 * defined at the provided centerPoint
	 * @param centerPoint
	 * @param currentRay
	 * @param lastRay
	 */
	public static double getZDiff(Vec3d centerPoint, Ray currentRay, Ray lastRay) {

		// Create a plane, orthogonal to the camera, but parallel to the Z axis
		Vec4d normal = new Vec4d(currentRay.getDirRef());
		normal.z = 0;
		normal.normalize3();

		double planeDist = centerPoint.dot3(normal);

		Plane plane = new Plane(normal, planeDist);

		double currentDist = plane.collisionDist(currentRay);
		double lastDist = plane.collisionDist(lastRay);

		if (currentDist < 0 || currentDist == Double.POSITIVE_INFINITY ||
		       lastDist < 0 ||    lastDist == Double.POSITIVE_INFINITY)
		{
			// The plane is parallel or behind one of the rays...
			return 0; // Just ignore it for now...
		}

		// The points where the previous pick ended and current position. Collision is with the entity's XY plane
		Vec3d currentPoint = currentRay.getPointAtDist(currentDist);
		Vec3d lastPoint = lastRay.getPointAtDist(lastDist);

		return currentPoint.z - lastPoint.z;
	}

	public static int[] stringToCodePoints(String s) {
		int numCodePoints = s.codePointCount(0, s.length());
		int[] ret = new int[numCodePoints];

		for (int i = 0; i < numCodePoints; ++i) {
			ret[i] = s.codePointAt(s.offsetByCodePoints(0, i));
		}
		return ret;
	}
}
