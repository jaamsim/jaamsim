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

import java.util.Comparator;

/**
 * Vector4d is a 4 space vector, stored internally as doubles.
 * This is the basic vector class for JaamRender, and is intended to be used for
 * both point (w = 1) and vector (w = 0) representation
 * @author Matt.Chudleigh
 *
 */
public class Vector4d {

public double[] data;


public static final Vector4d ORIGIN = new Vector4d(0, 0, 0, 1);
public static final Vector4d ONES = new Vector4d(1, 1, 1, 1); // Useful for things that want a scale

public static final Vector4d X_AXIS = new Vector4d(1, 0, 0, 0);
public static final Vector4d Y_AXIS = new Vector4d(0, 1, 0, 0);
public static final Vector4d Z_AXIS = new Vector4d(0, 0, 1, 0);

public static final Vector4d NEG_X_AXIS = new Vector4d(-1, 0, 0, 0);
public static final Vector4d NEG_Y_AXIS = new Vector4d(0, -1, 0, 0);
public static final Vector4d NEG_Z_AXIS = new Vector4d(0, 0, -1, 0);

public Vector4d()
{
	data = new double[4];
	data[0] = 0.0;
	data[1] = 0.0;
	data[2] = 0.0;
	data[3] = 1.0;
}

/**
 * Constructor, w parameter is set to 1
 * @param x
 * @param y
 * @param z
 */
public Vector4d(double x, double y, double z)
{
	data = new double[4];
	data[0] = x;
	data[1] = y;
	data[2] = z;
	data[3] = 1.0;
}

/**
 * Constructor, values set as expected
 * @param x
 * @param y
 * @param z
 * @param w
 */
public Vector4d(double x, double y, double z, double w)
{
	data = new double[4];
	data[0] = x;
	data[1] = y;
	data[2] = z;
	data[3] = w;
}

public Vector4d(double[] vals) {
	if (vals.length != 3 || vals.length != 4) {
		throw new IllegalArgumentException("Vector must be initialized with 3 or 4 values");
	}

	data = new double[4];
	for (int i = 0; i < vals.length; ++i)
	{
		data[i] = vals[i];
	}
	if (vals.length < 4)
	{
		vals[3] = 1.0;
	}

}

public Vector4d(final Vector4d v)
{
	data = new double[4];
	for (int i = 0; i < 4; ++i) {
		data[i] = v.data[i];
	}

}

/**
 * Convenience to set the internals
 * @param x
 * @param y
 * @param z
 */
public void set(double x, double y, double z)
{
	data[0] = x;
	data[1] = y;
	data[2] = z;
}

/**
 * Convenience to set the internals
 * @param x
 * @param y
 * @param z
 * @param w
 */
public void set(double x, double y, double z, double w)
{
	data[0] = x;
	data[1] = y;
	data[2] = z;
	data[3] = w;
}

public void copyFrom(Vector4d v) {
	for (int i = 0; i < 4; ++i) {
		data[i] = v.data[i];
	}
}

/**
 * Truncate the internal values to floats, may be useful in some cases
 * @return The values as floats
 */
public float[] toFloats() {
	float[] floats = new float[4];
	for (int i = 0; i < 4; ++i)
	{
		floats[i] = (float) data[i];
	}
	return floats;
}

/**
 * Truncate the internal values to floats, may be useful in some cases
 * @return The values as floats
 */
public float[] toFloats3() {
	float[] floats = new float[3];
	for (int i = 0; i < 3; ++i)
	{
		floats[i] = (float) data[i];
	}
	return floats;
}

/**
 * Return the dot product of the first 2 components of this vector
 * @param rhs - The other vector
 * @return
 */

public double dot2(Vector4d rhs) {
	double ret = 0;
	for (int i = 0; i < 2; ++i)
	{
		ret += data[i] * rhs.data[i];
	}
	return ret;
}

/**
 * Return the dot product of the first 3 components of this vector
 * @param rhs - The other vector
 * @return
 */

public double dot3(Vector4d rhs) {
	double ret = 0;
	for (int i = 0; i < 3; ++i)
	{
		ret += data[i] * rhs.data[i];
	}
	return ret;
}

/**
 * Return the dot product of all 4 components of this vector
 * @param rhs - The other vector
 * @return
 */

public double dot4(Vector4d rhs) {
	double ret = 0;
	for (int i = 0; i < 4; ++i)
	{
		ret += data[i] * rhs.data[i];
	}
	return ret;
}
/**
 * Cross product of the 4 vectors
 * @param rhs - The right hand side operand
 * @param out - Output vector, sets out.w = 1
 */

public void cross(Vector4d rhs, Vector4d out) {
	double x = data[1] * rhs.data[2] - data[2] * rhs.data[1];
	double y = data[2] * rhs.data[0] - data[0] * rhs.data[2];
	double z = data[0] * rhs.data[1] - data[1] * rhs.data[0];

	out.data[0] = x;
	out.data[1] = y;
	out.data[2] = z;
	out.data[3] = 1.0d;
}

/**
 * Add all 4 vector components
 * @param rhs - right hand side
 * @param out - result is stored here
 */
public void add4(Vector4d rhs, Vector4d out) {
	for (int i = 0; i < 4; ++i)
	{
		out.data[i] = data[i] + rhs.data[i];
	}
}

/**
 * Add the first 3 vector components
 * @param rhs - right hand side
 * @param out - result is stored here
 */
public void add3(Vector4d rhs, Vector4d out) {
	for (int i = 0; i < 3; ++i)
	{
		out.data[i] = data[i] + rhs.data[i];
	}
}

/**
 * Add all 4 vector components to the existing value
 * @param rhs - right hand side
 */
public void addLocal4(Vector4d rhs) {
	for (int i = 0; i < 4; ++i)
	{
		data[i] += rhs.data[i];
	}
}

/**
 * Add the first 3 vector components to the existing value
 * @param rhs - right hand side
 */
public void addLocal3(Vector4d rhs) {
	for (int i = 0; i < 3; ++i)
	{
		data[i] += rhs.data[i];
	}
}

/**
 * Sub all 4 components
 * @param rhs - other value
 * @param out - result
 */
public void sub4(Vector4d rhs, Vector4d out) {
	for (int i = 0; i < 4; ++i)
	{
		out.data[i] = data[i] - rhs.data[i];
	}
}

/**
 * Sub first 3 components
 * @param rhs - other value
 * @param out - result
 */
public void sub3(Vector4d rhs, Vector4d out) {
	for (int i = 0; i < 3; ++i)
	{
		out.data[i] = data[i] - rhs.data[i];
	}
}

/**
 * Sub rhs from this, stored in this, applied to all 4
 * @param rhs - other value
 */
public void subLocal4(Vector4d rhs) {
	for (int i = 0; i < 4; ++i)
	{
		data[i] -= rhs.data[i];
	}
}

/**
 * Sub rhs from this, stored in this, applied to first 3
 * @param rhs - other value
 */
public void subLocal3(Vector4d rhs) {
	for (int i = 0; i < 3; ++i)
	{
		data[i] -= rhs.data[i];
	}
}

/**
 * Scale the vector by scalar 'scale'. Applied to all 4 components
 * @param scale - the scale factor
 * @param out - result
 */
public void scale4(double scale, Vector4d out) {
	for (int i = 0; i < 4; ++i)
	{
		out.data[i] = data[i] * scale;
	}
}

/**
 * Scale the vector by scalar 'scale'. Applied to first 3 components
 * @param scale - the scale factor
 * @param out - result
 */
public void scale3(double scale, Vector4d out) {
	for (int i = 0; i < 3; ++i)
	{
		out.data[i] = data[i] * scale;
	}
}

public void scaleLocal4(double scale) {
	for (int i = 0; i < 4; ++i)
	{
		data[i] *= scale;
	}
}

public void scaleLocal3(double scale) {
	for (int i = 0; i < 3; ++i)
	{
		data[i] *= scale;
	}
}

/**
 * The magnitude squared of the first 2 components
 * @return magnitude squared
 */
public double magSquared2() {
	double ret = 0;
	for (int i = 0; i < 2; ++i)
	{
		ret += data[i] * data[i];
	}
	return ret;
}

/**
 * The magnitude of this vector (2 components)
 * @return the magnitude
 */
public double mag2() {
	return Math.sqrt(magSquared2());
}

/**
 * The magnitude squared of the first 3 components
 * @return magnitude squared
 */
public double magSquared3() {
	double ret = 0;
	for (int i = 0; i < 3; ++i)
	{
		ret += data[i] * data[i];
	}
	return ret;
}

/**
 * The magnitude of this vector (3 components)
 * @return the magnitude
 */
public double mag3() {
	return Math.sqrt(magSquared3());
}

/**
 * The magnitude squared of all 4 components
 * @return magnitude squared
 */
public double magSquared4() {
	double ret = 0;
	for (int i = 0; i < 4; ++i)
	{
		ret += data[i] * data[i];
	}
	return ret;
}

/**
 * The magnitude of this vector (4 components)
 * @return the magnitude
 */
public double mag4() {
	return Math.sqrt(magSquared4());
}

/**
 * Normalize this vector (4 components)
 * @param out - The result
 */
public void normalize4(Vector4d out) {
	double length = mag4();
	if (length <= Constants.EPSILON)
	{
		// Return the x axis (or we could throw...)
		out.data[0] = 1;
		out.data[1] = 0;
		out.data[2] = 0;
		out.data[3] = 0;
		return;
	}

	for (int i = 0; i < 4; ++i)
	{
		out.data[i] = data[i] / length;
	}
}

/**
 * Normalize this vector (3 components)
 * @param out - The result
 */
public void normalize3(Vector4d out) {
	double length = mag3();
	if (length <= Constants.EPSILON)
	{
		// Return the x axis (or we could throw...)
		out.data[0] = 1;
		out.data[1] = 0;
		out.data[2] = 0;
		out.data[3] = data[3];
		return;
	}

	for (int i = 0; i < 3; ++i)
	{
		out.data[i] = data[i] / length;
	}
	out.data[3] = data[3];
}

/**
 * Normalize this vector and store the result here (4 components)
 */
public void normalizeLocal4() {
	double length = mag4();
	if (length <= Constants.EPSILON)
	{
		// Return the x axis (or we could throw...)
		data[0] = 1;
		data[1] = 0;
		data[2] = 0;
		data[3] = 0;
		return;
	}

	for (int i = 0; i < 4; ++i)
	{
		data[i] /= length;
	}
}

/**
 * Normalize this vector and store the result here (3 components)
 */
public void normalizeLocal3() {
	double length = mag3();
	if (length <= Constants.EPSILON)
	{
		// Return the x axis (or we could throw...)
		data[0] = 1;
		data[1] = 0;
		data[2] = 0;
		return;
	}

	for (int i = 0; i < 3; ++i)
	{
		data[i] /= length;
	}
}

public void clear() {
	for (int i = 0; i < 4; ++i)
	{
		data[i] = 0;
	}
}

/**
 * Checks if all vector components are within EPSILON of each other
 * @param other - the other vector
 * @return
 */
@Override
public boolean equals(Object o)
{
	if (!(o instanceof Vector4d)) return false;

	Vector4d other = (Vector4d)o;

	// Object equality is naturally equal
	if (other == this) { return true; }

	for (int i = 0; i < 4; ++i) {
		if (!MathUtils.near(data[i], other.data[i]))
			return false;
	}

	return true;
}

@Override
public int hashCode() {
	return Double.valueOf(data[0]).hashCode() +
	       Double.valueOf(data[1]).hashCode() * 7 +
	       Double.valueOf(data[2]).hashCode() * 79 +
	       Double.valueOf(data[3]).hashCode() * 1239;
}

/**
 * Checks if the first 3 components are within EPSILON of each other
 * @param other - the other vector
 * @return
 */
public boolean equals3(Vector4d other)
{
	// Quick out if these are the same object
	if (other == this) {
		return true;
	}

	for (int i = 0; i < 3; ++i) {
		if (!MathUtils.near(data[i], other.data[i]))
			return false;
	}

	return true;
}

public String toString()
{
	return "[" + data[0] + ", "  + data[1] + ", "  + data[2] + ", "  + data[3] + "]";
}

public double x() {
	return data[0];
}
public double y() {
	return data[1];
}
public double z() {
	return data[2];
}
public double w() {
	return data[3];
}

/**
 * Truncate the significand to only a few bits, this is a ridiculous function
 * that only really makes sense with the sorting comparator in order to give a reliable sorting
 * order. This method returns a double, except it is truncated to only 4 bits of mantissa
 * @param d
 * @return
 */
private static double binaryTruncate(double d) {
	long l = Double.doubleToLongBits(d);
	l = l & 0xFFFF000000000000L;
	return Double.longBitsToDouble(l);
}

public static Comparator<Vector4d> COMP = new Comparator<Vector4d>()
{

	@Override
	public int compare(Vector4d v0, Vector4d v1) {
		// Sorts on X, then Y, then Z, then W based. The algorithm moves on to the next coordinate if the current values are 'near' each other

		for (int i = 0; i < 4; ++i) {
			double d0 = binaryTruncate(v0.data[i]);
			double d1 = binaryTruncate(v1.data[i]);
			if (d0 < d1) {
				return -1;
			}
			if (d0 > d1) {
				return 1;
			}

		}

		return 0;
	}

};

} // class
