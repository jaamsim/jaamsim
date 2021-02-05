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

import com.jaamsim.basicsim.GUIListener;
import com.jaamsim.input.Input;
import com.jaamsim.input.Keyword;

public class SubModel extends AbstractSubModel {

	@Keyword(description = "Defines new keywords for the sub-model and creates new outputs with "
	                     + "the same names. "
	                     + "This allows the components of a sub-model to receive all their inputs "
	                     + "from either the parent sub-model or from other components.",
	         exampleList = {"{ ServiceTime TimeUnit } { NumberOfUnits }"})
	protected final PassThroughListInput keywordListInput;

	{
		keywordListInput = new PassThroughListInput("KeywordList", OPTIONS, new ArrayList<PassThroughData>());
		this.addInput(keywordListInput);
	}

	public SubModel() {}

	@Override
	public void updateForInput(Input<?> in) {
		super.updateForInput(in);

		if (in == keywordListInput) {
			updateKeywords(keywordListInput.getValue());
			GUIListener gui = getJaamSimModel().getGUIListener();
			if (gui != null && gui.isSelected(this))
				gui.updateInputEditor();
			return;
		}
	}

}
