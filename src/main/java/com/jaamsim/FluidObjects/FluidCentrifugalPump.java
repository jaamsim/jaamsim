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
package com.jaamsim.FluidObjects;

import com.jaamsim.CalculationObjects.DoubleCalculation;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.ValueInput;
import com.jaamsim.units.PressureUnit;
import com.jaamsim.units.VolumeFlowUnit;
import com.sandwell.JavaSimulation.EntityInput;
import com.sandwell.JavaSimulation.InputErrorException;

/**
 * FluidCentrifugalPump models the performance of a centrifugal pump.
 * @author Harry King
 *
 */
public class FluidCentrifugalPump extends FluidComponent {

	@Keyword(description = "Maximum volumetric flow rate that the pump can generate.",
	         example = "Pump1 MaxFlowRate { 1.0 m3/s }")
	private final ValueInput maxFlowRateInput;

	@Keyword(description = "Maximum static pressure that the pump can generate (at zero flow rate).",
	         example = "Pump1 MaxPressure { 1.0 Pa }")
	private final ValueInput maxPressureInput;

	@Keyword(description = "Maximum static pressure loss speed for the pump (at maximum flow rate).",
	         example = "Pump1 MaxPressureLoss { 1.0 Pa }")
	private final ValueInput maxPressureLossInput;

	@Keyword(description = "The CalculationEntity whose output sets the rotational speed of the pump.  " +
			"The output value is ratio of present speed to maximum speed (0.0 - 1.0).",
	         example = "Pump1 SpeedController { Calc1 }")
	private final EntityInput<DoubleCalculation> speedControllerInput;

	{
		maxFlowRateInput = new ValueInput( "MaxFlowRate", "Key Inputs", 1.0d);
		maxFlowRateInput.setValidRange( 0.0, Double.POSITIVE_INFINITY);
		maxFlowRateInput.setUnitType( VolumeFlowUnit.class );
		this.addInput( maxFlowRateInput);

		maxPressureInput = new ValueInput( "MaxPressure", "Key Inputs", 1.0d);
		maxPressureInput.setValidRange( 0.0, Double.POSITIVE_INFINITY);
		maxPressureInput.setUnitType( PressureUnit.class );
		this.addInput( maxPressureInput);

		maxPressureLossInput = new ValueInput( "MaxPressureLoss", "Key Inputs", 1.0d);
		maxPressureLossInput.setValidRange( 0.0, Double.POSITIVE_INFINITY);
		maxPressureLossInput.setUnitType( PressureUnit.class );
		this.addInput( maxPressureLossInput);

		speedControllerInput = new EntityInput<DoubleCalculation>( DoubleCalculation.class, "SpeedController", "Key Inputs", null);
		this.addInput( speedControllerInput);
	}

	@Override
	public void validate() {
		super.validate();

		// Confirm that the SpeedController keyword has been set
		if( speedControllerInput.getValue() == null ) {
			throw new InputErrorException( "The SpeedController keyword must be set." );
		}
	}

	/*
	 * Return the outlet pressure for the given inlet pressure and flow acceleration.
	 */
	@Override
	public double calcOutletPressure( double inletPres, double flowAccel ) {
		double speedFactor = speedControllerInput.getValue().getValue();
		speedFactor = Math.max(speedFactor, 0.0);
		speedFactor = Math.min(speedFactor, 1.0);
		double flowFactor = this.getFluidFlow().getFlowRate() / maxFlowRateInput.getValue();
		double pres = inletPres;
		pres += maxPressureInput.getValue() * speedFactor * speedFactor;
		pres -= maxPressureLossInput.getValue() * Math.abs(flowFactor) * flowFactor;
		return pres;
	}
}
