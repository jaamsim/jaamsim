/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2015 Ausenco Engineering Canada Inc.
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
package com.jaamsim.input;

import java.util.ArrayList;

import com.jaamsim.basicsim.Entity;

public class OutputChain {

	private final Entity ent;  // The Entity against which to apply the first Output name
	private final String outputName;  // The first Output name in the chain
	private final OutputHandle out;  // The OutputHandle for the first Output in the chain
	private final ArrayList<String> outputNameList;  // The names of the second, third, etc. Outputs in the chain.

	public OutputChain(Entity e, String outName, OutputHandle o, ArrayList<String> outNameList) {
		ent = e;
		outputName = outName;
		out = o;
		outputNameList = outNameList;
	}

	public OutputHandle getOutputHandle(double simTime) {
		OutputHandle o = out;
		for (String name : outputNameList) {
			Entity e = o.getValue(simTime, Entity.class);
			if (e == null || !e.hasOutput(name))
				return null;
			o = e.getOutputHandle(name);
		}
		return o;
	}

	@Override
	public String toString() {
		String str = String.format("[%s].%s", ent.getName(), outputName);
		StringBuilder sb = new StringBuilder(str);
		for (String outName : outputNameList) {
			sb.append(".").append(outName);
		}
		return sb.toString();
	}

}
