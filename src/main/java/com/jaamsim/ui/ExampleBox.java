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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;

import javax.imageio.ImageIO;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.jaamsim.basicsim.JaamSimModel;
import com.jaamsim.input.Input;
import com.jaamsim.input.InputAgent;

public class ExampleBox extends JDialog {

	private String presentExample;
	private final ArrayList<String> exampleList = new ArrayList<>();
	private JList<String> list;
	private final SearchField exampleSearch;

	private final JLabel previewLabel;
	private final ImageIcon previewIcon = new ImageIcon();

	private static ExampleBox myInstance;

	private static final String DIALOG_NAME = "Examples - JaamSim";
	private static final String DEFAULT_TOPIC = "";

	public ExampleBox() {
		super((JDialog)null, DIALOG_NAME, false);
		setIconImages(GUIFrame.getWindowIcons());
		setResizable(true);
		setDefaultCloseOperation(HIDE_ON_CLOSE);

		getContentPane().setLayout( new BorderLayout() );
		setMinimumSize(new Dimension(300, 300));
		setPreferredSize(new Dimension(1000, 800));

		// Example List
		for (String name : GUIFrame.getResourceFileNames("/resources/examples")) {
			if (name.endsWith(".cfg")) {
				exampleList.add(name.substring(0, name.length() - 4));
			}
		}
		Collections.sort(exampleList, Input.uiSortOrder);

		// Example search
		exampleSearch = new SearchField(30) {
			@Override
			public void showTopic(String topic) {
				ExampleBox.this.showTopic(topic);
			}
			@Override
			public ArrayList<String> getTopicList(String str) {
				ArrayList<String> ret = new ArrayList<>();
				for (String topic : exampleList) {
					if (!topic.toUpperCase().contains(str.toUpperCase()))
						continue;
					ret.add(topic);
				}
				return ret;
			}
		};
		exampleSearch.setToolTipText(GUIFrame.formatToolTip("Example Model",
				"Title of the example model to find."));

		JPanel textPanel = new JPanel();
		textPanel.setLayout( new FlowLayout(FlowLayout.CENTER, 0, 0) );
		textPanel.add(new JLabel("Find Example Model:"));
		textPanel.add(Box.createRigidArea(new Dimension(5, 5)));
		textPanel.add(exampleSearch);
		textPanel.setBorder(new EmptyBorder(10, 5, 5, 5));
		getContentPane().add(textPanel, BorderLayout.NORTH);

		// Example selector
		String[] topics = new String[exampleList.size()];
		topics = exampleList.toArray(topics);
		list = new JList<>(topics);
		list.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
		list.addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {
				int ind = list.getSelectedIndex();
				if (ind == -1)
					return;
				showTopic(exampleList.get(ind));
				exampleSearch.setText("");
			}
		});
		JScrollPane listScroller = new JScrollPane(list);
		listScroller.setBorder(new EmptyBorder(5, 5, 5, 0));
		listScroller.setPreferredSize(new Dimension(250, 200));
		getContentPane().add(listScroller, BorderLayout.WEST);

		// Example preview
		previewLabel = new JLabel("", JLabel.CENTER);
		previewLabel.setBorder(new EmptyBorder(5, 5, 5, 5));
		getContentPane().add(previewLabel, BorderLayout.CENTER);

		// Open button
		JButton openButton = new JButton("Open");
		openButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {

				// Create the new simulation model
				JaamSimModel simModel = new JaamSimModel(presentExample + ".cfg");

				// Load the specified model file
				simModel.autoLoad();
				GUIFrame.getInstance().setWindowDefaults(simModel.getSimulation());
				InputAgent.readResource(simModel, "<res>/examples/" + presentExample + ".cfg");
				simModel.postLoad();

				// Display the new model
				GUIFrame.setJaamSimModel(simModel);
				GUIFrame.getInstance().clear();
				FrameBox.setSelectedEntity(simModel.getSimulation(), false);
			}
		});

		// Close button
		JButton closeButton = new JButton("Close");
		closeButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				setVisible(false);
			}
		});

		// Add the buttons to the dialog
		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout( new FlowLayout(FlowLayout.CENTER) );
		buttonPanel.add(openButton);
		buttonPanel.add(closeButton);
		getContentPane().add("South", buttonPanel);
		pack();

		// Set initial position in middle of screen
		setLocationRelativeTo(null);
	}

	public synchronized static ExampleBox getInstance() {
		if (myInstance == null)
			myInstance = new ExampleBox();
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

	/**
	 * Launches the Help tool for the specified string that determines the topic to be displayed.
	 * If the string is an exact match to a topic then that topic is displayed.
	 * Otherwise, the displayed topic is the first one in the list of topics that contains the string.
	 * If the string is blank, the previous topic is retained.
	 * If there is no previous topic, the default topic is displayed.
	 * @param str - determines the topic to be displayed
	 */
	public void showDialog(String str) {
		String topic = "";  // displays the present topic

		// Present topic or default topic
		if (str.isEmpty()) {
			if (presentExample == null)
				topic = DEFAULT_TOPIC;
		}

		// Exact match to a topic
		else if (exampleList.contains(str)) {
			topic = str;
		}

		// First topic that contains the string
		else {
			for (String tpc : exampleList) {
				if (tpc.toUpperCase().contains(str.toUpperCase())) {
					topic = tpc;
					break;
				}
			}
		}

		// Display the selected topic
		showTopic(topic);
		exampleSearch.setText("");
		this.setVisible(true);
	}

	private void showTopic(String topic) {
		try {
			URL url = GUIFrame.class.getResource("/resources/examples/" + topic + ".cfg");
			if (url == null)
				return;
			presentExample = topic;
			int ind = exampleList.indexOf(topic);
			list.setSelectedIndex(ind);
			list.ensureIndexIsVisible(ind);

			// Clear the old preview image
			previewLabel.setIcon(null);

			// Get the preview image
			url = GUIFrame.class.getResource("/resources/examples/" + topic + ".png");
			if (url == null)
				return;
			BufferedImage image = ImageIO.read(url);

			// Display the image
			previewIcon.setImage(image);
			previewLabel.setIcon(previewIcon);
		}
		catch (Throwable t) {}
	}

}
