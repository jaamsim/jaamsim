/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2014 Ausenco Engineering Canada Inc.
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

import com.jaamsim.Samples.SampleConstant;
import com.jaamsim.Samples.SampleExpInput;
import com.jaamsim.input.Keyword;
import com.jaamsim.units.TimeUnit;

public class Unpack extends LinkedService {

	@Keyword(description = "The service time required to unpacking each entity.",
	         exampleList = { "3.0 h", "NormalDistribution1", "'1[s] + 0.5*[TimeSeries1].PresentValue'" })
	private final SampleExpInput serviceTime;

	private int numberToRemove;   // Number of entities to remove from the present EntityContainer

	{
		serviceTime = new SampleExpInput("ServiceTime", "Key Inputs", new SampleConstant(TimeUnit.class, 0.0));
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
	public void startAction() {

		// Determine the match value
		Integer m = this.getNextMatchValue(getSimTime());

		// Is there a container waiting to be unpacked?
		if (container == null && waitQueue.getValue().getMatchCount(m) == 0) {
			this.setBusy(false);
			this.setPresentState();
			return;
		}

		// Do any of the thresholds stop the generator?
		if (!this.isOpen()) {
			this.setBusy(false);
			this.setPresentState();
			return;
		}

		if (container == null) {

			// Remove the container from the queue
			container = (EntityContainer)this.getNextEntityForMatch(m);
			numberToRemove = this.getNumberToRemove();
			numberRemoved = 0;

			// Position the container over the unpack object
			this.moveToProcessPosition(container);
		}

		// Schedule the removal of the next entity
		double dt = 0.0;
		if (numberRemoved < numberToRemove && container.getCount() > 0)
			dt = serviceTime.getValue().getNextSample(getSimTime());
		this.scheduleProcess(dt, 5, endActionTarget);
	}

	protected void disposeContainer(EntityContainer c) {
		c.kill();
	}

	protected int getNumberToRemove() {
		return Integer.MAX_VALUE;
	}

	@Override
	public void endAction() {

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

		// Continue the process
		this.startAction();
	}

}
