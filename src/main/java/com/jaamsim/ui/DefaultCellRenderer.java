/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2012 Ausenco Engineering Canada Inc.
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

import java.awt.Component;

import javax.swing.BorderFactory;
import javax.swing.JTable;
import javax.swing.border.Border;
import javax.swing.table.DefaultTableCellRenderer;

class DefaultCellRenderer extends DefaultTableCellRenderer {

private final Border border = BorderFactory.createEmptyBorder(0, 10, 0, 0);

public DefaultCellRenderer() {}

@Override
public Component getTableCellRendererComponent(JTable table, Object value,
                                               boolean isSelected, boolean hasFocus,
                                               int row, int column) {
	Component cell = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

	if (row == table.getSelectedRow())
		cell.setBackground(FrameBox.TABLE_SELECT);
	else
		cell.setBackground(null);

	cell.setForeground(null);
	this.setBorder(border);
	return cell;
}
}
