/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2023 JaamSim Software Inc.
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
import com.jaamsim.Samples.SampleInput;
import com.jaamsim.Statistics.TimeBasedFrequency;
import com.jaamsim.Statistics.TimeBasedStatistics;
import com.jaamsim.basicsim.Entity;
import com.jaamsim.basicsim.EntityTarget;
import com.jaamsim.events.Conditional;
import com.jaamsim.events.EventManager;
import com.jaamsim.input.Input;
import com.jaamsim.input.InputCallback;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.Output;
import com.jaamsim.input.UnitTypeInput;
import com.jaamsim.math.MathUtils;
import com.jaamsim.units.DimensionlessUnit;
import com.jaamsim.units.Unit;
import com.jaamsim.units.UserSpecifiedUnit;

public class ExpressionStatistics extends DisplayEntity {

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

	private double lastValue;
	private final TimeBasedStatistics timeStats = new TimeBasedStatistics();
	private final TimeBasedFrequency freq = new TimeBasedFrequency(0, 10);

	{
		unitType = new UnitTypeInput("UnitType", KEY_INPUTS, UserSpecifiedUnit.class);
		unitType.setRequired(true);
		unitType.setCallback(inputCallback);
		this.addInput(unitType);

		dataSource = new SampleInput("DataSource", KEY_INPUTS, null);
		dataSource.setUnitType(UserSpecifiedUnit.class);
		dataSource.setRequired(true);
		this.addInput(dataSource);

		histogramBinWidth = new SampleInput("HistogramBinWidth", KEY_INPUTS, null);
		histogramBinWidth.setUnitType(UserSpecifiedUnit.class);
		this.addInput(histogramBinWidth);
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
		doValueTrace();
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
		if (dataSource.isDefault())
			return Double.NaN;
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
