/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2015 Ausenco Engineering Canada Inc.
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
package com.jaamsim.Graphics;

import java.util.ArrayList;

import com.jaamsim.input.Input;
import com.jaamsim.input.InputAgent;
import com.jaamsim.input.InputErrorException;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.KeywordIndex;
import com.jaamsim.input.KeywordInput;
import com.jaamsim.input.Parser;
import com.jaamsim.ui.GUIFrame;

public class InputBox extends TextBasics {

	@Keyword(description = "The name of the entity and keyword that will receive the input value.",
	         exampleList = {"DisplayEntity1 Size"})
	protected final KeywordInput target;

	{
		target = new KeywordInput("TargetInput", "Key Inputs", null);
		target.setRequired(true);
		this.addInput(target);
	}

	public InputBox() {}

	@Override
	public void setInputsForDragAndDrop() {
		super.setInputsForDragAndDrop();
		this.setSavedText(this.getName());
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
			ArrayList<String> tokens = new ArrayList<>();
			Parser.tokenize(tokens, getEditText(), true);
			KeywordIndex kw = new KeywordIndex(target.getValue(), tokens, null);
			InputAgent.apply(target.getTargetEntity(), kw);
			super.acceptEdits();
		}
		catch (InputErrorException e) {
			GUIFrame.invokeErrorDialog("Input Error", e.getMessage());
		}
	}

	@Override
	public void handleSelectionLost() {
		super.handleSelectionLost();

		// Stop editing, even if the inputs were not accepted successfully
		cancelEdits();
	}

	@Override
	public String getCachedText() {
		Input<?> targetInput = target.getTargetInput();
		if (!isEditMode() && targetInput != null) {
			String str = targetInput.getValueString();
			if (str.isEmpty())
				str = targetInput.getDefaultString();
			this.setSavedText(str);
		}
		return getEditText();
	}

}
