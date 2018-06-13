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
import java.util.ArrayList;

import javax.swing.JTable;

import com.jaamsim.input.Parser;

/**
 * Handles inputs where a list of entities can be selected.
 *
 */
public class ListEditor extends ChooserEditor {

	private ArrayList<String> options;
	private boolean caseSensitive;
	private boolean innerBraces;

	public ListEditor(JTable table, ArrayList<String> aList) {
		super(table, true);
		options = aList;
		caseSensitive = true;
		innerBraces = false;
	}

	@Override
	public void actionPerformed(ActionEvent e) {

		if (!"button".equals(e.getActionCommand())) {
			return;
		}

		if (!caseSensitive) {
			for (int i = 0; i < options.size(); i++ ) {
				options.set(i, options.get(i).toUpperCase() );
			}
		}

		// Open the dialog box and wait for it to be closed
		ArrayList<String> initList = new ArrayList<>();
		Parser.tokenize(initList, getValue(), true);
		ListDialog dialog = new ListDialog(EditBox.getInstance(), "Select items", true,
				options, initList);
		dialog.setLocationRelativeTo((Component)e.getSource());
		int result = dialog.showDialog();

		// Return the selected items
		if (result == ListDialog.APPROVE_OPTION) {
			StringBuilder sb = new StringBuilder();
			for (String str : dialog.getList()) {
				if (innerBraces)
					sb.append("{ ").append(str).append(" } ");
				else
					sb.append(str).append(" ");
			}
			setValue(sb.toString());
		}

		// Apply editing
		stopCellEditing();

		// Focus the cell
		propTable.requestFocusInWindow();
	}

	public void setCaseSensitive(boolean bool) {
		caseSensitive = bool;
	}

	public void setInnerBraces(boolean bool) {
		innerBraces = bool;
	}

}
