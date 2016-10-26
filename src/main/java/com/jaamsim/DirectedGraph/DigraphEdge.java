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

public class DigraphEdge {

	private DigraphVertex tail;  // vertex at the start of the edge
	private DigraphVertex head;  // vertex at the end of the edge
	private double weight;  // weight assigned to this edge
	private Digraph graph;  // directed graph containing this edge
	private DigraphUser user;  // higher-level object using the directed graph

	public DigraphEdge(DigraphVertex vertex1, DigraphVertex vertex2, double wght, Digraph grph, DigraphUser u) {
		tail = vertex1;
		head = vertex2;
		weight = wght;
		graph = grph;
		user = u;
	}

	public DigraphEdge(DigraphVertex vertex1, DigraphVertex vertex2, double wght, Digraph grph) {
		this(vertex1, vertex2, wght, grph, null);
	}

	public void initForUnitTest() {
		tail.addOutgoingEdge(this);
		head.addIncomingEdge(this);
		graph.addEdge(this);
	}

	public void clear() {
		tail = null;
		head = null;
	}

	public void kill() {
		user = null;
		graph = null;
		this.clear();
	}

	public void setTail(DigraphVertex vert) {
		tail = vert;
	}

	public DigraphVertex getTail() {
		return tail;
	}

	public void setHead(DigraphVertex vert) {
		head = vert;
	}

	public DigraphVertex getHead() {
		return head;
	}

	public void setWeight(double wght) {
		weight = wght;
	}

	public double getWeight() {
		return weight;
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

	@Override
	public String toString() {
		return String.format("%s-%s", tail, head);
	}

}
