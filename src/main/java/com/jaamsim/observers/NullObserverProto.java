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

import com.sandwell.JavaSimulation.Entity;

public class NullObserverProto extends ObserverProto {

	@Override
	public Observer instantiate() {
		return new NullObserver(observeeInput.getDefaultValue());
	}

	@Override
	public Observer instantiate(Entity observee) {
		return new NullObserver(observee);
	}

	public static Observer defaultObs(Entity observee) {
		return new NullObserver(observee);
	}

}
