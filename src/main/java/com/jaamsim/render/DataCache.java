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

/**
 * DataCache is a class used to cache data needed by a display model when determining if a set of proxies needs to be updated
 * @author matt.chudleigh
 *
 */
public class DataCache<T> {

	private T cachedValue = null;

	/**
	 * Updates the cached value and returns if the new value is equal to the cached value
	 * @param val
	 * @return
	 */
	public boolean checkValue(T val) {
		boolean valIsNull = val == null;
		boolean cachedIsNull = cachedValue == null;

		boolean ret = ((valIsNull && cachedIsNull) || (!valIsNull && val.equals(cachedValue)));

		cachedValue = val;

		return ret;
	}
}
