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
package com.jaamsim.Samples;

import com.jaamsim.units.Unit;
import com.sandwell.JavaSimulation.DoubleVector;
import com.sandwell.JavaSimulation.Entity;
import com.sandwell.JavaSimulation.ErrorException;
import com.sandwell.JavaSimulation.Input;
import com.sandwell.JavaSimulation.InputErrorException;
import com.sandwell.JavaSimulation.StringVector;

public class SampleInput extends Input<SampleProvider> {
	private Class<? extends Unit> unitType;

	public SampleInput(String key, String cat, SampleProvider def) {
		super(key, cat, def);

		if (def != null)
			unitType = def.getUnitType();
		else
			throw new ErrorException("A default provider must be given to define unitType.");
	}

	@Override
	public void parse(StringVector input)
	throws InputErrorException {
		Input.assertCountRange(input, 1, 2);

		// Try to parse as a constant value
		try {
			DoubleVector tmp = Input.parseDoubles(input, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, unitType);
			value = new ConstantDouble(unitType, tmp.get(0));
			this.updateEditingFlags();
			return;
		}
		catch (InputErrorException e) {}

		// If not a constant, try parsing a SampleProvider
		Entity ent = Input.parseEntity(input.get(0), Entity.class);
		SampleProvider s = Input.castImplements(ent, SampleProvider.class);
		Input.assertUnitsMatch(unitType, s.getUnitType());
		value = s;
		this.updateEditingFlags();
	}
}
