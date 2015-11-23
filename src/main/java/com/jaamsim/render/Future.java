/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2012 Ausenco Engineering Canada Inc.
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

package com.jaamsim.render;


/**
 * Future is a promise to return an object of type T
 * The progress can be queried, or the current thread can block until done
 * @author matt.chudleigh
 *
 */
public class Future<T> {

	private final Runnable _runWhenDone;
	private boolean _done = false;
	private boolean _failed = false;
	private String _failureString;

	private final Object _blockingLock = new Object();

	private T _val = null;

	public Future(Runnable runWhenDone) {
		_runWhenDone = runWhenDone;
	}

	public boolean isDone() { return _done; }
	public boolean failed() { return _failed; }

	public void setComplete(T val) {
		synchronized(_blockingLock) {
			_val = val;
			_done = true;

			if (_runWhenDone != null) {
				_runWhenDone.run();
			}

			_blockingLock.notifyAll();
		}
	}

	/**
	 * Set this FutureImage into a failed state (ie: an error occurred during rendering)
	 */
	public void setFailed(String errorMsg) {
		synchronized(_blockingLock) {
			_done = true;
			_failed = true;
			_val = null;
			_failureString = errorMsg;

			if (_runWhenDone != null) {
				_runWhenDone.run();
			}

			_blockingLock.notifyAll();
		}
	}

	public String getFailureMessage() {
		return _failureString;
	}

	/**
	 * Block this thread until the image is ready, DO NOT call this from the render thread
	 * or the renderer will deadlock
	 */
	public void blockUntilDone() {
		synchronized(_blockingLock) {
			while (!_done) {
				try {
					_blockingLock.wait();
				} catch (InterruptedException e) {
					// Ignore
				}
			}
		}
	}

	public T get() {
		if (!_done && !_failed) {
			return null;
		}

		return _val;
	}
}
