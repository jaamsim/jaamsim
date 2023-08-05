/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2009-2011 Ausenco Engineering Canada Inc.
 * Copyright (C) 2017-2023 JaamSim Software Inc.
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
import com.jaamsim.DisplayModels.PolylineModel;
import com.jaamsim.input.ColourInput;
import com.jaamsim.input.IntegerInput;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.Vec3dInput;
import com.jaamsim.math.Color4d;
import com.jaamsim.math.Vec3d;
import com.jaamsim.units.DistanceUnit;

public class Arrow extends DisplayEntity implements LineEntity {

	@Keyword(description = "The colour of the arrow.",
	         exampleList = {"red"})
	private final ColourProvInput color;

	@Keyword(description = "The width of the Arrow line segments in pixels.",
	         exampleList = {"1"})
	private final IntegerInput width;

	@Keyword(description = "A set of (x, y, z) numbers that define the size of the arrowhead.",
	         exampleList = {"0.165 0.130 0.0 m"})
	private final Vec3dInput arrowHeadSize;

	{
		displayModelListInput.clearValidClasses();
		displayModelListInput.addValidClass(PolylineModel.class);

		color = new ColourProvInput("LineColour", FORMAT, ColourInput.BLACK);
		color.setDefaultText("PolylineModel");
		this.addInput(color);
		this.addSynonym(color, "Color");
		this.addSynonym(color, "Colour");

		width = new IntegerInput("LineWidth", FORMAT, 1);
		width.setValidRange(1, Integer.MAX_VALUE);
		width.setDefaultText("PolylineModel");
		this.addInput(width);
		this.addSynonym(width, "Width");

		arrowHeadSize = new Vec3dInput( "ArrowHeadSize", FORMAT, new Vec3d(0.1d, 0.1d, 0.0d) );
		arrowHeadSize.setUnitType(DistanceUnit.class);
		arrowHeadSize.setDefaultText("PolylineModel");
		this.addInput( arrowHeadSize );
		this.addSynonym(arrowHeadSize, "ArrowSize");
	}

	public Arrow() {}

	public Vec3d getArrowHeadSize() {
		if (arrowHeadSize.isDefault()) {
			PolylineModel model = getDisplayModel(PolylineModel.class);
			if (model != null)
				return model.getArrowHeadSize();
		}
		return arrowHeadSize.getValue();
	}

	@Override
	public boolean isOutlined() {
		return true;
	}

	@Override
	public int getLineWidth() {
		if (width.isDefault()) {
			LineEntity model = getDisplayModel(LineEntity.class);
			if (model != null)
				return model.getLineWidth();
		}
		return width.getValue();
	}

	@Override
	public Color4d getLineColour(double simTime) {
		if (color.isDefault()) {
			LineEntity model = getDisplayModel(LineEntity.class);
			if (model != null)
				return model.getLineColour(simTime);
		}
		return color.getNextColour(this, simTime);
	}

}
