/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2016-2023 JaamSim Software Inc.
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
import com.jaamsim.DisplayModels.ShapeModel;
import com.jaamsim.Samples.SampleInput;
import com.jaamsim.input.ColourInput;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.Output;
import com.jaamsim.math.Vec3d;
import com.jaamsim.units.DimensionlessUnit;

public class BarGauge extends DisplayEntity {

	@Keyword(description = "Height of the bar expressed as a value between 0 and 1. Values "
	                     + "outside this range will be truncated.",
	         exampleList = {"0.5", "'0.5 + 0.5*sin(this.SimTime/10[s])'"})
	private final SampleInput dataSource;

	@Keyword(description = "Colour of the bar.",
	         exampleList = {"red"})
	private final ColourProvInput colour;

	@Keyword(description = "Colour of the gauge's body.",
	         exampleList = {"white"})
	private final ColourProvInput backgroundColour;

	{
		dataSource = new SampleInput("DataSource", KEY_INPUTS, 0.5d);
		dataSource.setUnitType(DimensionlessUnit.class);
		dataSource.setValidRange(0.0d, 1.0d);
		this.addInput(dataSource);

		colour = new ColourProvInput("Colour", KEY_INPUTS, ColourInput.BLUE);
		this.addInput(colour);
		this.addSynonym(colour, "Color");

		backgroundColour = new ColourProvInput("BackgroundColour", KEY_INPUTS, ColourInput.LIGHT_GREY);
		this.addInput(backgroundColour);
		this.addSynonym(backgroundColour, "BackgroundColor");
	}

	@Output(name = "Value",
	 description = "Value displayed by the gauge.",
	    unitType = DimensionlessUnit.class)
	public double getValue(double simTime) {
		double ret = dataSource.getNextSample(this, simTime);
		ret = Math.min(ret, 1.0d);
		ret = Math.max(ret, 0.0d);
		return ret;
	}

	@Override
		public Vec3d getSize() {
		Vec3d ret = super.getSize();
		ret.z = Math.max(ret.z, 0.001d);
		return ret;
	}

	@Override
	public void updateGraphics(double simTime) {
		super.updateGraphics(simTime);

		double val = this.getValue(simTime);
		if (Double.isNaN(val)) {
			val = 0.0d;
		}

		setTagSize(ShapeModel.TAG_CONTENTS, val);
		setTagColour(ShapeModel.TAG_CONTENTS, colour.getNextColour(this, simTime));
		setTagColour(ShapeModel.TAG_BODY, backgroundColour.getNextColour(this, simTime));
	}

}
