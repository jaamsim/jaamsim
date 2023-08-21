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
package com.jaamsim.DisplayModels;

import com.jaamsim.BooleanProviders.BooleanProvInput;
import com.jaamsim.ColourProviders.ColourProvInput;
import com.jaamsim.Graphics.FillEntity;
import com.jaamsim.Graphics.LineEntity;
import com.jaamsim.Samples.SampleInput;
import com.jaamsim.input.ColourInput;
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
	protected final BooleanProvInput filled;

	@Keyword(description = "The colour with which the object is filled.")
	protected final ColourProvInput fillColour;

	@Keyword(description = "Determines whether or not the object is outlined. "
	                     + "If TRUE, it is outlined with a specified colour. "
	                     + "If FALSE, it is drawn without an outline.",
	         exampleList = {"FALSE"})
	protected final BooleanProvInput outlined;

	@Keyword(description = "The colour with which the object is outlined.")
	protected final ColourProvInput lineColour;

	@Keyword(description = "Width of the outline in pixels.",
	         exampleList = { "3" })
	protected final SampleInput lineWidth;

	{
		fillColour = new ColourProvInput("FillColour", KEY_INPUTS, ColourInput.MED_GREY);
		this.addInput(fillColour);
		this.addSynonym(fillColour, "FillColor");

		lineColour = new ColourProvInput("LineColour", KEY_INPUTS, ColourInput.BLACK);
		this.addInput(lineColour);
		this.addSynonym(lineColour, "OutlineColour");
		this.addSynonym(lineColour, "OutlineColor");

		filled = new BooleanProvInput("Filled", KEY_INPUTS, false);
		this.addInput(filled);

		outlined = new BooleanProvInput("Outlined", KEY_INPUTS, false);
		this.addInput(outlined);

		lineWidth = new SampleInput("LineWidth", KEY_INPUTS, 1);
		lineWidth.setValidRange(0, Double.POSITIVE_INFINITY);
		lineWidth.setIntegerValue(true);
		this.addInput(lineWidth);
	}

	public AbstractShapeModel() {}

	@Override
	public boolean isFilled(double simTime) {
		return filled.getNextBoolean(this, simTime);
	}

	@Override
	public boolean isOutlined(double simTime) {
		return outlined.getNextBoolean(this, simTime);
	}

	@Override
	public int getLineWidth(double simTime) {
		return (int) lineWidth.getNextSample(this, simTime);
	}

	@Override
	public Color4d getFillColour(double simTime) {
		return fillColour.getNextColour(this, simTime);
	}

	@Override
	public Color4d getLineColour(double simTime) {
		return lineColour.getNextColour(this, simTime);
	}

}
