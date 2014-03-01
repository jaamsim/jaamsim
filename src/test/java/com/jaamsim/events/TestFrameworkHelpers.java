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


public class TestFrameworkHelpers {
	static void runEventsToTick(EventManager evt, long tick, long timeoutMS) {
		TestTimeListener tl = new TestTimeListener();
		tl.waitforstop(evt, tick, timeoutMS);
	}

	private static class TestTimeListener implements EventTimeListener {
		Thread waitThread = null;

		@Override
		public void tickUpdate(long tick) {}
		@Override
		public void timeRunning(boolean running) {
			synchronized (this) {
				if (running) return;

				if (waitThread != null)
					waitThread.interrupt();
			}
		}

		public void waitforstop(EventManager evt, long ticks, long timeoutMS) {
			synchronized (this) {
				waitThread = Thread.currentThread();
				evt.setTimeListener(this);
				evt.resume(ticks);

				try { this.wait(timeoutMS); }
				catch (InterruptedException e) {
					waitThread = null;
					evt.setTimeListener(null);
					return;
				}

				evt.setTimeListener(null);
				waitThread = null;
				throw new RuntimeException("Test not complete before timeout");
			}
		}
	}
}
