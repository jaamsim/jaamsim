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
import com.jaamsim.input.Output;
import com.jaamsim.input.OutputInput;
import com.jaamsim.input.ValueInput;
import com.jaamsim.units.TimeUnit;
import com.sandwell.JavaSimulation.InputErrorException;

/**
 * The PIDController simulates a Proportional-Integral-Differential type Controller.
 * Error = SetPoint - ProcessVariable
 * Output = ScaleCoefficient * ProportionalGain * [ Error + (Integral/IntegralTime) + (DerivativeTime*Derivative) ]
 * @author Harry King
 *
 */
public class PIDController extends DoubleCalculation {


	@Keyword(description = "The Entity and Output that provides the set point for the PID controller.",
	         example = "PIDController-1 SetPoint { Calc1 Value }")
	private final OutputInput<Double> setPointInput;

	@Keyword(description = "The Entity and Output that provides the process variable feedback to the PID controller.",
	         example = "PIDController-1 ProcessVariable { Calc1 Value }")
	private final OutputInput<Double> processVariableInput;

	@Keyword(description = "The scale coefficient applied to the output signal.",
	         example = "PIDController-1 ScaleConversionCoefficient { 1.0 }")
	private final ValueInput scaleConversionCoefficientInput;

	@Keyword(description = "The coefficient applied to the proportional feedback loop.",
	         example = "PIDController-1 ProportionalGain { 1.0 }")
	private final ValueInput proportionalGainInput;

	@Keyword(description = "The coefficient applied to the integral feedback loop.",
	         example = "PIDController-1 IntegralTime { 1.0 s }")
	private final ValueInput integralTimeInput;

	@Keyword(description = "The coefficient applied to the differential feedback loop.",
	         example = "PIDController-1 DerivativeTime { 1.0 s }")
	private final ValueInput derivativeTimeInput;

	@Keyword(description = "The lower limit for the output signal.",
	         example = "PIDController-1 OutputLow { 0.0 }")
	private final ValueInput outputLowInput;

	@Keyword(description = "The upper limit for the output signal.",
	         example = "PIDController-1 OutputHigh { 1.0 }")
	private final ValueInput outputHighInput;

	private double lastUpdateTime;  // The time at which the last update was performed (seconds)
	private double error;  // The present value for the error signal
	private double lastError;  // The previous value for the error signal
	private double integral;  // The integral of the error signal
	private double derivative;  // The derivative of the error signal

	{
		inputValueInput.setHidden(true);

		setPointInput = new OutputInput<Double>( Double.class, "SetPoint", "Key Inputs", null);
		this.addInput( setPointInput, true);

		processVariableInput = new OutputInput<Double>( Double.class, "ProcessVariable", "Key Inputs", null);
		this.addInput( processVariableInput, true);

		proportionalGainInput = new ValueInput( "ProportionalGain", "Key Inputs", 0.0d);
		proportionalGainInput.setValidRange( 0.0d, Double.POSITIVE_INFINITY);
		this.addInput( proportionalGainInput, true);

		scaleConversionCoefficientInput = new ValueInput( "ScaleConversionCoefficient", "Key Inputs", 1.0d);
		scaleConversionCoefficientInput.setValidRange( 0.0d, Double.POSITIVE_INFINITY);
		this.addInput( scaleConversionCoefficientInput, true);

		integralTimeInput = new ValueInput( "IntegralTime", "Key Inputs", 1.0d);
		integralTimeInput.setValidRange( 1.0e-10, Double.POSITIVE_INFINITY);
		integralTimeInput.setUnitType( TimeUnit.class );
		this.addInput( integralTimeInput, true);

		derivativeTimeInput = new ValueInput( "DerivativeTime", "Key Inputs", 0.0d);
		derivativeTimeInput.setValidRange( 0.0d, Double.POSITIVE_INFINITY);
		derivativeTimeInput.setUnitType( TimeUnit.class );
		this.addInput( derivativeTimeInput, true);

		outputLowInput = new ValueInput( "OutputLow", "Key Inputs", Double.NEGATIVE_INFINITY);
		this.addInput( outputLowInput, true);

		outputHighInput = new ValueInput( "OutputHigh", "Key Inputs", Double.POSITIVE_INFINITY);
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
		if( processVariableInput.getValue() == null ) {
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
	public void update(double simTime) {
		double val;

		// Calculate the elapsed time
		double dt = simTime - lastUpdateTime;

		// Calculate the error signal
		error = setPointInput.getOutputValue(simTime) - processVariableInput.getOutputValue(simTime);

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
		lastUpdateTime = simTime;
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
