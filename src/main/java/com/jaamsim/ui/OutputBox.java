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
package com.jaamsim.ui;

import java.awt.Point;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map.Entry;

import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;

import com.jaamsim.basicsim.Entity;
import com.jaamsim.datatypes.DoubleVector;
import com.jaamsim.input.Input;
import com.jaamsim.input.OutputHandle;
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

		setLocation(GUIFrame.COL3_START, GUIFrame.LOWER_START);
		setSize(GUIFrame.COL3_WIDTH, GUIFrame.LOWER_HEIGHT);
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

		ArrayList<OutputHandle> handles = OutputHandle.getOutputHandleList(currentEntity);
		for (OutputHandle h : handles) {
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
			if (currentEntity == null ||
			    row >= entries.size() ||
			    entries.get(row) instanceof Class) {
				return null;
			}

			OutputHandle output = (OutputHandle)entries.get(row);
			return GUIFrame.formatOutputToolTip(output.getName(), output.getDescription());
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
					StringBuilder sb = new StringBuilder();
					String str;

					// Determine the preferred unit
					OutputHandle out = (OutputHandle)entry;
					Class<? extends Unit> ut = out.getUnitType();
					double factor = Unit.getDisplayedUnitFactor(ut);
					String unitString = Unit.getDisplayedUnit(ut);

					// Numeric outputs
					if (out.isNumericValue()) {
						double val = out.getValueAsDouble(simTime, Double.NaN);
						if (out.getReturnType() == int.class || out.getReturnType() == long.class || out.getReturnType() == Integer.class) {
							str = String.format("%.0f", val/factor);
						}
						else {
							str = String.format("%g", val/factor);
						}
						sb.append(str);
					}

					// DoubleVector output
					else if (out.getReturnType() == DoubleVector.class) {
						sb.append("{");
						DoubleVector vec = out.getValue(simTime, DoubleVector.class);
						for (int i=0; i<vec.size(); i++) {
							str = String.format("%g, ", vec.get(i)/factor);
							sb.append(str);
						}
						if (sb.length() > 1)
							sb.replace(sb.length()-2, sb.length()-1, "}");
						else
							sb.append("}");
					}

					// ArrayList output
					else if (out.getReturnType() == ArrayList.class) {
						sb.append("{");
						ArrayList<?> array = out.getValue(simTime, ArrayList.class);
						for (int i=0; i<array.size(); i++) {
							Object obj = array.get(i);
							if (obj instanceof Double) {
								double val = (Double)obj;
								str = String.format("%g, ", val/factor);
							}
							else {
								str = String.format("%s, ", obj);
							}
							sb.append(str);
						}
						if (sb.length() > 1)
							sb.replace(sb.length()-2, sb.length()-1, "}");
						else
							sb.append("}");
					}

					// Keyed outputs
					else if (out.getReturnType() == LinkedHashMap.class) {
						sb.append("{");
						LinkedHashMap<?, ?> map = out.getValue(simTime, LinkedHashMap.class);
						for (Entry<?, ?> mapEntry : map.entrySet()) {
							Object obj = mapEntry.getValue();
							if (obj instanceof Double) {
								double val = (Double)obj;
								str = String.format("%s=%g, ", mapEntry.getKey(), val/factor);
							}
							else {
								str = String.format("%s=%s, ", mapEntry.getKey(), obj);
							}
							sb.append(str);
						}
						if (sb.length() > 1)
							sb.replace(sb.length()-2, sb.length()-1, "}");
						else
							sb.append("}");
					}

					// All other outputs
					else {
						if (out.getValue(simTime, out.getReturnType()) == null)
							return "null";
						str = out.getValue(simTime, out.getReturnType()).toString();
						sb.append(str);
						unitString = Unit.getSIUnit(ut);  // other outputs are not converted to preferred units
					}

					// Append the appropriate unit
					if (ut != Unit.class && ut != DimensionlessUnit.class)
						sb.append(Input.SEPARATOR).append(unitString);

					return sb.toString();
				}
				catch (Throwable e) {
					LogBox.logException(e);
					return "Cannot evaluate - see Log Viewer for details";
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
