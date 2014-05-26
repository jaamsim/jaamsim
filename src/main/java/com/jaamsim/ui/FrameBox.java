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
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;

import javax.swing.JFrame;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import com.jaamsim.controllers.RenderManager;
import com.jaamsim.input.InputAgent;
import com.sandwell.JavaSimulation.Entity;
import com.sandwell.JavaSimulation.Simulation;
import com.sandwell.JavaSimulation3D.GUIFrame;

public class FrameBox extends JFrame {

	private static final ArrayList<FrameBox> allInstances;

	private static final FrameBoxUpdater updater;
	private static final FrameBoxValueUpdater valueUpdater;

	protected static final Color TABLE_SELECT = new Color(255, 250, 180);

	protected static final Font boldFont;
	protected static final TableCellRenderer colRenderer;
	private static double secondsPerTick;

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

	public static void clear() {
		ArrayList<FrameBox> boxes = new ArrayList<FrameBox>(allInstances);
		for (FrameBox each : boxes) {
			each.dispose();
		}
	}

	public static void setSecondsPerTick(double secsPerTick) {
		secondsPerTick = secsPerTick;
	}

	/**
	 * Return the number of seconds represented by the given number of ticks.
	 */
	public static final double ticksToSeconds(long ticks) {
		return ticks * secondsPerTick;
	}

	public static WindowAdapter getCloseListener(String key) {
		return new CloseListener(key);
	}

	/**
	 * Listens for window events for the GUI and sets the appropriate keyword
	 * controlling visibility.
	 */
	private static class CloseListener extends WindowAdapter {
		final String keyword;
		public CloseListener(String keyword) {
			this.keyword = keyword;
		}

		@Override
		public void windowClosing(WindowEvent e) {
			InputAgent.processEntity_Keyword_Value(Simulation.getInstance(), keyword, "FALSE");
		}
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

	// This is equivalent to calling setSelectedEntity again with the same entity as used previously
	public static final void reSelectEntity() {
		updater.rescheduleUpdate();
	}

	public static final void timeUpdate(long tick) {
		valueUpdater.scheduleUpdate(tick);
		RenderManager.updateTime(tick);
	}

	public static final void valueUpdate() {
		valueUpdater.scheduleUpdate();
		RenderManager.redraw();
	}

	public void setEntity(Entity ent) {}
	public void updateValues(double simTime) {}

	private static class FrameBoxUpdater implements Runnable {
		private boolean scheduled;
		private Entity entity;

		FrameBoxUpdater() {
			scheduled = false;
		}

		public void rescheduleUpdate() {
			synchronized (this) {
				schedUpdate();
			}
		}

		public void scheduleUpdate(Entity ent) {
			synchronized (this) {
				entity = ent;
				schedUpdate();
			}
		}

		/**
		 * Must be called inside a synchronized block to protect the reference
		 * to scheduled.
		 */
		private void schedUpdate() {
			if (!scheduled)
				SwingUtilities.invokeLater(this);

			scheduled = true;
		}

		@Override
		public void run() {
			Entity selectedEnt;
			synchronized (this) {
				selectedEnt = entity;
				scheduled = false;
			}

			for (int i = 0; i < allInstances.size(); i++) {
				try {
					FrameBox each = allInstances.get(i);
					each.setEntity(selectedEnt);
				}
				catch (IndexOutOfBoundsException e) {
					// reschedule and try again
					this.scheduleUpdate(selectedEnt);
					return;
				}
			}
			FrameBox.valueUpdate();
		}
	}

	private static class FrameBoxValueUpdater implements Runnable {
		private boolean scheduled;
		private long simTick;

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

		public void scheduleUpdate(long simTick) {
			synchronized (this) {
				if (!scheduled)
					SwingUtilities.invokeLater(this);

				scheduled = true;
				this.simTick = simTick;
			}
		}

		@Override
		public void run() {
			double callBackTime;
			synchronized (this) {
				scheduled = false;
				callBackTime = FrameBox.ticksToSeconds(simTick);
			}

			GUIFrame.instance().setClock(callBackTime);
			for (int i = 0; i < allInstances.size(); i++) {
				try {
					FrameBox each = allInstances.get(i);
					each.updateValues(callBackTime);
				}
				catch (IndexOutOfBoundsException e) {
					// reschedule and try again
					this.scheduleUpdate();
					return;
				}
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
