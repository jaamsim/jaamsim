/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2013 Ausenco Engineering Canada Inc.
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

import com.jaamsim.math.Mat4d;
import com.jaamsim.math.Quaternion;
import com.jaamsim.math.Vec3d;

public class Action {

	public static class Description {
		public String name;
		public double duration;
	}

	public static class Queue {
		public String name;
		public double time;

		@Override
		public boolean equals(Object o) {
			if (!(o instanceof Queue)) {
				return false;
			}
			Queue q = (Queue)o;
			return q.name == name && q.time == time;
		}
	}

	/**
	 * Used to bind entity outputs to active actions
	 * @author matt.chudleigh
	 *
	 */
	public static class Binding {
		public String actionName;
		public String outputName;
	}

	public static class RotKey {
		public double time;
		public Quaternion rot;
	}

	public static class TransKey {
		public double time;
		public Vec3d trans;
	}

	public static class Channel {
		public String name;
		public ArrayList<RotKey> rotKeys;
		public ArrayList<TransKey> transKeys;
	}

	// Actual members of Action
	public String name;
	public double duration;
	public ArrayList<Channel> channels = new ArrayList<>();

	// Calculate the interpolated rotation for this channel at time
	private static Quaternion interpRot(Action.Channel ch, double time) {
		if (ch.rotKeys == null || ch.rotKeys.size() == 0) {
			return new Quaternion(); // identity
		}
		if (time < ch.rotKeys.get(0).time) {
			return ch.rotKeys.get(0).rot; // Before the first key, so just take that value
		}
		if (time > ch.rotKeys.get(ch.rotKeys.size()-1).time) {
			return ch.rotKeys.get(ch.rotKeys.size()-1).rot; // past the end
		}
		for (int i = 0; i < ch.rotKeys.size() - 1; ++i) {
			Action.RotKey a = ch.rotKeys.get(i);
			Action.RotKey b = ch.rotKeys.get(i+1);
			if (time < a.time || time > b.time) continue;

			double bWeight = (time-a.time) / (b.time-a.time);
			Quaternion ret = new Quaternion();
			a.rot.slerp(b.rot, bWeight, ret);
			return ret;
		}
		assert(false);
		return new Quaternion();
	}

	// Calculate the interpolated tranlation for this channel at time
	private static Vec3d interpTrans(Action.Channel ch, double time) {
		if (ch.transKeys == null || ch.transKeys.size() == 0) {
			return new Vec3d(); // identity
		}
		if (time < ch.transKeys.get(0).time) {
			return ch.transKeys.get(0).trans; // Before the first key, so just take that value
		}
		if (time > ch.transKeys.get(ch.transKeys.size()-1).time) {
			return ch.transKeys.get(ch.transKeys.size()-1).trans; // past the end
		}
		for (int i = 0; i < ch.transKeys.size()-1; ++i) {
			Action.TransKey a = ch.transKeys.get(i);
			Action.TransKey b = ch.transKeys.get(i+1);
			if (time < a.time || time > b.time) continue;

			double bw = (time-a.time) / (b.time-a.time);
			double aw = 1 - bw;
			Vec3d ret = new Vec3d(	a.trans.x*aw + b.trans.x*bw,
									a.trans.y*aw + b.trans.y*bw,
									a.trans.z*aw + b.trans.z*bw);
			return ret;
		}
		assert(false);
		return new Vec3d();
	}

	/**
	 * Returns a Mat4d of the time interpolated value of the rotation and translation defined by Channel ch
	 * @param ch
	 * @param time
	 */
	public static Mat4d getChannelMatAtTime(Channel ch, double time) {
		Mat4d ret = new Mat4d();
		ret.setRot3(interpRot(ch, time));
		ret.setTranslate3(interpTrans(ch, time));
		return ret;
	}

}
