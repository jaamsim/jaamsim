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

import com.jaamsim.math.Vec2d;
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

				// Interpolate bezier as 4 linear segments (this may need to be improved at a later date)
				for (double s = 0.25; s <=1; s+=0.25) {

					Vec4d val = new Vec4d();
					double t = 0;

					double oneMinS = 1 - s;
					double coeffP0 = oneMinS*oneMinS*oneMinS;
					double coeffC0 = 3*s*oneMinS*oneMinS;
					double coeffC1 = 3*s*s*oneMinS;
					double coeffP1 = s*s*s;

					// Run the bezier solver for each component in the output vector
					for (int compInd = 0; compInd < colData.numComponents; ++compInd) {

						// Calculate the bezier result, see the collada 1.4.1 spec (page 4-4) for the source of this algorithm
						Vec2d p0 = new Vec2d(colData.in[i  ].x,    colData.out[i  ].getByInd(compInd));
						Vec2d p1 = new Vec2d(colData.in[i+1].x,    colData.out[i+1].getByInd(compInd));
						Vec2d c0 = new Vec2d(colData.outTan[i].x,  colData.outTan[i].getByInd(compInd + 1));
						Vec2d c1 = new Vec2d(colData.inTan[i+1].x, colData.inTan[i+1].getByInd(compInd + 1));

						t = (coeffP0*p0.x + coeffC0*c0.x + coeffC1*c1.x + coeffP1*p1.x);

						double v = coeffP0*p0.y + coeffC0*c0.y + coeffC1*c1.y + coeffP1*p1.y;
						val.setByInd(compInd, v);
					}
					ts.add(t);
					vs.add(val);
				}
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
