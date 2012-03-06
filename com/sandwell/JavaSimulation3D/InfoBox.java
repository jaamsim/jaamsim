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

import com.sandwell.JavaSimulation.Entity;
import com.sandwell.JavaSimulation.Vector;

import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.*;

/**
 * Class to display information about model objects. <br>
 * displays the results of getInfo for an object
 */
public class InfoBox extends FrameBox {
	private static InfoBox myInstance;  // only one instance allowed to be open

	private Entity currentEntity;
	private JTable propTable;     // The table
	private JScrollPane scroller;

	public InfoBox() {
		super( "Output Viewer" );

		setDefaultCloseOperation( FrameBox.HIDE_ON_CLOSE );
		scroller = new JScrollPane();

		// Create a table
		propTable = new AjustToLastColumnTable(0, 2);

		scroller = new JScrollPane(propTable);
		getContentPane().add(scroller);

		propTable.getColumnModel().getColumn( 0 ).setHeaderValue( "<html><b style=\"align=center;font-family:verdana;\">Property</b><html>" );
		propTable.getColumnModel().getColumn( 1 ).setHeaderValue( "<html><b style=\"align=center;font-family:verdana;\">Value</b><html>" );
		propTable.getTableHeader().setReorderingAllowed(false);
		propTable.getColumnModel().getColumn( 0 ).setWidth(250);
		propTable.getColumnModel().getColumn( 1 ).setWidth(280);

		// Set the location of the window
		pack();
		setLocation(750, 710);
		setSize(530, 290);
	}

	public void setEntity(Entity entity) {
		if(currentEntity == entity || ! this.isVisible())
			return;

		currentEntity = entity;

		if(currentEntity == null) {
			propTable.setVisible(false);
			setTitle("Output Viewer");
			return;
		}

		setTitle(String.format("Output Viewer - %s", currentEntity.getName()));
		updateValues();
		propTable.setVisible(true);
	}

	public void updateValues() {

		if(currentEntity == null || ! this.isVisible())
			return;

		Vector info = currentEntity.getInfo();

		DefaultTableModel tableModel =(DefaultTableModel) propTable.getModel();
		tableModel.setRowCount(info.size());

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

	public void dispose() {
		myInstance = null;
		super.dispose();
	}
}