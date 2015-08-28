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
package com.jaamsim.BasicObjects;

import java.util.ArrayList;

import com.jaamsim.Graphics.TextBasics;
import com.jaamsim.Samples.SampleProvider;
import com.jaamsim.input.Input;
import com.jaamsim.input.InputAgent;
import com.jaamsim.input.InputErrorException;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.KeywordIndex;
import com.jaamsim.input.Output;
import com.jaamsim.input.Parser;
import com.jaamsim.input.UnitTypeInput;
import com.jaamsim.input.ValueInput;
import com.jaamsim.ui.GUIFrame;
import com.jaamsim.units.Unit;
import com.jaamsim.units.UserSpecifiedUnit;

public class InputValue extends TextBasics implements SampleProvider {

	@Keyword(description = "The unit type for the input value.",
	         exampleList = {"DistanceUnit"})
	protected final UnitTypeInput unitType;

	@Keyword(description = "The numerical value for the input.",
	         exampleList = {"1.5 km"})
	protected final ValueInput valInput;

	private boolean suppressUpdate = false; // prevents the white space in the edited text from changing

	{
		unitType = new UnitTypeInput("UnitType", "Key Inputs", UserSpecifiedUnit.class);
		unitType.setRequired(true);
		this.addInput(unitType);

		valInput = new ValueInput("Value", "Key Inputs", 0.0d);
		valInput.setUnitType(UserSpecifiedUnit.class);
		this.addInput(valInput);
	}

	public InputValue() {
		setSavedText(valInput.getDefaultString());
	}

	@Override
	public void updateForInput(Input<?> in) {
		super.updateForInput(in);

		if (in == unitType) {
			setUnitType(unitType.getUnitType());
			if (valInput.isDefault())
				setSavedText(valInput.getDefaultString());
			return;
		}

		if (in == valInput) {
			if (!suppressUpdate)
				setSavedText(valInput.getValueString());
			suppressUpdate = false;
			return;
		}
	}

	private void setUnitType(Class<? extends Unit> ut) {
		valInput.setUnitType(ut);
	}

	@Override
	public void acceptEdits() {
		try {
			suppressUpdate = true;
			ArrayList<String> tokens = new ArrayList<>();
			Parser.tokenize(tokens, getEditText(), true);
			InputAgent.apply(this, new KeywordIndex(valInput.getKeyword(), tokens, null));
			super.acceptEdits();
		}
		catch (InputErrorException e) {
			GUIFrame.invokeErrorDialog("Input Error", e.getMessage());
			suppressUpdate = false;
		}
	}

	@Override
	public void handleSelectionLost() {
		super.handleSelectionLost();

		// Stop editing, even if the inputs were not accepted successfully
		cancelEdits();
	}

	@Override
	public Class<? extends Unit> getUnitType() {
		return unitType.getUnitType();
	}

	@Override
	public Class<? extends Unit> getUserUnitType() {
		return unitType.getUnitType();
	}

	@Output(name = "Value",
	        description = "The present value for this input.",
	        unitType = UserSpecifiedUnit.class)
	@Override
	public double getNextSample(double simTime) {
		return valInput.getValue();
	}

	@Override
	public double getMeanValue(double simTime) {
		return valInput.getValue();
	}

	@Override
	public double getMinValue() {
		return valInput.getValue();
	}

	@Override
	public double getMaxValue() {
		return valInput.getValue();
	}

}
