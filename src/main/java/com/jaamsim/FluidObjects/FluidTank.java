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

import com.jaamsim.DisplayModels.ShapeModel;
import com.jaamsim.basicsim.Entity;
import com.jaamsim.input.ColourInput;
import com.jaamsim.input.Input;
import com.jaamsim.input.InputCallback;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.Output;
import com.jaamsim.input.ValueInput;
import com.jaamsim.units.DistanceUnit;
import com.jaamsim.units.PressureUnit;
import com.jaamsim.units.VolumeUnit;

/**
 * FluidTank is a storage tank that contains a fluid.
 * @author Harry King
 *
 */
public class FluidTank extends FluidComponent {

	@Keyword(description = "The total volume of fluid that can be stored in the tank.",
	         exampleList = {"1.0 m3"})
	private final ValueInput capacityInput;

	@Keyword(description = "The volume of fluid in the tank at the start of the simulation run.",
	         exampleList = {"1.0 m3"})
	private final ValueInput initialVolumeInput;

	@Keyword(description = "The atmospheric pressure acting on the surface of the fluid in the "
	                     + "tank.",
	         exampleList = {"1.0 Pa"})
	private final ValueInput ambientPressureInput;

	@Keyword(description = "The height of the flow feeding the tank. Measured relative to the "
	                     + "bottom of the tank.",
	         exampleList = {"1.0 m"})
	private final ValueInput inletHeightInput;

	private double fluidVolume;  // The present volume of the fluid in the tank.
	private double fluidLevel;  // The height of the fluid in the tank.

	{
		capacityInput = new ValueInput( "Capacity", KEY_INPUTS, 1.0d);
		capacityInput.setValidRange( 0.0, Double.POSITIVE_INFINITY);
		capacityInput.setUnitType( VolumeUnit.class );
		this.addInput( capacityInput);

		initialVolumeInput = new ValueInput( "InitialVolume", KEY_INPUTS, 0.0d);
		initialVolumeInput.setValidRange( 0.0, Double.POSITIVE_INFINITY);
		initialVolumeInput.setUnitType( VolumeUnit.class );
		initialVolumeInput.setCallback(updateInitialVolumeInputCallback);
		this.addInput( initialVolumeInput);

		ambientPressureInput = new ValueInput( "AmbientPressure", KEY_INPUTS, 0.0d);
		ambientPressureInput.setUnitType( PressureUnit.class );
		this.addInput( ambientPressureInput);

		inletHeightInput = new ValueInput( "InletHeight", KEY_INPUTS, 0.0d);
		inletHeightInput.setValidRange( 0.0, Double.POSITIVE_INFINITY);
		inletHeightInput.setUnitType( DistanceUnit.class );
		this.addInput( inletHeightInput);
	}

	static final InputCallback updateInitialVolumeInputCallback = new InputCallback() {
		@Override
		public void callback(Entity ent, Input<?> inp) {
			double val = ((ValueInput) inp).getValue();
			((FluidTank) ent).setFluidVolume(val);
		}
	};

	@Override
	public void earlyInit() {
		super.earlyInit();
		setFluidVolume(initialVolumeInput.getValue());
	}

	@Override
	public void addVolume( double v ) {
		fluidVolume += v;
		fluidLevel = fluidVolume / this.getFlowArea();
	}

	@Override
	public double calcOutletPressure( double inletPres, double flowAccel ) {
		return this.getFluidPressure(0.0);
	}

	@Override
	public double getTargetInletPressure() {
		return this.getFluidPressure( inletHeightInput.getValue() );
	}

	/*
	 * Return the pressure in the tank at the given height above the outlet.
	 */
	private double getFluidPressure( double h ) {
		double pres = ambientPressureInput.getValue();
		if( h < fluidLevel ) {
			pres += ( fluidLevel - h) * this.getFluid().getDensityxGravity();
		}
		return pres;
	}

	public void setFluidVolume(double val) {
		fluidVolume = val;
	}

	@Override
	public double getFluidVolume() {
		return fluidVolume;
	}

	public double getFluidLevel() {
		return fluidLevel;
	}

	@Override
	public void updateGraphics(double simTime) {
		super.updateGraphics(simTime);

		double ratio = Math.min( 1.0, fluidVolume / capacityInput.getValue() );

		setTagSize(ShapeModel.TAG_CONTENTS, ratio);

		if( this.getFluid() != null )
			setTagColour(ShapeModel.TAG_CONTENTS, this.getFluid().getColour());
		else
			setTagColour(ShapeModel.TAG_CONTENTS, ColourInput.getColorWithName("black"));
	}

	@Output(name = "FluidVolume",
	 description = "The volume of the fluid stored in the tank.",
	    unitType = VolumeUnit.class)
	public double getFluidVolume( double simTime ) {
		return fluidVolume;
	}

	@Output(name = "FluidLevel",
	 description = "The height of the fluid from the bottom of the tank.",
	    unitType = DistanceUnit.class)
	public double getFluidLevel( double simTime ) {
		return fluidLevel;
	}
}
