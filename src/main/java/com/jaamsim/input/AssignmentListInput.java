/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2014 Ausenco Engineering Canada Inc.
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

import java.util.ArrayList;

import com.jaamsim.input.Input;
import com.sandwell.JavaSimulation.InputErrorException;
import com.sandwell.JavaSimulation.ListInput;
import com.sandwell.JavaSimulation.StringVector;

public class AssignmentListInput extends ListInput<ArrayList<ExpParser.Assignment>> {

	public AssignmentListInput(String key, String cat, ArrayList<ExpParser.Assignment> def){
		super(key, cat, def);
	}

	@Override
	public void parse(StringVector input) throws InputErrorException {

		// Divide up the inputs by the inner braces
		ArrayList<StringVector> splitData = InputAgent.splitStringVectorByBraces(input);
		ArrayList<ExpParser.Assignment> temp = new ArrayList<ExpParser.Assignment>(splitData.size());

		// Parse the inputs within each inner brace
		for (int i = 0; i < splitData.size(); i++) {
			try {
				StringVector data = splitData.get(i);
				Input.assertCount(data, 1);

				// Parse the assignment expression
				ExpParser.Assignment ass = ExpParser.parseAssignment(data.get(0));

				// Save the data for this assignment
				temp.add(ass);

			} catch (ExpParser.Error e) {
				throw new InputErrorException(INP_ERR_ELEMENT, i, e.getMessage());
			}
		}

		// Save the data for each assignment
		value = temp;
	}

	@Override
	public String getDefaultString() {
		return NO_VALUE;
	}

}
