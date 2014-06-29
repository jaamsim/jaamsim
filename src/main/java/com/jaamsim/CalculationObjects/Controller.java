/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2013 Ausenco Engineering Canada Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 */
package com.jaamsim.CalculationObjects;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import com.jaamsim.events.ProcessTarget;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.ValueInput;
import com.jaamsim.units.TimeUnit;
import com.sandwell.JavaSimulation.Entity;
import com.sandwell.JavaSimulation.EntityTarget;
import com.sandwell.JavaSimulation3D.DisplayEntity;

/**
 * The Controller object simulates the operation of a Programmable Logic Controller
 * @author Harry King
 *
 */
public class Controller extends DisplayEntity {

	@Keyword(description = "The sampling time for the Controller.",
	         example = "Controller1 SamplingTime { 100 ms }")
	private final ValueInput samplingTime;

	private final ArrayList<CalculationEntity> calculationEntityList;  // List of the CalculationEntities controller by this Controller.
	private int count;  // Number of times that the controller has initiated its calculations.

	private final ProcessTarget doUpdate = new DoUpdateTarget(this);

	{
		samplingTime = new ValueInput("SamplingTime", "Key Inputs", 1.0d);
		samplingTime.setUnitType(TimeUnit.class);
		samplingTime.setValidRange(0.0, Double.POSITIVE_INFINITY);
		this.addInput(samplingTime);
	}

	public Controller() {
		calculationEntityList = new ArrayList<CalculationEntity>();
	}

	@Override
	public void earlyInit() {
		super.earlyInit();
		count = 0;

		// Prepare a list of the calculation entities managed by this controller
		calculationEntityList.clear();
		for (CalculationEntity ent : Entity.getClonesOfIterator(CalculationEntity.class)) {
			if (ent.getController() == this)
				calculationEntityList.add(ent);
		}

		// Sort the calculation entities into the correct sequence
		Collections.sort(calculationEntityList, new SequenceCompare());
	}

	// Sorts by increasing sequence number
	private static class SequenceCompare implements Comparator<CalculationEntity> {
		@Override
		public int compare(CalculationEntity c1, CalculationEntity c2) {
			return Double.compare(c1.getSequenceNumber(), c2.getSequenceNumber());
		}
	}

	@Override
	public void startUp() {
		super.startUp();

		// Schedule the first update
		this.scheduleProcess(samplingTime.getValue(), 5, doUpdate);
	}

	private static class DoUpdateTarget extends EntityTarget<Controller> {
		DoUpdateTarget(Controller ent) {
			super(ent, "doUpdate");
		}

		@Override
		public void process() {
			ent.doUpdate();
		}
	}

	public void doUpdate() {

		// Update the last value for each entity
		double simTime = this.getSimTime();
		for (CalculationEntity ent : calculationEntityList) {
			ent.update(simTime);
		}

		// Increment the number of cycles
		count++;

		// Schedule the next update
		this.scheduleProcess(samplingTime.getValue(), 5, doUpdate);
	}

	public int getCount() {
		return count;
	}
}
