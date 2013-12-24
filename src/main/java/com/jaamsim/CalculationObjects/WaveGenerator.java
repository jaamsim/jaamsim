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
import com.jaamsim.input.ValueInput;
import com.jaamsim.units.AngleUnit;
import com.jaamsim.units.TimeUnit;

/**
 * Super-class for wave generators that produce either sine or square waves.
 * @author Harry King
 *
 */
public abstract class WaveGenerator extends DoubleCalculation {

	@Keyword(description = "Amplitude of the generated wave",
	         example = "Wave1 Amplitude { 2.0 }")
	private final ValueInput amplitudeInput;

	@Keyword(description = "Period of the generated wave",
	         example = "Wave1 Period { 2 s }")
	private final ValueInput periodInput;

	@Keyword(description = "Initial phase angle of the generated wave",
	         example = "Wave1 PhaseAngle { 45 deg }")
	private final ValueInput phaseAngleInput;

	@Keyword(description = "Offset added to the output of the generated wave",
	         example = "Wave1 Offset { 2.0 }")
	private final ValueInput offsetInput;

	{
		inputValue.setHidden(true);

		amplitudeInput = new ValueInput( "Amplitude", "Key Inputs", 1.0d);
		amplitudeInput.setValidRange( 0.0d, Double.POSITIVE_INFINITY);
		this.addInput( amplitudeInput, true);

		periodInput = new ValueInput("Period", "Key Inputs", 1.0d);
		periodInput.setUnitType(TimeUnit.class);
		periodInput.setValidRange(0.0d, Double.POSITIVE_INFINITY);
		this.addInput(periodInput, true);

		phaseAngleInput = new ValueInput( "PhaseAngle", "Key Inputs", 0.0d);
		phaseAngleInput.setUnitType( AngleUnit.class );
		this.addInput( phaseAngleInput, true);

		offsetInput = new ValueInput( "Offset", "Key Inputs", 0.0d);
		this.addInput( offsetInput, true);
	}

	@Override
	public void earlyInit() {
		super.earlyInit();
		this.setValue( amplitudeInput.getValue() * this.getSignal( phaseAngleInput.getValue() ) );
	}

	@Override
	public void update(double simTime) {

		// Calculate the present phase angle
		double angle = 2.0 * Math.PI * simTime / periodInput.getValue() + phaseAngleInput.getValue();

		// Set the output value for the wave
		this.setValue( amplitudeInput.getValue() * this.getSignal( angle )  +  offsetInput.getValue() );
		return;
	}

	/*
	 * Calculate the current dimensionless signal for the wave.
	 */
	protected abstract double getSignal( double angle );
}
