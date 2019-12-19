/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2019 JaamSim Software Inc.
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

import com.jaamsim.DisplayModels.PolylineModel;
import com.jaamsim.input.BooleanInput;
import com.jaamsim.input.ColourInput;
import com.jaamsim.input.IntegerInput;
import com.jaamsim.input.Keyword;
import com.jaamsim.math.Color4d;

public class Polyline extends DisplayEntity implements LineEntity, FillEntity {

	@Keyword(description = "Determines whether or not the polyline is filled. "
	                     + "If TRUE, it is filled with a specified colour. "
	                     + "If FALSE, it is hollow.",
	         exampleList = {"FALSE"})
	private final BooleanInput filled;

	@Keyword(description = "The colour with which the polyline is filled.",
	         exampleList = {"red"})
	private final ColourInput fillColour;

	@Keyword(description = "Determines whether or not the polyline is outlined. "
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
		displayModelListInput.addValidClass(PolylineModel.class);

		fillColour = new ColourInput("FillColour", FORMAT, null);
		fillColour.setDefaultText("PolylineModel");
		this.addInput(fillColour);

		lineColour = new ColourInput("LineColour", FORMAT, null);
		lineColour.setDefaultText("PolylineModel");
		this.addInput(lineColour);

		filled = new BooleanInput("Filled", FORMAT, true);
		filled.setDefaultText("PolylineModel");
		this.addInput(filled);

		outlined = new BooleanInput("Outlined", FORMAT, true);
		outlined.setDefaultText("PolylineModel");
		this.addInput(outlined);

		lineWidth = new IntegerInput("LineWidth", FORMAT, 1);
		lineWidth.setValidRange(0, Integer.MAX_VALUE);
		lineWidth.setDefaultText("PolylineModel");
		this.addInput(lineWidth);
	}

	public Polyline() {}

	public PolylineModel getPolylineModel() {
		return (PolylineModel) getDisplayModel();
	}

	@Override
	public boolean isFilled() {
		if (filled.isDefault())
			return getPolylineModel().isFilled();
		return filled.getValue();
	}

	@Override
	public boolean isOutlined() {
		if (outlined.isDefault())
			return getPolylineModel().isOutlined();
		return outlined.getValue();
	}

	@Override
	public int getLineWidth() {
		if (lineWidth.isDefault())
			return getPolylineModel().getLineWidth();
		return lineWidth.getValue();
	}

	@Override
	public Color4d getFillColour() {
		if (fillColour.isDefault())
			return getPolylineModel().getFillColour();
		return fillColour.getValue();
	}

	@Override
	public Color4d getLineColour() {
		if (lineColour.isDefault())
			return getPolylineModel().getLineColour();
		return lineColour.getValue();
	}

}
