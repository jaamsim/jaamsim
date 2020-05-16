/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2012 Ausenco Engineering Canada Inc.
 * Copyright (C) 2020 JaamSim Software Inc.
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

	// 1) Keyword
	if (column == 0) {
		str = in.getKeyword();
	}

	// 2) Default value
	else if (column == 1) {
		if (in.getDefaultText() != null)
			str = EditBox.formatEditorText(in.getDefaultText());
		else {
			str = in.getDefaultString();
			if (str == null || str.isEmpty())
				str = EditBox.NONE;
		}
	}

	// 3) Present value
	else {
		str = in.getValueString();
		if (!in.isValid())
			str = EditBox.formatErrorText(str);
		if (in.isDefault() && in.isRequired())
			str = EditBox.REQD;
		if (in.isLocked())
			str = EditBox.formatLockedText(str);
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
