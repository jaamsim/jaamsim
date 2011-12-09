/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2003-2011 Ausenco Engineering Canada Inc.
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

import javax.media.j3d.Appearance;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.ColoringAttributes;
import javax.media.j3d.PolygonAttributes;
import javax.media.j3d.Shape3D;
import javax.vecmath.Color3f;
import javax.vecmath.Point2d;
import javax.vecmath.Vector3d;

/**
 * Utility class that provides simple 2D shape manipulation <br>
 * Created to create a quick graphic representation of a 2D object in J3D <br>
 * This is the parent for Circle, Polygon, RoundedRectangle, ... <br>
 * Available methods: setPos() - changes location, NO ROTATION, NO SCALE, NO
 * FIXED SIZE FUNCTIONALITY <br>
 */
public abstract class Shape2D extends BranchGroup {

	protected Appearance filledAppearance; // color and style of the shape fill
	protected Appearance outlineAppearance; // color and style of the shape outline
	protected Vector3d pos; // position of the graphic (center)
	protected Shape3D fill;
	protected Shape3D outline;
	protected int offset;

	/** Constructs a Shape2D */
	public Shape2D() {
		// setup the transform group
		super();

		pos = new Vector3d(); // default is (0,0,0)

		// create a new appreance for the shape fill .  render both sides
		filledAppearance = new Appearance();
		filledAppearance.setPolygonAttributes( new PolygonAttributes( PolygonAttributes.POLYGON_FILL, PolygonAttributes.CULL_BACK, 0.0f ) );
		filledAppearance.setColoringAttributes(Shape.getPresetColor(Shape.COLOR_WHITE));

		// create a new appearance for the outline
		outlineAppearance = new Appearance();
		outlineAppearance.setPolygonAttributes( new PolygonAttributes( PolygonAttributes.POLYGON_LINE, PolygonAttributes.CULL_BACK, -50.0f ) );
		outlineAppearance.setColoringAttributes(Shape.getPresetColor(Shape.COLOR_BLACK));

		// ensure that we can read and write the color attributes and transformations
		filledAppearance.setCapability( Appearance.ALLOW_COLORING_ATTRIBUTES_WRITE );
		filledAppearance.setCapability( Appearance.ALLOW_COLORING_ATTRIBUTES_READ );

		outlineAppearance.setCapability( Appearance.ALLOW_COLORING_ATTRIBUTES_WRITE );
		outlineAppearance.setCapability( Appearance.ALLOW_COLORING_ATTRIBUTES_READ );

		setCapability( BranchGroup.ALLOW_CHILDREN_WRITE ); // for labels 	//TODO remove
		setCapability( BranchGroup.ALLOW_CHILDREN_EXTEND );
		setCapability( BranchGroup.ALLOW_DETACH );

		// build the objects to hold the geometry all objects have a fill and outline
		// even if they are not used.
		fill = new Shape3D();
		fill.setCapability( Shape3D.ALLOW_GEOMETRY_WRITE );
		fill.setAppearance( filledAppearance );

		outline = new Shape3D();
		outline.setCapability( Shape3D.ALLOW_GEOMETRY_WRITE );
		outline.setAppearance( outlineAppearance );

		addChild( fill );
		addChild( outline );
	}

	/** Returns the color for the fill area of the shape */
	public ColoringAttributes getFillColor() {
		return filledAppearance.getColoringAttributes();
	}

	/** Returns the color for the outline of the shape */
	public ColoringAttributes getOutlineColor() {
		return outlineAppearance.getColoringAttributes();
	}

	/** assigns the color for the fill area of the shape */
	public void setFillColor( ColoringAttributes fillColor ) {
		filledAppearance.setColoringAttributes( fillColor );
	}

	/** assigns the color for the outline of the shape */
	public void setOutlineColor( ColoringAttributes outlineColor ) {
		outlineAppearance.setColoringAttributes( outlineColor );
	}

	/** returns the full appearance for the fill area of the shape */
	public Appearance getFillAppearance() {
		return filledAppearance;
	}

	/** assigns the full appearance for the fill area of the shape */
	public void setFillAppearance( javax.media.j3d.Appearance app ) {
		fill.setAppearance( app );
		filledAppearance = app;
	}

	/** returns the full appearance for the outline of the shape */
	public Appearance getOutlineAppearance() {
		return outlineAppearance;
	}

	/** assigns the full appearance for the outline of the shape */
	public void setOutlineAppearance( javax.media.j3d.Appearance app ) {
		app.setPolygonAttributes( new PolygonAttributes( PolygonAttributes.POLYGON_LINE, PolygonAttributes.CULL_BACK, 0.0f ) );
		outline.setAppearance( app );
		outlineAppearance = app;
	}

	/** positions the shape at the given (x,y) location */
	public void setPos( double x, double y ) {
		setPos( x, y, pos.z );
	}

	/** positions the shape at the given (x,y,z) location */
	public abstract void setPos( double x, double y, double z );

	/** positions the shape at the given position */
	public void setPos( Point2d newPos ) {
		setPos( newPos.x, newPos.y );
	}

	/** returns the present position of the shape */
	public Point2d getPos() {
		return new Point2d( pos.x, pos.y );
	}

	public String toString() {
		Color3f col = new Color3f();
		String ret;

		ret = "" + pos + "{";

		if( filledAppearance != null && filledAppearance.getColoringAttributes() != null ) {
			filledAppearance.getColoringAttributes().getColor( col );
			ret = ret + col;
		}
		else
			ret = ret + "-";

		ret = ret + ",";

		if( outlineAppearance != null && outlineAppearance.getColoringAttributes() != null ) {
			outlineAppearance.getColoringAttributes().getColor( col );
			ret = ret + col;
		}
		else
			ret = ret + "-";
		ret = ret + "}";

		return ret;
	}

	public javax.swing.tree.DefaultMutableTreeNode getTreeNode() {
		javax.swing.tree.DefaultMutableTreeNode myRoot = new javax.swing.tree.DefaultMutableTreeNode( "[" + this.getClass().toString() + "]" );

		// add the position
		myRoot.add( new javax.swing.tree.DefaultMutableTreeNode( "position:" + pos ) );

		// add the fill color
		Color3f col = new Color3f();
		try {
			if( filledAppearance != null && filledAppearance.getColoringAttributes() != null ) {
				filledAppearance.getColoringAttributes().getColor( col );
				myRoot.add( new javax.swing.tree.DefaultMutableTreeNode( "fill color:" + col ) );
			}
		}
		catch( Exception e ) {
			myRoot.add( new javax.swing.tree.DefaultMutableTreeNode( "fill color: unavailable" ) );
		}

		// add the outline color
		try {
			if( outlineAppearance != null && outlineAppearance.getColoringAttributes() != null ) {
				outlineAppearance.getColoringAttributes().getColor( col );
				myRoot.add( new javax.swing.tree.DefaultMutableTreeNode( "outline color:" + col ) );
			}
		}
		catch( Exception e ) {
			myRoot.add( new javax.swing.tree.DefaultMutableTreeNode( "outline color: unavailable" ) );
		}

		myRoot.add( new javax.swing.tree.DefaultMutableTreeNode( "layer:" + offset ) );

		return myRoot;
	}

	public void hideFill() {
		if( fill.getParent() == this )
			removeChild( fill );
	}

	public void hideOutline() {
		if( outline.getParent() == this )
			removeChild( outline );
	}

	public void showFill() {
		if( fill.getParent() != this )
			addChild( fill );
	}

	public void showOutline() {
		if( outline.getParent() != this )
			addChild( outline );
	}

	/** defines the layer for the shape to break ties for shapes that are equal distance from the viewer<br>
	 *   @param layer - the layer that this shape will exist upon.  Valid layers are in the range (-50, 50).<br>
	 *   positive layers are closer to the viewer and negative layers are further from the viewer.  The default layer is 0.
	 **/
	public void setLayer( int layer ) {
		if( layer < -50 || layer > 50 )
			throw new RuntimeException( "Valid layers are in the range (-50, 50)" );

		offset = layer;

		if( outlineAppearance != null )
			outlineAppearance.getPolygonAttributes().setPolygonOffset( -100.0f * layer - 50.0f );

		if( filledAppearance != null )
			filledAppearance.getPolygonAttributes().setPolygonOffset( -100.0f * layer );
	}
}
