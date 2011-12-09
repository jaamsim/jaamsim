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

import java.awt.Font;

import javax.media.j3d.BoundingBox;
import javax.media.j3d.ColoringAttributes;
import javax.media.j3d.Font3D;
import javax.media.j3d.FontExtrusion;
import javax.media.j3d.Text3D;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.vecmath.Point2d;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;


/**
 * Utility class that provides 2D text Created to create a quick graphic
 * representation of a 2D object in J3D <br>
 * <br>
 * Example use of a Label - creates "Hello World" of dimension 2 by 1 <br>-
 * changes text to "Bye World" <br>
 * <br>
 * <code>
 *		Label myLabel = new Label("Hello World", 2.0, 1.0);<br>
 *		displayModel.addChild(myLabel);<br>
 *		....<br>
 *      myLabel.setText("Bye World");<br>
 * </code>
 */
public class LabelShape extends Shape2D {

	private static Font3D defaultFont; // default font shared by all
	private Text3D myText3D; // the 3D shaped text used by this label
	private TransformGroup tg; // local transformgroup for sizing the label
	private Transform3D trans; // scaling transform
	private BoundingBox bounds; // temp used for calculations
	private double textWidth; // width of the text object
	private double textHeight; // height of the text object
	private double myWidth; // display width
	private double myHeight; // display height
	private static FontExtrusion defaultExtrusion;
	private Vector3d scale;
	private String myText;
	private boolean rightJustified = false;
	private boolean leftJustified = false;
	private Vector3d initialPos; // position which enters in constructor method

	// create a single font (with no extrusion), shared by all labels
	static {
		java.awt.geom.Line2D.Float extrusionPath = new java.awt.geom.Line2D.Float( 0f, 0f, 0f, 0f );
		defaultExtrusion = new FontExtrusion( extrusionPath );
		//defaultFont = new Font3D( new Font( "Trebuchet MS", Font.BOLD + Font.ITALIC, 1 ), defaultExtrusion );
		defaultFont = new Font3D( new Font( "Verdana", Font.TRUETYPE_FONT + Font.PLAIN, 1 ), defaultExtrusion );
	}

	/** Constructs a Label with the specified text, color, font (type, size and style) */
	public LabelShape( String text, double width, double height, double x, double y, Font3D font ) {
		super();

		scale = new Vector3d( 1.0, 1.0, 1.0 );
		myText3D = new Text3D( font, "A" ); // Text height for every text should be the same as a capital letter
		myText = text;
		bounds = new BoundingBox();
		trans = new Transform3D();
		tg = new TransformGroup();
		initialPos = new Vector3d( x, y, 0.0 );

		myText3D.setAlignment( Text3D.ALIGN_CENTER );
		myText3D.setCapability( Text3D.ALLOW_POSITION_WRITE );
		myText3D.setCapability( Text3D.ALLOW_STRING_WRITE );
		myText3D.setCapability( Text3D.ALLOW_BOUNDING_BOX_READ );
		myText3D.setCapability( Text3D.ALLOW_FONT3D_WRITE );
		myText3D.getBoundingBox( bounds );
		Point3d upper = new Point3d();
		Point3d lower = new Point3d();
		bounds.getUpper( upper );
		bounds.getLower( lower );
		textWidth = upper.x - lower.x;
		textHeight = upper.y - lower.y;
		fill.setGeometry( myText3D );

		// add transform group to the model
		removeChild( fill );
		tg.setCapability( TransformGroup.ALLOW_TRANSFORM_WRITE );
		tg.addChild( fill );
		addChild( tg );

		// setup scale

		setText( text );
		setSize( width, height );
		setPos( x, y, 0.0 );

		// get rid of the outline
		removeChild( outline );
		outline = null;
		outlineAppearance = null;
//		System.out.println( " for " + text + "  myWidth:" + myWidth );
	}

	public LabelShape( String text, double width, double height, double x, double y, String fontName ) {
		this( text, width, height, x, y );
		Font3D thisFont = new Font3D( new Font( fontName, Font.PLAIN, 1 ), defaultExtrusion );
		myText3D.setFont3D( thisFont );
	}


	public LabelShape( String text, double width, double height, double x, double y ) {
		this( text, width, height, x, y, defaultFont );
	}

	public LabelShape( String text ) {
		this( text, 1.0, 1.0, 0.0, 0.0 );
	}

	public LabelShape( String text, ColoringAttributes textColor ) {
		this( text, 1.0, 1.0, 0.0, 0.0 );
		setFillColor( textColor );
	}

	public void setText( String text ) {
		// change the text
		myText3D.setString( text );

		myText = text;

		// calculate width and height values
		myText3D.getBoundingBox( bounds );
		Point3d upper = new Point3d();
		Point3d lower = new Point3d();
		bounds.getUpper( upper );
		bounds.getLower( lower );
		textWidth = upper.x - lower.x;

		// reset the display width
		myWidth = textWidth / textHeight * myHeight;

		// scale the Text3D to dimensions
		setSize( myWidth, myHeight );
	}

	public String getText() {
		return myText3D.getString();
	}

	public void setSize( double w, double h ) {
		double oldY = pos.y + textHeight / 2.0 * scale.y;
		// store the display size
		if( w != 0 ) {
			myWidth = w;
		}
		if ( h != 0 ) {
			myHeight = h;
		}
		if (textWidth > 0.0d && textHeight > 0.0d)
			scale.set( w / textWidth, h / textHeight, 1.0 );
		pos.y = oldY - textHeight / 2.0 * scale.y;
		trans.setScale( scale );
		trans.setTranslation( pos );

		tg.setTransform( trans );
	}

	public void setHeight( double h ) {
		double oldY = pos.y + textHeight / 2.0 * scale.y;
		myHeight = h;
		myWidth = textWidth / textHeight * h;
		scale.set( myHeight / textHeight, myHeight / textHeight, 1.0 );
		pos.y = oldY - textHeight / 2.0 * scale.y;
		trans.setScale( scale );
		trans.setTranslation( pos );
		tg.setTransform( trans );
	}

	public void setForColor( float r, float g, float b ) {

		ColoringAttributes color = new ColoringAttributes();
		color.setColor(r, g, b);
		this.setFillColor( color );
	}

	public void setForColor( ColoringAttributes color ) {

		this.setFillColor( color );
	}

	public void setFont( String fontName, int fontStyle, int fontSize ) {

		Font3D thisFont = new Font3D( new Font( fontName, fontStyle, fontSize ), defaultExtrusion );
		myText3D.setFont3D( thisFont );
		this.setText( myText );
	}

	public Point2d getSize() {
		return new Point2d( myWidth, myHeight );
	}

	public void setPos( double x, double y, double z ) {
		if( rightJustified ) {
			double length = myText.length();
			double charLength = textWidth / length;
			double distanceToMoveToLeft = charLength * ( length - 1 ) / 2;
			x = x - distanceToMoveToLeft * scale.x;
		}
		else if(leftJustified){
			double length = myText.length();
			double charLength = textWidth / length;
			double distanceToMoveToRight = charLength * ( length - 1 ) / 2;
			x = x + distanceToMoveToRight * scale.x;
		}
		pos.set( x, y - textHeight / 2.0 * scale.y, z );
		trans.setScale( scale );
		trans.setTranslation( pos );

		tg.setTransform( trans );
	}

	public double getTextWidth() {
		return textWidth * scale.x;
	}

	public double getTextHeight() {
		return textHeight * scale.y;
	}

	public String toString() {
		return "[Label] " + super.toString() + " w:" + myWidth + " h:" + myHeight + " t:" + myText;
	}

	public void rotate( double degree ) {
		Transform3D temp = new Transform3D();

		//Read the transform from the shape
		tg.getTransform(temp);

		//Create a rotation that will be applied
		Transform3D tempDelta = new Transform3D();
		tempDelta.rotZ( degree * Math.PI / 180.0 );

		//Apply the rotation
		temp.mul(tempDelta);

		//Write the value back into the scene graph
		tg.setTransform(temp);
	}

	public void setRightJustified( boolean bool ) {
		rightJustified = bool;
		this.setPos( initialPos.x, initialPos.y, initialPos.z );
	}

	public void setLeftJustified( boolean bool ) {
		leftJustified = bool;
		this.setPos( initialPos.x, initialPos.y, initialPos.z );
	}
//	public javax.swing.tree.DefaultMutableTreeNode getTreeNode() {
//		javax.swing.tree.DefaultMutableTreeNode myRoot = super.getTreeNode();
//		myRoot.setUserObject( "[Label]" );
//		myRoot.add( new javax.swing.tree.DefaultMutableTreeNode( "Width: " + myWidth ) );
//		myRoot.add( new javax.swing.tree.DefaultMutableTreeNode( "Height: " + myHeight ) );
//		myRoot.add( new javax.swing.tree.DefaultMutableTreeNode( "Text: " + myText ) );
//		myRoot.add( new javax.swing.tree.DefaultMutableTreeNode( "Scale: " + scale ) );
//		return myRoot;
//	}
}
