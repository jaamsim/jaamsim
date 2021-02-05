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

import com.jaamsim.input.Input;
import com.jaamsim.input.Keyword;
import com.jaamsim.ui.EditBox;
import com.jaamsim.ui.GUIFrame;

public class SubModel extends AbstractSubModel {

	@Keyword(description = "Defines new keywords for the sub-model that can be passed to its "
	                     + "component objects. Each new keyword defines a matching sub-model "
	                     + "output that can be used in the inputs to the sub-model's components.",
	         exampleList = {"{ ServiceTime TimeUnit } { NumberOfUnits }"})
	protected final PassThroughListInput keywordListInput;

	{
		keywordListInput = new PassThroughListInput("KeywordList", KEY_INPUTS, new ArrayList<PassThroughData>());
		this.addInput(keywordListInput);
	}

	public SubModel() {}

	@Override
	public void updateForInput(Input<?> in) {
		super.updateForInput(in);

		if (in == keywordListInput) {
			updateKeywords(keywordListInput.getValue());
			if (GUIFrame.getInstance() != null)
				EditBox.getInstance().setEntity(null);
			return;
		}
	}

}
