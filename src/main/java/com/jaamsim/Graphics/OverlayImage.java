/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2013 Ausenco Engineering Canada Inc.
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

import com.jaamsim.DisplayModels.IconModel;
import com.jaamsim.DisplayModels.ImageModel;
import com.jaamsim.datatypes.IntegerVector;
import com.jaamsim.input.IntegerListInput;
import com.jaamsim.input.Keyword;

/**
 * OverlayImage displays a 2D image (JPG, PNG, etc.) as an overlay on a View window.
 * @author Matt Chudleight, with modifications by Harry King
 *
 */
public class OverlayImage extends OverlayEntity {

	@Keyword(description = "The size of the image. Value is in pixels",
	         exampleList = {"200 100"})
	private final IntegerListInput size;

	{
		displayModelListInput.clearValidClasses();
		displayModelListInput.addValidClass(ImageModel.class);
		displayModelListInput.addInvalidClass(IconModel.class);

		IntegerVector defSize = new IntegerVector(2);
		defSize.add(100);
		defSize.add(100);
		size = new IntegerListInput("ImageSize", GRAPHICS, defSize);
		size.setValidCount(2);
		size.setValidRange(0, 2500);
		this.addInput(size);
	}

	public IntegerVector getImageSize() {
		return size.getValue();
	}
}
