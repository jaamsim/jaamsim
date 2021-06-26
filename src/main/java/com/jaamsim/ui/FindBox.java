/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2019-2021 JaamSim Software Inc.
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
import java.util.ArrayList;
import java.util.Collections;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import com.jaamsim.DisplayModels.IconModel;
import com.jaamsim.basicsim.Entity;
import com.jaamsim.basicsim.JaamSimModel;
import com.jaamsim.basicsim.ObjectType;
import com.jaamsim.input.Input;
import com.jaamsim.units.Unit;

public class FindBox extends JDialog {

	private SearchField searchText;

	private static FindBox myInstance;
	public static final String DIALOG_NAME = "Entity Finder";
	public static final int MAX_LIST_SIZE = 1000;  // max size of the entity list pop-up

	public FindBox() {
		super((JDialog)null, DIALOG_NAME, false);
		getContentPane().setLayout( new BorderLayout() );
		setIconImages(GUIFrame.getWindowIcons());
		setDefaultCloseOperation(HIDE_ON_CLOSE);

		// Search text
		searchText = new SearchField(30) {
			@Override
			public void showTopic(String topic) {
				findEntity(topic);
			}
			@Override
			public ArrayList<String> getTopicList(String str) {
				return getNameList(str);
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

	public void showDialog() {
		showDialog("");
	}

	public void showDialog(String str) {
		searchText.setText(str);
		this.setVisible(true);
	}

	private void findEntity(String name) {
		if (name.isEmpty())
			return;
		Entity ent = GUIFrame.getJaamSimModel().getEntity(name);
		if (ent == null || ent instanceof ObjectType || ent instanceof Unit ||
				ent instanceof IconModel) {
			String msg = String.format("Cannot find entity named: '%s'.", name);
			GUIFrame.showErrorDialog("Error", msg);
			return;
		}
		FrameBox.setSelectedEntity(ent, false);
	}

	private ArrayList<String> getNameList(String name) {
		ArrayList<String> nameList = new ArrayList<>();
		JaamSimModel simModel = GUIFrame.getJaamSimModel();
		for (Entity ent: simModel.getClonesOfIterator(Entity.class)) {
			if (ent instanceof ObjectType || ent instanceof Unit || ent instanceof IconModel)
				continue;
			if (!ent.getName().toUpperCase().contains(name.toUpperCase()))
				continue;
			nameList.add(ent.getName());
			if (nameList.size() >= MAX_LIST_SIZE) {
				nameList.add("more ...");
				break;
			}
		}
		Collections.sort(nameList, Input.uiSortOrder);
		return nameList;
	}

}
