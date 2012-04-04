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
package com.sandwell.JavaSimulation3D;

import java.util.ArrayList;

import javax.media.j3d.Appearance;
import javax.media.j3d.ColoringAttributes;
import javax.media.j3d.LineAttributes;
import javax.vecmath.Vector3d;

import com.sandwell.JavaSimulation.DoubleVector;
import com.sandwell.JavaSimulation.Input;
import com.sandwell.JavaSimulation.InputErrorException;
import com.sandwell.JavaSimulation.StringVector;
import com.sandwell.JavaSimulation.Vector3dListInput;
import com.sandwell.JavaSimulation3D.util.Line;
import com.sandwell.JavaSimulation3D.util.Shape;
import com.sandwell.JavaSimulation3D.util.Vec3dUtil;

public class Arrow extends DisplayEntity {
	private static final ArrayList<Arrow> allInstances;

	protected final Vector3dListInput pointsInput;
	protected Vector3d startCoord;         // Graphical start point
	protected Vector3d endCoord;           // Graphical end point
	protected DoubleVector intermediateX;  // intermediate x coordinates for bends
	protected DoubleVector intermediateY;  // intermediate y coordinates for bends
	protected float width;                 // width of the lines in pixel units
	protected LineAttributes la;
	protected Appearance lineAppearance;
	private Line line;
	private final ArrayList<Vector3d> screenPoints;
	protected ArrowHead arrowHead;         // arrow head at end of line
	protected Vector3d arrowHeadSize;      // Graphical extent of arrow head
	protected ColoringAttributes color;    // Line color (RGB)

	private final Vector3d arrowHeadOffset = new Vector3d();

	static {
		allInstances = new ArrayList<Arrow>();
	}
	{
		pointsInput = new Vector3dListInput("Points", "Graphics", null);
		pointsInput.setValidCountRange( 2, Integer.MAX_VALUE );
		this.addInput(pointsInput, true);

		addEditableKeyword( "Start",              "  -  ",   " - "     , false, "Graphics" );
		addEditableKeyword( "End",       		  "  -  ",   " - "     , false, "Graphics" );
		addEditableKeyword( "X",                  "  -  ",   " - "     , false, "Graphics" );
		addEditableKeyword( "Y",                  "  -  ",   " - "     , false, "Graphics" );
		addEditableKeyword( "Width",              "  -  ",   " 1 "     , false, "Graphics" );
		addEditableKeyword( "ArrowSize",          "  -  ",   "(1,1,0)" , false, "Graphics" );
		addEditableKeyword( "Color",             "  -  ",   "Black"   , false, "Graphics", "Colour" );
	}

	public Arrow() {
		allInstances.add(this);
		startCoord = new Vector3d( 0.0, 0.0, 0.0 );
		endCoord = new Vector3d( 0.0, 0.0, 0.0 );
		intermediateX = new DoubleVector();
		intermediateY = new DoubleVector();
		width = 1.0f;
		screenPoints = new ArrayList<Vector3d>();
		arrowHead = new ArrowHead();
		arrowHeadSize = new Vector3d( 1, 1, 0 );
		color = Shape.getPresetColor(Shape.COLOR_BLACK);
	}

	public static ArrayList<Arrow> getAll() {
		return allInstances;
	}

	public void kill() {
		super.kill();
		allInstances.remove(this);
	}

	public void initailize() {
		validateStartAndEndCoords();
		initializeScreenPoints();
		initializeMouseNodes();
		this.setGraphicsForStartPt_EndPt( startCoord, endCoord );
	}

	// ******************************************************************************************************
	// INPUT METHODS
	// ******************************************************************************************************

	/**
	 * Processes the input data corresponding to the specified keyword. If syntaxOnly is true,
	 * checks input syntax only; otherwise, checks input syntax and process the input values.
	 */
	public void readData_ForKeyword(StringVector data, String keyword, boolean syntaxOnly, boolean isCfgInput)
	throws InputErrorException {
		if( "START".equalsIgnoreCase( keyword ) ) {
			startCoord = Input.parseVector3d(data);
			return;
		}
		if( "END".equalsIgnoreCase( keyword ) ) {
			endCoord = Input.parseVector3d(data);
			return;
		}
		if( "X".equalsIgnoreCase( keyword ) ) {
			intermediateX = Input.parseDoubleVector(data, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
			return;
		}
		if( "Y".equalsIgnoreCase( keyword ) ) {
			intermediateY = Input.parseDoubleVector(data, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
			return;
		}
		if( "WIDTH".equalsIgnoreCase( keyword ) ) {
			Input.assertCount(data, 1);
			width = (float)Input.parseDouble(data.get(0), 0.0d, Double.POSITIVE_INFINITY);
			return;
		}
		if( "ARROWSIZE".equalsIgnoreCase( keyword ) ) {
			arrowHeadSize = Input.parseVector3d(data);
			return;
		}

		if( "COLOUR".equalsIgnoreCase( keyword ) || "COLOR".equalsIgnoreCase( keyword ) ) {
			color = Input.parseColour(data);
			return;
		}

		super.readData_ForKeyword( data, keyword, syntaxOnly, isCfgInput );
	}

	// ******************************************************************************************************
	// GRAPHICS METHODS
	// ******************************************************************************************************

	public void initializeGraphics() {
		this.setGraphicsForStartPt_EndPt( startCoord, endCoord );
	}

	public Vector3d getStartCoord() {
		return startCoord;
	}

	public void setStartCoord( Vector3d pt ) {
		startCoord = pt;
	}

	public Vector3d getEndCoord() {
		return endCoord;
	}

	public void setEndCoord( Vector3d pt ) {
		endCoord = pt;
	}

	public DoubleVector getIntermediateX() {
		return intermediateX;
	}

	public DoubleVector getIntermediateY() {
		return intermediateY;
	}

	public ArrayList<Vector3d> getScreenPoints() {
		return screenPoints;
	}

	public void setGraphicsForStartPt_EndPt( Vector3d p1, Vector3d p2 ) {
		if( p1 == null ) {
			throw new InputErrorException( this.getName()+": Missing Start Point" );
		}

		if( p2 == null ) {
			throw new InputErrorException( this.getName()+": Missing End Point" );
		}

		startCoord = p1;
		endCoord = p2;
		this.setGraphics();
	}

	/**
	 * Create the graphical representation of the arrow from the input file.
	 * An arrow can have multiple segments.
	 */
	public void setGraphics() {
		//if( traceFlag ) this.trace( "setGraphics()" );

		// Line appearance is required to set a line width
		lineAppearance = new Appearance();
		lineAppearance.setCapability( Appearance.ALLOW_POLYGON_ATTRIBUTES_WRITE );
		lineAppearance.setCapability( Appearance.ALLOW_COLORING_ATTRIBUTES_READ );
		lineAppearance.setColoringAttributes(Shape.getPresetColor(Shape.COLOR_LIGHT_GREY));
		la = new LineAttributes();
		la.setLineWidth( width );
		lineAppearance.setLineAttributes( la );

		if( intermediateX.size() != intermediateY.size() ) {
			throw new InputErrorException( "Number of intermediate X and Y coordinates must be the same for segment " + this.getName() );
		}

		// update x/y points from mouseNodes
		this.updateScreenPoints();

		// Store the min and max screen coordinates
		Vector3d minPt = screenPoints.get( 0 );
		Vector3d maxPt = screenPoints.get( 0 );

		for (int i = 1; i < screenPoints.size(); i++) {
			minPt = Vec3dUtil.min( minPt, screenPoints.get(i) );
			maxPt = Vec3dUtil.max( maxPt, screenPoints.get(i) );
		}

		if (line == null) {
			line = new Line(0.0d, 0.0d, 0.0d, 0.0d, 0.0d, 0.0d);
			line.setColor(color);
			line.setLineStyle(la);
			this.addShape(line);
		}

		line.assignPoints(screenPoints, minPt, maxPt);

		// Position arrow at the end point
		Vector3d end = screenPoints.get(screenPoints.size() - 1);
		Vector3d prev = screenPoints.get(screenPoints.size() - 2);

		arrowHead.setSize( arrowHeadSize );
		arrowHead.setPosition(end);
		arrowHead.setColor(color);
		Vector3d temp = arrowHead.getOrientation();
		temp.z = Math.atan2(end.y - prev.y, end.x - prev.x);
		arrowHead.setOrientation(temp);
		arrowHead.enterRegion();

		this.setSize( Vec3dUtil.sub( maxPt, minPt ) );
		this.setAlignment(new Vector3d(-0.5d, -0.5d, 0.0d));
		this.setPosition(minPt);
	}

	/**
	 * create screen point from startCood, intermediateX&Y, and endCoord
	 */
	public void initializeScreenPoints(){

		// set up screen points
		screenPoints.clear();

		// If Points were input, then use them to populate screenPoints
		if( pointsInput.getValue() != null ) {
			screenPoints.addAll( pointsInput.getValue() );
		}
		else {
			// Otherwise, populate screen points from the start, end, and intermediate points
			screenPoints.add( startCoord );

			// intermediate points
			for ( int i = 0; i < intermediateX.size(); i++ ) {
				screenPoints.add( new Vector3d( intermediateX.get(i), intermediateY.get(i), 0.0	));
			}

			screenPoints.add( endCoord );
		}
	}

	/**
	 * initializeMouseNodes from screen points
	 */
	public void initializeMouseNodes(){

		this.killMouseNodes();

		for( Vector3d points : screenPoints ){
			MouseNode mouse = new MouseNode( this, mouseNodesSize, Shape.COLOR_BLUE );
			mouse.setRegion( getCurrentRegion() );
			mouse.setPosition(points);
		}

		if( screenPoints.size() > 1){
			getMouseNodes().get(0).setColor(Shape.getPresetColor(Shape.COLOR_GREEN));
			getMouseNodes().get(screenPoints.size() - 1).setColor(Shape.getPresetColor(Shape.COLOR_RED));
		}
	}

	/**
	 * Adds a mouseNode at the given position, and create an intermediate point there
	 * @param posn
	 */
	public void addMouseNodeAt( Vector3d posn ) {

		Vector3d closestPoint = new Vector3d();
		Vector3d newPoint;
		int indexOfClosestPoint = 0;
		int indexOfNewPoint = 0;
		double distanceFromClosestPoint = 99.999;

		// find closest point
		for ( int i = 0; i < screenPoints.size(); i++ ) {
			Vector3d each = screenPoints.get( i );
			double distance = Vec3dUtil.distance( each, posn );
			if ( distance < distanceFromClosestPoint ) {
				distanceFromClosestPoint = distance;
				closestPoint = each;
				indexOfClosestPoint = i;
			}
		}

		// find point on Line to create
		if ( indexOfClosestPoint > 0 ) {

			Vector3d pointBefore = screenPoints.get( indexOfClosestPoint-1 );
			Vector3d point1 = Vec3dUtil.getClosestPointTo_OnLineBetween( posn, pointBefore, closestPoint );
			newPoint = point1;
			indexOfNewPoint = indexOfClosestPoint;

			// if not the end point
			if ( indexOfClosestPoint < screenPoints.size()-1 ) {
				Vector3d pointAfter = screenPoints.get( indexOfClosestPoint+1 );
				Vector3d point2 = Vec3dUtil.getClosestPointTo_OnLineBetween( posn, closestPoint, pointAfter );

				// which point is closer to original point?
				if ( Vec3dUtil.distance( posn, point2 ) < Vec3dUtil.distance( posn, point1 ) ) {
						newPoint = point2;
						indexOfNewPoint = indexOfClosestPoint + 1;
			    }
			}

		}
		// closest point is the first point
		else {
			Vector3d pointAfter = screenPoints.get( indexOfClosestPoint+1 );
			Vector3d point2 = Vec3dUtil.getClosestPointTo_OnLineBetween( posn, closestPoint, pointAfter );
			newPoint = point2;
			indexOfNewPoint = indexOfClosestPoint + 1;
		}

		// point has been found, now add to model
		screenPoints.add( indexOfNewPoint, newPoint );

		MouseNode newMouseNode = new MouseNode(this);

		// mouseNode will have been added to wrong spot in list, remove and add to correct spot
		this.getMouseNodes().remove(newMouseNode);
		this.getMouseNodes().add(indexOfNewPoint, newMouseNode);

		newMouseNode.setRegion( this.getCurrentRegion() );
		newMouseNode.setPosition(newPoint);
		newMouseNode.enterRegion();
	}

	public void updateScreenPoints(){
		if ( this.getMouseNodes() != null ) {
			for ( int i = 0; i < this.getMouseNodes().size(); i++ ) {
				MouseNode mouseNode = this.getMouseNodes().get( i );
				// screen points
				screenPoints.set(i, mouseNode.getPosition());
			}
		}
	}

 	/**
	 *  Inform simulation and editBox of new positions.
	 */
	public void updateInputPosition() {

		Input<?> pts = this.getInput("Points");
		if (pts != null) {
			StringBuilder tmp = new StringBuilder();
			for( Vector3d point : this.getScreenPoints() ) {
				tmp.append(String.format(" { %.3f %.3f %.3f }", point.x, point.y, point.z));
			}
			InputAgent.processEntity_Keyword_Value(this, pts, tmp.toString());
		}
	}

	public void validateStartAndEndCoords() {

		// If Points were input, then use them to set the start and end coordinates
		if( pointsInput.getValue() != null ) {
			this.setStartCoord( pointsInput.getValue().get( 0 ) );
			this.setEndCoord( pointsInput.getValue().get( pointsInput.getValue().size()-1 ) );
		}

		// If Start and end points are not defined, use the first and last elements intermediate points for those points
		if( startCoord.getX() == 0 && startCoord.getY() == 0 && endCoord.getX() == 0 && endCoord.getY() == 0 && intermediateX.size() > 1 ) {
			Vector3d aPt;

			// Set the start point and remove from the intermediate list
			aPt = new Vector3d(intermediateX.remove(0), intermediateY.remove(0), 0.0 );
			this.setStartCoord(aPt);

			// Set the end point and remove from the intermediate list
			aPt = new Vector3d(intermediateX.remove(intermediateX.size() - 1),
							intermediateY.remove(intermediateY.size() - 1), 0.0 );
			this.setEndCoord( aPt );
		}
	}

	public void preDrag() {
		arrowHeadOffset.sub(arrowHead.getPosition(), this.getPosition());
		super.preDrag();
	}

	public void dragged(Vector3d distance) {
		super.dragged(distance);

		updateScreenPoints();

		Vector3d temp = this.getPosition();
		temp.add(arrowHeadOffset);
		arrowHead.setPosition(temp);
	}

	public void postDrag() {
		this.setGraphics();
		this.updateInputPosition();
		super.postDrag();
	}

	public void setRegion( Region newRegion ) {
		arrowHead.setRegion( newRegion );
		super.setRegion( newRegion );
	}
}
