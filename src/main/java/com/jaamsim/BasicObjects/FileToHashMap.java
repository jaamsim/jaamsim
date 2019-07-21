/*
 * JaamSim Discrete Event Simulation
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
package com.jaamsim.BasicObjects;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import com.jaamsim.input.ExpError;
import com.jaamsim.input.ExpResType;
import com.jaamsim.input.ExpResult;
import com.jaamsim.input.FileInput;
import com.jaamsim.input.Output;

public class FileToHashMap extends FileToArray {

	LinkedHashMap<String, ArrayList<ExpResult>> value;

	public FileToHashMap() {
		value = new LinkedHashMap<>();
	}

	@Override
	protected void setValueForURI(URI uri, double simTime) {
		value = getHashMapForURI(uri, simTime);
	}

	@Override
	protected void clearValue() {
		value = new LinkedHashMap<>();
	}

	private LinkedHashMap<String, ArrayList<ExpResult>> getHashMapForURI(URI uri, double simTime) {
		ArrayList<ArrayList<String>> tokens = FileInput.getTokensFromURI(uri);
		LinkedHashMap<String, ArrayList<ExpResult>> ret = new LinkedHashMap<>(tokens.size());

		// Process each record from the file
		for (ArrayList<String> strRecord : tokens) {
			ArrayList<ExpResult> record = getExpResultList(strRecord, simTime);
			if (record.size() < 1)
				error("Entry has no key: %s", strRecord);

			if (record.get(0).type != ExpResType.STRING)
				error("Key is not a string in record: %s", strRecord);

			// Add the entry to the hashmap
			String key = record.get(0).stringVal;
			ArrayList<ExpResult> list = new ArrayList<>(record.subList(1, record.size()));
			ret.put(key, list);
		}
		return ret;
	}

	/**
	 * Sets the data for the FileToMatrix directly from a Java data structure, without the use
	 * of the DataFile input which can be left blank. The hashmap input can contain the following
	 * Java classes and their sub-classes: Double, Integer, String, Entity, List, Map, and Array.
	 * @param map - map whose values are a lists of Java objects containing the input data.
	 * @throws ExpError
	 */
	public void setValue(Map<String, ArrayList<Object>> map) throws ExpError {
		LinkedHashMap<String, ArrayList<ExpResult>> temp = new LinkedHashMap<>(map.size());
		for (Map.Entry<String, ArrayList<Object>> entry : map.entrySet()) {
			String key = entry.getKey();
			ArrayList<ExpResult> resRow = getExpResultList(entry.getValue());
			temp.put(key, resRow);
		}
		value = temp;
	}

	@Output(name = "Value",
	 description = "A HashMap with a string-valued key that contains the data from the input "
	             + "file.",
	    sequence = 1)
	public LinkedHashMap<String, ArrayList<ExpResult>> getValue(double simTime) {
		return value;
	}

}
