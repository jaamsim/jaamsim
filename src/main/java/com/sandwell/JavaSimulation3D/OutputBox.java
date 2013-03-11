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
package com.sandwell.JavaSimulation3D;

import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;

import com.jaamsim.ui.FrameBox;
import com.sandwell.JavaSimulation.Entity;

public class OutputBox extends FrameBox {

	private static OutputBox myInstance = new OutputBox();

	private JScrollPane scrollPane;
	private JTable table;

	private Entity currentEntity;
	private String[] outputNames;
	OutputTableModel tableModel;

	public OutputBox() {
		super( "Output Viewer" );
		setDefaultCloseOperation(FrameBox.HIDE_ON_CLOSE);

		tableModel = new OutputTableModel();
		table = new JTable(tableModel);
		scrollPane = new JScrollPane(table);

		getContentPane().add( scrollPane );
		setSize( 300, 150 );
		setLocation(0, 110);

		outputNames = new String[0];

		pack();
	}

	@Override
	public void setEntity( Entity entity ) {
		currentEntity = entity;
		if (currentEntity != null) {
			outputNames = currentEntity.getOutputNames(false);
		} else {
			outputNames = new String[0];

		}

		updateValues();
	}

	@Override
	public void updateValues() {
		tableModel.fireTableDataChanged();
	}

	private class OutputTableModel extends AbstractTableModel {

		@Override
		public int getColumnCount() {
			return 2;
		}

		@Override
		public String getColumnName(int col) {
			switch (col) {
			case 0:
				return "Name";
			case 1:
				return "Value";
			default:
				assert false;
				return null;
			}
		}

		@Override
		public int getRowCount() {
			return outputNames.length;
		}

		@Override
		public Object getValueAt(int row, int col) {

			switch (col) {
			case 0:
				return outputNames[row];
			case 1:
				return currentEntity.getOutputAsString(outputNames[row], 0);
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

	/**
	 * Returns the only instance of the property box
	 */
	public synchronized static OutputBox getInstance() {
		if (myInstance == null)
			myInstance = new OutputBox();

		return myInstance;
	}

	@Override
	public void dispose() {
		myInstance = null;
		super.dispose();
	}

}
