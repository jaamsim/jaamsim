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

import javax.media.j3d.ColoringAttributes;
import javax.media.j3d.PointArray;
import javax.media.j3d.PointAttributes;

/**
 * Utility class that provides a point with a fixed number of pixels in size
 * Created to create a quick graphic representation of a 2D object in J3D
 */
public class PointWithSize extends Shape {

	private PointArray geometry;
	private PointAttributes pStyle;
	private float pointSize;
	private boolean state;

	/**
	 * Constructs a Point with the given size and given state.
	 *
	 * @param width
	 * @param state
	 * @param outlineColor
	 */
	public PointWithSize( float width, boolean state, String name  ) {
		super();
		setName( "PointWithSize");

		pointSize = width;
		this.state = state;
		setCenter( 0.0d, 0.0d, 0.0d );
		updateGeometry();
		nameOfTheCallingVariable = name;
	}

	/**
	 * Initializes a PointArray Object with two coordinates, which will produce a
	 * single line segment.
	 */
	public void createInitialGeometry() {
		geometry = new PointArray( 1, PointArray.COORDINATES );
		geometry.setCapability( PointArray.ALLOW_COORDINATE_WRITE );

		// width determines number of pixels - default is one pixel
		// state sets antialiasing - false makes point a square, true makes point a circle
		pointSize = 1.0f;
		state = true;
		pStyle = new PointAttributes( pointSize, state );
		pStyle.setCapability( PointAttributes.ALLOW_ANTIALIASING_WRITE );
		pStyle.setCapability( PointAttributes.ALLOW_SIZE_WRITE );
		appearance.setPointAttributes( pStyle );
		shape.setGeometry( geometry );
	}

	public void setCenter( double x, double y, double z ) {
		// store for later use
		super.setCenter( x, y, z );
		updateGeometry();
	}

	public void updateGeometry() {
		pStyle.setPointSize( pointSize );
		pStyle.setPointAntialiasingEnable( state );
		double zPush = getLayer() * 0.001;
		double[] coords = { centreX, centreY, centreZ + zPush };

		geometry.setCoordinates( 0, coords );
	}

	public void setColor(ColoringAttributes [] colors){
		setColor(colors[0]);
	}


//	public javax.swing.tree.DefaultMutableTreeNode getTreeNode() {
//		javax.swing.tree.DefaultMutableTreeNode myRoot = super.getTreeNode();
//		myRoot.setUserObject( nameOfTheCallingVariable + "(PointWithSize)" );
//		myRoot.add( new javax.swing.tree.DefaultMutableTreeNode( "Center: " + centreX + " " + centreY ) );
//		return myRoot;
//	}
}
