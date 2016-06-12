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

public class DigraphVertex {

	private String name;
	private Digraph graph;  // directed graph containing this vertex
	private ArrayList<DigraphEdge> inList;  // edges that enter the vertex
	private ArrayList<DigraphEdge> outList; // edges that leave the vertex

	public DigraphVertex(String str, Digraph grph) {
		name = str;
		graph = grph;
		inList = new ArrayList<>();
		outList = new ArrayList<>();
	}

	public void init() {
		graph.addVertex(this);
	}

	public void clear() {
		inList.clear();
		outList.clear();
	}

	public void kill() {
		name = null;
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

	@Override
	public String toString() {
		return name;
	}

}
