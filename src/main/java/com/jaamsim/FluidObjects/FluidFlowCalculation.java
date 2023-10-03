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

import com.jaamsim.CalculationObjects.CalculationEntity;
import com.jaamsim.input.EntityInput;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.Output;
import com.jaamsim.units.VolumeFlowUnit;

/**
 * FluidFlowCalculation is the super-class for all flows between source and destination tanks.
 * @author Harry King
 *
 */
public abstract class FluidFlowCalculation extends CalculationEntity {

	@Keyword(description = "The Fluid being moved by the flow.")
	private final EntityInput<Fluid> fluidInput;

	@Keyword(description = "The source object for the flow.",
	         exampleList = {"Tank1"})
	protected final EntityInput<FluidComponent> sourceInput;

	@Keyword(description = "The destination object for the flow.",
	         exampleList = {"Tank1"})
	protected final EntityInput<FluidComponent> destinationInput;

	private double flowRate;  // The volumetric flow rate (m3/s) for the route.
	private double lastUpdateTime;  // The time at which the last update was performed.

	{
		fluidInput = new EntityInput<>( Fluid.class, "Fluid", KEY_INPUTS, null);
		this.addInput( fluidInput);
		fluidInput.setRequired(true);

		sourceInput = new EntityInput<>( FluidComponent.class, "Source", KEY_INPUTS, null);
		this.addInput( sourceInput);

		destinationInput = new EntityInput<>( FluidComponent.class, "Destination", KEY_INPUTS, null);
		this.addInput( destinationInput);
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
