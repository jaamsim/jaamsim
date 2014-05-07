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
	}

	private Node root;
	private Node lowest;

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

	private void updateLowest() {
		if (root == null)
			return;

		Node current = root;
		while (current.left != nilNode)
			current = current.left;

		lowest = current;
	}

	public void insertEvent(Event e, long schedTick, int priority) {
		if (root == null) {
			root = new Node(schedTick, priority);
			root.addFront(e);
			return;
		}
		resetScratch();
		Node newNode = insertInTree(root, e, schedTick, priority);
		if (newNode != null) {
			insertBalance(newNode);
			root.red = false;
		}
	}

	private Node insertInTree(Node n, Event e, long schedTick, int priority) {
		while (true) {
			int comp = n.compare(schedTick, priority);
			if (comp == 0) {
				n.addFront(e);
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
			newNode.addFront(e);
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
		if (parent == null || !parent.red) return; // case 2

		// case 4
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

		if (n == parent.right && gp != null && parent == gp.left) {
			// Right child of a left parent, rotate left at parent
			parent.rotateLeft(gp);
			n = n.left;
		}
		if (n == parent.left && gp != null && parent == gp.right) {
			// left child of right parent, rotate right at parent
			parent.rotateRight(gp);
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

	// Verify the sorting structure and return the number of nodes
	public int verify() {
		if (root == null) return 0;
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
	public boolean find(long schedTick, int priority) {
		Node curr = root;
		while (true) {
			int comp = curr.compare(schedTick, priority);
			if (comp == 0) {
				return true;
			}
			if (comp < 0) {
				if (curr.right == nilNode) return false;
				curr = curr.right;
				continue;
			}
			if (curr.left == nilNode) return false;
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
