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
import com.jaamsim.input.Input;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.Output;
import com.jaamsim.input.UnitTypeInput;
import com.jaamsim.input.ValueInput;
import com.jaamsim.ui.FrameBox;
import com.jaamsim.units.DimensionlessUnit;
import com.jaamsim.units.TimeUnit;
import com.jaamsim.units.Unit;
import com.jaamsim.units.UserSpecifiedUnit;
import com.sandwell.JavaSimulation.InputErrorException;

/**
 * The PIDController simulates a Proportional-Integral-Differential type Controller.
 * Error = SetPoint - ProcessVariable
 * Output = ScaleCoefficient * ProportionalGain * [ Error + (Integral/IntegralTime) + (DerivativeTime*Derivative) ]
 * @author Harry King
 *
 */
public class PIDController extends DoubleCalculation {

	@Keyword(description = "The unit type for the set point and the process variable.",
	         example = "PIDController-1 SetPointUnitType { DistanceUnit }")
	private final UnitTypeInput setPointUnitType;

	@Keyword(description = "The set point for the PID controller.\n" +
			"The input can be a number or an entity that returns a number, such as a CalculationObject, ProbabilityDistribution, or a TimeSeries.",
	         example = "PIDController-1 SetPoint { Calc-1 }")
	private final SampleExpInput setPoint;

	@Keyword(description = "The process variable feedback to the PID controller.\n" +
			"The input can be a number or an entity that returns a number, such as a CalculationObject, ProbabilityDistribution, or a TimeSeries.",
	         example = "PIDController-1 ProcessVariable { Calc-1 }")
	private final SampleExpInput processVariable;

	@Keyword(description = "The scale coefficient applied to the output signal.\n" +
			"This coefficient converts from the units for the setpoint and process variable to the units for the " +
			"manipulated variable. The units for the scale coefficient are the ratio of the manipulated variable's unit type " +
			"and the set point's unit type. At present, this input should be entered in the appropriate SI unit, but " +
			"with no unit shown explicitly.",
	         example = "PIDController-1 ScaleConversionCoefficient { 1.0 }")
	private final ValueInput scaleConversionCoefficient;

	@Keyword(description = "The coefficient applied to the proportional feedback loop.",
	         example = "PIDController-1 ProportionalGain { 1.0 }")
	private final ValueInput proportionalGain;

	@Keyword(description = "The coefficient applied to the integral feedback loop.",
	         example = "PIDController-1 IntegralTime { 1.0 s }")
	private final ValueInput integralTime;

	@Keyword(description = "The coefficient applied to the differential feedback loop.",
	         example = "PIDController-1 DerivativeTime { 1.0 s }")
	private final ValueInput derivativeTime;

	@Keyword(description = "The lower limit for the output signal.",
	         example = "PIDController-1 OutputLow { 0.0 }")
	private final ValueInput outputLow;

	@Keyword(description = "The upper limit for the output signal.",
	         example = "PIDController-1 OutputHigh { 1.0 }")
	private final ValueInput outputHigh;

	private double lastUpdateTime;  // The time at which the last update was performed
	private double lastError;  // The previous value for the error signal
	private double integral;  // The integral of the error signal
	private double derivative;  // The derivative of the error signal

	{
		controllerRequired = true;
		inputValue.setHidden(true);

		setPointUnitType = new UnitTypeInput( "SetPointUnitType", "Key Inputs", UserSpecifiedUnit.class);
		this.addInput(setPointUnitType);

		setPoint = new SampleExpInput( "SetPoint", "Key Inputs", new SampleConstant(UserSpecifiedUnit.class, 0.0d));
		setPoint.setUnitType(UserSpecifiedUnit.class);
		setPoint.setEntity(this);
		this.addInput( setPoint);

		processVariable = new SampleExpInput( "ProcessVariable", "Key Inputs", new SampleConstant(UserSpecifiedUnit.class, 0.0d));
		processVariable.setUnitType(UserSpecifiedUnit.class);
		processVariable.setEntity(this);
		this.addInput( processVariable);

		proportionalGain = new ValueInput( "ProportionalGain", "Key Inputs", 1.0d);
		proportionalGain.setValidRange( 0.0d, Double.POSITIVE_INFINITY);
		proportionalGain.setUnitType(DimensionlessUnit.class);
		this.addInput( proportionalGain);

		scaleConversionCoefficient = new ValueInput( "ScaleConversionCoefficient", "Key Inputs", 1.0d);
		scaleConversionCoefficient.setValidRange( 0.0d, Double.POSITIVE_INFINITY);
		scaleConversionCoefficient.setUnitType(DimensionlessUnit.class);
		this.addInput( scaleConversionCoefficient);

		integralTime = new ValueInput( "IntegralTime", "Key Inputs", 1.0d);
		integralTime.setValidRange( 1.0e-10, Double.POSITIVE_INFINITY);
		integralTime.setUnitType( TimeUnit.class );
		this.addInput( integralTime);

		derivativeTime = new ValueInput( "DerivativeTime", "Key Inputs", 0.0d);
		derivativeTime.setValidRange( 0.0d, Double.POSITIVE_INFINITY);
		derivativeTime.setUnitType( TimeUnit.class );
		this.addInput( derivativeTime);

		outputLow = new ValueInput( "OutputLow", "Key Inputs", Double.NEGATIVE_INFINITY);
		outputLow.setUnitType(UserSpecifiedUnit.class);
		this.addInput( outputLow);

		outputHigh = new ValueInput( "OutputHigh", "Key Inputs", Double.POSITIVE_INFINITY);
		outputHigh.setUnitType(UserSpecifiedUnit.class);
		this.addInput( outputHigh);
	}

	@Override
	public void updateForInput( Input<?> in ) {
		super.updateForInput( in );

		if (in == setPointUnitType)
			this.setSPUnitType(setPointUnitType.getUnitType());
	}

	@Override
	protected boolean repeatableInputs() {
		return super.repeatableInputs()
				&& ! (setPoint.getValue() instanceof Distribution)
				&& ! (processVariable.getValue() instanceof Distribution);
	}

	@Override
	public void validate() {
		super.validate();

		// Confirm that the SetPoint keyword has been set
		if( setPoint.getValue() == null )
			throw new InputErrorException( "The SetPoint keyword must be set." );

		// Confirm that the ProcessVariable keyword has been set
		if( processVariable.getValue() == null )
			throw new InputErrorException( "The ProcessVariable keyword must be set." );
	}

	@Override
	public void earlyInit() {
		super.earlyInit();
		lastError = 0.0;
		integral = 0.0;
		lastUpdateTime = 0.0;
	}

	@Override
	protected void setUnitType(Class<? extends Unit> ut) {
		super.setUnitType(ut);
		outputLow.setUnitType(ut);
		outputHigh.setUnitType(ut);
		FrameBox.reSelectEntity();  // Update the units in the Output Viewer
	}

	private void setSPUnitType(Class<? extends Unit> ut) {
		setPoint.setUnitType(ut);
		processVariable.setUnitType(ut);
		FrameBox.reSelectEntity();  // Update the units in the Output Viewer
	}

	@Override
	protected double calculateValue(double simTime) {

		// Calculate the elapsed time
		double dt = simTime - lastUpdateTime;

		// Calculate the error signal
		double error = setPoint.getValue().getNextSample(simTime) - processVariable.getValue().getNextSample(simTime);

		// Calculate integral and differential terms
		double intgrl = integral + error * dt;
		double deriv = 0.0;
		if( dt > 0.0 )
			deriv = ( error - lastError ) / dt;

		// Calculate the output value
		double val = error;
		val += intgrl / integralTime.getValue();
		val += derivativeTime.getValue() * deriv;
		val *= scaleConversionCoefficient.getValue() * proportionalGain.getValue();

		// Condition the output value
		val = Math.max( val, outputLow.getValue());
		val = Math.min( val, outputHigh.getValue());

		return val;
	}

	@Override
	public void update(double simTime) {
		super.update(simTime);
		double dt = simTime - lastUpdateTime;
		double error = setPoint.getValue().getNextSample(simTime) - processVariable.getValue().getNextSample(simTime);
		integral += error * dt;
		lastError = error;
		lastUpdateTime = simTime;
		return;
	}

	@Output(name = "Error",
	 description = "The value for SetPoint - ProcessVariable.")
	public double getError( double simTime ) {
		return setPoint.getValue().getNextSample(simTime) - processVariable.getValue().getNextSample(simTime);
	}

	@Output(name = "ProportionalValue",
	 description = "The proportional component of the output value.",
	    unitType = UserSpecifiedUnit.class)
	public double getProportionalValue( double simTime ) {
		double error = setPoint.getValue().getNextSample(simTime) - processVariable.getValue().getNextSample(simTime);
		return scaleConversionCoefficient.getValue() * proportionalGain.getValue() * error;
	}

	@Output(name = "IntegralValue",
	 description = "The integral component of the output value.",
	    unitType = UserSpecifiedUnit.class)
	public double getIntegralValue( double simTime ) {
		return scaleConversionCoefficient.getValue() * proportionalGain.getValue()
				* integral / integralTime.getValue();
	}

	@Output(name = "DerivativeValue",
	 description = "The derivative component of the output value.",
	    unitType = UserSpecifiedUnit.class)
	public double getDifferentialValue( double simTime ) {
		return scaleConversionCoefficient.getValue() * proportionalGain.getValue()
				* derivativeTime.getValue() * derivative;
	}
}
