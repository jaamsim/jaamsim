/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2013 Ausenco Engineering Canada Inc.
 * Copyright (C) 2019-2024 JaamSim Software Inc.
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

import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;

import com.jaamsim.basicsim.Entity;
import com.jaamsim.basicsim.JaamSimModel;
import com.jaamsim.input.InputAgent;
import com.jaamsim.input.ValueHandle;
import com.jaamsim.units.DimensionlessUnit;
import com.jaamsim.units.Unit;

public class OutputBox extends FrameBox {
	private static OutputBox myInstance;
	private Entity currentEntity;
	OutputTableModel tableModel;
	OutputTable table;

	private final ArrayList<Object> entries = new ArrayList<>();

	public OutputBox() {
		super( "Output Viewer" );
		setDefaultCloseOperation(FrameBox.DISPOSE_ON_CLOSE);
		addWindowListener(FrameBox.getCloseListener("ShowOutputViewer"));

		tableModel = new OutputTableModel();
		table = new OutputTable(tableModel);
		JScrollPane scrollPane = new JScrollPane(table);

		getContentPane().add( scrollPane );

		addComponentListener(FrameBox.getSizePosAdapter(this, "OutputViewerSize", "OutputViewerPos"));

		// Copy
		table.getInputMap().put(KeyStroke.getKeyStroke("control C"), "copy");
		table.getActionMap().put("copy", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String str = table.getSelectedString();
				if (str == null)
					return;
				GUIFrame.copyToClipboard(str);
			}
		});

		// Find
		table.getInputMap().put(KeyStroke.getKeyStroke("control F"), "find");
		table.getActionMap().put("find", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String str = table.getSelectedString();
				if (str == null)
					return;
				FindBox.getInstance().search(str);
			}
		});

		// Context menu
		table.addMouseListener(new MyMouseListener());
	}

	/**
	 * Returns the only instance of the property box
	 */
	public synchronized static OutputBox getInstance() {
		if (myInstance == null)
			myInstance = new OutputBox();

		return myInstance;
	}

	class MyMouseListener implements MouseListener {
		private final JPopupMenu menu= new JPopupMenu();
		@Override
		public void mouseClicked(MouseEvent e) {}
		@Override
		public void mouseEntered(MouseEvent e) {}
		@Override
		public void mouseExited(MouseEvent e) {}
		@Override
		public void mousePressed(MouseEvent e) {}

		@Override
		public void mouseReleased(MouseEvent e) {
			// Show context menu for right-click
			if (e.getButton() != MouseEvent.BUTTON3 || currentEntity == null)
				return;
			String str = table.getSelectedString();
			if (str == null)
				return;
			menu.removeAll();

			// Copy
			JMenuItem copyMenuItem = new JMenuItem("Copy");
			copyMenuItem.setIcon( new ImageIcon(
					GUIFrame.class.getResource("/resources/images/Copy-16.png")) );
			copyMenuItem.setAccelerator(KeyStroke.getKeyStroke(
			        KeyEvent.VK_C, ActionEvent.CTRL_MASK));
			copyMenuItem.addActionListener( new ActionListener() {
				@Override
				public void actionPerformed( ActionEvent event ) {
					GUIFrame.copyToClipboard(str);
				}
			} );
			menu.add(copyMenuItem);

			// Find
			JMenuItem findMenuItem = new JMenuItem("Find");
			findMenuItem.setIcon( new ImageIcon(
					GUIFrame.class.getResource("/resources/images/Find-16.png")) );
			findMenuItem.setAccelerator(KeyStroke.getKeyStroke(
			        KeyEvent.VK_F, ActionEvent.CTRL_MASK));
			findMenuItem.addActionListener( new ActionListener() {
				@Override
				public void actionPerformed( ActionEvent event ) {
					FindBox.getInstance().search(str);
				}
			} );
			menu.add(findMenuItem);
			menu.show(e.getComponent(), e.getX(), e.getY());
		}
	}

	@Override
	public void setEntity( Entity entity ) {
		currentEntity = entity;
		if (currentEntity == null) {
			setTitle("Output Viewer");
			entries.clear();
			return;
		}
		setTitle("Output Viewer - " + currentEntity.getName());

		// Build up the row list, leaving extra rows for entity names
		Class<?> currClass = null;
		entries.clear();

		for (ValueHandle h : currentEntity.getAllOutputs()) {
			Class<?> klass = h.getDeclaringClass();
			if (currClass != klass) {
				// This is the first time we've seen this class, add a place holder row
				currClass = klass;
				entries.add(klass);
			}
			entries.add(h);
		}
	}

	@Override
	public void updateValues(double simTime) {
		if (tableModel == null) return;
		tableModel.simTime = simTime;
		tableModel.fireTableDataChanged();
	}

	private synchronized static void killInstance() {
		myInstance = null;
	}

	@Override
	public void dispose() {
		killInstance();
		super.dispose();
	}

	private class OutputTable extends JTable {
		public OutputTable(TableModel model) {
			super(model);

			setDefaultRenderer(Object.class, colRenderer);

			getColumnModel().getColumn(0).setWidth(150);
			getColumnModel().getColumn(1).setWidth(100);

			this.getTableHeader().setFont(FrameBox.boldFont);
			this.getTableHeader().setReorderingAllowed(false);
		}

		@Override
		public String getToolTipText(MouseEvent event) {
			Point p = event.getPoint();
			int row = rowAtPoint(p);
			int col = columnAtPoint(p);

			// Only the first column has tooltip
			if (col != 0)
				return "";

			if (currentEntity == null ||
			    row >= entries.size() ||
			    entries.get(row) instanceof Class) {
				return "";
			}

			ValueHandle output = (ValueHandle)entries.get(row);
			return GUIFrame.formatOutputToolTip(output.getName(), output.getDescription());
		}

		@Override
		public Point getToolTipLocation(MouseEvent e) {
			int row = rowAtPoint(e.getPoint());
			int y = getCellRect(row, 0, true).getLocation().y;
			return new Point(getColumnModel().getColumn(0).getWidth(), y);
		}

		@Override
		public void doLayout() {
			FrameBox.fitTableToLastColumn(this);
		}

		public String getSelectedString() {
			int row = getSelectedRow();
			if (row < 0)
				return null;
			Object obj = getValueAt(row, 1);
			if (!(obj instanceof String))
				return null;
			return (String) obj;
		}
	}

	private class OutputTableModel extends AbstractTableModel {
		double simTime = 0.0d;
		@Override
		public int getColumnCount() {
			return 2;
		}

		@Override
		public String getColumnName(int column) {
			switch (column) {
			case 0: return "Output";
			case 1: return "Value";
			}

			return "Unknown";
		}

		@Override
		public int getRowCount() {
			return entries.size();
		}

		@Override
		public Object getValueAt(int row, int col) {
			Object entry = entries.get(row);
			switch (col) {
			case 0:
				if (entry instanceof Class)
					return String.format("<HTML><B>%s</B></HTML>", ((Class<?>)entry).getSimpleName());
				return String.format("    %s", ((ValueHandle)entry).getName());
			case 1:
				if (entry instanceof Class)
					return "";
				try {
					// Determine the preferred unit
					JaamSimModel simModel = GUIFrame.getJaamSimModel();
					ValueHandle out = (ValueHandle)entry;
					Class<? extends Unit> ut = out.getUnitType();
					double factor = 1.0d;
					String unitString = simModel.getDisplayedUnit(ut);
					if (!unitString.isEmpty()) {
						factor = simModel.getDisplayedUnitFactor(ut);
					}

					// Select the appropriate format for numbers
					String fmt = "%g";
					if (out.isIntegerValue() && out.getUnitType() == DimensionlessUnit.class)
						fmt = "%.0f";

					// Evaluate the output
					return InputAgent.getValueAsString(simModel, out, simTime, fmt, factor, unitString);
				}
				catch (Throwable e) {
					return "Cannot evaluate";
				}
			default:
				assert false;
				return null;
			}
		}

		@Override
		public boolean isCellEditable(int rowIndex, int columnIndex) {
			return false;
		}
	}

}
