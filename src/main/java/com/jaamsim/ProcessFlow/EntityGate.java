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

import com.jaamsim.BasicObjects.DowntimeEntity;
import com.jaamsim.Graphics.DisplayEntity;
import com.jaamsim.Samples.SampleInput;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.Output;
import com.jaamsim.units.DimensionlessUnit;
import com.jaamsim.units.TimeUnit;

public class EntityGate extends LinkedService {

	@Keyword(description = "The time delay before each queued entity is released.\n" +
			"Entities arriving at an open gate are not delayed.",
	         exampleList = {"3.0 h", "ExponentialDistribution1", "'1[s] + 0.5*[TimeSeries1].PresentValue'"})
	private final SampleInput releaseDelay;

	@Keyword(description = "Maximum number of entites to release each time the gate is opened.",
	         exampleList = {"3", "[InputValue1].Value"})
	private final SampleInput numberToRelease;

	private DisplayEntity servedEntity; // the entity about to be released from the queue
	private int num = 0;  // number released since the gate opened

	{
		releaseDelay = new SampleInput("ReleaseDelay", KEY_INPUTS, 0.0);
		releaseDelay.setUnitType(TimeUnit.class);
		releaseDelay.setValidRange(0.0, Double.POSITIVE_INFINITY);
		this.addInput(releaseDelay);

		numberToRelease = new SampleInput("NumberToRelease", KEY_INPUTS, Double.POSITIVE_INFINITY);
		numberToRelease.setUnitType(DimensionlessUnit.class);
		numberToRelease.setIntegerValue(true);
		numberToRelease.setValidRange(0.0, Double.POSITIVE_INFINITY);
		this.addInput(numberToRelease);
	}

	public EntityGate() {}

	@Override
	public void earlyInit() {
		super.earlyInit();
		servedEntity = null;
		num = 0;
	}

	@Override
	public void addEntity(DisplayEntity ent) {

		// If the gate is closed, in maintenance or breakdown, or other entities are already
		// queued, then add the entity to the queue
		double simTime = getSimTime();
		Queue queue = getQueue(simTime);
		if (!queue.isEmpty() || !this.isIdle() || num >= getNumberToRelease(getSimTime())) {
			queue.addEntity(ent);
			return;
		}

		// If the gate is open and there are no other entities still in the queue,
		// then send the entity to the next component
		num++;
		receiveEntity(ent);
		setEntityState(ent);
		this.sendToNextComponent(ent);
	}

	@Override
	protected boolean startProcessing(double simTime) {

		if (num >= getNumberToRelease(simTime))
			return false;

		// Determine the match value
		String m = this.getNextMatchValue(getSimTime());
		this.setMatchValue(m);

		// Select the next entity to release
		servedEntity = this.removeNextEntity(m);
		if (servedEntity == null)
			return false;

		receiveEntity(servedEntity);
		setEntityState(servedEntity);
		num++;

		// Assign attributes
		assignAttributesAtStart(simTime);

		return true;
	}

	/**
	 * Loop recursively through the queued entities, releasing them one by one.
	 */
	@Override
	protected void processStep(double simTime) {

		// Release the first element in the queue and send to the next component
		this.sendToNextComponent(servedEntity);
		servedEntity = null;
	}

	@Override
	protected double getStepDuration(double simTime) {
		return releaseDelay.getNextSample(simTime);
	}
	@Override
	public void thresholdChanged() {
		super.thresholdChanged();
		if (!isOpen()) {
			num = 0;
		}
	}

	@Override
	public void startDowntime(DowntimeEntity down) {
		super.startDowntime(down);
		num = 0;
	}

	public int getNumberToRelease(double simTime) {
		return (int) numberToRelease.getNextSample(simTime);
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

	@Output(name = "NumberReleased",
	 description = "Number of entities released during the present opening of the gate. "
	             + "Zero is returned if the gate is closed.",
	    unitType = DimensionlessUnit.class,
	    sequence = 0)
	public int getNumberReleased(double simTime) {
		return num;
	}

}
