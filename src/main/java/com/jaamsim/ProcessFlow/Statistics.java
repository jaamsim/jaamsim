/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2014 Ausenco Engineering Canada Inc.
 * Copyright (C) 2018-2023 JaamSim Software Inc.
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

import java.util.LinkedHashMap;
import java.util.Map;

import com.jaamsim.BooleanProviders.BooleanProvInput;
import com.jaamsim.Graphics.DisplayEntity;
import com.jaamsim.Samples.SampleInput;
import com.jaamsim.Statistics.SampleFrequency;
import com.jaamsim.Statistics.SampleStatistics;
import com.jaamsim.Statistics.TimeBasedStatistics;
import com.jaamsim.basicsim.Entity;
import com.jaamsim.events.EventManager;
import com.jaamsim.input.Input;
import com.jaamsim.input.InputCallback;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.Output;
import com.jaamsim.input.UnitTypeInput;
import com.jaamsim.states.StateEntity;
import com.jaamsim.states.StateRecord;
import com.jaamsim.units.DimensionlessUnit;
import com.jaamsim.units.TimeUnit;
import com.jaamsim.units.Unit;
import com.jaamsim.units.UserSpecifiedUnit;

/**
 * Collects basic statistical information on the entities that are received.
 * @author Harry King
 *
 */
public class Statistics extends LinkedComponent {

	@Keyword(description = "The unit type for the variable whose statistics will be collected.",
	         exampleList = {"DistanceUnit"})
	private final UnitTypeInput unitType;

	@Keyword(description = "The variable for which statistics will be collected.",
	         exampleList = {"'this.obj.attrib1'"})
	private final SampleInput sampleValue;

	@Keyword(description = "Width of the histogram bins into which the recorded values are "
	                     + "placed. Histogram data will not be generated if the input is left "
	                     + "blank.",
	         exampleList = {"1 h"})
	private final SampleInput histogramBinWidth;

	@Keyword(description = "If TRUE, the state times for received entities are recorded for "
	                     + "statistics generation. "
	                     + "The statistics for each state are returned by the outputs: "
	                     + "'EntityTimeMinimum', 'EntityTimeMaximum', 'EntityTimeAverage', and "
	                     + "'EntityTimeStandardDeviation'.")
	private final BooleanProvInput recordEntityStateTimes;

	@Keyword(description = "If TRUE, the state times for received entities are set to zero on "
	                     + "departure.")
	private final BooleanProvInput resetEntityStateTimes;

	private final SampleStatistics sampStats = new SampleStatistics();
	private final TimeBasedStatistics timeStats = new TimeBasedStatistics();
	private final SampleFrequency freq = new SampleFrequency(0, 10);
	private final LinkedHashMap<String, SampleStatistics> stateStats = new LinkedHashMap<>();

	{
		stateAssignment.setHidden(true);

		unitType = new UnitTypeInput("UnitType", KEY_INPUTS, UserSpecifiedUnit.class);
		unitType.setRequired(true);
		unitType.setCallback(inputCallback);
		this.addInput(unitType);

		sampleValue = new SampleInput("SampleValue", KEY_INPUTS, null);
		sampleValue.setUnitType(UserSpecifiedUnit.class);
		this.addInput(sampleValue);

		histogramBinWidth = new SampleInput("HistogramBinWidth", KEY_INPUTS, null);
		histogramBinWidth.setUnitType(UserSpecifiedUnit.class);
		this.addInput(histogramBinWidth);

		recordEntityStateTimes = new BooleanProvInput("RecordEntityStateTimes", KEY_INPUTS, false);
		this.addInput(recordEntityStateTimes);

		resetEntityStateTimes = new BooleanProvInput("ResetEntityStateTimes", KEY_INPUTS, false);
		this.addInput(resetEntityStateTimes);
	}

	public Statistics() {}

	static final InputCallback inputCallback = new InputCallback() {
		@Override
		public void callback(Entity ent, Input<?> inp) {
			((Statistics)ent).updateUnitTypeInputValue();
		}
	};

	void updateUnitTypeInputValue() {
		Class<? extends Unit> ut = unitType.getUnitType();
		sampleValue.setUnitType(ut);
		histogramBinWidth.setUnitType(ut);
	}

	@Override
	public void earlyInit() {
		super.earlyInit();
		sampStats.clear();
		timeStats.clear();
		freq.clear();
		stateStats.clear();
	}

	private double getBinWidth() {
		return histogramBinWidth.getNextSample(this, 0.0d);
	}

	public boolean isRecordEntityStateTimes(double simTime) {
		return recordEntityStateTimes.getNextBoolean(this, simTime);
	}

	public boolean isResetEntityStateTimes(double simTime) {
		return resetEntityStateTimes.getNextBoolean(this, simTime);
	}

	@Override
	public void addEntity(DisplayEntity ent) {
		super.addEntity(ent);
		double simTime = this.getSimTime();

		// Update the statistics
		if (!sampleValue.isDefault()) {
			double val = sampleValue.getNextSample(this, simTime);
			sampStats.addValue(val);
			timeStats.addValue(simTime, val);
			if (!histogramBinWidth.isDefault()) {
				freq.addValue((int) Math.round(val/getBinWidth()));
			}
		}

		// Update the statistics for each of the entity's states
		EventManager evt = EventManager.current();
		if (ent instanceof StateEntity) {
			StateEntity se = (StateEntity) ent;
			if (isRecordEntityStateTimes(simTime)) {
				for (StateRecord rec : se.getStateRecs()) {
					SampleStatistics durStats = stateStats.get(rec.getName());
					if (durStats == null) {
						durStats = new SampleStatistics();
						stateStats.put(rec.getName(), durStats);
					}
					long ticks = se.getTicksInState(rec);
					if (isResetEntityStateTimes(simTime)) {
						ticks = se.getCurrentCycleTicks(rec);
					}
					double dur = evt.ticksToSeconds(ticks);
					durStats.addValue(dur);
				}
			}

			// Reset the entity's statistics
			if (isResetEntityStateTimes(simTime)) {
				se.collectCycleStats();
			}
		}

		// Pass the entity to the next component
		this.sendToNextComponent(ent);
	}

	@Override
	public void clearStatistics() {
		super.clearStatistics();
		sampStats.clear();
		timeStats.clear();
		freq.clear();
		stateStats.clear();
	}

	@Override
	public Class<? extends Unit> getUserUnitType() {
		return unitType.getUnitType();
	}

	// ******************************************************************************************************
	// OUTPUT METHODS
	// ******************************************************************************************************

	@Output(name = "SampleMinimum",
	 description = "The smallest value that was recorded.",
	    unitType = UserSpecifiedUnit.class,
	  reportable = true,
	    sequence = 0)
	public double getSampleMinimum(double simTime) {
		return sampStats.getMin();
	}

	@Output(name = "SampleMaximum",
	 description = "The largest value that was recorded.",
	    unitType = UserSpecifiedUnit.class,
	  reportable = true,
	    sequence = 1)
	public double getSampleMaximum(double simTime) {
		return sampStats.getMax();
	}

	@Output(name = "SampleAverage",
	 description = "The average of the values that were recorded.",
	    unitType = UserSpecifiedUnit.class,
	  reportable = true,
	    sequence = 2)
	public double getSampleAverage(double simTime) {
		return sampStats.getMean();
	}

	@Output(name = "SampleStandardDeviation",
	 description = "The standard deviation of the values that were recorded.",
	    unitType = UserSpecifiedUnit.class,
	  reportable = true,
	    sequence = 3)
	public double getSampleStandardDeviation(double simTime) {
		return sampStats.getStandardDeviation();
	}

	@Output(name = "StandardDeviationOfTheMean",
	 description = "The estimated standard deviation of the sample mean.",
	    unitType = UserSpecifiedUnit.class,
	  reportable = true,
	    sequence = 4)
	public double getStandardDeviationOfTheMean(double simTime) {
		return sampStats.getStandardDeviation()/Math.sqrt(sampStats.getCount() - 1L);
	}

	@Output(name = "TimeAverage",
	 description = "The average of the values recorded, weighted by the duration of each value.",
	    unitType = UserSpecifiedUnit.class,
	  reportable = true,
	    sequence = 5)
	public double getTimeAverage(double simTime) {
		return timeStats.getMean(simTime);
	}

	@Output(name = "TimeStandardDeviation",
	 description = "The standard deviation of the values recorded, weighted by the duration of each value.",
	    unitType = UserSpecifiedUnit.class,
	  reportable = true,
	    sequence = 6)
	public double getTimeStandardDeviation(double simTime) {
		return timeStats.getStandardDeviation(simTime);
	}

	@Output(name = "HistogramBinCentres",
	 description = "The central value for each histogram bin.",
	    unitType = UserSpecifiedUnit.class,
	  reportable = true,
	    sequence = 7)
	public double[] getHistogramBinCentres(double simTime) {
		if (histogramBinWidth.isDefault()) {
			return new double[0];
		}
		int[] binVals = freq.getBinValues();
		double[] ret = new double[binVals.length];
		for (int i = 0; i < binVals.length; i++) {
			ret[i] = getBinWidth() * binVals[i];
		}
		return ret;
	}

	@Output(name = "HistogramBinUpperLimits",
	 description = "The largest value that can be assigned to each histogram bin.",
	    unitType = UserSpecifiedUnit.class,
	  reportable = true,
	    sequence = 8)
	public double[] getHistogramBinUpperLimits(double simTime) {
		if (histogramBinWidth.isDefault()) {
			return new double[0];
		}
		int[] binVals = freq.getBinValues();
		double[] ret = new double[binVals.length];
		for (int i = 0; i < binVals.length; i++) {
			ret[i] = getBinWidth() * (binVals[i] + 0.5d);
		}
		return ret;
	}

	@Output(name = "HistogramBinFractions",
	 description = "The fractional number of values within each histogram bin.",
	    unitType = DimensionlessUnit.class,
	  reportable = true,
	    sequence = 9)
	public double[] getHistogramBinFractions(double simTime) {
		if (histogramBinWidth.isDefault()) {
			return new double[0];
		}
		return freq.getBinFractions();
	}

	@Output(name = "HistogramBinCumulativeFractions",
	 description = "The fractional number of values within each histogram bin or smaller.",
	    unitType = DimensionlessUnit.class,
	  reportable = true,
	    sequence = 10)
	public double[] getHistogramCumulativeBinFractions(double simTime) {
		if (histogramBinWidth.isDefault()) {
			return new double[0];
		}
		return freq.getBinCumulativeFractions();
	}

	@Output(name = "EntityTimeMinimum",
	 description = "The minimum time the received entities have spent in each state.",
	    unitType = TimeUnit.class,
	  reportable = true,
	    sequence = 11)
	public LinkedHashMap<String, Double> getEntityTimeMinimum(double simTime) {
		long num = getNumberProcessed(simTime);
		LinkedHashMap<String, Double> ret = new LinkedHashMap<>(stateStats.size());
		for (Map.Entry<String, SampleStatistics> entry : stateStats.entrySet()) {
			double min = entry.getValue().getMin();
			if (entry.getValue().getCount() < num) {
				min = 0.0d;
			}
			ret.put(entry.getKey(), min);
		}
		return ret;
	}

	@Output(name = "EntityTimeMaximum",
	 description = "The maximum time the received entities have spent in each state.",
	    unitType = TimeUnit.class,
	  reportable = true,
	    sequence = 12)
	public LinkedHashMap<String, Double> getEntityTimeMaximum(double simTime) {
		LinkedHashMap<String, Double> ret = new LinkedHashMap<>(stateStats.size());
		for (Map.Entry<String, SampleStatistics> entry : stateStats.entrySet()) {
			double max = entry.getValue().getMax();
			ret.put(entry.getKey(), max);
		}
		return ret;
	}

	@Output(name = "EntityTimeAverage",
	 description = "The average time the received entities have spent in each state.",
	    unitType = TimeUnit.class,
	  reportable = true,
	    sequence = 13)
	public LinkedHashMap<String, Double> getEntityTimeAverage(double simTime) {
		long num = getNumberProcessed(simTime);
		LinkedHashMap<String, Double> ret = new LinkedHashMap<>(stateStats.size());
		for (Map.Entry<String, SampleStatistics> entry : stateStats.entrySet()) {
			double mean = entry.getValue().getSum() / num;
			ret.put(entry.getKey(), mean);
		}
		return ret;
	}

	@Output(name = "EntityTimeStandardDeviation",
	 description = "The standard deviation of the time the received entities have spent in "
	             + "each state.",
	    unitType = TimeUnit.class,
	  reportable = true,
	    sequence = 14)
	public LinkedHashMap<String, Double> getEntityTimeStandardDeviation(double simTime) {
		long num = getNumberProcessed(simTime);
		LinkedHashMap<String, Double> ret = new LinkedHashMap<>(stateStats.size());
		for (Map.Entry<String, SampleStatistics> entry : stateStats.entrySet()) {
			double mean = entry.getValue().getSum() / num;
			double meanSquared = entry.getValue().getSumSquared() / num;
			double sd = Math.sqrt(meanSquared - mean*mean);
			ret.put(entry.getKey(), sd);
		}
		return ret;
	}

}
