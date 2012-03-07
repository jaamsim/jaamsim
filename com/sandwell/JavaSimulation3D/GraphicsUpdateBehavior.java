/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2011 Ausenco Engineering Canada Inc.
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
package com.sandwell.JavaSimulation3D;

import java.util.ArrayList;
import java.util.Enumeration;

import javax.media.j3d.Behavior;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.WakeupOnElapsedFrames;
import javax.media.j3d.WakeupOnElapsedTime;

class GraphicsUpdateBehavior extends Behavior {
	private static double lastRenderTime = -1.0d;

	static double simTime = 0.0d;
	static boolean forceUpdate = false;

	private static boolean inCallback = false;
	private static final ArrayList<BranchGroup> bgList = new ArrayList<BranchGroup>();
	private static final ArrayList<AttachGroup> atList = new ArrayList<GraphicsUpdateBehavior.AttachGroup>();

	private final WakeupOnElapsedFrames frameCondition;
	private final WakeupOnElapsedTime timeCondition;

	public GraphicsUpdateBehavior() {
		timeCondition = new WakeupOnElapsedTime(50);
		frameCondition = new WakeupOnElapsedFrames(0);
	}

	public void initialize() {
		this.wakeupOn(frameCondition);
	}

	static void detachBG(BranchGroup bg) {
		synchronized (bgList) {
			if (!inCallback) {
				bg.detach();
			}
			else {
				bgList.add(bg);
				forceUpdate = true;
			}
		}
	}

	private static class AttachGroup {
		BranchGroup parent;
		BranchGroup child;

		public AttachGroup(BranchGroup pnt, BranchGroup cld) {
			parent = pnt;
			child = cld;
		}
	}

	static void attachBG(BranchGroup parent, BranchGroup child) {
		synchronized (bgList) {
			if (!inCallback) {
				parent.addChild(child);
			}
			else {
				atList.add(new AttachGroup(parent, child));
				forceUpdate = true;
			}
		}
	}

	public void processStimulus(Enumeration criteria) {
		// If we render again at the same model time as the previous render, wait
		// 50ms before trying again
		double timeAtCallback = GraphicsUpdateBehavior.simTime;
		if (lastRenderTime == timeAtCallback &&
		    !forceUpdate) {
			this.wakeupOn(timeCondition);
			return;
		}
		lastRenderTime = timeAtCallback;
		forceUpdate = false;

		synchronized (bgList) {
			inCallback = true;
		}

		for (int i = 0; i < DisplayEntity.getAll().size(); i++) {
			try {
				DisplayEntity.getAll().get(i).render(timeAtCallback);
			}
			// Catch everything so we don't screw up the behavior handling
			catch (Throwable e) {
				//e.printStackTrace();
			}
		}

		synchronized (bgList) {
			for (BranchGroup each : bgList)
				each.detach();
			bgList.clear();

			for (AttachGroup each : atList)
				each.parent.addChild(each.child);
			atList.clear();

			inCallback = false;
		}
		this.wakeupOn(frameCondition);
	}
}
