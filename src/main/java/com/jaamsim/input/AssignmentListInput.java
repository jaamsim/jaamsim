/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2014 Ausenco Engineering Canada Inc.
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

import com.jaamsim.basicsim.Entity;


public class AssignmentListInput extends ListInput<ArrayList<ExpParser.Assignment>> {

	private Entity thisEnt;

	public AssignmentListInput(String key, String cat, ArrayList<ExpParser.Assignment> def){
		super(key, cat, def);
	}

	public void setEntity(Entity ent) {
		thisEnt = ent;
	}

	@Override
	public void parse(KeywordIndex kw) throws InputErrorException {

		// Divide up the inputs by the inner braces
		ArrayList<KeywordIndex> subArgs = kw.getSubArgs();
		ArrayList<ExpParser.Assignment> temp = new ArrayList<>(subArgs.size());

		// Parse the inputs within each inner brace
		for (int i = 0; i < subArgs.size(); i++) {
			KeywordIndex subArg = subArgs.get(i);
			Input.assertCount(subArg, 1);
			try {
				// Parse the assignment expression
				ExpParser.Assignment ass = ExpParser.parseAssignment(ExpEvaluator.getParseContext(thisEnt), subArg.getArg(0));

				// Save the data for this assignment
				temp.add(ass);

			} catch (ExpError e) {
				throw new InputErrorException(INP_ERR_ELEMENT, i, e.getMessage());
			}
		}

		// Save the data for each assignment
		value = temp;
	}

	@Override
	public int getListSize() {
		if (value == null)
			return 0;
		else
			return value.size();
	}

	@Override
	public String getDefaultString() {
		return "";
	}

}
