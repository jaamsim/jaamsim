/*
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

package com.jaamsim.RunControlObjects;

import java.util.ArrayList;

import com.jaamsim.Graphics.DisplayEntity;
import com.jaamsim.ProcessFlow.Statistics;
import com.jaamsim.basicsim.EntityTarget;
import com.jaamsim.basicsim.Simulation;
import com.jaamsim.datatypes.DoubleVector;
import com.jaamsim.events.ProcessTarget;
import com.jaamsim.input.Input;
import com.jaamsim.input.IntegerInput;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.Output;
import com.jaamsim.input.StringChoiceInput;
import com.jaamsim.input.ValueInput;
//import com.jaamsim.ui.GUIFrame;
import com.jaamsim.units.DimensionlessUnit;
import com.jaamsim.units.TimeUnit;
import com.jaamsim.units.UserSpecifiedUnit;

/**
 * Extension of the base statistics entity wherein there are additional
 * variables that store the values for the time intervals.
 *
 * Just to simplify the batching of received values for entity statistics
 * rather than having an additional batch recorder.
 *
 * @author Michael Bergman
 *
 */
public class BatchStatistics extends Statistics {


	@Keyword(description="The smallest time width of an interval",
			 exampleList={"0.25 h"})
	private final ValueInput minIntervalTimeInput;


	@Keyword(description="The minimum number of samples to constitute a tally interval",
			 exampleList={"16"})
	private final IntegerInput minNumSamples;


	@Keyword(description="The number of intervals for which to store values",
			 exampleList={"20"})
	private final IntegerInput numIntervalsInput;


	@Keyword(description="Percent confidence iterval input",
			 exampleList={"95.0"})
	private final ValueInput confIntInput;


	@Keyword(description="The variable being watched for the batches",
	         exampleList={"TALLY_MEAN" })
	private final StringChoiceInput varInput;


	// stores the choices for the StringChoiceInput
	public final static ArrayList<String> outputTypes;

	private int numIntervals = 0;
	private double intervalTimeWidth = 0d;
	private long numSamplesWidth = 0;

	private double lastTotalTimeValue = 0d;
	private double lastIntervalTime = 0d;

	private long currAdded = 0;

	private double currentIntervalValue = 0d;
	private double confPercent = 0d;

	private boolean correlated = true;

	//private double sumIntervals = 0d;
	private double halfWidth = Double.POSITIVE_INFINITY;

	// enum value for the types of batch means
	private InputType inType;

	private final DoubleVector recordsList = new DoubleVector();

	private CurrentIntervalMean currMeanFunction;

	private final ProcessTarget scheduleTarget = new SchedulingTarget();

	static {

		// Initialisation of selection list options
		outputTypes = new ArrayList<>();
		outputTypes.add("TALLY_MEAN");
		outputTypes.add("TIME_MEAN");

	}


	{

		varInput = new StringChoiceInput("InputType", "Key Inputs", 0);
		varInput.setChoices(outputTypes);
		varInput.setRequired(true);
		this.addInput(varInput);


		numIntervalsInput = new IntegerInput("NumberIntervals", "Key Inputs", Integer.valueOf(20));
		numIntervalsInput.setValidRange(2, Integer.MAX_VALUE);
		this.addInput(numIntervalsInput);


		minIntervalTimeInput = new ValueInput("ShortestInterval", "Key Inputs", Double.valueOf(900.0));
		minIntervalTimeInput.setUnitType(TimeUnit.class);
		minIntervalTimeInput.setValidRange(1e-12, Double.POSITIVE_INFINITY);
		//minIntervalTimeInput.setHidden(true);
		this.addInput(minIntervalTimeInput);


		minNumSamples = new IntegerInput("MinNumberSamples", "Key Inputs", Integer.valueOf(16));
		minNumSamples.setValidRange(2, Integer.MAX_VALUE);
		//minNumSamples.setHidden(true);
		this.addInput(minNumSamples);


		confIntInput = new ValueInput("PercentConfidence", "Key Inputs", Double.valueOf(95.0));
		confIntInput.setValidRange(0.01, 99.99);
		this.addInput(confIntInput);

	}


	// default constructor
	public BatchStatistics() { /*  NULL  */ };


	@Override
	public void updateForInput(Input<?> in) {
		super.updateForInput(in);

		// Hide or show the inputs for the tally or time-persistent interval widths
		// depending on the input
		if (in == varInput) {
			if (varInput.getChoice().equals("TALLY_MEAN")) {
				//minIntervalTimeInput.setHidden(true);
				//minNumSamples.setHidden(false);
				inType = InputType.TALLY_MEAN;
			} else {
				//minIntervalTimeInput.setHidden(false);
				//minNumSamples.setHidden(true);
				inType = InputType.TIME_MEAN;
			}
			//GUIFrame.updateUI();
			return;
		}
	}


	@Override
	public void earlyInit() {
		super.earlyInit();

		// change behaviour for input selected
		if (inType == InputType.TALLY_MEAN) {
			currMeanFunction = new CurrentSamplesMean();

		} else {
			currMeanFunction = new CurrentTimeMean();

		}
		// reset all values for new run

		intervalTimeWidth = minIntervalTimeInput.getValue().doubleValue();
		numSamplesWidth   = minNumSamples.getValue().intValue();

		lastTotalTimeValue = 0d;

		// get fractional value from input percent
		confPercent = confIntInput.getValue().doubleValue() / 100.0;

		currAdded = 0;

		currentIntervalValue = 0d;
		lastIntervalTime = 0d;

		correlated = true;
		halfWidth = Double.POSITIVE_INFINITY;

		recordsList.clear();
		numIntervals = 0;

	}


	@Override
	public void addEntity(DisplayEntity ent) {
		super.addEntity(ent);

		double simTime = getSimTime();
		currAdded += 1;

		// if still in initialisation-time, don't assign a new interval
		if(simTime < Simulation.getInitializationTime()) {
			return;
		}

		if(inType == InputType.TALLY_MEAN) {

			currentIntervalValue += getLastValue();

			// New Interval if past threshold for it, tally mean, maybe redo for other
			if(currAdded >= numSamplesWidth) {
				newInterval(simTime);
			}
		}
		else {
			// start scheduling
			if (this.getNumberAdded() == 1L) {
				lastIntervalTime = getFirstSampleTime();
				lastTotalTimeValue = 0d;
				this.scheduleProcess(intervalTimeWidth, 20, scheduleTarget);
			}
		}

	}


	@Override
	public void clearStatistics() {
		super.clearStatistics();

		currAdded = 0;
		currentIntervalValue = 0d;

		recordsList.clear();
		numIntervals = 0;

		lastTotalTimeValue = 0d;
		lastIntervalTime = getSimTime();

	}


	/**
	 *
	 * Saves the current mean-value into the interval and creates a new one
	 *
	 */
	private void newInterval(double simTime) {

		recordsList.add(getIntervalAverage(simTime));
		numIntervals += 1;

		//sumIntervals += getIntervalAverage(simTime);

		if (numIntervals == 2*numIntervalsInput.getValue().intValue()) {
			mergeIntervals();
		}

		// clear for new interval
		currAdded = 0;
		lastIntervalTime = getSimTime();

		currentIntervalValue = 0d;
		lastTotalTimeValue = getTotalTimeValue() + getLastValue()*(simTime-getLastUpdateTime());

		if (numIntervals < numIntervalsInput.getValue().intValue()) {
			correlated = true;
		} else {
			correlated = StatsUtils.isSampleCorrelated(recordsList);
		}

		if(!correlated) {
			halfWidth = StatsUtils.batchHalfWidthInterval(recordsList, 1 - confPercent );
		} else {
			halfWidth = Double.POSITIVE_INFINITY;
		}

	}


	/**
	 *
	 * Merges the intervals to give half the original number
	 * but with double the time length or number of samples
	 * that make up an interval
	 *
	 */
	private void mergeIntervals() {

		int n = numIntervalsInput.getValue().intValue();

		for(int i = 0 ; i < n ; i++) {
			double v1 = recordsList.get(2*i);
			double v2 = recordsList.get(2*i + 1);

			recordsList.set(i, (v1+v2)/2);
		}

		recordsList.drop(n); // drop to halve the size of the array
		numIntervals = n;

		numSamplesWidth *= 2; 		// double collection width
		intervalTimeWidth *= 2;

	}


	/**
	 * This is used to schedule new intervals for when the mean type
	 * is a Time averaged mean rather than a sample mean
	 */
	private class SchedulingTarget extends EntityTarget<BatchStatistics> {

		SchedulingTarget() {
			super(BatchStatistics.this, "newInterval");
		}

		@Override
		public void process() {
			ent.newInterval(getSimTime());
			// schedule next
			ent.scheduleProcess(intervalTimeWidth, 20, scheduleTarget);
		}

	}


	/*
	 * ******************************************************************
	 *
	 * 		OUTPUTS
	 *
	 * ******************************************************************
	 */


	@Output(name="CurrentIntervalAverage",
			description="The average in the current interval",
			unitType=UserSpecifiedUnit.class,
			reportable=true,
			sequence = 0)
	public double getIntervalAverage(double simTime) {

		return currMeanFunction.GetValue(simTime);
	}


	@Output(name="NumberOfIntervals",
			description="The number of complete intervals",
			unitType=DimensionlessUnit.class,
			reportable=true,
			sequence=1)
	public int getNumberIntervals(double simTime) {

		return numIntervals;
	}

	@Output(name="IntervalTimeWidth",
			description="The length of time for an interval",
			unitType=TimeUnit.class,
			reportable=true,
			sequence=2)
	public double getTimeWidth(double simTime) {
		return intervalTimeWidth;
	}

	@Output(name="IntervalSamplesWidth",
			description="The number of samples for an interval",
			unitType=DimensionlessUnit.class,
			reportable=true,
			sequence=3)
	public long getSamplesWidth(double simTime) {
		return numSamplesWidth;
	}

	@Output(name="AllIntervalMeans",
			description="Each of the means of the intervals",
			unitType=UserSpecifiedUnit.class,
			reportable=true,
			sequence=4)
	public DoubleVector getIntervals(double simTime) {

		return new DoubleVector(recordsList);
	}


	@Output(name="PreviousIntervalsMean",
			description="The mean of the previous intervals, not considering the value in the current interval",
			unitType=UserSpecifiedUnit.class,
			reportable=true,
			sequence=5)
	public double getIntervalsMean(double simTime) {

		if(numIntervals == 0) {
			return 0.0;
		}
		return recordsList.sum() / numIntervals;
	}


	@Output(name="HalfWidthInterval",
			description="Half Width interval for the expected mean",
			unitType=UserSpecifiedUnit.class,
			reportable=true,
			sequence=6)
	public double getHalfWidthInterval(double simTime) {

		return halfWidth;
	}


	@Output(name="SamplesIndependent",
			description="True if the assumption that the samples means are independent holds",
			reportable=true,
			sequence=7)
	public boolean getIndependent(double simTime) {

		return !correlated;
	}


	/*
	 * *****************************************************************
	 *
	 *		HELPERS
	 *
	 * *****************************************************************
	 */

	/**
	 *
	 * Enum so that the string does not have to be compared
	 * in the switch-case statement in code above
	 *
	 */
	private enum InputType {
		TALLY_MEAN,
		TIME_MEAN;
	}


	/**
	 *
	 * Return the current mean in the interval for the type of width selected
	 * This delegates for the different types of mean
	 *
	 */
	private interface CurrentIntervalMean {
		public double GetValue(double simTime);
	}

	private class CurrentSamplesMean implements CurrentIntervalMean {
		@Override
		public double GetValue(double simTime) {
			if (currAdded == 0) {
				return 0.0;
			}
			return currentIntervalValue / currAdded;
		}
	}
	private class CurrentTimeMean implements CurrentIntervalMean {
		@Override
		public double GetValue(double simTime) {
			if (simTime - lastIntervalTime <= 1e-12) {
				return 0.0;
			}
			double ret = 0;
			ret = getTotalTimeValue() + getLastValue()*(simTime-getLastUpdateTime()) - lastTotalTimeValue;
			ret /= simTime - lastIntervalTime;
			return ret;
		}
	}

}
