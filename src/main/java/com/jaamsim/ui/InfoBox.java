/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2002-2013 Ausenco Engineering Canada Inc.
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

import java.util.ArrayList;

import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;

import com.sandwell.JavaSimulation.Entity;
import com.sandwell.JavaSimulation.Vector;

/**
 * Class to display information about model objects. <br>
 * displays the results of getInfo for an object
 */
public class InfoBox extends FrameBox {
	private static InfoBox myInstance;  // only one instance allowed to be open
	private Entity currentEntity;
	private final InfoTableModel tableModel;


	public InfoBox() {
		super("Info Viewer");

		setDefaultCloseOperation(FrameBox.DISPOSE_ON_CLOSE);
		tableModel = new InfoTableModel();
		InfoTable propTable = new InfoTable(tableModel);
		JScrollPane scroller = new JScrollPane(propTable);
		getContentPane().add(scroller);

		// Set the location of the window
		pack();
		setLocation(750, 710);
		setSize(530, 290);
	}

	public synchronized static InfoBox getInstance() {
		if (myInstance == null)
			myInstance = new InfoBox();

		return myInstance;
	}

	@Override
	public void setEntity(Entity entity) {
		if (currentEntity == entity)
			return;

		currentEntity = entity;

		if (currentEntity == null) {
			setTitle("Info Viewer");
			tableModel.setInfos(null);
			tableModel.fireTableDataChanged();
		}
		else {
			setTitle("Info Viewer - " + currentEntity.getInputName());
		}
	}

	@Override
	public void updateValues(double simTime) {
		if (currentEntity == null)
			return;

		Vector info = null;
		try {
			info = currentEntity.getInfo();
		}
		catch (Throwable e) {
			return;
		}

		ArrayList<Info> tmp = new ArrayList<Info>(info.size());
		for (int i = 0; i < info.size(); i++) {
			String[] record = ((String)info.get(i)).split("\t", 2);

			Info inf = new Info();
			inf.name = record[0];

			if (record.length > 1) {
				inf.value = record[1];
			}
			else {
				inf.value = "";
			}
			tmp.add(inf);
		}

		tableModel.setInfos(tmp);
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

private static class Info {
	String name;
	String value;
}

private static class InfoTable extends JTable {
	public InfoTable(InfoTableModel mod) {
		super(mod);

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
private static class InfoTableModel extends AbstractTableModel {
	ArrayList<Info> infos;

	InfoTableModel() {
		infos = null;
	}

	void setInfos(ArrayList<Info> list) {
		infos = list;
	}

	@Override
	public int getColumnCount() {
		return 2;
	}

	@Override
	public int getRowCount() {
		if (infos == null)
			return 0;

		return infos.size();
	}

	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex) {
		return false;
	}

	@Override
	public Object getValueAt(int row, int col) {
		if (infos == null)
			return "";

		switch (col) {
			case 0: return infos.get(row).name;
			case 1: return infos.get(row).value;
		}
		return "";
	}
}

}
