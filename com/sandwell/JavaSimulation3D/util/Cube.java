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


/**
 * Based on original Clearcube implementation.
 */
public class Cube extends Shape {

	private QuadArray geometry;
	private int type;

	protected double width;
	protected double height;
	protected double depth;

	/**
	 * Creates a clearcube centered at x,y,z with a given width, height, depth.
	 *
	 * @param x
	 * @param y
	 * @param z
	 * @param width
	 * @param height
	 * @param depth
	 * @param type
	 */
	public Cube( double x, double y, double z, double width, double height, double depth, int type ) {
		super();
		setName( "Cube" );

		super.setCenter( x, y, z );
		this.width = width;
		this.height = height;
		this.depth = depth;
		this.type = type;

		updateGeometry();

		if( this.type == Shape.SHAPE_FILLED ) {
			setPolygonStyle( Shape.POLYGON_FILL_BOTH );
		}
		else {
			setPolygonStyle( Shape.POLYGON_LINES );
		}
	}

	/**
	 * Creates a cube at x,y,z with dimensions 1,1,1.
	 *
	 * @param x
	 * @param y
	 * @param z
	 * @param type
	 */
	public Cube( double x, double y, double z, int type, String name ) {
		this( x, y, z, 1.0d, 1.0d, 1.0d, type );
		nameOfTheCallingVariable = name;
	}

	/**
	 * Creates a default cube at 0,0,0 with dimensions 1,1,1.
	 *
	 * @param type
	 */
	public Cube( int type, String name  ) {
		this( 0.0d, 0.0d, 0.0d, 1.0, 1.0, 1.0, type);
		nameOfTheCallingVariable = name;
	}

	/**
	 * Creates the cube geometry.
	 */
	public void createInitialGeometry() {
		geometry = new QuadArray( 24, QuadArray.COORDINATES );
		geometry.setCapability( QuadArray.ALLOW_COORDINATE_WRITE );
		shape.setGeometry( geometry );
	}

	public void setBounds(double xmin, double ymin, double zmin, double xmax, double ymax, double zmax) {
		width = xmax - xmin;
		centreX = xmin + width / 2.0d;
		height = ymax - ymin;
		centreY = ymin + height / 2.0d;
		depth = zmax - zmin;
		centreZ = zmin + height / 2.0d;

		updateGeometry();
	}

	/**
	 * Update the cubes geometry coordinates.
	 */
	private void updateGeometry() {
		double coords[] = new double[72];
		double xMaxCoord = centreX + (width / 2.0d);
		double xMinCoord = centreX - (width / 2.0d);
		double yMaxCoord = centreY + (height / 2.0d);
		double yMinCoord = centreY - (height / 2.0d);
		double zMaxCoord = centreZ + (depth / 2.0d);
		double zMinCoord = centreZ - (depth / 2.0d);

		// All faces drawn counter-clockwise
		int i = 0;
		// Face 1 - left face
		coords[i++] = xMinCoord;
		coords[i++] = yMaxCoord;
		coords[i++] = zMinCoord;
		coords[i++] = xMinCoord;
		coords[i++] = yMaxCoord;
		coords[i++] = zMaxCoord;
		coords[i++] = xMinCoord;
		coords[i++] = yMinCoord;
		coords[i++] = zMaxCoord;
		coords[i++] = xMinCoord;
		coords[i++] = yMinCoord;
		coords[i++] = zMinCoord;

		// Face 2 - Top
		coords[i++] = xMinCoord;
		coords[i++] = yMinCoord;
		coords[i++] = zMaxCoord;
		coords[i++] = xMaxCoord;
		coords[i++] = yMinCoord;
		coords[i++] = zMaxCoord;
		coords[i++] = xMaxCoord;
		coords[i++] = yMaxCoord;
		coords[i++] = zMaxCoord;
		coords[i++] = xMinCoord;
		coords[i++] = yMaxCoord;
		coords[i++] = zMaxCoord;

		// Face 3 - Upper y face
		coords[i++] = xMaxCoord;
		coords[i++] = yMaxCoord;
		coords[i++] = zMinCoord;
		coords[i++] = xMinCoord;
		coords[i++] = yMaxCoord;
		coords[i++] = zMinCoord;
		coords[i++] = xMinCoord;
		coords[i++] = yMaxCoord;
		coords[i++] = zMaxCoord;
		coords[i++] = xMaxCoord;
		coords[i++] = yMaxCoord;
		coords[i++] = zMaxCoord;

		// Face 4 - Base face
		coords[i++] = xMaxCoord;
		coords[i++] = yMinCoord;
		coords[i++] = zMinCoord;
		coords[i++] = xMinCoord;
		coords[i++] = yMinCoord;
		coords[i++] = zMinCoord;
		coords[i++] = xMinCoord;
		coords[i++] = yMaxCoord;
		coords[i++] = zMinCoord;
		coords[i++] = xMaxCoord;
		coords[i++] = yMaxCoord;
		coords[i++] = zMinCoord;

		// Face 5 - lower y face
		coords[i++] = xMinCoord;
		coords[i++] = yMinCoord;
		coords[i++] = zMinCoord;
		coords[i++] = xMaxCoord;
		coords[i++] = yMinCoord;
		coords[i++] = zMinCoord;
		coords[i++] = xMaxCoord;
		coords[i++] = yMinCoord;
		coords[i++] = zMaxCoord;
		coords[i++] = xMinCoord;
		coords[i++] = yMinCoord;
		coords[i++] = zMaxCoord;

		// Face 6 - right face
		coords[i++] = xMaxCoord;
		coords[i++] = yMinCoord;
		coords[i++] = zMinCoord;
		coords[i++] = xMaxCoord;
		coords[i++] = yMaxCoord;
		coords[i++] = zMinCoord;
		coords[i++] = xMaxCoord;
		coords[i++] = yMaxCoord;
		coords[i++] = zMaxCoord;
		coords[i++] = xMaxCoord;
		coords[i++] = yMinCoord;
		coords[i++] = zMaxCoord;

		// set the new coordinates
		geometry.setCoordinates( 0, coords );
	}

	/**
	 * Update the centre's x and y only.
	 *
	 * @param x
	 * @param y
	 */
	public void setCenter( double x, double y ) {
		super.setCenter( x, y );
		updateGeometry();
	}

	/**
	 * Update the position of the Cube centre.
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
	 * Set a new size for the Cube.
	 *
	 * @param width
	 * @param height
	 */
	public void setSize( double width, double height, double depth ) {
		this.width = width;
		this.height = height;
		this.depth = depth;
		updateGeometry();
	}

	/**
	 * Set a new width for the Cube.
	 *
	 * @param width
	 */
	public void setWidth( double width ) {
		this.width = width;
		updateGeometry();
	}

	/**
	 * Set a new height for the Cube.
	 *
	 * @param height
	 */
	public void setHeight( double height ) {
		this.height = height;
		updateGeometry();
	}

	/**
	 * Set a new height for the Cube.
	 *
	 * @param height
	 */
	public void setDepth( double depth ) {
		this.depth = depth;
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
//		// Width/Height/Depth Information
//		myRoot.add( new javax.swing.tree.DefaultMutableTreeNode( "Height: " + height + " Width: " + width + " Depth: " + depth ) );
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
//		return myRoot;
//	}
}
