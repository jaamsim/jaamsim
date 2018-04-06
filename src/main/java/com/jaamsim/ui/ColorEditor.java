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

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;

import javax.swing.JColorChooser;
import javax.swing.JDialog;
import javax.swing.JTable;

import com.jaamsim.input.ColourInput;
import com.jaamsim.math.Color4d;

/**
 * Handles colour inputs.
 *
 */
public class ColorEditor extends ChooserEditor {

	private JColorChooser colorChooser;
	private JDialog dialog;

	public ColorEditor(JTable table) {
		super(table, true);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if("button".equals(e.getActionCommand())) {
			if(colorChooser == null || dialog == null) {
				colorChooser = new JColorChooser();
				dialog = JColorChooser.createDialog(null,
						"Pick a Color",
						true,  //modal
						colorChooser,
						this,  //OK button listener
						null); //no CANCEL button listener
				dialog.setIconImage(GUIFrame.getWindowIcon());
				dialog.setAlwaysOnTop(true);
			}

			Color4d col = ((ColourInput)input).getValue();
			colorChooser.setColor(new Color((float)col.r, (float)col.g, (float)col.b, (float)col.a));
			dialog.setLocationRelativeTo((Component)e.getSource());
			dialog.setVisible(true);

			// Apply editing
			stopCellEditing();

			// Focus the cell
			propTable.requestFocusInWindow();
		}
		else {
			Color color = colorChooser.getColor();
			if (color.getAlpha() == 255) {
				setValue( String.format("%d %d %d",
						color.getRed(), color.getGreen(), color.getBlue() ) );
				return;
			}
			setValue( String.format("%d %d %d %d",
					 color.getRed(),color.getGreen(), color.getBlue(), color.getAlpha() ) );
		}
	}

}
