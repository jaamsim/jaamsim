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

import com.jaamsim.input.Output;
import com.jaamsim.units.AreaUnit;
import com.jaamsim.units.SpeedUnit;
import com.jaamsim.units.PressureUnit;
import com.sandwell.JavaSimulation.DoubleInput;
import com.sandwell.JavaSimulation.EntityInput;
import com.sandwell.JavaSimulation.Keyword;
import com.sandwell.JavaSimulation3D.DisplayEntity;

/**
 * FluidComponent is the super-class for tanks, pipes, pumps, etc. in a hydraulic flow.
 * @author Harry King
 *
 */
public class FluidComponent extends DisplayEntity {

	@Keyword(desc = "The upstream component that feeds this component.",
	      example = "Component1 Previous { Comp1 }")
	private final EntityInput<FluidComponent> previousInput;

	@Keyword(desc = "The hydraulic diameter of the component.  " +
	                "Equal to the inside diameter of a pipe with a circular cross-section.",
	      example = "Comp1 Diameter { 1.0 m }")
	private final DoubleInput diameterInput;

	private FluidFlow fluidFlow;  // The fluid flow object that controls the flow from one component to the next.
	private double baseInletPressure;  // The static pressure at the component's inlet, ignoring the effect of flow acceleration.
	private double baseOutletPressure;  // The static pressure at the component's outlet, ignoring the effect of flow acceleration.
	private double inletPressure;  // The static pressure at the component's inlet.
	private double outletPressure;  // The static pressure at the component's outlet.
	private double velocity;  // The fluid velocity throughout the component.
	private double flowArea;  // The cross-section area of the flow.

	{
		previousInput = new EntityInput<FluidComponent>( FluidComponent.class, "Previous", "Key Inputs", null);
		this.addInput( previousInput, true);

		diameterInput = new DoubleInput( "Diameter", "Key Inputs", Double.POSITIVE_INFINITY);
		diameterInput.setValidRange( 0.0, Double.POSITIVE_INFINITY);
		diameterInput.setUnits( "m");
		this.addInput( diameterInput, true);
	}

	@Override
	public void earlyInit() {
		super.earlyInit();
		flowArea = 0.25 * Math.PI * diameterInput.getValue() * diameterInput.getValue();
	}

	public void updateVelocity() {
		velocity = fluidFlow.getFlowRate() / flowArea;
	}

	public void updateBaseInletPressure() {
		FluidComponent prev = previousInput.getValue();
		if( prev != null ) {
			baseInletPressure = prev.getBaseOutletPressure() + prev.getDynamicPressure()
				- this.getDynamicPressure();
		}
		else {
			baseInletPressure = - this.getDynamicPressure();
		}
	}

	/*
	 * Calculate the inlet pressure after allowing for acceleration
	 */
	public void updateInletPressure() {
		FluidComponent prev = previousInput.getValue();
		if( prev != null ) {
			inletPressure = prev.getOutletPressure() + prev.getDynamicPressure()
				- this.getDynamicPressure();
		}
		else {
			inletPressure = - this.getDynamicPressure();
		}
	}

	public void updateBaseOutletPressure() {
		baseOutletPressure = this.calcOutletPressure( baseInletPressure, 0.0 );
	}

	/*
	 * Update the outlet pressure after allowing for acceleration
	 */
	public void updateOutletPressure( double flowAccel ) {
		outletPressure = this.calcOutletPressure( inletPressure, flowAccel );
	}

	/*
	 * Return the outlet pressure for the given inlet pressure and flow acceleration.
	 */
	public double calcOutletPressure( double inletPres, double flowAccel ) {
		return inletPres;
	}

	/*
	 * Return the dynamic pressure for a flow.
	 * (Dynamic pressure is negative for negative velocities.)
	 */
	public double getDynamicPressure() {
		return 0.5 * fluidFlow.getFluid().getDensity() * velocity * Math.abs(velocity);
	}

	public double getReynoldsNumber() {
		return Math.abs(velocity) * diameterInput.getValue() / fluidFlow.getFluid().getKinematicViscosity();
	}

	public void setFluidFlow( FluidFlow flow ) {
		fluidFlow = flow;
	}

	public FluidComponent getPrevious() {
		return previousInput.getValue();
	}

	public FluidFlow getFluidFlow() {
		return fluidFlow;
	}

	public Fluid getFluid() {
		return fluidFlow.getFluid();
	}

	public double getLength() {
		return 0.0;
	}

	public double getDiameter() {
		return diameterInput.getValue();
	}

	public double getFlowArea() {
		return flowArea;
	}

	public void addVolume( double v ) {}

	public double getBaseInletPressure() {
		return baseInletPressure;
	}

	public double getBaseOutletPressure() {
		return baseOutletPressure;
	}

	public void setBaseOutletPressure( double x ) {
		baseOutletPressure = x;
	}

	public double getInletPressure() {
		return inletPressure;
	}

	public double getOutletPressure() {
		return outletPressure;
	}

	public void setOutletPressure( double x ) {
		outletPressure = x;
	}

	public double getVelocity() {
		return velocity;
	}

	public double getTargetInletPressure() {
		return 0.0;
	}

	public double getFluidVolume() {
		return 0.0;
	}

	@Output(name = "FlowArea",
	 description = "The cross-sectional area of the component.",
	        unit = AreaUnit.class)
	public double getFlowArea( double simTime ) {
		return flowArea;
	}

	@Output(name = "Velocity",
	 description = "The velocity of the fluid within the component.",
	        unit = SpeedUnit.class)
	public double getVelocity( double simTime ) {
		return velocity;
	}

	@Output(name = "ReynoldsNumber",
	 description = "The Reynolds Number for the fluid within the component.  Equal to (velocity)(diameter)/(kinematic viscosity).")
	public double getReynoldsNumber( double simTime ) {
		return this.getReynoldsNumber();
	}

	@Output(name = "DynamicPressure",
	 description = "The dynamic pressure of the fluid flow.  Equal to (0.5)(density)(velocity^2).",
	        unit = PressureUnit.class)
	public double getDynamicPressure( double simTime ) {
		return this.getDynamicPressure();
	}

	@Output(name = "InletPressure",
	 description = "The static pressure at the component's inlet.",
	        unit = PressureUnit.class)
	public double getInletPressure( double simTime ) {
		return inletPressure;
	}

	@Output(name = "OutletPressure",
	 description = "The static pressure at the component's outlet.",
	        unit = PressureUnit.class)
	public double getOutletPressure( double simTime ) {
		return outletPressure;
	}
}
