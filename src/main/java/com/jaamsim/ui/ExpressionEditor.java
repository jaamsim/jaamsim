/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2018-2020 JaamSim Software Inc.
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

import com.jaamsim.Commands.KeywordCommand;
import com.jaamsim.basicsim.Entity;
import com.jaamsim.input.InputAgent;
import com.jaamsim.input.KeywordIndex;

public class ExpressionEditor extends CellEditor {

	public ExpressionEditor(int width, int height) {
		super(width, height, true);
	}

	@Override
	public boolean canRetry() {
		return true;
	}

	@Override
	public void actionPerformed(ActionEvent e) {

		if ("button".equals(e.getActionCommand())) {

			// Launch the Expression Builder if there are no other options
			ArrayList<String> array = input.getValidOptions(EditBox.getInstance().getCurrentEntity());
			if (array == null || array.isEmpty()) {
				launchExpressionBox();
				return;
			}

			// If there are multiple options, select either one of the options or the
			// Expression Builder
			String valStr = input.getValueString();
			final String expBuilderOption = String.format("*** %s ***", ExpressionBox.DIALOG_NAME);
			array.add(0, expBuilderOption);
			ScrollablePopupMenu menu = new ScrollablePopupMenu();
			Component button = (Component)e.getSource();
			Component panel = button.getParent();
			for (final String option : array) {
				JMenuItem item = new JMenuItem(option);
				if (option.equals(valStr)) {
					item.setArmed(true);
				}
				item.setPreferredSize(panel.getPreferredSize());
				item.addActionListener( new ActionListener() {

					@Override
					public void actionPerformed( ActionEvent event ) {
						if (expBuilderOption.equals(option)) {
							launchExpressionBox();
							return;
						}
						setValue(option);
						stopCellEditing();
					}
				} );
				menu.add(item);
			}
			menu.show(panel, 0, panel.getHeight());

			// Scroll to show the present value
			if (input.isDefault())
				return;
			int index = array.indexOf(valStr);
			if (index != -1) {
				menu.ensureIndexIsVisible(index);
			}
			return;
		}
	}

	private void launchExpressionBox() {

		// Use the input from the Input Editor if it has been changed already,
		// otherwise use the input's value which includes any newline characters
		String str = input.getValueString();
		if (!str.replace('\n', ' ').equals(getValue()))
			str = getValue();

		// Launch the dialog box and wait for editing to finish
		ExpressionBox expDialog = new ExpressionBox(input, str);
		int result = expDialog.showDialog();

		// Return the new expression
		if (result == ExpressionBox.APPROVE_OPTION) {
			setValue(expDialog.getInputString());
		}
		else {
			// Reset the original value
			Entity ent = EditBox.getInstance().getCurrentEntity();
			try {
				KeywordIndex kw = InputAgent.formatInput(input.getKeyword(), str);
				InputAgent.storeAndExecute(new KeywordCommand(ent, kw));
			}
			catch (Exception e) {}
		}

		// Apply editing
		stopCellEditing();
	}

}
