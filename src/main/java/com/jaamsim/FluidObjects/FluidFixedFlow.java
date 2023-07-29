/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2013 Ausenco Engineering Canada Inc.
 * Copyright (C) 2018-2023 JaamSim Software Inc.
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
package com.jaamsim.FluidObjects;

import com.jaamsim.DisplayModels.PolylineModel;
import com.jaamsim.Graphics.FillEntity;
import com.jaamsim.Graphics.LineEntity;
import com.jaamsim.Graphics.PolylineEntity;
import com.jaamsim.input.ColourInput;
import com.jaamsim.input.IntegerInput;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.ValueInput;
import com.jaamsim.math.Color4d;
import com.jaamsim.units.DistanceUnit;
import com.jaamsim.units.VolumeFlowUnit;

/**
 * FluidFixedFlow models a specified flow rate between a source and destination.
 * A null source is taken to be an infinite reservoir to supply fluid.
 * A null destination is taken to be an infinite reservoir to receive fluid.
 * @author Harry King
 *
 */
public class FluidFixedFlow extends FluidFlowCalculation implements LineEntity, FillEntity, PolylineEntity {

	@Keyword(description = "The constant volumetric flow rate from the source to the destination.",
	         exampleList = {"1.0 m3/s"})
	private final ValueInput flowRateInput;

	@Keyword(description = "Physical width of the pipe segments with units of distance.",
	         exampleList = { "0.5 m" })
	protected final ValueInput polylineWidth;

	@Keyword(description = "The width of the pipe segments in pixels.",
	         exampleList = {"1"})
	private final IntegerInput widthInput;

	@Keyword(description = "The colour of the pipe.",
	         exampleList = {"red"})
	private final ColourInput colourInput;

	{
		displayModelListInput.clearValidClasses();
		displayModelListInput.addValidClass(PolylineModel.class);

		flowRateInput = new ValueInput( "FlowRate", KEY_INPUTS, 0.0d);
		flowRateInput.setValidRange( 0.0d, Double.POSITIVE_INFINITY);
		flowRateInput.setUnitType( VolumeFlowUnit.class );
		this.addInput( flowRateInput);

		polylineWidth = new ValueInput("PolylineWidth", FORMAT, 0.0d);
		polylineWidth.setUnitType(DistanceUnit.class);
		polylineWidth.setValidRange(0.0d, Double.POSITIVE_INFINITY);
		polylineWidth.setDefaultText("PolylineModel");
		this.addInput(polylineWidth);

		widthInput = new IntegerInput("LineWidth", FORMAT, 1);
		widthInput.setValidRange(1, Integer.MAX_VALUE);
		widthInput.setDefaultText("PolylineModel");
		this.addInput(widthInput);
		this.addSynonym(widthInput, "Width");

		colourInput = new ColourInput("LineColour", FORMAT, ColourInput.BLACK);
		colourInput.setDefaultText("PolylineModel");
		colourInput.setHidden(true);
		this.addInput(colourInput);
		this.addSynonym(colourInput, "Color");
		this.addSynonym(colourInput, "Colour");
	}

	@Override
	protected void calcFlowRate(FluidComponent source, FluidComponent destination, double dt) {

		// Update the flow rate
		this.setFlowRate( flowRateInput.getValue() );
	}

	@Override
	public boolean isOutlined() {
		return (getPolylineWidth() <= 0.0d);
	}

	@Override
	public int getLineWidth() {
		if (widthInput.isDefault()) {
			LineEntity model = getDisplayModel(LineEntity.class);
			if (model != null)
				return model.getLineWidth();
		}
		return widthInput.getValue();
	}

	@Override
	public Color4d getLineColour() {
		return getLineColour(0.0d);
	}

	@Override
	public Color4d getLineColour(double simTime) {
		if (colourInput.isDefault()) {
			LineEntity model = getDisplayModel(LineEntity.class);
			if (model != null)
				return model.getLineColour(simTime);
		}
		return colourInput.getValue();
	}

	@Override
	public boolean isFilled() {
		return false;
	}

	@Override
	public Color4d getFillColour() {
		return getFillColour(0.0d);
	}

	@Override
	public Color4d getFillColour(double simTime) {
		if (getFluid() == null)
			return ColourInput.BLACK;
		return getFluid().getColour();
	}

	@Override
	public boolean isClosed() {
		return false;
	}

	@Override
	public double getPolylineWidth() {
		if (polylineWidth.isDefault()) {
			PolylineEntity model = getDisplayModel(PolylineEntity.class);
			if (model != null)
				return model.getPolylineWidth();
		}
		return polylineWidth.getValue();
	}

}
