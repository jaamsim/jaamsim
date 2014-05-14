/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2014 Ausenco Engineering Canada Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
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
