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

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;

import org.junit.Test;

public class TestTrivalDirectedGraph {

	@Test
	public void basicTest() {

		Digraph graph = new Digraph();
		DigraphVertex vert1 = new DigraphVertex("Vertex1", graph);
		DigraphVertex vert2 = new DigraphVertex("Vertex2", graph);
		DigraphEdge edge1_2 = new DigraphEdge(vert1, vert2, 1.0, graph);

		vert1.init();
		vert2.init();
		edge1_2.init();
		graph.init();

		assertTrue(vert1.isSource());
		assertTrue(!vert2.isSource());

		assertTrue(!vert1.isSink());
		assertTrue(vert2.isSink());

		ArrayList<DigraphPath> pathList = vert1.getPathsToSink(vert2);

		assertTrue(pathList.size() == 1);
		DigraphPath path = pathList.get(0);

		assertTrue(path.getFirstVertex() == vert1);
		assertTrue(path.getLastVertex() == vert2);
		assertTrue(path.getWeight() == 1.0);
	}

}
