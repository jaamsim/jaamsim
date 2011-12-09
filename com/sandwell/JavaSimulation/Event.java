/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2002-2011 Ausenco Engineering Canada Inc.
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
 * Holder class for event data used by the event monitor to schedule future
 * events.
 */
class Event {

	final long eventTime;
	final int priority;
	final long creationTime;
	final Process process;
	final Entity caller;

	/**
	 * Constructs a new event object.
	 *
	 * @param neweventTime the simulation time the event is to be scheduled for
	 * @param newthread the thread that requires scheduling
	 * @param newpriority the priority of the event
	 */
	Event(long neweventTime, int newpriority, Entity caller, Process process) {
		eventTime = neweventTime;
		this.process = process;
		priority = newpriority;
		this.caller = caller;
		creationTime = process.getEventManager().currentTime();
	}

	public String getClassMethod() {
		StackTraceElement[] callStack = process.getStackTrace();

		for (int i = 0; i < callStack.length; i++) {
			if (callStack[i].getClassName().equals("com.sandwell.JavaSimulation.Entity")) {
				return String.format("%s.%s", caller.getClass().getSimpleName(), callStack[i + 1].getMethodName());
			}
		}

		// Possible the process hasn't started running yet, check the Process target
		// state
		return process.getClassMethod();
	}

	public String getFileLine() {
		StackTraceElement[] callStack = process.getStackTrace();

		for (int i = 0; i < callStack.length; i++) {
			if (callStack[i].getClassName().equals("com.sandwell.JavaSimulation.Entity")) {
				return String.format("%s:%s", callStack[i + 1].getFileName(), callStack[i + 1].getLineNumber());
			}
		}
		return "Unknown method state";
	}

	public String[] getData(int state) {
		String[] data = new String[10];

		data[0] = String.format("%15d", eventTime);
		data[1] = String.format("%15.3f", eventTime / Simulation.getSimTimeFactor());
		data[2] = String.format("%5d", priority);
		data[3] = String.format("%s", caller.getName());
		data[4] = String.format("%s", caller.getCurrentRegion().getName());
		data[5] = String.format("%s", "");
		data[6] = getClassMethod();
		data[7] = getFileLine();
		data[8] = String.format("%15.3f", creationTime / Simulation.getSimTimeFactor());
		data[9] = "Unknown";

		switch (state) {
		case EventManager.STATE_WAITING:
			data[9] = "Waiting";
			break;
		case EventManager.STATE_EXITED:
			data[9] = "Ran Normally";
			break;
		case EventManager.STATE_INTERRUPTED:
			data[9] = "Interrupted";
			break;
		case EventManager.STATE_TERMINATED:
			data[9] = "Terminated";
			break;
		}

		return data;
	}
}
