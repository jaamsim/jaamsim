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

import com.jaamsim.input.Output;
import com.sandwell.JavaSimulation.DoubleInput;
import com.sandwell.JavaSimulation.EntityInput;
import com.sandwell.JavaSimulation.InputErrorException;
import com.sandwell.JavaSimulation.Keyword;

/**
 * The PIDController simulates a Proportional-Integral-Differential type Controller.
 * Error = SetPoint - ProcessVariable
 * Output = ScaleCoefficient * ProportionalGain * [ Error + (Integral/IntegralTime) + (DerivativeTime*Derivative) ]
 * @author Harry King
 *
 */
public class PIDController extends DoubleCalculation {

	@Keyword(description = "The Calculation entity that represents the set point for the PID controller.",
	         example = "PIDController-1 SetPoint { Calc1 }")
	private final EntityInput<DoubleCalculation> setPointInput;

	@Keyword(description = "The Calculation entity that represents the process variable feedback to the PID controller.",
	         example = "PIDController-1 ProcessVariable { Calc1 }")
	private final EntityInput<DoubleCalculation> processVariableInput;

	@Keyword(description = "The scale coefficient applied to the output signal.",
	         example = "PIDController-1 ScaleConversionCoefficient { 1.0 }")
	private final DoubleInput scaleConversionCoefficientInput;

	@Keyword(description = "The coefficient applied to the proportional feedback loop.",
	         example = "PIDController-1 ProportionalGain { 1.0 }")
	private final DoubleInput proportionalGainInput;

	@Keyword(description = "The coefficient applied to the integral feedback loop.",
	         example = "PIDController-1 IntegralTime { 1.0 s }")
	private final DoubleInput integralTimeInput;

	@Keyword(description = "The coefficient applied to the differential feedback loop.",
	         example = "PIDController-1 DerivativeTime { 1.0 s }")
	private final DoubleInput derivativeTimeInput;

	@Keyword(description = "The lower limit for the output signal.",
	         example = "PIDController-1 OutputLow { 0.0 }")
	private final DoubleInput outputLowInput;

	@Keyword(description = "The upper limit for the output signal.",
	         example = "PIDController-1 OutputHigh { 1.0 }")
	private final DoubleInput outputHighInput;

	private double lastUpdateTime;  // The time at which the last update was performed (seconds)
	private double error;  // The present value for the error signal
	private double lastError;  // The previous value for the error signal
	private double integral;  // The integral of the error signal
	private double derivative;  // The derivative of the error signal

	{
		inputValueInput.setHidden(true);

		setPointInput = new EntityInput<DoubleCalculation>( DoubleCalculation.class, "SetPoint", "Key Inputs", null);
		this.addInput( setPointInput, true);

		processVariableInput = new EntityInput<DoubleCalculation>( DoubleCalculation.class, "ProcessVariable", "Key Inputs", null);
		this.addInput( processVariableInput, true);

		proportionalGainInput = new DoubleInput( "ProportionalGain", "Key Inputs", 0.0d);
		proportionalGainInput.setValidRange( 0.0d, Double.POSITIVE_INFINITY);
		this.addInput( proportionalGainInput, true);

		scaleConversionCoefficientInput = new DoubleInput( "ScaleConversionCoefficient", "Key Inputs", 1.0d);
		scaleConversionCoefficientInput.setValidRange( 0.0d, Double.POSITIVE_INFINITY);
		this.addInput( scaleConversionCoefficientInput, true);

		integralTimeInput = new DoubleInput( "IntegralTime", "Key Inputs", 1.0d);
		integralTimeInput.setValidRange( 1.0e-10, Double.POSITIVE_INFINITY);
		integralTimeInput.setUnits( "s" );
		this.addInput( integralTimeInput, true);

		derivativeTimeInput = new DoubleInput( "DerivativeTime", "Key Inputs", 0.0d);
		derivativeTimeInput.setValidRange( 0.0d, Double.POSITIVE_INFINITY);
		derivativeTimeInput.setUnits( "s" );
		this.addInput( derivativeTimeInput, true);

		outputLowInput = new DoubleInput( "OutputLow", "Key Inputs", Double.NEGATIVE_INFINITY);
		this.addInput( outputLowInput, true);

		outputHighInput = new DoubleInput( "OutputHigh", "Key Inputs", Double.POSITIVE_INFINITY);
		this.addInput( outputHighInput, true);
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
	public void update(double simtime) {
		double val;

		// Calculate the elapsed time
		double t = 3600.0 * simtime;  // convert from hours to seconds
		double dt = t - lastUpdateTime;

		// Calculate the error signal
		error = setPointInput.getValue().getValue() - processVariableInput.getValue().getValue();

		// Calculate integral and differential terms
		integral += error * dt;
		if( dt > 0.0 ) {
			derivative = ( error - lastError ) / dt;
		}
		else {
			derivative = 0.0;
		}

		// Calculate the output value
		val = error;
		val += integral / integralTimeInput.getValue();
		val += derivativeTimeInput.getValue() * derivative;
		val *= scaleConversionCoefficientInput.getValue() * proportionalGainInput.getValue();

		// Condition the output value
		val = Math.max( val, outputLowInput.getValue());
		val = Math.min( val, outputHighInput.getValue());

		// Set the present value
		this.setValue( val );

		// Record values needed for the next update
		lastError = error;
		lastUpdateTime = t;
		return;
	}

	@Output(name = "Error",
	 description = "The value for SetPoint - ProcessVariable.")
	public Double getError( double simTime ) {
		return error;
	}

	@Output(name = "ProportionalValue",
	 description = "The proportional component of the output value.")
	public Double getProportionalValue( double simTime ) {
		return scaleConversionCoefficientInput.getValue() * proportionalGainInput.getValue() * error;
	}

	@Output(name = "IntegralValue",
	 description = "The integral component of the output value.")
	public Double getIntegralValue( double simTime ) {
		return scaleConversionCoefficientInput.getValue() * proportionalGainInput.getValue()
				* integral / integralTimeInput.getValue();
	}

	@Output(name = "DerivativeValue",
	 description = "The derivative component of the output value.")
	public Double getDifferentialValue( double simTime ) {
		return scaleConversionCoefficientInput.getValue() * proportionalGainInput.getValue()
				* derivativeTimeInput.getValue() * derivative;
	}
}
