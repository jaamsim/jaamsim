/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2019 JaamSim Software Inc.
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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Collections;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.plaf.basic.BasicArrowButton;

import com.jaamsim.DisplayModels.IconModel;
import com.jaamsim.basicsim.Entity;
import com.jaamsim.basicsim.JaamSimModel;
import com.jaamsim.basicsim.ObjectType;
import com.jaamsim.input.Input;
import com.jaamsim.units.Unit;

public class FindBox extends JDialog {

	private JTextField searchText;
	private final ArrayList<String> prevNames = new ArrayList<>();  // previous entities found
	private final Dimension itemSize;
	private ScrollablePopupMenu entityMenu;
	private final ArrayList<String> nameList = new ArrayList<>(); // eligible entity names

	private static FindBox myInstance;
	public static final String DIALOG_NAME = "Entity Finder";
	public static final int MAX_LIST_SIZE = 1000;  // max size of the entity list pop-up

	public FindBox() {
		super((JDialog)null, DIALOG_NAME, false);

		getContentPane().setLayout( new BorderLayout() );
		setIconImages(GUIFrame.getWindowIcons());

		// Search text
		searchText = new JTextField("", 30);
		searchText.setToolTipText(GUIFrame.formatToolTip("Entity Name",
				"Name of the entity to find."));

		// Recent searches
		JButton dropdown = new BasicArrowButton(BasicArrowButton.SOUTH);
		dropdown.setToolTipText(GUIFrame.formatToolTip("Previous Entities",
				"Entities that have been found previously."));

		JPanel textPanel = new JPanel();
		textPanel.setLayout( new BorderLayout() );
		textPanel.add(searchText, BorderLayout.WEST);
		textPanel.add(dropdown, BorderLayout.EAST);
		textPanel.setBorder(new EmptyBorder(10, 10, 0, 10));
		getContentPane().add(textPanel, BorderLayout.NORTH);

		itemSize = searchText.getPreferredSize();
		itemSize.width += dropdown.getPreferredSize().width;

		// Buttons
		JButton findButton = new JButton("Find");
		JButton closeButton = new JButton("Close");

		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout( new FlowLayout(FlowLayout.CENTER) );
		buttonPanel.add(findButton);
		buttonPanel.add(closeButton);
		getContentPane().add(buttonPanel, BorderLayout.CENTER);
		pack();

		// Set initial position in middle of screen
		setLocationRelativeTo(null);

		// Dropdown button pressed
		dropdown.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				entityMenu = new ScrollablePopupMenu();
				for (final String name : prevNames) {
					JMenuItem item = new JMenuItem(name);
					item.setPreferredSize(itemSize);
					item.addActionListener( new ActionListener() {

						@Override
						public void actionPerformed( ActionEvent event ) {
							entityMenu = null;
							searchText.setText(name);
							findEntity(name);
						}
					} );
					entityMenu.add(item);
				}
				entityMenu.show(searchText, 0, searchText.getHeight());
			}
		});

		// Find button pressed
		findButton.addActionListener( new ActionListener() {
			@Override
			public void actionPerformed( ActionEvent e ) {
				String name = searchText.getText().trim();
				findEntity(name);
			}
		} );

		// Close button pressed
		closeButton.addActionListener( new ActionListener() {
			@Override
			public void actionPerformed( ActionEvent e ) {
				setVisible(false);
			}
		} );

		// Return pressed
		searchText.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String name = searchText.getText().trim();
				if (!nameList.isEmpty()) {
					name = nameList.get(0);
					searchText.setText(name);
				}
				findEntity(name);
			}
		});

		// Down arrow pressed
		searchText.addKeyListener(new KeyListener() {
			@Override
			public void keyTyped(KeyEvent e) {}

			@Override
			public void keyPressed(KeyEvent e) {
				int keyCode = e.getKeyCode();
				if (keyCode == KeyEvent.VK_DOWN && entityMenu != null) {
					entityMenu.setVisible(false);
					String name = searchText.getText().trim();
					showEntityMenu(name, true);

					// Set the pop-up menu to the second item on the list
					SwingUtilities.invokeLater(new Runnable() {
						@Override
						public void run() {
							entityMenu.dispatchEvent(new KeyEvent(entityMenu, KeyEvent.KEY_PRESSED, 0, 0, KeyEvent.VK_DOWN, '\0'));
						}
					});
				}
			}

			@Override
			public void keyReleased(KeyEvent e) {}
		});

		// Listen for changes to the text
		searchText.getDocument().addDocumentListener(new DocumentListener() {

			@Override
			public void insertUpdate(DocumentEvent e) {
				if (e.getLength() != 1)  // single character entered
					return;
				String name = searchText.getText().trim();
				myInstance.showEntityMenu(name, false);
			}

			@Override
			public void removeUpdate(DocumentEvent e) {
				String name = searchText.getText().trim();
				myInstance.showEntityMenu(name, false);
			}

			@Override
			public void changedUpdate(DocumentEvent e) {}
	    });

		// Window closed
		this.addWindowListener( new WindowAdapter() {
			@Override
			public void windowClosing( WindowEvent e ) {
				setVisible(false);
			}
		} );
	}

	public synchronized static FindBox getInstance() {
		if (myInstance == null)
			myInstance = new FindBox();
		return myInstance;
	}

	private synchronized static void killInstance() {
		myInstance = null;
	}

	@Override
	public void dispose() {
		killInstance();
		super.dispose();
	}

	public void showDialog() {
		showDialog("");
	}

	public void showDialog(String str) {
		searchText.setText(str);
		this.setVisible(true);
	}

	private void findEntity(String name) {
		if (entityMenu != null) {
			entityMenu.setVisible(false);
			entityMenu = null;
		}
		if (name.isEmpty())
			return;
		Entity ent = GUIFrame.getJaamSimModel().getEntity(name);
		if (ent == null || ent instanceof ObjectType || ent instanceof Unit ||
				ent instanceof IconModel) {
			String msg = String.format("Cannot find entity named: '%s'.", name);
			GUIFrame.showErrorDialog("Error", msg);
			return;
		}
		prevNames.remove(name);
		prevNames.add(0, name);
		FrameBox.setSelectedEntity(ent, false);
	}

	private void showEntityMenu(String name, boolean focusable) {
		if (name.isEmpty()) {
			if (entityMenu != null) {
				entityMenu.setVisible(false);
				entityMenu = null;
			}
			return;
		}
		entityMenu = new ScrollablePopupMenu();
		nameList.clear();
		JaamSimModel simModel = GUIFrame.getJaamSimModel();
		for (Entity ent: simModel.getClonesOfIterator(Entity.class)) {
			if (ent instanceof ObjectType || ent instanceof Unit || ent instanceof IconModel)
				continue;
			if (!ent.getName().toUpperCase().contains(name.toUpperCase()))
				continue;
			nameList.add(ent.getName());
			if (nameList.size() >= MAX_LIST_SIZE)
				break;
		}
		Collections.sort(nameList, Input.uiSortOrder);

		boolean first = true;
		for (final String entName : nameList) {
			JMenuItem item = new JMenuItem(entName);
			item.setPreferredSize(itemSize);
			item.addActionListener( new ActionListener() {
				@Override
				public void actionPerformed( ActionEvent event ) {
					entityMenu = null;
					searchText.setText(entName);
					findEntity(entName);
				}
			} );
			entityMenu.add(item);
			if (first && !focusable) {
				item.setArmed(true);
				first = false;
			}
		}
		if (nameList.size() >= MAX_LIST_SIZE) {
			JMenuItem item = new JMenuItem("more ...");
			item.setPreferredSize(itemSize);
			entityMenu.add(item);
		}
		if (!focusable)
			entityMenu.setFocusable(false);
		entityMenu.show(searchText, 0, searchText.getHeight());
	}

}
