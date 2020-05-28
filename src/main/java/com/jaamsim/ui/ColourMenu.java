/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2020 JaamSim Software Inc.
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
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;

import javax.swing.JColorChooser;
import javax.swing.JDialog;
import javax.swing.JMenuItem;

import com.jaamsim.input.ColourInput;
import com.jaamsim.math.Color4d;

public abstract class ColourMenu extends ScrollablePopupMenu {

	JMenuItem selectedItem = null;
	int selectedIndex = -1;

	private static JColorChooser colorChooser;

	public static final String DIALOG_NAME = "Colour Chooser";
	public static final String OPTION_COLOUR_CHOOSER = String.format("*** %s ***", DIALOG_NAME);

	public ColourMenu(Color4d presentColour, ArrayList<Color4d> coloursInUse, boolean preview) {

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
				setColour(item.getText());
			}
			@Override
			public void mouseExited(MouseEvent e) {
				String colStr = ColourInput.toString(presentColour);
				setColour(colStr);
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
		int ind = 0;
		for (Color4d col : coloursInUse) {
			String colStr = ColourInput.toString(col);
			JMenuItem item = new JMenuItem(colStr);
			ColorIcon icon = new ColorIcon(16, 16);
			icon.setFillColor(
					new Color((float)col.r, (float)col.g, (float)col.b, (float)col.a));
			icon.setOutlineColor(Color.DARK_GRAY);
			item.setIcon(icon);
			if (selectedItem == null && col.equals(presentColour)) {
				selectedItem = item;
				selectedIndex = ind;
			}
			ind++;
			item.addActionListener(actionListener);
			if (preview)
				item.addMouseListener(mouseListener);
			add(item);
		}
		addSeparator();

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
				dialog.setIconImage(GUIFrame.getWindowIcon());
				dialog.setAlwaysOnTop(true);
				if (presentColour != null) {
					chooser.setColor(new Color((float)presentColour.r, (float)presentColour.g,
							(float)presentColour.b, (float)presentColour.a));
				}
				dialog.setVisible(true);
			}
		});
		add(chooserItem);
		addSeparator();

		// All possible colours
		for (Color4d col : ColourInput.namedColourList) {
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
	}

	public static JColorChooser getColorChooser() {
		if (colorChooser == null)
			colorChooser = new JColorChooser();
		return colorChooser;
	}

	@Override
	public void show(Component invoker, int x, int y) {
		super.show(invoker, x, y);
		if (selectedItem != null) {
			ensureIndexIsVisible(selectedIndex);
			selectedItem.setArmed(true);
		}
	}

	public abstract void setColour(String colStr);

}
