/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2012 Ausenco Engineering Canada Inc.
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

import javax.media.j3d.ColoringAttributes;
import javax.vecmath.Point2d;

import com.sandwell.JavaSimulation.DoubleVector;

public class SideBySideBars extends Shape {

	private final ArrayList<Rectangle> rectangles;
	private final ArrayList<Rectangle> rectangleOutlines;
	private Point2d dimension;

	public SideBySideBars() {
		rectangles = new ArrayList<Rectangle>();
		rectangleOutlines = new ArrayList<Rectangle>();
	}

	public void createInitialGeometry() {
		this.setCapability(ALLOW_CHILDREN_EXTEND);
		this.setCapability(ALLOW_CHILDREN_WRITE);
	}

	public void setDimension(Point2d dim){
		dimension = dim;
	}

	public void setSize(double size) {
		DoubleVector sizes = new DoubleVector(1);
		sizes.add(size);
		this.setSize(sizes);
	}

	private void createRectanglesForBars(int numberOfBars) {

		// Add rectangles and outlines while they are needed
		while(rectangles.size() < numberOfBars) {
			Rectangle rect = new Rectangle(0, 0, Shape.SHAPE_FILLED, "rectangle-" + rectangles.size());
			rectangles.add(rect);
			this.addChild(rect);

			rect = new Rectangle(0, 0, Shape.SHAPE_OUTLINE, "rectangleOutline-" + rectangles.size());
			rect.setColor(Shape.getPresetColor(Shape.COLOR_BLACK));
			rectangleOutlines.add(rect);
			this.addChild(rect);
		}
	}

	public void setSize(DoubleVector sizes) {

		this.createRectanglesForBars(sizes.size());
		double usedWidth = 0;
		for( int index = 0; index < sizes.size(); index++ ) {
			double width = dimension.getX()/sizes.size();
			rectangles.get(index).setSize(width, sizes.get(index));
			rectangles.get(index).setCenter(-0.5 + usedWidth + (width/2.0), -0.5 + (sizes.get(index)/2.0), centreZ);
			rectangleOutlines.get(index).setSize(width, sizes.get(index));
			rectangleOutlines.get(index).setCenter(-0.5 + usedWidth + (width/2.0), -0.5 + (sizes.get(index)/2.0), centreZ);
			usedWidth += width;
		}
	}

	public void setColor(ColoringAttributes color) {
		ColoringAttributes [] colors = new ColoringAttributes[1];
		colors[0] = color;
		this.setColor(colors);
	}

	public void setColor(ColoringAttributes [] colors) {

		this.createRectanglesForBars(colors.length);
		for( int index = 0; index < colors.length; index++ ) {
			rectangles.get(index).setColor(colors[index]);
		}
	}
}
