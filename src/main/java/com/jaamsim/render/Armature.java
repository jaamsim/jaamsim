/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2013 Ausenco Engineering Canada Inc.
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

package com.jaamsim.render;

 import java.util.ArrayList;

import com.jaamsim.math.Mat4d;
import com.jaamsim.math.Quaternion;
import com.jaamsim.math.Vec3d;

 /**
  * A basic data holder for armature information
  * @author matt.chudleigh
  *
  */
public class Armature {

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

	public static class ArmAction {
		public String name;
		public double duration;
		public ArrayList<Channel> channels = new ArrayList<Channel>();
	}

	public static class Bone {
		private String name;
		private Mat4d mat;
		private Mat4d invMat;
		private Bone parent;
		private double length;
		private int index;
		private final ArrayList<Bone> children = new ArrayList<Bone>();
		public Bone(String name, Mat4d mat, Bone parent, double length, int index) {
			this.name = name;
			this.mat = new Mat4d(mat);
			this.parent = parent;
			this.length = length;
			this.invMat = mat.inverse();
			this.index = index;
		}

		public String getName() {
			return name;
		}
		/**
		 * Returns the bone space to model space matrix
		 * @return
		 */
		public Mat4d getMatrix() {
			return mat;
		}
		public Mat4d getInvMatrix() {
			return invMat;
		}
		public Bone getParent() {
			return parent;
		}
		public ArrayList<Bone> getChildren() {
			return children;
		}
		public double getLength() {
			return length;
		}
		public int getIndex() {
			return index;
		}
	}

	private final ArrayList<Bone> bones = new ArrayList<Bone>();
	private final ArrayList<ArmAction> actions = new ArrayList<ArmAction>();

	public void addAction(ArmAction act) {
		actions.add(act);
	}

	public void addBone(String boneName, Mat4d matrix, String parentName, double length) {
		Bone parent = null;
		if (parentName != null) {
			parent = getBoneByName(parentName);
			if (parent == null) {
				throw new RenderException(String.format("Could not find parent bone: %s", parentName));
			}
		}
		if (getBoneByName(boneName) != null) {
			throw new RenderException(String.format("Multiple bones of the same name: %s", boneName));
		}
		Bone b = new Bone(boneName, matrix, parent, length, bones.size());
		bones.add(b);
		if (parent != null) {
			parent.children.add(b);
		}
	}

	public Bone getBoneByName(String name) {
		for (Bone b : bones) {
			if (b.name.equals(name)) {
				return b;
			}
		}
		return null;
	}

	public ArrayList<Bone> getRootBones() {
		ArrayList<Bone> ret = new ArrayList<Bone>();
		for (Bone b : bones) {
			if (b.parent == null) {
				ret.add(b);
			}
		}
		return ret;
	}

	public ArrayList<Bone> getAllBones() {
		return bones;
	}

	public ArrayList<ArmAction> getActions() {
		return actions;
	}

	private ArmAction getActionByName(String name) {
		for (ArmAction a : actions) {
			if (a.name.equals(name))
				return a;
		}
		return null;
	}

	// Calculate the interpolated rotation for this channel at time
	private Quaternion interpRot(Channel ch, double time) {
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
			RotKey a = ch.rotKeys.get(i);
			RotKey b = ch.rotKeys.get(i+1);
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
	private Vec3d interpTrans(Channel ch, double time) {
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
			TransKey a = ch.transKeys.get(i);
			TransKey b = ch.transKeys.get(i+1);
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

	public int getBoneIndex(String name) {
		for (int i = 0; i < bones.size(); ++i) {
			if (bones.get(i).name.equals(name))
				return i;
		}
		return -1;
	}

	/**
	 * Returns a 'pose' (a list of matrices for known bones) based on the actions for this skeleton
	 * @param actions
	 * @return
	 */
	public ArrayList<Mat4d> getPose(ArrayList<Action.Queue> actions) {
		if (actions == null) {
			actions = new ArrayList<Action.Queue>();
		}

		ArrayList<Mat4d> poseTransforms = new ArrayList<Mat4d>(bones.size());
		for (int i = 0; i < bones.size(); ++i)
			poseTransforms.add(new Mat4d());

		for (Action.Queue aq : actions) {
			ArmAction a = getActionByName(aq.name);
			if (a == null) {
				continue;
			}

			for (Channel ch : a.channels) {
				int boneInd = getBoneIndex(ch.name);
				assert(boneInd != -1);
				Mat4d mat = new Mat4d();
				mat.setRot3(interpRot(ch, aq.time));
				mat.setTranslate3(interpTrans(ch, aq.time));
				poseTransforms.get(boneInd).mult4(mat);
			}
		}

		ArrayList<Mat4d> ret = new ArrayList<Mat4d>(bones.size());

		// We have the interpolated transform per bone, now build up a list of model space transforms per bone
		for (int i = 0; i < bones.size(); ++i) {
			Bone b = bones.get(i);
			Mat4d mat = new Mat4d(b.mat);
			mat.mult4(poseTransforms.get(i));
			mat.mult4(b.invMat);

			if (b.parent != null) {
				// Need to include the parent of this bone
				int parentBoneInd = b.parent.getIndex();
				mat.mult4(ret.get(parentBoneInd), mat);
			}

			ret.add(mat);
		}
		return ret;
	}


}
