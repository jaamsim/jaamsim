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

import com.sandwell.JavaSimulation.Entity;
import com.sandwell.JavaSimulation3D.GUIFrame;

public class OutputBox extends FrameBox {
	private static OutputBox myInstance;
	private Entity currentEntity;
	OutputTableModel tableModel;

	private ArrayList<String> outputNames = new ArrayList<String>();
	private ArrayList<Boolean> rowIsClass = new ArrayList<Boolean>();

	public OutputBox() {
		super( "Output Viewer" );
		setDefaultCloseOperation(FrameBox.DISPOSE_ON_CLOSE);

		tableModel = new OutputTableModel();
		OutputTable table = new OutputTable(tableModel);
		JScrollPane scrollPane = new JScrollPane(table);

		getContentPane().add( scrollPane );

		setEntity(null);

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
			outputNames.clear();
			rowIsClass.clear();
			return;
		}
		setTitle("Output Viewer - " + currentEntity.getInputName());

		// Build up the row list, leaving extra rows for entity names
		String[] tempNames = currentEntity.getSortedOutputNames();
		Class<?> currClass = null;

		outputNames = new ArrayList<String>();
		rowIsClass = new ArrayList<Boolean>();

		for (String name : tempNames) {
			Class<?> klass = currentEntity.getOutputDeclaringClass(name);
			if (currClass != klass) {
				// This is the first time we've seen this class, add a place holder row
				currClass = klass;
				outputNames.add(klass.getSimpleName());
				rowIsClass.add(true);
			}
			outputNames.add(name);
			rowIsClass.add(false);
		}
	}

	@Override
	public void updateValues(double simTime) {
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
		if (row >= outputNames.size() || currentEntity == null || rowIsClass.get(row)) {
			return null;
		}

		String outputName = outputNames.get(row);

		StringBuilder build = new StringBuilder();
		build.append("<HTML>");
		build.append("<b>Name:</b>  ");
		build.append(outputName);
		build.append("<BR>");
		String desc = currentEntity.getOutputDescripion(outputName);
		if (!desc.isEmpty()) {
			build.append("<BR>");
			build.append("<b>Description:</b> ");
			for (String line : desc.split("\n", 0)) {
				// Replace all <> for html parsing
				String tempLine = line.replaceAll("&", "&amp;");
				tempLine = tempLine.replaceAll("<", "&lt;");
				tempLine = tempLine.replaceAll(">", "&gt;");

				int len = 0;
				build.append("<BR>");
				// Break the line at 100-char boundaries
				for (String word : tempLine.split(" ", -1)) {
					build.append(word).append(" ");
					len += word.length() + 1;
					if (len > 100) {
						build.append("<BR>");
						len = 0;
					}
				}
			}
		}
		return build.toString();
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
		return outputNames.size();
	}

	@Override
	public Object getValueAt(int row, int col) {

		switch (col) {
		case 0:
			if (rowIsClass.get(row)) {
				return String.format("<HTML><B>%s</B></HTML>", outputNames.get(row));
			}
			return String.format("    %s", outputNames.get(row));
		case 1:
			if (rowIsClass.get(row)) {
				return "";
			}
			return currentEntity.getOutputAsString(outputNames.get(row), simTime);
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
