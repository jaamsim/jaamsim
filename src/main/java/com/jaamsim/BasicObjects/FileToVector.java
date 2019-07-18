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

import java.net.URI;
import java.util.ArrayList;

import com.jaamsim.input.ExpError;
import com.jaamsim.input.ExpResult;
import com.jaamsim.input.FileInput;
import com.jaamsim.input.Output;

public class FileToVector extends FileToArray {

	ArrayList<ExpResult> value;

	public FileToVector() {
		value = new ArrayList<>();
	}

	@Override
	protected void setValueForURI(URI uri, double simTime) {
		value = getVectorForURI(uri, simTime);
	}

	@Override
	protected void clearValue() {
		value = new ArrayList<>();
	}

	private ArrayList<ExpResult> getVectorForURI(URI uri, double simTime) {
		ArrayList<ArrayList<String>> tokens = FileInput.getTokensFromURI(uri);
		int n = 0;
		for (ArrayList<String> record : tokens) {
			n += record.size();
		}
		ArrayList<ExpResult> ret = new ArrayList<>(n);
		for (ArrayList<String> record : tokens) {
			ArrayList<ExpResult> expRecord = getExpResultList(record, simTime);
			ret.addAll(expRecord);
		}
		return ret;
	}

	/**
	 * Sets the data for the FileToVector directly from a Java data structure, without the use
	 * of the DataFile input which can be left blank. The list input can contain the following
	 * Java classes and their sub-classes: Double, Integer, String, Entity, List, Map, and Array.
	 * @param list - List of Java objects containing the input data.
	 * @throws Exception
	 */
	public void setValue(ArrayList<Object> list) throws ExpError {
		value = getExpResultList(list);
	}

	@Output(name = "Value",
	 description = "A vector containing the data from the input file.",
	    sequence = 1)
	public ArrayList<ExpResult> getValue(double simTime) {
		return value;
	}

}
