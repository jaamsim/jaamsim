/*
 * JaamSim Discrete Event Simulation
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
package com.jaamsim.resourceObjects;

import java.util.ArrayList;

import com.jaamsim.BasicObjects.DowntimeEntity;
import com.jaamsim.DisplayModels.ShapeModel;
import com.jaamsim.Graphics.DisplayEntity;
import com.jaamsim.ProcessFlow.StateUserEntity;
import com.jaamsim.basicsim.ErrorException;
import com.jaamsim.input.ColourInput;
import com.jaamsim.input.EntityInput;
import com.jaamsim.input.ExpError;
import com.jaamsim.input.ExpEvaluator;
import com.jaamsim.input.ExpParser.Expression;
import com.jaamsim.input.ExpressionInput;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.Output;
import com.jaamsim.math.Color4d;
import com.jaamsim.units.DimensionlessUnit;

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
	private final ExpressionInput priority;

	private DisplayEntity presentAssignment;  // entity to which this unit is assigned
	private long lastReleaseTicks;  // clock ticks at which the unit was unassigned
	private ArrayList<ResourceUser> userList;  // objects that can use this resource

	public static final Color4d COL_OUTLINE = ColourInput.MED_GREY;

	{
		active.setHidden(false);

		resourcePool = new EntityInput<>(ResourcePool.class, "ResourcePool", KEY_INPUTS, null);
		this.addInput(resourcePool);

		assignmentCondition = new ExpressionInput("AssignmentCondition", KEY_INPUTS, null);
		assignmentCondition.setEntity(this);
		assignmentCondition.setUnitType(DimensionlessUnit.class);
		this.addInput(assignmentCondition);

		priority = new ExpressionInput("Priority", KEY_INPUTS, null);
		priority.setEntity(this);
		priority.setUnitType(DimensionlessUnit.class);
		priority.setDefaultText("1");
		this.addInput(priority);
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

		// Set the new state
		setBusy(true);
		setPresentState();
	}

	@Override
	public void release() {
		presentAssignment = null;
		lastReleaseTicks = getSimTicks();

		// Set the new state
		setBusy(false);
		setPresentState();
	}

	@Override
	public DisplayEntity getAssignment() {
		return presentAssignment;
	}

	@Override
	public int getPriority(DisplayEntity ent) {
		Expression exp = priority.getValue();
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
		// TODO Auto-generated method stub
	}

	@Override
	public boolean canStartDowntime(DowntimeEntity down) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void prepareForDowntime(DowntimeEntity down) {
		// TODO Auto-generated method stub
	}

	@Override
	public void startDowntime(DowntimeEntity down) {
		// TODO Auto-generated method stub
	}

	@Override
	public void endDowntime(DowntimeEntity down) {
		// TODO Auto-generated method stub
	}

	@Override
	public void updateGraphics(double simTime) {
		super.updateGraphics(simTime);
		setTagVisibility(ShapeModel.TAG_CONTENTS, true);
		setTagVisibility(ShapeModel.TAG_OUTLINES, true);
		setTagColour(ShapeModel.TAG_CONTENTS, getColourForPresentState());
		setTagColour(ShapeModel.TAG_OUTLINES, COL_OUTLINE);
	}

	@Output(name = "Assignment",
	 description = "The entity to which this unit is assigned.",
	    sequence = 0)
	public DisplayEntity getAssignment(double simTime) {
		return presentAssignment;
	}

}
