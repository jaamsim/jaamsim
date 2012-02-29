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
import com.sandwell.JavaSimulation3D.util.Line;
import com.sandwell.JavaSimulation3D.util.Shape;
import com.sandwell.JavaSimulation3D.util.Vec3dUtil;

public class Arrow extends DisplayEntity {
	private static final ArrayList<Arrow> allInstances;

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
		this.killMouseNodes();
		this.validateStartAndEndCoords();
		this.setGraphicsForStartPt_EndPt(startCoord, endCoord);
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

		//setup mouseNodes if needed
		if ( this.getMouseNodes().size() != this.getIntermediateX().size()+2 ) {
			// initial mouseNode
			if ( this.getMouseNodes().size() == 0 ) {
				MouseNode startMouseNode;
				startMouseNode = new MouseNode( this, this.getMouseNodesSize(), Shape.COLOR_GREEN );
				startMouseNode.setRegion( this.getCurrentRegion() );
				startMouseNode.setPosition(startCoord);
			}

			//loop through list of points
			Vector3d nodePos = new Vector3d();
			for( int i = 1; i < this.getIntermediateX().size()+1; i++ ) {
				// check if mouseNode exists at this position in vector
				if ( getMouseNodes().size() < i+1 ) {
					MouseNode mouseNode = new MouseNode(this);
					mouseNode.setRegion( this.getCurrentRegion() );
					nodePos.x = getIntermediateX().get(i - 1);
					nodePos.y = getIntermediateY().get(i - 1);
					mouseNode.setPosition(nodePos);
				}
			}

			// end point
			if ( this.getMouseNodes().size() < this.getIntermediateX().size()+2 ) {
				MouseNode endMouseNode;
				endMouseNode = new MouseNode( this, this.getMouseNodesSize() + (int)Math.max(0.002, this.getMouseNodesSize()*0.1), Shape.COLOR_RED );
				endMouseNode.setRegion( this.getCurrentRegion() );
				endMouseNode.setPosition(endCoord);
			}

		}

		// update x/y points from mouseNodes
		if ( this.getMouseNodes() != null ) {
			for ( int i = 0; i < this.getMouseNodes().size(); i++ ) {
				MouseNode mouseNode = this.getMouseNodes().get(i);

				//start point
				if ( i == 0 ) {
					startCoord = mouseNode.getPosition();
				//end point
				} else if ( i == this.getMouseNodes().size()-1 ) {
					endCoord = mouseNode.getPosition();
				//intermediate point
				} else {
					Vector3d pos = mouseNode.getPosition();
					this.getIntermediateX().set(i - 1, pos.x);
					this.getIntermediateY().set(i - 1, pos.y);
					//TODO: interface with edited values here to update editbox and allow position saving
				}
			}
		}



		// Setup coordinate vectors



		// Store the min and max screen coordinates
		Vector3d minPt = Vec3dUtil.min(startCoord, endCoord);
		Vector3d maxPt = Vec3dUtil.max(startCoord, endCoord);

		// Build the screenpoints list
		screenPoints.clear();
		screenPoints.add(startCoord);
		for (int i = 0; i < intermediateX.size(); i++) {
			Vector3d intPt = new Vector3d(intermediateX.get(i), intermediateY.get(i), 0.0);

			minPt = Vec3dUtil.min( minPt, intPt );
			maxPt = Vec3dUtil.max( maxPt, intPt );
			screenPoints.add(intPt);

		}
		screenPoints.add(endCoord);

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
	 * Adds a mouseNode at the given position, and create an intermediate point there
	 * @param posn
	 */
	public void addMouseNodeAt( Vector3d posn ) {

		Vector3d closestPoint = new Vector3d();
		Vector3d newPoint;
		int indexOfClosestPoint = 0;
		int indexOfNewPoint = 0;
		double distanceFromClosestPoint = 99.999;
		ArrayList<Vector3d> pointList = new ArrayList<Vector3d>(this.getIntermediateX().size() + 2);
		pointList.add( startCoord );
		for ( int i = 0; i < this.getIntermediateX().size(); i++ ) {
			pointList.add( new Vector3d( this.getIntermediateX().get( i ), this.getIntermediateY().get( i ), 0.0 ) );
		}
		pointList.add( endCoord );

		// find closest point
		for ( int i = 0; i < pointList.size(); i++ ) {
			Vector3d each = pointList.get( i );
			double distance = Vec3dUtil.distance( each, posn );
			if ( distance < distanceFromClosestPoint ) {
				distanceFromClosestPoint = distance;
				closestPoint = each;
				indexOfClosestPoint = i;
			}
		}

		// find point on Line to create
		if ( indexOfClosestPoint > 0 ) {

			Vector3d pointBefore = pointList.get(indexOfClosestPoint - 1);
			Vector3d point1 = Vec3dUtil.getClosestPointTo_OnLineBetween( posn, pointBefore, closestPoint );
			newPoint = point1;
			indexOfNewPoint = indexOfClosestPoint;

			// if not the end point
			if ( indexOfClosestPoint < pointList.size()-1 ) {
				Vector3d pointAfter = pointList.get(indexOfClosestPoint + 1);
				Vector3d point2 = Vec3dUtil.getClosestPointTo_OnLineBetween( posn, closestPoint, pointAfter );

				// which point is closer to original point?
				if ( Vec3dUtil.distance( posn, point2 ) < Vec3dUtil.distance( posn, point1) ) {
						newPoint = point2;
						indexOfNewPoint = indexOfClosestPoint + 1;
			    }
			}

		}
		// closest point is the first point
		else {
			Vector3d pointAfter = pointList.get(indexOfClosestPoint + 1);
			Vector3d point2 = Vec3dUtil.getClosestPointTo_OnLineBetween( posn, closestPoint, pointAfter );
			newPoint = point2;
			indexOfNewPoint = indexOfClosestPoint + 1;
		}

		// point has been found, now add to model
		pointList.add(indexOfNewPoint, newPoint);
		this.getIntermediateX().add((indexOfNewPoint-1), newPoint.getX());
		this.getIntermediateY().add((indexOfNewPoint-1), newPoint.getY());

		MouseNode newMouseNode = new MouseNode(this);

		// mouseNode will have been added to wrong spot in list, remove and add to correct spot
		this.getMouseNodes().remove(newMouseNode);
		this.getMouseNodes().add(indexOfNewPoint, newMouseNode);

		newMouseNode.setRegion( this.getCurrentRegion() );
		newMouseNode.setPosition(newPoint);
		newMouseNode.enterRegion();

		this.initializeGraphics();

	}

 	/**
	 *  Inform simulation and editBox of new positions.
	 */
	public void updateInputPosition() {

		Input<?> in = this.getInput( "Start" );
		if ( in != null ) {
			InputAgent.processEntity_Keyword_Value(this, in,
					String.format( "%.3f %.3f", startCoord.getX(), startCoord.getY() ) );
		}

		in = this.getInput( "End" );
		if ( in != null ) {
			InputAgent.processEntity_Keyword_Value(this, in,
					String.format( "%.3f %.3f", endCoord.getX(), endCoord.getY() ) );
		}

		in = this.getInput( "X" );
		if ( in != null ) {
			StringBuilder tmp = new StringBuilder();
			for( int i = 0; i < this.getIntermediateX().size(); i++ ) {
				tmp.append(String.format( "%.3f ", this.getIntermediateX().get( i ) ));
			}
			InputAgent.processEntity_Keyword_Value(this, in, tmp.toString());
		}

		in = this.getInput( "Y" );
		if ( in != null ) {
			StringBuilder tmp = new StringBuilder();
			for( int i = 0; i < this.getIntermediateY().size(); i++ ) {
				tmp.append(String.format( "%.3f ", this.getIntermediateY().get( i ) ));
			}
			InputAgent.processEntity_Keyword_Value(this, in, tmp.toString());
		}
	}

	public void validateStartAndEndCoords() {

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
