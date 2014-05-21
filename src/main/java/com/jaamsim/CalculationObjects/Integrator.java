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
import com.jaamsim.input.Keyword;
import com.jaamsim.input.ValueInput;
import com.jaamsim.ui.FrameBox;
import com.jaamsim.units.TimeUnit;
import com.jaamsim.units.Unit;
import com.jaamsim.units.UserSpecifiedUnit;

/**
 * The Integrator returns the integral of the input values.
 * @author Harry King
 *
 */
public class Integrator extends DoubleCalculation {

	@Keyword(description = "The initial value for the integral at time = 0.",
	         example = "Integrator-1 InitialValue { 5.5 }")
	private final ValueInput initialValue;

	@Keyword(description = "The time scale for the integration:  integral = InitialValue + 1/IntegralTime * integral(x)\n" +
			"The input can be a number or an entity that returns a number, such as a CalculationObject, ProbabilityDistribution, or a TimeSeries.",
	         example = "Integrator-1 IntegralTime { 5 s }")
	protected final SampleExpInput integralTime;

	private double lastUpdateTime;  // The time at which the last update was performed
	private double integral; // The present value for the integral

	{
		controllerRequired = true;

		initialValue = new ValueInput( "InitialValue", "Key Inputs", 0.0d);
		initialValue.setUnitType(UserSpecifiedUnit.class);
		this.addInput( initialValue);

		integralTime = new SampleExpInput( "IntegralTime", "Key Inputs", new SampleConstant(TimeUnit.class, 1.0));
		integralTime.setUnitType(TimeUnit.class);
		integralTime.setEntity(this);
		this.addInput( integralTime);
	}

	@Override
	protected boolean repeatableInputs() {
		return super.repeatableInputs()
				&& ! (integralTime.getValue() instanceof Distribution);
	}

	@Override
	protected void setUnitType(Class<? extends Unit> ut) {
		super.setUnitType(ut);
		initialValue.setUnitType(ut);
		FrameBox.reSelectEntity();  // Update the units in the Output Viewer
	}

	@Override
	public void earlyInit() {
		super.earlyInit();
		lastUpdateTime = 0.0;
		integral = 0.0;
	}

	@Override
	protected double calculateValue(double simTime) {
		double dt = simTime - lastUpdateTime;
		double scale = integralTime.getValue().getNextSample(simTime);
		return ( integral + this.getInputValue(simTime) * dt )/scale  +  initialValue.getValue();
	}

	@Override
	public void update(double simTime) {
		super.update(simTime);
		double dt = simTime - lastUpdateTime;
		integral += this.getInputValue(simTime) * dt;
		lastUpdateTime = simTime;
	}

}
