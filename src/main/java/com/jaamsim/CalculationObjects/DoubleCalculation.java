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

import com.jaamsim.ProbabilityDistributions.Distribution;
import com.jaamsim.Samples.SampleConstant;
import com.jaamsim.Samples.SampleExpInput;
import com.jaamsim.Samples.SampleProvider;
import com.jaamsim.basicsim.ErrorException;
import com.jaamsim.input.Input;
import com.jaamsim.input.InputErrorException;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.Output;
import com.jaamsim.input.OutputHandle;
import com.jaamsim.input.UnitTypeInput;
import com.jaamsim.ui.FrameBox;
import com.jaamsim.units.Unit;
import com.jaamsim.units.UserSpecifiedUnit;

/**
 * DoubleCalculation is the super-class for all calculations that return a double.
 * @author Harry King
 *
 */
public abstract class DoubleCalculation extends CalculationEntity
implements SampleProvider {

	@Keyword(description = "The unit type for the value returned by the calculation.",
	         example = "Calc-1 UnitType { DistanceUnit }")
	protected final UnitTypeInput unitType;

	@Keyword(description = "The input value for the present calculation.\n" +
			"The input can be a number or an entity that returns a number, such as a CalculationObject, ProbabilityDistribution, or a TimeSeries.",
	         example = "Calc-1 InputValue { Calc-2 }")
	protected final SampleExpInput inputValue;

	private double value;  // Present value for this calculation

	{
		unitType = new UnitTypeInput( "UnitType", "Key Inputs", UserSpecifiedUnit.class);
		this.addInput(unitType);

		inputValue = new SampleExpInput( "InputValue", "Key Inputs", new SampleConstant(UserSpecifiedUnit.class, 0.0d));
		inputValue.setUnitType(UserSpecifiedUnit.class);
		inputValue.setEntity(this);
		this.addInput( inputValue);
	}

	@Override
	public void updateForInput( Input<?> in ) {
		super.updateForInput( in );

		if (in == unitType)
			this.setUnitType(this.getUnitType());
	}

	/**
	 * Test whether the inputs to this object can be sampled repeatably.
	 * An input is repeatable if getNextSample(t) returns the same value for successive calls.
	 * At present, the only non-repeatable input is a probability distribution.
	 * @return = TRUE if all the inputs are repeatable.
	 */
	protected boolean repeatableInputs() {
		return inputValue.getHidden() || ! (inputValue.getValue() instanceof Distribution);
	}

	@Override
	public OutputHandle getOutputHandle(String outputName) {
		OutputHandle out = super.getOutputHandle(outputName);
		if( out.getUnitType() == UserSpecifiedUnit.class )
			out.setUnitType( unitType.getUnitType() );
		return out;
	}

	@Override
	public void validate() {
		super.validate();

		// Confirm that the InputValue keyword has been set (unless it is hidden by the sub-class)
		if( ! inputValue.getHidden() && inputValue.getValue() == null )
			throw new InputErrorException( "The InputValue keyword must be set." );

		// If the input is a ProbabilityDistribution, then a Controller must be used to ensure repeatable results
		if( this.getController() == null && ! this.repeatableInputs() )
			throw new InputErrorException( "The Contoller keyword must be set when an input to the object is a ProbabilityDistribution, " +
					"or any other object that cannot be sampled repeatably." );
	}

	@Override
	public void earlyInit() {
		super.earlyInit();
		value = 0.0;
	}

	/*
	 * Return the stored value for this calculation.
	 */
	public double getValue() {
		return value;
	}

	/**
	 * Calculates and returns the output value at the given simulation time.
	 * <p>
	 * This method returns an output value that varies smoothly between the
	 * values stored at each update.
	 * @param simTime - the specified simulation time.
	 * @return the output value at the specified simulation time.
	 */
	protected abstract double calculateValue(double simTime);

	@Override
	public void update(double simTime) {
		value = this.calculateValue(simTime);
	}

	@Override
	public double getNextSample(double simTime) {

		// If this object has a non-repeatable input, then return the value stored by the last update
		if( !this.repeatableInputs() )
			return value;

		// Has this method has already been called for this object
		if( calculationInProgress ) {
			if( this.getController() != null ) {
				return value;
			}
			throw new ErrorException("A tight loop is present. Try setting the Controller keyword for object: %s.", this.getName());
		}

		// Perform the calculation (calls getNextSample for all its inputs)
		calculationInProgress = true;
		double val = this.calculateValue(simTime);
		calculationInProgress = false;

		return val;
	}

	protected void setUnitType(Class<? extends Unit> ut) {
		inputValue.setUnitType(ut);
		FrameBox.reSelectEntity();  // Update the units in the Output Viewer
	}

	@Override
	public Class<? extends Unit> getUnitType() {
		return unitType.getUnitType();
	}

	@Override
	public double getMeanValue(double simTime) {
		return value;
	}

	@Override
	public double getMinValue() {
		return value;
	}

	@Override
	public double getMaxValue() {
		return value;
	}

	/**
	 * Returns the value for the input to this calculation object at the
	 * specified simulation time.
	 * @param simTime - the specified simulation time.
	 * @return the input value to this calculation object.
	 */
	public double getInputValue( double simTime ) {
		return inputValue.getValue().getNextSample(simTime);
	}

	@Output(name = "Value",
	 description = "The result of the calcuation at the present time.",
	 unitType = UserSpecifiedUnit.class)
	public double getValue( double simTime ) {
		return calculateValue(simTime);
	}
}
