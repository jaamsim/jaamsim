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
package com.jaamsim.input;

import com.sandwell.JavaSimulation.Entity;
import com.sandwell.JavaSimulation.Input;
import com.sandwell.JavaSimulation.InputErrorException;
import com.sandwell.JavaSimulation.StringVector;

public class OutputInput<T> extends Input<String> {

	private Class<T> klass;
	private Entity ent;
	private String outputName;

	public OutputInput(Class<T> klass, String key, String cat, String def) {
		super(key, cat, def);
		this.klass = klass;
	}

	@Override
	public void parse(StringVector input) throws InputErrorException {
		Input.assertCount(input, 2);

		ent = Input.parseEntity(input.get(0), Entity.class);

		outputName = input.get(1);

		Class<?> retClass = ent.getOutputType(outputName);
		if (!klass.isAssignableFrom(retClass)) {
			throw new InputErrorException("OutputInput class mismatch. Expected: %s, got: %s", klass.toString(), retClass.toString());
		}

		value = String.format("%s.%s", ent.getInputName(), outputName);
	}

	public T getOutputValue(double simTime) {
		return ent.getOutputValue(outputName, simTime, klass);
	}

}
