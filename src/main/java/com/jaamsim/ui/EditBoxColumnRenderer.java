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
package com.jaamsim.ui;

import java.awt.Color;
import java.awt.Component;

import javax.swing.BorderFactory;
import javax.swing.JTable;
import javax.swing.border.Border;
import javax.swing.table.DefaultTableCellRenderer;

import com.jaamsim.input.Input;

public class EditBoxColumnRenderer extends DefaultTableCellRenderer {

private final Border border;
private final Border focusBorder;

public EditBoxColumnRenderer() {
	border = BorderFactory.createEmptyBorder(0, 10, 0, 0);

	// single pixel outline for the focused cell, require an inset with one fewer
	// pixel due to the outline
	Border lineBorder = BorderFactory.createLineBorder(Color.BLUE);
	Border insetBorder = BorderFactory.createEmptyBorder(0, 9, 0, 0);
	focusBorder = BorderFactory.createCompoundBorder(lineBorder, insetBorder);
}

@Override
public Component getTableCellRendererComponent(JTable table, Object value,
                                               boolean isSelected, boolean hasFocus,
                                               int row, int column) {

	Input<?> in = (Input<?>)value;

	String str;
	if (column == 0)
		str = in.getKeyword();
	else if (column == 1){
		str = in.getDefaultString();
	}
	else {
		str = in.getValueString();
	}

	// Pass along the keyword string, not the input itself
	Component cell = super.getTableCellRendererComponent(table, str, isSelected, hasFocus, row, column);

	if (row == table.getSelectedRow())
		cell.setBackground(FrameBox.TABLE_SELECT);
	else
		cell.setBackground(null);

	if (hasFocus)
		this.setBorder(focusBorder);
	else
		this.setBorder(border);
	return cell;
}
}
