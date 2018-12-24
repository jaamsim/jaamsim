/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2018 JaamSim Software Inc.
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
package com.jaamsim.math;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.jaamsim.input.ColourInput;

public class TestColor4d {

	@Test
	public void testColor4d() {
		for (Color4d col : ColourInput.namedColourList) {
			String colName = ColourInput.getColorName(col);
			compareColor(colName);
		}
	}

	private void compareColor(String name) {
		Color4d col = ColourInput.getColorWithName(name);

		int red = (int) Math.round(col.r * 255.0d);
		int green = (int) Math.round(col.g * 255.0d);
		int blue = (int) Math.round(col.b * 255.0d);
		int alpha = (int) Math.round(col.a * 255.0d);
		Color4d newCol = new Color4d(red, green, blue, alpha);
		assertTrue(col.equals(newCol));

		String newColName = ColourInput.getColorName(newCol);
		assertTrue(name.equals(newColName));
	}
}
