/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2023-2024 JaamSim Software Inc.
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

import com.jaamsim.BooleanProviders.BooleanProvInput;
import com.jaamsim.Graphics.DisplayEntity;
import com.jaamsim.Samples.SampleInput;
import com.jaamsim.Statistics.TimeBasedFrequency;
import com.jaamsim.Statistics.TimeBasedStatistics;
import com.jaamsim.basicsim.Entity;
import com.jaamsim.basicsim.EntityTarget;
import com.jaamsim.basicsim.ObserverEntity;
import com.jaamsim.basicsim.SubjectEntity;
import com.jaamsim.events.Conditional;
import com.jaamsim.events.EventManager;
import com.jaamsim.input.Input;
import com.jaamsim.input.InputCallback;
import com.jaamsim.input.InterfaceEntityListInput;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.Output;
import com.jaamsim.input.UnitTypeInput;
import com.jaamsim.math.MathUtils;
import com.jaamsim.units.DimensionlessUnit;
import com.jaamsim.units.Unit;
import com.jaamsim.units.UserSpecifiedUnit;

public class ExpressionStatistics extends DisplayEntity implements ObserverEntity {

	@Keyword(description = "Unit type for the variable whose statistics will be collected.",
	         exampleList = {"DistanceUnit"})
	private final UnitTypeInput unitType;

	@Keyword(description = "Variable for which statistics will be collected.",
	         exampleList = {"'this.obj.attrib1'"})
	private final SampleInput dataSource;

	@Keyword(description = "Width of the histogram bins into which the recorded values are "
	                     + "placed. "
	                     + "Histogram data will not be generated if the input is left blank.",
	         exampleList = {"1 h"})
	private final SampleInput histogramBinWidth;

	@Keyword(description = "Optional list of objects to monitor.\n\n"
	                     + "If the 'WatchList' input is provided, then the 'DataSource' input is "
	                     + "evaluated ONLY when triggered by an object in its 'WatchList'. "
	                     + "This is much more efficient than the default behaviour which "
	                     + "evaluates the 'DataSource' input at every event time.\n\n"
	                     + "Care must be taken to ensure that the 'WatchList' input includes "
	                     + "every object that can trigger a change in the value of 'DataSource' "
	                     + "input. "
	                     + "Normally, the 'WatchList' should include every object that is "
	                     + "referenced directly or indirectly by the 'DataSource' input. "
	                     + "The 'VerfiyWatchList' input can be used to ensure that the "
	                     + "'WatchList' includes all the necessary objects.",
	         exampleList = {"Object1  Object2"})
	protected final InterfaceEntityListInput<SubjectEntity> watchList;

	@Keyword(description = "Allows the user to verify that the 'WatchList' input includes all the "
	                     + "objects that can trigger a change in the value of the 'DataSource' "
	                     + "input. "
	                     + "When set to TRUE, the both the normal logic and the 'WatchList' logic "
	                     + "is used to test the value of the 'DataSource' input. "
	                     + "An error message is generated if 'DataSource' input changes "
	                     + "its value without being triggered by a 'WatchList' object.")
	private final BooleanProvInput verifyWatchList;

	private double lastValue;
	private final TimeBasedStatistics timeStats = new TimeBasedStatistics();
	private final TimeBasedFrequency freq = new TimeBasedFrequency(0, 10);

	{
		unitType = new UnitTypeInput("UnitType", KEY_INPUTS, UserSpecifiedUnit.class);
		unitType.setRequired(true);
		unitType.setCallback(inputCallback);
		this.addInput(unitType);

		dataSource = new SampleInput("DataSource", KEY_INPUTS, Double.NaN);
		dataSource.setUnitType(UserSpecifiedUnit.class);
		dataSource.setRequired(true);
		dataSource.setOutput(true);
		this.addInput(dataSource);

		histogramBinWidth = new SampleInput("HistogramBinWidth", KEY_INPUTS, Double.NaN);
		histogramBinWidth.setUnitType(UserSpecifiedUnit.class);
		histogramBinWidth.setOutput(true);
		this.addInput(histogramBinWidth);

		watchList = new InterfaceEntityListInput<>(SubjectEntity.class, "WatchList", KEY_INPUTS, new ArrayList<>());
		watchList.setIncludeSelf(false);
		watchList.setUnique(true);
		this.addInput(watchList);

		verifyWatchList = new BooleanProvInput("VerifyWatchList", KEY_INPUTS, false);
		this.addInput(verifyWatchList);
	}

	static final InputCallback inputCallback = new InputCallback() {
		@Override
		public void callback(Entity ent, Input<?> inp) {
			((ExpressionStatistics)ent).updateUnitTypeInputValue();
		}
	};

	void updateUnitTypeInputValue() {
		Class<? extends Unit> ut = unitType.getUnitType();
		dataSource.setUnitType(ut);
		histogramBinWidth.setUnitType(ut);
	}

	@Override
	public void earlyInit() {
		super.earlyInit();
		timeStats.clear();
		freq.clear();
	}

	@Override
	public void startUp() {
		super.startUp();
		lastValue = Double.NaN;

		// If there is no WatchList, the open/close expressions are tested after every event
		if (!isWatchList() || isVerifyWatchList(0.0d)) {
			doValueTrace();
		}
	}

	@Override
	public void clearStatistics() {
		super.clearStatistics();
		timeStats.clear();
		freq.clear();
	}

	private double getBinWidth() {
		return histogramBinWidth.getNextSample(this, 0.0d);
	}

	public boolean isVerifyWatchList(double simTime) {
		return verifyWatchList.getNextBoolean(this, simTime);
	}

	public boolean isWatchList() {
		return !getWatchList().isEmpty();
	}

	@Override
	public ArrayList<SubjectEntity> getWatchList() {
		return watchList.getValue();
	}

	@Override
	public void observerUpdate(SubjectEntity subj) {
		if (isValueChanged()) {
			recordValue();
		}
	}

	public void recordValue() {
		double simTime = getSimTime();

		double val = getValue(simTime);
		timeStats.addValue(simTime, val);
		if (!histogramBinWidth.isDefault()) {
			freq.addValue(simTime, (int) Math.round(val/getBinWidth()));
		}
	}

	final boolean isValueChanged() {
		double simTime = getSimTime();
		double value = getValue(simTime);
		if (MathUtils.near(value, lastValue))
			return false;
		lastValue = value;
		return true;
	}

	void doValueTrace() {

		// Record the new value
		recordValue();

		// Wait for the next value change
		EventManager.scheduleUntil(doValueTraceTarget, valueChangedConditional, null);
	}

	static class ValueChangedConditional extends Conditional {
		private final ExpressionStatistics ent;

		ValueChangedConditional(ExpressionStatistics ent) {
			this.ent = ent;
		}
		@Override
		public boolean evaluate() {
			return ent.isValueChanged();
		}
	}
	private final Conditional valueChangedConditional = new ValueChangedConditional(this);

	private final EntityTarget<ExpressionStatistics> doValueTraceTarget =
			new EntityTarget<ExpressionStatistics>(this, "doValueTrace") {
		@Override
		public void process() {
			ent.doValueTrace();
		}
	};

	@Output(name = "Value",
	 description = "Present value for the 'DataSource' input.",
	    unitType = UserSpecifiedUnit.class,
	  reportable = true,
	    sequence = 1)
	public double getValue(double simTime) {
		return dataSource.getNextSample(this, simTime);
	}

	@Output(name = "Minimum",
	 description = "Smallest value that was recorded.",
	    unitType = UserSpecifiedUnit.class,
	  reportable = true,
	    sequence = 2)
	public double getMinimum(double simTime) {
		return timeStats.getMin();
	}

	@Output(name = "Maximum",
	 description = "Largest value that was recorded.",
	    unitType = UserSpecifiedUnit.class,
	  reportable = true,
	    sequence = 3)
	public double getMaximum(double simTime) {
		return timeStats.getMax();
	}

	@Output(name = "TimeAverage",
	 description = "Average of the values recorded, weighted by the duration of each value.",
	    unitType = UserSpecifiedUnit.class,
	  reportable = true,
	    sequence = 4)
	public double getTimeAverage(double simTime) {
		return timeStats.getMean(simTime);
	}

	@Output(name = "TimeStandardDeviation",
	 description = "Standard deviation of the values recorded, weighted by the duration of each value.",
	    unitType = UserSpecifiedUnit.class,
	  reportable = true,
	    sequence = 5)
	public double getTimeStandardDeviation(double simTime) {
		return timeStats.getStandardDeviation(simTime);
	}

	@Output(name = "HistogramBinCentres",
	 description = "Central value for each histogram bin.",
	    unitType = UserSpecifiedUnit.class,
	  reportable = true,
	    sequence = 6)
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

	@Output(name = "HistogramBinFractions",
	 description = "Fraction of total time that the recorded value was within each histogram bin.",
	    unitType = DimensionlessUnit.class,
	  reportable = true,
	    sequence = 7)
	public double[] getHistogramBinFractions(double simTime) {
		if (histogramBinWidth.isDefault()) {
			return new double[0];
		}
		return freq.getBinFractions(simTime);
	}

}
