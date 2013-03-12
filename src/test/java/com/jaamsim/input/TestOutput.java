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

import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;

import org.junit.Test;

import com.sandwell.JavaSimulation.Entity;
import com.sandwell.JavaSimulation3D.DisplayEntity;

public class TestOutput {

	/**
	 * Returns false if any of the outputs for this class do no conform to the correct method signature
	 * @param klass
	 * @return
	 */
	private void testOutputsForClass(Class<?> klass) {
		for (Method m : klass.getMethods()) {
			Output o = m.getAnnotation(Output.class);
			if (o == null) {
				// Not an output
				continue;
			}

			Class<?>[] paramTypes = m.getParameterTypes();
			if (paramTypes.length != 1 ||
				paramTypes[0] != double.class) {
				String message = String.format("Output: %s. %s.%s() method signature is invalid as an output method",
				                               o.name(), klass.getSimpleName(), m.getName());
				assertTrue(message, false);
			}
		}
	}

	// As new classes are converted to the output system, they should be included here
	private Class<?>[] classesToTest = {
			Entity.class,
			DisplayEntity.class
	};

	@Test
	public void testClassSignatures() {
		for (Class<?> c : classesToTest) {
			testOutputsForClass(c);
		}
	}

}
