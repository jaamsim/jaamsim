/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2011 Ausenco Engineering Canada Inc.
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
package com.jaamsim.ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;

import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import com.jaamsim.basicsim.Entity;
import com.jaamsim.basicsim.Simulation;
import com.jaamsim.controllers.RenderManager;
import com.jaamsim.input.InputAgent;
import com.jaamsim.input.KeywordIndex;

public class FrameBox extends OSFixJFrame {

	private static final ArrayList<FrameBox> allInstances;

	private static volatile Entity selectedEntity;

	protected static final Color TABLE_SELECT = new Color(255, 250, 180);

	protected static final Font boldFont;
	protected static final TableCellRenderer colRenderer;

	static {
		allInstances = new ArrayList<>();

		boldFont = UIManager.getDefaults().getFont("TabbedPane.font").deriveFont(Font.BOLD);

		colRenderer = new DefaultCellRenderer();
	}

	public FrameBox(String title) {
		super(title);
		setType(Type.UTILITY);
		setAutoRequestFocus(false);
		allInstances.add(this);
	}

	public static void clear() {
		ArrayList<FrameBox> boxes = new ArrayList<>(allInstances);
		for (FrameBox each : boxes) {
			each.dispose();
		}
	}

	public static void stop() {
		ArrayList<FrameBox> boxes = new ArrayList<>(allInstances);
		for (FrameBox each : boxes) {
			each.reset();
		}
	}

	public void reset() {}

	public static WindowAdapter getCloseListener(String key) {
		return new CloseListener(key);
	}

	/**
	 * Listens for window events for the GUI and sets the appropriate keyword
	 * controlling visibility.
	 */
	private static class CloseListener extends WindowAdapter {
		final KeywordIndex kw;
		public CloseListener(String keyword) {
			ArrayList<String> arg = new ArrayList<>(1);
			arg.add("FALSE");
			kw = new KeywordIndex(keyword, arg, null);
		}

		@Override
		public void windowClosing(WindowEvent e) {
			InputAgent.apply(Simulation.getInstance(), kw);
		}
	}

	@Override
	public void dispose() {
		allInstances.remove(this);
		super.dispose();
	}

	/***
	 * Set the selected entity. This is also the mechanism the 'make link' feature uses to determine the next entity in the chain
	 * @param ent The entity to select
	 * @param canMakeLink if this selection can form a new link while 'make link' is active. This should be false in most cases.
	 */
	public static final void setSelectedEntity(Entity ent, boolean canMakeLink) {
		if (ent == selectedEntity)
			return;

		if (selectedEntity != null)
			selectedEntity.handleSelectionLost();

		selectedEntity = ent;
		RenderManager.setSelection(ent, canMakeLink);
		GUIFrame.setSelectedEntity(ent);
		GUIFrame.updateUI();
	}

	// This is equivalent to calling setSelectedEntity again with the same entity as used previously
	public static final void reSelectEntity() {
		GUIFrame.updateUI();
	}

	public void setEntity(Entity ent) {}
	public void updateValues(double simTime) {}

	static void updateEntityValues(double callBackTime) {
		for (int i = 0; i < allInstances.size(); i++) {
			try {
				FrameBox each = allInstances.get(i);
				each.setEntity(selectedEntity);
				each.updateValues(callBackTime);
			}
			catch (IndexOutOfBoundsException e) {
				// reschedule and try again
				GUIFrame.updateUI();
				return;
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
