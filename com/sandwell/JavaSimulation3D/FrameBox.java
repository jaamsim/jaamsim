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

import java.awt.Color;
import java.util.ArrayList;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import com.sandwell.JavaSimulation.Entity;

public class FrameBox extends JFrame {

	private static final ArrayList<FrameBox> allInstances;

	private static final FrameBoxUpdater updater;
	private static final FrameBoxValueUpdater valueUpdater;

	protected static final Color HEADER_COLOR = new Color(207, 200, 247);
	protected static final Color INACTIVE_TAB_COLOR = new Color(234, 234, 234);
	protected static final Color ACTIVE_TAB_COLOR = new Color(207, 200, 247);

	static {
		allInstances = new ArrayList<FrameBox>();
		updater = new FrameBoxUpdater();
		valueUpdater = new FrameBoxValueUpdater();
	}

	public FrameBox(String title) {
		super(title);
		setIconImage(GUIFrame.getWindowIcon());
		allInstances.add(this);
	}

	static ArrayList<FrameBox> getAll() {
		return allInstances;
	}

	public void dispose() {
		allInstances.remove(this);
		super.dispose();
	}

	public static final void setSelectedEntity(Entity ent) {
		updater.scheduleUpdate(ent);
		OrbitBehavior.selectEntity(ent);
	}

	public static final void valueUpdate() {
		valueUpdater.scheduleUpdate();
	}

	public void setEntity(Entity ent) {}
	public void updateValues() {}

	private static class FrameBoxUpdater implements Runnable {
		private boolean scheduled;
		private Entity entity;

		FrameBoxUpdater() {
			scheduled = false;
		}

		public void scheduleUpdate(Entity ent) {
			synchronized (this) {
				entity = ent;
				if (!scheduled)
					SwingUtilities.invokeLater(this);

				scheduled = true;
			}
		}

		public void run() {
			Entity selectedEnt;
			synchronized (this) {
				selectedEnt = entity;
				entity = null;
				scheduled = false;
			}

			for (FrameBox each : allInstances) {
				each.setEntity(selectedEnt);
			}
		}
	}

	private static class FrameBoxValueUpdater implements Runnable {
		private boolean scheduled;

		FrameBoxValueUpdater() {
			scheduled = false;
		}

		public void scheduleUpdate() {
			synchronized (this) {
				if (!scheduled)
					SwingUtilities.invokeLater(this);

				scheduled = true;
			}
		}

		public void run() {
			synchronized (this) {
				scheduled = false;
			}

			for (FrameBox each : allInstances) {
				each.updateValues();
			}

			for (int i = 0; i < Display2DEntity.getAll().size(); i++) {
				try {
					Display2DEntity.getAll().get(i).render(GraphicsUpdateBehavior.simTime);
				}
				// Catch everything so we don't screw up the behavior handling
				catch (Throwable e) {}
			}

		}
	}
}
