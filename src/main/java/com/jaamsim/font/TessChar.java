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
 * @param cp - the character this represents
 * @param numVerts - a list of vertices in the XY plane, will be stored and never written to
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
