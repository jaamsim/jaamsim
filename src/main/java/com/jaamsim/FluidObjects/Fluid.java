/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2013 Ausenco Engineering Canada Inc.
 * Copyright (C) 2023 JaamSim Software Inc.
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
package com.jaamsim.FluidObjects;

import com.jaamsim.ColourProviders.ColourProvInput;
import com.jaamsim.Graphics.DisplayEntity;
import com.jaamsim.Samples.SampleInput;
import com.jaamsim.input.ColourInput;
import com.jaamsim.input.Keyword;
import com.jaamsim.math.Color4d;
import com.jaamsim.units.AccelerationUnit;
import com.jaamsim.units.DensityUnit;
import com.jaamsim.units.ViscosityUnit;

/**
 * Fluid defines the properties of the fluid being used in a hydraulic calculation.
 * @author Harry King
 *
 */
public class Fluid extends DisplayEntity {

	@Keyword(description = "The density of the fluid (default = water).",
	         exampleList = {"1000 kg/m3"})
	private final SampleInput densityInput;

	@Keyword(description = "The dynamic viscosity of the fluid (default = water).",
	         exampleList = {"0.001002 Pa-s"})
	private final SampleInput viscosityInput;

	@Keyword(description = "The colour used to represent the fluid.",
	         exampleList = {"red"})
	private final ColourProvInput colourInput;

	@Keyword(description = "The acceleration of gravity to be used in the fluid flow "
	                     + "calculations.",
	         exampleList = {"9.81 m/s2"})
	private final SampleInput gravityInput;

	{
		densityInput = new SampleInput("Density", KEY_INPUTS, 1000.0d);
		densityInput.setValidRange( 0.0, Double.POSITIVE_INFINITY);
		densityInput.setUnitType( DensityUnit.class );
		this.addInput( densityInput);

		viscosityInput = new SampleInput("Viscosity", KEY_INPUTS, 0.001002d);
		viscosityInput.setValidRange( 0.0, Double.POSITIVE_INFINITY);
		viscosityInput.setUnitType( ViscosityUnit.class );
		this.addInput( viscosityInput);

		colourInput = new ColourProvInput("Colour", KEY_INPUTS, ColourInput.RED);
		this.addInput(colourInput);
		this.addSynonym(colourInput, "Color");

		gravityInput = new SampleInput("Gravity", KEY_INPUTS, 9.81d);
		gravityInput.setValidRange( 0.0, Double.POSITIVE_INFINITY);
		gravityInput.setUnitType( AccelerationUnit.class );
		this.addInput( gravityInput);
	}

	public double getDensity(double simTime) {
		return densityInput.getNextSample(this, simTime);
	}

	public double getViscosity(double simTime) {
		return viscosityInput.getNextSample(this, simTime);
	}

	public Color4d getColour(double simTime) {
		return colourInput.getNextColour(this, simTime);
	}

	public double getGravity(double simTime) {
		return gravityInput.getNextSample(this, simTime);
	}

	public double getDensityxGravity(double simTime) {
		return getDensity(simTime) * getGravity(simTime);
	}

	public double getKinematicViscosity(double simTime) {
		return getViscosity(simTime) / getDensity(simTime);
	}
}
