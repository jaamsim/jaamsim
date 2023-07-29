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
package com.jaamsim.Graphics;

import com.jaamsim.ColourProviders.ColourProvInput;
import com.jaamsim.input.BooleanInput;
import com.jaamsim.input.ColourInput;
import com.jaamsim.input.IntegerInput;
import com.jaamsim.input.Keyword;
import com.jaamsim.math.Color4d;

/**
 * Two dimensional objects that can be filled and/or outlined in specified colours.
 * @author Harry King
 *
 */
public abstract class AbstractShape extends DisplayEntity implements LineEntity, FillEntity {

	@Keyword(description = "Determines whether or not the object is filled. "
	                     + "If TRUE, it is filled with a specified colour. "
	                     + "If FALSE, it is hollow.",
	         exampleList = {"FALSE"})
	protected final BooleanInput filled;

	@Keyword(description = "The colour with which the object is filled.",
	         exampleList = {"red"})
	protected final ColourProvInput fillColour;

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
	protected final IntegerInput lineWidth;

	{
		filled = new BooleanInput("Filled", FORMAT, false);
		filled.setDefaultText("DisplayModel value");
		this.addInput(filled);

		fillColour = new ColourProvInput("FillColour", FORMAT, ColourInput.MED_GREY);
		fillColour.setDefaultText("DisplayModel value");
		this.addInput(fillColour);

		outlined = new BooleanInput("Outlined", FORMAT, false);
		outlined.setDefaultText("DisplayModel value");
		this.addInput(outlined);

		lineColour = new ColourProvInput("LineColour", FORMAT, ColourInput.BLACK);
		lineColour.setDefaultText("DisplayModel value");
		this.addInput(lineColour);

		lineWidth = new IntegerInput("LineWidth", FORMAT, 1);
		lineWidth.setValidRange(0, Integer.MAX_VALUE);
		lineWidth.setDefaultText("DisplayModel value");
		this.addInput(lineWidth);
	}

	public AbstractShape() {}

	@Override
	public boolean isFilled() {
		if (filled.isDefault()) {
			FillEntity model = getDisplayModel(FillEntity.class);
			if (model != null)
				return model.isFilled();
		}
		return filled.getValue();
	}

	@Override
	public Color4d getFillColour(double simTime) {
		if (fillColour.isDefault()) {
			FillEntity model = getDisplayModel(FillEntity.class);
			if (model != null)
				return model.getFillColour(simTime);
		}
		return fillColour.getNextColour(this, simTime);
	}

	@Override
	public boolean isOutlined() {
		if (outlined.isDefault()) {
			LineEntity model = getDisplayModel(LineEntity.class);
			if (model != null)
				return model.isOutlined();
		}
		return outlined.getValue();
	}

	@Override
	public int getLineWidth() {
		if (lineWidth.isDefault()) {
			LineEntity model = getDisplayModel(LineEntity.class);
			if (model != null)
				return model.getLineWidth();
		}
		return lineWidth.getValue();
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

}
