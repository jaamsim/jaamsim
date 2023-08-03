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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;

import javax.swing.JMenuItem;

public abstract class PreviewablePopupMenu extends ScrollablePopupMenu {

	public PreviewablePopupMenu(String presentValue, ArrayList<String> choices, boolean preview) {
		this(presentValue, new ArrayList<String>(), choices, preview);
	}

	/**
	 * Constructs a pop-up menu that previews the effect of each menu choice.
	 * @param presentValue - present choice for the value to be set
	 * @param valuesInUse - values that are already in use
	 * @param choices - values from which to choose
	 * @param preview - if true, the effect of each value is previewed on mouse over
	 */
	public PreviewablePopupMenu(String presentValue, ArrayList<String> valuesInUse,
			ArrayList<String> choices, boolean preview) {

		ActionListener actionListener = new ActionListener() {
			@Override
			public void actionPerformed( ActionEvent event ) {
				if (!(event.getSource() instanceof JMenuItem))
					return;
				JMenuItem item = (JMenuItem) event.getSource();
				setValue(item.getText());
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
				showPreview(presentValue);
			}
		};

		// Values in use
		for (String val : valuesInUse) {
			JMenuItem item = new JMenuItem(val);
			item.addActionListener(actionListener);
			if (preview)
				item.addMouseListener(mouseListener);
			add(item);
		}
		if (!valuesInUse.isEmpty())
			addSeparator();

		// Menu item for each choice
		for (String val : choices) {
			JMenuItem item = new JMenuItem(val);
			item.addActionListener(actionListener);
			if (preview)
				item.addMouseListener(mouseListener);
			add(item);
		}
	}

	/**
	 * Sets the result of the menu selection.
	 * @param str - value selected from the menu
	 */
	public abstract void setValue(String str);

}
