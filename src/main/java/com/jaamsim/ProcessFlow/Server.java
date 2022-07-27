/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2013 Ausenco Engineering Canada Inc.
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

import com.jaamsim.Graphics.DisplayEntity;
import com.jaamsim.Samples.SampleInput;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.Output;
import com.jaamsim.units.DimensionlessUnit;
import com.jaamsim.units.TimeUnit;

/**
 * Server processes entities one by one from a queue.  When finished with an entity, it passes it to the next
 * LinkedComponent in the chain.
 */
public class Server extends LinkedService {

	@Keyword(description = "The service time required to process an entity.",
	         exampleList = { "3.0 h", "NormalDistribution1", "'1[s] + 0.5*[TimeSeries1].PresentValue'" })
	private final SampleInput serviceTime;

	private DisplayEntity servedEntity;	// the DisplayEntity being server
	private double serviceDuration;  // total duration for the present service

	{
		releaseThresholdList.setHidden(false);

		serviceTime = new SampleInput("ServiceTime", KEY_INPUTS, 0.0d);
		serviceTime.setUnitType(TimeUnit.class);
		serviceTime.setValidRange(0, Double.POSITIVE_INFINITY);
		this.addInput(serviceTime);
	}

	public Server() {}

	@Override
	public void earlyInit() {
		super.earlyInit();
		servedEntity = null;
		serviceDuration = 0.0d;
	}

	@Override
	protected boolean startProcessing(double simTime) {

		// Determine the match value
		String m = this.getNextMatchValue(getSimTime());
		this.setMatchValue(m);

		// Remove the first entity from the queue
		servedEntity = this.removeNextEntity(m);
		if (servedEntity == null)
			return false;

		receiveEntity(servedEntity);
		setEntityState(servedEntity);

		// Set the service duration
		serviceDuration = serviceTime.getNextSample(simTime);

		// Assign attributes
		assignAttributesAtStart(simTime);

		return true;
	}

	@Override
	protected void processStep(double simTime) {

		// Check for a release threshold closure
		if (isReleaseThresholdClosure()) {
			setReadyToRelease(true);
			return;
		}

		// Send the entity to the next component in the chain
		this.sendToNextComponent(servedEntity);
		servedEntity = null;
		serviceDuration = 0.0d;
	}

	@Override
	protected double getStepDuration(double simTime) {
		return serviceDuration;
	}

	@Override
	protected boolean isNewStepReqd(boolean completed) {
		return completed && servedEntity == null;
	}

	@Override
	public boolean isFinished() {
		return servedEntity == null;
	}

	@Override
	public void updateGraphics(double simTime) {
		if (servedEntity == null)
			return;
		moveToProcessPosition(servedEntity);
	}

	@Output(name = "ServiceDuration",
	 description = "The total working time required for the present service activity.",
	    unitType = TimeUnit.class,
	    sequence = 1)
	public double getServiceDuration(double simTime) {
		return serviceDuration;
	}

	@Output(name = "ServicePerformed",
	 description = "The working time that has been completed for the present service activity.",
	    unitType = TimeUnit.class,
	    sequence = 2)
	public double getServicePerformed(double simTime) {
		if (servedEntity == null) {
			return 0.0d;
		}
		double ret = serviceDuration - getRemainingDuration();
		if (isBusy()) {
			ret += simTime - getLastUpdateTime();
		}
		return ret;
	}

	@Output(name = "FractionCompleted",
	 description = "The portion of the total service time for the present service activity that "
	             + "has been completed.",
	    unitType = DimensionlessUnit.class,
	    sequence = 3)
	public double getFractionCompleted(double simTime) {
		if (servedEntity == null) {
			return 0.0d;
		}
		return getServicePerformed(simTime)/serviceDuration;
	}

}
