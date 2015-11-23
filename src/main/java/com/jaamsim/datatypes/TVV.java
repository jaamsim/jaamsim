/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2011 Ausenco Engineering Canada Inc.
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
package com.jaamsim.datatypes;

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
