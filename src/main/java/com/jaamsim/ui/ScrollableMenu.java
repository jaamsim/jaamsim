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

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.UIManager;
import javax.swing.plaf.MenuItemUI;
import javax.swing.plaf.PopupMenuUI;

public class ScrollableMenu extends JMenu {

	private JPopupMenu popupMenu;  // JMenu has a private property with the same name

	public ScrollableMenu(String s) {
		super(s);
	}

	/**
	 * Mirrors the private method for JMenu with the same name
	 */
	protected void ensurePopupMenuCreated() {
		if (popupMenu == null) {
			popupMenu = new ScrollablePopupMenu();
			popupMenu.setInvoker(this);
			popupListener = createWinListener(popupMenu);
		}
	}

	// The following methods are the minimum for this application that need to be over-loaded
	// to refer to the new popupmenu property and ensurePopupMenuCreated method.
	// For full functionality, every method in JMenu that refers to popupMenu should be over-loaded.

	@Override
	public JPopupMenu getPopupMenu() {
		ensurePopupMenuCreated();
		return popupMenu;
	}

	@Override
	public JMenuItem add(JMenuItem menuItem) {
		ensurePopupMenuCreated();
		return popupMenu.add(menuItem);
	}

	@Override
	public boolean isPopupMenuVisible() {
		ensurePopupMenuCreated();
		return popupMenu.isVisible();
	}

	@Override
	public void updateUI() {
		setUI((MenuItemUI) UIManager.getUI(this));
		if (popupMenu != null) {
			popupMenu.setUI((PopupMenuUI) UIManager.getUI(popupMenu));
		}
	}

	public void ensureIndexIsVisible(int index) {
		((ScrollablePopupMenu)popupMenu).ensureIndexIsVisible(index);
	}

}
