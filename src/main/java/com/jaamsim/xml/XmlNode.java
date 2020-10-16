/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2012 Ausenco Engineering Canada Inc.
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
package com.jaamsim.xml;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

/**
 * ColNode's are the nodes of an internal DOM like collada tree representation.
 * @author matt.chudleigh
 *
 */
public class XmlNode {

	public class ChildIterable implements Iterable<XmlNode> {

		@Override
		public Iterator<XmlNode> iterator() {
			return new ChildIterator();
		}
	}
	public class ChildIterator implements Iterator<XmlNode> {
		private int index = 0;

		@Override
		public boolean hasNext() {
			return index < _children.size();
		}

		@Override
		public XmlNode next() {
			return _children.get(index++);
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
	}

	private XmlNode _parent;
	private HashMap<String, String> _attribs = new HashMap<>();

	private String _tag;
	private String _fragID;

	/**
	 * Content can be a String, double[], int[], boolean[] or String[] depending on tag type
	 */
	private Object _content;

	private ArrayList<XmlNode> _children;

	public XmlNode(XmlNode parent, String tag, String fragID) {
		_parent = parent;
		_tag = tag;
		_fragID = fragID;

		_children = new ArrayList<>();
	}

	public void addAttrib(String name, String value) {

		assert(!_attribs.containsKey(name));

		_attribs.put(name, value);
	}

	public String getAttrib(String name) {
		return _attribs.get(name);
	}

	public boolean hasAttrib(String name) {
		return _attribs.containsKey(name);
	}

	public void addChild(XmlNode child) {
		_children.add(child);
	}

	public int getNumChildren() {
		return _children.size();
	}

	public XmlNode getChild(int index) {
		if (index < 0 || index >= _children.size()) {
			assert(false);
			return null;
		}
		return _children.get(index);
	}

	public String getTag() {
		return _tag;
	}

	public String getFragID() {
		return _fragID;
	}

	public XmlNode getParent() {
		return _parent;
	}

	public ChildIterable children() {
		return new ChildIterable();
	}

	/**
	 * Return the number of child attributes of this tag
	 * @param tag
	 */
	public int getNumChildrenByTag(String tag) {
		int num = 0;
		for (XmlNode child : _children) {
			if (child.getTag().equals(tag))
				num++;
		}
		return num;
	}

	public void setContent(Object content) {
		_content = content;
	}

	public Object getContent() {
		return _content;
	}

	@Override
	public String toString() {
		return "<" + _tag + ">";
	}

	/**
	 * Find the first child tag of type 'tag'
	 * @param tag
	 * @param recurse
	 */
	public XmlNode findChildTag(String tag, boolean recurse) {
		for (XmlNode child : _children) {
			if (child.getTag().equals(tag)) {
				return child;
			}
		}
		// Note, this does a pseudo breadth first search
		if (recurse) {
			// Try all our children
			for (XmlNode child : _children) {
				XmlNode val = child.findChildTag(tag, true);

				if (val != null)
					return val;
			}
		}
		return null;
	}
}
