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

import com.sandwell.JavaSimulation.DoubleInput;
import com.sandwell.JavaSimulation.EntityInput;
import com.sandwell.JavaSimulation.InputErrorException;
import com.sandwell.JavaSimulation.Keyword;

/**
 * The PIDController simulates a Proportional-Integral-Differential type Controller.
 * @author Harry King
 *
 */
public class PIDController extends DoubleCalculation {

	@Keyword(desc = "The Calculation entity that represents the set point for the PID controller.",
	         example = "PIDController1 SetPoint { Calc1 }")
	private final EntityInput<DoubleCalculation> setPointInput;

	@Keyword(desc = "The Calculation entity that represents the process variable feedback to the PID controller.",
	         example = "PIDController1 ProcessVariable { Calc1 }")
	private final EntityInput<DoubleCalculation> processVariableInput;

	@Keyword(desc = "The coefficient applied to the proportional feedback loop.",
	         example = "PIDController1 ProportionalGain { 1.0 }")
	private final DoubleInput proportionalGainInput;

	@Keyword(desc = "The coefficient applied to the integral feedback loop.",
	         example = "PIDController1 IntegralGain { 1.0 }")
	private final DoubleInput integralGainInput;

	@Keyword(desc = "The coefficient applied to the differential feedback loop.",
	         example = "PIDController1 DifferentialGain { 1.0 }")
	private final DoubleInput differentialGainInput;

	private double lastUpdateTime;  // The time at which the last update was performed (seconds)
	private double error;  // The present value for the error signal
	private double lastError;  // The previous value for the error signal
	private double integral;  // The integral of the error signal
	private double differential;  // The differential of the error signal

	{
		setPointInput = new EntityInput<DoubleCalculation>( DoubleCalculation.class, "SetPoint", "Key Inputs", null);
		this.addInput( setPointInput, true);

		processVariableInput = new EntityInput<DoubleCalculation>( DoubleCalculation.class, "ProcessVariable", "Key Inputs", null);
		this.addInput( processVariableInput, true);

		proportionalGainInput = new DoubleInput( "ProportionalGain", "Key Inputs", 0.0);
		this.addInput( proportionalGainInput, true);

		integralGainInput = new DoubleInput( "IntegralGain", "Key Inputs", 0.0);
		this.addInput( integralGainInput, true);

		differentialGainInput = new DoubleInput( "DifferentailGain", "Key Inputs", 0.0);
		this.addInput( differentialGainInput, true);
	}

	@Override
	public void validate() {
		super.validate();

		// Confirm that the SetPoint keyword has been set
		if( setPointInput.getValue() == null ) {
			throw new InputErrorException( "The SetPoint keyword must be set." );
		}

		// Confirm that the ProcessVariable keyword has been set
		if( setPointInput.getValue() == null ) {
			throw new InputErrorException( "The ProcessVariable keyword must be set." );
		}
	}

	@Override
	public void earlyInit() {
		super.earlyInit();
		lastError = 0.0;
		integral = 0.0;
		lastUpdateTime = 0.0;
	}

	@Override
	public void update() {
		double val;

		// Calculate the elapsed time
		double t = 3600.0 * this.getCurrentTime();  // convert from hours to seconds
		double dt = t - lastUpdateTime;

		// Calculate the error signal
		error = setPointInput.getValue().getValue() - processVariableInput.getValue().getValue();

		// Calculate integral and differential terms
		integral += error * dt;
		if( dt > 0.0 ) {
			differential = ( error - lastError ) / dt;
		}
		else {
			differential = 0.0;
		}

		// Calculate the present value
		val = proportionalGainInput.getValue() * error;
		val += integralGainInput.getValue() * integral;
		val += differentialGainInput.getValue() * differential;

		// Set the present value
		this.setValue( val );

		// Record values needed for the next update
		lastError = error;
		lastUpdateTime = t;
		return;
	}
}
