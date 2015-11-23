/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2015 Ausenco Engineering Canada Inc.
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
package com.jaamsim.Samples;

import com.jaamsim.basicsim.ErrorException;
import com.jaamsim.input.OutputChain;
import com.jaamsim.input.OutputHandle;
import com.jaamsim.units.Unit;

public class SampleOutput implements SampleProvider {

	private final OutputChain chain;
	private final Class<? extends Unit> unitType;

	public SampleOutput(OutputChain ch, Class<? extends Unit> ut) {
		chain = ch;
		unitType = ut;
	}

	@Override
	public Class<? extends Unit> getUnitType() {
		return unitType;
	}

	@Override
	public double getNextSample(double simTime) {
		OutputHandle out = chain.getOutputHandle(simTime);

		if (out == null)
			throw new ErrorException("Output is null.");
		if (out.getUnitType() != unitType)
			throw new ErrorException("Unit mismatch. Expected a %s, received a %s", unitType, out.getUnitType());

		return out.getValueAsDouble(simTime, 0.0);
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
	public String toString() {
		return chain.toString();
	}

}
