/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2016 JaamSim Software Inc.
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
package com.jaamsim.DirectedGraph;

import java.util.ArrayList;

public class DigraphPath {

	private final ArrayList<DigraphEdge> edgeList;  // list of edges that form the path
	private double weight;  // total weight for edges in the path

	public DigraphPath() {
		edgeList = new ArrayList<>();
		weight = 0.0;
	}

	public void kill() {
		edgeList.clear();
	}

	public void append(DigraphEdge edge) {
		edgeList.add(edge);
		weight += edge.getWeight();
	}

	public void prepend(DigraphEdge edge) {
		edgeList.add(0, edge);
		weight += edge.getWeight();
	}

	public ArrayList<DigraphEdge> getEdgeList() {
		return edgeList;
	}

	public double getWeight() {
		return weight;
	}

	/**
	 * Returns the first vertex in the path.
	 * @return first vertex
	 */
	public DigraphVertex getFirstVertex() {
		if (edgeList.isEmpty())
			return null;
		return edgeList.get(0).getTail();
	}

	/**
	 * Returns the last vertex in the path.
	 * @return last vertex
	 */
	public DigraphVertex getLastVertex() {
		if (edgeList.isEmpty())
			return null;
		return edgeList.get(edgeList.size()-1).getHead();
	}

	/**
	 * Returns a list of the vertices that form the path.
	 * @return list of vertices
	 */
	public ArrayList<DigraphVertex> getVertexList() {
		ArrayList<DigraphVertex> ret = new ArrayList<>();
		if (edgeList.isEmpty())
			return ret;

		ret.add(edgeList.get(0).getTail());
		for (DigraphEdge edge : edgeList) {
			ret.add(edge.getHead());
		}
		return ret;
	}

	/**
	 * Returns a list of the entities that own the vertices and edges in the path.
	 * @return list of owners
	 */
	public ArrayList<DigraphUser> getUserPath() {
		ArrayList<DigraphUser> ret = new ArrayList<>();
		if (edgeList.isEmpty())
			return ret;

		// First vertex in the path
		DigraphUser user = edgeList.get(0).getTail().getUser();
		ret.add(user);

		// Loop through the full set of edges
		for (DigraphEdge edge : edgeList) {

			// User for the edge
			if (edge.getUser() != user) {
				user = edge.getUser();
				ret.add(user);
			}

			// User for the edge's head vertex
			if (edge.getHead().getUser() != user) {
				user = edge.getHead().getUser();
				ret.add(user);
			}
		}

		return ret;
	}

	@Override
	public String toString() {
		if (edgeList.isEmpty())
			return "";

		// Add the first vertex in the path
		StringBuilder sb = new StringBuilder();
		sb.append(edgeList.get(0).getTail());

		// Loop through the edges, adding the head vertices
		for (DigraphEdge edge : edgeList) {
			sb.append("-").append(edge.getHead());
		}
		return sb.toString();
	}

}
