/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2014-2015 Ausenco Engineering Canada Inc.
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
import com.jaamsim.Graphics.DisplayEntity;
import com.jaamsim.Samples.SampleInput;
import com.jaamsim.input.ColourInput;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.Output;
import com.jaamsim.input.StringInput;
import com.jaamsim.units.DimensionlessUnit;

public class BooleanIndicator extends DisplayEntity {

	@Keyword(description = "An expression returning a boolean value: zero = FALSE, non-zero = TRUE.",
	         exampleList = {"'[Queue1].QueueLength > 2'", "[Server1].Working"})
	private final SampleInput expInput;

	@Keyword(description = "The colour of the indicator when the property is true",
	         exampleList = {"green"})
	private final ColourInput trueColor;

	@Keyword(description = "The colour of the indicator when the property is false",
	         exampleList = {"red"})
	private final ColourInput falseColor;

	@Keyword(description = "The string displayed by the Text output when the property is true.",
	         exampleList = {"'True text'"})
	private final StringInput trueText;

	@Keyword(description = "The string displayed by the Text output when the property is false.",
	         exampleList = {"'False text'"})
	private final StringInput falseText;

	{
		expInput = new SampleInput("DataSource", "Key Inputs", null);
		expInput.setUnitType(DimensionlessUnit.class);
		expInput.setEntity(this);
		expInput.setRequired(true);
		this.addInput(expInput);
		this.addSynonym(expInput, "OutputName");

		trueColor = new ColourInput("TrueColour", "Graphics", ColourInput.GREEN);
		this.addInput(trueColor);
		this.addSynonym(trueColor, "TrueColor");

		falseColor = new ColourInput("FalseColour", "Graphics", ColourInput.RED);
		this.addInput(falseColor);
		this.addSynonym(falseColor, "FalseColor");

		trueText = new StringInput("TrueText", "Graphics", "TRUE");
		this.addInput(trueText);

		falseText = new StringInput("FalseText", "Graphics", "FALSE");
		this.addInput(falseText);
	}

	public BooleanIndicator() {
	}

	@Override
	public void updateGraphics(double simTime) {
		if (expInput.getValue() == null)
			return;
		if (expInput.getValue().getNextSample(simTime) != 0.0d)
			setTagColour(ShapeModel.TAG_CONTENTS, trueColor.getValue());
		else
			setTagColour(ShapeModel.TAG_CONTENTS, falseColor.getValue());
	}

	@Output(name = "Text",
	 description = "If the property is true, then return TrueText.  If the property is false, then return FalseText.",
	    unitType = DimensionlessUnit.class)
	public String getText(double simTime) {
		if (expInput.getValue() == null)
			return "";
		if (expInput.getValue().getNextSample(simTime) != 0.0d)
			return trueText.getValue();
		else
			return falseText.getValue();
	}

}
