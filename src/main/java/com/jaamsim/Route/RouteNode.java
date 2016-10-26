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

public class RouteNode extends RouteComponent {

	private final DigraphVertex vertex; // directed graph vertex for incoming and outgoing edges

	public RouteNode(DigraphUser user) {
		super(user);
		vertex = new DigraphVertex(null, null, user);
	}

	@Override
	public void clear() {
		super.clear();
		vertex.clear();
	}

	@Override
	public void kill() {
		super.kill();
		vertex.kill();
	}

	@Override
	public void setDigraph(Digraph grph) {
		super.setDigraph(grph);
		vertex.setDigraph(grph);
	}

	@Override
	public void setName(String str) {
		vertex.setName(str);
	}

	@Override
	public DigraphVertex getInVertex() {
		return vertex;
	}

	@Override
	public DigraphVertex getOutVertex() {
		return vertex;
	}

	@Override
	public String toString() {
		return String.format("{vertex=%s}", vertex);
	}

}
