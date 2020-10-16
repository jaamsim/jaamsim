/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2017-2018 JaamSim Software Inc.
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
package com.jaamsim.BasicObjects;

import java.util.ArrayList;

import com.jaamsim.input.ExpCollections;
import com.jaamsim.input.ExpError;
import com.jaamsim.input.ExpResult;
import com.jaamsim.units.DimensionlessUnit;

public class FileToVector extends FileToArray {

	public FileToVector() {}

	@Override
	protected ExpResult getValueForTokens(ArrayList<ArrayList<String>> tokens, double simTime) {
		int n = 0;
		for (ArrayList<String> record : tokens) {
			n += record.size();
		}
		ArrayList<ExpResult> ret = new ArrayList<>(n);
		for (ArrayList<String> record : tokens) {
			ArrayList<ExpResult> expRecord = getExpResultList(record, this, simTime);
			ret.addAll(expRecord);
		}
		return ExpCollections.wrapCollection(ret, DimensionlessUnit.class);
	}

	/**
	 * Sets the data for the FileToVector directly from a Java data structure, without the use
	 * of the DataFile input which can be left blank. The list input can contain the following
	 * Java classes and their sub-classes: Double, Integer, String, Entity, List, Map, and Array.
	 * @param list - List of Java objects containing the input data.
	 */
	public void setValue(ArrayList<Object> list) throws ExpError {
		ArrayList<ExpResult> resList = getExpResultList(list);
		ExpResult val = ExpCollections.wrapCollection(resList, DimensionlessUnit.class);
		setValue(val);
	}

}
