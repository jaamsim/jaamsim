/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2013 Ausenco Engineering Canada Inc.
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
	private final Vec3d position;
	private final Vec3d normal;
	private final Vec2d texCoord;

	private final int cachedHash;

	public Vertex(Vec3d position, Vec3d normal, Vec2d texCoord, Vec4d boneIndices, Vec4d boneWeights) {
		this.position = position;
		this.normal = normal;
		this.texCoord = texCoord;

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

	/** Calculate a hash of the Vertex contents */
	private int hashVertex() {
		int ret = 0;

		ret ^= hashVec3d(position);
		ret ^= hashVec3d(normal) * 11;

		if (texCoord != null)
			ret ^= hashVec2d(texCoord) * 19;

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
}
