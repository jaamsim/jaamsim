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

import com.jaamsim.DirectedGraph.Digraph;
import com.jaamsim.DirectedGraph.DigraphUser;
import com.jaamsim.DirectedGraph.DigraphVertex;

public class RouteWayPoint extends RouteNode {
	private final DigraphVertex inVertex;
	private final DigraphVertex outVertex;

	public RouteWayPoint(DigraphUser user) {
		super(user);
		inVertex = new DigraphVertex(null, null, user);
		outVertex = new DigraphVertex(null, null, user);
	}

	@Override
	public void clear() {
		super.clear();
		inVertex.clear();
		outVertex.clear();
	}

	@Override
	public void kill() {
		super.kill();
		inVertex.kill();
		outVertex.kill();
	}

	@Override
	public void setDigraph(Digraph grph) {
		super.setDigraph(grph);
		inVertex.setDigraph(grph);
		outVertex.setDigraph(grph);
	}

	@Override
	public void setName(String str) {
		inVertex.setName(str + "-in");
		outVertex.setName(str + "-out");
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
		return String.format("{inVertex=%s, outVertex=%s}", inVertex, outVertex);
	}

}
