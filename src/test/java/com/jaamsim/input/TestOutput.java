/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2013 Ausenco Engineering Canada Inc.
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
package com.jaamsim.input;

import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;

import org.junit.Test;

import com.jaamsim.Graphics.DisplayEntity;
import com.jaamsim.basicsim.Entity;

public class TestOutput {

	/**
	 * Returns false if any of the outputs for this class do no conform to the correct method signature
	 * @param klass
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
