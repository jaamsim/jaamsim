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

 /**
  * A basic data holder for armature information
  * @author matt.chudleigh
  *
  */
public class Armature {

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
	private final ArrayList<Action> actions = new ArrayList<Action>();

	public void addAction(Action act) {
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

	public ArrayList<Action> getActions() {
		return actions;
	}

	private Action getActionByName(String name) {
		for (Action a : actions) {
			if (a.name.equals(name))
				return a;
		}
		return null;
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
			Action a = getActionByName(aq.name);
			if (a == null) {
				continue;
			}

			for (Action.Channel ch : a.channels) {
				int boneInd = getBoneIndex(ch.name);
				assert(boneInd != -1);
				Mat4d mat = Action.getChannelMatAtTime(ch, aq.time);
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
