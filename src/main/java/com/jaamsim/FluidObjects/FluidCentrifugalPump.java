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

import com.sandwell.JavaSimulation.DoubleInput;
import com.sandwell.JavaSimulation.Keyword;

/**
 * FluidCentrifugalPump models the performance of a centrifugal pump.
 * @author Harry King
 *
 */
public class FluidCentrifugalPump extends FluidComponent {

	@Keyword(desc = "Maximum volumetric flow rate that the pump can generate.",
	         example = "Pump1 MaxFlowRate { 1.0 m3/s }")
	private final DoubleInput maxFlowRateInput;

	@Keyword(desc = "Maximum static pressure that the pump can generate (at zero flow rate).",
	         example = "Pump1 MaxPressure { 1.0 Pa }")
	private final DoubleInput maxPressureInput;

	@Keyword(desc = "Maximum static pressure loss speed for the pump (at maximum flow rate).",
	         example = "Pump1 MaxPressureLoss { 1.0 Pa }")
	private final DoubleInput maxPressureLossInput;

	@Keyword(desc = "Maximum rotation speed for the pump.",
	         example = "Pump1 MaxRotationSpeed { 1.0 rpm }")
	private final DoubleInput maxRotationSpeedInput;

	@Keyword(desc = "Present rotation speed for the pump.",
	         example = "Pump1 PresentRotationSpeed { 1.0 rpm }")
	private final DoubleInput presentRotationSpeedInput;

	{
		maxFlowRateInput = new DoubleInput( "MaxFlowRate", "Key Inputs", 1.0d);
		maxFlowRateInput.setValidRange( 0.0, Double.POSITIVE_INFINITY);
		maxFlowRateInput.setUnits( "m3/s");
		this.addInput( maxFlowRateInput, true);

		maxPressureInput = new DoubleInput( "MaxPressure", "Key Inputs", 1.0d);
		maxPressureInput.setValidRange( 0.0, Double.POSITIVE_INFINITY);
		maxPressureInput.setUnits( "Pa");
		this.addInput( maxPressureInput, true);

		maxPressureLossInput = new DoubleInput( "MaxPressureLoss", "Key Inputs", 1.0d);
		maxPressureLossInput.setValidRange( 0.0, Double.POSITIVE_INFINITY);
		maxPressureLossInput.setUnits( "Pa");
		this.addInput( maxPressureLossInput, true);

		maxRotationSpeedInput = new DoubleInput( "MaxRotationSpeed", "Key Inputs", 1.0d);
		maxRotationSpeedInput.setValidRange( 0.0, Double.POSITIVE_INFINITY);
		maxRotationSpeedInput.setUnits( "rad/s");
		this.addInput( maxRotationSpeedInput, true);

		presentRotationSpeedInput = new DoubleInput( "PresentRotationSpeed", "Key Inputs", 1.0d);
		presentRotationSpeedInput.setValidRange( 0.0, Double.POSITIVE_INFINITY);
		presentRotationSpeedInput.setUnits( "rad/s");
		this.addInput( presentRotationSpeedInput, true);
	}

	/*
	 * Return the outlet pressure for the given inlet pressure and flow acceleration.
	 */
	@Override
	public double calcOutletPressure( double inletPres, double flowAccel ) {
		double speedFactor = presentRotationSpeedInput.getValue() / maxRotationSpeedInput.getValue();
		double flowFactor = this.getFluidFlow().getFlowRate() / maxFlowRateInput.getValue();
		double pres = inletPres;
		pres += maxPressureInput.getValue() * speedFactor * speedFactor;
		pres -= maxPressureLossInput.getValue() * flowFactor * flowFactor;
		return pres;
	}
}
