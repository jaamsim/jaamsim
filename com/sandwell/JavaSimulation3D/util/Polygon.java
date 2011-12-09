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
import javax.media.j3d.TriangleStripArray;
import javax.vecmath.Point3f;

/**
 * Based on original Polygon implementation but not as flexible. Only will
 * accept concave polygon shapes and always is built as a trianglefanarray or a
 * linearray.  If a more general solution is needed, consider ressurecting the old
 * code.
 */
public class Polygon extends Shape {

	protected int numSegments;
	protected double coordList[];

	private GeometryArray geometry;
	private int type;

	/**
	 * Create a polygon with the given points, in order given.
	 *
	 * @param points
	 * @param type
	 */
	public Polygon( double[] points, int type, String name ) {
		super();
		setName( "Polygon" );
		nameOfTheCallingVariable = name;

		setGeometryType( type );
		setPoints( points );
	}

	/**
	 * Creates the initial geomtry.
	 */
	public void createInitialGeometry() {
		numSegments = 3;
		coordList = new double[9];
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
			sc[0] = numSegments;
			geometry = new TriangleFanArray( sc[0], TriangleFanArray.COORDINATES, sc );
			geometry.setCapability( TriangleFanArray.ALLOW_COORDINATE_WRITE );
			shape.setGeometry( geometry );
		}
		else if( type == Shape.SHAPE_FILLED_STRIP ){
			//int sc[] = { numSegments };
			int sc[] = new int[1];
			sc[0] = numSegments;
			geometry = new TriangleStripArray( sc[0], TriangleStripArray.COORDINATES, sc );
			geometry.setCapability( TriangleStripArray.ALLOW_COORDINATE_WRITE);
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
	 * Updates the polygon geometry.
	 */
	public void updateGeometry() {
		double zPush = 0.001 * getLayer();
		if( type == Shape.SHAPE_FILLED ) {
			double temp[] = new double[coordList.length];
			for( int i = 0; i < numSegments; i ++ ) {
				temp[i * 3] = coordList[i * 3];
				temp[(i * 3) + 1] = coordList[(i * 3) + 1];
				temp[(i * 3) + 2] = coordList[(i * 3) + 2] + zPush;
			}
			geometry.setCoordinates( 0, temp );
		}
		else if ( type == Shape.SHAPE_FILLED_STRIP ) {
			double temp[] = new double[coordList.length];
			for( int i = 0; i < numSegments; i ++ ) {
				temp[i * 3] = coordList[i * 3];
				temp[(i * 3) + 1] = coordList[(i * 3) + 1];
				temp[(i * 3) + 2] = coordList[(i * 3) + 2] + zPush;
			}
			geometry.setCoordinates( 0, temp );
		}
		else {
			double lineCoordList[] = new double[coordList.length * 2];

			// Set first and last point
			lineCoordList[0] = coordList[0];
			lineCoordList[1] = coordList[1];
			lineCoordList[2] = coordList[2] + zPush;
			lineCoordList[lineCoordList.length - 3] = coordList[0];
			lineCoordList[lineCoordList.length - 2] = coordList[1];
			lineCoordList[lineCoordList.length - 1] = coordList[2] + zPush;

			// Set intermediate points
			// End point of segments 'i' and start point of segment 'i+1'
			// Segments number starting at 1
			for( int i = 1; i < numSegments; i++ ) {
				int indexLineOffset = i * 6;
				int indexCoordOffset = i * 3;
				lineCoordList[indexLineOffset - 3] = coordList[indexCoordOffset];
				lineCoordList[indexLineOffset - 2] = coordList[indexCoordOffset + 1];
				lineCoordList[indexLineOffset - 1] = coordList[indexCoordOffset + 2] + zPush;
				lineCoordList[indexLineOffset] = coordList[indexCoordOffset];
				lineCoordList[indexLineOffset + 1] = coordList[indexCoordOffset + 1];
				lineCoordList[indexLineOffset + 2] = coordList[indexCoordOffset + 2] + zPush;
			}
			geometry.setCoordinates( 0, lineCoordList );
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
	 * Update the number of segments in the polygon, recreate the
	 * geometry only if necessary.
	 *
	 * @param segments The number of segments the Geometry should have.
	 */
	private void setNumSegments( int segments ) {
		// Check for at least 3 segments
		if( segments < 3 ) {
			segments = 3;
		}

		if( numSegments != segments ) {
			numSegments = segments;
			createNewGeometry();
		}
	}

	/**
	 * Sets the given points, adjusting the polygon size if more or less points
	 * are given.
	 *
	 * @param points
	 */
	public void setPoints( double[] points ) {
		int numTriplets = points.length / 3;
		int numValues = numTriplets * 3;

		if( numTriplets < 3 ) {
			numTriplets = 3;
			coordList = new double[9];
			for( int i = 0; i < numValues; i++ ) {
				coordList[i] = points[i];
			}
			for( int i = numValues; i < 9; i++ ) {
				coordList[i] = 0.0d;
			}
			setNumSegments( numTriplets );
			updateGeometry();
		}
		else {
			coordList = new double[numValues];
			for( int i = 0; i < numValues; i++ ) {
				coordList[i] = points[i];
			}
			setNumSegments( numTriplets );
			updateGeometry();
		}
	}

	public void setPoints( Point3f pointArray[] ) {
		double[] temp = new double[pointArray.length*3];
		for( int i = 0; i < pointArray.length; i++ ) {
			temp[i * 3] = pointArray[i].x;
			temp[(i * 3) + 1] = pointArray[i].y;
			temp[(i * 3) + 2] = pointArray[i].z;
		}
		setPoints( temp );
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
//		javax.swing.tree.DefaultMutableTreeNode pointsRoot = new javax.swing.tree.DefaultMutableTreeNode( "Points" );
//
//		// Add all the points to the returned treenode
//		for( int i = 0; i < numSegments; i ++ ) {
//			int tempIndex = i * 3;
//			pointsRoot.add( new javax.swing.tree.DefaultMutableTreeNode( "P: " + i + "-- x: " + coordList[tempIndex] + " y: " + coordList[tempIndex + 1] + " z: " + coordList[tempIndex + 2] ) );
//		}
//		myRoot.add( pointsRoot );
//
//		return myRoot;
//	}
}
