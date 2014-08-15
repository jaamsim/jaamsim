/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2014 Ausenco Engineering Canada Inc.
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
package com.jaamsim.BasicObjects;

import java.util.ArrayList;

import com.jaamsim.input.IntegerInput;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.ValueInput;
import com.jaamsim.math.Vec3d;
import com.jaamsim.units.DistanceUnit;
import com.sandwell.JavaSimulation3D.DisplayEntity;

public class EntityContainer extends SimEntity {

	@Keyword(description = "The amount of graphical space shown between entities in the container.",
	         example = "Container1 Spacing { 1 m }")
	private final ValueInput spacingInput;

	@Keyword(description = "The number of entities in each row inside the container.",
			example = "Queue1 MaxPerLine { 4 }")
	protected final IntegerInput maxPerLineInput;

	private ArrayList<DisplayEntity> entityList;

	{
		spacingInput = new ValueInput("Spacing", "Key Inputs", 0.0d);
		spacingInput.setUnitType(DistanceUnit.class);
		spacingInput.setValidRange(0.0d, Double.POSITIVE_INFINITY);
		this.addInput(spacingInput);

		maxPerLineInput = new IntegerInput("MaxPerLine", "Key Inputs", Integer.MAX_VALUE);
		maxPerLineInput.setValidRange( 1, Integer.MAX_VALUE);
		this.addInput(maxPerLineInput);
	}

	public EntityContainer() {
		entityList = new ArrayList<DisplayEntity>();
	}

	@Override
	public void earlyInit() {
		super.earlyInit();
		entityList.clear();
	}

	public void addEntity(DisplayEntity ent) {
		entityList.add(ent);
	}

	public DisplayEntity removeEntity() {
		DisplayEntity ent = entityList.remove(entityList.size()-1);
		return ent;
	}

	public int getCount() {
		return entityList.size();
	}

	@Override
	public void kill() {
		for (DisplayEntity ent : entityList) {
			ent.kill();
		}
		super.kill();
	}

	/**
	 * Update the position of all entities in the queue. ASSUME that entities
	 * will line up according to the orientation of the queue.
	 */
	@Override
	public void updateGraphics( double simTime ) {

		Vec3d orient = getOrientation();
		Vec3d size = this.getSize();
		Vec3d tmp = new Vec3d();

		// Find widest entity
		double maxWidth = 0;
		for (int j = 0; j < entityList.size(); j++) {
			maxWidth = Math.max(maxWidth, entityList.get(j).getSize().y);
		}

		// Update the position of each entity (start at the bottom left of the container)
		double distanceX = -0.5*size.x;
		double distanceY = -0.5*size.y + 0.5*maxWidth;
		for (int i = 0; i < entityList.size(); i++) {

			// if new row is required, reset distanceX and move distanceY up one row
			if (i > 0 && i % maxPerLineInput.getValue() == 0){
				 distanceX = -0.5*size.x;
				 distanceY += spacingInput.getValue() + maxWidth;
			}

			// Rotate each entity about its center so it points to the right direction
			DisplayEntity item = entityList.get(i);
			item.setOrientation(orient);

			// Set Position
			Vec3d itemSize = item.getSize();
			distanceX += spacingInput.getValue() + 0.5*itemSize.x;
			tmp.set3(distanceX/size.x, distanceY/size.y, 0.0d);
			Vec3d itemCenter = this.getGlobalPositionForAlignment(tmp);
			item.setPositionForAlignment(new Vec3d(), itemCenter);

			// increment total distance
			distanceX += 0.5*itemSize.x;
		}
	}

}
