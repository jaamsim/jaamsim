/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2010-2015 Ausenco Engineering Canada Inc.
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

import com.jaamsim.basicsim.Simulation;
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

		int temp = Simulation.getRunNumber(indexList, rangeList);
		if (temp < 1 || temp > max)
			throw new InputErrorException(INP_ERR_INTEGERRANGE, 1, max, temp);

		value = temp;
	}

}
