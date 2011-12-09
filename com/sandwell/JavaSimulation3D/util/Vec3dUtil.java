/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2009-2011 Ausenco Engineering Canada Inc.
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
package com.sandwell.JavaSimulation3D.util;

import java.util.ArrayList;

import javax.vecmath.Vector3d;

import com.sandwell.JavaSimulation.Vector;

public class Vec3dUtil {

	public static Vector3d min( Vector3d point1, Vector3d point2 ) {
		return new Vector3d( Math.min( point1.x, point2.x ), Math.min( point1.y, point2.y ), Math.min( point1.z, point2.z ) );
	}

	public static Vector3d max( Vector3d point1, Vector3d point2 ) {
		return new Vector3d( Math.max( point1.x, point2.x ), Math.max( point1.y, point2.y ), Math.max( point1.z, point2.z ) );
	}

	public static Vector3d sub( Vector3d point1, Vector3d point2 ) {
		return new Vector3d( point1.x - point2.x, point1.y - point2.y, point1.z - point2.z );
	}

	public static Vector3d add( Vector3d point1, Vector3d point2 ) {
		return new Vector3d( point1.x + point2.x, point1.y + point2.y, point1.z + point2.z );
	}

	public static double distance( Vector3d from, Vector3d to ) {
		return ( Math.sqrt((from.x - to.x) * (from.x - to.x)
						 + (from.y - to.y) * (from.y - to.y)) );
	}

	/**
	 * for the list of Vector3d points find a parallel line on both side with width apart
	 * @param width ( width of the parallel line )
	 * @param points ( the points in the Signal Block)
	 * @return
	 */
	public static Vector findParallelPointWithWidth(double width, ArrayList<Vector3d> points) {

		double unitVectorX = 0;
		double unitVectorY = 0;

		double lengthOfSegment = 0;

		Vector3d pointTop;
		Vector3d pointBottom;

		Vector newScreenPointsTop = new Vector();
		Vector newScreenPointsBottom = new Vector();
		Vector newScreenPoints = new Vector(2);

		Vector3d currentPoint= new Vector3d();
		Vector3d adjustVector = new Vector3d();

		// find the vector between the start and the end point
		adjustVector.sub(points.get(points.size() - 1), points.get(0));

		// find the unit vector this last point to current point
		lengthOfSegment = Math.hypot(adjustVector.getX(), adjustVector.getY());
		unitVectorX = (adjustVector.getX() / lengthOfSegment);
		unitVectorY = (adjustVector.getY() / lengthOfSegment);

		for( int i = 0; i < points.size(); i++ ){
			pointTop = new Vector3d();
			pointBottom = new Vector3d();
			currentPoint.set(points.get(i));
			pointTop.setX( -1*unitVectorY*width + currentPoint.getX() );
			pointTop.setY( unitVectorX*width + currentPoint.getY() );
			pointTop.setZ( 0 );
			newScreenPointsTop.add(pointTop);

			pointBottom.setX( unitVectorY*width + currentPoint.getX() );
			pointBottom.setY( -1*unitVectorX*width + currentPoint.getY() );
			pointBottom.setZ( 0 );
			newScreenPointsBottom.add( pointBottom );
		}

		newScreenPoints.add( newScreenPointsTop );
		newScreenPoints.add( newScreenPointsBottom );

		return newScreenPoints;
	}


	/**
	 * Return the closest point to the given point p on the line between a and b
	 */
	public static Vector3d getClosestPointTo_OnLineBetween( Vector3d p, Vector3d a, Vector3d b ) {

		// Determine the vector from point a to point p
	    Vector3d ap = Vec3dUtil.sub( p, a );

	    // Determine the vector from point a to point b
	    Vector3d ab = Vec3dUtil.sub( b, a );

	    // Determine t as the (projection of ap onto ab)/(length of ab).
	    // The projection of ap onto ab is equal to (dot product of ap and ab)/(length of ab)
	    double t = (ap.x*ab.x + ap.y*ab.y + ap.z*ab.z) / ab.lengthSquared();

	    if( t >= 1.0 ) {
	    	return new Vector3d( b );
	    }
	    else if( t <= 0.0 ) {
	    	return new Vector3d( a );
	    }

	    // Return a + (ab)t
	    ab.scaleAdd( t, a );
	    return ab;
    }
}
