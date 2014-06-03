/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2014 Ausenco Engineering Canada Inc.
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
package com.jaamsim.Thresholds;

/**
 * SignalThreshold is a type of Threshold that is controlled directly by
 * another object. At present, it is required only for EntitySignal.
 * @author Harry
 *
 */
public class SignalThreshold extends Threshold {

	@Override
	public void startUp() {
		super.startUp();
		this.setOpen(false);
	}

}
