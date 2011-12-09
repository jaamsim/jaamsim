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
package com.sandwell.JavaSimulation3D.util;

import java.util.ArrayList;

import javax.media.j3d.ColoringAttributes;
import javax.vecmath.Point2d;

import com.sandwell.JavaSimulation.DoubleVector;

public class StackedBar extends Shape {

	private final ArrayList<Rectangle> rectangles;
	private Point2d dimension;

	public StackedBar(){
		rectangles = new ArrayList<Rectangle>();
	}

	public void createInitialGeometry(){
		this.setCapability(ALLOW_CHILDREN_EXTEND);
		this.setCapability(ALLOW_CHILDREN_WRITE);
	}

	public void setDimension(Point2d dim){
		dimension = dim;
	}

	public void setSize(double size){
		DoubleVector sizes = new DoubleVector(1);
		sizes.add(size);
		this.setSize(sizes);
	}

	private void createRectanglesForBars(int numberOfBars) {

		// Add rectangle while they are needed
		while(rectangles.size() < numberOfBars){
			rectangles.add(new Rectangle(0, 0, Shape.SHAPE_FILLED, "rectangle-" + rectangles.size()));
			this.addChild(rectangles.get(rectangles.size()-1));
		}
	}

	public void setSize(DoubleVector sizes){

		this.createRectanglesForBars(sizes.size());
		double sizesSum = sizes.sum();
		double usedWidth = 0;
		int index = 0;
		for(Rectangle each: rectangles){
			if(index >= sizes.size()){
				break;
			}
			double width = dimension.getX()*sizes.get(index)/sizesSum;
			each.setSize(width, dimension.getY());
			each.setCenter(centreX + usedWidth + width/2, centreY, centreZ);
			usedWidth += width;
			index++;
		}
	}

	public void setColor(ColoringAttributes color){
		ColoringAttributes [] colors = new ColoringAttributes[1];
		colors[0] = color;
		this.setColor(colors);
	}

	public void setColor(ColoringAttributes [] colors){

		this.createRectanglesForBars(colors.length);
		int index = 0;
		for(Rectangle each: rectangles){
			if(index >= colors.length){
				break;
			}
			each.setColor(colors[index]);
			index++;
		}
	}
}
