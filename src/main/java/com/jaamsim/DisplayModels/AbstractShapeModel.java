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
package com.jaamsim.DisplayModels;

import com.jaamsim.Graphics.FillEntity;
import com.jaamsim.Graphics.LineEntity;
import com.jaamsim.input.BooleanInput;
import com.jaamsim.input.ColourInput;
import com.jaamsim.input.IntegerInput;
import com.jaamsim.input.Keyword;
import com.jaamsim.math.Color4d;

/**
 * DisplayModel for two dimensional objects that can be filled and/or outlined in specified
 * colours.
 * @author Harry King
 *
 */
public abstract class AbstractShapeModel extends DisplayModel implements LineEntity, FillEntity {

	@Keyword(description = "Determines whether or not the object is filled. "
	                     + "If TRUE, it is filled with a specified colour. "
	                     + "If FALSE, it is hollow.",
	         exampleList = {"FALSE"})
	protected final BooleanInput filled;

	@Keyword(description = "The colour with which the object is filled.",
	         exampleList = {"red"})
	protected final ColourInput fillColour;

	@Keyword(description = "Determines whether or not the object is outlined. "
	                     + "If TRUE, it is outlined with a specified colour. "
	                     + "If FALSE, it is drawn without an outline.",
	         exampleList = {"FALSE"})
	protected final BooleanInput outlined;

	@Keyword(description = "The colour with which the object is outlined.",
	         exampleList = {"red"})
	protected final ColourInput lineColour;

	@Keyword(description = "Width of the outline in pixels.",
	         exampleList = { "3" })
	protected final IntegerInput lineWidth;

	{
		fillColour = new ColourInput("FillColour", KEY_INPUTS, ColourInput.MED_GREY);
		this.addInput(fillColour);
		this.addSynonym(fillColour, "FillColor");

		lineColour = new ColourInput("LineColour", KEY_INPUTS, ColourInput.BLACK);
		this.addInput(lineColour);
		this.addSynonym(lineColour, "OutlineColour");
		this.addSynonym(lineColour, "OutlineColor");

		filled = new BooleanInput("Filled", KEY_INPUTS, false);
		this.addInput(filled);

		outlined = new BooleanInput("Outlined", KEY_INPUTS, false);
		this.addInput(outlined);

		lineWidth = new IntegerInput("LineWidth", KEY_INPUTS, 1);
		lineWidth.setValidRange(0, Integer.MAX_VALUE);
		this.addInput(lineWidth);
	}

	public AbstractShapeModel() {}

	@Override
	public boolean isFilled() {
		return filled.getValue();
	}

	@Override
	public boolean isOutlined() {
		return outlined.getValue();
	}

	@Override
	public int getLineWidth() {
		return lineWidth.getValue();
	}

	@Override
	public Color4d getFillColour(double simTime) {
		return fillColour.getValue();
	}

	@Override
	public Color4d getLineColour(double simTime) {
		return lineColour.getValue();
	}

}
