/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2019-2024 JaamSim Software Inc.
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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.ArrayList;
import java.util.Collections;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import com.jaamsim.DisplayModels.IconModel;
import com.jaamsim.Graphics.EntityLabel;
import com.jaamsim.basicsim.Entity;
import com.jaamsim.basicsim.JaamSimModel;
import com.jaamsim.basicsim.ObjectType;
import com.jaamsim.units.Unit;

public class FindBox extends JDialog {

	private SearchField searchText;
	private final AutoCompleteComparator autoCompleteComparator = new AutoCompleteComparator();

	private static FindBox myInstance;
	public static final String DIALOG_NAME = "Entity Finder";
	public static final int MAX_LIST_SIZE = 1000;  // max size of the entity list pop-up

	public FindBox() {
		super((JDialog)null, DIALOG_NAME, false);
		getContentPane().setLayout( new BorderLayout() );
		setIconImages(GUIFrame.getWindowIcons());
		setDefaultCloseOperation(HIDE_ON_CLOSE);

		// Search text
		searchText = new SearchField(50) {
			@Override
			public boolean showTopic(String topic) {
				return findEntity(topic);
			}
			@Override
			public ArrayList<String> getTopicList(String str) {
				str = str.trim();
				str = str.replaceAll("[{}\\[\\],'\"]", "");  // remove braces, commas, and quotes
				str = str.replaceAll("\\(R\\)", "");  // remove reverse tag
				String[] names = str.split("[\\s]");  // split on whitespace
				ArrayList<String> ret = new ArrayList<>();
				for (String name : names) {
					ret.addAll(getNameList(name));
				}
				return ret;
			}
		};
		searchText.setToolTipText(GUIFrame.formatToolTip("Entity Name",
				"Name of the entity to find."));

		searchText.setBorder(new EmptyBorder(10, 10, 0, 10));
		getContentPane().add(searchText, BorderLayout.NORTH);

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

		// Find button pressed
		findButton.addActionListener( new ActionListener() {
			@Override
			public void actionPerformed( ActionEvent e ) {
				String name = searchText.getText().trim();
				searchText.showAndSaveTopic(name);
			}
		} );

		// Close button pressed
		closeButton.addActionListener( new ActionListener() {
			@Override
			public void actionPerformed( ActionEvent e ) {
				setVisible(false);
			}
		} );

		// Focus on the search text
		addFocusListener(new FocusListener() {
			@Override
			public void focusGained(FocusEvent e) {
				searchText.requestFocusInWindow();
			}
			@Override
			public void focusLost(FocusEvent e) {}
		});
	}

	public synchronized static FindBox getInstance() {
		if (myInstance == null)
			myInstance = new FindBox();
		return myInstance;
	}

	public synchronized static boolean hasInstance() {
		return (myInstance != null);
	}

	private synchronized static void killInstance() {
		myInstance = null;
	}

	@Override
	public void dispose() {
		killInstance();
		super.dispose();
	}

	/**
	 * Launches the Find tool and searches for entities that contain the specified string.
	 * @param str - string to search
	 */
	public void search(String str) {
		if (str == null || str.isEmpty())
			return;
		this.setVisible(true);
		searchText.search(str);
	}

	public void showDialog() {
		showDialog("");
	}

	public void showDialog(String str) {
		searchText.setText(str);
		this.setVisible(true);
		searchText.requestFocusInWindow();
	}

	private boolean findEntity(String name) {
		if (name.isEmpty())
			return false;
		Entity ent = GUIFrame.getJaamSimModel().getEntity(name);
		if (ent == null || ent instanceof ObjectType || ent instanceof Unit ||
				ent instanceof IconModel) {
			if (searchText.getTopicList(name).isEmpty()) {
				String msg = String.format("Cannot find entity named: '%s'.", name);
				GUIFrame.showErrorDialog("Error", msg);
			}
			else {
				searchText.search(name);
			}
			return false;
		}
		FrameBox.setSelectedEntity(ent, false);
		return true;
	}

	private ArrayList<String> getNameList(String name) {
		ArrayList<String> nameList = new ArrayList<>();
		boolean more = false;
		JaamSimModel simModel = GUIFrame.getJaamSimModel();
		for (Entity ent: simModel.getClonesOfIterator(Entity.class)) {
			if (ent instanceof ObjectType || ent instanceof Unit || ent instanceof IconModel
					|| ent instanceof EntityLabel)
				continue;
			if (!ent.getName().toUpperCase().contains(name.toUpperCase()))
				continue;
			nameList.add(ent.getName());
			if (nameList.size() >= MAX_LIST_SIZE) {
				more = true;
				break;
			}
		}
		autoCompleteComparator.setName(name);
		Collections.sort(nameList, autoCompleteComparator);
		if (more)
			nameList.add("more ...");
		return nameList;
	}

}
