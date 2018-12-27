/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2014 Ausenco Engineering Canada Inc.
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
package com.jaamsim.BasicObjects;

import com.jaamsim.Graphics.DisplayEntity;
import com.jaamsim.Samples.SampleInput;
import com.jaamsim.Samples.SampleProvider;
import com.jaamsim.input.Input;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.Output;
import com.jaamsim.input.UnitTypeInput;
import com.jaamsim.units.Unit;
import com.jaamsim.units.UserSpecifiedUnit;

public class ExpressionEntity extends DisplayEntity implements SampleProvider {

	@Keyword(description = "The unit type for the returned expression values.",
	         exampleList = {"DistanceUnit"})
	protected final UnitTypeInput unitType;

	@Keyword(description = "The expression to be evaluated.",
	         exampleList = {"'[Queue1].QueueLength + [Queue2].QueueLength'"})
	private final SampleInput sampleValue;

	{
		unitType = new UnitTypeInput("UnitType", KEY_INPUTS, UserSpecifiedUnit.class);
		unitType.setRequired(true);
		this.addInput(unitType);

		sampleValue = new SampleInput("Expression", KEY_INPUTS, null);
		sampleValue.setUnitType(UserSpecifiedUnit.class);
		sampleValue.setEntity(this);
		sampleValue.setRequired(true);
		this.addInput(sampleValue);
	}

	public ExpressionEntity() {}

	@Override
	public void updateForInput(Input<?> in) {
		super.updateForInput(in);

		if (in == unitType) {
			sampleValue.setUnitType(getUnitType());
			return;
		}
	}

	@Override
	public Class<? extends Unit> getUserUnitType() {
		return unitType.getUnitType();
	}

	@Override
	public Class<? extends Unit> getUnitType() {
		return unitType.getUnitType();
	}

	@Override
	public double getMeanValue(double simTime) {
		return 0;
	}

	@Override
	public double getMinValue() {
		return Double.NEGATIVE_INFINITY;
	}

	@Override
	public double getMaxValue() {
		return Double.POSITIVE_INFINITY;
	}

	@Override
	@Output(name = "Value",
	 description = "The present value for the expression.",
	    unitType = UserSpecifiedUnit.class,
	  reportable = true)
	public double getNextSample(double simTime) {

		if (sampleValue.getValue() == null)
			return 0.0d;

		return sampleValue.getValue().getNextSample(simTime);
	}

}
