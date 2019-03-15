/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2010-2015 Ausenco Engineering Canada Inc.
 * Copyright (C) 2019 JaamSim Software Inc.
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

import com.jaamsim.basicsim.JaamSimModel;
import com.jaamsim.datatypes.IntegerVector;

public class RunNumberInput extends Input<Integer> {

	private IntegerVector rangeList;
	private int max;

	public RunNumberInput(String key, String cat, Integer def) {
		super(key, cat, def);
		rangeList = new IntegerVector(1);
		max = 1;
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
	}

	@Override
	public void parse(KeywordIndex kw)
	throws InputErrorException {
		Input.assertCount(kw, 1);
		String[] data = kw.getArg(0).split("-");

		// Run number entered as a single integer
		if (data.length == 1) {
			value = Input.parseInteger(kw.getArg(0), 1, max);
			return;
		}

		// Run number entered as a series of run indices
		if (data.length != rangeList.size())
			throw new InputErrorException("The number of run indices entered does not match "
					+ "the number that have been defined. Expected: %s, received: %s",
					rangeList.size(), data.length);

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

		value = temp;
	}

}
