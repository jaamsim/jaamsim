/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2014 Ausenco Engineering Canada Inc.
 * Copyright (C) 2016 JaamSim Software Inc.
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

import com.jaamsim.Samples.SampleConstant;
import com.jaamsim.Samples.SampleInput;
import com.jaamsim.input.Keyword;
import com.jaamsim.units.TimeUnit;

public class Unpack extends LinkedService {

	@Keyword(description = "The service time required to unpacking each entity.",
	         exampleList = { "3.0 h", "NormalDistribution1", "'1[s] + 0.5*[TimeSeries1].PresentValue'" })
	private final SampleInput serviceTime;

	private int numberToRemove;   // Number of entities to remove from the present EntityContainer

	{
		serviceTime = new SampleInput("ServiceTime", "Key Inputs", new SampleConstant(TimeUnit.class, 0.0));
		serviceTime.setUnitType(TimeUnit.class);
		serviceTime.setEntity(this);
		serviceTime.setValidRange(0, Double.POSITIVE_INFINITY);
		this.addInput(serviceTime);
	}

	private EntityContainer container;	// the received EntityContainer
	private int numberRemoved;   // Number of entities removed from the received EntityContainer

	public Unpack() {}

	@Override
	public void earlyInit() {
		super.earlyInit();
		container = null;
		numberRemoved = 0;
	}

	@Override
	protected boolean startProcessing(double simTime) {

		// Determine the match value
		Integer m = this.getNextMatchValue(getSimTime());

		// Is there a container waiting to be unpacked?
		if (container == null && waitQueue.getValue().getMatchCount(m) == 0) {
			return false;
		}

		if (container == null) {

			// Remove the container from the queue
			container = (EntityContainer)this.getNextEntityForMatch(m);
			numberToRemove = this.getNumberToRemove();
			numberRemoved = 0;

			// Position the container over the unpack object
			this.moveToProcessPosition(container);
		}

		return true;
	}

	protected void disposeContainer(EntityContainer c) {
		c.kill();
	}

	protected int getNumberToRemove() {
		return Integer.MAX_VALUE;
	}

	@Override
	protected boolean processStep(double simTime) {

		// Remove the next entity from the container
		if (numberRemoved < numberToRemove && container.getCount() > 0) {
			this.sendToNextComponent(container.removeEntity());
			numberRemoved++;
		}

		// Stop when the desired number of entities have been removed
		if (container.getCount() == 0 || numberRemoved == numberToRemove) {
			this.disposeContainer(container);
			container = null;
		}

		return true;
	}

	@Override
	protected double getStepDuration(double simTime) {
		double dur = 0.0;
		if (numberRemoved < numberToRemove && container.getCount() > 0)
			dur = serviceTime.getValue().getNextSample(simTime);
		return dur;
	}

}
