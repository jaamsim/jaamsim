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
package com.jaamsim.events;

class WaitTarget extends ProcessTarget {
	private Process proc;

	WaitTarget(Process p) {
		proc = p;
	}

	@Override
	Process getProcess() { return proc; }

	@Override
	public String getDescription() {
		StackTraceElement[] callStack = proc.getStackTrace();
		boolean seenEntity = false;
		for (int i = 0; i < callStack.length; i++) {
			if (callStack[i].getClassName().equals("com.sandwell.JavaSimulation.Entity")) {
				seenEntity = true;
				continue;
			}

			if (seenEntity)
				return String.format("%s:%s", callStack[i].getClassName(), callStack[i].getMethodName());
		}

		// Possible the process hasn't started running yet, check the Process target
		// state
		return "Unknown Method State";

	}

	@Override
	public void process() {}
}
