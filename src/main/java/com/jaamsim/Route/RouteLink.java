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
package com.jaamsim.Route;

import java.util.ArrayList;

import com.jaamsim.DirectedGraph.Digraph;
import com.jaamsim.DirectedGraph.DigraphEdge;
import com.jaamsim.DirectedGraph.DigraphUser;
import com.jaamsim.DirectedGraph.DigraphVertex;

public class RouteLink extends RouteComponent {

	private final DigraphEdge edge;
	private final DigraphVertex inVertex;
	private final DigraphVertex outVertex;

	public RouteLink(DigraphUser user) {
		super(user);
		inVertex = new DigraphVertex(null, null, user);
		outVertex = new DigraphVertex(null, null, user);
		edge = new DigraphEdge(null, null, 0.0, null, user);
	}

	@Override
	public void clear() {
		super.clear();
		edge.clear();
		inVertex.clear();
		outVertex.clear();
	}

	@Override
	public void kill() {
		super.kill();
		edge.kill();
		inVertex.kill();
		outVertex.kill();
	}

	@Override
	public void earlyInit() {
		super.earlyInit();
		edge.setTail(inVertex);
		edge.setHead(outVertex);
		inVertex.addOutgoingEdge(edge);
		outVertex.addIncomingEdge(edge);
	}

	@Override
	public void setDigraph(Digraph grph) {
		super.setDigraph(grph);
		edge.setDigraph(grph);
		inVertex.setDigraph(grph);
		outVertex.setDigraph(grph);
	}

	@Override
	public void setName(String str) {
		inVertex.setName(str + "_IN");
		outVertex.setName(str + "_OUT");
	}

	@Override
	public ArrayList<DigraphEdge> getEdges() {
		ArrayList<DigraphEdge> ret = super.getEdges();
		ret.add(edge);
		return ret;
	}

	public void setWeight(double wght) {
		edge.setWeight(wght);
	}

	@Override
	public DigraphVertex getInVertex() {
		return inVertex;
	}

	@Override
	public DigraphVertex getOutVertex() {
		return outVertex;
	}

	@Override
	public String toString() {
		return String.format("{inVertex=%s, outVertex=%s, edge=%s}", inVertex, outVertex, edge);
	}

}
