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

package com.jaamsim.render;

import com.jaamsim.input.OutputHandle;

public class Action {

	public static class Description {
		public String name;
		public double duration;
	}

	public static class Queue {
		public String name;
		public double time;
	}

	/**
	 * Used to bind entity outputs to active actions
	 * @author matt.chudleigh
	 *
	 */
	public static class Binding {
		public String actionName;
		public String outputName;
		public OutputHandle outputHandle;
	}
}
