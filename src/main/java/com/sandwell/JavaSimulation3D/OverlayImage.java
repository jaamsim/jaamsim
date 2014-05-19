/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2013 Ausenco Engineering Canada Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 */
package com.sandwell.JavaSimulation3D;

import com.jaamsim.input.Keyword;
import com.sandwell.JavaSimulation.IntegerListInput;
import com.sandwell.JavaSimulation.IntegerVector;

/**
 * OverlayImage displays a 2D image (JPG, PNG, etc.) as an overlay on a View window.
 * @author Matt Chudleight, with modifications by Harry King
 *
 */
public class OverlayImage extends OverlayEntity {

	@Keyword(description = "The size of the image. Value is in pixels",
	         example = "OverlayImage1 ImageSize { 200 100 }")
	private final IntegerListInput size;

	{
		IntegerVector defSize = new IntegerVector(2);
		defSize.add(100);
		defSize.add(100);
		size = new IntegerListInput("ImageSize", "Basic Graphics", defSize);
		size.setValidCount(2);
		size.setValidRange(0, 2500);
		this.addInput(size);
	}

	public IntegerVector getImageSize() {
		return size.getValue();
	}
}
