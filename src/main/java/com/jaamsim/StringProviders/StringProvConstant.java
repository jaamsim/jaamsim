/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2018-2021 JaamSim Software Inc.
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

public class StringProvConstant implements StringProvider {

	private final String val;

	public StringProvConstant(String str) {
		val = str;
	}

	@Override
	public String getNextString(double simTime) {
		return val;
	}

	@Override
	public String getNextString(double simTime, double siFactor) {
		return val;
	}

	@Override
	public String getNextString(double simTime, double siFactor, boolean integerValue) {
		return val;
	}

	@Override
	public String getNextString(double simTime, String fmt, double siFactor) {
		return String.format(fmt, val);
	}

	@Override
	public double getNextValue(double simTime) {
		return Double.NaN;
	}

	@Override
	public String toString() {
		return val;
	}

}
