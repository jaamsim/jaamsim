/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2011 Ausenco Engineering Canada Inc.
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
package com.sandwell.JavaSimulation;

/**
 * A Time-Varying-Value implementation to bridge the discrete->continuous value
 * gap.
 */
public class TVV {

private double datumTime = 0.0d;
private double datumVal = 0.0d;
private double datumRate = 0.0d;

public TVV() {}

public double getValueAtTime(double time) {
	double dt = time - datumTime;

	// Required for a rate of infinity
	if( dt == 0.0 )
		return datumVal;

	return datumVal + dt * datumRate;
}

public void setValue(double time, double val) {
	this.set(time, val, datumRate);
}

public void setRate(double time, double rate) {
	double newDatum = getValueAtTime(time);
	set(time, newDatum, rate);
}

public void set(double time, double val, double rate) {
	datumVal = val;
	datumRate = rate;
	datumTime = time;
}

public double getRate() {
	return datumRate;
}

}
