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
package com.jaamsim.MeshFiles;

import com.jaamsim.math.Vec2d;
import com.jaamsim.math.Vec3d;
import com.jaamsim.math.Vec4d;

/**
 * Vertex is a simple data container class, mostly needed for its equals() and hashcode() overrides
 * this allows us to build up semi-efficient hash maps of vertex data when building geometry
 * @author matt.chudleigh
 *
 */
public class Vertex {
	private Vec3d position;
	private Vec3d normal;
	private Vec2d texCoord;

	private Vec4d boneIndices;
	private Vec4d boneWeights;

	private final int cachedHash;

	public Vertex(Vec3d position, Vec3d normal, Vec2d texCoord, Vec4d boneIndices, Vec4d boneWeights) {
		this.position = position;
		this.normal = normal;
		this.texCoord = texCoord;
		this.boneIndices = boneIndices;
		this.boneWeights = boneWeights;

		// If we have boneIndices we must also have boneWeights
		assert((boneIndices==null) == (boneWeights==null));

		// Calculate a hash of the Vertex contents
		cachedHash = hashVertex();
	}

	private static int hashDouble(double d) {
		long l = Double.doubleToLongBits(d);
		return (int)(l ^ (l >>> 32));
	}

	private static int hashVec2d(Vec2d v) {
		int hash = 0;
		hash ^= hashDouble(v.x);
		hash ^= hashDouble(v.y) * 3;
		return hash;
	}

	private static int hashVec3d(Vec3d v) {
		int hash = 0;
		hash ^= hashDouble(v.x);
		hash ^= hashDouble(v.y) * 3;
		hash ^= hashDouble(v.z) * 7;
		return hash;
	}

	private static int hashVec4d(Vec4d v) {
		int hash = 0;
		hash ^= hashDouble(v.x);
		hash ^= hashDouble(v.y) * 3;
		hash ^= hashDouble(v.z) * 7;
		hash ^= hashDouble(v.w) * 15;
		return hash;
	}

	/** Calculate a hash of the Vertex contents */
	private int hashVertex() {
		int ret = 0;

		ret ^= hashVec3d(position);
		ret ^= hashVec3d(normal) * 11;

		if (texCoord != null)
			ret ^= hashVec2d(texCoord) * 19;

		if (boneIndices != null) {
			ret ^= hashVec4d(boneIndices) * 23;
			ret ^= hashVec4d(boneWeights) * 29;
		}
		return ret;
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof Vertex)) {
			return false;
		}
		Vertex ov = (Vertex)o;
		if (position != ov.position && !position.equals3(ov.position)) {
			return false;
		}
		if (normal != ov.normal && !normal.equals3(ov.normal)) {
			return false;
		}

		// One has a texCoord, but not the other
		if ((texCoord==null) != (ov.texCoord==null)) {
			return false;
		}

		if (texCoord != null && texCoord != ov.texCoord && !texCoord.equals2(ov.texCoord)) {
			return false;
		}

		if ((boneIndices==null) != (ov.boneIndices==null)) {
			return false;
		}

		if (boneIndices != null &&
		    boneIndices != ov.boneIndices && !boneIndices.equals4(ov.boneIndices) &&
		    boneWeights != ov.boneWeights && !boneWeights.equals4(ov.boneWeights)) {
			return false;
		}
		return true;
	}

	@Override
	public int hashCode() {
		return cachedHash;
	}

	public Vec3d getPos() {
		return position;
	}

	public Vec3d getNormal() {
		return normal;
	}

	public Vec2d getTexCoord() {
		return texCoord;
	}
	public Vec4d getBoneIndices() {
		return boneIndices;
	}
	public Vec4d getBoneWeights() {
		return boneWeights;
	}
}
