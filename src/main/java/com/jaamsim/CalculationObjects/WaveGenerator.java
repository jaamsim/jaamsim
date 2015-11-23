/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2013 Ausenco Engineering Canada Inc.
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

import com.jaamsim.input.Keyword;
import com.jaamsim.input.ValueInput;
import com.jaamsim.ui.FrameBox;
import com.jaamsim.units.AngleUnit;
import com.jaamsim.units.TimeUnit;
import com.jaamsim.units.Unit;
import com.jaamsim.units.UserSpecifiedUnit;

/**
 * Super-class for wave generators that produce either sine or square waves.
 * @author Harry King
 *
 */
public abstract class WaveGenerator extends DoubleCalculation {

	@Keyword(description = "Amplitude of the generated wave",
	         example = "Wave1 Amplitude { 2.0 }")
	private final ValueInput amplitude;

	@Keyword(description = "Period of the generated wave",
	         example = "Wave1 Period { 2 s }")
	private final ValueInput period;

	@Keyword(description = "Initial phase angle of the generated wave",
	         example = "Wave1 PhaseAngle { 45 deg }")
	private final ValueInput phaseAngle;

	@Keyword(description = "Offset added to the output of the generated wave",
	         example = "Wave1 Offset { 2.0 }")
	private final ValueInput offset;

	{
		inputValue.setHidden(true);

		amplitude = new ValueInput( "Amplitude", "Key Inputs", 1.0d);
		amplitude.setValidRange( 0.0d, Double.POSITIVE_INFINITY);
		amplitude.setUnitType(UserSpecifiedUnit.class);
		this.addInput( amplitude);

		period = new ValueInput("Period", "Key Inputs", 1.0d);
		period.setUnitType(TimeUnit.class);
		period.setValidRange(0.0d, Double.POSITIVE_INFINITY);
		this.addInput(period);

		phaseAngle = new ValueInput( "PhaseAngle", "Key Inputs", 0.0d);
		phaseAngle.setUnitType( AngleUnit.class );
		this.addInput( phaseAngle);

		offset = new ValueInput( "Offset", "Key Inputs", 0.0d);
		offset.setUnitType(UserSpecifiedUnit.class);
		this.addInput( offset);
	}


	@Override
	protected void setUnitType(Class<? extends Unit> ut) {
		super.setUnitType(ut);
		amplitude.setUnitType(ut);
		offset.setUnitType(ut);
		FrameBox.reSelectEntity();  // Update the units in the Output Viewer
	}

	@Override
	public double calculateValue(double simTime) {

		// Calculate the present phase angle
		double angle = 2.0 * Math.PI * simTime / period.getValue() + phaseAngle.getValue();

		// Set the output value for the wave
		return amplitude.getValue() * this.getSignal( angle )  +  offset.getValue();
	}

	/*
	 * Calculate the current dimensionless signal for the wave.
	 */
	protected abstract double getSignal( double angle );
}
