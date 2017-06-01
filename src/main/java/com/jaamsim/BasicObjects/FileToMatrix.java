/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2017 JaamSim Software Inc.
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

import com.jaamsim.input.FileInput;
import com.jaamsim.input.Output;
import com.jaamsim.units.DimensionlessUnit;

public class FileToMatrix extends FileToArray {

	double[][] value;

	public FileToMatrix() {}

	@Override
	protected void setValueForURI(URI uri) {
		value = get2DArrayForURI(uri);
	}

	@Override
	protected void clearValue() {
		value = null;
	}

	private static double[][] get2DArrayForURI(URI uri) {
		ArrayList<ArrayList<String>> tokens = FileInput.getTokensFromURI(uri);
		double[][] ret = new double[tokens.size()][];
		for (int i=0; i<tokens.size(); i++) {
			double[] record = new double[tokens.get(i).size()];
			for (int j=0; j<tokens.get(i).size(); j++) {
				try {
					record[j] = Double.parseDouble(tokens.get(i).get(j));
				}
				catch (NumberFormatException e) {
					record[j] = Double.NaN;
				}
			}
			ret[i] = record;
		}
		return ret;
	}

	@Output(name = "Value",
	 description = "A matrix containing the numerical data from the input file.",
	    unitType = DimensionlessUnit.class,
	    sequence = 1)
	public double[][] getValue(double simTime) {
		return value;
	}

}
