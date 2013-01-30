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

private Vec4d _center;
private double _radius;

public Sphere(Vec4d center, double radius) {
	_center = new Vec4d(center);
	_radius = radius;
}

public void setCenter(Vec4d cent) {
	_center.set4(cent);
}

public void setRadius(double radius) {
	_radius = radius;
}

public void getCenter(Vec4d centOut) {
	centOut.set4(_center);
}

public Vec4d getCenterRef() {
	return _center;
}

public double getRadius() {
	return _radius;
}

public double getDistance(Vec4d point) {
	Vec4d diff = new Vec4d(0.0d, 0.0d, 0.0d, 1.0d);
	diff.sub3(_center, point);
	double dist = diff.mag3();

	return dist - _radius;
}

public double getDistance(Sphere s) {

	Vec4d diff = new Vec4d(0.0d, 0.0d, 0.0d, 1.0d);
	diff.sub3(_center, s._center);
	double dist = diff.mag3();

	return dist - _radius - s._radius;
}

public double getDistance(Plane p) {
	double dist = p.getNormalDist(_center);
	return dist - _radius;
}

} // class Sphere
