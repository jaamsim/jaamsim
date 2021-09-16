/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2021 JaamSim Software Inc.
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

import com.jaamsim.DisplayModels.ShapeModel;
import com.jaamsim.GameObjects.GameEntity;
import com.jaamsim.input.BooleanInput;
import com.jaamsim.input.ColourInput;
import com.jaamsim.input.Input;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.Output;
import com.jaamsim.math.Color4d;

public class ToggleButton extends GameEntity {

	@Keyword(description = "The initial state for the button: "
	                     + "TRUE = button pressed, FALSE = button unpressed.",
	         exampleList = { "TRUE" })
	private final BooleanInput initialValue;

	@Keyword(description = "The colour of the button when pressed.",
	         exampleList = { "yellow" })
	private final ColourInput pressedColour;

	@Keyword(description = "The colour of the button when not pressed.",
	         exampleList = { "magenta" })
	private final ColourInput unpressedColour;

	private boolean value;  // true = pressed, false = not pressed

	{
		initialValue = new BooleanInput("InitialValue", KEY_INPUTS, false);
		this.addInput(initialValue);

		pressedColour = new ColourInput("PressedColour", FORMAT, ColourInput.LIGHT_GREY);
		this.addInput(pressedColour);

		unpressedColour = new ColourInput("UnpressedColour", FORMAT, ColourInput.getColorWithName("Ivory"));
		this.addInput(unpressedColour);
	}

	@Override
	public void updateForInput(Input<?> in) {
		super.updateForInput(in);

		if (in == initialValue) {
			value = initialValue.getValue();
			return;
		}
	}

	@Override
	public void setState() {
		value = !value;
	}

	@Override
	public void doAction() {
		// needed only to trigger a new event
	}

	@Override
	public void updateGraphics(double simTime) {
		super.updateGraphics(simTime);

		// Select the colour
		Color4d col;
		if (value) {
			col = pressedColour.getValue();
		}
		else {
			col = unpressedColour.getValue();
		}

		// Display the button
		setTagVisibility(ShapeModel.TAG_CONTENTS, true);
		setTagVisibility(ShapeModel.TAG_OUTLINES, true);
		setTagColour(ShapeModel.TAG_CONTENTS, col);
		setTagColour(ShapeModel.TAG_OUTLINES, ColourInput.BLACK);
	}

	@Output(name = "Value",
	 description = "Returns TRUE if the button is in the 'pressed' state, FALSE if it is not.",
	    sequence = 1)
	public boolean isPressed(double simTime) {
		return value;
	}

}
