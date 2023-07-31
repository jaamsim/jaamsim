/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2005-2013 Ausenco Engineering Canada Inc.
 * Copyright (C) 2016-2023 JaamSim Software Inc.
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

import com.jaamsim.ColourProviders.ColourProvInput;
import com.jaamsim.basicsim.Entity;
import com.jaamsim.input.ColourInput;
import com.jaamsim.math.Color4d;

/**
 * Handles colour inputs.
 *
 */
public class ColorEditor extends CellEditor {

	public static final String OPTION_INPUT_BUILDER = String.format("*** %s ***", ExpressionBox.DIALOG_NAME);

	public ColorEditor(int width, int height) {
		super(width, height, true);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if("button".equals(e.getActionCommand())) {

			// Present colour
			Color4d col = null;
			if (input instanceof ColourInput) {
				col = (Color4d) input.getValue();
			}
			if (input instanceof ColourProvInput && ((ColourProvInput) input).isConstant()) {
				Entity ent = getTable().getEntity();
				col = ((ColourProvInput) input).getNextColour(ent, 0.0d);
			}
			ArrayList<Color4d> coloursInUse = new ArrayList<>(1);
			if (col != null)
				coloursInUse.add(col);

			// Input Builder
			JMenuItem builderItem = null;
			if (input.useExpressionBuilder()) {
				builderItem = new JMenuItem(OPTION_INPUT_BUILDER);
				builderItem.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent event) {
						launchExpressionBox();
					}
				});
			}

			ColourMenu menu = new ColourMenu(col, coloursInUse, builderItem, true) {

				@Override
				public void setColour(String colStr) {
					setValue(colStr);
					stopCellEditing();
				}

				@Override
				public void showPreview(String colStr) {
					setInputValue(colStr);
				}

			};

			Component button = (Component)e.getSource();
			Component panel = button.getParent();
			menu.show(panel, 0, panel.getHeight());
			return;
		}
	}

}
