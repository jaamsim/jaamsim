/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2011 Ausenco Engineering Canada Inc.
 * Copyright (C) 2019 JaamSim Software Inc.
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
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Point;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;

import javax.swing.JFrame;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import com.jaamsim.Commands.KeywordCommand;
import com.jaamsim.basicsim.Entity;
import com.jaamsim.basicsim.JaamSimModel;
import com.jaamsim.basicsim.Simulation;
import com.jaamsim.controllers.RenderManager;
import com.jaamsim.datatypes.IntegerVector;
import com.jaamsim.input.InputAgent;
import com.jaamsim.input.KeywordIndex;

public class FrameBox extends OSFixJFrame {

	private static final ArrayList<FrameBox> allInstances;

	private static volatile Entity selectedEntity;

	protected static final Color TABLE_SELECT = new Color(255, 250, 180);

	public static final Font boldFont;
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

	public static void stop() {
		ArrayList<FrameBox> boxes = new ArrayList<>(allInstances);
		for (FrameBox each : boxes) {
			each.reset();
		}
	}

	public static void allowResizing(boolean bool) {
		for (FrameBox frame : allInstances) {
			frame.setResizable(bool);
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
			kw = InputAgent.formatBoolean(keyword, false);
		}

		@Override
		public void windowClosing(WindowEvent e) {
			JaamSimModel simModel = GUIFrame.getJaamSimModel();
			Simulation simulation = simModel.getSimulation();
			simModel.storeAndExecute(new KeywordCommand(simulation, kw));
		}
	}

	public static ComponentAdapter getSizePosAdapter(JFrame frame, String sizeKey, String posKey) {
		return new SizePosAdapter(frame, sizeKey, posKey);
	}

	/**
	 * Listens for re-size and re-position events and sets the appropriate keywords.
	 */
	private static class SizePosAdapter extends ComponentAdapter {
		final JFrame tool;
		final String sizeKeyword;
		final String posKeyword;

		public SizePosAdapter(JFrame frame, String sizeKey, String posKey) {
			tool = frame;
			sizeKeyword = sizeKey;
			posKeyword = posKey;
		}

		@Override
		public void componentMoved(ComponentEvent e) {
			if (GUIFrame.getJaamSimModel().getSimulation().isLockWindows()) {
				return;
			}
			setSizePos();
		}

		@Override
		public void componentResized(ComponentEvent e) {
			if (GUIFrame.getJaamSimModel().getSimulation().isLockWindows()) {
				return;
			}
			setSizePos();
		}

		private void setSizePos() {
			Dimension size = tool.getSize();
			Point pos = tool.getLocation();
			pos = GUIFrame.getInstance().getRelativeLocation(pos.x, pos.y);

			JaamSimModel simModel = GUIFrame.getJaamSimModel();
			Simulation simulation = simModel.getSimulation();
			IntegerVector oldSize = (IntegerVector) simulation.getInput(sizeKeyword).getValue();
			IntegerVector oldPos = (IntegerVector) simulation.getInput(posKeyword).getValue();
			if (oldSize.get(0) == size.width && oldSize.get(1) == size.height
					&& oldPos.get(0) == pos.x && oldPos.get(1) == pos.y)
				return;

			KeywordIndex sizeKw = InputAgent.formatIntegers(sizeKeyword, size.width, size.height);
			KeywordIndex posKw = InputAgent.formatIntegers(posKeyword, pos.x, pos.y);
			simModel.storeAndExecute(new KeywordCommand(simulation, sizeKw, posKw));
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

	public static final boolean isSelected(Entity ent) {
		return (ent == selectedEntity);
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
