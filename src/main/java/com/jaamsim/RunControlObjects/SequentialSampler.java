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
import java.util.Collections;

import com.jaamsim.Graphics.DisplayEntity;
import com.jaamsim.basicsim.Simulation;
import com.jaamsim.Samples.SampleListInput;
import com.jaamsim.Samples.SampleProvider; 		/*
import com.jaamsim.basicsim.EntityTarget;
import com.jaamsim.basicsim.FileEntity;
import com.jaamsim.events.ProcessTarget; 		 */
import com.jaamsim.input.Input;
import com.jaamsim.input.Output;
import com.jaamsim.input.InputErrorException;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.ValueListInput;
import com.jaamsim.input.UnitTypeListInput;
import com.jaamsim.units.DimensionlessUnit;
import com.jaamsim.units.Unit;
import com.jaamsim.units.UserSpecifiedUnit;
import com.jaamsim.datatypes.DoubleVector;
import com.jaamsim.datatypes.BooleanVector;


/**
 * Reads from chosen inputs and stores them.
 * If the confidence interval of the means are less than some given
 * tolerances then it will output a true condition
 *
 * @author Michael Bergman
 *
 */
public class SequentialSampler extends DisplayEntity {

	@Keyword(description = "The unit types for the selected outputs for the simulation run. "
			,
            exampleList = {"DistanceUnit  SpeedUnit"})
	private final UnitTypeListInput unitTypeList;

	@Keyword(description = "One or more selected outputs to be saved at the end of each"
            + "simulation run. Each output is specified by an expression. "
			+ "These values are used to calculate confidence intervals.",
            exampleList = {"{ [Entity1].Output1 } { [Entity2].Output2 }"})
	private final SampleListInput outputMonitorList;


	@Keyword(description = "One or more interval widths that will cause the simulation to pause/stop "
			+ "when the interval values from the output above are below the input length",
            exampleList = {" { 0.1 s } { 1 } "})
	private final SampleListInput halfWidthList;


	@Keyword(description="This accept values between 0.01 and 99.99 as percentages for the "
			+ "confidence intervals.  If only one value is present, all will have the same "
			+ "percentage confidence interval.",
			exampleList = {"  95.0  70.0  99.5  "}
			)
	private final ValueListInput confidencePercentList;

	//private FileEntity file; // file is here just in case we wish to save outputs to a log-file
	private int runNumber;
	private int numRecords;

	private final ArrayList<Double>   rangesList = new ArrayList<>(); // stores the target range
	private final ArrayList<Double>   confIntList = new ArrayList<>(); // stores the decimal confidence intervals


	private final DoubleVector sumVals = new DoubleVector();
	private final DoubleVector sumSqVals = new DoubleVector();

	private final DoubleVector intervalsList = new DoubleVector();  //stores the intervals for the current run
	private final DoubleVector meansList = new DoubleVector(); // stores the means for the current run
	private final BooleanVector withinList = new BooleanVector();

	private boolean pauseCond = false;

	// Initialise inputs
	{
		ArrayList<Class<? extends Unit>> defList = new ArrayList<>();
		defList.add(DimensionlessUnit.class);
		unitTypeList = new UnitTypeListInput("UnitList", "Key Inputs", defList);
		this.addInput(unitTypeList);

		outputMonitorList = new SampleListInput("DataList", "Key Inputs",new ArrayList<SampleProvider>());
		outputMonitorList.setUnitType(UserSpecifiedUnit.class);
		outputMonitorList.setEntity(this);
		this.addInput(outputMonitorList);

		halfWidthList = new SampleListInput("HalfWidths", "Key Inputs",new ArrayList<SampleProvider>());
		halfWidthList.setUnitType(UserSpecifiedUnit.class);
		halfWidthList.setValidRange(1e-9, Double.POSITIVE_INFINITY);
		halfWidthList.setEntity(this);
		this.addInput(halfWidthList);

		DoubleVector dv = new DoubleVector();
		dv.add(95.0);

		confidencePercentList = new ValueListInput("ConfidencePercent", "Key Inputs", dv);
		confidencePercentList.setUnitType(DimensionlessUnit.class);
		confidencePercentList.setValidRange(0.01, 99.99);
		this.addInput(confidencePercentList);
	}

	public SequentialSampler() {/* Nothing */}

	@Override
	public void updateForInput(Input<?> in) {
		super.updateForInput(in);
		if (in == unitTypeList) {
			outputMonitorList.setUnitTypeList(unitTypeList.getUnitTypeList());
			halfWidthList.setUnitTypeList(unitTypeList.getUnitTypeList());
			return;
		}
		if (in == outputMonitorList) {
			numRecords = outputMonitorList.getListSize();
			return;
		}  // */
	}

	@Override
	public void validate() {
		super.validate();
		// make sure half width has same number of entries as the output monitor
		// this is to keep units consistent
		if (outputMonitorList.getValue().size() != halfWidthList.getValue().size()) {
			throw new InputErrorException( "There must be the same number of entries in DataList and HalfWidths!" );
		}
		if(confidencePercentList.getValue().size() != 1) {
			if(confidencePercentList.getValue().size() != numRecords) {
				throw new InputErrorException("The number of Confidence Intervals must be either 1 or the same as the number of entries in DataList!");
			}
		}
	}

	@Override
	public void earlyInit() {
		super.earlyInit();
		// if first run then clear saved inputs
		if(Simulation.isFirstRun()) {
			runNumber = 0;
			pauseCond = false;

			meansList.clear();
			intervalsList.clear();
			sumVals.clear();
			sumSqVals.clear();

			intervalsList.clear();
			meansList.clear();
			rangesList.clear();
			confIntList.clear();
			withinList.clear();

			sumVals.fillWithEntriesOf(numRecords, 0.0);
			sumSqVals.fillWithEntriesOf(numRecords, 0.0);
			withinList.fillWithEntriesOf(numRecords, false);

			meansList.fillWithEntriesOf(numRecords, 0.0);
			intervalsList.fillWithEntriesOf(numRecords, Double.POSITIVE_INFINITY);


			// set the allowable range
			for(int i = 0 ; i < numRecords ; i++) {
				// absolute value so I don't have to worry about negatives
				double val = Math.abs (halfWidthList.getValue().get(i).getNextSample(0.0));

				// in case input is actually 0
				val += val==0.0 ? 1 : 0;

				double factor = Unit.getDisplayedUnitFactor(outputMonitorList.getUnitType(i));
				rangesList.add(Double.valueOf(val/factor));
			}

			// fill the confidence interval with values.
			if(confidencePercentList.getValue().size() == 1) {

				double val = confidencePercentList.getValue().get(0) / 100;
				val = 1 - val;
				confIntList.addAll(Collections.nCopies(numRecords, Double.valueOf(val)));

			} else {
				for(int i = 0 ; i < numRecords ; i++) {

					double val = confidencePercentList.getValue().get(i) / 100;
					val = 1 - val;
					confIntList.add(Double.valueOf(val));
				}
			}
		}
	}

	// Save to the array list each run, and recalculate the half width interval
	private void EndOfRun() {

		runNumber++;

		for(int i = 0; i < numRecords ; i++) {

			// read value from input field and add it to the list of saved means
			double val = outputMonitorList.getValue().get(i).getNextSample(getSimTime());
			double factor = Unit.getDisplayedUnitFactor(outputMonitorList.getUnitType(i));

			//add to current array
			double tmp = val/factor;

			sumVals.addAt( tmp, i );
			sumSqVals.addAt( tmp*tmp, i );
		}

		RecalculateLists();
		pauseCond = getAllLessThanInput();
	}

	// call at end of current run
	@Override
	public void doEnd() {
		super.doEnd();

		EndOfRun();
		/* remove this?
		if(!pauseCond) {
			EndOfRun();
		} */
	}

	/*
	 * *********************************************************************************
	 *
	 *		 OUTPUTS
	 *
	 * *********************************************************************************
	 */

	@Output(name="CompletedRuns",
			description="How many runs have been completed",
			unitType=DimensionlessUnit.class,
			reportable = true,
			sequence=0)
	public int getRunNumber(double simTime) {
		return runNumber;
	}


	@Output(name="Means",
			description="The running means of the samples",
			reportable = true,
			sequence=1)
	public DoubleVector getMeans(double simTime) {
		return meansList;
	}

	// these are not calculated each iteration, only at the end of each run and thus saving cycles
	@Output(name="HalfWidthIntervals",
			description="The lengths of the half-width intervals",
			reportable = true,
			sequence=2)
	public DoubleVector getHalfWidthIntervals(double simTime) {
		return intervalsList;
	}

	@Output(name="AllWithinUserRange",
			description="True if all calculated half-width intervals are less than the inputs",
			reportable = true,
			sequence=3)
	public boolean getAllWithinWidth(double simTime) {

		// it does not change every simulation step
		return pauseCond;
	}


	@Output(name="WidthCheckList",
			description="Outputs whether each width is within the desired range",
			reportable = true,
			sequence=4)
	public BooleanVector getWidthCheckList(double simTime) {

		return withinList;
	}

	/*
	 * *********************************************************************************
	 *
	 * 		Helper functions
	 *
	 * *********************************************************************************
	*/

	/**
	 * Calculates the half width intervals at the end of each run
	 */
	private final void RecalculateLists() {
		int n = runNumber;

		// if first run then initialise and fill the arrays
		if (Simulation.isFirstRun()) {
			for(int i = 0 ; i < numRecords ; i++) {
				meansList.set(i, sumVals.get(i));
			}

		} else {
			double[] stdErr = new double[numRecords];
			for(int i = 0 ; i < numRecords ; i++) {
				meansList.set(i, sumVals.get(i)/n);

				stdErr[i] = Math.sqrt( (sumSqVals.get(i) - sumVals.get(i)*sumVals.get(i)/n) / (n-1) / n );

				double p = confIntList.get(i).doubleValue();
				double t = StatsUtils.tStat2Tail(p, n);

				intervalsList.set(i, t*stdErr[i]);
				withinList.set(i, intervalsList.get(i) < rangesList.get(i).doubleValue());
			}
		}
	}

	/**
	 * checks to see if all the intervals are within a range
	 */
	private final boolean getAllLessThanInput() {
		boolean result = true;

		for(int i = 0 ; i < numRecords ; i++) {
			if (intervalsList.get(i) >= rangesList.get(i).doubleValue()) {
				result = false;
			}
		}

		return result;
	}
}