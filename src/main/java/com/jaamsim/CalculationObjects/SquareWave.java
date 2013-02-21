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
package com.jaamsim.CalculationObjects;

/**
 * Generates a square wave.
 * @author Harry King
 *
 */
public class SquareWave extends WaveGenerator {

	@Override
	protected double getSignal( double angle ) {
		if( Math.IEEEremainder( angle, 2.0*Math.PI) >= 0.0 ) {
			return 1.0;
		}
		else {
			return -1.0;
		}
	}
}
