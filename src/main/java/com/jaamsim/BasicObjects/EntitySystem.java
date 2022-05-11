/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2020-2022 JaamSim Software Inc.
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
import com.jaamsim.basicsim.ObserverEntity;
import com.jaamsim.basicsim.SubjectEntity;
import com.jaamsim.basicsim.SubjectEntityDelegate;
import com.jaamsim.events.EventHandle;
import com.jaamsim.events.EventManager;
import com.jaamsim.events.ProcessTarget;
import com.jaamsim.input.ExpError;
import com.jaamsim.input.ExpEvaluator;
import com.jaamsim.input.ExpResType;
import com.jaamsim.input.ExpResult;
import com.jaamsim.input.ExpressionInput;
import com.jaamsim.input.InterfaceEntityListInput;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.Output;
import com.jaamsim.states.StateRecord;

public class EntitySystem extends AbstractStateUserEntity implements ObserverEntity, SubjectEntity {

	@Keyword(description = "An expression returning a string that sets this object's present "
	                     + "state. "
	                     + "If left blank, the state will be set to Working if any of the "
	                     + "entities specified by the WatchList input are working. "
	                     + "It will be set to Idle if all of the entities in the WatchList are "
	                     + "idle.",
	         exampleList = {"'[Server1].Working || [Server2].Working ? \"Working\" : \"Idle\"'"})
	protected final ExpressionInput stateExp;

	@Keyword(description = "A list of objects to monitor.\n\n"
	                     + "The system's state will be re-calculated whenever one of "
	                     + "the WatchList objects changes state.",
	         exampleList = {"Object1  Object2"})
	protected final InterfaceEntityListInput<SubjectEntity> watchList;

	private final SubjectEntityDelegate subject = new SubjectEntityDelegate(this);

	{
		stateExp = new ExpressionInput("StateExpression", KEY_INPUTS, null);
		stateExp.setResultType(ExpResType.STRING);
		this.addInput(stateExp);

		watchList = new InterfaceEntityListInput<>(SubjectEntity.class, "WatchList", KEY_INPUTS, new ArrayList<>());
		watchList.setIncludeSelf(false);
		watchList.setUnique(true);
		watchList.setRequired(true);
		this.addInput(watchList);
	}

	@Override
	public void validate() {
		super.validate();
		ObserverEntity.validate(this);
	}


	@Override
	public void lateInit() {
		super.lateInit();
		ObserverEntity.registerWithSubjects(this, getWatchList());
	}

	@Override
	public void registerObserver(ObserverEntity obs) {
		subject.registerObserver(obs);
	}

	@Override
	public void notifyObservers() {
		subject.notifyObservers();
	}

	@Override
	public ArrayList<ObserverEntity> getObserverList() {
		return subject.getObserverList();
	}

	@Override
	public ArrayList<SubjectEntity> getWatchList() {
		return watchList.getValue();
	}

	@Override
	public void observerUpdate(SubjectEntity subj) {
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
		for (SubjectEntity subj : getWatchList()) {
			if (!(subj instanceof AbstractStateUserEntity))
				continue;
			if (((AbstractStateUserEntity) subj).isBusy())
				return true;
		}
		return false;
	}

	@Override
	public boolean isMaintenance() {
		for (SubjectEntity subj : getWatchList()) {
			if (!(subj instanceof AbstractStateUserEntity))
				continue;
			if (((AbstractStateUserEntity) subj).isMaintenance())
				return true;
		}
		return false;
	}

	@Override
	public boolean isBreakdown() {
		for (SubjectEntity subj : getWatchList()) {
			if (!(subj instanceof AbstractStateUserEntity))
				continue;
			if (((AbstractStateUserEntity) subj).isBreakdown())
				return true;
		}
		return false;
	}

	@Override
	public boolean isStopped() {
		for (SubjectEntity subj : getWatchList()) {
			if (!(subj instanceof AbstractStateUserEntity))
				continue;
			if (((AbstractStateUserEntity) subj).isStopped())
				return true;
		}
		return false;
	}

	@Override
	public boolean isSetup() {
		for (SubjectEntity subj : getWatchList()) {
			if (!(subj instanceof AbstractStateUserEntity))
				continue;
			if (((AbstractStateUserEntity) subj).isSetup())
				return true;
		}
		return false;
	}

	@Override
	public boolean isSetdown() {
		for (SubjectEntity subj : getWatchList()) {
			if (!(subj instanceof AbstractStateUserEntity))
				continue;
			if (((AbstractStateUserEntity) subj).isSetdown())
				return true;
		}
		return false;
	}

	@Override
	public boolean isIdle() {
		for (SubjectEntity subj : getWatchList()) {
			if (!(subj instanceof AbstractStateUserEntity))
				continue;
			if (!((AbstractStateUserEntity) subj).isIdle())
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
			ExpResult res = ExpEvaluator.evaluateExpression(stateExp.getValue(), this, simTime);
			setPresentState(res.stringVal);
		}
		catch (ExpError e) {
			throw new ErrorException(this, e);
		}
	}

	@Override
	public void stateChanged(StateRecord prev, StateRecord next) {
		super.stateChanged(prev, next);
		notifyObservers();
	}

	@Output(name = "EntityList",
	 description = "Entities included in this system. "
	             + "Consists of the entities in the WatchList for which a state can be obtained.",
	    sequence = 1)
	public ArrayList<AbstractStateUserEntity> getEntityList(double simTime) {
		ArrayList<AbstractStateUserEntity> ret = new ArrayList<>();
		for (SubjectEntity subj : getWatchList()) {
			if (!(subj instanceof AbstractStateUserEntity))
				continue;
			ret.add((AbstractStateUserEntity) subj);
		}
		return ret;
	}

}
