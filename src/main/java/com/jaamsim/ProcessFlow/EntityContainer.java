/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2014 Ausenco Engineering Canada Inc.
 * Copyright (C) 2018 JaamSim Software Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jaamsim.ProcessFlow;

import java.util.ArrayList;

import com.jaamsim.Graphics.DisplayEntity;
import com.jaamsim.input.BooleanInput;
import com.jaamsim.input.IntegerInput;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.Output;
import com.jaamsim.input.ValueInput;
import com.jaamsim.input.Vec3dInput;
import com.jaamsim.math.Vec3d;
import com.jaamsim.states.StateEntity;
import com.jaamsim.states.StateRecord;
import com.jaamsim.units.DimensionlessUnit;
import com.jaamsim.units.DistanceUnit;

public class EntityContainer extends SimEntity {

	@Keyword(description = "The position of the first entity in the container relative to the container.",
	         exampleList = {"1.0 0.0 0.01 m"})
	protected final Vec3dInput positionOffset;

	@Keyword(description = "The amount of graphical space shown between entities in the container.",
	         exampleList = {"1 m"})
	private final ValueInput spacingInput;

	@Keyword(description = "The number of entities in each row inside the container.",
			exampleList = {"4"})
	protected final IntegerInput maxPerLineInput;

	@Keyword(description = "If TRUE, the entities in the EntityContainer are displayed.",
			exampleList = {"FALSE"})
	protected final BooleanInput showEntities;

	private ArrayList<DisplayEntity> entityList;

	{
		positionOffset = new Vec3dInput("PositionOffset", KEY_INPUTS, new Vec3d(0.0d, 0.0d, 0.01d));
		positionOffset.setUnitType(DistanceUnit.class);
		this.addInput(positionOffset);

		spacingInput = new ValueInput("Spacing", KEY_INPUTS, 0.0d);
		spacingInput.setUnitType(DistanceUnit.class);
		spacingInput.setValidRange(0.0d, Double.POSITIVE_INFINITY);
		this.addInput(spacingInput);

		maxPerLineInput = new IntegerInput("MaxPerLine", KEY_INPUTS, Integer.MAX_VALUE);
		maxPerLineInput.setValidRange( 1, Integer.MAX_VALUE);
		this.addInput(maxPerLineInput);

		showEntities = new BooleanInput("ShowEntities", KEY_INPUTS, true);
		this.addInput(showEntities);
	}

	public EntityContainer() {
		entityList = new ArrayList<>();
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
		if (!showEntities.getValue()) {
			ent.setShow(true);
		}
		return ent;
	}

	public int getCount() {
		return entityList.size();
	}

	@Override
	public void stateChanged(StateRecord prev, StateRecord next) {
		super.stateChanged(prev, next);

		// Set the states for the entities carried by the EntityContainer to the new state
		for (DisplayEntity ent : entityList) {
			if (ent instanceof StateEntity)
				((StateEntity)ent).setPresentState(next.name);
		}
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

		boolean visible = showEntities.getValue();
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
			item.setShow(visible);
			item.setOrientation(orient);

			// Set Position
			Vec3d itemSize = item.getSize();
			distanceX += spacingInput.getValue() + 0.5*itemSize.x;
			tmp.set3(distanceX/size.x, distanceY/size.y, 0.0d);
			Vec3d itemCenter = this.getGlobalPositionForAlignment(tmp);
			itemCenter.add3(positionOffset.getValue());
			item.setGlobalPositionForAlignment(new Vec3d(), itemCenter);

			// increment total distance
			distanceX += 0.5*itemSize.x;
		}
	}

	@Output(name = "Count",
	 description = "The present number of entities in the EntityContainer.",
	    unitType = DimensionlessUnit.class,
	    sequence = 1)
	public int getCount(double simTime) {
		return entityList.size();
	}

	@Output(name = "EntityList",
	 description = "The entities contained by the EntityContainer.",
	    unitType = DimensionlessUnit.class,
	    sequence = 2)
	public ArrayList<DisplayEntity> getEntityList(double simTime) {
		return entityList;
	}

}
