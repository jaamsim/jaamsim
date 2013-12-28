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

import com.jaamsim.input.Keyword;
import com.jaamsim.input.OutputHandle;
import com.jaamsim.input.UnitTypeInput;
import com.jaamsim.ProbabilityDistributions.Distribution;
import com.jaamsim.Samples.SampleInput;
import com.jaamsim.Samples.SampleProvider;
import com.jaamsim.input.Output;
import com.jaamsim.ui.FrameBox;
import com.jaamsim.units.Unit;
import com.jaamsim.units.UserSpecifiedUnit;
import com.sandwell.JavaSimulation.Input;
import com.sandwell.JavaSimulation.InputErrorException;

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
	protected final SampleInput inputValue;

	private double value;  // Present value for this calculation

	{
		unitType = new UnitTypeInput( "UnitType", "Key Inputs", UserSpecifiedUnit.class);
		this.addInput(unitType, true);

		inputValue = new SampleInput( "InputValue", "Key Inputs", null);
		inputValue.setUnitType(UserSpecifiedUnit.class);
		this.addInput( inputValue, true);
	}

	@Override
	public void updateForInput( Input<?> in ) {
		super.updateForInput( in );

		if (in == unitType)
			this.setUnitType(this.getUnitType());
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
		if( this.getController() == null && ! inputValue.getHidden() && inputValue.getValue() instanceof Distribution )
			throw new InputErrorException( "The Contoller keyword must be set when the InputValue keyword is set to a ProbabilityDistribution." );
	}

	@Override
	public void earlyInit() {
		super.earlyInit();
		value = 0.0;
	}

	/*
	 * Return the present value for this calculation.
	 */
	public double getValue() {
		return value;
	}

	@Override
	public double getNextSample(double simTime) {
		if( this.getController() == null && ! updateInProgress ) {
			updateInProgress = true;
			this.update(simTime);
			updateInProgress = false;
		}
		return value;
	}

	protected void setUnitType(Class<? extends Unit> ut) {
		inputValue.setUnitType(ut);
		FrameBox.setSelectedEntity(this);  // Update the units in the Output Viewer
	}

	@Override
	public Class<? extends Unit> getUnitType() {
		return unitType.getUnitType();
	}

	@Override
	public double getMeanValue(double simTime) {
		return value;
	}

	/*
	 * Set the present value for this calculation.
	 */
	protected void setValue( double val ) {
		value = val;
	}

	public Double getInputValue( double simTime ) {
		return inputValue.getValue().getNextSample( simTime );
	}

	@Output(name = "Value",
	 description = "The result of the calcuation at the present time.",
	 unitType = UserSpecifiedUnit.class)
	public Double getValue( double simTime ) {
		return value;
	}
}
