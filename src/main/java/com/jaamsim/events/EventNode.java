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

class EventNode {

	interface Runner {
		void runOnNode(EventNode node);
	}

	long schedTick; // The tick at which this event will execute
	int priority;   // The schedule priority of this event
	Event head;
	Event tail;

	boolean red;
	EventNode left;
	EventNode right;

	EventNode(long tick, int prio, EventNode def) {
		schedTick = tick;
		priority = prio;
		left = def;
		right = def;
		red = false;
	}

	final void addEvent(Event e, boolean fifo) {
		if (head == null) {
			head = e;
			tail = e;
			e.next = null;
			return;
		}

		if (fifo) {
			tail.next = e;
			tail = e;
			e.next = null;
		}
		else {
			e.next = head;
			head = e;
		}
	}

	final void removeEvent(Event evt) {
		// quick case where we are the head event
		if (this.head == evt) {
			this.head = evt.next;
			if (evt.next == null)
				this.tail = null;
		}
		else {
			Event prev = this.head;
			while (prev.next != evt) {
				prev = prev.next;
			}

			prev.next = evt.next;
			if (evt.next == null)
				this.tail = prev;
		}
	}

	final int compareToNode(EventNode other) {
		return compare(other.schedTick, other.priority);
	}

	final int compare(long schedTick, int priority) {
		if (this.schedTick < schedTick) return -1;
		if (this.schedTick > schedTick) return  1;

		if (this.priority < priority) return -1;
		if (this.priority > priority) return  1;

		return 0;
	}

	final void rotateRight(EventNode parent) {
		if (parent != null) {
			if (parent.left == this)
				parent.left = left;
			else
				parent.right = left;
		}

		EventNode oldMid = left.right;
		left.right = this;

		this.left = oldMid;
	}
	final void rotateLeft(EventNode parent) {
		if (parent != null) {
			if (parent.left == this)
				parent.left = right;
			else
				parent.right = right;
		}

		EventNode oldMid = right.left;
		right.left = this;

		this.right = oldMid;
	}

	final void cloneFrom(EventNode source) {
		this.head = source.head;
		this.tail = source.tail;
		this.schedTick = source.schedTick;
		this.priority = source.priority;
		Event next = this.head;
		while (next != null) {
			next.node = this;
			next = next.next;
		}
	}
}
