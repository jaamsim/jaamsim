/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2010-2015 Ausenco Engineering Canada Inc.
 * Copyright (C) 2019-2022 JaamSim Software Inc.
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

import com.jaamsim.Samples.SampleConstant;
import com.jaamsim.Samples.SampleInput;
import com.jaamsim.Samples.SampleProvider;
import com.jaamsim.basicsim.Entity;
import com.jaamsim.basicsim.JaamSimModel;
import com.jaamsim.datatypes.IntegerVector;

public class RunNumberInput extends SampleInput {

	private IntegerVector rangeList;
	private int max;

	public RunNumberInput(String key, String cat, SampleProvider def) {
		super(key, cat, def);
		rangeList = new IntegerVector(1);
		max = Integer.MAX_VALUE;
	}

	public RunNumberInput(String key, String cat, int def) {
		this(key, cat, new SampleConstant(def));
	}

	public void setRunIndexRangeList(IntegerVector list) {

		// If the index ranges have not changed, then do nothing
		if (list.size() == rangeList.size()) {
			boolean equal = true;
			for (int i=0; i<list.size(); i++) {
				if (list.get(i) != rangeList.get(i)) {
					equal = false;
					break;
				}
			}
			if (equal)
				return;
		}

		this.reset();
		rangeList = list;
		max = 1;
		for (int i=0; i<rangeList.size(); i++) {
			max *= rangeList.get(i);
		}
		if (rangeList.size() == 0)
			max = Integer.MAX_VALUE;
		setValidRange(1, max);
	}

	@Override
	public void parse(Entity thisEnt, KeywordIndex kw)
	throws InputErrorException {
		Input.assertCount(kw, 1);
		String[] data = kw.getArg(0).split("-");

		// Run number entered as a number or expression
		if (data.length != rangeList.size()) {
			super.parse(thisEnt, kw);
			return;
		}

		// Run number entered as a series of run indices
		IntegerVector indexList = new IntegerVector(data.length);
		indexList.fillWithEntriesOf(data.length, 0);
		for (int i=0; i<data.length; i++) {
			int val = Input.parseInteger(data[i]);

			if (val > rangeList.get(i))
				throw new InputErrorException("The run index value %s exceeds the defined range "
						+ "of %s.", val, rangeList.get(i));
			if (val <= 0)
				throw new InputErrorException("The run index value must be greater than or equal "
						+ "to 1. Received: %s", val);

			indexList.set(i, val);
		}

		int temp = JaamSimModel.getRunNumber(indexList, rangeList);
		if (temp < 1 || temp > max)
			throw new InputErrorException(INP_ERR_INTEGERRANGE, 1, max, temp);

		value = new SampleConstant(temp);
	}

	@Override
	public String getValidInputDesc() {
		return Input.VALID_SCENARIO_NUMBER;
	}

	@Override
	public void getValueTokens(ArrayList<String> toks) {
		if (value == null || isDef)
			return;

		if (rangeList.size() == 0) {
			super.getValueTokens(toks);
			return;
		}

		if (valueTokens == null)
			return;

		for (String each : valueTokens)
			toks.add(each);
	}

}
