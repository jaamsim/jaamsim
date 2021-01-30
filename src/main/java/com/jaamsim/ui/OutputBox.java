/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2013 Ausenco Engineering Canada Inc.
 * Copyright (C) 2019-2020 JaamSim Software Inc.
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
import java.awt.event.MouseEvent;
import java.util.ArrayList;

import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;

import com.jaamsim.basicsim.Entity;
import com.jaamsim.basicsim.JaamSimModel;
import com.jaamsim.input.Input;
import com.jaamsim.input.InputAgent;
import com.jaamsim.input.OutputHandle;
import com.jaamsim.input.ValueHandle;
import com.jaamsim.units.DimensionlessUnit;
import com.jaamsim.units.Unit;

public class OutputBox extends FrameBox {
	private static OutputBox myInstance;
	private Entity currentEntity;
	OutputTableModel tableModel;

	private final ArrayList<Object> entries = new ArrayList<>();

	public OutputBox() {
		super( "Output Viewer" );
		setDefaultCloseOperation(FrameBox.DISPOSE_ON_CLOSE);
		addWindowListener(FrameBox.getCloseListener("ShowOutputViewer"));

		tableModel = new OutputTableModel();
		OutputTable table = new OutputTable(tableModel);
		JScrollPane scrollPane = new JScrollPane(table);

		getContentPane().add( scrollPane );

		addComponentListener(FrameBox.getSizePosAdapter(this, "OutputViewerSize", "OutputViewerPos"));
	}

	/**
	 * Returns the only instance of the property box
	 */
	public synchronized static OutputBox getInstance() {
		if (myInstance == null)
			myInstance = new OutputBox();

		return myInstance;
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
				return null;

			if (currentEntity == null ||
			    row >= entries.size() ||
			    entries.get(row) instanceof Class) {
				return null;
			}

			OutputHandle output = (OutputHandle)entries.get(row);
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
				return String.format("    %s", ((OutputHandle)entry).getName());
			case 1:
				if (entry instanceof Class)
					return "";
				try {
					// Determine the preferred unit
					JaamSimModel simModel = GUIFrame.getJaamSimModel();
					OutputHandle out = (OutputHandle)entry;
					Class<? extends Unit> ut = out.getUnitType();
					double factor = simModel.getDisplayedUnitFactor(ut);

					// Select the appropriate format
					String fmt = "%s";
					if (out.isNumericValue()) {
						if (out.isIntegerValue() && out.getUnitType() == DimensionlessUnit.class) {
							fmt = "%.0f";
						}
						else {
							fmt = "%g";
						}
					}

					// Evaluate the output
					StringBuilder sb = new StringBuilder();
					sb.append(InputAgent.getValueAsString(simModel, out, simTime, fmt, factor));

					// Append the appropriate unit
					if (ut != Unit.class && ut != DimensionlessUnit.class) {
						String unitString = simModel.getDisplayedUnit(ut);
						sb.append(Input.SEPARATOR).append(unitString);
					}

					return sb.toString();
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
