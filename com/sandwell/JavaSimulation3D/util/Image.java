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

import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.WritableRaster;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.media.j3d.Appearance;
import javax.media.j3d.ColoringAttributes;
import javax.media.j3d.ImageComponent;
import javax.media.j3d.ImageComponent2D;
import javax.media.j3d.QuadArray;
import javax.media.j3d.Shape3D;
import javax.media.j3d.Texture;
import javax.media.j3d.Texture2D;
import javax.media.j3d.TextureAttributes;
import javax.media.j3d.TransparencyAttributes;
import javax.vecmath.Point2d;

/**
 * Utility class that provides a texture filled rectangle. Created to create a
 * quick graphic representation of a 2D object in J3D.
 */
public class Image extends Shape2D {

	protected QuadArray plane;
	protected double myWidth;
	protected double myHeight;
	protected java.net.URL fname;
	protected float[] texCoords = { 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f };
	private static ColorSpace cs = ColorSpace.getInstance( ColorSpace.CS_sRGB );
	private static int[] nBits = { 8, 8, 8, 8 };
	private static int[] bandOffset = { 0, 1, 2, 3 };
	private static ComponentColorModel colorModel = new ComponentColorModel( cs, nBits, true, false, Transparency.TRANSLUCENT, 0 );

	private static int maxTexPixels = 2048;
	private static TextureAttributes texAttr;
	private static TransparencyAttributes ta;
	static {
		texAttr = new TextureAttributes();
		texAttr.setTextureMode( TextureAttributes.MODULATE );

		ta = new TransparencyAttributes();
		ta.setTransparencyMode( TransparencyAttributes.BLENDED );
		ta.setTransparency( 0f );
	}

	/** Constructs a Rectangle with the specified dimensions */
	public Image( double x, double y, double width, double height, java.net.URL filename ) {
		// setup the Shape2D
		super();

		fname = filename;
		Texture tex = getTexture( filename );

		plane = new QuadArray( 4, QuadArray.COORDINATES | QuadArray.TEXTURE_COORDINATE_2 );
		plane.setCapability( QuadArray.ALLOW_COORDINATE_WRITE );
		plane.setCapability( QuadArray.ALLOW_COORDINATE_READ );

		filledAppearance.setTexture( tex );
		fill.setCapability( Shape3D.ALLOW_APPEARANCE_WRITE );

		filledAppearance.setTextureAttributes( texAttr );
		filledAppearance.setTransparencyAttributes( ta );

		assignGeometry( x, y, 0.0, width, height );

		// orient the image
		plane.setTextureCoordinates( 0, 0, texCoords );

		removeChild( outline );
		outlineAppearance = null;
		outline = null;
	}

	public Image( double x, double y, double width, double height, String filename ) throws java.net.MalformedURLException {
		//this( x, y, width, height, new java.net.URL( "file://" + filename ) );
		this( x, y, width, height, new java.net.URL( "file:" + filename ) );
	}

	public void assignGeometry( double x, double y, double z, double width, double height ) {
		double halfWidth = width / 2.0;
		double halfHeight = height / 2.0;

		// store for later
		myWidth = width;
		myHeight = height;
		pos.set( x, y, z );

		// build a Quadrangle for the shape's geometry

		double[] verts = { x - halfWidth, y - halfHeight, z, x + halfWidth, y - halfHeight, z, x + halfWidth, y + halfHeight, z, x - halfWidth, y + halfHeight, z };

		// populate the QuadArray
		plane.setCoordinates( 0, verts );

		fill.setGeometry( plane );
	}

	/** Constructs an Image with the specified dimension with center at (0,0) */
	public Image( double width, double height, String filename ) throws java.net.MalformedURLException {
		//this( 0.0, 0.0, width, height, new java.net.URL( "file:" + filename ) );
		this( 0.0, 0.0, width, height, filename );
	}

	/** Constructs an Image with the specified dimension with center at (0,0) */
	public Image( double width, double height, java.net.URL filename ) {
		this( 0.0, 0.0, width, height, filename );
	}

	public void setSize( double w, double h ) {
		assignGeometry( pos.x, pos.y, pos.z, w, h );
	}

	public Point2d getSize() {
		return new Point2d( myWidth, myHeight );
	}

	public void setPos( double x, double y, double z ) {
		assignGeometry( x, y, z , myWidth, myHeight );
	}

	/** Returns the color for the fill area of the shape */
	public ColoringAttributes getFillColor() {
		throw new RuntimeException( "Not supported by Image" );
	}

	/** Returns the color for the outline of the shape */
	public ColoringAttributes getOutlineColor() {
		throw new RuntimeException( "Not supported by Image" );
	}

	/** assigns the color for the fill area of the shape */
	public void setFillColor( ColoringAttributes fillColor ) {
		throw new RuntimeException( "Not supported by Image" );
	}

	/** assigns the color for the outline of the shape */
	public void setOutlineColor( ColoringAttributes outlineColor ) {
		throw new RuntimeException( "Not supported by Image" );
	}

	/** returns the full appearance for the fill area of the shape */
	public Appearance getFillAppearance() {
		throw new RuntimeException( "Not supported by Image" );
	}

	/** returns the full appearance for the outline of the shape */
	public Appearance getOutlineAppearance() {
		throw new RuntimeException( "Not supported by Image" );
	}

	public void setFillAppearance( javax.media.j3d.Appearance app ) {
		throw new RuntimeException( "Not supported by Image" );
	}

	public void setOutlineAppearance( javax.media.j3d.Appearance app ) {
		throw new RuntimeException( "Not supported by Image" );
	}

	/**
	 * Basic functionality to generate a texture.
	 *
	 * @param filename
	 */
	private Texture getTexture( final java.net.URL filename ) {
		// Thanks to the Java3D folks for creating a maze of format constants
		int imageComponentFormat = ImageComponent.FORMAT_RGBA;
		int textureFormat = Texture.RGBA;

		// The texture we are creating
		Texture tex = null;

		// This is magic, and took way too long to figure out
		BufferedImage bImage = java.security.AccessController.doPrivileged(
		new java.security.PrivilegedAction<BufferedImage>() {

			public BufferedImage run() {
				try {
					return ImageIO.read(filename);
				} catch (IOException e) {
					System.err.println(e);
					return null;
				}
			}
		});

		if( bImage == null ) {
			System.err.println( "Error loading Image " + filename.toString() );
			return null;
		}

		ImageComponent2D scaledImage = null;
		BufferedImage finalImage = null;

		int imageWidth = bImage.getWidth();
		int imageHeight = bImage.getHeight();
		float transformScale = 1.0f;

		if( imageWidth > maxTexPixels || imageHeight > maxTexPixels ) {
			if( imageWidth > imageHeight ) {
				transformScale = (float)maxTexPixels / (float)imageWidth;
			}
			else {
				transformScale = (float)maxTexPixels / (float)imageHeight;
			}
		}

		int finalWidth = Math.min( (int)(imageWidth * transformScale + 0.5), maxTexPixels );
		int finalHeight = Math.min( (int)(imageHeight * transformScale + 0.5), maxTexPixels );
		int goalWidth = getClosestPowerOf2( finalWidth );
		int goalHeight = getClosestPowerOf2( finalHeight );
		float texCoordx = (float)finalWidth / (float)goalWidth;
		float texCoordy = 1 - ((float)finalHeight / (float)goalHeight);

		// Set the texture coords in the padded texture
		texCoords[0] = 0.0f;
		texCoords[1] = texCoordy;
		texCoords[2] = texCoordx;
		texCoords[3] = texCoordy;
		texCoords[4] = texCoordx;
		texCoords[5] = 1.0f;
		texCoords[6] = 0.0f;
		texCoords[7] = 1.0f;

		// Create texture from image
		finalImage = getScaledImage( bImage, transformScale, goalWidth, goalHeight );
		scaledImage = new ImageComponent2D( imageComponentFormat, finalImage, false, false );

		tex = new Texture2D( Texture.BASE_LEVEL, textureFormat, goalWidth, goalHeight );
		tex.setImage( 0, scaledImage );

		tex.setMinFilter( Texture.BASE_LEVEL_LINEAR );
		tex.setMagFilter( Texture.BASE_LEVEL_LINEAR );
		return tex;
	}

	/**
	 * Find a capped power of 2 value of the texture being loaded.
	 *
	 * @param current
	 * @return
	 */
	private int getClosestPowerOf2( int current ) {
		for( int i = 32; i <= maxTexPixels; i *= 2 ) {
			if( current <= i ) {
				return i;
			}
		}
		return maxTexPixels;
	}

	 // return a scaled image of given x and y scale
	private BufferedImage getScaledImage( BufferedImage origImage, float scale, int Xpixels, int Ypixels ) {

		WritableRaster wr = java.awt.image.Raster.createInterleavedRaster( DataBuffer.TYPE_BYTE, Xpixels, Ypixels, Xpixels * 4, 4, bandOffset, null );
		BufferedImage scaledImage = new BufferedImage( colorModel, wr, false, null );

		java.awt.Graphics2D g2 = scaledImage.createGraphics();
		AffineTransform at = AffineTransform.getScaleInstance( scale, scale );
		g2.transform( at );
		g2.drawImage( origImage, 0, 0, null );

		return scaledImage;
	}

	public String toString() {
		return "[Image]" + super.toString() + " f:" + fname;
	}

//	public javax.swing.tree.DefaultMutableTreeNode getTreeNode() {
//		javax.swing.tree.DefaultMutableTreeNode myRoot = super.getTreeNode();
//		myRoot.setUserObject( "[Image]" );
//		myRoot.add( new javax.swing.tree.DefaultMutableTreeNode( "Image File: " + fname ) );
//		return myRoot;
//	}
}
