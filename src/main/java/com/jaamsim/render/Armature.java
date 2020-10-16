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
		private final ArrayList<Bone> children = new ArrayList<>();
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

	private final ArrayList<Bone> bones = new ArrayList<>();
	private final ArrayList<Action> actions = new ArrayList<>();

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
		ArrayList<Bone> ret = new ArrayList<>();
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
	 */
	public ArrayList<Mat4d> getPose(ArrayList<Action.Queue> actions) {
		if (actions == null) {
			actions = new ArrayList<>();
		}

		ArrayList<Mat4d> poseTransforms = new ArrayList<>(bones.size());
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

		ArrayList<Mat4d> ret = new ArrayList<>(bones.size());

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
