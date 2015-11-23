/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2014 Ausenco Engineering Canada Inc.
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
package com.jaamsim.events;

class WaitTarget extends ProcessTarget {
	private Process proc;

	WaitTarget(Process p) {
		proc = p;
	}

	@Override
	Process getProcess() { return proc; }

	@Override
	void kill() {
		proc.kill();
	}

	@Override
	public String getDescription() {
		return "Waiting";
	}

	@Override
	public void process() {}
}
