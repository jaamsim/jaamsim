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
import java.util.ArrayList;

import com.jaamsim.math.Color4d;
import com.jaamsim.ui.EditBox.EditTable;

/**
 * Handles colour inputs.
 *
 */
public class ColorEditor extends CellEditor {

	public ColorEditor(EditTable table) {
		super(table, true);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if("button".equals(e.getActionCommand())) {

			// Present colour
			Color4d col = (Color4d) input.getValue();
			ArrayList<Color4d> coloursInUse = new ArrayList<>(1);
			if (col != null)
				coloursInUse.add(col);
			ColourMenu menu = new ColourMenu(col, coloursInUse, false) {

				@Override
				public void setColour(String colStr) {
					setValue(colStr);
					stopCellEditing();
					propTable.requestFocusInWindow();
				}

			};
			Component button = (Component)e.getSource();
			Component panel = button.getParent();
			menu.show(panel, 0, panel.getHeight());
			return;
		}
	}

}
