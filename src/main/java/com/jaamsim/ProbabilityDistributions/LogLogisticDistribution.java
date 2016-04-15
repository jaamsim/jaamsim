/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2013 Ausenco Engineering Canada Inc.
 * Copyright (C) 2016 JaamSim Software Inc.
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
package com.jaamsim.ProbabilityDistributions;

import com.jaamsim.Samples.SampleConstant;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.ValueInput;
import com.jaamsim.rng.MRG1999a;
import com.jaamsim.units.DimensionlessUnit;
import com.jaamsim.units.Unit;
import com.jaamsim.units.UserSpecifiedUnit;

/**
 * Log-Logistic Distribution.
 * Adapted from A.M. Law, "Simulation Modelling and Analysis, 4th Edition", page 456.
 */
public class LogLogisticDistribution extends Distribution {

	@Keyword(description = "The scale parameter for the Log-Logistic distribution.",
	         exampleList = {"3.0"})
	private final ValueInput scaleInput;

	@Keyword(description = "The shape parameter for the Log-Logistic distribution.  A decimal value > 0.0.",
	         exampleList = {"1.0"})
	private final ValueInput shapeInput;

	private final MRG1999a rng = new MRG1999a();

	{
		minValueInput.setDefaultValue(new SampleConstant(0.0d));

		scaleInput = new ValueInput("Scale", "Key Inputs", 1.0d);
		scaleInput.setValidRange( 0.0, Double.POSITIVE_INFINITY);
		scaleInput.setUnitType( UserSpecifiedUnit.class );
		this.addInput(scaleInput);

		shapeInput = new ValueInput("Shape", "Key Inputs", 1.0d);
		shapeInput.setValidRange( 2.000001d, Double.POSITIVE_INFINITY);
		shapeInput.setUnitType( DimensionlessUnit.class );
		this.addInput(shapeInput);
	}

	public LogLogisticDistribution() {}

	@Override
	public void earlyInit() {
		super.earlyInit();
		rng.setSeedStream(getStreamNumber(), getSubstreamNumber());
	}

	@Override
	protected void setUnitType(Class<? extends Unit> ut) {
		super.setUnitType(ut);
		scaleInput.setUnitType(ut);
	}

	@Override
	protected double getSample(double simTime) {

		// Inverse transform method
		double u = rng.nextUniform();
		return scaleInput.getValue() * Math.pow( u / (1 - u), 1.0 / shapeInput.getValue() );
	}

	@Override
	protected double getMean(double simTime) {
		double theta = Math.PI / shapeInput.getValue();
		return scaleInput.getValue() * theta / Math.sin( theta );
	}

	@Override
	protected double getStandardDev(double simTime) {
		double theta = Math.PI / shapeInput.getValue();
		return scaleInput.getValue() * Math.sqrt( theta * ( 2.0/Math.sin(2.0*theta) - theta/Math.pow( Math.sin(theta), 2.0) ) );
	}
}
