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

public class AjustToLastColumnTable extends JTable {

	public AjustToLastColumnTable(int column, int row) {
		super(column, row);
	}

	// Table is not editable
	@Override
	public boolean isCellEditable( int row, int column ) {
		return false;
	}

	@Override
	public void doLayout() {
		int lastColumnWidth = this.getColumnModel().getColumn(
				this.getColumnCount() - 1).getWidth();
		int delta = this.getSize().width;
		for(int i = 0; i < this.getColumnCount(); i++) {
			delta -= this.getColumnModel().getColumn(i).getWidth();
		}
		lastColumnWidth += delta;
		this.getColumnModel().getColumn(this.getColumnCount() - 1).setWidth(
				lastColumnWidth);
	}
}
