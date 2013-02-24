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
package com.jaamsim.FluidObjects;

import com.sandwell.JavaSimulation.DoubleInput;
import com.sandwell.JavaSimulation.Keyword;

/**
 * FluidTank is a storage tank that contains a fluid.
 * @author Harry King
 *
 */
public class FluidTank extends FluidComponent {

	@Keyword(desc = "The volume of fluid in the tank at the start of the simulation.",
	         example = "Tank1 InitialVolume { 1.0 m3 }")
	private final DoubleInput initialVolumeInput;

	@Keyword(desc = "The ambient pressure in the tank.",
	         example = "Tank1 AmbientPressure { 1.0 Pa }")
	private final DoubleInput ambientPressureInput;

	@Keyword(desc = "The height of the inlet to the tank above its outlet.",
	         example = "Tank1 InletHeight { 1.0 m }")
	private final DoubleInput inletHeightInput;

	private double fluidVolume;  // The present volume of the fluid in the tank.
	private double fluidLevel;  // The height of the fluid in the tank.

	{
		initialVolumeInput = new DoubleInput( "InitialVolume", "Key Inputs", 0.0d);
		initialVolumeInput.setValidRange( 0.0, Double.POSITIVE_INFINITY);
		initialVolumeInput.setUnits( "m3");
		this.addInput( initialVolumeInput, true);

		ambientPressureInput = new DoubleInput( "AmbientPressure", "Key Inputs", 0.0d);
		ambientPressureInput.setUnits( "Pa");
		this.addInput( ambientPressureInput, true);

		inletHeightInput = new DoubleInput( "InletHeight", "Key Inputs", 0.0d);
		inletHeightInput.setValidRange( 0.0, Double.POSITIVE_INFINITY);
		inletHeightInput.setUnits( "m");
		this.addInput( inletHeightInput, true);
	}

	@Override
	public void earlyInit() {
		super.earlyInit();
		fluidVolume = initialVolumeInput.getValue();
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

	@Override
	public double getFluidVolume() {
		return fluidVolume;
	}

	public double getFluidLevel() {
		return fluidLevel;
	}
}
