/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2020-2023 JaamSim Software Inc.
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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;

import javax.swing.JColorChooser;
import javax.swing.JDialog;
import javax.swing.JMenuItem;

import com.jaamsim.input.ColourInput;
import com.jaamsim.math.Color4d;

public abstract class ColourMenu extends ScrollablePopupMenu {

	private static JColorChooser colorChooser;

	public static final String DIALOG_NAME = "Colour Chooser";
	public static final String OPTION_COLOUR_CHOOSER = String.format("*** %s ***", DIALOG_NAME);

	public ColourMenu(Color4d presentColour, ArrayList<Color4d> coloursInUse, boolean preview) {
		this(presentColour, coloursInUse, null, preview);
	}

	public ColourMenu(Color4d presentColour, ArrayList<Color4d> coloursInUse, JMenuItem builderItem, boolean preview) {

		ActionListener actionListener = new ActionListener() {
			@Override
			public void actionPerformed( ActionEvent event ) {
				if (!(event.getSource() instanceof JMenuItem))
					return;
				JMenuItem item = (JMenuItem) event.getSource();
				setColour(item.getText());
			}
		};

		MouseListener mouseListener = new MouseListener() {
			@Override
			public void mouseClicked(MouseEvent e) {}
			@Override
			public void mousePressed(MouseEvent e) {}
			@Override
			public void mouseReleased(MouseEvent e) {}
			@Override
			public void mouseEntered(MouseEvent e) {
				if (!(e.getSource() instanceof JMenuItem))
					return;
				JMenuItem item = (JMenuItem) e.getSource();
				showPreview(item.getText());
			}
			@Override
			public void mouseExited(MouseEvent e) {
				String colStr = ColourInput.toString(presentColour);
				showPreview(colStr);
			}
		};

		final ActionListener chooserActionListener = new ActionListener() {
			@Override
			public void actionPerformed( ActionEvent event ) {
				Color clr = getColorChooser().getColor();
				Color4d newColour = new Color4d(clr.getRed(), clr.getGreen(),
						clr.getBlue(), clr.getAlpha());
				String colStr = ColourInput.toString(newColour);
				setColour(colStr);
			}
		};

		// Colours already in use
		for (Color4d col : coloursInUse) {
			String colStr = ColourInput.toString(col);
			JMenuItem item = new JMenuItem(colStr);
			ColorIcon icon = new ColorIcon(16, 16);
			icon.setFillColor(
					new Color((float)col.r, (float)col.g, (float)col.b, (float)col.a));
			icon.setOutlineColor(Color.DARK_GRAY);
			item.setIcon(icon);
			item.addActionListener(actionListener);
			if (preview)
				item.addMouseListener(mouseListener);
			add(item);
		}
		addSeparator();

		// Input Builder
		if (builderItem != null) {
			add(builderItem);
		}

		// Colour chooser
		JMenuItem chooserItem = new JMenuItem(OPTION_COLOUR_CHOOSER);
		chooserItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				JColorChooser chooser = getColorChooser();
				JDialog dialog = JColorChooser.createDialog(null,
						DIALOG_NAME,
						true,  //modal
						chooser,
						chooserActionListener,  //OK button listener
						null); //no CANCEL button listener
				dialog.setIconImages(GUIFrame.getWindowIcons());
				dialog.setAlwaysOnTop(true);
				if (presentColour != null) {
					chooser.setColor(new Color((float)presentColour.r, (float)presentColour.g,
							(float)presentColour.b, (float)presentColour.a));
				}
				dialog.setVisible(true);
			}
		});
		add(chooserItem);

		// All possible colours
		for (String family : ColourInput.getColorFamilies()) {
			addSeparator();
			add(new JMenuItem(family.toUpperCase() + " COLOURS:"));
			for (String colStr : ColourInput.getColorListForFamily(family)) {
				Color4d col = ColourInput.getColorWithName(colStr);
				JMenuItem item = new JMenuItem(colStr);
				ColorIcon icon = new ColorIcon(16, 16);
				icon.setFillColor(
						new Color((float)col.r, (float)col.g, (float)col.b, (float)col.a));
				icon.setOutlineColor(Color.DARK_GRAY);
				item.setIcon(icon);
				item.addActionListener(actionListener);
				if (preview)
					item.addMouseListener(mouseListener);
				add(item);
			}
		}
	}

	public static JColorChooser getColorChooser() {
		if (colorChooser == null)
			colorChooser = new JColorChooser();
		return colorChooser;
	}

	public abstract void setColour(String colStr);

}
