/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2013 Ausenco Engineering Canada Inc.
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
package com.jaamsim.CalculationObjects;

import java.util.ArrayList;

import com.jaamsim.Commands.KeywordCommand;
import com.jaamsim.Graphics.DisplayEntity;
import com.jaamsim.Samples.SampleInput;
import com.jaamsim.Samples.SampleProvider;
import com.jaamsim.basicsim.Entity;
import com.jaamsim.events.EventManager;
import com.jaamsim.input.Input;
import com.jaamsim.input.InputAgent;
import com.jaamsim.input.InputCallback;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.KeywordIndex;
import com.jaamsim.input.Output;
import com.jaamsim.input.UnitTypeInput;
import com.jaamsim.units.Unit;
import com.jaamsim.units.UserSpecifiedUnit;

/**
 * DoubleCalculation is the super-class for all calculations that return a double.
 * @author Harry King
 *
 */
public abstract class DoubleCalculation extends CalculationEntity
implements SampleProvider {

	@Keyword(description = "The unit type for the input value(s) to the calculation.",
	         exampleList = {"DistanceUnit"})
	protected final UnitTypeInput unitType;

	@Keyword(description = "The input value for the present calculation.",
	         exampleList = {"1.5", "TimeSeries1", "'3 + 2*[Queue1].QueueLength'"})
	protected final SampleInput inputValue;

	private double lastUpdateTime;  // The time at which the last update was performed
	private double lastInputValue;  // Input to this object evaluated at the last update time
	private double lastValue;       // Output from this object evaluated at the last update time

	{
		unitType = new UnitTypeInput("UnitType", KEY_INPUTS, UserSpecifiedUnit.class);
		unitType.setRequired(true);
		unitType.setCallback(unitTypeInputCallback);
		this.addInput(unitType);

		inputValue = new SampleInput("InputValue", KEY_INPUTS, 0.0d);
		inputValue.setUnitType(UserSpecifiedUnit.class);
		this.addInput(inputValue);
	}

	public DoubleCalculation() {}

	static final InputCallback unitTypeInputCallback = new InputCallback() {
		@Override
		public void callback(Entity ent, Input<?> inp) {
			((DoubleCalculation)ent).updateUnitType();
		}
	};

	void updateUnitType() {
		this.setUnitType(unitType.getUnitType());
	}

	protected void setUnitType(Class<? extends Unit> ut) {
		inputValue.setUnitType(ut);
	}

	@Override
	public Class<? extends Unit> getUnitType() {
		return unitType.getUnitType();
	}

	@Override
	public Class<? extends Unit> getUserUnitType() {
		return unitType.getUnitType();
	}

	@Override
	public void earlyInit() {
		super.earlyInit();
		lastUpdateTime = 0.0;
		lastValue = this.getInitialValue();
	}

	@Override
	public void lateInit() {
		super.lateInit();
		lastInputValue = getInputValue(0.0d);
	}

	public double getInitialValue() {
		return 0.0;
	}

	/**
	 * Returns the value for the input to this calculation object at the
	 * specified simulation time.
	 * @param simTime - specified simulation time.
	 * @return input value to this calculation object.
	 */
	public double getInputValue(double simTime) {

		// An exception will be generated if the model has an infinite loop causing the
		// call stack size to be exceeded
		double ret = lastInputValue;
		try {
			ret = inputValue.getNextSample(simTime);
		} catch(Exception e) {
			if (EventManager.hasCurrent()) {
				error("Closed loop detected in calculation. Insert a UnitDelay object.");
			}
		}
		return ret;
	}

	/*
	 * Return the stored value for this calculation.
	 */
	public double getLastValue() {
		return lastValue;
	}

	/**
	 * Returns the output value at the specified simulation time.
	 * <p>
	 * This method returns an output value that varies smoothly between the
	 * values stored at each update.
	 * @param simTime - specified simulation time.
	 * @param inputVal - input value at the specified simulation time.
	 * @param lastTime - simulation time when the most recent update was performed.
	 * @param lastInputVal - input value when the most recent update was performed.
	 * @param lastVal - output value when the moset recent update was performed.
	 * @return output value at the specified simulation time.
	 */
	protected abstract double calculateValue(double simTime, double inputVal, double lastTime, double lastInputVal, double lastVal);

	@Override
	public void update(double simTime) {

		// Calculate the new input value to the calculation
		double inputVal = getInputValue(simTime);

		// Calculate the new output value
		double newValue = this.calculateValue(simTime, inputVal, lastUpdateTime, lastInputValue, lastValue);

		// Store the new input and output values
		lastUpdateTime = simTime;
		lastInputValue = inputVal;
		lastValue = newValue;
	}

	@Output(name = "Value",
	 description = "The result of the calculation at the present time.",
	    unitType = UserSpecifiedUnit.class)
	public double getNextSample(double simTime) {
		return getNextSample(this, simTime);
	}

	@Override
	public double getNextSample(Entity thisEnt, double simTime) {

		// Calculate the new input value to the calculation
		double inputVal = getInputValue(simTime);

		// Return the new output value
		return this.calculateValue(simTime, inputVal, lastUpdateTime, lastInputValue, lastValue);
	}

	@Override
	public double getMeanValue(double simTime) {
		return lastValue;
	}

	@Override
	public boolean canLink(boolean dir) {
		// UnitType input must be set or hidden
		return dir && (!unitType.isDefault() || unitType.getHidden());
	}

	@Override
	public void linkTo(DisplayEntity nextEnt, boolean dir) {
		if (!dir || !(nextEnt instanceof DoubleCalculation))
			return;

		ArrayList<KeywordIndex> kwList = new ArrayList<>();
		DoubleCalculation nextCalc = (DoubleCalculation) nextEnt;

		// Set the Controller for the next object
		if (!nextCalc.controller.getHidden()
				&& !controller.getHidden() && !controller.isDefault()) {
			String key = controller.getKeyword();
			kwList.add( InputAgent.formatArgs(key, controller.getValue().getName()) );
		}

		// Set the UnitType input for the next object
		if (!nextCalc.unitType.getHidden()
				&& !unitType.getHidden() && !unitType.isDefault()) {
			String key = unitType.getKeyword();
			kwList.add( InputAgent.formatArgs(key, getUnitType().getSimpleName()) );
		}

		// Set the InputValue input for the next object
		if (!nextCalc.inputValue.getHidden()) {
			String key = nextCalc.inputValue.getKeyword();
			kwList.add( InputAgent.formatArgs(key, this.getName()) );
		}

		if (kwList.isEmpty())
			return;

		KeywordIndex[] kws = new KeywordIndex[kwList.size()];
		kwList.toArray(kws);
		InputAgent.storeAndExecute(new KeywordCommand(nextCalc, kws));
	}

}
