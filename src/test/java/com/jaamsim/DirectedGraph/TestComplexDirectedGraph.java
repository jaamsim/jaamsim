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

public class TestComplexDirectedGraph {

	@Test
	public void basicTest() {

		Digraph graph = new Digraph();

		DigraphVertex vert1 = new DigraphVertex("Vertex1", graph);
		DigraphVertex vert2 = new DigraphVertex("Vertex2", graph);
		DigraphVertex vert3 = new DigraphVertex("Vertex3", graph);
		DigraphVertex vert4 = new DigraphVertex("Vertex4", graph);
		DigraphVertex vert5 = new DigraphVertex("Vertex5", graph);
		DigraphVertex vert6 = new DigraphVertex("Vertex6", graph);
		DigraphVertex vert7 = new DigraphVertex("Vertex7", graph);
		DigraphVertex vert8 = new DigraphVertex("Vertex8", graph);

		DigraphEdge edge1_2 = new DigraphEdge(vert1, vert2, 1.0, graph);
		DigraphEdge edge4_2 = new DigraphEdge(vert4, vert2, 2.0, graph);
		DigraphEdge edge4_5 = new DigraphEdge(vert4, vert5, 1.0, graph);
		DigraphEdge edge7_5 = new DigraphEdge(vert7, vert5, 2.0, graph);
		DigraphEdge edge7_8 = new DigraphEdge(vert7, vert8, 1.0, graph);
		DigraphEdge edge2_3 = new DigraphEdge(vert2, vert3, 1.0, graph);
		DigraphEdge edge2_6 = new DigraphEdge(vert2, vert6, 2.0, graph);
		DigraphEdge edge2_8 = new DigraphEdge(vert2, vert8, 3.0, graph);
		DigraphEdge edge5_6 = new DigraphEdge(vert5, vert6, 1.0, graph);

		vert1.init();
		vert2.init();
		vert3.init();
		vert4.init();
		vert5.init();
		vert6.init();
		vert7.init();
		vert8.init();

		edge1_2.init();
		edge4_2.init();
		edge4_5.init();
		edge7_5.init();
		edge7_8.init();
		edge2_3.init();
		edge2_6.init();
		edge2_8.init();
		edge5_6.init();

		graph.init();

		assertTrue(vert1.isSource());
		assertTrue(!vert2.isSource());
		assertTrue(!vert3.isSource());
		assertTrue(vert4.isSource());
		assertTrue(!vert5.isSource());
		assertTrue(!vert6.isSource());
		assertTrue(vert7.isSource());

		assertTrue(!vert1.isSink());
		assertTrue(!vert2.isSink());
		assertTrue(vert3.isSink());
		assertTrue(!vert4.isSink());
		assertTrue(!vert5.isSink());
		assertTrue(vert6.isSink());
		assertTrue(!vert7.isSink());
		assertTrue(vert8.isSink());

		ArrayList<DigraphPath> vert1ToVert3List = vert1.getPathsToSink(vert3);
		ArrayList<DigraphPath> vert1ToVert6List = vert1.getPathsToSink(vert6);
		ArrayList<DigraphPath> vert1ToVert8List = vert1.getPathsToSink(vert8);
		ArrayList<DigraphPath> vert4ToVert3List = vert4.getPathsToSink(vert3);
		ArrayList<DigraphPath> vert4ToVert6List = vert4.getPathsToSink(vert6);
		ArrayList<DigraphPath> vert4ToVert8List = vert4.getPathsToSink(vert8);
		ArrayList<DigraphPath> vert7ToVert3List = vert7.getPathsToSink(vert3);
		ArrayList<DigraphPath> vert7ToVert6List = vert7.getPathsToSink(vert6);
		ArrayList<DigraphPath> vert7ToVert8List = vert7.getPathsToSink(vert8);

		/*System.out.println("vert1ToVert3List="+vert1ToVert3List);
		System.out.println("vert1ToVert6List="+vert1ToVert6List);
		System.out.println("vert1ToVert8List="+vert1ToVert8List);
		System.out.println("vert4ToVert3List="+vert4ToVert3List);
		System.out.println("vert4ToVert6List="+vert4ToVert6List);
		System.out.println("vert4ToVert8List="+vert4ToVert8List);
		System.out.println("vert7ToVert3List="+vert7ToVert3List);
		System.out.println("vert7ToVert6List="+vert7ToVert6List);
		System.out.println("vert7ToVert8List="+vert7ToVert8List);*/

		assertTrue(vert1ToVert3List.size() == 1);
		assertTrue(vert1ToVert6List.size() == 1);
		assertTrue(vert1ToVert8List.size() == 1);
		assertTrue(vert4ToVert3List.size() == 1);
		assertTrue(vert4ToVert6List.size() == 2);
		assertTrue(vert4ToVert8List.size() == 1);
		assertTrue(vert7ToVert3List == null);
		assertTrue(vert7ToVert6List.size() == 1);
		assertTrue(vert7ToVert8List.size() == 1);

		assertTrue(vert1ToVert3List.get(0).getWeight() == 2.0);
		assertTrue(vert1ToVert6List.get(0).getWeight() == 3.0);
		assertTrue(vert1ToVert8List.get(0).getWeight() == 4.0);
		assertTrue(vert4ToVert3List.get(0).getWeight() == 3.0);
		assertTrue(vert4ToVert6List.get(0).getWeight() == 2.0);
		assertTrue(vert4ToVert8List.get(0).getWeight() == 5.0);
		assertTrue(vert7ToVert6List.get(0).getWeight() == 3.0);
		assertTrue(vert7ToVert8List.get(0).getWeight() == 1.0);
	}

}
