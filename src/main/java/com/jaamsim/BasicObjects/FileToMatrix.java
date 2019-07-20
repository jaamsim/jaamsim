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

public class FileToMatrix extends FileToArray {

	ArrayList<ArrayList<ExpResult>> value;

	public FileToMatrix() {
		value = new ArrayList<>();
	}

	@Override
	protected void setValueForURI(URI uri, double simTime) {
		value = getMatrixForURI(uri, simTime);
	}

	@Override
	protected void clearValue() {
		value = new ArrayList<>();
	}

	private ArrayList<ArrayList<ExpResult>> getMatrixForURI(URI uri, double simTime) {
		ArrayList<ArrayList<String>> tokens = FileInput.getTokensFromURI(uri);
		ArrayList<ArrayList<ExpResult>> ret = new ArrayList<>(tokens.size());
		for (ArrayList<String> strRecord : tokens) {
			ArrayList<ExpResult> record = getExpResultList(strRecord, simTime);
			ret.add(record);
		}
		return ret;
	}

	/**
	 * Sets the data for the FileToMatrix directly from a Java data structure, without the use
	 * of the DataFile input which can be left blank. The matrix input can contain the following
	 * Java classes and their sub-classes: Double, Integer, String, Entity, List, Map, and Array.
	 * @param matrix - List of lists of Java objects containing the input data.
	 * @throws ExpError
	 */
	public void setValue(ArrayList<ArrayList<Object>> matrix) throws ExpError {
		ArrayList<ArrayList<ExpResult>> temp = new ArrayList<>(matrix.size());
		for (ArrayList<Object> row : matrix) {
			ArrayList<ExpResult> resRow = getExpResultList(row);
			temp.add(resRow);
		}
		value = temp;
	}

	@Output(name = "Value",
	 description = "A matrix containing the data from the input file.",
	    sequence = 1)
	public ArrayList<ArrayList<ExpResult>> getValue(double simTime) {
		return value;
	}

}
