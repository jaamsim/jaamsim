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


public class TestFrameworkHelpers {
	public static void runEventsToTick(EventManager evt, long tick, long timeoutMS) {
		TestTimeListener tl = new TestTimeListener();
		tl.waitforstop(evt, tick, timeoutMS);
	}

	private static class TestTimeListener implements EventTimeListener {
		Thread waitThread = null;

		@Override
		public void tickUpdate(long tick) {}
		@Override
		public void timeRunning() {
			synchronized (this) {
				if (EventManager.current().isRunning()) return;

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
				waitThread = null;
			}

			evt.pause();
			evt.setTimeListener(null);
			throw new RuntimeException("Test not complete before timeout");
		}

		@Override
		public void handleError(Throwable t) {
			synchronized (this) {
				if (waitThread != null)
					waitThread.interrupt();
			}
		}
	}
}
