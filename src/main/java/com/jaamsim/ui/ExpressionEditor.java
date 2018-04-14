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

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTable;

public class ExpressionEditor extends ChooserEditor {

	private static final String OPTION_EXP_BUILDER = "*** Expression Builder ***";

	public ExpressionEditor(JTable table) {
		super(table, true);
	}

	@Override
	public boolean canRetry() {
		return true;
	}

	@Override
	public void actionPerformed(ActionEvent e) {

		if ("button".equals(e.getActionCommand())) {

			// Launch the Expression Builder if there are no other options
			ArrayList<String> array = input.getValidOptions();
			if (array == null || array.isEmpty()) {
				launchExpressionBox();
				return;
			}

			// If there are multiple options, select either one of the options or the
			// Expression Builder
			array.add(0, OPTION_EXP_BUILDER);
			JPopupMenu menu = new JPopupMenu();
			Component button = (Component)e.getSource();
			Component panel = button.getParent();
			for (final String option : array) {
				JMenuItem item = new JMenuItem(option);
				item.setPreferredSize(panel.getPreferredSize());
				item.addActionListener( new ActionListener() {

					@Override
					public void actionPerformed( ActionEvent event ) {
						if (OPTION_EXP_BUILDER.equals(option)) {
							launchExpressionBox();
							return;
						}
						setValue(option);
						stopCellEditing();
						propTable.requestFocusInWindow();
					}
				} );
				menu.add(item);
			}
			menu.show(panel, 0, panel.getHeight());
			return;
		}
	}

	private void launchExpressionBox() {

		// Launch the dialog box and wait for editing to finish
		ExpressionBox expDialog = new ExpressionBox(input, getValue());
		int result = expDialog.showDialog();

		// Return the new expression
		if (result == ExpressionBox.APPROVE_OPTION) {
			setValue(expDialog.getInputString());
		}

		// Apply editing
		stopCellEditing();

		// Focus the cell
		propTable.requestFocusInWindow();
	}

}
