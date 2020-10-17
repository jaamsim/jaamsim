/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2005-2013 Ausenco Engineering Canada Inc.
 * Copyright (C) 2016-2020 JaamSim Software Inc.
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

import com.jaamsim.ui.EditBox.EditTable;

/**
 * Handles inputs with drop-down menus.
 *
 */
public class DropDownMenuEditor extends CellEditor {

	private ArrayList<String> options;

	public DropDownMenuEditor(EditTable table, ArrayList<String> aList) {
		super(table, true);
		options = aList;
	}

	@Override
	public void actionPerformed(ActionEvent e) {

		if ("button".equals(e.getActionCommand())) {

			String valStr = input.getValueString();
			ScrollablePopupMenu menu = new ScrollablePopupMenu();
			Component button = (Component)e.getSource();
			Component panel = button.getParent();
			for (final String option : options) {
				JMenuItem item = new JMenuItem(option);
				item.setPreferredSize(panel.getPreferredSize());
				item.addActionListener( new ActionListener() {

					@Override
					public void actionPerformed( ActionEvent event ) {
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
			int index = options.indexOf(valStr);
			if (index != -1) {
				menu.ensureIndexIsVisible(index);
			}
			return;
		}
	}

	@Override
	public boolean canRetry() {
		return true;
	}

}
