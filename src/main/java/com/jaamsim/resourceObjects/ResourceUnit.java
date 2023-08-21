/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2018-2023 JaamSim Software Inc.
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
package com.jaamsim.resourceObjects;

import java.util.ArrayList;

import com.jaamsim.BasicObjects.DowntimeEntity;
import com.jaamsim.BooleanProviders.BooleanProvInput;
import com.jaamsim.DisplayModels.ShapeModel;
import com.jaamsim.Graphics.DisplayEntity;
import com.jaamsim.ProcessFlow.StateUserEntity;
import com.jaamsim.Statistics.TimeBasedFrequency;
import com.jaamsim.Statistics.TimeBasedStatistics;
import com.jaamsim.events.EventManager;
import com.jaamsim.input.ColourInput;
import com.jaamsim.input.EntityInput;
import com.jaamsim.input.ExpResType;
import com.jaamsim.input.ExpressionInput;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.Output;
import com.jaamsim.input.Vec3dInput;
import com.jaamsim.math.Color4d;
import com.jaamsim.math.Vec3d;
import com.jaamsim.units.DimensionlessUnit;
import com.jaamsim.units.DistanceUnit;
import com.jaamsim.units.TimeUnit;

public class ResourceUnit extends StateUserEntity implements Seizable, ResourceProvider {

	@Keyword(description = "The name of the ResourcePool from which this ResourceUnit can be "
	                     + "selected. If no pool is specified, the ResourceUnit itself is "
	                     + "considered to be a ResourcePool with one unit.",
	         exampleList = {"ResourcePool1"})
	private final EntityInput<ResourcePool> resourcePool;

	@Keyword(description = "An optional expression that tests whether an entity is elible to "
	                     + "seize this unit. "
	                     + "The entry 'this.Assignment' represents the entity that is being "
	                     + "tested in the expression. "
	                     + "The expression should return 1 (true) if the entity is eligible.",
	         exampleList = {"'this.Assignment.type == 1'"})
	private final ExpressionInput assignmentCondition;

	@Keyword(description = "An optional expression that returns the priority for this unit to be "
	                     + "used by the ResourcePool when choosing the next unit to be seized. "
	                     + "The calculated priority should be a positive integer, with a lower "
	                     + "value indicating a higher priority. "
	                     + "The entry 'this.Assignment' can be used in the expression to "
	                     + "represent the entity that would seize the unit.",
	         exampleList = {"'this.Assignment.type == 1 ? 1 : 2'"})
	private final ExpressionInput assignmentPriority;

	@Keyword(description = "If TRUE, the ResourceUnit will move next to the entity that has "
	                     + "seized it, and will follow that entity until it is released.")
	private final BooleanProvInput followAssignment;

	@Keyword(description = "The position of the ResourceUnit relative to the entity that has seized it.",
	         exampleList = {"0.0 1.0 0.01 m"})
	protected final Vec3dInput assignmentOffset;

	private DisplayEntity presentAssignment;  // entity to which this unit is assigned
	private long assignmentTicks;   // clock ticks at which the unit was assigned
	private long lastReleaseTicks;  // clock ticks at which the unit was unassigned
	private ArrayList<ResourceUser> userList;  // objects that can use this resource

	//	Statistics
	private int unitsSeized;    // number of units that have been seized
	private int unitsReleased;  // number of units that have been released
	private final TimeBasedStatistics stats;
	private final TimeBasedFrequency freq;

	public static final Color4d COL_OUTLINE = ColourInput.MED_GREY;

	{
		active.setHidden(false);
		stateGraphics.setHidden(false);
		immediateThresholdList.setHidden(true);
		immediateReleaseThresholdList.setHidden(true);
		immediateMaintenanceList.setHidden(true);
		immediateBreakdownList.setHidden(true);

		resourcePool = new EntityInput<>(ResourcePool.class, "ResourcePool", KEY_INPUTS, null);
		this.addInput(resourcePool);

		assignmentCondition = new ExpressionInput("AssignmentCondition", KEY_INPUTS, null);
		assignmentCondition.setUnitType(DimensionlessUnit.class);
		assignmentCondition.setResultType(ExpResType.NUMBER);
		this.addInput(assignmentCondition);

		assignmentPriority = new ExpressionInput("AssignmentPriority", KEY_INPUTS, null);
		assignmentPriority.setUnitType(DimensionlessUnit.class);
		assignmentPriority.setResultType(ExpResType.NUMBER);
		assignmentPriority.setDefaultText("1");
		this.addInput(assignmentPriority);
		this.addSynonym(assignmentPriority, "Priority");

		followAssignment = new BooleanProvInput("FollowAssignment", FORMAT, false);
		this.addInput(followAssignment);

		assignmentOffset = new Vec3dInput("AssignmentOffset", FORMAT, new Vec3d());
		assignmentOffset.setUnitType(DistanceUnit.class);
		this.addInput(assignmentOffset);
	}

	public ResourceUnit() {
		userList = new ArrayList<>();
		stats = new TimeBasedStatistics();
		freq = new TimeBasedFrequency(0, 10);
	}

	@Override
	public void earlyInit() {
		super.earlyInit();
		presentAssignment = null;
		assignmentTicks = -1L;
		lastReleaseTicks = 0L;
		userList = AbstractResourceProvider.getUserList(this);

		unitsSeized = 0;
		unitsReleased = 0;
		stats.clear();
		stats.addValue(0.0d, 0);
		freq.clear();
		freq.addValue(0.0d,  0);
	}

	@Override
	public ResourcePool getResourcePool() {
		return resourcePool.getValue();
	}

	/**
	 * Returns the ResourceProvider that manages this ResourceUnit.
	 * Normally, the ResourceProvider is the ResourceUnit's ResourcePool, however if no
	 * ResourcePool was assigned then the ResourceUnit as its own ResourceProv, essentially as a
	 * ResourcePool with one unit.
	 * @return ResourceProvider that manages this ResourceUnit
	 */
	public ResourceProvider getResourceProvider() {
		return resourcePool.isDefault() ? this : getResourcePool();
	}

	/**
	 * Tests whether the specified entity is eligible to seize this unit.
	 * @param ent - entity to be tested
	 * @return true if the entity is eligible
	 */
	public boolean isAllowed(DisplayEntity ent) {
		if (assignmentCondition.isDefault())
			return true;

		// Temporarily set the present user so that the expression can be evaluated
		DisplayEntity oldAssignment = presentAssignment;
		presentAssignment = ent;

		// Evaluate the condition for the proposed user
		boolean ret = assignmentCondition.getNextResult(this, getSimTime()).value != 0;

		// Reset the original user
		presentAssignment = oldAssignment;

		return ret;
	}

	@Override
	public boolean canSeize(DisplayEntity ent) {
		return (presentAssignment == null && isAllowed(ent) && isAbleToRestart());
	}

	@Override
	public void seize(DisplayEntity ent) {
		double simTime = this.getSimTime();
		if (!canSeize(ent)) {
			error("Unit is already in use: assignment=%s, entity=%s", presentAssignment, ent);
		}
		presentAssignment = ent;
		assignmentTicks = getSimTicks();
		unitsSeized++;
		setPresentState();
		collectStatistics(simTime, getUnitsInUse());
	}

	@Override
	public void release() {
		double simTime = this.getSimTime();
		presentAssignment = null;
		assignmentTicks = -1L;
		lastReleaseTicks = getSimTicks();
		unitsReleased++;
		setPresentState();
		collectStatistics(simTime, getUnitsInUse());
	}

	public void collectStatistics(double simTime, int unitsInUse) {
		stats.addValue(simTime, unitsInUse);
		freq.addValue(simTime, unitsInUse);
	}

	@Override
	public void clearStatistics() {
		super.clearStatistics();
		double simTime = this.getSimTime();
		unitsSeized = 0;
		unitsReleased = 0;
		stats.clear();
		stats.addValue(simTime, getUnitsInUse());
		freq.clear();
		freq.addValue(simTime, getUnitsInUse());
	}

	@Override
	public int getCapacity(double simTime) {
		return 1;
	}

	@Override
	public int getUnitsInUse() {
		return presentAssignment == null ? 0 : 1;
	}

	@Override
	public boolean isBusy() {
		return presentAssignment != null;
	}

	@Override
	public DisplayEntity getAssignment() {
		return presentAssignment;
	}

	@Override
	public int getPriority(DisplayEntity ent) {
		if (assignmentPriority.isDefault())
			return 1;

		// Temporarily set the present user so that the expression can be evaluated
		DisplayEntity oldAssignment = presentAssignment;
		presentAssignment = ent;

		// Evaluate the condition for the proposed user
		int ret = (int) assignmentPriority.getNextResult(this, getSimTime()).value;

		// Reset the original user
		presentAssignment = oldAssignment;

		return ret;
	}

	@Override
	public long getLastReleaseTicks() {
		return lastReleaseTicks;
	}

	// ResourcePool interface methods

	@Override
	public boolean canSeize(double simTime, int n, DisplayEntity ent) {
		return canSeize(ent) && n <= 1;
	}

	@Override
	public void seize(int n, DisplayEntity ent) {
		if (n > 1)
			error(AbstractResourceProvider.ERR_CAPACITY, 1, n);
		seize(ent);
	}

	@Override
	public void release(int n, DisplayEntity ent) {
		release();
	}

	@Override
	public ArrayList<ResourceUser> getUserList() {
		return userList;
	}

	@Override
	public boolean isStrictOrder() {
		return false;
	}

	@Override
	public void thresholdChanged() {
		if (isTraceFlag())
			trace(0, "thresholdChanged");
		setPresentState();

		// If the resource unit is available, try to put it to work
		if (isAvailable()) {
			AbstractResourceProvider.notifyResourceUsers(getResourceProvider());
			return;
		}
		// Do nothing if the threshold is closed. A threshold closure takes effect after the
		// ResourceUnit has been released
	}

	@Override
	public void endDowntime(DowntimeEntity down) {
		super.endDowntime(down);

		// If the resource unit is available, try to put it to work
		if (isAvailable()) {
			AbstractResourceProvider.notifyResourceUsers(getResourceProvider());
			return;
		}
	}

	@Override
	public void updateGraphics(double simTime) {
		super.updateGraphics(simTime);

		// Set the resource unit's position
		if (followAssignment.getNextBoolean(this, simTime)) {
			if (presentAssignment == null) {
				setPosition(positionInput.getValue());
			}
			else {
				Vec3d pos = presentAssignment.getGlobalPosition();
				pos.add3(assignmentOffset.getValue());
				setGlobalPosition(pos);
			}
		}

		// Set the resource unit's colour based on its state
		setTagVisibility(ShapeModel.TAG_CONTENTS, true);
		setTagVisibility(ShapeModel.TAG_OUTLINES, true);
		setTagColour(ShapeModel.TAG_CONTENTS, getColourForPresentState());
		setTagColour(ShapeModel.TAG_OUTLINES, COL_OUTLINE);
	}

	@Output(name = "UserList",
	 description = "The objects that can seize this resource unit.",
	    sequence = 1)
	public ArrayList<ResourceUser> getUserList(double simTime) {
		return getResourceProvider().getUserList();
	}

	@Output(name = "Capacity",
	 description = "The total number of resource units that can be used.",
	    unitType = DimensionlessUnit.class,
	    sequence = 2)
	public int getPresentCapacity(double simTime) {
		return getCapacity(simTime);
	}

	@Output(name = "UnitsInUse",
	 description = "The present number of resource units that are in use.",
	    unitType = DimensionlessUnit.class,
	    sequence = 3)
	public int getUnitsInUse(double simTime) {
		return getUnitsInUse();
	}

	@Output(name = "AvailableUnits",
	 description = "The number of resource units that are not in use.",
	    unitType = DimensionlessUnit.class,
	    sequence = 4)
	public int getAvailableUnits(double simTime) {
		return getCapacity(simTime) - getUnitsInUse();
	}

	@Output(name = "UnitsSeized",
	 description = "The total number of times that the unit has been seized.",
	    unitType = DimensionlessUnit.class,
	  reportable = true,
	    sequence = 5)
	public int getUnitsSeized(double simTime) {
		return unitsSeized;
	}

	@Output(name = "UnitsReleased",
	 description = "The total number of times that the unit has been released.",
	    unitType = DimensionlessUnit.class,
	  reportable = true,
	    sequence = 6)
	public int getUnitsReleased(double simTime) {
		return unitsReleased;
	}

	@Output(name = "UnitsInUseAverage",
	 description = "The average number of resource units that are in use.",
	    unitType = DimensionlessUnit.class,
	  reportable = true,
	    sequence = 7)
	public double getUnitsInUseAverage(double simTime) {
		return stats.getMean(simTime);
	}

	@Output(name = "UnitsInUseStandardDeviation",
	 description = "The standard deviation of the number of resource units that are in use.",
	    unitType = DimensionlessUnit.class,
	  reportable = true,
	    sequence = 8)
	public double getUnitsInUseStandardDeviation(double simTime) {
		return stats.getStandardDeviation(simTime);
	}

	@Output(name = "UnitsInUseMinimum",
	 description = "The minimum number of resource units that are in use.",
	    unitType = DimensionlessUnit.class,
	  reportable = true,
	    sequence = 9)
	public int getUnitsInUseMinimum(double simTime) {
		return (int) stats.getMin();
	}

	@Output(name = "UnitsInUseMaximum",
	 description = "The maximum number of resource units that are in use.",
	    unitType = DimensionlessUnit.class,
	  reportable = true,
	    sequence = 10)
	public int getUnitsInUseMaximum(double simTime) {
		int ret = (int) stats.getMax();
		// A unit that is seized and released immediately
		// does not count as a non-zero maximum in use
		if (ret == 1 && freq.getBinTime(simTime, 1) == 0.0d)
			return 0;
		return ret;
	}

	@Output(name = "UnitsInUseTimes",
	 description = "The total time that the number of resource units in use was 0, 1, 2, etc.",
	    unitType = TimeUnit.class,
	  reportable = true,
	    sequence = 11)
	public double[] getUnitsInUseDistribution(double simTime) {
		return freq.getBinTimes(simTime);
	}

	@Output(name = "Assignment",
	 description = "The entity to which this unit is assigned.",
	    sequence = 12)
	public DisplayEntity getAssignment(double simTime) {
		return presentAssignment;
	}

	@Output(name = "AssignmentTime",
	 description = "Time at which this unit became assigned to its present entity. "
	             + "NaN is returned if the unit is unassigned.",
	    unitType = TimeUnit.class,
	    sequence = 13)
	public double getAssignmentTime(double simTime) {
		if (presentAssignment == null)
			return Double.NaN;
		EventManager evt = getJaamSimModel().getEventManager();
		return evt.ticksToSeconds(assignmentTicks);
	}

	@Output(name = "ReleaseTime",
	 description = "Time at which this unit became unassigned. "
	             + "NaN is returned if the unit is assigned.",
	    unitType = TimeUnit.class,
	    sequence = 14)
	public double getReleaseTime(double simTime) {
		if (presentAssignment != null)
			return Double.NaN;
		EventManager evt = getJaamSimModel().getEventManager();
		return evt.ticksToSeconds(lastReleaseTicks);
	}

}
