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

import com.jaamsim.basicsim.ErrorException;
import com.jaamsim.input.InputErrorException;
import com.jaamsim.input.Output;

/**
 * FluidFlow tracks the flow rate between a source and a destination.
 * @author Harry King
 *
 */
public class FluidFlow extends FluidFlowCalculation {

	private double flowAcceleration;  // The rate of change of the volumetric flow rate with respect to time (m3/s2).

	private ArrayList<FluidComponent> routeList;  // A list of the hydraulic components in the flow, from source to destination.
	private double totalFlowInertia;  // The sum of Density x Length / FlowArea for the hydraulic components in the route.
	private double destinationBaseInletPressure;  // The base pressure at the destination's inlet.
	private double destinationTargetInletPressure;  // The desired inlet pressure at the destination's inlet.

	public FluidFlow() {
		routeList = new ArrayList<FluidComponent>();
	}

	@Override
	public void validate() {
		super.validate();

		// Confirm that the source has been specified
		if( this.getSource() == null ) {
			throw new InputErrorException( "The keyword Source must be set." );
		}

		// Confirm that the destination has been specified
		if( this.getDestination() == null ) {
			throw new InputErrorException( "The keyword Destination must be set." );
		}
	}

	@Override
	public void earlyInit() {
		super.earlyInit();

		flowAcceleration = 0.0;

		// Construct the list of hydraulic components in the flow path
		routeList.clear();
		routeList.add( this.getDestination() );
		FluidComponent prev = routeList.get(0).getPrevious();
		while( prev != null ) {
			routeList.add(0, prev);
			prev = prev.getPrevious();
		}

		// Confirm that the first component is the source
		if( routeList.get(0) != this.getSource() ) {
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
	protected void calcFlowRate( FluidComponent source, FluidComponent destination, double dt ) {

		// Update the flow rate
		this.setFlowRate( this.getFlowRate() + flowAcceleration * dt );

		// Update the flow velocity and base pressures in each component of the flow route
		// (base pressure ignores the affect of acceleration)
		for( FluidComponent each : routeList ) {
			each.updateVelocity();
			each.updateBaseInletPressure();
			each.updateBaseOutletPressure();
		}

		// Update the flow acceleration
		destinationBaseInletPressure = destination.getBaseInletPressure();
		destinationTargetInletPressure = destination.getTargetInletPressure();
		flowAcceleration = ( destinationBaseInletPressure
				- destinationTargetInletPressure ) / totalFlowInertia;

		// Update the pressure in each component of the flow route after allowing for acceleration
		for( FluidComponent each : routeList ) {
			each.updateInletPressure();
			each.updateOutletPressure( flowAcceleration );
		}

		// Confirm that the pressure is now balanced
		double diff = destination.getInletPressure() /
				destination.getTargetInletPressure() - 1.0;
		if( Math.abs( diff ) > 1.0e-4 ) {
			throw new ErrorException( "Pressure did not balance correctly.  Difference = " + diff );
		}
	}

	@Output(name = "FlowAcceleration",
	 description = "The time derivative of the volumetric flow rate.")
	public double getFlowAcceleration( double simTime ) {
		return flowAcceleration;
	}

	@Output(name = "FlowInertia",
	 description = "The sum of (density)(length)/(flow area) for the hydraulic components in the route.")
	public double getFlowInertia( double simTime ) {
		return totalFlowInertia;
	}
}
