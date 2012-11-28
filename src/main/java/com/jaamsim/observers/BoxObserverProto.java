/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2012 Ausenco Engineering Canada Inc.
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
package com.jaamsim.observers;

import com.sandwell.JavaSimulation.ColourInput;
import com.sandwell.JavaSimulation.Entity;

public class BoxObserverProto extends ObserverProto {

	protected final ColourInput colourInput;

	public BoxObserverProto() {
		colourInput = new ColourInput("Colour", "Key Inputs", ColourInput.LIGHT_GREY);
		this.addInput(colourInput, true);
	}

	@Override
	public Observer instantiate() {
		return new BoxObserver(observeeInput.getValue(), colourInput.getValue());
	}

	@Override
	public Observer instantiate(Entity observee) {
		return new BoxObserver(observee, colourInput.getValue());
	}

}
