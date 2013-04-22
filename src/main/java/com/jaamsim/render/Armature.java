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
 */package com.jaamsim.render;

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

	public static class Action {
		public String name;
		public ArrayList<Channel> channels = new ArrayList<Channel>();
	}

	public static class Bone {
		private String name;
		private Mat4d mat;
		private Mat4d invMat;
		private Bone parent;
		private double length;
		private final ArrayList<Bone> children = new ArrayList<Bone>();
		public Bone(String name, Mat4d mat, Bone parent, double length) {
			this.name = name;
			this.mat = new Mat4d(mat);
			this.parent = parent;
			this.length = length;
			this.invMat = mat.inverse();
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
		Bone b = new Bone(boneName, matrix, parent, length);
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
}
