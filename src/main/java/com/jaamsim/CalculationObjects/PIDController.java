/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2013 Ausenco Engineering Canada Inc.
 * Copyright (C) 2016-2019 JaamSim Software Inc.
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

import com.jaamsim.Samples.SampleInput;
import com.jaamsim.input.Input;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.Output;
import com.jaamsim.input.UnitTypeInput;
import com.jaamsim.input.ValueInput;
import com.jaamsim.units.DimensionlessUnit;
import com.jaamsim.units.RateUnit;
import com.jaamsim.units.TimeUnit;
import com.jaamsim.units.Unit;
import com.jaamsim.units.UserSpecifiedUnit;

/**
 * The PIDController simulates a Proportional-Integral-Differential type Controller.
 * Error = (SetPoint - ProcessVariable) / ProcessVariableScale
 * Output =  ProportionalGain * [ Error + (Integral/IntegralTime) + (DerivativeTime*Derivative) ]
 * @author Harry King
 *
 */
public class PIDController extends DoubleCalculation {

	@Keyword(description = "The set point for the PID controller. The unit type for the set point "
	                     + "is given by the UnitType keyword.",
	         exampleList = {"1.2 m", "TimeSeries1", "'1[m] + 2*[TimeSeries1].Value'"})
	private final SampleInput setPoint;

	@Keyword(description = "The process variable feedback to the PID controller. The unit type "
	                     + "for the process variable is given by the UnitType keyword.",
	         exampleList = {"Process", "'1[m] + [Process].Value'"})
	private final SampleInput processVariable;

	@Keyword(description = "A constant with the same unit type as the process variable and the "
	                     + "set point. The difference between the process variable and the set "
	                     + "point is divided by this quantity to make a dimensionless variable.",
	         exampleList = {"1.0 kg"})
	private final ValueInput processVariableScale;

	@Keyword(description = "The unit type for the output from the PID controller.",
	         exampleList = {"DistanceUnit"})
	protected final UnitTypeInput outputUnitType;

	@Keyword(description = "The coefficient applied to the proportional feedback loop. "
	                     + "The unit type for the proportional gain is given by the "
	                     + "outputUnitType keyword.",
	         exampleList = {"1.3 m"})
	private final ValueInput proportionalGain;

	@Keyword(description = "The time scale applied to the integral feedback loop.",
	         exampleList = {"1.0 s"})
	private final ValueInput integralTime;

	@Keyword(description = "The time scale applied to the differential feedback loop.",
	         exampleList = {"1.0 s"})
	private final ValueInput derivativeTime;

	@Keyword(description = "The lower limit for the output signal.",
	         exampleList = {"0.0 m"})
	private final ValueInput outputLow;

	@Keyword(description = "The upper limit for the output signal.",
	         exampleList = {"1.0 m"})
	private final ValueInput outputHigh;

	private double lastUpdateTime;  // The time at which the last update was performed
	private double lastError;  // The previous value for the error signal
	private double integral;  // The integral of the error signal

	{
		inputValue.setHidden(true);

		setPoint = new SampleInput("SetPoint", KEY_INPUTS, null);
		setPoint.setUnitType(UserSpecifiedUnit.class);
		setPoint.setRequired(true);
		this.addInput(setPoint);

		processVariable = new SampleInput("ProcessVariable", KEY_INPUTS, null);
		processVariable.setUnitType(UserSpecifiedUnit.class);
		processVariable.setRequired(true);
		this.addInput(processVariable);

		processVariableScale = new ValueInput("ProcessVariableScale", KEY_INPUTS, 1.0d);
		processVariableScale.setValidRange(0.0d, Double.POSITIVE_INFINITY);
		processVariableScale.setUnitType(UserSpecifiedUnit.class);
		this.addInput(processVariableScale);

		outputUnitType = new UnitTypeInput("OutputUnitType", KEY_INPUTS, UserSpecifiedUnit.class);
		outputUnitType.setRequired(true);
		this.addInput(outputUnitType);

		proportionalGain = new ValueInput("ProportionalGain", KEY_INPUTS, 1.0d);
		proportionalGain.setValidRange(0.0d, Double.POSITIVE_INFINITY);
		proportionalGain.setUnitType(UserSpecifiedUnit.class);
		this.addInput(proportionalGain);

		integralTime = new ValueInput("IntegralTime", KEY_INPUTS, 1.0d);
		integralTime.setValidRange(1.0e-10, Double.POSITIVE_INFINITY);
		integralTime.setUnitType(TimeUnit.class );
		this.addInput(integralTime);

		derivativeTime = new ValueInput("DerivativeTime", KEY_INPUTS, 1.0d);
		derivativeTime.setValidRange(0.0d, Double.POSITIVE_INFINITY);
		derivativeTime.setUnitType(TimeUnit.class );
		this.addInput(derivativeTime);

		outputLow = new ValueInput("OutputLow", KEY_INPUTS, Double.NEGATIVE_INFINITY);
		outputLow.setUnitType(UserSpecifiedUnit.class);
		this.addInput(outputLow);

		outputHigh = new ValueInput("OutputHigh", KEY_INPUTS, Double.POSITIVE_INFINITY);
		outputHigh.setUnitType(UserSpecifiedUnit.class);
		this.addInput(outputHigh);
	}

	public PIDController() {}

	@Override
	public void updateForInput( Input<?> in ) {
		super.updateForInput( in );

		if (in == outputUnitType) {
			Class<? extends Unit> outUnitType = outputUnitType.getUnitType();
			outputLow.setUnitType(outUnitType);
			outputHigh.setUnitType(outUnitType);
			proportionalGain.setUnitType(outUnitType);
			return;
		}
	}

	@Override
	protected void setUnitType(Class<? extends Unit> ut) {
		super.setUnitType(ut);
		setPoint.setUnitType(ut);
		processVariable.setUnitType(ut);
		processVariableScale.setUnitType(ut);
	}

	@Override
	public Class<? extends Unit> getUnitType() {
		return outputUnitType.getUnitType();
	}

	@Override
	public Class<? extends Unit> getUserUnitType() {
		return outputUnitType.getUnitType();
	}

	@Override
	public void earlyInit() {
		super.earlyInit();
		lastError = 0.0;
		integral = 0.0;
		lastUpdateTime = 0.0;
	}

	@Output(name = "Error",
	 description = "The difference between the set point and the process variable values divided "
	             + "by the process variable scale.",
	    unitType = DimensionlessUnit.class,
	    sequence = 1)
	public double getError(double simTime) {
		if (setPoint.getValue() == null || processVariable.getValue() == null)
			return Double.NaN;
		double diff = setPoint.getValue().getNextSample(simTime)
				- processVariable.getValue().getNextSample(simTime);
		return diff/processVariableScale.getValue();
	}

	@Override
	protected double calculateValue(double simTime, double inputVal, double lastTime, double lastInputVal, double lastVal) {

		// Calculate the elapsed time
		double dt = simTime - lastTime;

		// Calculate the error signal
		double error = this.getError(simTime);

		// Calculate integral and differential terms
		double intgrl = integral + error*dt;
		double deriv = 0.0;
		if (dt > 0.0)
			deriv = (error - lastError)/dt;

		// Calculate the output value
		double val = (error +  intgrl/integralTime.getValue() + deriv*derivativeTime.getValue());
		val *= proportionalGain.getValue();

		// Condition the output value
		val = Math.max(val, outputLow.getValue());
		val = Math.min(val, outputHigh.getValue());

		return val;
	}

	@Override
	public void update(double simTime) {
		super.update(simTime);
		double dt = simTime - lastUpdateTime;
		double error = this.getError(simTime);
		integral += error * dt;
		lastError = error;
		lastUpdateTime = simTime;
		return;
	}

	@Output(name = "Integral",
	 description = "The integral of the dimensionless error value.",
	    unitType = TimeUnit.class,
	    sequence = 2)
	public double getIntegral(double simTime) {
		return integral;
	}

	@Output(name = "Derivative",
	 description = "The derivative of the dimensionless error value.",
	    unitType = RateUnit.class,
	    sequence = 3)
	public double getDerivative(double simTime) {
		double derivative = 0.0;
		double dt = simTime - lastUpdateTime;
		if (dt > 0.0)
			derivative = (getError(simTime) - lastError)/dt;
		return derivative;
	}

	@Output(name = "ProportionalValue",
	 description = "The proportional component of the output value.",
	    unitType = UserSpecifiedUnit.class,
	    sequence = 4)
	public double getProportionalValue(double simTime) {
		return getError(simTime) * proportionalGain.getValue();
	}

	@Output(name = "IntegralValue",
	 description = "The integral component of the output value.",
	    unitType = UserSpecifiedUnit.class,
	    sequence = 5)
	public double getIntegralValue(double simTime) {
		return (integral / integralTime.getValue()) * proportionalGain.getValue();
	}

	@Output(name = "DerivativeValue",
	 description = "The derivative component of the output value.",
	    unitType = UserSpecifiedUnit.class,
	    sequence = 6)
	public double getDifferentialValue(double simTime) {
		return getDerivative(simTime) * derivativeTime.getValue() * proportionalGain.getValue();
	}

}
