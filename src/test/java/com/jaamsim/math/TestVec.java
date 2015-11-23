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

import static org.junit.Assert.assertTrue;

public class TestVec {

// Known single-value and sequence values
public static final Vec4d zero = new Vec4d(0.0d, 0.0d, 0.0d, 0.0d);
public static final Vec4d one = new Vec4d(1.0d, 1.0d, 1.0d, 1.0d);
public static final Vec4d negone = new Vec4d(-1.0d, -1.0d, -1.0d, -1.0d);
public static final Vec4d seq = new Vec4d(1.0d, 2.0d, 3.0d, 4.0d);
public static final Vec4d nseq = new Vec4d(-1.0d, -2.0d, -3.0d, -4.0d);

//Single-axis Vectors
public static final Vec4d xaxis = new Vec4d(1.0d, 0.0d, 0.0d, 0.0d);
public static final Vec4d yaxis = new Vec4d(0.0d, 1.0d, 0.0d, 0.0d);
public static final Vec4d zaxis = new Vec4d(0.0d, 0.0d, 1.0d, 0.0d);
public static final Vec4d waxis = new Vec4d(0.0d, 0.0d, 0.0d, 1.0d);

public static final Vec4d negxaxis = new Vec4d(-1.0d, 0.0d, 0.0d, 0.0d);
public static final Vec4d negyaxis = new Vec4d(0.0d, -1.0d, 0.0d, 0.0d);
public static final Vec4d negzaxis = new Vec4d(0.0d, 0.0d, -1.0d, 0.0d);
public static final Vec4d negwaxis = new Vec4d(0.0d, 0.0d, 0.0d, -1.0d);

// Used to test partially zeroed sequence
public static final Vec4d zseq2 = new Vec4d(0.0d, 0.0d, 3.0d, 4.0d);
public static final Vec4d zseq3 = new Vec4d(0.0d, 0.0d, 0.0d, 4.0d);
public static final Vec4d zseq4 = new Vec4d(0.0d, 0.0d, 0.0d, 0.0d);

// Used to test partially added sequences
public static final Vec4d seqadd2 = new Vec4d(2.0d, 4.0d, 3.0d, 4.0d);
public static final Vec4d seqadd3 = new Vec4d(2.0d, 4.0d, 6.0d, 4.0d);
public static final Vec4d seqadd4 = new Vec4d(2.0d, 4.0d, 6.0d, 8.0d);

//Used to test partially subtracted values
public static final Vec4d seqsub2 = new Vec4d(0.0d, 1.0d, 3.0d, 4.0d);
public static final Vec4d seqsub3 = new Vec4d(0.0d, 1.0d, 2.0d, 4.0d);
public static final Vec4d seqsub4 = new Vec4d(0.0d, 1.0d, 2.0d, 3.0d);

//Used to test partially multiplied values
public static final Vec4d seqmul2 = new Vec4d(1.0d, 4.0d, 3.0d, 4.0d);
public static final Vec4d seqmul3 = new Vec4d(1.0d, 4.0d, 9.0d, 4.0d);
public static final Vec4d seqmul4 = new Vec4d(1.0d, 4.0d, 9.0d, 16.0d);

//Used to test max/min values
public static final Vec4d seqmin2 = new Vec4d(-1.0d, -2.0d,  3.0d,  4.0d);
public static final Vec4d seqmin3 = new Vec4d(-1.0d, -2.0d, -3.0d,  4.0d);
public static final Vec4d seqmin4 = new Vec4d(-1.0d, -2.0d, -3.0d, -4.0d);
public static final Vec4d seqmax2 = new Vec4d(1.0d, 2.0d, -3.0d, -4.0d);
public static final Vec4d seqmax3 = new Vec4d(1.0d, 2.0d,  3.0d, -4.0d);
public static final Vec4d seqmax4 = new Vec4d(1.0d, 2.0d,  3.0d,  4.0d);

//Used to test scaling
public static final Vec4d scaleseq2 = new Vec4d(2.0d, 4.0d, 3.0d, 4.0d);
public static final Vec4d scaleseq3 = new Vec4d(2.0d, 4.0d, 6.0d, 4.0d);
public static final Vec4d scaleseq4 = new Vec4d(2.0d, 4.0d, 6.0d, 8.0d);

//Used to test interpolation between seq and scaleseq
public static final Vec4d interpseq2 = new Vec4d(1.5d, 3.0d, 3.0d, 4.0d);
public static final Vec4d interpseq3 = new Vec4d(1.5d, 3.0d, 4.5d, 4.0d);
public static final Vec4d interpseq4 = new Vec4d(1.5d, 3.0d, 4.5d, 6.0d);

//Assorted garbage matrices to test fallbacks.
public static final Vec4d posInf = new Vec4d(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);
public static final Vec4d negInf = new Vec4d(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY);
public static final Vec4d mixInf = new Vec4d(Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY);
public static final Vec4d allNaN = new Vec4d(Double.NaN, Double.NaN, Double.NaN, Double.NaN);
public static final Vec4d mixInfNaN = new Vec4d(Double.NaN, Double.POSITIVE_INFINITY, Double.NaN, Double.POSITIVE_INFINITY);
public static final Vec4d oneNaN = new Vec4d(Double.NaN, 0.0d, 0.0d, 0.0d);

//Tests the degenerate normalize cases
public static final Vec4d deg2posInf = new Vec4d(0.0d, 1.0d, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);
public static final Vec4d deg3posInf = new Vec4d(0.0d, 0.0d, 1.0d, Double.POSITIVE_INFINITY);

public static final Vec4d deg2negInf = new Vec4d(0.0d, 1.0d, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY);
public static final Vec4d deg3negInf = new Vec4d(0.0d, 0.0d, 1.0d, Double.NEGATIVE_INFINITY);

public static final Vec4d deg2mixInf = new Vec4d(0.0d, 1.0d, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY);
public static final Vec4d deg3mixInf = new Vec4d(0.0d, 0.0d, 1.0d, Double.NEGATIVE_INFINITY);

public static final Vec4d deg2allNaN = new Vec4d(0.0d, 1.0d, Double.NaN, Double.NaN);
public static final Vec4d deg3allNaN = new Vec4d(0.0d, 0.0d, 1.0d, Double.NaN);

public static final Vec4d deg2mixInfNaN = new Vec4d(0.0d, 1.0d, Double.NaN, Double.POSITIVE_INFINITY);
public static final Vec4d deg3mixInfNaN = new Vec4d(0.0d, 0.0d, 1.0d, Double.POSITIVE_INFINITY);

//Tests for the mult cases
public static final Vec4d mult2 = new Vec4d(3.0d, 11.0d, 0.0d, 0.0d);
public static final Vec4d mult2tpose = new Vec4d(6.0d, 8.0d, 0.0d, 0.0d);
public static final Vec4d mult3 = new Vec4d(6.0d, 18.0d, 30.0d, 0.0d);
public static final Vec4d mult3tpose = new Vec4d(15.0d, 18.0d, 21.0d, 0.0d);
public static final Vec4d mult4 = new Vec4d(10.0d, 26.0d, 42.0d, 58.0d);
public static final Vec4d mult4tpose = new Vec4d(28.0d, 32.0d, 36.0d, 40.0d);

private static void assertEqual(double val, double exp, String msg) {
	assertTrue(msg, val == exp);
}

private static void assertEqualNaN(double val, double exp, String msg) {
	assertTrue(msg, val == exp || Double.isNaN(val) == Double.isNaN(exp));
}

private static final double EPS = 1e-12d;
public static final void assertNear(double d1, double d2) {
	assertTrue(Math.abs(d1 - d2) < EPS);
}

public static final void assertEqual(Vec2d val, Vec4d exp) {
	assertEqual(val.x, exp.x, "X");
	assertEqual(val.y, exp.y, "Y");
}

public static final void assertEqual(Vec3d val, Vec4d exp) {
	assertEqual(val.x, exp.x, "X");
	assertEqual(val.y, exp.y, "Y");
	assertEqual(val.z, exp.z, "Z");
}

public static final void assertEqual(Vec4d val, Vec4d exp) {
	assertEqual(val.x, exp.x, "X");
	assertEqual(val.y, exp.y, "Y");
	assertEqual(val.z, exp.z, "Z");
	assertEqual(val.w, exp.w, "W");
}

public static final void assertEqualNaN(Vec2d val, Vec4d exp) {
	assertEqualNaN(val.x, exp.x, "X");
	assertEqualNaN(val.y, exp.y, "Y");
}

public static final void assertEqualNaN(Vec3d val, Vec4d exp) {
	assertEqualNaN(val.x, exp.x, "X");
	assertEqualNaN(val.y, exp.y, "Y");
	assertEqualNaN(val.z, exp.z, "Z");
}

public static final void assertEqualNaN(Vec4d val, Vec4d exp) {
	assertEqualNaN(val.x, exp.x, "X");
	assertEqualNaN(val.y, exp.y, "Y");
	assertEqualNaN(val.z, exp.z, "Z");
	assertEqualNaN(val.w, exp.w, "W");
}

public static void assertNear(Vec4d v, Vec4d v4) {
	assertNear(v.x, v4.x);
	assertNear(v.y, v4.y);
	assertNear(v.z, v4.z);
	assertNear(v.w, v4.w);
}

public static void assertNear(Vec3d v, Vec3d v4) {
	assertNear(v.x, v4.x);
	assertNear(v.y, v4.y);
	assertNear(v.z, v4.z);
}

public void printVec(Vec4d vec) {
	System.out.format("%.20f %.20f %.20f %.20f%n", vec.x, vec.y, vec.z, vec.w);
}
}
