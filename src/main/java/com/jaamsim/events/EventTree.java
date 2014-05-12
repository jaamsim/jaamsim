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


/**
 * EventTree is a custom red-black tree implementation intended to be used as the priority queue
 * storing Jaamsim's discrete events
 * @author matt.chudleigh
 *
 */
class EventTree {

	static interface EventComp {
		public boolean comp(Event e);
	}

	static class Node {
		Event first;
		Event last;
		Node left;
		Node right;
		long schedTick;
		int priority;
		boolean red;

		Node(long schedTick, int priority) {
			this.schedTick = schedTick;
			this.priority = priority;
			this.right = nilNode;
			this.left = nilNode;
		}

		Event popFront() {
			if (isEmpty()) return null;

			Event ret = first;
			first = first.next;
			if (first == null)
				last = null;

			return ret;
		}

		Event peekFront() {
			return first;
		}

		void addFront(Event e) {
			e.next = first;
			first = e;
			if (last == null)
				last = e;
		}

		void addEnd(Event e) {
			if (first == null) {
				first = e;
				last = e;
				return;
			}

			last.next = e;
			last = e;
		}

		boolean isEmpty() {
			return first == null;
		}

		Event removeIf(EventComp comp) {
			if (isEmpty()) return null;
			Event e = first;
			Event prev = null;

			while (e != null) {
				if (comp.comp(e)) {

					if (first == e) {
						first = e.next;
					} else {
						prev.next = e.next;
					}

					if (last == e) {
						last = prev;
					}

					return e;
				}

				prev = e;
				e = e.next;
			}
			return null;
		}

		int compareToNode(Node other) {
			return compare(other.schedTick, other.priority);
		}

		int compare(long schedTick, int priority) {
			if (this.schedTick < schedTick) return -1;
			if (this.schedTick > schedTick) return  1;

			if (this.priority < priority) return -1;
			if (this.priority > priority) return  1;

			return 0;
		}

		void rotateRight(Node parent) {
			if (parent != null) {
				if (parent.left == this)
					parent.left = left;
				else
					parent.right = left;
			}

			Node oldMid = left.right;
			left.right = this;

			this.left = oldMid;
		}
		void rotateLeft(Node parent) {
			if (parent != null) {
				if (parent.left == this)
					parent.left = right;
				else
					parent.right = right;
			}

			Node oldMid = right.left;
			right.left = this;

			this.right = oldMid;
		}

		void cloneFrom(Node source) {
			this.first = source.first;
			this.last = source.last;
			this.schedTick = source.schedTick;
			this.priority = source.priority;
		}

	}

	private Node root = nilNode;
	private Node lowest = null;

	private static final Node nilNode;

	static {
		nilNode = new Node(0, 0);
		nilNode.left = null;
		nilNode.right = null;
		nilNode.red = false;
	}

	///////////////////////////////////////////
	// Scratch space, used instead of having parent pointers

	private Node[] scratch = new Node[32];
	private int scratchPos = 0;

	private void pushScratch(Node n) {
		scratch[scratchPos++] = n;
	}
	private void dropScratch(int n) {
		scratchPos = Math.max(0, scratchPos - n);
	}
	// Get the 'nth' node from the end of the scratch (1 being the first)
	private Node getScratch(int n) {
		return (scratchPos >= n) ? scratch[scratchPos - n] : null;
	}
	private void resetScratch() {
		scratchPos = 0;
	}

	Node getNextNode() {
		if (lowest == null) updateLowest();
		return lowest;
	}

	public void reset() {
		root = nilNode;
		lowest = null;
	}

	private void updateLowest() {
		if (root == nilNode) {
			lowest = null;
			return;
		}
		Node current = root;
		while (current.left != nilNode)
			current = current.left;

		lowest = current;
	}

	public Node createNode(long schedTick, int priority) {
		if (root == nilNode) {
			root = new Node(schedTick, priority);
			lowest = root;
			return root;
		}
		resetScratch();
		Node newNode = insertInTree(schedTick, priority);
		if (newNode == null) {
			return null;
		}
		insertBalance(newNode);
		root.red = false;

		if (newNode.compareToNode(lowest) < 0) {
			lowest = newNode;
		}
		return newNode;
	}

	private Node insertInTree(long schedTick, int priority) {
		Node n = root;

		while (true) {
			int comp = n.compare(schedTick, priority);
			if (comp == 0) {
				return null; // No new node added
			}
			Node next = comp > 0 ? n.left : n.right;
			if (next != nilNode) {
				pushScratch(n);
				n = next;
				continue;
			}

			// There is no current node for this time/priority
			Node newNode = new Node(schedTick, priority);
			pushScratch(n);
			newNode.red = true;
			if (comp > 0)
				n.left = newNode;
			else
				n.right = newNode;
			return newNode;
		}
	}

	private void insertBalance(Node n) {
		// See the wikipedia page for red-black trees to understand the case numbers

		Node parent = getScratch(1);
		if (parent == null || !parent.red) return; // cases 1 and 2

		Node gp = getScratch(2);
		if (gp == null) return;

		Node uncle = (gp.left == parent ? gp.right : gp.left);
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
		if (n == parent.right && gp != null && parent == gp.left) {
			// Right child of a left parent, rotate left at parent
			parent.rotateLeft(gp);
			parent = n;
			n = n.left;
		}
		else if (n == parent.left && gp != null && parent == gp.right) {
			// left child of right parent, rotate right at parent
			parent.rotateRight(gp);
			parent = n;
			n = n.right;
		}

		Node ggp = getScratch(3);
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

	public boolean removeNode(long schedTick, int priority) {
		// First find the node to remove
		resetScratch();

		Node current = root;
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
		// We have the node to remove
		if (current.left != nilNode && current.right != nilNode) {
			current = swapToLeaf(current);
		}

//		// Verify we have a proper parent list (testing only)
//		if (scratchPos > 0 && scratch[0] != root) throw new RuntimeException("Bad parent list");
//		for (int i = 1; i < scratchPos; ++i) {
//			// Check the current node is a child of the previous
//			Node child = scratch[i];
//			Node parent = scratch[i-1];
//			if (parent.left != child && parent.right != child) {
//				throw new RuntimeException("Bad parent list");
//			}
//		}

		Node child = current.left != nilNode ? current.left : current.right;

		Node parent = getScratch(1);

		// Drop the node
		if (parent != null) {
			if (parent.left == current)
				parent.left = child;
			else
				parent.right = child;
		}

		if (current == root)
			root = child;


		if (current.red) {
			updateLowest();
			return true; // We swapped out a red node, there's nothing else to do
		}
		if (child.red) {
			child.red = false;
			updateLowest();
			return true; // traded a red for a black, still all good.
		}

		// We removed a black node with a black child, we need to re-balance the tree
		deleteBalance(child);
		root.red = false;
		updateLowest();
		return true;
	}

	private Node swapToLeaf(Node node) {
		pushScratch(node);
		Node curr = node.left;
		while (curr.right != nilNode) {
			pushScratch(curr);
			curr = curr.right;
		}
		node.cloneFrom(curr);
		return curr;
	}

	private void deleteBalance(Node n) {
		// At all times the scratch space should contain the parent list (but not n)
		Node parent = getScratch(1);
		if (parent == null)
			return;

		Node sib = (parent.left == n) ? parent.right : parent.left;
		Node gp = getScratch(2);

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

	// Verify the sorting structure and return the number of nodes
	public int verify() {
		if (root == nilNode) return 0;

		if (nilNode.red == true)
			throw new RuntimeException("nil node corrupted, turned red");
		return verifyNode(root);
	}

	private int verifyNode(Node n) {
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
	public Node find(long schedTick, int priority) {
		Node curr = root;
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

	public int verifyEventCount() {
		return countEvents(root);
	}

	private int countEvents(Node n) {
		Event e = n.first;
		int count = 0;
		while (e != null) {
			count++;
			e = e.next;
		}
		if (n.left != nilNode)
			count += countEvents(n.left);
		if (n.right != nilNode)
			count += countEvents(n.right);

		return count;
	}
	public int verifyNodeCount() {
		if (root == nilNode) return 0;
		return countNodes(root);
	}
	private int countNodes(Node n) {
		int count = 1;
		if (n.left != nilNode)
			count += countNodes(n.left);
		if (n.right != nilNode)
			count += countNodes(n.right);

		return count;
	}
}
