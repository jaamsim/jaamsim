/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2013 Ausenco Engineering Canada Inc.
 * Copyright (C) 2022-2023 JaamSim Software Inc.
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

import java.util.ArrayList;

import com.jaamsim.basicsim.Entity;
import com.jaamsim.input.Input;
import com.jaamsim.input.Parser;
import com.jaamsim.units.Unit;

public interface SampleProvider {
	public Class<? extends Unit> getUnitType();
	public double getNextSample(Entity thisEnt, double simTime);
	public double getMeanValue(double simTime);

	public static String addQuotesIfNeeded(String str) {

		// No changes required if the input is a number and unit
		ArrayList<String> tokens = new ArrayList<>();
		Parser.tokenize(tokens, str, true);
		if (tokens.size() == 2 && Input.isDouble(tokens.get(0)))
			return str;

		return Parser.addQuotesIfNeeded(str);
	}
}
