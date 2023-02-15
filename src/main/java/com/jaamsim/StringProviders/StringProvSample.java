/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2015 Ausenco Engineering Canada Inc.
 * Copyright (C) 2017-2022 JaamSim Software Inc.
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
package com.jaamsim.StringProviders;

import com.jaamsim.Samples.SampleProvider;
import com.jaamsim.basicsim.Entity;

public class StringProvSample implements StringProvider {
	private final SampleProvider samp;

	public StringProvSample(SampleProvider s) {
		samp = s;
	}

	@Override
	public String getNextString(Entity thisEnt, double simTime) {
		return Double.toString(samp.getNextSample(thisEnt, simTime));
	}

	@Override
	public String getNextString(Entity thisEnt, double simTime, double siFactor) {
		return Double.toString(samp.getNextSample(thisEnt, simTime)/siFactor);
	}

	@Override
	public String getNextString(Entity thisEnt, double simTime, double siFactor, boolean integerValue) {
		if (integerValue) {
			return Double.toString((int)(samp.getNextSample(thisEnt, simTime)/siFactor));
		}
		else {
			return Double.toString(samp.getNextSample(thisEnt, simTime)/siFactor);
		}
	}

	@Override
	public String getNextString(Entity thisEnt, double simTime, String fmt, double siFactor) {
		return String.format(fmt, samp.getNextSample(thisEnt, simTime)/siFactor);
	}

	@Override
	public double getNextValue(Entity thisEnt, double simTime) {
		return samp.getNextSample(thisEnt, simTime);
	}

	@Override
	public String toString() {
		return samp.toString();
	}

	public SampleProvider getSampleProvider() {
		return samp;
	}

}
