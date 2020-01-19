/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2018-2020 JaamSim Software Inc.
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
import com.jaamsim.DisplayModels.ShapeModel;
import com.jaamsim.Graphics.DisplayEntity;
import com.jaamsim.ProcessFlow.StateUserEntity;
import com.jaamsim.basicsim.ErrorException;
import com.jaamsim.input.BooleanInput;
import com.jaamsim.input.ColourInput;
import com.jaamsim.input.EntityInput;
import com.jaamsim.input.ExpError;
import com.jaamsim.input.ExpEvaluator;
import com.jaamsim.input.ExpParser.Expression;
import com.jaamsim.input.ExpressionInput;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.Output;
import com.jaamsim.input.Vec3dInput;
import com.jaamsim.math.Color4d;
import com.jaamsim.math.Vec3d;
import com.jaamsim.units.DimensionlessUnit;
import com.jaamsim.units.DistanceUnit;

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
	                     + "seized it, and will follow that entity until it is released.",
	         exampleList = {"TRUE"})
	private final BooleanInput followAssignment;

	@Keyword(description = "The position of the ResourceUnit relative to the entity that has seized it.",
	         exampleList = {"0.0 1.0 0.01 m"})
	protected final Vec3dInput assignmentOffset;

	private DisplayEntity presentAssignment;  // entity to which this unit is assigned
	private long lastReleaseTicks;  // clock ticks at which the unit was unassigned
	private ArrayList<ResourceUser> userList;  // objects that can use this resource

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
		this.addInput(assignmentCondition);

		assignmentPriority = new ExpressionInput("AssignmentPriority", KEY_INPUTS, null);
		assignmentPriority.setUnitType(DimensionlessUnit.class);
		assignmentPriority.setDefaultText("1");
		this.addInput(assignmentPriority);
		this.addSynonym(assignmentPriority, "Priority");

		followAssignment = new BooleanInput("FollowAssignment", FORMAT, false);
		this.addInput(followAssignment);

		assignmentOffset = new Vec3dInput("AssignmentOffset", FORMAT, new Vec3d());
		assignmentOffset.setUnitType(DistanceUnit.class);
		this.addInput(assignmentOffset);
	}

	public ResourceUnit() {
		userList = new ArrayList<>();
	}

	@Override
	public void earlyInit() {
		super.earlyInit();
		presentAssignment = null;
		lastReleaseTicks = 0L;
		userList = AbstractResourceProvider.getUserList(this);
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
		Expression exp = assignmentCondition.getValue();
		if (exp == null)
			return true;

		// Temporarily set the present user so that the expression can be evaluated
		DisplayEntity oldAssignment = presentAssignment;
		presentAssignment = ent;

		// Evaluate the condition for the proposed user
		boolean ret = false;
		double simTime = getSimTime();
		try {
			ret = ExpEvaluator.evaluateExpression(exp, simTime).value != 0;
		}
		catch (ExpError e) {
			throw new ErrorException(this, e);
		}

		// Reset the original user
		presentAssignment = oldAssignment;

		return ret;
	}

	@Override
	public boolean canSeize(DisplayEntity ent) {
		return (presentAssignment == null && isAllowed(ent) && isAvailable());
	}

	@Override
	public void seize(DisplayEntity ent) {
		if (!canSeize(ent)) {
			error("Unit is already in use: assignment=%s, entity=%s", presentAssignment, ent);
		}
		presentAssignment = ent;
		setPresentState();
	}

	@Override
	public void release() {
		presentAssignment = null;
		lastReleaseTicks = getSimTicks();
		setPresentState();
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
		Expression exp = assignmentPriority.getValue();
		if (exp == null)
			return 1;

		// Temporarily set the present user so that the expression can be evaluated
		DisplayEntity oldAssignment = presentAssignment;
		presentAssignment = ent;

		// Evaluate the condition for the proposed user
		int ret = 0;
		double simTime = getSimTime();
		try {
			ret = (int) ExpEvaluator.evaluateExpression(exp, simTime).value;
		}
		catch (ExpError e) {
			throw new ErrorException(this, e);
		}

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
	public boolean canSeize(int n, DisplayEntity ent) {
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
		if (followAssignment.getValue()) {
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

	@Output(name = "Assignment",
	 description = "The entity to which this unit is assigned.",
	    sequence = 2)
	public DisplayEntity getAssignment(double simTime) {
		return presentAssignment;
	}

}
