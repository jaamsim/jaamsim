/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2018-2021 JaamSim Software Inc.
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
package com.jaamsim.SubModels;

import java.util.ArrayList;

import com.jaamsim.input.ExpressionInput;
import com.jaamsim.input.Input;

public abstract class AbstractSubModel extends CompoundEntity {

	protected ArrayList<PassThroughData> keywordList;
	private ArrayList<ExpressionInput> newInputList;

	public AbstractSubModel() {
		keywordList = new ArrayList<>();
		newInputList = new ArrayList<>();
	}

	@Override
	public void updateForInput(Input<?> in) {
		super.updateForInput(in);

		if (newInputList.contains(in)) {
			ExpressionInput expIn = (ExpressionInput) in;
			addInputAsOutput(expIn.getKeyword(), expIn.getValue(), expIn.getUnitType());
			return;
		}
	}

	public void setKeywordList(ArrayList<PassThroughData> list) {
		keywordList = new ArrayList<>(list);
	}

	public ArrayList<PassThroughData> getKeywordList() {
		return keywordList;
	}

	/**
	 * Updates the added keywords to match the specified list.
	 * @param newDataList - data for the new list of added keywords
	 */
	public void updateKeywords(ArrayList<PassThroughData> newDataList) {

		// Do nothing if the keywords are unchanged
		if (newDataList.equals(keywordList))
			return;

		// Remove the old inputs and outputs
		for (ExpressionInput in : newInputList) {
			removeInput(in);
			removeInputAsOutput(in.getKeyword());
		}

		// Add the new keywords, using the old ones whenever possible to save their input values
		ArrayList<ExpressionInput> list = new ArrayList<>(newDataList.size());
		for (PassThroughData data : newDataList) {
			int index = keywordList.indexOf(data);
			ExpressionInput in = null;
			if (index == -1) {
				in = new ExpressionInput(data.getName(), KEY_INPUTS, null);
				in.setUnitType(data.getUnitType());
				in.setRequired(true);
			}
			else {
				in = newInputList.get(index);
			}
			addInput(in);
			addInputAsOutput(in.getKeyword(), in.getValue(), in.getUnitType());
			list.add(in);
		}
		newInputList = list;
		setKeywordList(newDataList);
	}

}
