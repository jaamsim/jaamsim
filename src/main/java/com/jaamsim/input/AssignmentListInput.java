/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2014 Ausenco Engineering Canada Inc.
 * Copyright (C) 2016-2021 JaamSim Software Inc.
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
import com.jaamsim.basicsim.JaamSimModel;


public class AssignmentListInput extends ListInput<ArrayList<ExpParser.Assignment>> {

	private ArrayList<ExpEvaluator.EntityParseContext> parseContextList;

	public AssignmentListInput(String key, String cat, ArrayList<ExpParser.Assignment> def){
		super(key, cat, def);
	}

	@Override
	public void copyFrom(Entity thisEnt, Input<?> in) {
		super.copyFrom(thisEnt, in);

		// An expression input must be re-parsed to reset the entity referred to by "this"
		parseFrom(thisEnt, in);
	}

	@Override
	public void parse(Entity thisEnt, KeywordIndex kw) throws InputErrorException {

		// Divide up the inputs by the inner braces
		ArrayList<KeywordIndex> subArgs = kw.getSubArgs();
		ArrayList<ExpParser.Assignment> temp = new ArrayList<>(subArgs.size());
		ArrayList<ExpEvaluator.EntityParseContext> pcList = new ArrayList<>(subArgs.size());

		// Parse the inputs within each inner brace
		for (int i = 0; i < subArgs.size(); i++) {
			KeywordIndex subArg = subArgs.get(i);
			Input.assertCount(subArg, 1);
			try {
				// Parse the assignment expression
				String assignmentString = subArg.getArg(0);
				ExpEvaluator.EntityParseContext pc = ExpEvaluator.getParseContext(thisEnt, assignmentString);
				ExpParser.Assignment ass = ExpParser.parseAssignment(pc, assignmentString);

				// Save the data for this assignment
				pcList.add(pc);
				temp.add(ass);

			} catch (ExpError e) {
				throw new InputErrorException(e);
			}
		}

		// Save the data for each assignment
		parseContextList = pcList;
		value = temp;
	}

	@Override
	public void getValueTokens(ArrayList<String> toks) {
		if (value == null) return;

		for (int i = 0; i < value.size(); i++) {
			toks.add("{");
			toks.add(parseContextList.get(i).getUpdatedSource());
			toks.add("}");
		}
	}

	@Override
	public int getListSize() {
		if (value == null)
			return 0;
		else
			return value.size();
	}

	@Override
	public String getDefaultString(JaamSimModel simModel) {
		return "";
	}

	@Override
	public boolean useExpressionBuilder() {
		return true;
	}

}
