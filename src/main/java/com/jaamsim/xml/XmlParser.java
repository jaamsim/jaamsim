/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2012 Ausenco Engineering Canada Inc.
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
package com.jaamsim.xml;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.jaamsim.render.RenderException;

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
	private HashMap<String, XmlNode> nodeIDMap = new HashMap<String, XmlNode>();

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
			e.printStackTrace();
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
			ArrayList<String> strings = contentsToStringArray();
			String[] temp = new String[strings.size()];
			for (int i = 0; i < strings.size(); ++i) {
				temp[i] = strings.get(i);
			}
			contents = temp;
		} else {
			contents = contentBuilder.toString().trim();
		}

		currentNode.setContent(contents);
		currentNode = currentNode.getParent();
		contentBuilder.setLength(0);
	}

	private double[] parseDoubleArray() {
		ArrayList<String> strings = contentsToStringArray();

		double[] ret = new double[strings.size()];
		int index = 0;
		for (String s : strings) {
			ret[index++] = Double.parseDouble(s);
		}
		return ret;
	}

	private int[] parseIntArray() {
		ArrayList<String> strings = contentsToStringArray();

		int[] ret = new int[strings.size()];
		int index = 0;
		for (String s : strings) {
			ret[index++] = Integer.parseInt(s);
		}
		return ret;
	}

	private boolean[] parseBooleanArray() {
		ArrayList<String> strings = contentsToStringArray();

		boolean[] ret = new boolean[strings.size()];
		int index = 0;
		for (String s : strings) {
			ret[index++] = Boolean.parseBoolean(s);
		}
		return ret;
	}

	private ArrayList<String> contentsToStringArray() {
		ArrayList<String> ret = new ArrayList<String>();
		StringBuilder val = new StringBuilder();
		for (int i = 0; i < contentBuilder.length(); ++i) {
			char c = contentBuilder.charAt(i);
			if (c == ' ' || c == '\t' || c == '\n') {
				if (val.length() != 0) {
					ret.add(val.toString());
					val.setLength(0);
				}
			} else {
				val.append(c);
			}
		}
		if (val.length() != 0) {
			ret.add(val.toString());
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
