/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2015 KMA Technologies
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
package com.jaamsim.render;

import java.util.ArrayList;

import com.jaamsim.math.Vec4d;

public class AnimCurve {

	// A holder for collada curve information
	public static class ColCurve {
		public int numComponents;
		public Vec4d[] in;
		public Vec4d[] out;
		public Vec4d[] inTan;
		public Vec4d[] outTan;
		public String[] interp;
	}

	public double[] times;
	public Vec4d[] values;

	// Cache the last value as this may be requested several times in a row
	private Vec4d lastVal;
	private double lastTime;

	// Animation curves represent up to 4 values at a given time
	private static class CurveVal {
		public double time;
		public Vec4d vals;
	}

	private static CurveVal solveBezier(double s, Vec4d p0, Vec4d p1, Vec4d c0, Vec4d c1, int numComponents) {

		double oneMinS = 1 - s;
		double coeffP0 = oneMinS*oneMinS*oneMinS;
		double coeffC0 = 3*s*oneMinS*oneMinS;
		double coeffC1 = 3*s*s*oneMinS;
		double coeffP1 = s*s*s;

		CurveVal ret = new CurveVal();

		ret.time = (coeffP0*p0.x + coeffC0*c0.x + coeffC1*c1.x + coeffP1*p1.x);

		ret.vals = new Vec4d();
		for (int compInd = 0; compInd < numComponents; ++compInd) {
			double v = coeffP0*p0.getByInd(compInd+1) +
			           coeffC0*c0.getByInd(compInd+1) +
			           coeffC1*c1.getByInd(compInd+1) +
			           coeffP1*p1.getByInd(compInd+1);

			ret.vals.setByInd(compInd, v);
		}
		return ret;
	}

	// Add bezier keys to allow for smooth interpolation on the parameter range (s0, s1)
	// This does not insert the end points
	public static void interpBezier(double s0, double s1, Vec4d p0, Vec4d p1, Vec4d c0, Vec4d c1, int numComponents, ArrayList<Double> ts, ArrayList<Vec4d> vs) {
		if ((s1-s0)<= 1/16.0) {
			return; // Do not divide a curve into more than 16 parts
		}

		double halfS = (s0 + s1)/2.0;
		CurveVal startVal = solveBezier(s0, p0, p1, c0, c1, numComponents);
		CurveVal endVal = solveBezier(s1, p0, p1, c0, c1, numComponents);
		// Determine if this midpoint has value

		boolean recurse = false;
		for (double samp = 0.25; samp < 1; samp+=0.25) {
			double sampS = s0 + samp*(s1-s0);
			CurveVal sampVal = solveBezier(sampS, p0, p1, c0, c1, numComponents);

			for (int i = 0; i < numComponents; ++i) {
				double slope = (endVal.vals.getByInd(i) - startVal.vals.getByInd(i))/(endVal.time - startVal.time);
				double sampSlope = (sampVal.vals.getByInd(i) - startVal.vals.getByInd(i))/(sampVal.time - startVal.time);

				if (slope == 0) {
					if (sampSlope != 0)
						recurse = true;

					continue;
				}
				double scaledSlope = sampSlope/slope;

				if (Math.abs(scaledSlope-1) > 0.05) {
					recurse = true;
				}
			}
		}

		if (!recurse) {
			return;
		}
		// Otherwise recurse
		interpBezier(s0, halfS, p0, p1, c0, c1, numComponents, ts, vs);

		CurveVal midVal = solveBezier(halfS, p0, p1, c0, c1, numComponents);
		ts.add(midVal.time);
		vs.add(midVal.vals);

		interpBezier(halfS, s1, p0, p1, c0, c1, numComponents, ts, vs);
	}

	public static AnimCurve buildFromColCurve(ColCurve colData) {
		ArrayList<Double> ts = new ArrayList<>();
		ArrayList<Vec4d> vs = new ArrayList<>();

		ts.add(colData.in[0].x);
		vs.add(colData.out[0]);

		for (int i = 0; i < colData.in.length-1; ++i) {
			String interp = colData.interp[i];
			if (interp.equals("LINEAR")) {
				ts.add(colData.in[i+1].x);
				vs.add(colData.out[i+1]);
				continue;
			}
			if (interp.equals("BEZIER")) {
				if (colData.inTan == null) {
					throw new RenderException("Missing IN_TANGENT component in collada bezier animation");
				}
				if (colData.outTan == null) {
					throw new RenderException("Missing OUT_TANGENT component in collada bezier animation");
				}

				Vec4d p0 = new Vec4d();
				Vec4d p1 = new Vec4d();

				// Due to collada weirdness we need to reassemble the basic bezier position vectors
				p0.x = colData.in[i  ].x;
				p1.x = colData.in[i+1].x;
				for (int compInd = 0; compInd < colData.numComponents; ++compInd) {
					p0.setByInd(compInd+1, colData.out[i  ].getByInd(compInd));
					p1.setByInd(compInd+1, colData.out[i+1].getByInd(compInd));
				}

				Vec4d c0 = colData.outTan[i];
				Vec4d c1 = colData.inTan[i+1];

				interpBezier(0, 1, p0, p1, c0, c1, colData.numComponents, ts, vs);

				// Add the end point
				CurveVal val = solveBezier(1, p0, p1, c0, c1, colData.numComponents);
				ts.add(val.time);
				vs.add(val.vals);

				continue;
			}
			// Currently unsupported interpolation
			throw new RenderException(String.format("Unknown animation interpolation:%s", colData.interp[i]));
		}

		AnimCurve ret = new AnimCurve();
		ret.times = new double[ts.size()];
		ret.values = new Vec4d[vs.size()];
		assert(ts.size() == vs.size());

		for (int i = 0; i < ts.size(); ++i) {
			ret.times[i] = ts.get(i);
			ret.values[i] = vs.get(i);
		}
		return ret;
	}
	private AnimCurve() {
	}

	public Vec4d getValueForTime(double time) {
		if (lastVal != null && lastTime == time) {
			return lastVal;
		}

		// Check if we are past the ends
		if (time <= times[0]) {
			return values[0];
		}

		if (time >= times[times.length-1]) {
			return values[values.length -1];
		}

		// Basic binary search for appropriate segment
		int start = 0;
		int end = times.length;
		while ((end - start) > 1) {
			int test = (start + end)/2;
			double samp = times[test];

			if (samp == time) // perfect match
				return values[test];

			if (samp < time) {
				start = test;
			} else {
				end = test;
			}
		}

		assert(end - start == 1);

		// Linearly interpolate on the segment
		double t0 = times[start];
		double t1 = times[end];
		assert(time >= t0);
		assert(time <= t1);

		Vec4d v0 = values[start];
		Vec4d v1 = values[end];
		double scale = (time - t0)/(t1-t0);

		Vec4d ret = new Vec4d(v0);
		ret.scale4(1-scale);

		Vec4d temp = new Vec4d(v1);
		temp.scale4(scale);

		ret.add4(temp);

		lastTime = time;
		lastVal = ret;
		return ret;

	}
}
