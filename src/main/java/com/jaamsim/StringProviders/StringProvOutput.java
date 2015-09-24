/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2015 Ausenco Engineering Canada Inc.
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
package com.jaamsim.StringProviders;

import com.jaamsim.basicsim.ErrorException;
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
						unitType, out.getUnitType());
			double d = out.getValueAsDouble(simTime, 0.0d);
			return String.format(fmt, d/siFactor);
		}
		else {
			Object obj = out.getValue(simTime, out.getReturnType());
			return String.format(fmt, obj);
		}
	}

	@Override
	public String toString() {
		return chain.toString();
	}

}
