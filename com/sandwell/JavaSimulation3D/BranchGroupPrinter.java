/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2011 Ausenco Engineering Canada Inc.
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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.awt.image.MemoryImageSource;
import java.io.File;
import java.io.IOException;

import com.sandwell.JavaSimulation.ErrorException;
import com.sun.j3d.utils.universe.SimpleUniverse;

import javax.imageio.ImageIO;
import javax.media.j3d.Background;
import javax.media.j3d.BoundingSphere;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.Canvas3D;
import javax.media.j3d.ImageComponent;
import javax.media.j3d.ImageComponent2D;
import javax.media.j3d.Screen3D;
import javax.vecmath.Color3f;
import javax.vecmath.Point3d;

public class BranchGroupPrinter {
	private final Canvas3D offScreenCanvasLowRes;
	private final Canvas3D offScreenCanvasHighRes;

	private final SimpleUniverse simpleUniverse;
	private final BranchGroup rootBranchGroup;
	private final Background background;

	// JaamSim folder on temporary folder for storing the printed images
	public final static String imageFolder;
	private static BranchGroupPrinter myInstance;

	static {
		File tempFile = new File(System.getProperty("java.io.tmpdir")+"/JaamSim/");
		if(! tempFile.exists() ) {
			tempFile.mkdir();
		}
		imageFolder = tempFile.getPath() + "/";
	}

	public BranchGroupPrinter() {

		GraphicsConfiguration config = SimpleUniverse.getPreferredConfiguration();

		// BranchGroup may be added or removed to rootBranchGroup
		rootBranchGroup = new BranchGroup();
		rootBranchGroup.setCapability(BranchGroup.ALLOW_DETACH);
		rootBranchGroup.setCapability(BranchGroup.ALLOW_CHILDREN_EXTEND);
		rootBranchGroup.setCapability(BranchGroup.ALLOW_CHILDREN_WRITE);

		offScreenCanvasLowRes = new Canvas3D(config, true);
		initCanvas(offScreenCanvasLowRes, 25, 25);

		offScreenCanvasHighRes = new Canvas3D(config, true);
		initCanvas(offScreenCanvasHighRes, 180, 180);

		simpleUniverse = new SimpleUniverse(offScreenCanvasHighRes);
		simpleUniverse.getViewer().getView().addCanvas3D(offScreenCanvasLowRes);

		// Made sure that a 1 * 1 * 1 cube can be viewed on onScreenCanvas
		simpleUniverse.getViewingPlatform().setNominalViewingTransform();

		// Define background
		BoundingSphere bounds = new BoundingSphere(new Point3d(), Double.POSITIVE_INFINITY);
		background = new Background();
		background.setCapability(Background.ALLOW_COLOR_WRITE);  // Color might change
		background.setApplicationBounds(bounds);
		rootBranchGroup.addChild(background);

		// Defined the same lighting as GraphicSimulation
		GraphicSimulation.setupLightingForBranchGroup_WithinBounds(rootBranchGroup, bounds);

		simpleUniverse.addBranchGraph(rootBranchGroup);
	}

	private void initCanvas(Canvas3D canvas, int width, int height) {
		ImageComponent2D buf = new ImageComponent2D(ImageComponent.FORMAT_RGBA, width, height);
		buf.setCapability(ImageComponent2D.ALLOW_IMAGE_READ);
		buf.set(new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB));
		canvas.setOffScreenBuffer(buf);

		Screen3D screen = canvas.getScreen3D();
		screen.setSize(new Dimension(width, height));
		screen.setPhysicalScreenWidth(0.5d);
		screen.setPhysicalScreenHeight(0.5d);

	}

	synchronized private static BranchGroupPrinter getInstance() {
		if(myInstance == null)
			myInstance = new BranchGroupPrinter();
		return myInstance;
	}

	/**
	 * Print BranchGroup to a low resolution and a high resolution file
	 * on  imageFolder
	 *
	 * @param bg
	 * @param file
	 */
	public static void printBranchGroup_On(BranchGroup bg, String fileName) {
		BranchGroupPrinter printer = BranchGroupPrinter.getInstance();
		printer.rootBranchGroup.addChild(bg);

		fileName = imageFolder + fileName;
		printer.writeBufferedImageOnFile(printer.offScreenCanvasLowRes, fileName + "LowRes.png" );
		printer.writeBufferedImageOnFile(printer.offScreenCanvasHighRes, fileName + "HighRes.png" );
		printer.rootBranchGroup.removeChild(bg);
	}

	private BufferedImage renderOffsceen(Canvas3D canvas) {

		// render the screen and wait for completion
		canvas.renderOffScreenBuffer();
		canvas.waitForOffScreenRendering();

		return canvas.getOffScreenBuffer().getImage();
	}

	/**
	 * Return the transparent Background BufferedImage of a given OffScreenCanvas3D
	 * @param canvas
	 * @return
	 */
	private BufferedImage getTransparentBufferedImageOf(Canvas3D canvas) {

		// Get BufferedImage when background is White
		background.setColor(new Color3f(Color.white));
		BufferedImage whiteBackgroundBufferedImage = renderOffsceen(canvas);

		// Get BufferedImage when background is Black
		background.setColor(new Color3f(Color.black));
		BufferedImage blackBackgroundBufferedImage = renderOffsceen(canvas);

		// List of colors for white background buffered image
		int [] whiteBackgroundPixels = whiteBackgroundBufferedImage.getRGB(
				0, 0, whiteBackgroundBufferedImage.getWidth(), whiteBackgroundBufferedImage.getHeight(), null,
				0, whiteBackgroundBufferedImage.getWidth());

		// List of colors for black background buffered image
		int [] blackBackgroundPixels = blackBackgroundBufferedImage.getRGB(
				0, 0, blackBackgroundBufferedImage.getWidth(), blackBackgroundBufferedImage.getHeight(), null,
				0, blackBackgroundBufferedImage.getWidth());

		// Make background pixels transparent in whiteBackGrounPixels
		for(int i = 0; i < whiteBackgroundPixels.length; i++) {

			// Background pixel
			if (blackBackgroundPixels [i] == 0xFF000000  && whiteBackgroundPixels [i] == 0xFFFFFFFF) {
				whiteBackgroundPixels [i] = 0; // Make pixel transparent
			}
		}

		// Create Transparent background BufferedImage from pixel colors stored in whiteBackgroundPixels
		BufferedImage transparentBackGroundBufferedImage = new BufferedImage(whiteBackgroundBufferedImage.getWidth(),
				whiteBackgroundBufferedImage.getHeight(), BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphic2D = (Graphics2D)transparentBackGroundBufferedImage.getGraphics();
		graphic2D.drawImage(Toolkit.getDefaultToolkit().createImage(new MemoryImageSource(
				transparentBackGroundBufferedImage.getWidth(), transparentBackGroundBufferedImage.getHeight(),
				whiteBackgroundPixels, 0, transparentBackGroundBufferedImage.getWidth())), null, null);

		graphic2D.dispose();
		return transparentBackGroundBufferedImage;
	}

	/**
	 * Write the given BufferedImage to the full path png file
	 * @param bImage
	 * @param file
	 */
	private void writeBufferedImageOnFile(Canvas3D canvas, String file){
		BufferedImage bImage = this.getTransparentBufferedImageOf(canvas);
		// Print the transparent bufferedImage to the file
		try {
			ImageIO.write(bImage, "png", new File(file));
		} catch (IOException e) {
			throw new ErrorException(e);
		}

	}

	public static void renderBranchGroup_On(DisplayModel dm) {
		BranchGroupPrinter printer = BranchGroupPrinter.getInstance();
		BranchGroup bg = dm.getUnitDisplayModel();
		printer.rootBranchGroup.addChild(bg);
		BufferedImage lowRes = printer.getTransparentBufferedImageOf(printer.offScreenCanvasLowRes);
		BufferedImage highRes = printer.getTransparentBufferedImageOf(printer.offScreenCanvasHighRes);
		printer.rootBranchGroup.removeChild(bg);
		dm.setImages(lowRes, highRes);

	}
}