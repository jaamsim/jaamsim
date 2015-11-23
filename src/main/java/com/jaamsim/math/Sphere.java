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
