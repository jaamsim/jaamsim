/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2016-2023 JaamSim Software Inc.
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

import java.util.ArrayList;

import com.jaamsim.units.DimensionlessUnit;
import com.jaamsim.units.Unit;


public class ExpValResult {

	public static enum State {
		VALID, ERROR, UNDECIDABLE
	}

	public final State state;
	public final ArrayList<ExpError> errors;

	public final Class<? extends Unit> unitType;
	public final ExpResType type;

	public static String typeString(ExpResType res) {
		switch(res) {
		case ENTITY:
			return "entity";
		case NUMBER:
			return "number";
		case STRING:
			return "string";
		case COLLECTION:
			return "collection";
		case LAMBDA:
			return "function";
		default:
			assert(false);
			return "unknown type";
		}
	}

	public static ExpValResult makeValidRes(ExpResType t, Class<? extends Unit> ut)
	{
		return new ExpValResult(State.VALID, t, ut, null);
	}

	public static ExpValResult makeUndecidableRes()
	{
		return new ExpValResult(State.UNDECIDABLE, null, DimensionlessUnit.class, null);
	}

	public static ExpValResult makeErrorRes(ArrayList<ExpError> es) {
		return new ExpValResult(State.ERROR, null, DimensionlessUnit.class, es);
	}

	public static ExpValResult makeErrorRes(ExpError error) {
		ArrayList<ExpError> es = new ArrayList<>(1);
		es.add(error);
		return new ExpValResult(State.ERROR, null, DimensionlessUnit.class, es);
	}

	private ExpValResult(State s, ExpResType t, Class<? extends Unit> ut, ArrayList<ExpError> es) {
		state = s;
		unitType = ut;
		type = t;

		if (es == null)
			errors = new ArrayList<>();
		else
			errors = es;
	}

	@Override
	public String toString() {
		String utStr = "null";
		if (unitType != null)
			utStr = unitType.getSimpleName();
		return String.format("state=%s, errors=%s, unitType=%s, type=%s",
				state, errors, utStr, type);
	}

}
