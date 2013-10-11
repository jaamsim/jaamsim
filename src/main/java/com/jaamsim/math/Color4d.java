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
package com.jaamsim.math;

/**
 * A data structure to hold RGBA color information.
 */
public class Color4d {

public double r;
public double g;
public double b;
public double a;

public Color4d() {
	r = 0.0d; g = 0.0d; b = 0.0d; a = 1.0d;
}

public Color4d(double r, double g, double b) {
	this.r = r; this.g = g; this.b = b; this.a = 1.0d;
}

public Color4d(double r, double g, double b, double a) {
	this.r = r; this.g = g; this.b = b; this.a = a;
}

public Color4d(Color4d col) {
	this.r = col.r; this.g = col.g; this.b = col.b; this.a = col.a;
}

public float[] toFloats() {
	float[] ret = new float[4];
	ret[0] = (float)r; ret[1] = (float)g; ret[2] = (float)b; ret[3] = (float)a;
	return ret;
}

/**
 * Tests the first four components are exactly equal.
 *
 * This returns true if the r,g,b,a components compare as equal using the ==
 * operator.  Note that NaN will always return false, and -0.0 and 0.0
 * will compare as equal.
 * @throws NullPointerException if col is null
 */
public boolean equals4(Color4d c) {
	return r == c.r && g == c.g && b == c.b && a == c.a;
}

/**
 * Checks if all vector components are within EPSILON of each other
 * @param other - the other vector
 * @return
 */
@Override
public boolean equals(Object o)
{
	if (!(o instanceof Color4d))
		return false;

	Color4d c = (Color4d)o;

	return r == c.r && g == c.g && b == c.b && a == c.a;
}

@Override
public int hashCode() {
	return Double.valueOf(r).hashCode() +
	       Double.valueOf(g).hashCode() * 7 +
	       Double.valueOf(b).hashCode() * 79 +
	       Double.valueOf(a).hashCode() * 1239;
}

}
