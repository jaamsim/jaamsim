/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2014 Ausenco Engineering Canada Inc.
 * Copyright (C) 2018-2019 JaamSim Software Inc.
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
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;

import com.jaamsim.Graphics.DisplayEntity;
import com.jaamsim.Samples.SampleConstant;
import com.jaamsim.Samples.SampleInput;
import com.jaamsim.StringProviders.StringProvInput;
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
import com.jaamsim.units.TimeUnit;

public class EntityContainer extends SimEntity implements EntContainer {

	@Keyword(description = "The priority for positioning the received entity in the container. "
	                     + "Priority is integer valued and a lower numerical value indicates a "
	                     + "higher priority. "
	                     + "For example, priority 3 is higher than 4, and priorities 3, 3.2, and "
	                     + "3.8 are equivalent.",
	         exampleList = {"this.obj.Attrib1"})
	private final SampleInput priority;

	@Keyword(description = "An expression returning a string value that categorizes the entities "
	                     + "in the container. "
	                     + "The expression is evaluated and the value saved when the entity is "
	                     + "first loaded into the container. "
	                     + "Expressions that return a dimensionless integer or an object are also "
	                     + "valid. The returned number or object is converted to a string "
	                     + "automatically. A floating point number is truncated to an integer.",
	         exampleList = {"this.obj.Attrib1"})
	private final StringProvInput match;

	@Keyword(description = "Determines the order in which entities are placed in the container "
	                     + "(FIFO or LIFO):\n"
	                     + "TRUE = first in first out (FIFO) order (the default setting),\n"
	                     + "FALSE = last in first out (LIFO) order.",
	         exampleList = {"FALSE"})
	private final BooleanInput fifo;

	@Keyword(description = "Determines whether the states for the entities held by the container "
	                     + "are updated to match each change to the container's state.",
	         exampleList = {"FALSE"})
	private final BooleanInput setEntityState;

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

	private EntContainerDelegate container;

	{
		priority = new SampleInput("Priority", KEY_INPUTS, new SampleConstant(0));
		priority.setUnitType(DimensionlessUnit.class);
		priority.setEntity(this);
		priority.setValidRange(0.0d, Double.POSITIVE_INFINITY);
		this.addInput(priority);

		match = new StringProvInput("Match", KEY_INPUTS, null);
		match.setUnitType(DimensionlessUnit.class);
		match.setEntity(this);
		this.addInput(match);

		fifo = new BooleanInput("FIFO", KEY_INPUTS, true);
		this.addInput(fifo);

		setEntityState = new BooleanInput("SetEntityState", KEY_INPUTS, true);
		this.addInput(setEntityState);

		positionOffset = new Vec3dInput("PositionOffset", GRAPHICS, new Vec3d(0.0d, 0.0d, 0.01d));
		positionOffset.setUnitType(DistanceUnit.class);
		this.addInput(positionOffset);

		spacingInput = new ValueInput("Spacing", FORMAT, 0.0d);
		spacingInput.setUnitType(DistanceUnit.class);
		this.addInput(spacingInput);

		maxPerLineInput = new IntegerInput("MaxPerLine", FORMAT, Integer.MAX_VALUE);
		maxPerLineInput.setValidRange( 1, Integer.MAX_VALUE);
		this.addInput(maxPerLineInput);

		showEntities = new BooleanInput("ShowEntities", FORMAT, true);
		this.addInput(showEntities);
	}

	public EntityContainer() {
		container = new EntContainerDelegate();
	}

	@Override
	public void earlyInit() {
		super.earlyInit();
		container.clear();
	}

	@Override
	public void registerEntity(DisplayEntity ent) {
		container.registerEntity(ent);
	}

	@Override
	public void addEntity(DisplayEntity ent) {
		double simTime = getSimTime();

		// Register the entity so that the outputs are updated before the expressions for priority
		// and match value are evaluated
		registerEntity(ent);

		// Determine the priority and match value for the received entity
		int pri = (int) priority.getValue().getNextSample(simTime);

		String m = null;
		if (match.getValue() != null)
			m = match.getValue().getNextString(simTime, 1.0d, true);

		container.addEntity(ent, m, pri, fifo.getValue(), simTime);
	}

	@Override
	public DisplayEntity removeEntity(String m) {
		DisplayEntity ent = container.removeEntity(m);
		ent.setShow(true);
		return ent;
	}

	@Override
	public int getCount(String m) {
		return container.getCount(m);
	}

	@Override
	public boolean isEmpty(String m) {
		return container.isEmpty(m);
	}

	@Override
	public void stateChanged(StateRecord prev, StateRecord next) {
		super.stateChanged(prev, next);

		if (!setEntityState.getValue())
			return;

		// Set the states for the entities carried by the EntityContainer to the new state
		Iterator<DisplayEntity> itr = container.iterator();
		while (itr.hasNext()) {
			DisplayEntity ent = itr.next();
			if (ent instanceof StateEntity)
				((StateEntity)ent).setPresentState(next.getName());
		}
	}

	@Override
	public void kill() {
		Iterator<DisplayEntity> itr = container.iterator();
		while (itr.hasNext()) {
			DisplayEntity ent = itr.next();
			ent.kill();
		}
		super.kill();
	}

	@Override
	public void clearStatistics() {
		super.clearStatistics();
		container.clearStatistics();
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
		Iterator<DisplayEntity> itr = container.iterator();
		while (itr.hasNext()) {
			DisplayEntity ent = itr.next();
			maxWidth = Math.max(maxWidth, ent.getGlobalSize().y);
		}

		// Update the position of each entity (start at the bottom left of the container)
		double distanceX = -0.5*size.x;
		double distanceY = -0.5*size.y + 0.5*maxWidth;
		itr = container.iterator();
		int i = 0;
		while (itr.hasNext()) {
			DisplayEntity item = itr.next();

			// if new row is required, reset distanceX and move distanceY up one row
			if (i > 0 && i % maxPerLineInput.getValue() == 0){
				 distanceX = -0.5*size.x;
				 distanceY += spacingInput.getValue() + maxWidth;
			}

			// Rotate each entity about its center so it points to the right direction
			item.setShow(visible);
			item.setRelativeOrientation(orient);

			// Set Position
			Vec3d itemSize = item.getGlobalSize();
			distanceX += 0.5*itemSize.x;
			tmp.set3(distanceX/size.x, distanceY/size.y, 0.0d);
			Vec3d itemCenter = this.getGlobalPositionForAlignment(tmp);
			itemCenter.add3(positionOffset.getValue());
			item.setGlobalPositionForAlignment(new Vec3d(), itemCenter);

			// increment total distance
			distanceX += 0.5*itemSize.x + spacingInput.getValue();
			i++;
		}
	}

	@Output(name = "obj",
	 description = "The entity that was loaded most recently.",
	    sequence = 0)
	public DisplayEntity getLastEntity(double simTime) {
		return container.getLastEntity();
	}

	@Output(name = "NumberAdded",
	 description = "The number of entities loaded after the initialization period.",
	    unitType = DimensionlessUnit.class,
	  reportable = true,
	    sequence = 1)
	public long getNumberAdded(double simTime) {
		return container.getTotalNumberAdded();
	}

	@Output(name = "NumberRemoved",
	 description = "The number of entities unloaded after the initialization period.",
	    unitType = DimensionlessUnit.class,
	  reportable = true,
	    sequence = 2)
	public long getNumberRemoved(double simTime) {
		return container.getTotalNumberProcessed();
	}

	@Output(name = "Count",
	 description = "The present number of entities in the EntityContainer.",
	    unitType = DimensionlessUnit.class,
	    sequence = 3)
	public int getCount(double simTime) {
		return getCount(null);
	}

	@Output(name = "EntityList",
	 description = "The entities contained by the EntityContainer.",
	    unitType = DimensionlessUnit.class,
	    sequence = 4)
	public ArrayList<DisplayEntity> getEntityList(double simTime) {
		return container.getEntityList(null);
	}

	@Output(name = "PriorityValues",
	 description = "The Priority expression value for each entity in the queue.",
	    unitType = DimensionlessUnit.class,
	    sequence = 5)
	public ArrayList<Integer> getPriorityValues(double simTime) {
		return container.getPriorityList();
	}

	@Output(name = "MatchValues",
	 description = "The Match expression value for each entity in the queue.",
	    unitType = DimensionlessUnit.class,
	    sequence = 6)
	public ArrayList<String> getMatchValues(double simTime) {
		return container.getTypeList();
	}

	@Output(name = "StorageTimes",
	 description = "The elapsed time since each entity was placed in storage.",
	    unitType = TimeUnit.class,
	    sequence = 7)
	public ArrayList<Double> getStorageTimes(double simTime) {
		return container.getStorageTimeList(simTime);
	}

	@Output(name = "MatchValueCount",
	 description = "The present number of unique match values in the container.",
	    unitType = DimensionlessUnit.class,
	    sequence = 8)
	public int getMatchValueCount(double simTime) {
		return container.getTypeList().size();
	}

	@Output(name = "UniqueMatchValues",
	 description = "The list of unique Match values for the entities in the container.",
	    sequence = 9)
	public ArrayList<String> getUniqueMatchValues(double simTime) {
		ArrayList<String> ret = new ArrayList<>(container.getTypeList());
		Collections.sort(ret);
		return ret;
	}

	@Output(name = "MatchValueCountMap",
	 description = "The number of entities in the container for each Match expression value.\n"
	             + "For example, '[EntityContainer1].MatchValueCountMap(\"SKU1\")' returns the "
	             + "number of entities whose Match value is \"SKU1\".",
	    unitType = DimensionlessUnit.class,
	    sequence = 10)
	public LinkedHashMap<String, Integer> getMatchValueCountMap(double simTime) {
		LinkedHashMap<String, Integer> ret = new LinkedHashMap<>(container.getTypeList().size());
		for (String m : getUniqueMatchValues(simTime)) {
			ret.put(m, getCount(m));
		}
		return ret;
	}

	@Output(name = "MatchValueMap",
	 description = "Provides a list of entities in the container for each Match expression "
	             + "value.\n"
	             + "For example, '[EntityContainer1].MatchValueMap(\"SKU1\")' returns a list of "
	             + "entities whose Match value is \"SKU1\".",
	    sequence = 11)
	public LinkedHashMap<String, ArrayList<DisplayEntity>> getMatchValueMap(double simTime) {
		LinkedHashMap<String, ArrayList<DisplayEntity>> ret = new LinkedHashMap<>(container.getTypeList().size());
		for (String m : getUniqueMatchValues(simTime)) {
			ret.put(m, container.getEntityList(m));
		}
		return ret;
	}

}
