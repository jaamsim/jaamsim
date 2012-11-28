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

public class ConvexHullKey extends AssetKey {

	private MeshProtoKey _key;
	public ConvexHullKey(MeshProtoKey key) {
		_key = key;
	}

	public MeshProtoKey getMeshKey() {
		return _key;
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof ConvexHullKey)) {
			return false;
		}
		return ((ConvexHullKey)o)._key.equals(_key);
	}

	@Override
	public int hashCode() {
		return _key.hashCode() ^ 0x124087; // Randomly swizzle this so we don't hash the same as the key
	}

}
