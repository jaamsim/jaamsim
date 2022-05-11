/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2014 Ausenco Engineering Canada Inc.
 * Copyright (C) 2016-2022 JaamSim Software Inc.
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

import com.jaamsim.Commands.KeywordCommand;
import com.jaamsim.EntityProviders.EntityProvInput;
import com.jaamsim.Graphics.DisplayEntity;
import com.jaamsim.StringProviders.StringProvInput;
import com.jaamsim.SubModels.CompoundEntity;
import com.jaamsim.basicsim.ErrorException;
import com.jaamsim.basicsim.SubjectEntity;
import com.jaamsim.input.ExpError;
import com.jaamsim.input.ExpEvaluator;
import com.jaamsim.input.ExpResType;
import com.jaamsim.input.ExpressionInput;
import com.jaamsim.input.InputAgent;
import com.jaamsim.input.InterfaceEntityListInput;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.KeywordIndex;
import com.jaamsim.input.Output;
import com.jaamsim.input.Vec3dInput;
import com.jaamsim.input.ExpParser.Expression;
import com.jaamsim.math.Vec3d;
import com.jaamsim.units.DimensionlessUnit;
import com.jaamsim.units.DistanceUnit;

public abstract class LinkedService extends LinkedDevice implements QueueUser {

	@Keyword(description = "The position of the entity being processed relative to the processor.",
	         exampleList = {"1.0 0.0 0.01 m"})
	protected final Vec3dInput processPosition;

	@Keyword(description = "Queue from which the next entity for processing will be selected.\n\n"
	                     + "If an expression is entered that can return various Queues, it is "
	                     + "necessary for each Queue to be included in the entry to the "
	                     + "'WatchList' keyword. "
	                     + "If a Queue is not included, then the arrival of an entity to that "
	                     + "Queue will not wake up this processor.",
	         exampleList = {"Queue1", "'this.NumberProcessed % 2 == 0 ? [Queue1] : [Queue2]'"})
	protected final EntityProvInput<Queue> waitQueue;

	@Keyword(description = "An expression returning a string value that determines which of the "
	                     + "queued entities are eligible to be selected. "
	                     + "If used, the only entities eligible for selection are the ones whose "
	                     + "inputs for the Queue's Match keyword are equal to value returned by "
	                     + "the expression entered for this Match keyword. "
	                     + "Expressions that return a dimensionless integer or an object are also "
	                     + "valid. The returned number or object is converted to a string "
	                     + "automatically. A floating point number is truncated to an integer."
	                     + "\n\n"
	                     + "Note that a change in the Match value does not trigger the processor "
	                     + "automatically to re-check the Queue. "
	                     + "The processor can be triggered by adding one or more objects to the "
	                     + "'WatchList' input.",
	         exampleList = {"this.obj.Attrib1"})
	protected final StringProvInput match;

	@Keyword(description = "An optional expression that tests whether an entity in the queue is "
	                     + "eligible to be processed. "
	                     + "The expression should return 1 (true) if the entity is eligible. "
	                     + "The entity chosen for processing is the first one in the queue that "
	                     + "satisfies both the SelectionCondition expression and the Match value "
	                     + "(if specified)."
	                     + "\n\n"
	                     + "Unlike the Match value, which must be specified when an entity "
	                     + "first enters the queue, the SelectionCondition is evaluated when an "
	                     + "entity is to be removed from the queue. "
	                     + "Consequently, a SelectionCondition is more flexible than a Match "
	                     + "value, but is significantly less efficient.",
	         exampleList = {"'this.obj.attrib > 10'"})
	protected final ExpressionInput selectionCondition;

	@Keyword(description = "An optional expression that returns the next entity to be removed "
	                     + "from the queue. "
	                     + "No entity is removed if the expression returns null or the entity is "
	                     + "not present in the queue. "
	                     + "To be removed, the entity must also satisfy the Match and "
	                     + "SelectionCondition inputs if these are entered.",
	         exampleList = {"'this.nextEntity'"})
	protected final ExpressionInput nextEntity;

	@Keyword(description = "An optional list of objects to monitor.\n\n"
	                     + "The queue will be inspected for an entity to process whenever one of "
	                     + "the WatchList objects changes state.",
	         exampleList = {"Object1  Object2"})
	protected final InterfaceEntityListInput<SubjectEntity> watchList;

	private String matchValue;

	{
		stateGraphics.setHidden(false);
		workingStateListInput.setHidden(false);

		processPosition = new Vec3dInput("ProcessPosition", FORMAT, new Vec3d(0.0d, 0.0d, 0.01d));
		processPosition.setUnitType(DistanceUnit.class);
		this.addInput(processPosition);

		waitQueue = new EntityProvInput<>(Queue.class, "WaitQueue", KEY_INPUTS, null);
		waitQueue.setRequired(true);
		this.addInput(waitQueue);

		match = new StringProvInput("Match", KEY_INPUTS, null);
		match.setUnitType(DimensionlessUnit.class);
		this.addInput(match);

		selectionCondition = new ExpressionInput("SelectionCondition", KEY_INPUTS, null);
		selectionCondition.setUnitType(DimensionlessUnit.class);
		selectionCondition.setResultType(ExpResType.NUMBER);
		this.addInput(selectionCondition);

		nextEntity = new ExpressionInput("NextEntity", KEY_INPUTS, null);
		selectionCondition.setResultType(ExpResType.ENTITY);
		this.addInput(nextEntity);

		watchList = new InterfaceEntityListInput<>(SubjectEntity.class, "WatchList", KEY_INPUTS, new ArrayList<>());
		watchList.setIncludeSelf(false);
		watchList.setUnique(true);
		this.addInput(watchList);
	}

	public LinkedService() {}

	@Override
	public void validate() {
		super.validate();

		// Check the WaitQueue expression
		if (!waitQueue.isDefault()) {
			Queue q = getQueue(0.0d);  // Ensures that the expression can be evaluated
			if (q == null)
				throw new ErrorException(waitQueue.getValueString(), 0, getName(),
						"Input to 'WaitQueue' returns null");
		}
	}

	@Override
	public void earlyInit() {
		super.earlyInit();
		matchValue = null;
	}

	@Override
	public ArrayList<SubjectEntity> getWatchList() {
		return watchList.getValue();
	}

	@Override
	public void observerUpdate(SubjectEntity subj) {

		// Avoid unnecessary updates
		if (isBusy())
			return;

		this.performUnscheduledUpdate();
	}

	@Override
	public void addEntity(DisplayEntity ent) {
		if (isTraceFlag()) trace(0, "addEntity(%s)", ent);

		// If there is no queue, then process the entity immediately
		double simTime = getSimTime();
		Queue queue = getQueue(simTime);
		if (queue == null) {
			super.addEntity(ent);
			return;
		}

		// Add the entity to the queue
		queue.addEntity(ent);
	}

	// ********************************************************************************************
	// SELECTING AN ENTITY FROM THE WAIT QUEUE
	// ********************************************************************************************

	/**
	 * Removes the next entity to be processed from the queue.
	 * If the specified match value is not null, then only the queued entities
	 * with the same match value are eligible to be removed.
	 * @param m - match value.
	 * @return next entity for processing.
	 */
	protected DisplayEntity removeNextEntity(String m) {
		double simTime = getSimTime();
		Queue queue = getQueue(simTime);

		if (selectionCondition.isDefault() && nextEntity.isDefault())
			return queue.removeFirst(m);

		// Evaluate the NextEntity input
		DisplayEntity entity = null;
		if (!nextEntity.isDefault()) {
			entity = getNextEntityValue(simTime);
			if (entity == null)
				return null;
		}

		// Find the first entity that satisfies the SelectionCondition and NextEntity inputs
		return queue.removeFirst(m, this, simTime, entity);
	}

	protected DisplayEntity getNextEntity(String m) {
		double simTime = getSimTime();
		Queue queue = getQueue(simTime);

		if (selectionCondition.isDefault() && nextEntity.isDefault())
			return queue.getFirst(m);

		// Evaluate the NextEntity input
		DisplayEntity entity = null;
		if (!nextEntity.isDefault()) {
			entity = getNextEntityValue(simTime);
			if (entity == null)
				return null;
		}

		// Find the first entity that satisfies the SelectionCondition and NextEntity inputs
		return queue.getFirst(m, this, simTime, entity);
	}

	private DisplayEntity getNextEntityValue(double simTime) {
		DisplayEntity ret = null;
		Expression exp = nextEntity.getValue();
		try {
			ret = (DisplayEntity) ExpEvaluator.evaluateExpression(exp, this, simTime).entVal;
		}
		catch (ExpError e) {
			throw new ErrorException(this, e);
		}
		return ret;
	}

	/**
	 * Returns a value which determines which of the entities in the queue are
	 * eligible to be removed. Returns null if the Match keyword has not been set.
	 * @param simTime - present simulation time in seconds.
	 * @return match value.
	 */
	protected String getNextMatchValue(double simTime) {
		if (match.isDefault())
			return null;

		return match.getNextString(simTime, 1.0d, true);
	}

	protected void setMatchValue(String m) {
		matchValue = m;
	}

	protected String getMatchValue() {
		return matchValue;
	}

	/**
	 * Returns whether the specified entity satisfies the SelectionCondition input.
	 * @param ent - entity to be tested
	 * @param simTime - present simulation time
	 * @return true if the entity satisfies the SelectionCondition
	 */
	public boolean isAllowed(DisplayEntity ent, double simTime) {
		Expression exp = selectionCondition.getValue();
		if (exp == null)
			return true;

		// Temporarily set the output 'obj' so that the expression can be evaluated
		DisplayEntity oldEnt = getReceivedEntity(simTime);
		setReceivedEntity(ent);

		// Evaluate the condition for the proposed user
		boolean ret = false;
		try {
			ret = ExpEvaluator.evaluateExpression(exp, this, simTime).value != 0;
		}
		catch (ExpError e) {
			throw new ErrorException(this, e);
		}

		// Reset the output 'obj' to the original entity
		setReceivedEntity(oldEnt);

		return ret;
	}

	// ********************************************************************************************
	// WAIT QUEUE
	// ********************************************************************************************

	public void addQueue(Queue que) {
		if (waitQueue.getHidden()) {
			return;
		}

		ArrayList<String> toks = new ArrayList<>();
		toks.add(que.getName());
		KeywordIndex kw = new KeywordIndex(waitQueue.getKeyword(), toks, null);
		InputAgent.storeAndExecute(new KeywordCommand(this, kw));
	}

	public Queue getQueue(double simTime) {
		if (waitQueue.isDefault())
			return null;
		return waitQueue.getNextEntity(simTime);
	}

	@Override
	public ArrayList<Queue> getQueues() {
		ArrayList<Queue> ret = new ArrayList<>();

		// Do not register the WaitQueue when the 'WatchList' input is set
		if (!getWatchList().isEmpty())
			return ret;

		try {
			Queue queue = getQueue(0.0d);
			if (queue != null)
				ret.add(queue);
		}
		catch (Exception e) {}
		return ret;
	}

	@Override
	public void queueChanged() {
		if (isTraceFlag()) trace(0, "queueChanged");
		this.restart();
	}

	// ********************************************************************************************
	// DEVICE METHODS
	// ********************************************************************************************

	@Override
	protected void updateProgress(double dt) {}

	@Override
	protected void processChanged() {}

	@Override
	protected boolean isNewStepReqd(boolean completed) {
		return completed;
	}

	@Override
	protected void setProcessStopped() {}

	// ********************************************************************************************
	// GRAPHICS
	// ********************************************************************************************

	protected final void moveToProcessPosition(DisplayEntity ent) {
		if (!getShow() && getVisibleParent() instanceof CompoundEntity) {
			CompoundEntity ce = (CompoundEntity) getVisibleParent();
			ent.moveToProcessPosition(ce, ce.getProcessPosition());
			return;
		}
		ent.moveToProcessPosition(this, processPosition.getValue());
	}

	@Override
	public ArrayList<DisplayEntity> getSourceEntities() {
		ArrayList<DisplayEntity> ret = super.getSourceEntities();
		try {
			Queue queue = getQueue(0.0d);
			if (queue != null)
				ret.add(queue);
		}
		catch (Exception e) {}
		return ret;
	}

	// ********************************************************************************************
	// OUTPUTS
	// ********************************************************************************************

	@Output(name = "MatchValue",
	 description = "The present value to be matched in the queue.",
	    sequence = 0)
	public String getMatchValue(double simTime) {
		return matchValue;
	}

}
