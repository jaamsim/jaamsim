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

	private HashMap<Vertex, Integer> vertMap = new HashMap<Vertex, Integer>();
	private ArrayList<Vertex> verts = new ArrayList<Vertex>();

	/**
	 * Checks if the current vertex is unique. Returns the existing index if present, or adds the vertex if unique
	 * @param pos
	 * @param normal
	 * @param texCoord
	 * @return
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
