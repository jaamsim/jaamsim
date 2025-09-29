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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.List;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.jaamsim.basicsim.Log;
import com.jaamsim.render.RenderException;
import com.jaamsim.ui.LogBox;

/**
 * A simple DOM like parser that handles arrays of white space separated numbers. This is used by both the COLLADA parser and the
 * JaamSim mesh format parser
 * @author matt.chudleigh
 *
 */
public class XmlParser  extends DefaultHandler{

	private XmlNode rootNode;
	private XmlNode currentNode;

	private StringBuilder contentBuilder = new StringBuilder();

	// The _nodeIDMap is a mapping of fragment IDs to nodes to make data analysis easier
	private HashMap<String, XmlNode> nodeIDMap = new HashMap<>();

	private List<String> doubleArrayTags;
	private List<String> intArrayTags;
	private List<String> stringArrayTags;
	private List<String> booleanArrayTags;

	private URL content;

	public XmlParser(URL content) {

		this.content = content;

		rootNode = new XmlNode(null, "", "");
		currentNode = rootNode;
	}

	/**
	 * Sets the list of tags that should have their contents parsed as an array of doubles
	 * @param arrayNames
	 */
	public void setDoubleArrayTags(List<String> arrayNames) {
		doubleArrayTags = arrayNames;
	}
	/**
	 * Sets the list of tags that should have their contents parsed as an array of ints
	 * @param arrayNames
	 */
	public void setIntArrayTags(List<String> arrayNames) {
		intArrayTags = arrayNames;
	}
	/**
	 * Sets the list of tags that should have their contents parsed as an array of booleans
	 * @param arrayNames
	 */
	public void setBooleanArrayTags(List<String> arrayNames) {
		booleanArrayTags = arrayNames;
	}
	/**
	 * Sets the list of tags that should have their contents parsed as an array of strings
	 * @param arrayNames
	 */
	public void setStringArrayTags(List<String> arrayNames) {
		stringArrayTags = arrayNames;
	}

	public void parse() {
		InputStream in;
		try {
			in = content.openStream();
		} catch (IOException ex) {
			throw new RenderException("Can't read " + content);
		}

		SAXParserFactory factory = SAXParserFactory.newInstance();
		factory.setValidating(false);

		try {
			SAXParser saxParser = factory.newSAXParser();

			saxParser.parse(in, this);

		} catch (Exception e) {
			Log.logException(e);
			throw new RenderException(e.getMessage());
		}
	}

	@Override
	public void startElement(String uri, String localName, String name, Attributes attributes) throws SAXException {

		int IDIndex = attributes.getIndex("id");
		String fragID = null;
		if (IDIndex != -1) {
			fragID = attributes.getValue(IDIndex);
		}
		XmlNode node = new XmlNode(currentNode, name, fragID);
		currentNode.addChild(node);
		currentNode = node;
		for (int i = 0; i < attributes.getLength(); ++i) {
			if (i == IDIndex) continue; // This attribute is special and handled

			String n = attributes.getQName(i);
			String v = attributes.getValue(i);
			node.addAttrib(n, v);
		}
		if (IDIndex != -1) {
			nodeIDMap.put(fragID, node);
		}
		contentBuilder.setLength(0);
	}

	@Override
	public void characters(char [] ch, int start, int length) throws SAXException {
		contentBuilder.append(ch, start, length);
	}

	@Override
	public void endElement(String uri, String localName, String name) throws SAXException {
		// Handle the contents type based on the current nodes tag
		Object contents;
		if (doubleArrayTags.contains(name)) {
			contents = parseDoubleArray();
		} else if (intArrayTags.contains(name)) {
			contents = parseIntArray();
		} else if (booleanArrayTags.contains(name)) {
			contents = parseBooleanArray();
		} else if (stringArrayTags.contains(name)) {
			contents = parseStringArray();
		} else {
			contents = contentBuilder.toString().trim();
		}

		currentNode.setContent(contents);
		currentNode = currentNode.getParent();
		contentBuilder.setLength(0);
	}

	// return the number of 'words' in the contents
	private int wordCount() {

		int numWords = 0;
		boolean readingWord = false;
		for (int i = 0; i < contentBuilder.length(); ++i) {
			char c = contentBuilder.charAt(i);
			if (isWhitespace(c)) {
				if (readingWord) {
					// end of word
					readingWord = false;
				}
			} else {
				if (!readingWord) {
					// beginning of word
					readingWord = true;
					++numWords;
				}
			}
		}
		return numWords;
	}

	private static final boolean isWhitespace(char c) {
		return (c == ' ' || c == '\t' || c == '\n');
	}

	private int parsePos = 0;
	private String getWord() {
		StringBuilder ret = new StringBuilder();
		while (parsePos < contentBuilder.length()) {
			char c = contentBuilder.charAt(parsePos);
			// Read through leading whitespace
			if (!isWhitespace(c)) {
				break;
			}
			++parsePos;
		}

		while (parsePos < contentBuilder.length()) {
			char c = contentBuilder.charAt(parsePos++);
			// Build up the 'word'
			if (isWhitespace(c)) {
				return ret.toString();
			}
			ret.append(c);
		}
		return ret.toString();
	}

	private double[] parseDoubleArray() {

		int numWords = wordCount();
		parsePos = 0;

		double[] ret = new double[numWords];
		for (int i = 0; i < numWords; ++i) {
			String val = getWord();
			ret[i] = Double.parseDouble(val);
		}
		return ret;
	}

	private int[] parseIntArray() {
		int numWords = wordCount();
		parsePos = 0;

		int[] ret = new int[numWords];
		for (int i = 0; i < numWords; ++i) {
			String val = getWord();
			ret[i] = Integer.parseInt(val);
		}
		return ret;
	}

	private boolean[] parseBooleanArray() {

		int numWords = wordCount();
		parsePos = 0;

		boolean[] ret = new boolean[numWords];
		for (int i = 0; i < numWords; ++i) {
			String val = getWord();
			ret[i] = Boolean.parseBoolean(val);
		}
		return ret;
	}

	private String[] parseStringArray() {

		int numWords = wordCount();
		parsePos = 0;

		String[] ret = new String[numWords];
		for (int i = 0; i < numWords; ++i) {
			String val = getWord();
			ret[i] = val;
		}
		return ret;
	}

	public XmlNode getRootNode() {
		return rootNode;
	}

	public XmlNode getNodeByID(String id) {
		return nodeIDMap.get(id);
	}

}
