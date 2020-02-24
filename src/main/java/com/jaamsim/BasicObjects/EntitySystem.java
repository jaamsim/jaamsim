/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2020 JaamSim Software Inc.
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
package com.jaamsim.BasicObjects;

import java.util.ArrayList;

import com.jaamsim.ProcessFlow.AbstractStateUserEntity;
import com.jaamsim.basicsim.ErrorException;
import com.jaamsim.events.EventHandle;
import com.jaamsim.events.EventManager;
import com.jaamsim.events.ProcessTarget;
import com.jaamsim.input.ExpError;
import com.jaamsim.input.ExpEvaluator;
import com.jaamsim.input.ExpResType;
import com.jaamsim.input.ExpResult;
import com.jaamsim.input.ExpressionInput;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.Output;

public class EntitySystem extends AbstractStateUserEntity {

	@Keyword(description = "An expression returning a string that sets this object's present "
	                     + "state. "
	                     + "If left blank, the state will be set to Working if any of the "
	                     + "entities in the system are working. "
	                     + "It will be set to Idle if all of the entities in the system are idle.",
	         exampleList = {"'[Server1].Working || [Server2].Working ? \"Working\" : \"Idle\"'"})
	protected final ExpressionInput stateExp;

	private final ArrayList<AbstractStateUserEntity> entityList = new ArrayList<>();

	{
		stateExp = new ExpressionInput("StateExpression", KEY_INPUTS, null);
		stateExp.setResultType(ExpResType.STRING);
		this.addInput(stateExp);
	}

	@Override
	public void earlyInit() {
		super.earlyInit();

		entityList.clear();
		for (AbstractStateUserEntity stateEnt : getJaamSimModel().getClonesOfIterator(AbstractStateUserEntity.class)) {
			if (stateEnt.getEntitySystem() == this)
				entityList.add(stateEnt);
		}
	}

	public void performUpdate() {
		if (updateHandle.isScheduled())
			return;
		EventManager.scheduleTicks(0L, 11, false, updateTarget, updateHandle);
	}

	private final EventHandle updateHandle = new EventHandle();
	private final ProcessTarget updateTarget = new ProcessTarget() {

		@Override
		public void process() {
			setPresentState();
		}

		@Override
		public String getDescription() {
			return "setPresentState";
		}

	};

	@Override
	public boolean isBusy() {
		for (AbstractStateUserEntity ent : entityList) {
			if (ent.isWorkingState())
				return true;
		}
		return false;
	}

	@Override
	public boolean isMaintenance() {
		for (AbstractStateUserEntity ent : entityList) {
			if (ent.isMaintenance())
				return true;
		}
		return false;
	}

	@Override
	public boolean isBreakdown() {
		for (AbstractStateUserEntity ent : entityList) {
			if (ent.isBreakdown())
				return true;
		}
		return false;
	}

	@Override
	public boolean isStopped() {
		for (AbstractStateUserEntity ent : entityList) {
			if (ent.isStopped())
				return true;
		}
		return false;
	}

	@Override
	public boolean isSetup() {
		for (AbstractStateUserEntity ent : entityList) {
			if (ent.isSetup())
				return true;
		}
		return false;
	}

	@Override
	public boolean isIdle() {
		for (AbstractStateUserEntity ent : entityList) {
			if (!ent.isIdle())
				return false;
		}
		return true;
	}

	@Override
	public void setPresentState() {

		// Calculate the default state if no StateExpression is provided
		if (stateExp.isDefault()) {
			super.setPresentState();
			return;
		}

		// Calculate the state from the StateExpression input
		double simTime = getSimTime();
		try {
			ExpResult res = ExpEvaluator.evaluateExpression(stateExp.getValue(), simTime);
			setPresentState(res.stringVal);
		}
		catch (ExpError e) {
			throw new ErrorException(this, e);
		}
	}

	@Output(name = "EntityList",
	 description = "Entities included in this system.",
	    sequence = 1)
	public ArrayList<AbstractStateUserEntity> getEntityList(double simTime) {
		return entityList;
	}

}
