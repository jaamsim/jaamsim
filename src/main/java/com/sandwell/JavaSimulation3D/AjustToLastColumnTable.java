/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2012 Ausenco Engineering Canada Inc.
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

import javax.swing.JTable;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;


public class AjustToLastColumnTable extends JTable {

	public AjustToLastColumnTable(int column, int row) {
		super(column, row);
	}

	public AjustToLastColumnTable(TableModel tm) {
		super(tm);
	}

	// Table is not editable
	@Override
	public boolean isCellEditable( int row, int column ) {
		return false;
	}

	@Override
	public void doLayout() {
		TableColumnModel model = this.getColumnModel();
		TableColumn lastCol = model.getColumn(model.getColumnCount() - 1);

		int delta = this.getSize().width;
		for(int i = 0; i < model.getColumnCount(); i++) {
			delta -= model.getColumn(i).getWidth();
		}
		int newWidth = lastCol.getWidth() + delta;
		lastCol.setWidth(newWidth);
	}
}
