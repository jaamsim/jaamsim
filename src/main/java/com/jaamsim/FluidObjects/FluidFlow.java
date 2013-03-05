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

import java.util.ArrayList;
import com.jaamsim.CalculationObjects.CalculationEntity;
import com.jaamsim.input.Output;
import com.sandwell.JavaSimulation.EntityInput;
import com.sandwell.JavaSimulation.InputErrorException;
import com.sandwell.JavaSimulation.ErrorException;
import com.sandwell.JavaSimulation.Keyword;

/**
 * FluidFlow tracks the flow rate between a source and a destination.
 * @author Harry King
 *
 */
public class FluidFlow extends CalculationEntity {

	@Keyword(desc = "The Fluid being moved by the flow.",
	         example = "FluidFlow1 Fluid { Fluid1 }")
	private final EntityInput<Fluid> fluidInput;

	@Keyword(desc = "The source for the flow.",
	         example = "FluidFlow1 Source { Tank1 }")
	private final EntityInput<FluidComponent> sourceInput;

	@Keyword(desc = "The destination for the flow.",
	         example = "FluidFlow1 Destination { Tank1 }")
	private final EntityInput<FluidComponent> destinationInput;

	private double flowRate;  // The volumetric flow rate (m3/s) for the route.
	private double flowAcceleration;  // The rate of change of the volumetric flow rate with respect to time (m3/s2).
	private double lastUpdateTime;  // The time at which the last update was performed.

	private ArrayList<FluidComponent> routeList;  // A list of the hydraulic components in the flow, from source to destination.
	private double totalFlowInertia;  // The sum of Density x Length / FlowArea for the hydraulic components in the route.
	private double destinationBaseInletPressure;  // The base pressure at the destination's inlet.
	private double destinationTargetInletPressure;  // The desired inlet pressure at the destination's inlet.

	{
		fluidInput = new EntityInput<Fluid>( Fluid.class, "Fluid", "Key Inputs", null);
		this.addInput( fluidInput, true);

		sourceInput = new EntityInput<FluidComponent>( FluidComponent.class, "Source", "Key Inputs", null);
		this.addInput( sourceInput, true);

		destinationInput = new EntityInput<FluidComponent>( FluidComponent.class, "Destination", "Key Inputs", null);
		this.addInput( destinationInput, true);
	}

	public FluidFlow() {
		routeList = new ArrayList<FluidComponent>();
	}

	@Override
	public void validate() {
		super.validate();

		// Confirm that the fluid has been specified
		if( fluidInput.getValue() == null ) {
			throw new InputErrorException( "The keyword Fluid must be set." );
		}

		// Confirm that the source has been specified
		if( sourceInput.getValue() == null ) {
			throw new InputErrorException( "The keyword Source must be set." );
		}

		// Confirm that the destination has been specified
		if( destinationInput.getValue() == null ) {
			throw new InputErrorException( "The keyword Destination must be set." );
		}
	}

	@Override
	public void earlyInit() {
		super.earlyInit();

		flowRate = 0.0;
		flowAcceleration = 0.0;
		lastUpdateTime = 0.0;

		// Construct the list of hydraulic components in the flow path
		routeList.clear();
		routeList.add( destinationInput.getValue() );
		FluidComponent prev = routeList.get(0).getPrevious();
		while( prev != null ) {
			routeList.add(0, prev);
			prev = prev.getPrevious();
		}

		// Confirm that the first component is the source
		if( routeList.get(0) != sourceInput.getValue() ) {
			throw new InputErrorException( "The source of the route is not connected to the destination by the 'Previous' keyword inputs for the individual components." );
		}

		// Set the Flow object for each component in the route
		for( FluidComponent each : routeList ) {
			each.setFluidFlow( this );
		}

		// Calculate the total flow inertia of the flow path
		totalFlowInertia = 0.0;
		for( FluidComponent each : routeList ) {
			each.earlyInit();  // Needs to be called to set flowArea
			totalFlowInertia += each.getLength() / each.getFlowArea();
		}
		totalFlowInertia *= this.getFluid().getDensity();
	}

	@Override
	public void update() {

		// Calculate the elapsed time
		double t = 3600.0 * this.getCurrentTime();  // convert from hours to seconds
		double dt = t - lastUpdateTime;
		lastUpdateTime = t;

		// Update the volume stored at the source and destination
		double dV = flowRate * dt;
		if( dV > 0.0 ) {
			dV = Math.min( dV, sourceInput.getValue().getFluidVolume() );
		}
		else {
			dV = - Math.min( - dV, destinationInput.getValue().getFluidVolume() );
		}
		sourceInput.getValue().addVolume( -dV );
		destinationInput.getValue().addVolume( dV );

		// Update the flow rate
		flowRate += flowAcceleration * dt;

		// Update the flow velocity and base pressures in each component of the flow route
		// (base pressure ignores the affect of acceleration)
		for( FluidComponent each : routeList ) {
			each.updateVelocity();
			each.updateBaseInletPressure();
			each.updateBaseOutletPressure();
		}

		// Update the flow acceleration
		destinationBaseInletPressure = destinationInput.getValue().getBaseInletPressure();
		destinationTargetInletPressure = destinationInput.getValue().getTargetInletPressure();
		flowAcceleration = ( destinationBaseInletPressure
				- destinationTargetInletPressure ) / totalFlowInertia;

		// Update the pressure in each component of the flow route after allowing for acceleration
		for( FluidComponent each : routeList ) {
			each.updateInletPressure();
			each.updateOutletPressure( flowAcceleration );
		}

		// Confirm that the pressure is now balanced
		double diff = destinationInput.getValue().getInletPressure() -
				destinationInput.getValue().getTargetInletPressure();
		if( Math.abs( diff ) > 1.0e-8 ) {
			throw new ErrorException( "Pressure did not balance correctly.  Difference = " + diff );
		}
	}

	public double getFlowRate() {
		return flowRate;
	}

	public Fluid getFluid() {
		return fluidInput.getValue();
	}

	@Output( name="FlowRate",
			 description="The volumetric flow rate for the system.")
	public double getFlowRate( double simTime ) {
		return flowRate;
	}

	@Output( name="FlowAcceleration",
			 description="The time derivative of the volumetric flow rate.")
	public double getFlowAcceleration( double simTime ) {
		return flowAcceleration;
	}

	@Output( name="FlowInertia",
			 description="The sum of (density)(length)/(flow area) for the hydraulic components in the route.")
	public double getFlowInertia( double simTime ) {
		return totalFlowInertia;
	}
}
