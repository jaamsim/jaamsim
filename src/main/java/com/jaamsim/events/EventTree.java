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
		Node parent;
		Node left;
		Node right;
		long schedTick;
		int priority;
		boolean red;

		Node(long schedTick, int priority) {
			this.schedTick = schedTick;
			this.priority = priority;
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
		Node grandParent() {
			if (parent == null) return null;

			return parent.parent;
		}
		Node uncle() {
			Node gp = grandParent();
			if (gp == null) return null;

			if (gp.left == parent) return gp.right;

			return gp.left;
		}

		void rotateRight() {
			if (parent != null) {
				if (parent.left == this)
					parent.left = left;
				else
					parent.right = left;
			}

			Node oldMid = left.right;
			left.right = this;

			left.parent = parent;
			this.parent = left;

			this.left = oldMid;
			if (oldMid != null)
				oldMid.parent = this;
		}
		void rotateLeft() {
			if (parent != null) {
				if (parent.left == this)
					parent.left = right;
				else
					parent.right = right;
			}

			Node oldMid = right.left;
			right.left = this;

			right.parent = parent;
			this.parent = right;

			this.right = oldMid;
			if (oldMid != null)
				oldMid.parent = this;
		}
	}

	private Node root;
	private Node lowest;

	Node getNextNode() {
		if (lowest == null) updateLowest();
		return lowest;
	}

	private void updateLowest() {
		if (root == null)
			return;

		Node current = root;
		while (current.left != null)
			current = current.left;

		lowest = current;
	}

	public void insertEvent(Event e, long schedTick, int priority) {
		if (root == null) {
			root = new Node(schedTick, priority);
			root.addFront(e);
			return;
		}
		Node newNode = insertInTree(root, e, schedTick, priority);
		if (newNode != null) {
			insertBalance(newNode);
			root.red = false;
		}
	}

	private Node insertInTree(Node n, Event e, long schedTick, int priority) {
		int comp = n.compare(schedTick, priority);
		if (comp == 0) {
			n.addFront(e);
			return null; // No new node added
		}
		Node next = comp > 0 ? n.left : n.right;
		if (next != null)
			return insertInTree(next, e, schedTick, priority);

		// There is no current node for this time/priority
		Node newNode = new Node(schedTick, priority);
		newNode.parent = n;
		newNode.addFront(e);
		newNode.red = true;
		if (comp > 0)
			n.left = newNode;
		else
			n.right = newNode;
		return newNode;
	}

	private void insertBalance(Node n) {
		// See the wikipedia page for red-black trees to understand the case numbers

		if (n.parent == null || !n.parent.red) return; // case 2

		Node uncle = n.uncle();
		if (uncle != null && uncle.red) {
			// Both parent and uncle are red
			// case 2
			n.parent.red = false;
			uncle.red = false;
			Node gp = n.grandParent();
			gp.red = true;
			insertBalance(gp);
			return;
		}

		// case 4
		Node gp = n.grandParent();
		if (gp == null) return;

		if (n == n.parent.right && gp != null && n.parent == gp.left) {
			// Right child of a left parent, rotate left at parent
			n.parent.rotateLeft();
			n = n.left;
		}
		if (n == n.parent.left && gp != null && n.parent == gp.right) {
			// left child of right parent, rotate right at parent
			n.parent.rotateRight();
			n = n.right;
		}

		// case 5
		gp.red = true;
		n.parent.red = false;
		if (n.parent.left == n)
			gp.rotateRight();
		else
			gp.rotateLeft();

		if (gp == root) root = gp.parent;

	}

	// Verify the sorting structure and return the number of nodes
	public int verify() {
		if (root == null) return 0;
		return verifyNode(root);
	}

	private int verifyNode(Node n) {
		int lBlacks = 0;
		int rBlacks = 0;

		if (n.left != null) {
			if (n.compareToNode(n.left) != 1)
				throw new RuntimeException("RB tree order verify failed");
			if (n.left.parent != n)
				throw new RuntimeException("Left branch parent pointer verify failed");
			lBlacks = verifyNode(n.left);
		}
		if (n.right != null) {
			if (n.compareToNode(n.right) != -1)
				throw new RuntimeException("RB tree order verify failed");
			if (n.right.parent != n)
				throw new RuntimeException("Right branch parent pointer verify failed");
			rBlacks = verifyNode(n.right);
		}

		if (n.red) {
			if (n.left != null && n.left.red)
				throw new RuntimeException("RB tree red-red child verify failed");
			if (n.right != null && n.right.red)
				throw new RuntimeException("RB tree red-red child verify failed");
		}

		if (lBlacks != rBlacks)
			throw new RuntimeException("RB depth equality verify failed");
		return lBlacks + (n.red ? 0 : 1);
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
		if (n.left != null)
			count += countEvents(n.left);
		if (n.right != null)
			count += countEvents(n.right);

		return count;
	}
	public int verifyNodeCount() {
		return countNodes(root);
	}
	private int countNodes(Node n) {
		int count = 1;
		if (n.left != null)
			count += countNodes(n.left);
		if (n.right != null)
			count += countNodes(n.right);

		return count;
	}
}
