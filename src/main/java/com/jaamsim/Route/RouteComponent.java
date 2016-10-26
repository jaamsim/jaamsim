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

public abstract class RouteComponent {

	private final ArrayList<RouteComponent> nextComponentList;  // downstream links, nodes, etc.
	private final ArrayList<DigraphEdge> outfeedList;  // edges departing from this component
	private final DigraphUser user;
	private Digraph graph;

	public RouteComponent(DigraphUser u) {
		user = u;
		nextComponentList = new ArrayList<>();
		outfeedList = new ArrayList<>();
	}

	protected void clear() {
		nextComponentList.clear();
		outfeedList.clear();
	}

	public void kill() {
		for (DigraphEdge outEdge : outfeedList) {
			outEdge.kill();
		}
		this.clear();
	}

	public void earlyInit() {
		this.clear();
	}

	public void lateInit() {
		for (RouteComponent next : nextComponentList) {
			this.connectTo(next);
		}
	}

	public void setDigraph(Digraph grph) {
		graph = grph;
	}

	public void addNext(RouteComponent comp) {
		if (!nextComponentList.contains(comp)) {
			nextComponentList.add(comp);
		}
	}

	protected void connectTo(RouteComponent next) {
		DigraphVertex outVert = this.getOutVertex();
		DigraphVertex inVert = next.getInVertex();
		DigraphEdge outEdge = new DigraphEdge(outVert, inVert, 0.0d, graph, user);
		outVert.addOutgoingEdge(outEdge);
		inVert.addIncomingEdge(outEdge);
		outfeedList.add(outEdge);
	}

	public ArrayList<DigraphVertex> getVertices() {
		ArrayList<DigraphVertex> ret = new ArrayList<>(2);
		ret.add( this.getInVertex() );
		if (this.getInVertex() != this.getOutVertex()) {
			ret.add( this.getOutVertex() );
		}
		return ret;
	}

	public ArrayList<DigraphEdge> getEdges() {
		return new ArrayList<>(outfeedList);
	}

	public abstract void setName(String str);

	public abstract DigraphVertex getInVertex();

	public abstract DigraphVertex getOutVertex();

}
