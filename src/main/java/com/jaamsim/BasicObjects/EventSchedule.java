/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2014 Ausenco Engineering Canada Inc.
 * Copyright (C) 2021 JaamSim Software Inc.
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

import com.jaamsim.Graphics.DisplayEntity;
import com.jaamsim.Samples.SampleProvider;
import com.jaamsim.datatypes.DoubleVector;
import com.jaamsim.events.EventManager;
import com.jaamsim.input.InputErrorException;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.Output;
import com.jaamsim.input.ValueInput;
import com.jaamsim.input.ValueListInput;
import com.jaamsim.units.DimensionlessUnit;
import com.jaamsim.units.TimeUnit;
import com.jaamsim.units.Unit;

public class EventSchedule extends DisplayEntity implements SampleProvider{

	@Keyword(description = "A sequence of monotonically-increasing simulation times at which to "
	                     + "generate events. "
	                     + "Times entered in date format are converted to simulation time using "
	                     + "the StartDate input for the Simulation object.",
	         exampleList = {"2  10  18  h",
	                        "'1970-01-15 12:30:00'  '1970-02-07 8:00:00'  '1970-03-30 18:00:00'"})
	private final ValueListInput timeList;

	@Keyword(description = "Defines when the event times will repeat from the start.",
	         exampleList = {"8760.0 h"})
	private final ValueInput cycleTime;

	private int index = -1;
	private boolean firstSample = true;

	{
		timeList = new ValueListInput("TimeList", KEY_INPUTS, null);
		timeList.setUnitType(TimeUnit.class);
		timeList.setValidRange(0.0, Double.POSITIVE_INFINITY);
		timeList.setMonotonic(1);
		timeList.setRequired(true);
		this.addInput(timeList);

		cycleTime = new ValueInput("CycleTime", KEY_INPUTS, null);
		cycleTime.setUnitType(TimeUnit.class);
		cycleTime.setValidRange(0.0, Double.POSITIVE_INFINITY);
		cycleTime.setRequired(true);
		this.addInput(cycleTime);
	}


	public EventSchedule() {}

	@Override
	public void validate() {
		super.validate();
		DoubleVector list = timeList.getValue();
		if (list.get(list.size()-1) > cycleTime.getValue())
			throw new InputErrorException("The input for CycleTime must be greater than or equal "
					+ "to the last entry for TimeList.");
	}

	@Override
	public void earlyInit() {
		super.earlyInit();
		index = -1;
		firstSample = true;
	}

	@Override
	public Class<? extends Unit> getUnitType() {
		return TimeUnit.class;
	}

	@Override
	public double getMeanValue(double simTime) {
		return 0;
	}

	@Override
	public double getMinValue() {
		return 0;
	}

	@Override
	public double getMaxValue() {
		return 0;
	}

	@Output(name = "Index",
	 description = "The position of the event time in the list for the last inter-arrival time "
	             + "that was returned.",
	    unitType = DimensionlessUnit.class,
	    sequence = 0)
	public int getIndexOfSample(double simTime) {
		return index+1;
	}

	@Output(name = "Value",
	 description = "The last inter-arrival time returned from the sequence. When used in an "
	             + "expression, this output returns a new value every time the expression "
	             + "is evaluated.",
	    unitType = TimeUnit.class,
	    sequence = 1)
	@Override
	public double getNextSample(double simTime) {
		DoubleVector list = timeList.getValue();

		if (list == null)
			return Double.NaN;

		// If called from a model thread, increment the index to be selected
		if (EventManager.hasCurrent()) {
			index = (index + 1) % list.size();
			if (firstSample && index > 0)
				firstSample = false;
		}

		// Trap an index that is out of range. Note that index can exceed the size of the list
		// if the TimeList keyword is edited in the middle of a run
		if (index < 0 || index >= list.size())
			return Double.NaN;


		if (index == 0) {
			// The first IAT calculated from the list is referenced to zero simulation time
			if (firstSample)
				return list.get(0);

			// All but the first IATs are referenced to the last time in the list
			return list.get(0) + cycleTime.getValue() - list.get(list.size()-1);
		}

		return list.get(index) - list.get(index-1);
	}

}
