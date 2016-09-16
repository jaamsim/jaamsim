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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

public class DigraphVertex {

	private String name;
	private Digraph graph;  // directed graph containing this vertex
	private DigraphUser user;  // higher-level object using the directed graph
	private ArrayList<DigraphEdge> inList;  // edges that enter the vertex
	private ArrayList<DigraphEdge> outList; // edges that leave the vertex
	private HashMap<DigraphVertex, ArrayList<DigraphPath>> pathListMap; // paths from this vertex
	private static final Comparator<DigraphPath> pathSortOrder = new DigraphPathComparator();

	public DigraphVertex(String str, Digraph grph, DigraphUser u) {
		name = str;
		graph = grph;
		user = u;
		inList = new ArrayList<>();
		outList = new ArrayList<>();
		pathListMap = null;
	}

	public DigraphVertex(String str, Digraph grph) {
		this(str, grph, null);
	}

	public void initForUnitTest() {
		graph.addVertex(this);
	}

	public void clear() {
		inList.clear();
		outList.clear();
		pathListMap = null;
	}

	public void kill() {
		name = null;
		user = null;
		graph = null;
		this.clear();
	}

	public void setName(String str) {
		name = str;
	}

	public void addIncomingEdge(DigraphEdge edge) {
		inList.add(edge);
	}

	public void addOutgoingEdge(DigraphEdge edge) {
		outList.add(edge);
	}

	public ArrayList<DigraphEdge> getInList() {
		return inList;
	}

	public ArrayList<DigraphEdge> getOutList() {
		return outList;
	}

	public boolean isSource() {
		return inList.isEmpty();
	}

	public boolean isSink() {
		return outList.isEmpty();
	}

	public void setDigraph(Digraph grph) {
		graph = grph;
	}

	public Digraph getDigraph() {
		return graph;
	}

	public DigraphUser getUser() {
		return user;
	}

	/**
	 * Returns a sorted list of paths that start at this vertex and end at the specified sink.
	 * @param sink - vertex at the end of each path (must be a sink)
	 * @return sorted list of paths
	 */
	public ArrayList<DigraphPath> getPathsToSink(DigraphVertex sink) {
		if (pathListMap == null)
			this.populatePathListMap();
		return pathListMap.get(sink);
	}

	private void populatePathListMap() {
		pathListMap = new HashMap<>();

		// Prepare a list of paths sorted by sink and weight
		ArrayList<DigraphPath> pathList = Digraph.findAllPathsFromVertex(this);
		if (pathList.isEmpty())
			return;
		Collections.sort(pathList, pathSortOrder);

		// Add a separate list of paths to the hashmap for each sink
		DigraphVertex sink = pathList.get(0).getLastVertex();
		ArrayList<DigraphPath> list = new ArrayList<>();
		for (DigraphPath path : pathList) {
			if (path.getLastVertex() != sink) {
				pathListMap.put(sink, list);
				sink = path.getLastVertex();
				list = new ArrayList<>();
			}
			list.add(path);
		}
		pathListMap.put(sink, list);
	}

	@Override
	public String toString() {
		return name;
	}

}
