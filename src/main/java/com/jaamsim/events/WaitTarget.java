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

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;

class WaitTarget extends ProcessTarget {
	private Process proc;
	final Condition cond;
	final AtomicBoolean dieFlag;

	WaitTarget(EventManager evt) {
		proc = Process.current();
		cond = evt.getWaitCondition();
		dieFlag = new AtomicBoolean(false);
	}

	@Override
	Process getProcess() { return proc; }

	@Override
	void kill() {
		dieFlag.set(true);
		eventWake();
	}

	void eventWake() {
		cond.signal();
	}

	@Override
	public String getDescription() {
		return "Waiting";
	}

	@Override
	public void process() {}
}
