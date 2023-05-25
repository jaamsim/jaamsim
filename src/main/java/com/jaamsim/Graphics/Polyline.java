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
import com.jaamsim.input.BooleanInput;
import com.jaamsim.input.Keyword;

/**
 * A series of nodes that are connected by straight or curved lines. A filled polyline can be used
 * to generate an arbitrary two dimensional shape.
 * @author Harry King
 *
 */
public class Polyline extends AbstractShape  {

	@Keyword(description = "Determines whether or not to show a line between the last point of "
	                     + "the polyline and its first point. "
	                     + "If TRUE, the closing line is displayed. "
	                     + "If FALSE, the closing line is not displayed.",
	         exampleList = {"TRUE", "FALSE"})
	protected final BooleanInput closed;

	{
		displayModelListInput.clearValidClasses();
		displayModelListInput.addValidClass(PolylineModel.class);

		outlined.setDefaultValue(true);

		closed = new BooleanInput("Closed", FORMAT, false);
		closed.setDefaultText("DisplayModel value");
		this.addInput(closed);
	}

	public Polyline() {}

	public boolean isClosed() {
		if (closed.isDefault() && getDisplayModel() instanceof PolylineModel) {
			PolylineModel model = (PolylineModel) getDisplayModel();
			return model.isClosed();
		}
		return closed.getValue();
	}

}
