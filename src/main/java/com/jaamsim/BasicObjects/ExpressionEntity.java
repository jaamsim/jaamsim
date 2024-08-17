/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2014 Ausenco Engineering Canada Inc.
 * Copyright (C) 2022-2024 JaamSim Software Inc.
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
import com.jaamsim.basicsim.Entity;
import com.jaamsim.input.Input;
import com.jaamsim.input.InputCallback;
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
		unitType.setCallback(unitTypeInputCallback);
		this.addInput(unitType);

		sampleValue = new SampleInput("Expression", KEY_INPUTS, Double.NaN);
		sampleValue.setUnitType(UserSpecifiedUnit.class);
		sampleValue.setRequired(true);
		this.addInput(sampleValue);
	}

	public ExpressionEntity() {}

	static final InputCallback unitTypeInputCallback = new InputCallback() {
		@Override
		public void callback(Entity ent, Input<?> inp) {
			((ExpressionEntity)ent).updateSampleUnitType();
		}
	};

	void updateSampleUnitType() {
		sampleValue.setUnitType(getUnitType());
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

	@Output(name = "Value",
	 description = "The present value for the expression.",
	    unitType = UserSpecifiedUnit.class,
	  reportable = true)
	public final double getNextSample(double simTime) {
		return getNextSample(this, simTime);
	}

	@Override
	public double getNextSample(Entity thisEnt, double simTime) {
		return sampleValue.getNextSample(this, simTime);
	}

}
