/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2015 Ausenco Engineering Canada Inc.
 * Copyright (C) 2018-2019 JaamSim Software Inc.
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
package com.jaamsim.Graphics;

import com.jaamsim.Commands.KeywordCommand;
import com.jaamsim.basicsim.GUIListener;
import com.jaamsim.input.Input;
import com.jaamsim.input.InputAgent;
import com.jaamsim.input.InputErrorException;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.KeywordIndex;
import com.jaamsim.input.KeywordInput;

public class InputBox extends TextBasics {

	@Keyword(description = "The name of the entity and keyword that will receive the input value.",
	         exampleList = {"DisplayEntity1 Size"})
	protected final KeywordInput target;

	{
		target = new KeywordInput("TargetInput", KEY_INPUTS, null);
		target.setRequired(true);
		this.addInput(target);
	}

	public InputBox() {}

	@Override
	public void setInputsForDragAndDrop() {
		super.setInputsForDragAndDrop();
		this.setText(this.getName());
	}

	@Override
	public void updateForInput(Input<?> in) {
		super.updateForInput(in);

		if (in == target) {
			cancelEdits();
			return;
		}
	}

	@Override
	public void acceptEdits() {
		if (target.getValue() == null) {
			super.acceptEdits();
			return;
		}
		try {
			KeywordIndex kw = InputAgent.formatInput(target.getValue(), getText());
			InputAgent.storeAndExecute(new KeywordCommand(target.getTargetEntity(), kw));
			super.acceptEdits();
		}
		catch (InputErrorException e) {
			GUIListener gui = getJaamSimModel().getGUIListener();
			if (gui != null)
				gui.invokeErrorDialogBox("Input Error", e.getMessage());
		}
	}

	@Override
	public String getCachedText() {
		Input<?> targetInput = target.getTargetInput();
		if (!isEditMode() && targetInput != null) {
			String str = targetInput.getValueString();
			if (str.isEmpty())
				str = targetInput.getDefaultString();
			this.setText(str);
		}
		return getText();
	}

}
