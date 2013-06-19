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

public final Vec3d center;
public double radius;

public Sphere(Vec3d center, double radius) {
	this.center = new Vec3d(center);
	this.radius = radius;
}

public double getDistance(Vec3d point) {
	Vec3d diff = new Vec3d();
	diff.sub3(center, point);

	double dist = diff.mag3();
	return dist - radius;
}

public double getDistance(Sphere s) {
	Vec3d diff = new Vec3d();
	diff.sub3(center, s.center);
	double dist = diff.mag3();

	return dist - radius - s.radius;
}

public double getDistance(Plane p) {
	double dist = p.getNormalDist(center);
	return dist - radius;
}
}
