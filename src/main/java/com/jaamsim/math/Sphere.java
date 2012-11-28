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
 * A simple geometric representation of a sphere. Internally is just a point and radius
 * @author Matt.Chudleigh
 *
 */
public class Sphere {

private Vector4d _center;
private double _radius;

public Sphere(Vector4d center, double radius) {
	_center = new Vector4d(center);
	_radius = radius;
}

public void setCenter(Vector4d cent) {
	_center.copyFrom(cent);
}

public void setRadius(double radius) {
	_radius = radius;
}

public void getCenter(Vector4d centOut) {
	centOut.copyFrom(_center);
}

public Vector4d getCenterRef() {
	return _center;
}

public double getRadius() {
	return _radius;
}

public double getDistance(Vector4d point) {
	Vector4d diff = new Vector4d();
	_center.sub3(point, diff);
	double dist = diff.mag3();

	return dist - _radius;
}

public double getDistance(Sphere s) {

	Vector4d diff = new Vector4d();
	_center.sub3(s._center, diff);
	double dist = diff.mag3();

	return dist - _radius - s._radius;
}

public double getDistance(Plane p) {
	double dist = p.getNormalDist(_center);
	return dist - _radius;
}

} // class Sphere
