/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2013 Ausenco Engineering Canada Inc.
 * Copyright (C) 2016 JaamSim Software Inc.
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

import com.jaamsim.Graphics.DisplayEntity;
import com.jaamsim.Samples.SampleProvider;
import com.jaamsim.input.Input;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.Output;
import com.jaamsim.input.UnitTypeInput;
import com.jaamsim.input.ValueInput;
import com.jaamsim.units.AngleUnit;
import com.jaamsim.units.TimeUnit;
import com.jaamsim.units.Unit;
import com.jaamsim.units.UserSpecifiedUnit;

/**
 * Super-class for wave generators that produce either sine or square waves.
 * @author Harry King
 *
 */
public abstract class WaveGenerator extends DisplayEntity implements SampleProvider {

	@Keyword(description = "The unit type for the value returned by the wave.",
			exampleList = {"DistanceUnit"})
	protected final UnitTypeInput unitType;

	@Keyword(description = "Amplitude of the generated wave.",
	         exampleList = {"2.0"})
	private final ValueInput amplitude;

	@Keyword(description = "Period of the generated wave.",
			exampleList = {"2 s"})
	private final ValueInput period;

	@Keyword(description = "Initial phase angle of the generated wave.",
			exampleList = {"45 deg"})
	private final ValueInput phaseAngle;

	@Keyword(description = "Offset added to the output of the generated wave.",
			exampleList = {"2.0"})
	private final ValueInput offset;

	{
		unitType = new UnitTypeInput("UnitType", KEY_INPUTS, UserSpecifiedUnit.class);
		unitType.setRequired(true);
		this.addInput(unitType);

		amplitude = new ValueInput("Amplitude", KEY_INPUTS, 1.0d);
		amplitude.setValidRange(0.0d, Double.POSITIVE_INFINITY);
		amplitude.setUnitType(UserSpecifiedUnit.class);
		this.addInput(amplitude);

		period = new ValueInput("Period", KEY_INPUTS, 1.0d);
		period.setUnitType(TimeUnit.class);
		period.setValidRange(0.0d, Double.POSITIVE_INFINITY);
		this.addInput(period);

		phaseAngle = new ValueInput("PhaseAngle", KEY_INPUTS, 0.0d);
		phaseAngle.setUnitType(AngleUnit.class);
		this.addInput(phaseAngle);

		offset = new ValueInput("Offset", KEY_INPUTS, 0.0d);
		offset.setUnitType(UserSpecifiedUnit.class);
		this.addInput(offset);
	}

	@Override
	public void updateForInput(Input<?> in) {
		super.updateForInput(in);

		if (in == unitType) {
			amplitude.setUnitType(unitType.getUnitType());
			offset.setUnitType(unitType.getUnitType());
			return;
		}
	}

	@Override
	public Class<? extends Unit> getUnitType() {
		return unitType.getUnitType();
	}

	@Override
	public Class<? extends Unit> getUserUnitType() {
		return unitType.getUnitType();
	}

	/*
	 * Calculate the current dimensionless signal for the wave.
	 */
	protected abstract double getSignal(double angle);

	@Override
	@Output(name = "Value",
	 description = "The present value for the wave.",
	    unitType = UserSpecifiedUnit.class)
	public double getNextSample(double simTime) {

		// Calculate the present phase angle
		double angle = 2.0*Math.PI * simTime/period.getValue() + phaseAngle.getValue();

		// Set the output value for the wave
		return amplitude.getValue() * this.getSignal(angle)  +  offset.getValue();
	}

	@Override
	public double getMeanValue(double simTime) {
		return offset.getValue();
	}

	@Override
	public double getMinValue() {
		return offset.getValue() - amplitude.getValue();
	}

	@Override
	public double getMaxValue() {
		return offset.getValue() + amplitude.getValue();
	}

}
