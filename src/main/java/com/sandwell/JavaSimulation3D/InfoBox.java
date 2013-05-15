/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2002-2011 Ausenco Engineering Canada Inc.
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
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;

import com.jaamsim.ui.FrameBox;
import com.sandwell.JavaSimulation.Entity;
import com.sandwell.JavaSimulation.Vector;

/**
 * Class to display information about model objects. <br>
 * displays the results of getInfo for an object
 */
public class InfoBox extends FrameBox {
	private static InfoBox myInstance;  // only one instance allowed to be open

	private final InfoTable propTable;
	private final DefaultTableModel tabModel;

	private Entity currentEntity;

	public InfoBox() {
		super("Info Viewer");

		setDefaultCloseOperation(FrameBox.HIDE_ON_CLOSE);

		propTable = new InfoTable(0, 2);
		tabModel = (DefaultTableModel)propTable.getModel();

		JScrollPane scroller = new JScrollPane(propTable);
		getContentPane().add(scroller);

		// Set the location of the window
		pack();
		setLocation(750, 710);
		setSize(530, 290);
	}

	@Override
	public void setEntity(Entity entity) {
		if (currentEntity == entity)
			return;

		currentEntity = entity;

		if (currentEntity == null) {
			setTitle("Info Viewer");
			propTable.setVisible(false);
		}
		else {
			setTitle("Info Viewer - " + currentEntity.getInputName());
			propTable.setVisible(true);
		}
	}

	@Override
	public void updateValues(double simTime) {

		if (currentEntity == null || !this.isVisible())
			return;

		Vector info = null;
		try {
			info = currentEntity.getInfo();
		}
		catch (Throwable e) {
			tabModel.setRowCount(0);
			return;
		}

		tabModel.setRowCount(info.size());

		for (int i = 0; i < info.size(); i++) {
			String[] record = ((String)info.get(i)).split("\t", 2);
			propTable.setValueAt(record[0], i, 0);

			if (record.length >= 2)
				propTable.setValueAt(record[1], i, 1);
			else
				propTable.setValueAt("", i, 1);
		}
	}

	public synchronized static InfoBox getInstance() {
		if (myInstance == null)
			myInstance = new InfoBox();

		return myInstance;
	}

	private synchronized static void killInstance() {
		myInstance = null;
	}

	@Override
	public void dispose() {
		killInstance();
		super.dispose();
	}

private static class InfoTable extends JTable {
	public InfoTable(int column, int row) {
		super(column, row);

		this.getTableHeader().setFont(FrameBox.boldFont);
		this.getTableHeader().setReorderingAllowed(false);

		TableColumn col;

		col = this.getColumnModel().getColumn(0);
		col.setHeaderValue("Property");
		col.setCellRenderer(FrameBox.colRenderer);
		col.setWidth(250);

		col = this.getColumnModel().getColumn(1);
		col.setHeaderValue("Value");
		col.setCellRenderer(FrameBox.colRenderer);
		col.setWidth(280);
	}

	// Table is not editable
	@Override
	public boolean isCellEditable( int row, int column ) {
		return false;
	}

	@Override
	public void doLayout() {
		FrameBox.fitTableToLastColumn(this);
	}
}

}
