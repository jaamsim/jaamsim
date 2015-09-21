/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2015 Ausenco Engineering Canada Inc.
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
