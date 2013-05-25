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
import com.sandwell.JavaSimulation.Entity;
import com.sandwell.JavaSimulation.Input;
import com.sandwell.JavaSimulation.InputErrorException;
import com.sandwell.JavaSimulation.StringVector;

public class SampleInput extends Input<SampleProvider> {
	public SampleInput(String key, String cat, SampleProvider def) {
		super(key, cat, def);
	}

	@Override
	public void parse(StringVector input)
	throws InputErrorException {
		Input.assertCountRange(input, 1, 2);

		// Only one argument means we must have passed an Entity
		if (input.size() == 1) {
			Entity ent = Input.parseEntity(input.get(0), Entity.class);
			SampleProvider s = Input.castImplements(ent, SampleProvider.class);
			// TODO: verify the unitType
			value = s;
			this.updateEditingFlags();
			return;
		}

		// In case we pass a constant and a Unit
		if (input.size() == 2) {
			double val = Input.parseDouble(input.get(0));

			// TODO: verify the unitType
			Unit u = Input.parseUnits(input.get(1));
			val *= u.getConversionFactorToSI();
			value = new ConstantDouble(u.getClass(), val);
			this.updateEditingFlags();
			return;
		}
	}
}
