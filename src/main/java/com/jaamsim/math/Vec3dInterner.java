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
package com.jaamsim.math;

import java.util.HashMap;

/**
 * Vec3dInterner is a container type used to 'intern' Vec3d instances hopefully saving space on repeating entries
 * @author matt.chudleigh
 *
 */
public class Vec3dInterner {

	private static class VecWrapper {
		public Vec3d val;
		public VecWrapper(Vec3d v) {
			val = v;
		}

		@Override
		public boolean equals(Object o) {
			VecWrapper vw = (VecWrapper)o;
			return val.equals3(vw.val);
		}

		@Override
		public int hashCode() {
			int hash = 0;
			hash ^= Double.valueOf(val.x).hashCode();
			hash ^= Double.valueOf(val.y).hashCode() * 3;
			hash ^= Double.valueOf(val.z).hashCode() * 7;
			return hash;
		}
	}

	private HashMap<VecWrapper, Vec3d> map = new HashMap<VecWrapper, Vec3d>();

	/**
	 * intern will return a pointer to a Vec3d (which may differ from input 'v') that is mathematically equal but
	 * may be a shared object. Any value returned by intern should be defensively copied before being modified
	 * @return
	 */
	public Vec3d intern(Vec3d v) {
		VecWrapper wrapped = new VecWrapper(v);
		Vec3d interned = map.get(wrapped);
		if (interned != null) {
			return interned;
		}

		map.put(wrapped, v);
		return v;
	}
}
