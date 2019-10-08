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
import java.awt.FlowLayout;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Collections;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import com.jaamsim.DisplayModels.IconModel;
import com.jaamsim.basicsim.Entity;
import com.jaamsim.basicsim.JaamSimModel;
import com.jaamsim.basicsim.ObjectType;
import com.jaamsim.input.Input;
import com.jaamsim.units.Unit;

public class FindBox extends JDialog {

	private JTextField searchText;

	private static FindBox myInstance;
	public static final String DIALOG_NAME = "Entity Finder";

	public FindBox() {
		super((JDialog)null, DIALOG_NAME, false);

		getContentPane().setLayout( new BorderLayout() );
		setIconImage(GUIFrame.getWindowIcon());

		// Search text
		searchText = new JTextField("", 30);
		searchText.setToolTipText(GUIFrame.formatToolTip("Entity Name Search.",
				"Entity name to find."));

		JPanel textPanel = new JPanel();
		textPanel.setLayout( new FlowLayout(FlowLayout.CENTER) );
		textPanel.add(searchText);
		getContentPane().add(textPanel, BorderLayout.NORTH);

		// Buttons
		JButton findButton = new JButton("Find");
		JButton closeButton = new JButton("Close");

		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout( new FlowLayout(FlowLayout.CENTER) );
		buttonPanel.add(findButton);
		buttonPanel.add(closeButton);
		getContentPane().add(buttonPanel, BorderLayout.CENTER);
		pack();

		// Find button pressed
		findButton.addActionListener( new ActionListener() {
			@Override
			public void actionPerformed( ActionEvent e ) {
				String name = searchText.getText().trim();
				if (name.isEmpty())
					return;
				findEntity(name);
			}
		} );

		// Close button pressed
		closeButton.addActionListener( new ActionListener() {
			@Override
			public void actionPerformed( ActionEvent e ) {
				setVisible(false);
				dispose();
			}
		} );

		// Return pressed
		searchText.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String name = searchText.getText().trim();
				if (name.isEmpty())
					return;
				findEntity(name);
			}
		});

		// Listen for changes to the text
		searchText.getDocument().addDocumentListener(new DocumentListener() {

			@Override
			public void insertUpdate(DocumentEvent e) {
				if (e.getLength() != 1)  // single character entered
					return;
				String name = searchText.getText().trim();
				myInstance.showEntityMenu(name);
			}

			@Override
			public void removeUpdate(DocumentEvent e) {
				String name = searchText.getText().trim();
				if (name.isEmpty())
					return;
				myInstance.showEntityMenu(name);
			}

			@Override
			public void changedUpdate(DocumentEvent e) {}
	    });

		// Window closed
		this.addWindowListener( new WindowAdapter() {
			@Override
			public void windowClosing( WindowEvent e ) {
				setVisible(false);
				dispose();
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

		// Position the finder at the centre of the screen
		Rectangle winSize = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
		setLocation((winSize.width - getWidth())/2, (winSize.height - getHeight())/2);

		// Show the dialog box and wait for editing to finish
		searchText.setText(str);
		this.setVisible(true);
	}

	private void findEntity(String name) {
		Entity ent = GUIFrame.getJaamSimModel().getEntity(name);
		if (ent == null || ent instanceof ObjectType || ent instanceof Unit ||
				ent instanceof IconModel) {
			String msg = String.format("Cannot find entity named: '%s'.", name);
			GUIFrame.showErrorDialog("Error", msg);
			return;
		}
		FrameBox.setSelectedEntity(ent, false);
	}

	private void showEntityMenu(String name) {
		ScrollablePopupMenu entityMenu = new ScrollablePopupMenu();
		ArrayList<String> nameList = new ArrayList<>();
		JaamSimModel simModel = GUIFrame.getJaamSimModel();
		for (Entity ent: simModel.getClonesOfIterator(Entity.class)) {
			if (ent instanceof ObjectType || ent instanceof Unit || ent instanceof IconModel)
				continue;
			if (!ent.getName().toUpperCase().contains(name.toUpperCase()))
				continue;
			nameList.add(ent.getName());
		}
		Collections.sort(nameList, Input.uiSortOrder);

		for (final String entName : nameList) {
			JMenuItem item = new JMenuItem(entName);
			item.addActionListener( new ActionListener() {
				@Override
				public void actionPerformed( ActionEvent event ) {
					searchText.setText(entName);
					findEntity(entName);
				}
			} );
			entityMenu.add(item);
		}
		entityMenu.show(searchText, 0, searchText.getHeight());
		searchText.requestFocusInWindow();
	}

}
