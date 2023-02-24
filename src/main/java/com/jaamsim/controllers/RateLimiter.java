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
package com.jaamsim.controllers;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A class to limit updates to a set rate in real time
 * @author matt.chudleigh
 *
 */
public class RateLimiter implements Runnable {
	private long lastCallbackTime = 0;
	private final AtomicLong schedTime = new AtomicLong(Long.MAX_VALUE);
	private final Object timingLock = new Object();
	private final long frameTime;

	private final ArrayList<Runnable> callbacks = new ArrayList<>();

	public static RateLimiter create(double updatesPerSecond) {
		RateLimiter ret = new RateLimiter(updatesPerSecond);
		Thread refreshThread = new Thread(ret, "RefreshThread");
		refreshThread.setDaemon(true);
		refreshThread.start();
		return ret;
	}

	private RateLimiter(double updatesPerSecond) {
		// Start the display timer
		frameTime = (long)(1000.0d / updatesPerSecond);
	}

	@Override
	public void run() {
		synchronized(timingLock) {
			while (true) {
				// Is a redraw scheduled
				long currentTime = System.currentTimeMillis();
				long waitLength = schedTime.get() - currentTime;
				if (waitLength > 0) {
					try {
						// We have a scheduled time, wait until then
						timingLock.wait(waitLength);
					}
					catch(InterruptedException ex) {}
					continue;
				}

				lastCallbackTime = currentTime;
				schedTime.set(Long.MAX_VALUE);
				for (Runnable r : callbacks) {
					r.run();
				}
			}
		}
	}

	public void queueUpdate() {
		// If we already are schedule to run, don't wake the thread
		if (schedTime.get() != Long.MAX_VALUE)
			return;

		synchronized(timingLock) {
			// Set a new target callback time based on the last time a callback was
			// actually done
			schedTime.set(lastCallbackTime + frameTime);
			timingLock.notify();
		}
	}

	public void registerCallback(Runnable r) {
		synchronized (timingLock) {
			callbacks.add(r);
		}
	}
}
