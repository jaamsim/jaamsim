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

import java.util.ArrayList;
import java.util.HashMap;

import com.jaamsim.math.Vec2d;
import com.jaamsim.math.Vec3d;

/**
 * This map builds up a list of unique vertices, and can return the index of vertices at add time
 * @author matt.chudleigh
 *
 */
public class VertexMap {

	private HashMap<Vertex, Integer> vertMap = new HashMap<>();
	private ArrayList<Vertex> verts = new ArrayList<>();

	/**
	 * Checks if the current vertex is unique. Returns the existing index if present, or adds the vertex if unique
	 * @param pos
	 * @param normal
	 * @param texCoord
	 */
	public int getVertIndex(Vec3d pos, Vec3d normal, Vec2d texCoord) {
		Vertex v = new Vertex(pos, normal, texCoord, null, null);
		Integer index = vertMap.get(v);
		if (index != null) {
			return index.intValue();
		}

		int newIndex = verts.size();
		verts.add(v);
		vertMap.put(v, newIndex);
		return newIndex;
	}

	public ArrayList<Vertex> getVertList() {
		return verts;
	}
}
