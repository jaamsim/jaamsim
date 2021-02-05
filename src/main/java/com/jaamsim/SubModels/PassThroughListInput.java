/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2018-2021 JaamSim Software Inc.
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
package com.jaamsim.SubModels;

import java.util.ArrayList;

import com.jaamsim.basicsim.Entity;
import com.jaamsim.basicsim.JaamSimModel;
import com.jaamsim.input.Input;
import com.jaamsim.input.InputErrorException;
import com.jaamsim.input.KeywordIndex;
import com.jaamsim.input.ListInput;
import com.jaamsim.units.DimensionlessUnit;
import com.jaamsim.units.Unit;

public class PassThroughListInput extends ListInput<ArrayList<PassThroughData>> {

	public PassThroughListInput(String key, String cat, ArrayList<PassThroughData> def) {
		super(key, cat, def);
	}

	@Override
	public int getListSize() {
		if (value == null)
			return 0;
		return value.size();
	}

	@Override
	public void parse(Entity thisEnt, KeywordIndex kw) throws InputErrorException {
		// Divide up the inputs by the inner braces
		ArrayList<KeywordIndex> subArgs = kw.getSubArgs();
		ArrayList<PassThroughData> temp = new ArrayList<>(subArgs.size());

		// Parse the inputs within each inner brace
		for (int i = 0; i < subArgs.size(); i++) {
			KeywordIndex subArg = subArgs.get(i);
			Input.assertCount(subArg, 1, 2);
			try {
				// Fist entry is the name of the keyword/output
				String name = subArg.getArg(0);

				// Second entry if present is the unit type
				Class<? extends Unit> unitType = DimensionlessUnit.class;
				if (subArg.numArgs() == 2) {
					unitType = Input.parseUnitType(thisEnt.getJaamSimModel(), subArg.getArg(1));
				}
				temp.add(new PassThroughData(name, unitType));
			}
			catch(InputErrorException e) {
				throw new InputErrorException(INP_ERR_ELEMENT, i+1, e.getMessage());
			}
		}
		value = temp;
	}

	@Override
	public String getValidInputDesc() {
		return Input.VALID_PASSTHROUGH;
	}

	@Override
	public String getDefaultString(JaamSimModel simModel) {
		if (defValue == null || defValue.isEmpty())
			return "";

		return defValue.toString();
	}

	@Override
	public boolean useExpressionBuilder() {
		return true;
	}

}
