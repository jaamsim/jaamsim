/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2014 Ausenco Engineering Canada Inc.
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
package com.jaamsim.Graphics;

import com.jaamsim.units.DimensionlessUnit;

/**
 * BillboardText is a DisplayEntity used to display billboarded text labels
 * @author matt.chudleigh
 *
 */
public class BillboardText extends Text {

	{
		// Set the default text height to 10 pixels
		textHeight.setUnitType(DimensionlessUnit.class);
		textHeight.setDefaultValue(10.0);

		// Alignment and AutoSize inputs are ignored
		alignmentInput.setHidden(true);
		autoSize.setHidden(true);
		autoSize.setDefaultValue(false);
	}

	public BillboardText() {}

	@Override
	public double getTextHeight(double simTime) {
		if (textHeight.isDefault()) {
			return getTextModel().getTextHeightInPixels(simTime);
		}
		return textHeight.getNextSample(this, simTime);
	}

	@Override
	public String getTextHeightString() {
		if (textHeight.isDefault()) {
			return getTextModel().getTextHeightInPixelsString();
		}
		return textHeight.getValueString();
	}

}
