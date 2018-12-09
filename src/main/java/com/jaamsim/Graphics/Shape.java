/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2018 JaamSim Software Inc.
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

import com.jaamsim.DisplayModels.ShapeModel;
import com.jaamsim.input.BooleanInput;
import com.jaamsim.input.ColourInput;
import com.jaamsim.input.Input;
import com.jaamsim.input.IntegerInput;
import com.jaamsim.input.Keyword;
import com.jaamsim.math.Color4d;
import com.jaamsim.ui.GUIFrame;

public class Shape extends DisplayEntity implements LineEntity, FillEntity {

	@Keyword(description = "Determines whether or not the shape is filled. "
	                     + "If TRUE, it is filled with a specified colour. "
	                     + "If FALSE, it is hollow.",
	         exampleList = {"FALSE"})
	private final BooleanInput filled;

	@Keyword(description = "The colour with which the shape is filled.",
	         exampleList = {"red"})
	private final ColourInput fillColour;

	@Keyword(description = "Determines whether or not the shape is outlined. "
	                     + "If TRUE, it is outlined with a specified colour. "
	                     + "If FALSE, it is drawn without an outline.",
	         exampleList = {"FALSE"})
	private final BooleanInput outlined;

	@Keyword(description = "Width of the outline in pixels.",
	         exampleList = { "3" })
	private final IntegerInput lineWidth;

	@Keyword(description = "The colour with which the shape is outlined.",
	         exampleList = {"red"})
	private final ColourInput lineColour;

	{
		displayModelListInput.clearValidClasses();
		displayModelListInput.addValidClass(ShapeModel.class);

		fillColour = new ColourInput("FillColour", FORMAT, null);
		fillColour.setDefaultText("ShapeModel");
		this.addInput(fillColour);

		lineColour = new ColourInput("LineColour", FORMAT, null);
		lineColour.setDefaultText("ShapeModel");
		this.addInput(lineColour);

		filled = new BooleanInput("Filled", FORMAT, true);
		filled.setDefaultText("ShapeModel");
		this.addInput(filled);

		outlined = new BooleanInput("Outlined", FORMAT, true);
		outlined.setDefaultText("ShapeModel");
		this.addInput(outlined);

		lineWidth = new IntegerInput("LineWidth", FORMAT, 1);
		lineWidth.setValidRange(0, Integer.MAX_VALUE);
		lineWidth.setDefaultText("ShapeModel");
		this.addInput(lineWidth);
	}

	public Shape() {}

	@Override
	public void updateForInput( Input<?> in ) {
		super.updateForInput( in );

		if (in == lineColour || in == outlined || in == lineWidth
				|| in == fillColour || in == filled) {
			if (GUIFrame.getInstance() == null)
				return;
			GUIFrame.getInstance().updateLineButtons();
			return;
		}
	}

	public ShapeModel getShapeModel() {
		return (ShapeModel) getDisplayModel();
	}

	@Override
	public boolean isFilled() {
		if (filled.isDefault())
			return getShapeModel().isFilled();
		return filled.getValue();
	}

	@Override
	public boolean isOutlined() {
		if (outlined.isDefault())
			return getShapeModel().isOutlined();
		return outlined.getValue();
	}

	@Override
	public int getLineWidth() {
		if (lineWidth.isDefault())
			return getShapeModel().getLineWidth();
		return lineWidth.getValue();
	}

	@Override
	public Color4d getFillColour() {
		if (fillColour.isDefault())
			return getShapeModel().getFillColour();
		return fillColour.getValue();
	}

	@Override
	public Color4d getLineColour() {
		if (lineColour.isDefault())
			return getShapeModel().getLineColour();
		return lineColour.getValue();
	}

}
