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

import com.jaamsim.Graphics.DisplayEntity;
import com.jaamsim.Samples.SampleInput;
import com.jaamsim.input.EntityInput;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.Output;
import com.jaamsim.units.AreaUnit;
import com.jaamsim.units.DimensionlessUnit;
import com.jaamsim.units.DistanceUnit;
import com.jaamsim.units.PressureUnit;
import com.jaamsim.units.SpeedUnit;

/**
 * FluidComponent is the super-class for tanks, pipes, pumps, etc. in a hydraulic flow.
 * @author Harry King
 *
 */
public class FluidComponent extends DisplayEntity {

	@Keyword(description = "The upstream component that feeds this component.",
	         exampleList = {"Comp1"})
	private final EntityInput<FluidComponent> previousInput;

	@Keyword(description = "The hydraulic diameter of the component. "
	                     + "Equal to the inside diameter of a pipe with a circular cross-section.",
	         exampleList = {"1.0 m"})
	private final SampleInput diameterInput;

	private FluidFlow fluidFlow;  // The fluid flow object that controls the flow from one component to the next.
	private double baseInletPressure;  // The static pressure at the component's inlet, ignoring the effect of flow acceleration.
	private double baseOutletPressure;  // The static pressure at the component's outlet, ignoring the effect of flow acceleration.
	private double inletPressure;  // The static pressure at the component's inlet.
	private double outletPressure;  // The static pressure at the component's outlet.
	private double velocity;  // The fluid velocity throughout the component.
	private double flowArea;  // The cross-section area of the flow.

	{
		previousInput = new EntityInput<>( FluidComponent.class, "Previous", KEY_INPUTS, null);
		this.addInput( previousInput);

		diameterInput = new SampleInput("Diameter", KEY_INPUTS, Double.POSITIVE_INFINITY);
		diameterInput.setValidRange( 0.0, Double.POSITIVE_INFINITY);
		diameterInput.setUnitType( DistanceUnit.class );
		this.addInput( diameterInput);
	}

	@Override
	public void earlyInit() {
		super.earlyInit();
		flowArea = 0.25 * Math.PI * getDiameter(0.0d) * getDiameter(0.0d);
	}

	public void updateVelocity() {
		velocity = fluidFlow.getFlowRate() / flowArea;
	}

	public void updateBaseInletPressure(double simTime) {
		FluidComponent prev = previousInput.getValue();
		if( prev != null ) {
			baseInletPressure = prev.getBaseOutletPressure() + prev.getDynamicPressure(simTime)
				- getDynamicPressure(simTime);
		}
		else {
			baseInletPressure = - getDynamicPressure(simTime);
		}
	}

	/*
	 * Calculate the inlet pressure after allowing for acceleration
	 */
	public void updateInletPressure(double simTime) {
		FluidComponent prev = previousInput.getValue();
		if( prev != null ) {
			inletPressure = prev.getOutletPressure() + prev.getDynamicPressure(simTime)
				- getDynamicPressure(simTime);
		}
		else {
			inletPressure = - getDynamicPressure(simTime);
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
		if( fluidFlow != null ) {
			return fluidFlow.getFluid();
		}
		else {
			return null;  // fluidFlow is null for FluidFixedFlow
		}
	}

	public double getLength(double simTime) {
		return 0.0;
	}

	public double getDiameter(double simTime) {
		return diameterInput.getNextSample(this, simTime);
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
	    unitType = AreaUnit.class,
	    sequence = 0)
	public double getFlowArea(double simTime) {
		return flowArea;
	}

	@Output(name = "Velocity",
	 description = "The velocity of the fluid within the component.",
	    unitType = SpeedUnit.class,
	    sequence = 1)
	public double getVelocity(double simTime) {
		return velocity;
	}

	@Output(name = "ReynoldsNumber",
	 description = "The Reynolds Number for the fluid within the component. "
	             + "Equal to (velocity)(diameter)/(kinematic viscosity).",
	    unitType = DimensionlessUnit.class,
	    sequence = 2)
	public double getReynoldsNumber(double simTime) {
		if (fluidFlow == null)
			return 0.0;
		return Math.abs(velocity) * getDiameter(simTime) / fluidFlow.getFluid().getKinematicViscosity(simTime);
	}

	@Output(name = "DynamicPressure",
	 description = "The dynamic pressure of the fluid flow. "
	             + "Equal to (0.5)(density)(velocity^2). "
	             + "Dynamic pressure is negative for negative velocities.",
	    unitType = PressureUnit.class,
	    sequence = 3)
	public double getDynamicPressure(double simTime) {
		if (fluidFlow == null)
			return 0.0;
		return 0.5 * fluidFlow.getFluid().getDensity(simTime) * velocity * Math.abs(velocity);
	}

	@Output(name = "InletPressure",
	 description = "The static pressure at the component's inlet.",
	    unitType = PressureUnit.class,
	    sequence = 4)
	public double getInletPressure(double simTime) {
		return inletPressure;
	}

	@Output(name = "OutletPressure",
	 description = "The static pressure at the component's outlet.",
	    unitType = PressureUnit.class,
	    sequence = 5)
	public double getOutletPressure(double simTime) {
		return outletPressure;
	}
}
