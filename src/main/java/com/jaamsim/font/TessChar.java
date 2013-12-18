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
package com.jaamsim.font;

/**
 * A single representation of a tesselated character. Mostly a wrapper around a bounding box and
 * the triangle list (in the XY plane) needed to draw the character. This is rendering context agnostic
 * and will need to be interpreted by something that can actually put it on screen
 * @author Matt Chudleigh
 *
 */

public class TessChar {

/**
 * The code point this represents
 */
private int _cp;
/**
 * A list of vertices to make the character, only a dumb triangle list for now so the length
 * should be a multiple of 3
 */
//private double[] _vertices;

private int _startIndex;
private int _numVerts;

/**
 * The bounds of the character, includes padding so the bounds can be tightly packed, assumes
 * The character origin is at the lower left
 */
private double _width, _height;

/**
 * The horizontal advance of this character
 */
private double _advance;

/**
 * Creates a TessChar
 * @param c - the character this represents
 * @param vertices - a list of vertices in the XY plane, will be stored and never written to
 * @param width - width of the bounds
 * @param height - height of the bounds
 */
public TessChar(int cp, int startIndex, int numVerts, double width, double height, double advance) {
	_cp = cp;
	_width = width;
	_height = height;
	_advance = advance;
	_startIndex = startIndex;
	_numVerts = numVerts;
}

/**
 * Returns a list of alternating x,y coordinates for this character. Renders in the XY plane with Y up
 * @return
 */
//public double[] getVertices() {
//	return _vertices;
//}

public double getWidth() {
	return _width;
}

public double getHeight() {
	return _height;
}

public int getCodePoint() {
	return _cp;
}

public double getAdvance() {
	return _advance;
}

public int getStartIndex() {
	return _startIndex;
}

public int getNumVerts() {
	return _numVerts;
}
} // class TessChar
