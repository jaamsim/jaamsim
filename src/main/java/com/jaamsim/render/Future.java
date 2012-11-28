/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2012 Ausenco Engineering Canada Inc.
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
