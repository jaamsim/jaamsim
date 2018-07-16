/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2005-2013 Ausenco Engineering Canada Inc.
 * Copyright (C) 2016-2018 JaamSim Software Inc.
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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JTable;

import com.jaamsim.input.Input;
import com.jaamsim.ui.EditBox.EditTable;

/**
 * Handles inputs with drop-down menus.
 *
 */
public class DropDownMenuEditor extends CellEditor
implements ActionListener {

	private final JComboBox<String> dropDown;

	private boolean retrying;

	public DropDownMenuEditor(EditTable table, ArrayList<String> aList) {
		super(table);

		dropDown = new JComboBox<>();
		dropDown.setEditable(true);
		DefaultComboBoxModel<String> model = (DefaultComboBoxModel<String>) dropDown.getModel();

		// Populate the items in the combo box
		for(String each: aList) {

			// Space inside the font name
			if( each.contains(" ") )
				each = String.format("'%s'", each);
			model.addElement(each);
		}

		dropDown.setActionCommand("comboBoxChanged");
		dropDown.addActionListener(this); // Now it is safe to listen
	}

	@Override
	public void actionPerformed(ActionEvent e) {

		// If combo box is edited, this method invokes twice
		if(e.getActionCommand().equals("comboBoxChanged") && !retrying)
			stopCellEditing();
	}

	@Override
	public Component getTableCellEditorComponent(JTable table,
			Object value, boolean isSelected, int row, int column) {

		setTableInfo(table, row, column);

		input = (Input<?>)value;
		String text = input.getValueString();

		if (retryString != null) {
			text = retryString;
			retrying = true;
		}
		retryString = null;

		dropDown.setSelectedItem(text);
		retrying = false;

		return dropDown;
	}

	@Override
	public String getValue() {
		// dropDown.getSelectedItem() returns blank with a typed entry until Enter is pressed
		return dropDown.getEditor().getItem().toString();
	}

	@Override
	public boolean canRetry() {
		return true;
	}

}
