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

import javax.media.j3d.QuadArray;
import javax.vecmath.Point3d;

/**
 * Creates a number of rectangles link in an array and all displayed at the same
 * layer with the same attributes.
 */
public class RectangleArray extends Shape {

	protected int numRectangles;
	protected QuadArray geometry;
	private int type;

	protected double[] recCoords;

	/**
	 * Creates a group of rectangles with corners determined by the input points.
	 *
	 * @param points
	 * @param type
	 */
	public RectangleArray( double[] points, int type, String name ) {
		super();
		setName( "RectangleArray" );
		nameOfTheCallingVariable = name;

		this.type = type;
		setPoints( points );

		if( this.type == Shape.SHAPE_FILLED ) {
			setPolygonStyle( Shape.POLYGON_FILL_FRONT );
		}
		else {
			setPolygonStyle( Shape.POLYGON_LINES );
		}
	}

	/**
	 * Creates the backing QuadArray for the RectangleArray.
	 */
	public void createInitialGeometry() {
		numRectangles = 1;
		recCoords = new double[12];
		geometry = new QuadArray( 4, QuadArray.COORDINATES );
		geometry.setCapability( QuadArray.ALLOW_COORDINATE_WRITE );
		shape.setGeometry( geometry );
	}

	/**
	 * Creates new geometry, need to check elsewhere if new geometry is actually
	 * needed.
	 */
	private void createNewGeometry() {
		geometry = new QuadArray( numRectangles * 4, QuadArray.COORDINATES );
		geometry.setCapability( QuadArray.ALLOW_COORDINATE_WRITE );
		shape.setGeometry( geometry );
		updateGeometry();
	}

	/**
	 * Sets the coordinates of the rectangle corners.
	 *
	 * @param points
	 */
	public void setPoints( double[] points ) {
		int tempNumRectangles = points.length / 12;
		int numValues = tempNumRectangles * 12;

		if( tempNumRectangles < 1 ) {
			tempNumRectangles = 1;
			recCoords = new double[12];
			for( int i = 0; i < numValues; i++ ) {
				recCoords[i] = points[i];
			}
			for( int i = numValues; i < 12; i++ ) {
				recCoords[i] = 0.0d;
			}
			setNumRectangles( tempNumRectangles );
			updateGeometry();
		}
		else {
			recCoords = new double[numValues];
			for( int i = 0; i < numValues; i++ ) {
				recCoords[i] = points[i];
			}
			setNumRectangles( tempNumRectangles );
			updateGeometry();
		}
	}

	public void setPoints( Point3d[] pointArray ) {
		double temp[] = new double[pointArray.length * 3];
		for( int i = 0; i < pointArray.length; i++ ) {
			temp[i * 3] = pointArray[i].x;
			temp[(i * 3) + 1] = pointArray[i].y;
			temp[(i * 3) + 2] = pointArray[i].z;
		}
		setPoints( temp );
	}

	/**
	 * Updates the geometry with the contents of recCoords[].
	 */
	public void updateGeometry() {
		double zPush = 0.001 * getLayer();
		double temp[] = new double[recCoords.length];
		for( int i = 0; i < numRectangles * 4; i++ ) {
			temp[i * 3] = recCoords[i * 3];
			temp[(i * 3) + 1] = recCoords[(i * 3) + 1];
			temp[(i * 3) + 2] = recCoords[(i * 3) + 2] + zPush;
		}
		geometry.setCoordinates( 0, temp );
	}

	/**
	 * Sets the current size of the backing quadarray.
	 *
	 * @param numRectangles
	 */
	private void setNumRectangles( int numRectangles ) {
		// Check for at least 1 rectangle
		if( numRectangles < 1 ) {
			numRectangles = 1;
		}

		if( numRectangles != this.numRectangles ) {
			this.numRectangles = numRectangles;
			createNewGeometry();
		}
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
//		for( int i = 0; i < numRectangles; i ++ ) {
//			javax.swing.tree.DefaultMutableTreeNode rectRoot = new javax.swing.tree.DefaultMutableTreeNode( "Rectangle " + i );
//			for( int j = 0; j < 4; j++ ) {
//				int tempIndex = (i * 12) + (j * 3);
//				rectRoot.add( new javax.swing.tree.DefaultMutableTreeNode( "P: " + j + "-- x: " + recCoords[tempIndex] + " y: " + recCoords[tempIndex + 1] + " z: " + recCoords[tempIndex + 2] ) );
//			}
//			pointsRoot.add( rectRoot );
//		}
//		myRoot.add( pointsRoot );
//
//		return myRoot;
//	}
}
