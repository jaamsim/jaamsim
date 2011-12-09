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
 * Rectangle class based on original Shape2D implementation.
 */
public class Rectangle extends Shape {

	private QuadArray geometry;
	private int type;

	protected double width;
	protected double height;

	/**
	 * Creates a rectangle centred at x,y,z with width,height either filled or
	 * wireframed depending on type.
	 *
	 * @param x
	 * @param y
	 * @param z
	 * @param width
	 * @param height
	 * @param type
	 */
	public Rectangle( double x, double y, double z, double width, double height, int type ) {
		super();
		setName( "Rectangle" );

		this.type = type;
		this.width = width;
		this.height = height;

		super.setCenter( x, y, z );

		updateGeometry();

		if( this.type == Shape.SHAPE_FILLED ) {
			setPolygonStyle( Shape.POLYGON_FILL_FRONT );
		}
		else {
			setPolygonStyle( Shape.POLYGON_LINES );
		}
	}

	/**
	 * Creates rectangle centered at 0,0,0.
	 *
	 * @param width
	 * @param height
	 * @param type
	 */
	public Rectangle( double width, double height, int type, String name ) {
		this( 0.0d, 0.0d, 0.0d, width, height, type );
		nameOfTheCallingVariable = name;
	}

	/**
	 * Shortcut to create a Rectangle with no z parameter.
	 *
	 * @param x
	 * @param y
	 * @param width
	 * @param height
	 * @param type
	 */
	public Rectangle( double x, double y, double width, double height, int type ) {
		this( x, y, 0.0d, width, height, type );
	}

	public Rectangle( double x, double y, double width, double height, int type, String name ) {
		this( x, y, 0.0d, width, height, type );
		nameOfTheCallingVariable = name;
	}

	/**
	 * Creates the backing quadarray for the rectangle.
	 */
	public void createInitialGeometry() {
		geometry = new QuadArray( 4, QuadArray.COORDINATES );
		geometry.setCapability( QuadArray.ALLOW_COORDINATE_WRITE );
		shape.setGeometry( geometry );
	}

	/**
	 * Update the rectangle after state change.
	 */
	public void updateGeometry() {
		// four extremes of rectangles
		double maxX = centreX + (width / 2.0d);
		double minX = centreX - (width / 2.0d);
		double maxY = centreY + (height / 2.0d);
		double minY = centreY - (height / 2.0d);

		double corners[] = { maxX, maxY, centreZ, minX, maxY, centreZ, minX, minY, centreZ, maxX, minY, centreZ };
		geometry.setCoordinates( 0, corners );
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
	 * Update the position of the rectangle centre.
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
	 * Set a new size for the Rectangle.
	 *
	 * @param width
	 * @param height
	 */
	public void setSize( double width, double height ) {
		this.width = width;
		this.height = height;
		updateGeometry();
	}

	/**
	 * Set a new width for the Rectangle.
	 *
	 * @param width
	 */
	public void setWidth( double width ) {
		this.width = width;
		updateGeometry();
	}

	/**
	 * get the width for the Rectangle.
	 *
	 * @param width
	 */
	public double getWidth( ) {
		return width;
	}

	public double getHeight( ) {
		return height;
	}

	/**
	 * Set a new height for the Rectangle.
	 *
	 * @param height
	 */
	public void setHeight( double height ) {
		this.height = height;
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
//		// Width/Height Information
//		myRoot.add( new javax.swing.tree.DefaultMutableTreeNode( "Height: " + height + " Width: " + width ) );
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
