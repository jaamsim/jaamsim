/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2013 Ausenco Engineering Canada Inc.
 * Copyright (C) 2017-2020 JaamSim Software Inc.
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

import com.jaamsim.Graphics.DisplayEntity;
import com.jaamsim.basicsim.EntityTarget;
import com.jaamsim.basicsim.SubjectEntity;
import com.jaamsim.events.EventHandle;
import com.jaamsim.events.EventManager;
import com.jaamsim.events.ProcessTarget;
import com.jaamsim.resourceObjects.AbstractResourceProvider;

public class Seize extends AbstractLinkedResourceUser {

	{
		processPosition.setHidden(true);
		workingStateListInput.setHidden(true);
		immediateMaintenanceList.setHidden(true);
		forcedMaintenanceList.setHidden(true);
		opportunisticMaintenanceList.setHidden(true);
		immediateBreakdownList.setHidden(true);
		forcedBreakdownList.setHidden(true);
		opportunisticBreakdownList.setHidden(true);

		immediateThresholdList.setHidden(true);
		immediateReleaseThresholdList.setHidden(true);

		resourceList.setRequired(true);
	}

	public Seize() {}

	public void stateChanged() {
		if (!isReadyToStart())
			return;
		AbstractResourceProvider.notifyResourceUsers(getResourceList());
	}

	@Override
	public void queueChanged() {
		stateChanged();
	}

	@Override
	public void thresholdChanged() {
		stateChanged();
		super.thresholdChanged();
	}

	@Override
	public void observerUpdate(SubjectEntity subj) {
		if (!stateChangedHandle.isScheduled()) {
			EventManager.scheduleTicks(0, PRI_LOW, EVT_FIFO, stateChangedTarget, stateChangedHandle);
		}
	}

	private final EventHandle stateChangedHandle = new EventHandle();
	private final ProcessTarget stateChangedTarget = new EntityTarget<Seize>(this, "stateChanged") {
		@Override
		public void process() {
			stateChanged();
		}
	};

	@Override
	protected boolean startProcessing(double simTime) {
		return false;
	}

	@Override
	protected double getStepDuration(double simTime) {
		return 0.0d;
	}

	@Override
	protected void processStep(double simTime) {}

	@Override
	public boolean isFinished() {
		return true;  // can always stop when isFinished is called in startStep
	}

	@Override
	public void startNextEntity() {
		super.startNextEntity();
		double simTime = getSimTime();
		DisplayEntity ent = getReceivedEntity(simTime);
		this.sendToNextComponent(ent);
	}

}
