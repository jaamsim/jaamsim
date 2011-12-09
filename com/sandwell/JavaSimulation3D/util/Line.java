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

import java.util.ArrayList;

import javax.media.j3d.LineArray;
import javax.vecmath.Vector3d;

/**
 * Class to create a single drawable line with a start and end point.
 */
public class Line extends Shape {
	private LineArray geometry;
	private int points = 0;

	/**
	 * Convenience constructor for a line existing in the x,y plane.
	 *
	 * @param x1
	 * @param y1
	 * @param x2
	 * @param y2
	 */
	public Line( double x1, double y1, double x2, double y2, String name  ) {
		this( x1, y1, 0.0d, x2, y2, 0.0d );
		nameOfTheCallingVariable = name;
	}

	/**
	 * Creates a line object with a segment from (x1,y1,z1) to (x2,y2,z2).
	 *
	 * @param x1
	 * @param y1
	 * @param z1
	 * @param x2
	 * @param y2
	 * @param z2
	 */
	public Line( double x1, double y1, double z1, double x2, double y2, double z2 ) {
		super();
		setName( "Line" );

		assignPoints( x1, y1, z1, x2, y2, z2 );
	}

	public Line(int lineCount, String name) {
		this.setName(name);
		this.setNumCoordinates(lineCount * 2);
		assignPoints(0.0d, 0.0d, 0.0d, 0.0d, 0.0d, 0.0d);
	}

	/**
	 * Initializes a LineArray Object with two coordinates, which will produce a
	 * single line segment.
	 */
	public void createInitialGeometry() {
		this.setNumCoordinates(2);
	}

	private void setNumCoordinates(int numPoints) {
		if (points > numPoints)
			return;
		points = numPoints;
		geometry = new LineArray(numPoints, LineArray.COORDINATES);
		geometry.setCapability( LineArray.ALLOW_COORDINATE_WRITE );
		geometry.setCapability(LineArray.ALLOW_COUNT_WRITE);
		shape.setGeometry(geometry);
	}

	public void assignPoints(ArrayList<Vector3d> pts, Vector3d minPt, Vector3d maxPt) {
		double[] points = new double[(pts.size() - 1) * 6];

		// Determine starting x coordinate of first line
		double xext = maxPt.getX() - minPt.getX();
		xext = Math.max(xext, 0.0001d);
		double yext = maxPt.getY() - minPt.getY();
		yext = Math.max(yext, 0.0001d);
		double zext = maxPt.getZ() - minPt.getZ();
		zext = Math.max(zext, 0.0001d);

		for (int i = 1; i < pts.size(); i++) {
			int index = (i - 1) * 6;
			Vector3d vec = pts.get(i - 1);
			points[index + 0] = (vec.x - minPt.x) / xext - 0.5d;
			points[index + 1] = (vec.y - minPt.y) / yext - 0.5d;
			points[index + 2] = (vec.z - minPt.z) / zext - 0.5d;
			vec = pts.get(i);
			points[index + 3] = (vec.x - minPt.x) / xext - 0.5d;
			points[index + 4] = (vec.y - minPt.y) / yext - 0.5d;
			points[index + 5] = (vec.z - minPt.z) / zext - 0.5d;

		}
		this.setNumCoordinates((pts.size() - 1) * 2);
		geometry.setCoordinates(0, points);
		geometry.setValidVertexCount((pts.size() - 1) * 2);
	}

	public void assignPoints(ArrayList<Vector3d> pts) {
		double[] points = new double[(pts.size() - 1) * 6];

		for (int i = 1; i < pts.size(); i++) {
			int index = (i - 1) * 6;
			Vector3d vec = pts.get(i - 1);
			points[index + 0] = vec.x;
			points[index + 1] = vec.y;
			points[index + 2] = vec.z;
			vec = pts.get(i);
			points[index + 3] = vec.x;
			points[index + 4] = vec.y;
			points[index + 5] = vec.z;

		}
		this.assignPoints(points);
	}

	public void assignPointsPairs(ArrayList<Vector3d> pts) {
		double[] points = new double[pts.size() * 3];
		for (int i = 0; i < pts.size(); i++) {
			Vector3d vec = pts.get(i);
			points[i * 3 + 0] = vec.x;
			points[i * 3 + 1] = vec.y;
			points[i * 3 + 2] = vec.z;
		}

		this.assignPoints(points);
	}

	public void assignPoints(double[] points) {
		this.setNumCoordinates((points.length / 6) * 2);
		geometry.setCoordinates(0, points);
		geometry.setValidVertexCount((points.length / 6) * 2);
	}

	public void assignPoints( double x1, double y1, double z1, double x2, double y2, double z2 ) {
		double[] lineCoordinates = new double[6];
		lineCoordinates[0] = x1;
		lineCoordinates[1] = y1;
		lineCoordinates[2] = z1;
		lineCoordinates[3] = x2;
		lineCoordinates[4] = y2;
		lineCoordinates[5] = z2;

		geometry.setCoordinates(0, lineCoordinates);
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
//		// Start Point Information
//		myRoot.add( new javax.swing.tree.DefaultMutableTreeNode( "Start x: " + lineCoordinates[0] + " y: " + lineCoordinates[1] + " z: " + lineCoordinates[2] ) );
//
//		// End Point Information
//		myRoot.add( new javax.swing.tree.DefaultMutableTreeNode( "End x: " + lineCoordinates[3] + " y: " + lineCoordinates[4] + " z: " + lineCoordinates[5] ) );
//
//		return myRoot;
//	}
}
