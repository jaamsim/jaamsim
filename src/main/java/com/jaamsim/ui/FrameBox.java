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
package com.jaamsim.ui;

import java.awt.Color;
import java.awt.Font;
import java.util.ArrayList;

import javax.swing.JFrame;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import com.jaamsim.controllers.RenderManager;
import com.sandwell.JavaSimulation.Entity;
import com.sandwell.JavaSimulation3D.GUIFrame;

public class FrameBox extends JFrame {

	private static final ArrayList<FrameBox> allInstances;

	private static final FrameBoxUpdater updater;
	private static final FrameBoxValueUpdater valueUpdater;

	protected static final Color TABLE_SELECT = new Color(255, 250, 180);

	protected static final Font boldFont;
	protected static final TableCellRenderer colRenderer;

	static {
		allInstances = new ArrayList<FrameBox>();

		boldFont = UIManager.getDefaults().getFont("TabbedPane.font").deriveFont(Font.BOLD);

		updater = new FrameBoxUpdater();
		valueUpdater = new FrameBoxValueUpdater();

		colRenderer = new DefaultCellRenderer();
	}

	public FrameBox(String title) {
		super(title);
		setIconImage(GUIFrame.getWindowIcon());
		allInstances.add(this);
	}

	public static ArrayList<FrameBox> getAllFB() {
		return allInstances;
	}

	@Override
	public void dispose() {
		allInstances.remove(this);
		super.dispose();
	}

	public static final void setSelectedEntity(Entity ent) {
		updater.scheduleUpdate(ent);
		RenderManager.setSelection(ent);
	}

	public static final void timeUpdate(double time) {
		valueUpdater.scheduleUpdate(time);
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

		@Override
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
		private double simTime;

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

		public void scheduleUpdate(double simTime) {
			synchronized (this) {
				if (!scheduled)
					SwingUtilities.invokeLater(this);

				scheduled = true;
				this.simTime = simTime;
			}
		}

		@Override
		public void run() {
			double callBackTime;
			synchronized (this) {
				scheduled = false;
				callBackTime = simTime;
			}

			GUIFrame.instance().setClock(callBackTime);
			RenderManager.updateTime(callBackTime);

			for (FrameBox each : allInstances) {
				each.updateValues();
			}
		}
	}

	public static void fitTableToLastColumn(JTable tab) {
		TableColumnModel model = tab.getColumnModel();
		TableColumn lastCol = model.getColumn(model.getColumnCount() - 1);

		int delta = tab.getSize().width;
		for(int i = 0; i < model.getColumnCount(); i++) {
			delta -= model.getColumn(i).getWidth();
		}
		int newWidth = lastCol.getWidth() + delta;
		lastCol.setWidth(newWidth);
	}
}
