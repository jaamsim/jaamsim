/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2014-2015 Ausenco Engineering Canada Inc.
 * Copyright (C) 2022-2024 JaamSim Software Inc.
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

import com.jaamsim.ColourProviders.ColourProvInput;
import com.jaamsim.DisplayModels.ShapeModel;
import com.jaamsim.Graphics.DisplayEntity;
import com.jaamsim.Samples.SampleInput;
import com.jaamsim.input.ColourInput;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.Output;
import com.jaamsim.input.StringInput;
import com.jaamsim.units.DimensionlessUnit;

public class BooleanIndicator extends DisplayEntity {

	@Keyword(description = "An expression returning a boolean value: zero = FALSE, "
	                     + "non-zero = TRUE.",
	         exampleList = {"'[Queue1].QueueLength > 2'", "[Server1].Working"})
	private final SampleInput expInput;

	@Keyword(description = "The colour of the indicator when the DataSource expression is TRUE.")
	private final ColourProvInput trueColor;

	@Keyword(description = "The colour of the indicator when the DataSource expression is FALSE.")
	private final ColourProvInput falseColor;

	@Keyword(description = "The string returned by the Text output when the DataSource expression "
	                     + "is TRUE.",
	         exampleList = {"'True text'"})
	private final StringInput trueText;

	@Keyword(description = "The string returned by the Text output when the DataSource expression "
	                     + "is FALSE.",
	         exampleList = {"'False text'"})
	private final StringInput falseText;

	{
		expInput = new SampleInput("DataSource", KEY_INPUTS, Double.NaN);
		expInput.setUnitType(DimensionlessUnit.class);
		expInput.setRequired(true);
		this.addInput(expInput);
		this.addSynonym(expInput, "OutputName");

		trueColor = new ColourProvInput("TrueColour", KEY_INPUTS, ColourInput.GREEN);
		this.addInput(trueColor);
		this.addSynonym(trueColor, "TrueColor");

		falseColor = new ColourProvInput("FalseColour", KEY_INPUTS, ColourInput.RED);
		this.addInput(falseColor);
		this.addSynonym(falseColor, "FalseColor");

		trueText = new StringInput("TrueText", KEY_INPUTS, "TRUE");
		this.addInput(trueText);

		falseText = new StringInput("FalseText", KEY_INPUTS, "FALSE");
		this.addInput(falseText);
	}

	public BooleanIndicator() {
	}

	@Override
	public void updateGraphics(double simTime) {
		super.updateGraphics(simTime);

		if (expInput.isDefault())
			return;
		if (expInput.getNextSample(this, simTime) != 0.0d)
			setTagColour(ShapeModel.TAG_CONTENTS, trueColor.getNextColour(this, simTime));
		else
			setTagColour(ShapeModel.TAG_CONTENTS, falseColor.getNextColour(this, simTime));
	}

	@Output(name = "Text",
	 description = "If the DataSource expression is TRUE, then return TrueText. "
	             + "If it is FALSE, then return FalseText.",
	    unitType = DimensionlessUnit.class)
	public String getText(double simTime) {
		if (expInput.isDefault())
			return "";
		if (expInput.getNextSample(this, simTime) != 0.0d)
			return trueText.getValue();
		else
			return falseText.getValue();
	}

}
