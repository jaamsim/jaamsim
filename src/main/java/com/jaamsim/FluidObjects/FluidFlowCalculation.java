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

import com.jaamsim.CalculationObjects.CalculationEntity;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.Output;
import com.jaamsim.units.VolumeFlowUnit;
import com.sandwell.JavaSimulation.EntityInput;
import com.sandwell.JavaSimulation.InputErrorException;

/**
 * FluidFlowCalculation is the super-class for all flows between source and destination tanks.
 * @author Harry King
 *
 */
public abstract class FluidFlowCalculation extends CalculationEntity {

	@Keyword(description = "The Fluid being moved by the flow.",
	      example = "FluidFlow1 Fluid { Fluid1 }")
	private final EntityInput<Fluid> fluidInput;

	@Keyword(description = "The source for the flow.",
	      example = "FluidFlow1 Source { Tank1 }")
	private final EntityInput<FluidComponent> sourceInput;

	@Keyword(description = "The destination for the flow.",
	      example = "FluidFlow1 Destination { Tank1 }")
	private final EntityInput<FluidComponent> destinationInput;

	private double flowRate;  // The volumetric flow rate (m3/s) for the route.
	private double lastUpdateTime;  // The time at which the last update was performed.

	{
		fluidInput = new EntityInput<Fluid>( Fluid.class, "Fluid", "Key Inputs", null);
		this.addInput( fluidInput);

		sourceInput = new EntityInput<FluidComponent>( FluidComponent.class, "Source", "Key Inputs", null);
		this.addInput( sourceInput);

		destinationInput = new EntityInput<FluidComponent>( FluidComponent.class, "Destination", "Key Inputs", null);
		this.addInput( destinationInput);
	}

	@Override
	public void validate() {
		super.validate();

		// Confirm that the fluid has been specified
		if( fluidInput.getValue() == null ) {
			throw new InputErrorException( "The keyword Fluid must be set." );
		}
	}

	@Override
	public void earlyInit() {
		super.earlyInit();
		lastUpdateTime = 0.0;
	}

	@Override
	public void update(double simTime) {

		double dt = simTime - lastUpdateTime;
		lastUpdateTime = simTime;

		// Update the volume stored at the source and destination
		FluidComponent source = sourceInput.getValue();
		FluidComponent destination = destinationInput.getValue();
		double dV = flowRate * dt;
		if( dV > 0.0 && source != null ) {
			dV = Math.min( dV, source.getFluidVolume() );
		}
		else if( dV < 0.0 && destination != null ) {
			dV = - Math.min( - dV, destination.getFluidVolume() );
		}
		if( source != null ) { source.addVolume( -dV ); }
		if( destination != null ) { destination.addVolume( dV ); }

		// Set the new flow rate
		this.calcFlowRate( source, destination, dt);
	}

	protected abstract void calcFlowRate( FluidComponent source, FluidComponent destination, double dt );

	protected void setFlowRate( double rate) {
		flowRate = rate;
	}

	public double getFlowRate() {
		return flowRate;
	}

	protected FluidComponent getSource() {
		return sourceInput.getValue();
	}

	protected FluidComponent getDestination() {
		return destinationInput.getValue();
	}

	public Fluid getFluid() {
		return fluidInput.getValue();
	}

	@Output(name = "FlowRate",
	 description = "The volumetric flow rate for the system.",
	    unitType = VolumeFlowUnit.class)
	public Double getFlowRate( double simTime ) {
		return flowRate;
	}
}
