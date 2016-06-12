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

import com.jaamsim.basicsim.ErrorException;

public class Digraph {

	private final ArrayList<DigraphVertex> vertexList;  // list of the vertices in the graph
	private final ArrayList<DigraphEdge> edgeList;  // list of the edges in the graph
	private final ArrayList<DigraphVertex> sourceList;  // list of the vertices that are sources
	private final ArrayList<DigraphVertex> sinkList;  // list of the vertices that are sinks

	public Digraph() {
		vertexList = new ArrayList<>();
		edgeList = new ArrayList<>();
		sourceList = new ArrayList<>();
		sinkList = new ArrayList<>();
	}

	public void init() {
		sourceList.clear();
		sinkList.clear();
		for (DigraphVertex vert : vertexList) {
			if (vert.isSource())
				sourceList.add(vert);
			else if (vert.isSink())
				sinkList.add(vert);
		}
	}

	public void clear() {
		vertexList.clear();
		edgeList.clear();
		sourceList.clear();
		sinkList.clear();
	}

	public void addVertex(DigraphVertex vert) {
		vertexList.add(vert);
	}

	public void addEdge(DigraphEdge edge) {
		edgeList.add(edge);
	}

	/**
	 * Returns an unsorted list of all the paths that start at the specified vertex and end at a
	 * sink. The paths are found using a recursive depth-first search. The directed graph is
	 * assumed to be acyclic - if a closed loop is found, it is excluded from the returned list.
	 * @param start - first vertex in all the paths
	 * @return unsorted list of paths
	 * @throws Exception
	 */
	public static ArrayList<DigraphPath> findAllPathsFromVertex(DigraphVertex start) {
		ArrayList<DigraphPath> ret = new ArrayList<>();

		if (start.getOutList() == null)
			throw new ErrorException("OutputList is null. Start=%s", start);

		// Loop though the vertices that connect to the starting vertex
		for (DigraphEdge edge : start.getOutList()) {
			DigraphVertex vert = edge.getHead();
			if (vert == null)
				throw new ErrorException("Head vertex for edge is null. Edge=%s", edge);

			// Find the paths that start from the next vertex
			ArrayList<DigraphPath> pathList = Digraph.findAllPathsFromVertex(vert);

			// If no paths start from the next vertex, then create a new path to that vertex
			if (pathList.isEmpty()) {
				DigraphPath newPath = new DigraphPath();
				newPath.append(edge);
				ret.add(newPath);
				continue;
			}

			// If paths do start from the next vertex then turn them into paths from the
			// starting vertex
			for (DigraphPath path : pathList) {

				// Ignore any closed loops
				if (path.getEdgeList().contains(edge))
					continue;

				// Form the path from the starting vertex by adding the out-going edge
				path.prepend(edge);
				ret.add(path);
			}
		}
		return ret;
	}

}
