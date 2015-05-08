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
		String desc = new String(output.getDescription());
		desc = desc.replaceAll("&", "&amp;");
		desc = desc.replaceAll("<", "&lt;");
		desc = desc.replaceAll(">", "&gt;");
		desc = desc.replaceAll("\n", "<BR>");
		return GUIFrame.formatOutputToolTip(output.getName(), desc);
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
				OutputHandle o = (OutputHandle)entry;
				if (o.isNumericValue()) {
					double d = o.getValueAsDouble(simTime, Double.NaN);
					Class<? extends Unit> ut = o.getUnitType();
					if (ut == Unit.class || ut == DimensionlessUnit.class) {
						return String.format("%g", d);
					}
					else {
						return String.format("%g  %s",
								d/Unit.getDisplayedUnitFactor(ut), Unit.getDisplayedUnit(ut));
					}
				}

				String s = o.getValue(simTime, o.getReturnType()).toString();
				if (o.getUnitType() == Unit.class )
					return s;
				else
					return s + "  " + Unit.getSIUnit(o.getUnitType());
			}
			catch (Throwable e) {
				return "";
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
