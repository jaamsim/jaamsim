/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2014 Ausenco Engineering Canada Inc.
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
package com.jaamsim.events;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class TestEventTree {
@Test
public void testRBTree() {
	EventTree tree = new EventTree();

	for (int i = 0; i < 10000; ++i) {
		tree.createOrFindNode(i, 0);
		tree.verify();
		int nodeCount = tree.verifyNodeCount();
		assertTrue((i+1) == nodeCount);
	}
	for (int i = 0; i < 10000; ++i) {
		assertTrue(tree.find(i,  0) != null);
	}
	assertTrue(tree.getNextNode().schedTick == 0);

	for (int i = 0; i < 10000; ++i) {
		tree.removeNode(i, 0);
		tree.verify();
		int nodeCount = tree.verifyNodeCount();
		assertTrue(nodeCount == 10000 - i - 1);
	}

	tree = new EventTree();
	for (int i = 10000; i > 0; --i) {
		tree.createOrFindNode(i, 0);
		tree.verify();
		int nodeCount = tree.verifyNodeCount();
		assertTrue((10001-i) == nodeCount);
	}

	assertTrue(tree.getNextNode().schedTick == 1);

	for (int i = 1; i <= 10000; ++i) {
		assertTrue(tree.find(i,  0) != null);
	}

	for (int i = 10000; i > 0; --i) {
		tree.removeNode(i, 0);
		tree.verify();
		int nodeCount = tree.verifyNodeCount();
		assertTrue(nodeCount == i - 1);
	}

	for (int i = 10000; i > 0; --i) {
		tree.createOrFindNode(i, 0);
		tree.verify();
		tree.createOrFindNode(-i, 0);
		tree.verify();
		int nodeCount = tree.verifyNodeCount();
		assertTrue(((10001-i)*2) == nodeCount);
	}

	assertTrue(tree.getNextNode().schedTick == -10000);

	for (int i = 0; i > 10000; --i) {
		tree.removeNode(i, 0);
		tree.verify();
		tree.removeNode(-i, 0);
		tree.verify();
		int nodeCount = tree.verifyNodeCount();
		assertTrue((20000-2-2*i) == nodeCount);
	}
}
}
