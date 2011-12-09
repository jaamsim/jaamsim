/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2004-2011 Ausenco Engineering Canada Inc.
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

import javax.media.j3d.LineArray;


/**
 * Converted previous Arc implementation relying on Shape2D. Creates an arc in
 * the x-y plane with a centre point and a chord of a circle defined by the
 * start and end angles and the interval in degrees for each segment. Arcs are
 * always drawn in counter-clockwise direction, thus it is important in how you
 * pick you start and end angles.
 */
public class Arc extends Shape {

	private LineArray geometry;
	protected double radius;
	protected double startAngle;
	protected double endAngle;
	protected int numSegments; // Number of segments to render in the Arc

	/**
	 * Creates an arc centred at x,y,z with radius from start angle to end angle
	 * in segments steps.
	 *
	 * @param radius
	 * @param x
	 * @param y
	 * @param z
	 * @param start
	 * @param end
	 * @param segments
	 */
	public Arc( double radius, double x, double y, double z, double start, double end, int segments ) {
		super();
		setName( "Arc" );

		this.radius = radius;
		super.setCenter( x, y, z );
		startAngle = start;
		endAngle = end;
		setNumSegments( segments );
	}

	/**
	 * Convenient way to create an arc with radius, startangle, endangle.
	 * Defaults to centre 0,0,0 and 36 segments of detail.
	 *
	 * @param segments
	 */
	public Arc( double radius, double start, double end, String name ) {
		this( radius, 0.0d, 0.0d, 0.0d, start, end, 36 );
		nameOfTheCallingVariable = name;
	}

	/**
	 * Convenient way to create an arc with radius, startangle, endangle.
	 * Defaults to centre 0,0,0 and 36 segments of detail.
	 *
	 * @param segments
	 */
	public Arc( double radius, double x, double y, double start, double end, String name ) {
		this( radius, x, y, 0.0d, start, end, 36 );
		nameOfTheCallingVariable = name;
	}

	/**
	 * Update the number of segments in the Arc, recreate the lineArray only if
	 * necessary.
	 *
	 * @param segments The number of segments the Arc should have.
	 */
	public void setNumSegments( int segments ) {
		// Check for at least one segment
		if( segments < 1 ) {
			segments = 1;
		}

		if( numSegments != segments ) {
			numSegments = segments;
			createNewGeometry();
		}

		// Update for the new level of detail
		updateGeometry();
	}

	/**
	 * Creates the initial geometry.
	 */
	public void createInitialGeometry() {
		numSegments = 20;
		createNewGeometry();
	}

	/**
	 * Helper to wrap the geometry creation step.
	 */
	private void createNewGeometry() {
		geometry = new LineArray( numSegments * 2, LineArray.COORDINATES );
		geometry.setCapability( LineArray.ALLOW_COORDINATE_WRITE );
		shape.setGeometry( geometry );
	}

	/**
	 * Method to calculate the Arc points.
	 */
	private void updateGeometry() {
		double pointArray[] = new double[6 * numSegments];

		double interval = Math.abs( (startAngle - endAngle) / numSegments );

		// Calculate first point exactly
		pointArray[0] = centreX + Math.cos( startAngle ) * radius;
		pointArray[1] = centreY + Math.sin( startAngle ) * radius;
		pointArray[2] = centreZ;

		// Calculate intermediate points
		for( int i = 1; i < numSegments; i++ ) {
			double tempAngle = startAngle + (i * interval);

			// Index of first coordinate in the array at this intermediate point
			int index = ((i - 1) * 6) + 3;
			pointArray[index] = centreX + Math.cos( tempAngle ) * radius;
			pointArray[index + 3] = pointArray[index];
			index++;
			pointArray[index] = centreY + Math.sin( tempAngle ) * radius;
			pointArray[index + 3] = pointArray[index];
			index++;
			pointArray[index] = centreZ;
			pointArray[index + 3] = pointArray[index];
		}

		// Calculate last point exactly
		int lastIndex = (6 * numSegments) - 3;
		pointArray[lastIndex] = centreX + Math.cos( endAngle ) * radius;
		pointArray[lastIndex + 1] = centreY + Math.sin( endAngle ) * radius;
		pointArray[lastIndex + 2] = centreZ;
		geometry.setCoordinates( 0, pointArray );
	}

	/**
	 * Change to a new center point.
	 *
	 * @param x
	 * @param y
	 * @param z
	 */
	public void setCenter( double x, double y, double z ) {
		super.setCenter( x, y, z );
		updateGeometry();
	}

	/**
	 * Sets the radius of the circle to the given parameter.
	 *
	 * @param newRadius
	 */
	public void setRadius( double newRadius ) {
		radius = newRadius;
		updateGeometry();
	}

//	/**
//	 * Method to return a description of the Shape in treenode form.
//	 *
//	 * @return A treenode element which can be added to a GUI.
//	 */
//	public javax.swing.tree.DefaultMutableTreeNode getTreeNode() {
//
//		javax.swing.tree.DefaultMutableTreeNode myRoot = super.getTreeNode();
//
//		// Centre Point Information
//		myRoot.add( new javax.swing.tree.DefaultMutableTreeNode( "Centre x: " + centreX + " y: " + centreY + " z: " + pseudoCenterZ ) );
//
//		// Radius Information
//		myRoot.add( new javax.swing.tree.DefaultMutableTreeNode( "Radius: " + radius ) );
//
//		// Angle Information
//		myRoot.add( new javax.swing.tree.DefaultMutableTreeNode( "Start: " + startAngle + " End: " + endAngle ) );
//
//		// Detail Information
//		myRoot.add( new javax.swing.tree.DefaultMutableTreeNode( "Segments: " + numSegments ) );
//
//		return myRoot;
//	}
}
