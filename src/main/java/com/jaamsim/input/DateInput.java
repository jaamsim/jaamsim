/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2019-2026 JaamSim Software Inc.
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

import com.jaamsim.basicsim.Entity;
import com.jaamsim.basicsim.SimDate;
import com.jaamsim.units.DimensionlessUnit;
import com.jaamsim.units.Unit;

public class DateInput extends Input<SimDate> {

	public DateInput(String key, String cat, SimDate def) {
		super(key, cat, def);
	}

	@Override
	public String applyConditioning(String str) {
		return Parser.addQuotesIfNeeded(str);
	}

	@Override
	public void parse(Entity thisEnt, KeywordIndex kw) throws InputErrorException {
		Input.assertCount(kw, 1);
		int[] temp = Input.parseRFC8601Date(kw.getArg(0));
		value = new SimDate(temp[0], temp[1], temp[2], temp[3], temp[4], temp[5], temp[6]);
	}

	@Override
	public String getValidInputDesc() {
		return Input.VALID_DATE;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <V> V getValue(Entity thisEnt, double simTime, Class<V> klass) {
		return (V) getValue().toArray();
	}

	@Override
	public Class<?> getReturnType() {
		return int[].class;
	}

	@Override
	public Class<? extends Unit> getUnitType() {
		return DimensionlessUnit.class;
	}

}
