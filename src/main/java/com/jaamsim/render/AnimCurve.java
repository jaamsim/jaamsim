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
		public Vec4d[] in;
		public Vec4d[] out;
		public Vec4d[] inTan;
		public Vec4d[] outTan;
		public String[] interp;
	}

	private double[] times;
	private double[] values;

	public static AnimCurve buildFromColCurve(ColCurve colData) {
		ArrayList<Double> ts = new ArrayList<>();
		ArrayList<Double> vs = new ArrayList<>();

		ts.add(colData.in[0].x);
		vs.add(colData.out[0].x);

		for (int i = 0; i < colData.in.length-1; ++i) {
			String interp = colData.interp[i];
			if (interp.equals("LINEAR")) {
				ts.add(colData.in[i+1].x);
				vs.add(colData.out[i+1].x);
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

					// Calculate the bezier result, see the collada 1.4.1 spec (page 4-4) for the source of this algorithm
					Vec2d p0 = new Vec2d(colData.in[i  ].x, colData.out[i  ].x);
					Vec2d p1 = new Vec2d(colData.in[i+1].x, colData.out[i+1].x);
					Vec2d c0 = colData.outTan[i];
					Vec2d c1 = colData.inTan[i+1];
					double oneMinS = 1 - s;
					double coeffP0 = oneMinS*oneMinS*oneMinS;
					double coeffC0 = 3*s*oneMinS*oneMinS;
					double coeffC1 = 3*s*s*oneMinS;
					double coeffP1 = s*s*s;

					ts.add(coeffP0*p0.x + coeffC0*c0.x + coeffC1*c1.x + coeffP1*p1.x);
					vs.add(coeffP0*p0.y + coeffC0*c0.y + coeffC1*c1.y + coeffP1*p1.y);
				}
				continue;
			}
			// Currently unsupported interpolation
			throw new RenderException(String.format("Unknown animation interpolation:%s", colData.interp[i]));
		}

		AnimCurve ret = new AnimCurve();
		ret.times = new double[ts.size()];
		ret.values = new double[vs.size()];
		assert(ts.size() == vs.size());

		for (int i = 0; i < ts.size(); ++i) {
			ret.times[i] = ts.get(i);
			ret.values[i] = vs.get(i);
		}
		return ret;
	}
	private AnimCurve() {
	}

	public double getValueForTime(double time) {
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
		int test = end/2;
		while ((end - start) > 1) {
			double samp = times[test];

			if (samp == time) // perfect match
				return values[test];

			if (samp < time) {
				start = test + 1;
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

		double v0 = values[start];
		double v1 = values[end];

		double slope = (v1-v0)/(t1-t0);
		return slope * (time - t0) + v0;
	}
}
