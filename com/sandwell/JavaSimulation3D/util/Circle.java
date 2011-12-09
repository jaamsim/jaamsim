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

/**
 * Circle class based on original implementation.
 */
public class Circle extends Shape {

	private GeometryArray geometry;
	private int type;

	protected double radius;
	protected int numSegments;

	/**
	 * Creates a circle at centre x,y,z with radius and in a number of segments.
	 *
	 * @param x
	 * @param y
	 * @param z
	 * @param radius
	 * @param numSegments
	 * @param type
	 */
	public Circle( double x, double y, double z, double radius, int numSegments, int type ) {
		super();
		setName( "Circle" );

		super.setCenter( x, y, z );
		this.radius = radius;
		setGeometryType( type );
		setNumSegments( numSegments );
		updateGeometry();
	}

	/**
	 * Default circle at centre 0,0,0.
	 *
	 * @param radius
	 * @param type
	 */
	public Circle( double radius, int type, String name ) {
		this( 0.0d, 0.0d, 0.0d, radius, 36, type );
		nameOfTheCallingVariable = name;
	}

	/**
	 * Default circle at centre 0,0,0 with numSegments.
	 *
	 * @param radius
	 * @param type
	 */
	public Circle( double radius, int numSegments, int type, String name ) {
		this( 0.0d, 0.0d, 0.0d, radius, numSegments, type );
		nameOfTheCallingVariable = name;
	}

	/**
	 * Creates the initial geomtry.
	 */
	public void createInitialGeometry() {
		numSegments = 36;
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
		if( type == Shape.SHAPE_FILLED ) {
			int totalVertices = (numSegments + 2) * 3;
			double coords[] = new double[totalVertices];

			// Set the centre and the last point at the far right
			coords[0] = centreX;
			coords[1] = centreY;
			coords[2] = centreZ;
			coords[3] = centreX + radius;
			coords[4] = centreY;
			coords[5] = centreZ;
			coords[totalVertices - 3] = centreX + radius;
			coords[totalVertices - 2] = centreY;
			coords[totalVertices - 1] = centreZ;

			// Fill in the intermediate points
			for( int i = 1; i < numSegments; i++ ) {
				coords[(i * 3) + 3] = centreX + (Math.cos( 2 * Math.PI / numSegments * i ) * radius);
				coords[(i * 3) + 4] = centreY + (Math.sin( 2 * Math.PI / numSegments * i ) * radius);
				coords[(i * 3) + 5] = centreZ;
			}
			geometry.setCoordinates( 0, coords );
		}
		else {
			int totalVertices = numSegments * 2 * 3;
			double coords[] = new double[totalVertices];

			// Set the First/last point
			coords[0] = centreX + radius;
			coords[1] = centreY;
			coords[2] = centreZ;
			coords[totalVertices - 3] = centreX + radius;
			coords[totalVertices - 2] = centreY;
			coords[totalVertices - 1] = centreZ;

			// Fill in the intermediate points
			for( int i = 1; i < numSegments; i++ ) {
				int vertexOffset = (i * 6) - 3;
				coords[vertexOffset] = centreX + (Math.cos( 2 * Math.PI / numSegments * i ) * radius);
				coords[vertexOffset + 1] = centreY + (Math.sin( 2 * Math.PI / numSegments * i ) * radius);
				coords[vertexOffset + 2] = centreZ;
				coords[vertexOffset + 3] = centreX + (Math.cos( 2 * Math.PI / numSegments * i ) * radius);
				coords[vertexOffset + 4] = centreY + (Math.sin( 2 * Math.PI / numSegments * i ) * radius);
				coords[vertexOffset + 5] = centreZ;
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
	 * Update the number of segments in the Circle, recreate the geometry only if
	 * necessary.
	 *
	 * @param segments The number of segments the Circle should have.
	 */
	public void setNumSegments( int segments ) {
		// Check for at least three segments
		if( segments < 3 ) {
			segments = 3;
		}

		if( numSegments != segments ) {
			numSegments = segments;
			createNewGeometry();
		}
	}

	public void setCenterRadius(double x, double y, double z, double newRadius) {
		super.setCenter(x, y, z);
		radius = newRadius;
		updateGeometry();
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
//		// Type of Circle
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
