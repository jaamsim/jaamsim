/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2015 Ausenco Engineering Canada Inc.
 * Copyright (C) 2026 JaamSim Software Inc.
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

import com.jaamsim.basicsim.Entity;
import com.jaamsim.datatypes.DoubleVector;
import com.jaamsim.input.Input;
import com.jaamsim.input.InputErrorException;
import com.jaamsim.input.KeywordIndex;
import com.jaamsim.units.DimensionlessUnit;
import com.jaamsim.units.Unit;

public class CumulativeProbInput extends Input<DoubleVector>{
	public CumulativeProbInput(String key, String cat, DoubleVector def) {
		super(key, cat, def);
	}

	@Override
	public void parse(Entity thisEnt, KeywordIndex kw)
	throws InputErrorException {
		DoubleVector temp = Input.parseDoubles(thisEnt.getJaamSimModel(), kw, 0.0d, 1.0d, DimensionlessUnit.class);
		if (temp.get(0) != 0.0d)
			throw new InputErrorException("The first value of a cumulative probability list must be 0.0, got %f", temp.get(0));

		if (temp.get(temp.size() - 1) != 1.0d)
			throw new InputErrorException("The last value of a cumulative probability list must be 1.0, got %f", temp.get(temp.size() - 1));

		for (int i = 1; i < temp.size(); i++) {
			if (temp.get(i - 1) > temp.get(i))
				throw new InputErrorException("The values of a cumulative probability list must be strictly increasing");
		}

		value = temp;
	}

	@Override
	public Class<?> getReturnType() {
		return DoubleVector.class;
	}

	@Override
	public Class<? extends Unit> getUnitType() {
		return DimensionlessUnit.class;
	}

}
