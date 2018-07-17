/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2018 JaamSim Software Inc.
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

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.plaf.basic.BasicArrowButton;

import com.jaamsim.input.Input;
import com.jaamsim.ui.EditBox.EditTable;

public abstract class ChooserEditor extends CellEditor
implements ActionListener {

	private final JPanel jPanel;
	private final JTextField text;
	private final JButton button;

	public ChooserEditor(EditTable table, boolean showButton) {
		super(table);

		// Table cell
		jPanel = new JPanel(new BorderLayout());
		int height = table.getRowHeight();
		int width = table.getColumnModel().getColumn(EditBox.VALUE_COLUMN).getWidth() -
				table.getColumnModel().getColumnMargin();
		jPanel.setPreferredSize(new Dimension(width, height));

		// Editable text
		text = new JTextField();
		jPanel.add(text, BorderLayout.WEST);

		// Dropdown button
		int buttonWidth = 0;
		if (showButton) {
			button = new BasicArrowButton(BasicArrowButton.SOUTH,
					UIManager.getColor("ComboBox.buttonBackground"),  // FIXME does not respect look and feel
					UIManager.getColor("ComboBox.buttonBackground"),  // "ComboBox.buttonShadow"
					UIManager.getColor("ComboBox.buttonDarkShadow"),
					UIManager.getColor("ComboBox.buttonBackground")); // "ComboBox.buttonHighlight"
			button.addActionListener(this);
			button.setActionCommand("button");
			buttonWidth = button.getPreferredSize().width;
			jPanel.add(button, BorderLayout.EAST);
		}
		else {
			button = null;
		}

		text.setPreferredSize(new Dimension(width - buttonWidth, height));
	}

	public void setValue(String str) {
		text.setText(str);
	}

	@Override
	public String getValue() {
		return text.getText();
	}

	@Override
	public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {

		setTableInfo(table, row, column);

		// set the value
		input = (Input<?>)value;
		String val = input.getValueString();
		if (canRetry() && retryString != null) {
			val = retryString;
			retryString = null;
		}
		text.setText(val);

		return jPanel;
	}

}
