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

/**
 * Cube Observers are a very dumb test observer that simply draws a coloured cube in the place
 * that the DisplayEntity claims to be, mostly for testing
 * @author Matt.Chudleigh
 *
 */
public class CubeObserverProto extends ObserverProto {

	protected final ColourInput colourInput;

	public CubeObserverProto() {
		colourInput = new ColourInput("Colour", "Key Inputs", ColourInput.LIGHT_GREY);
		this.addInput(colourInput, true);
	}

	@Override
	public Observer instantiate() {
		return new CubeObserver(observeeInput.getValue());
	}

	@Override
	public Observer instantiate(Entity observee) {
		return new CubeObserver(observee);
	}
}
