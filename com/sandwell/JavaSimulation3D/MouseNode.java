/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2007-2011 Ausenco Engineering Canada Inc.
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

import javax.media.j3d.ColoringAttributes;
import javax.vecmath.Vector3d;

import com.sandwell.JavaSimulation.Entity;
import com.sandwell.JavaSimulation3D.DisplayEntity;
import com.sandwell.JavaSimulation3D.util.Shape;

/**
 * Creates a grabber 'mouseNode' which allows for graphical modification of x/y
 * coordinate of a part of a DisplayEntity written with segments/paths in mind,
 * calls 'setStartPoint_EndPoint', could be modified to change everything.
 */
public class MouseNode extends DisplayEntity {

	private DisplayEntity theEntity; // object to be adjusted is going to be a DisplayEntity.
	private ColoringAttributes color;

	public MouseNode(DisplayEntity ent) {
		this(ent, ent.getMouseNodesSize(), Shape.COLOR_BLUE);
	}

	public MouseNode( DisplayEntity ent, double size, int col) {
		color = Shape.getPresetColor(col);
		theEntity = ent;
		ent.getMouseNodes().add( this );
		this.setSize(new Vector3d(size, size, 0.0d));
		this.setFlag(Entity.FLAG_GENERATED);
	}

	public void dragged(Vector3d distance) {
		super.dragged(distance);

		theEntity.initializeGraphics();
		theEntity.updateInputPosition();
	}

	public void setColor(ColoringAttributes col) {
		color = col;
		for(DisplayModelBG each: displayModels){
			each.setNode_Color(DisplayModel.TAG_OUTLINES, color);
		}
	}

	public void updateInputPosition() {}

	public void render(double time) {
		super.render(time);
		this.setColor(color);
	}

	public void setResizeBounds(boolean bool) {
		theEntity.setResizeBounds(bool);
	}
}
