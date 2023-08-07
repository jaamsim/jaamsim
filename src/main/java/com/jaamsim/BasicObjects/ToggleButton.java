/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2021-2023 JaamSim Software Inc.
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

import java.util.ArrayList;

import com.jaamsim.ColourProviders.ColourProvInput;
import com.jaamsim.DisplayModels.ShapeModel;
import com.jaamsim.GameObjects.GameEntity;
import com.jaamsim.Graphics.FillEntity;
import com.jaamsim.Graphics.LineEntity;
import com.jaamsim.Samples.SampleInput;
import com.jaamsim.basicsim.Entity;
import com.jaamsim.basicsim.ObserverEntity;
import com.jaamsim.basicsim.SubjectEntity;
import com.jaamsim.basicsim.SubjectEntityDelegate;
import com.jaamsim.input.BooleanInput;
import com.jaamsim.input.ColourInput;
import com.jaamsim.input.Input;
import com.jaamsim.input.InputCallback;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.Output;
import com.jaamsim.math.Color4d;

public class ToggleButton extends GameEntity implements SubjectEntity, LineEntity, FillEntity {

	@Keyword(description = "The initial state for the button: "
	                     + "TRUE = button pressed, FALSE = button unpressed.",
	         exampleList = { "TRUE" })
	private final BooleanInput initialValue;

	@Keyword(description = "The colour of the button when pressed.",
	         exampleList = { "yellow" })
	private final ColourProvInput pressedColour;

	@Keyword(description = "The colour of the button when not pressed.",
	         exampleList = { "magenta" })
	private final ColourProvInput unpressedColour;

	@Keyword(description = "Determines whether or not the object is outlined. "
	                     + "If TRUE, it is outlined with a specified colour. "
	                     + "If FALSE, it is drawn without an outline.",
	         exampleList = {"FALSE"})
	protected final BooleanInput outlined;

	@Keyword(description = "The colour with which the object is outlined.",
	         exampleList = {"red"})
	protected final ColourProvInput lineColour;

	@Keyword(description = "Width of the outline in pixels.",
	         exampleList = { "3" })
	protected final SampleInput lineWidth;

	private boolean value;  // true = pressed, false = not pressed
	private final SubjectEntityDelegate subject = new SubjectEntityDelegate(this);

	{
		displayModelListInput.clearValidClasses();
		displayModelListInput.addValidClass(ShapeModel.class);

		initialValue = new BooleanInput("InitialValue", KEY_INPUTS, false);
		initialValue.setCallback(initialValueCallback);
		this.addInput(initialValue);

		pressedColour = new ColourProvInput("PressedColour", FORMAT, ColourInput.MED_GREY);
		pressedColour.setDefaultText("ShapeModel value");
		this.addInput(pressedColour);
		this.addSynonym(pressedColour, "FillColour");

		unpressedColour = new ColourProvInput("UnpressedColour", FORMAT, ColourInput.getColorWithName("Ivory"));
		this.addInput(unpressedColour);

		outlined = new BooleanInput("Outlined", FORMAT, true);
		outlined.setDefaultText("ShapeModel value");
		this.addInput(outlined);

		lineColour = new ColourProvInput("LineColour", FORMAT, ColourInput.BLACK);
		lineColour.setDefaultText("ShapeModel value");
		this.addInput(lineColour);

		lineWidth = new SampleInput("LineWidth", FORMAT, 1);
		lineWidth.setValidRange(0, Double.POSITIVE_INFINITY);
		lineWidth.setIntegerValue(true);
		lineWidth.setDefaultText("ShapeModel value");
		this.addInput(lineWidth);
	}

	static final InputCallback initialValueCallback = new InputCallback() {
		@Override
		public void callback(Entity ent, Input<?> inp) {
			((ToggleButton)ent).updateInputValue();
		}
	};

	void updateInputValue() {
		value = initialValue.getValue();
	}

	@Override
	public void setState() {
		value = !value;
	}

	@Override
	public void doAction() {
		notifyObservers();
	}

	@Override
	public void registerObserver(ObserverEntity obs) {
		subject.registerObserver(obs);
	}

	@Override
	public void notifyObservers() {
		subject.notifyObservers();
	}

	@Override
	public ArrayList<ObserverEntity> getObserverList() {
		return subject.getObserverList();
	}

	@Override
	public boolean isFilled(double simTime) {
		return true;
	}

	@Override
	public Color4d getFillColour(double simTime) {
		if (pressedColour.isDefault()) {
			FillEntity model = getDisplayModel(FillEntity.class);
			if (model != null)
				return model.getFillColour(simTime);
		}
		return pressedColour.getNextColour(this, simTime);
	}

	@Override
	public boolean isOutlined(double simTime) {
		if (outlined.isDefault()) {
			LineEntity model = getDisplayModel(LineEntity.class);
			if (model != null)
				return model.isOutlined(simTime);
		}
		return outlined.getValue();
	}

	@Override
	public int getLineWidth(double simTime) {
		if (lineWidth.isDefault()) {
			LineEntity model = getDisplayModel(LineEntity.class);
			if (model != null)
				return model.getLineWidth(simTime);
		}
		return (int) lineWidth.getNextSample(this, simTime);
	}

	@Override
	public Color4d getLineColour(double simTime) {
		if (lineColour.isDefault()) {
			LineEntity model = getDisplayModel(LineEntity.class);
			if (model != null)
				return model.getLineColour(simTime);
		}
		return lineColour.getNextColour(this, simTime);
	}

	@Override
	public void updateGraphics(double simTime) {
		super.updateGraphics(simTime);

		// Select the colour
		Color4d col;
		if (value) {
			col = getFillColour(simTime);
		}
		else {
			col = unpressedColour.getNextColour(this, simTime);
		}

		// Display the button
		setTagVisibility(ShapeModel.TAG_CONTENTS, true);
		setTagColour(ShapeModel.TAG_CONTENTS, col);
	}

	@Output(name = "Value",
	 description = "Returns TRUE if the button is in the 'pressed' state, FALSE if it is not.",
	    sequence = 1)
	public boolean isPressed(double simTime) {
		return value;
	}

}
