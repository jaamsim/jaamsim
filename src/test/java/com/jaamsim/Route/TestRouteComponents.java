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

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;

import org.junit.Test;

import com.jaamsim.DirectedGraph.Digraph;
import com.jaamsim.DirectedGraph.DigraphPath;
import com.jaamsim.DirectedGraph.DigraphVertex;

public class TestRouteComponents {

	@Test
	// Two RouteNodes connected by a RouteLink
	public void basicTest() {
		Digraph graph = new Digraph();

		// Create the two nodes and the link
		RouteNode nodeA = new RouteNode(null);
		nodeA.setName("nodeA");
		nodeA.setDigraph(graph);

		RouteLink link = new RouteLink(null);
		link.setName("link");
		link.setDigraph(graph);

		RouteNode nodeB = new RouteNode(null);
		nodeB.setName("nodeB");
		nodeB.setDigraph(graph);

		// First initialisation of the components
		nodeA.earlyInit();
		link.earlyInit();
		nodeB.earlyInit();

		// Connect the components (must be done AFTER earlyInit)
		nodeA.addNext(link);
		link.addNext(nodeB);

		// Second initialisation of the components
		nodeA.lateInit();
		link.lateInit();
		nodeB.lateInit();

		// Initialise the directed graph
		graph.init();

		// First node should have just one outfeed edge
		assertTrue( nodeA.getInVertex().getInList().isEmpty() );
		assertTrue( nodeA.getOutVertex().getOutList().size() == 1 );

		// Second node should have just one infeed edge
		assertTrue( nodeB.getInVertex().getInList().size() == 1 );
		assertTrue( nodeB.getOutVertex().getOutList().isEmpty() );

		// First node's outfeed edge should be the same as the link's infeed edge
		assertTrue( link.getInVertex().getInList().size() == 1 );
		assertTrue( link.getInVertex().getInList().get(0) == nodeA.getOutVertex().getOutList().get(0) );

		// Second node's infeed edge should be the same as the link' outfeed edge
		assertTrue( link.getOutVertex().getOutList().size() == 1 );
		assertTrue( link.getOutVertex().getOutList().get(0) == nodeB.getInVertex().getInList().get(0) );

		// There should be exactly one path between the first and second nodes
		DigraphVertex vertA = nodeA.getOutVertex();
		DigraphVertex vertB = nodeB.getInVertex();
		ArrayList<DigraphPath> dgpathList = vertA.getPathsToSink(vertB);
		assertTrue( dgpathList.size() == 1 );
	}

}
