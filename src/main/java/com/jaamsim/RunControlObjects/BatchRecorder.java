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
import com.jaamsim.Samples.SampleInput;
import com.jaamsim.basicsim.EntityTarget;
import com.jaamsim.basicsim.Simulation;
import com.jaamsim.datatypes.DoubleVector;
import com.jaamsim.events.Conditional;
import com.jaamsim.events.EventManager;
import com.jaamsim.events.ProcessTarget;
import com.jaamsim.input.Input;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.Output;
import com.jaamsim.input.StringChoiceInput;
import com.jaamsim.input.UnitTypeInput;
import com.jaamsim.input.IntegerInput;
import com.jaamsim.input.ValueInput;
import com.jaamsim.units.Unit;
import com.jaamsim.units.DimensionlessUnit;
import com.jaamsim.units.TimeUnit;
import com.jaamsim.units.UserSpecifiedUnit;

/**
 * Watches an input and calculates the means of the intervals
 * over a single run
 *
 * @author Michael Bergman
 *
 */
public class BatchRecorder extends DisplayEntity {


	@Keyword(description="The smallest time width of an interval",
			 exampleList={"1.0 h"})
	private final ValueInput minIntervalWidth;


	@Keyword(description="The number of intervals for which to store values",
			 exampleList={"20"})
	private final IntegerInput numIntervalsIn;


	@Keyword(description="The unit type for the variable whose statistics will be collected.",
	         exampleList={"DistanceUnit"})
	private final UnitTypeInput unitType;


	@Keyword(description="The variable for which the batched statistics will be collected."
			 + "This should be a time varying, non-averaged quantity",
	         exampleList={"'[Entity].attrib1'"})
	private final SampleInput sampleValue;


	@Keyword(description="Percent confidence iterval input",
			 exampleList={"95.0"})
	private final ValueInput confIntInput;


	@Keyword(description="The type of variable being watched for the batches",
	         exampleList={ "TRANSIENT" })
	private final StringChoiceInput typeInput;


	// stores the choices for the StringChoiceInput
	public static final ArrayList<String> variableTypes;

	private double intervalWidth = 0d;
	private int numIntervals = 0;
	private double lastIntervalTime = 0d;
	private double lastIntervalSampleVal = 0d;
	private double confPercent = 0d;

	private double halfWidth = Double.POSITIVE_INFINITY;
	private boolean correlated = true;

	private ValueFunction typeFunc;
	private final DoubleVector valsList = new DoubleVector();

	protected final ProcessTarget scheduleTarget = new SchedulingTarget();


	static {

		// Initialisation of selection list options
		variableTypes = new ArrayList<>();
		variableTypes.add("ACCUMULATED"); // TODO: Remove this input?, average over the time period
		variableTypes.add("TIME_MEAN");  // Some overlapping functionality but I cannot see how this is avoidable
		variableTypes.add("TRANSIENT");  // this one may need to be re-implemented with a value listener

	}


	{	// Initialisation

		minIntervalWidth = new ValueInput("SmallestInterval", "Key Inputs", Double.valueOf(900.0));
		minIntervalWidth.setUnitType(TimeUnit.class);
		minIntervalWidth.setValidRange(1e-15d, Double.POSITIVE_INFINITY);
		this.addInput(minIntervalWidth);


		numIntervalsIn = new IntegerInput("NumberIntervals", "Key Inputs", Integer.valueOf(20));
		numIntervalsIn.setValidRange(2, Integer.MAX_VALUE);
		this.addInput(numIntervalsIn);


		typeInput = new StringChoiceInput("InputType", "Key Inputs", InputType.ACCUMULATED.getOrd());
		typeInput.setChoices(variableTypes);
		//typeInput.setRequired(true);
		this.addInput(typeInput);


		unitType = new UnitTypeInput("UnitType", "Key Inputs", UserSpecifiedUnit.class);
		unitType.setRequired(true);
		this.addInput(unitType);


		sampleValue = new SampleInput("SampleValue", "Key Inputs", null);
		sampleValue.setUnitType(UserSpecifiedUnit.class);
		sampleValue.setEntity(this);
		sampleValue.setRequired(true);
		this.addInput(sampleValue);


		confIntInput = new ValueInput("PercentConfidence", "Key Inputs", Double.valueOf(95.0));
		confIntInput.setValidRange(0.01, 99.99);
		this.addInput(confIntInput);

	}


	public BatchRecorder() {  /* NULL */  }


	@Override
	public void updateForInput(Input<?> in) {
		super.updateForInput(in);

		if(in == unitType) {
			sampleValue.setUnitType(unitType.getUnitType());
			return;
		}

	}


	@Override
	public void earlyInit() {
		super.earlyInit();

		// reset values for new run

		// get fraction value of the percentage
		confPercent = confIntInput.getValue().doubleValue() / 100.0;

		intervalWidth = minIntervalWidth.getValue().doubleValue();
		numIntervals = 0;

		valsList.clear();

		halfWidth = Double.POSITIVE_INFINITY;
		correlated = true;

		// choose the function
		switch ( InputType.getFromNum(typeInput.getValue().intValue()) ) {
		case ACCUMULATED:
			typeFunc = new Accumulated();
			break;
		case TIME_MEAN:
			typeFunc = new TimeAveraged();
			break;
		case TRANSIENT:
			typeFunc = new Transient();
			break;
		}

	}


	@Override
	public void clearStatistics() {
		super.clearStatistics();

		// reset records

		double simTime = getSimTime();
		lastIntervalTime = simTime;
		lastIntervalSampleVal = 0.0;
		numIntervals = 0;
		valsList.clear();

		typeFunc.ClearData(simTime);
		typeFunc.doValueTrace();

		//schedule interval - to start saving
		this.scheduleProcess(intervalWidth, 20, scheduleTarget);

	}


	/**
	 *
	 * Saves the current value in the interval and creates a new one
	 *
	 */
	public void newInterval(double simTime) {

		double newVal =  getCurrentValueInInterval(simTime);
		valsList.add(newVal);
		numIntervals++;

		lastIntervalTime = simTime;
		lastIntervalSampleVal = sampleValue.getValue().getNextSample(simTime);

		if (numIntervals >= numIntervalsIn.getValue()*2) {
			mergeIntervals();
			intervalWidth *= 2; // double the interval time
		}

		typeFunc.ClearData(simTime);

		if (numIntervals >= numIntervalsIn.getValue()) {
			correlated = StatsUtils.isSampleCorrelated(valsList);
		} else {
			correlated = true;
		}

		if(!correlated) {
			halfWidth = StatsUtils.batchHalfWidthInterval( valsList, 1 - confPercent );
		} else {
			halfWidth = Double.POSITIVE_INFINITY;
		}

		this.scheduleProcess(intervalWidth, 20, scheduleTarget);

	}


	/**
	 *
	 * Merges the intervals to give half the original number
	 * but with double the time length
	 *
	 */
	private void mergeIntervals() {

		int n = numIntervalsIn.getValue().intValue();
		// average pairs of consecutive intervals
		for(int i = 0 ; i < n ; i++) {
			double v1 = valsList.get(2*i);
			double v2 = valsList.get(2*i + 1);
			// Assuming all time intervals have the same width
			valsList.set(i, (v1 + v2) / 2);
		}

		valsList.drop(n); // drop the second half of the vector; the obsolete data
		numIntervals = n;

	}


	/**
	 * Event controller to schedule the intervals
	 */
	private class SchedulingTarget extends EntityTarget<BatchRecorder> {
		SchedulingTarget() {
			super(BatchRecorder.this, "NewInterval");
		}

		@Override
		public void process() {
			double simTime = getSimTime();
			ent.newInterval(simTime);
		}
	}


	@Override
	public Class<? extends Unit> getUserUnitType() {

		return unitType.getUnitType();
	}


	/*
	 * **********************************************************************
	 *		 TODO: Any More?
	 *		OUTPUTS
	 *		 TODO: better descriptions!
	 * **********************************************************************
	 */


	@Output(name = "CurrentMean",
			description = "The current mean in the current interval",
			unitType = UserSpecifiedUnit.class,
			reportable = true,
			sequence = 1)
	public double getCurrentValueInInterval(double simTime) {

		return typeFunc.Invoke(simTime) / (simTime-lastIntervalTime);
	}


	@Output(name = "NumberIntervals",
			description = "The number of intervals currently",
			unitType = DimensionlessUnit.class,
			reportable = true,
			sequence = 2)
	public int getNumIntervals(double simTime) {

		return numIntervals;
	}


	@Output(name = "IntervalWidth",
			description = "Length of time of an interval",
			unitType = TimeUnit.class,
			reportable = true,
			sequence = 3)
	public double getIntervalWidth(double simTime) {

		return intervalWidth;
	}


	@Output(name = "ValuesInIntervals",
			description = "Values saved from each of the intervals",
			unitType = UserSpecifiedUnit.class,
			reportable = true,
			sequence = 4)
	public DoubleVector getIntervalsValues(double simTime) {

		return new DoubleVector(valsList);
	}


	@Output(name = "PreviousIntervalsMean",
			description = "The mean of the previous intervals, not considering the value in the current interval",
			unitType = UserSpecifiedUnit.class,
			reportable = true,
			sequence = 5)
	public double getMean(double simTime) {

		return valsList.sum() / numIntervals;
	}


	@Output(name = "HalfWidthInterval",
			description = "The half width interval of the confidence of the mean",
			unitType = UserSpecifiedUnit.class,
			reportable = true,
			sequence = 6)
	public double getHalfWidthInterval(double simTime) {

		return halfWidth;
	}


	@Output(name = "IsIndependent",
			description = "True if the assumption that the samples means are independent holds",
			reportable = true,
			sequence = 7)
	public boolean getIndependent(double simTime) {

		return !correlated;
	}


	/*
	 * *********************************************************************************
	 *
	 * 		Function chooser, to allow for the three different methods
	 *
	 * *********************************************************************************
	 */

	/**
	 * Enum so that the string does not have to be compared
	 * in the switch-case statement in code above
	 */
	private static enum InputType {
		ACCUMULATED (0),
		TIME_MEAN (1),
		TRANSIENT (2);

		private final int val;

		InputType(int val) { this.val = val; }

		public final int getOrd() { return val; }

		public static InputType getFromNum(int num) {
			assert (0<=num && num <=2);

			InputType ret = InputType.ACCUMULATED;;

			switch(num) {
				case 0: ret=InputType.ACCUMULATED; 	break;
				case 1: ret=InputType.TIME_MEAN; 	break;
				case 2: ret=InputType.TRANSIENT; 	break;
			}
			return ret;
		}
	}


	/**
	 * To delegate for the different mean functions
	 */
	private static abstract class ValueFunction {
		public abstract double Invoke(double simTime);
		public void ClearData(double simTime) {    }
		public void doValueTrace() {	}
	}


	/**
	 * For when the sample value increases
	 */
	private class Accumulated extends ValueFunction {
		@Override
		public double Invoke(double simTime) {
			double cval = sampleValue.getValue().getNextSample(simTime);
			return cval - lastIntervalSampleVal;
		}
	}


	/**
	 * For when the sample value is the time-averaged mean
	 */
	private class TimeAveraged extends ValueFunction {
		@Override
		public double Invoke(double simTime) {
			double initTime = Simulation.getInitializationTime();
			double cval = sampleValue.getValue().getNextSample(simTime)*(simTime - initTime);
			return cval - lastIntervalSampleVal*(lastIntervalTime - initTime);
		}
	}


	/**
	 * For when the sample value
	 */
	private class Transient extends ValueFunction {

		private double lastSampleVal;
		private double lastChangeTime;
		private double accumulated;

		private final Conditional valueChange;
		private final ProcessTarget doValueChange;

		{

			lastSampleVal = 0d;
			lastChangeTime = 0d;
			accumulated = 0d;

			valueChange = new ValueChangeConditional(this);
			doValueChange = new TraceTarget();

		}

		@Override
		public double Invoke(double simTime) {

			double dt = simTime-lastChangeTime;
			return accumulated + lastSampleVal*dt;
		}

		@Override
		public void ClearData(double simTime) {

			//lastSampleVal = 0d;
			lastChangeTime = simTime;
			accumulated = 0d;
		}

		/**
		 * Return true if the value being monitored has changed.
		 * This also updates the internal variables if so.
		 */
		boolean valueChanged() {

			boolean ret = false;

			double simTime = BatchRecorder.this.getSimTime();
			double cval = sampleValue.getValue().getNextSample(simTime);

			// if value has changed, then update interval variables
			if(cval != lastSampleVal) {
				// update values
				double dt = simTime - lastChangeTime;
				ret = true;
				accumulated += lastSampleVal*dt;
				lastSampleVal  = cval;
				lastChangeTime = simTime;
			}
			return ret;
		}

		@Override
		public void doValueTrace() {
			EventManager.scheduleUntil(doValueChange, valueChange, null);
		}

		/**
		 * To manage the value listener, called when condition is true
		 */
		private class TraceTarget extends ProcessTarget {

			@Override
			public String getDescription() {
				return BatchRecorder.this.getName() + ".TransientValueFunction.scheduleListener";
			}

			@Override
			public void process() {
				doValueTrace();
			}

		}

	}

	/**
	 * Checks if the value being watched has changed
	 */
	private static class ValueChangeConditional extends Conditional {

		private final Transient func;

		public ValueChangeConditional(Transient func) {
			this.func = func;
		}

		@Override
		public boolean evaluate() {
			return func.valueChanged();
		}
	}

}
