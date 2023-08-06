/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2013 Ausenco Engineering Canada Inc.
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

import com.jaamsim.CalculationObjects.DoubleCalculation;
import com.jaamsim.Samples.SampleInput;
import com.jaamsim.input.EntityInput;
import com.jaamsim.input.Keyword;
import com.jaamsim.units.PressureUnit;
import com.jaamsim.units.VolumeFlowUnit;

/**
 * FluidCentrifugalPump models the performance of a centrifugal pump.
 * @author Harry King
 *
 */
public class FluidCentrifugalPump extends FluidComponent {

	@Keyword(description = "Maximum volumetric flow rate that the pump can generate.",
	         exampleList = {"1.0 m3/s"})
	private final SampleInput maxFlowRateInput;

	@Keyword(description = "Maximum static pressure that the pump can generate (at zero flow rate).",
	         exampleList = {"1.0 Pa"})
	private final SampleInput maxPressureInput;

	@Keyword(description = "Maximum static pressure loss for the pump (at maximum flow rate).",
	         exampleList = {"1.0 Pa"})
	private final SampleInput maxPressureLossInput;

	@Keyword(description = "The CalculationEntity whose output sets the rotational speed of the pump. "
	                     + "The output value is ratio of present speed to maximum speed (0.0 - 1.0).",
	         exampleList = {"Calc1"})
	private final EntityInput<DoubleCalculation> speedControllerInput;

	{
		maxFlowRateInput = new SampleInput("MaxFlowRate", KEY_INPUTS, 1.0d);
		maxFlowRateInput.setValidRange( 0.0, Double.POSITIVE_INFINITY);
		maxFlowRateInput.setUnitType( VolumeFlowUnit.class );
		this.addInput( maxFlowRateInput);

		maxPressureInput = new SampleInput("MaxPressure", KEY_INPUTS, 1.0d);
		maxPressureInput.setValidRange( 0.0, Double.POSITIVE_INFINITY);
		maxPressureInput.setUnitType( PressureUnit.class );
		this.addInput( maxPressureInput);

		maxPressureLossInput = new SampleInput("MaxPressureLoss", KEY_INPUTS, 1.0d);
		maxPressureLossInput.setValidRange( 0.0, Double.POSITIVE_INFINITY);
		maxPressureLossInput.setUnitType( PressureUnit.class );
		this.addInput( maxPressureLossInput);

		speedControllerInput = new EntityInput<>( DoubleCalculation.class, "SpeedController", KEY_INPUTS, null);
		this.addInput( speedControllerInput);
		speedControllerInput.setRequired(true);
	}

	/*
	 * Return the outlet pressure for the given inlet pressure and flow acceleration.
	 */
	@Override
	public double calcOutletPressure( double inletPres, double flowAccel ) {
		double simTime = getSimTime();
		double speedFactor = speedControllerInput.getValue().getLastValue();
		speedFactor = Math.max(speedFactor, 0.0);
		speedFactor = Math.min(speedFactor, 1.0);
		double flowFactor = getFluidFlow().getFlowRate() / maxFlowRateInput.getNextSample(this, simTime);
		double pres = inletPres;
		pres += maxPressureInput.getNextSample(this, simTime) * speedFactor * speedFactor;
		pres -= maxPressureLossInput.getNextSample(this, simTime) * Math.abs(flowFactor) * flowFactor;
		return pres;
	}
}
