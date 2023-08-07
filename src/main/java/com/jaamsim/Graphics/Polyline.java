/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2019-2023 JaamSim Software Inc.
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
import com.jaamsim.Samples.SampleInput;
import com.jaamsim.input.BooleanInput;
import com.jaamsim.input.Keyword;
import com.jaamsim.units.DistanceUnit;

/**
 * A series of nodes that are connected by straight or curved lines. A filled polyline can be used
 * to generate an arbitrary two dimensional shape.
 * @author Harry King
 *
 */
public class Polyline extends AbstractShape implements PolylineEntity  {

	@Keyword(description = "Determines whether or not to show a line between the last point of "
	                     + "the polyline and its first point. "
	                     + "If TRUE, the closing line is displayed. "
	                     + "If FALSE, the closing line is not displayed.",
	         exampleList = {"TRUE", "FALSE"})
	protected final BooleanInput closed;

	@Keyword(description = "Physical width of the polyline with units of distance.",
	         exampleList = { "0.5 m" })
	protected final SampleInput polylineWidth;

	{
		displayModelListInput.clearValidClasses();
		displayModelListInput.addValidClass(PolylineModel.class);

		outlined.setDefaultValue(true);

		closed = new BooleanInput("Closed", FORMAT, false);
		closed.setDefaultText("DisplayModel value");
		this.addInput(closed);

		polylineWidth = new SampleInput("PolylineWidth", FORMAT, 0.0d);
		polylineWidth.setUnitType(DistanceUnit.class);
		polylineWidth.setValidRange(0.0d, Double.POSITIVE_INFINITY);
		polylineWidth.setDefaultText("DisplayModel value");
		this.addInput(polylineWidth);
	}

	public Polyline() {}

	@Override
	public boolean isClosed() {
		return isClosed(0.0d);
	}

	@Override
	public boolean isClosed(double simTime) {
		if (closed.isDefault()) {
			PolylineEntity model = getDisplayModel(PolylineEntity.class);
			if (model != null)
				return model.isClosed(simTime);
		}
		return closed.getValue();
	}

	@Override
	public double getPolylineWidth() {
		return getPolylineWidth(0.0d);
	}

	@Override
	public double getPolylineWidth(double simTime) {
		if (polylineWidth.isDefault()) {
			PolylineEntity model = getDisplayModel(PolylineEntity.class);
			if (model != null)
				return model.getPolylineWidth(simTime);
		}
		return polylineWidth.getNextSample(this, simTime);
	}

}
