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
package com.jaamsim.StringProviders;

import com.jaamsim.basicsim.ErrorException;
import com.jaamsim.basicsim.ObjectType;
import com.jaamsim.input.OutputChain;
import com.jaamsim.input.OutputHandle;
import com.jaamsim.units.Unit;

public class StringProvOutput implements StringProvider {

	private final OutputChain chain;
	private final Class<? extends Unit> unitType;

	public StringProvOutput(OutputChain ch, Class<? extends Unit> ut) {
		chain = ch;
		unitType = ut;
	}

	@Override
	public String getNextString(double simTime, String fmt, double siFactor) {

		OutputHandle out = chain.getOutputHandle(simTime);

		if (out == null)
			throw new ErrorException("Expression cannot be evaluated: %s.", chain.toString());

		if (out.isNumericValue()) {
			if (out.getUnitType() != unitType && unitType != null)
				throw new ErrorException("Unit mismatch. Expected a %s, received a %s",
						ObjectType.getObjectTypeForClass(unitType),
						ObjectType.getObjectTypeForClass(out.getUnitType()));
			double d = out.getValueAsDouble(simTime, 0.0d);
			return String.format(fmt, d/siFactor);
		}
		else {
			Object obj = out.getValue(simTime, Object.class);
			return String.format(fmt, obj);
		}
	}

	@Override
	public String toString() {
		return chain.toString();
	}

}
