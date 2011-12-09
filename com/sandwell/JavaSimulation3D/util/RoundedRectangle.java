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

import javax.media.j3d.GeometryArray;
import javax.media.j3d.LineArray;
import javax.media.j3d.TriangleFanArray;
import javax.vecmath.Point2d;

/**
 * RoundedRectangle class based on original implementation.
 */
public class RoundedRectangle extends Shape {

	protected double xRadius;
	protected double yRadius;
	protected double width;
	protected double height;
	protected int numSegments;

	private GeometryArray geometry;
	private int type;

	/**
	 * Constructor to create a new RoundedRectangle with the given parameters.
	 *
	 * @param x
	 * @param y
	 * @param z
	 * @param width
	 * @param height
	 * @param radius
	 * @param type
	 */
	public RoundedRectangle( double x, double y, double z, double width, double height, double radius, int numSegments, int type ) {
		super();
		setName( "RoundedRectangle" );

		super.setCenter( x, y, z );
		this.width = width;
		this.height = height;
		calcRadii();

		setGeometryType( type );
		setNumSegments( numSegments );
		updateGeometry();
	}

	/**
	 * Default constructor with 42 segments.
	 *
	 * @param x
	 * @param y
	 * @param z
	 * @param width
	 * @param height
	 * @param radius
	 * @param type
	 */
	public RoundedRectangle( double x, double y, double z, double width, double height, double radius, int type ) {
		this( x, y, z, width, height, radius, 42, type );
	}

	/**
	 * Constructor with centre at 0,0,0.
	 *
	 * @param width
	 * @param height
	 * @param radius
	 * @param type
	 */
	public RoundedRectangle( double width, double height, double radius, int type ) {
		this( 0.0d, 0.0d, 0.0d, width, height, radius, type );
	}

	/**
	 * Constructor with centre at 0,0,0 and radii of Min(width,height)/4.
	 *
	 * @param width
	 * @param height
	 * @param radius
	 * @param type
	 */
	public RoundedRectangle( double width, double height, int type, String name ) {
		this( width, height, Math.min( width, height ) / 4.0d, type );
		nameOfTheCallingVariable = name;
	}

	/**
	 * Helper to do radii calculations.
	 */
	private void calcRadii() {
		xRadius = Math.min( height, width ) / 2.0;
		yRadius = height / 2.0;
	}

	/**
	 * Creates the initial geomtry.
	 */
	public void createInitialGeometry() {
		numSegments = 42;
		type = Shape.SHAPE_FILLED;
		createNewGeometry();
	}

	/**
	 * Creates new geometry, need to check elsewhere if new geometry is actually
	 * needed.
	 */
	private void createNewGeometry() {
		if( type == Shape.SHAPE_FILLED ) {
			int sc[] = new int[1];
			sc[0] = numSegments + 2;
			geometry = new TriangleFanArray( sc[0], TriangleFanArray.COORDINATES, sc );
			geometry.setCapability( TriangleFanArray.ALLOW_COORDINATE_WRITE );
			shape.setGeometry( geometry );
		}
		else {
			geometry = new LineArray( numSegments * 2, LineArray.COORDINATES );
			geometry.setCapability( LineArray.ALLOW_COORDINATE_WRITE );
			shape.setGeometry( geometry );
		}
		updateGeometry();
	}

	/**
	 * Update the coordinates of the cicle.
	 */
	public void updateGeometry() {

		double rightCorner = (width / 2.0d) - xRadius + centreX;
		double leftCorner = -((width / 2.0d) - xRadius) + centreX;
		double topCorner = (height / 2.0d) + centreY;
		double bottomCorner = -(height / 2.0d) + centreY;
		int numSideSegments = (numSegments - 2) / 2;

		if( type == Shape.SHAPE_FILLED ) {
			int totalVertices = (numSegments + 2) * 3;
			double coords[] = new double[totalVertices];

			// Set the centre and the upper corner coordinates
			coords[0] = centreX;
			coords[1] = centreY;
			coords[2] = centreZ;
			coords[3] = rightCorner;
			coords[4] = topCorner;
			coords[5] = centreZ;
			coords[6] = leftCorner;
			coords[7] = topCorner;
			coords[8] = centreZ;

			// Set the last point to close the roundedRectangle
			coords[totalVertices - 3] = rightCorner;
			coords[totalVertices - 2] = topCorner;
			coords[totalVertices - 1] = centreZ;

			// Fill in the intermediate points for the left rounded side
			int startingIndex = 9;
			for( int i = 1; i < numSideSegments; i++ ) {
				coords[startingIndex + ((i - 1) * 3)] = leftCorner + (Math.cos( (Math.PI / 2) + (Math.PI / numSideSegments * i) ) * xRadius);
				coords[startingIndex + ((i - 1) * 3) + 1] = centreY + (Math.sin( (Math.PI / 2) + (Math.PI / numSideSegments * i) ) * yRadius);
				coords[startingIndex + ((i - 1) * 3) + 2] = centreZ;
			}
			startingIndex += (numSideSegments - 1) * 3;

			//Fill in the lower left and right corners
			coords[startingIndex] = leftCorner;
			coords[startingIndex + 1] = bottomCorner;
			coords[startingIndex + 2] = centreZ;
			coords[startingIndex + 3] = rightCorner;
			coords[startingIndex + 4] = bottomCorner;
			coords[startingIndex + 5] = centreZ;
			startingIndex += 6;

			// Fill in the intermediate points for the right rounded side
			for( int i = 1; i < numSideSegments; i++ ) {
				coords[startingIndex + ((i - 1) * 3)] = rightCorner + (Math.cos( (-Math.PI / 2) + (Math.PI / numSideSegments * i) ) * xRadius);
				coords[startingIndex + ((i - 1) * 3) + 1] = centreY + (Math.sin( (-Math.PI / 2) + (Math.PI / numSideSegments * i) ) * yRadius);
				coords[startingIndex + ((i - 1) * 3) + 2] = centreZ;
			}
			geometry.setCoordinates( 0, coords );
		}
		else {
			int totalVertices = numSegments * 2 * 3;
			double coords[] = new double[totalVertices];

			// Create the top flat part and the first point anchoring the rounded part
			coords[0] = rightCorner;
			coords[1] = topCorner;
			coords[2] = centreZ;
			coords[3] = leftCorner;
			coords[4] = topCorner;
			coords[5] = centreZ;
			coords[6] = leftCorner;
			coords[7] = topCorner;
			coords[8] = centreZ;

			// Set the last point to close the roundedRectangle
			coords[totalVertices - 3] = rightCorner;
			coords[totalVertices - 2] = topCorner;
			coords[totalVertices - 1] = centreZ;

			// Fill in the intermediate points for the left rounded side
			int startingIndex = 9;
			for( int i = 1; i < numSideSegments; i++ ) {
				coords[startingIndex + ((i - 1) * 6)] = leftCorner + (Math.cos( (Math.PI / 2) + (Math.PI / numSideSegments * i) ) * xRadius);
				coords[startingIndex + ((i - 1) * 6) + 1] = centreY + (Math.sin( (Math.PI / 2) + (Math.PI / numSideSegments * i) ) * yRadius);
				coords[startingIndex + ((i - 1) * 6) + 2] = centreZ;
				coords[startingIndex + ((i - 1) * 6) + 3] = leftCorner + (Math.cos( (Math.PI / 2) + (Math.PI / numSideSegments * i) ) * xRadius);
				coords[startingIndex + ((i - 1) * 6) + 4] = centreY + (Math.sin( (Math.PI / 2) + (Math.PI / numSideSegments * i) ) * yRadius);
				coords[startingIndex + ((i - 1) * 6) + 5] = centreZ;
			}
			startingIndex += (numSideSegments - 1) * 6;

			// Fill in the lower left and right corners
			coords[startingIndex] = leftCorner;
			coords[startingIndex + 1] = bottomCorner;
			coords[startingIndex + 2] = centreZ;
			coords[startingIndex + 3] = leftCorner;
			coords[startingIndex + 4] = bottomCorner;
			coords[startingIndex + 5] = centreZ;
			coords[startingIndex + 6] = rightCorner;
			coords[startingIndex + 7] = bottomCorner;
			coords[startingIndex + 8] = centreZ;
			coords[startingIndex + 9] = rightCorner;
			coords[startingIndex + 10] = bottomCorner;
			coords[startingIndex + 11] = centreZ;
			startingIndex += 12;

			// Fill in the intermediate points for the right rounded side
			for( int i = 1; i < numSideSegments; i++ ) {
				coords[startingIndex + ((i - 1) * 6)] = rightCorner + (Math.cos( (-Math.PI / 2) + (Math.PI / numSideSegments * i) ) * xRadius);
				coords[startingIndex + ((i - 1) * 6) + 1] = centreY + (Math.sin( (-Math.PI / 2) + (Math.PI / numSideSegments * i) ) * yRadius);
				coords[startingIndex + ((i - 1) * 6) + 2] = centreZ;
				coords[startingIndex + ((i - 1) * 6) + 3] = rightCorner + (Math.cos( (-Math.PI / 2) + (Math.PI / numSideSegments * i) ) * xRadius);
				coords[startingIndex + ((i - 1) * 6) + 4] = centreY + (Math.sin( (-Math.PI / 2) + (Math.PI / numSideSegments * i) ) * yRadius);
				coords[startingIndex + ((i - 1) * 6) + 5] = centreZ;
			}
			geometry.setCoordinates( 0, coords );
		}
	}

	/**
	 * Set a new type (either filled or outline).
	 *
	 * @param type
	 */
	public void setGeometryType( int type ) {
		if( this.type != type ) {
			this.type = type;
			createNewGeometry();
		}
	}

	/**
	 * Update the number of segments in the RoundedRectangle, recreate the
	 * geometry only if necessary.
	 *
	 * @param segments The number of segments the Geometry should have.
	 */
	public void setNumSegments( int segments ) {
		// Check for at least 6 segments
		if( segments < 6 ) {
			segments = 6;
		}

		if( numSegments != segments ) {
			numSegments = segments;
			createNewGeometry();
		}
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
	 * Set a new width and height for the rectangle.
	 *
	 * @param width
	 * @param height
	 */
	public void setSize( double width, double height ) {
		this.width = width;
		this.height = height;

		// Need to recalculate the xRadius and yRadius
		calcRadii();
		updateGeometry();
	}

	/**
	 * Suggest you work towards eliminating this method.
	 *
	 * @return
	 */
	public Point2d getSize() {
		return new Point2d( width, height );
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
//		// Hieght/Width Information
//		myRoot.add( new javax.swing.tree.DefaultMutableTreeNode( "Height: " + height + " Width: " + width ) );
//
//		// Radius Information
//		myRoot.add( new javax.swing.tree.DefaultMutableTreeNode( "Ideal Radius: " + idealRadius ) );
//		myRoot.add( new javax.swing.tree.DefaultMutableTreeNode( "xRadius: " + xRadius + " yRadius: " + yRadius) );
//
//		// Type of RoundedRectangle
//		String typeString;
//		if( type == SHAPE_FILLED ) {
//			typeString = "Filled";
//		}
//		else if( type == SHAPE_OUTLINE ) {
//			typeString = "Outline";
//		}
//		else {
//			typeString = "Unknown";
//		}
//		myRoot.add( new javax.swing.tree.DefaultMutableTreeNode( "FillType: " + typeString ) );
//
//		// Detail Information
//		myRoot.add( new javax.swing.tree.DefaultMutableTreeNode( "Segments: " + numSegments ) );
//
//		return myRoot;
//	}
}
