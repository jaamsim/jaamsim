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
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.JColorChooser;
import javax.swing.JDialog;
import javax.swing.JMenuItem;

import com.jaamsim.input.ColourInput;
import com.jaamsim.math.Color4d;
import com.jaamsim.ui.EditBox.EditTable;

/**
 * Handles colour inputs.
 *
 */
public class ColorEditor extends ChooserEditor {

	private static JColorChooser colorChooser;

	public static final String DIALOG_NAME = "Colour Chooser";

	public ColorEditor(EditTable table) {
		super(table, true);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if("button".equals(e.getActionCommand())) {

			// Present colour
			Color4d col = (Color4d) input.getValue();
			String colName = ColourInput.getColorName(col);

			// Prepare a list of the named colours
			ArrayList<String> array = input.getValidOptions();
			final String colourChooserOption = String.format("*** %s ***", DIALOG_NAME);
			array.add(0, colourChooserOption);
			ScrollablePopupMenu menu = new ScrollablePopupMenu();
			Component button = (Component)e.getSource();
			Component panel = button.getParent();
			for (final String option : array) {
				JMenuItem item = new JMenuItem(option);
				if (option.equals(colName)) {
					item.setArmed(true);
				}
				item.setPreferredSize(panel.getPreferredSize());
				item.addActionListener( new ActionListener() {

					@Override
					public void actionPerformed( ActionEvent event ) {
						if (colourChooserOption.equals(option)) {
							launchDialog();
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

			// Scroll to show the present colour
			if (input.isDefault())
				return;
			int index = array.indexOf(colName);
			if (index != -1) {
				menu.ensureIndexIsVisible(index);
			}
			return;
		}
		else {
			Color color = getColorChooser().getColor();
			Color4d newColour = new Color4d(color.getRed(), color.getGreen(),
					color.getBlue(), color.getAlpha());
			setValue(ColourInput.toString(newColour));
		}
	}

	public static JColorChooser getColorChooser() {
		if (colorChooser == null)
			colorChooser = new JColorChooser();
		return colorChooser;
	}

	public void launchDialog() {
		JColorChooser chooser = getColorChooser();
		JDialog dialog = JColorChooser.createDialog(null,
				DIALOG_NAME,
				true,  //modal
				chooser,
				this,  //OK button listener
				null); //no CANCEL button listener
		dialog.setIconImage(GUIFrame.getWindowIcon());
		dialog.setAlwaysOnTop(true);

		Color4d col = ((ColourInput)input).getValue();
		chooser.setColor(new Color((float)col.r, (float)col.g, (float)col.b, (float)col.a));
		dialog.setVisible(true);

		// Apply editing
		stopCellEditing();

		// Focus the cell
		propTable.requestFocusInWindow();
	}

}
