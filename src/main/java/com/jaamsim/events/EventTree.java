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

import java.util.Arrays;


/**
 * EventTree is a custom red-black tree implementation intended to be used as the priority queue
 * storing Jaamsim's discrete events
 * @author matt.chudleigh
 *
 */
class EventTree {
	private final EventNode nilNode = new EventNode(0, 0, null);
	private EventNode root = nilNode;
	private EventNode lowest = null;

	///////////////////////////////////////////
	// Scratch space, used instead of having parent pointers

	private EventNode[] scratch = new EventNode[64];
	private int scratchPos = 0;

	private void pushScratch(EventNode n) {
		scratch[scratchPos++] = n;
	}
	private void dropScratch(int n) {
		scratchPos = Math.max(0, scratchPos - n);
	}
	// Get the 'nth' node from the end of the scratch (1 being the first)
	private EventNode getScratch(int n) {
		return (scratchPos >= n) ? scratch[scratchPos - n] : null;
	}
	private void resetScratch() {
		scratchPos = 0;
	}

	EventNode getNextNode() {
		if (lowest == null) updateLowest();
		return lowest;
	}

	final void reset() {
		root = nilNode;
		lowest = null;
		clearFreeList();
		resetScratch();
		Arrays.fill(scratch, null);
	}

	private void updateLowest() {
		if (root == nilNode) {
			lowest = null;
			return;
		}
		EventNode current = root;
		while (current.left != nilNode)
			current = current.left;

		lowest = current;
	}

	final EventNode createOrFindNode(long schedTick, int priority) {

		if (root == nilNode) {
			root = getNewNode(schedTick, priority);
			lowest = root;
			return root;
		}
		resetScratch();

		EventNode n = root;
		EventNode newNode = null;

		while (true) {
			int comp = n.compare(schedTick, priority);
			if (comp == 0) {
				return n; // Found existing node
			}
			EventNode next = comp > 0 ? n.left : n.right;
			if (next != nilNode) {
				pushScratch(n);
				n = next;
				continue;
			}

			// There is no current node for this time/priority
			newNode = getNewNode(schedTick, priority);
			pushScratch(n);
			newNode.red = true;
			if (comp > 0)
				n.left = newNode;
			else
				n.right = newNode;
			break;
		}

		insertBalance(newNode);
		root.red = false;

		if (lowest != null && newNode.compareToNode(lowest) < 0) {
			lowest = newNode;
		}
		return newNode;

	}

	private void insertBalance(EventNode n) {
		// See the wikipedia page for red-black trees to understand the case numbers

		EventNode parent = getScratch(1);
		if (parent == null || !parent.red) return; // cases 1 and 2

		final EventNode gp = getScratch(2);
		if (gp == null) return;

		EventNode uncle = (gp.left == parent ? gp.right : gp.left);
		if (uncle.red) {
			// Both parent and uncle are red
			// case 2
			parent.red = false;
			uncle.red = false;
			gp.red = true;
			dropScratch(2);
			insertBalance(gp);
			return;
		}

		// case 4
		if (n == parent.right && parent == gp.left) {
			// Right child of a left parent, rotate left at parent
			parent.rotateLeft(gp);
			parent = n;
			n = n.left;
		}
		else if (n == parent.left && parent == gp.right) {
			// left child of right parent, rotate right at parent
			parent.rotateRight(gp);
			parent = n;
			n = n.right;
		}

		EventNode ggp = getScratch(3);
		// case 5
		gp.red = true;
		parent.red = false;
		if (parent.left == n) {
			if (gp == root)
				root = gp.left;
			gp.rotateRight(ggp);
		} else {
			if (gp == root)
				root = gp.right;
			gp.rotateLeft(ggp);
		}

	}

	final boolean removeNode(long schedTick, int priority) {
		// First find the node to remove
		resetScratch();
		lowest = null;

		EventNode current = root;
		while (true) {
			int comp = current.compare(schedTick, priority);

			if (comp == 0) break;

			pushScratch(current);
			if (comp > 0)
				current = current.left;
			else
				current = current.right;
			if (current == nilNode) {
				return false; // Node not found
			}
		}

		// Debugging
		if (current.head != null || current.tail != null)
			throw new RuntimeException("Removing non-empy node");

		// We have the node to remove
		if (current.left != nilNode && current.right != nilNode) {
			current = swapToLeaf(current);
		}

//		// Verify we have a proper parent list (testing only)
//		if (scratchPos > 0 && scratch[0] != root) throw new RuntimeException("Bad parent list");
//		for (int i = 1; i < scratchPos; ++i) {
//			// Check the current node is a child of the previous
//			EventNode child = scratch[i];
//			EventNode parent = scratch[i-1];
//			if (parent.left != child && parent.right != child) {
//				throw new RuntimeException("Bad parent list");
//			}
//		}

		EventNode child = current.left != nilNode ? current.left : current.right;

		EventNode parent = getScratch(1);

		// Drop the node
		if (parent != null) {
			if (parent.left == current)
				parent.left = child;
			else
				parent.right = child;
		}

		if (current == root)
			root = child;

		boolean currentIsRed = current.red;

		reuseNode(current);

		if (currentIsRed) {
			return true; // We swapped out a red node, there's nothing else to do
		}
		if (child.red) {
			child.red = false;
			return true; // traded a red for a black, still all good.
		}

		// We removed a black node with a black child, we need to re-balance the tree
		deleteBalance(child);
		root.red = false;
		return true;
	}

	private EventNode swapToLeaf(EventNode node) {
		pushScratch(node);
		EventNode curr = node.left;
		while (curr.right != nilNode) {
			pushScratch(curr);
			curr = curr.right;
		}
		node.cloneFrom(curr);
		return curr;
	}

	private void deleteBalance(EventNode n) {
		// At all times the scratch space should contain the parent list (but not n)
		EventNode parent = getScratch(1);
		if (parent == null)
			return;

		EventNode sib = (parent.left == n) ? parent.right : parent.left;
		EventNode gp = getScratch(2);

		// case 2
		if (sib.red) {
			sib.red = false;
			parent.red = true;
			if (n == parent.left)
				parent.rotateLeft(gp);
			else
				parent.rotateRight(gp);
			if (root == parent)
				root = sib;

			// update the parent list after the rotation
			dropScratch(1);
			pushScratch(sib);
			pushScratch(parent);
			gp = getScratch(2);

			// update the sibling
			sib = (parent.left == n) ? parent.right : parent.left;
		}

		// case 3
		if (!parent.red && !sib.left.red && !sib.right.red) {
			sib.red = true;
			dropScratch(1);
			deleteBalance(parent);
			return;
		}

		// case 4
		if (parent.red && !sib.left.red && !sib.right.red) {
			parent.red = false;
			sib.red = true;
			return;
		}

		// case 5
		if (parent.left == n &&
		    !sib.right.red &&
		    sib.left.red) {

			sib.red = true;
			sib.left.red = false;
			sib.rotateRight(parent);

			sib = parent.right;
		} else if (parent.right == n &&
		           !sib.left.red &&
		           sib.right.red) {

			sib.red = true;
			sib.right.red = false;
			sib.rotateLeft(parent);

			sib = parent.left;
		}

		// case 6
		sib.red = parent.red;
		parent.red = false;
		if (n == parent.left) {
			sib.right.red = false;
			parent.rotateLeft(gp);
		} else {
			sib.left.red = false;
			parent.rotateRight(gp);
		}
		if (root == parent) {
			root = sib;
		}
	}

	final void runOnAllNodes(EventNode.Runner runner) {
		runOnNode(root, runner);
	}

	private void runOnNode(EventNode node, EventNode.Runner runner) {
		if (node == nilNode)
			return;

		runOnNode(node.left, runner);

		runner.runOnNode(node);

		runOnNode(node.right, runner);
	}

	// Verify the sorting structure and return the number of nodes
	final int verify() {
		if (root == nilNode) return 0;

		if (nilNode.red == true)
			throw new RuntimeException("nil node corrupted, turned red");
		return verifyNode(root);
	}

	private int verifyNode(EventNode n) {
		int lBlacks = 0;
		int rBlacks = 0;

		if (n.left != nilNode) {
			if (n.compareToNode(n.left) != 1)
				throw new RuntimeException("RB tree order verify failed");
			lBlacks = verifyNode(n.left);
		}
		if (n.right != nilNode) {
			if (n.compareToNode(n.right) != -1)
				throw new RuntimeException("RB tree order verify failed");
			rBlacks = verifyNode(n.right);
		}

		if (n.red) {
			if (n.left.red)
				throw new RuntimeException("RB tree red-red child verify failed");
			if (n.right.red)
				throw new RuntimeException("RB tree red-red child verify failed");
		}

		if (lBlacks != rBlacks)
			throw new RuntimeException("RB depth equality verify failed");
		return lBlacks + (n.red ? 0 : 1);
	}

	// Search the tree and return true if this node is found
	final EventNode find(long schedTick, int priority) {
		EventNode curr = root;
		while (true) {
			if (curr == nilNode) return null;
			int comp = curr.compare(schedTick, priority);
			if (comp == 0) {
				return curr;
			}
			if (comp < 0) {
				curr = curr.right;
				continue;
			}
			curr = curr.left;
			continue;
		}
	}

	final int verifyNodeCount() {
		if (root == nilNode) return 0;
		return countNodes(root);
	}
	private int countNodes(EventNode n) {
		int count = 1;
		if (n.left != nilNode)
			count += countNodes(n.left);
		if (n.right != nilNode)
			count += countNodes(n.right);

		return count;
	}

	private EventNode freeList = null;

	private EventNode getNewNode(long schedTick, int priority) {
		if (freeList == null) {
			return new EventNode(schedTick, priority, nilNode);
		}

		EventNode ret = freeList;
		freeList = freeList.left;

		ret.schedTick = schedTick;
		ret.priority = priority;
		ret.head = null;
		ret.tail = null;

		ret.left = nilNode;
		ret.right = nilNode;
		ret.red = false;

		return ret;
	}

	private void reuseNode(EventNode node) {
		// Clear the node
		node.left = null;
		node.right = null;
		node.head = null;
		node.tail = null;

		node.left = freeList;
		freeList = node;
	}

	private void clearFreeList() {
		freeList = null;
	}

}
