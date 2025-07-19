/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2020-2025 JaamSim Software Inc.
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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.jaamsim.Graphics.View;
import com.jaamsim.basicsim.JaamSimModel;
import com.jaamsim.basicsim.RunManager;
import com.jaamsim.basicsim.Simulation;
import com.jaamsim.controllers.RenderManager;
import com.jaamsim.input.Input;
import com.jaamsim.input.InputAgent;
import com.jaamsim.render.CameraInfo;
import com.jaamsim.render.Future;

public class ExampleBox extends JDialog {

	private String presentExample;
	private final ArrayList<String> exampleList;
	private JList<String> list;
	private final SearchField exampleSearch;
	private final AutoCompleteComparator autoCompleteComparator = new AutoCompleteComparator();

	private final JLabel previewLabel;
	private final ImageIcon previewIcon = new ImageIcon();

	private final HashMap<String, Future<BufferedImage>> imageCache = new HashMap<>();

	private static ExampleBox myInstance;

	private static final String DIALOG_NAME = "Examples - JaamSim";
	private static final String DEFAULT_TOPIC = "";
	private static final String EXAMPLES_FOLDER_NAME = "/resources/examples";

	private ExampleBox() {
		super((JDialog)null, DIALOG_NAME, false);
		setIconImages(GUIFrame.getWindowIcons());
		setResizable(true);
		setDefaultCloseOperation(HIDE_ON_CLOSE);

		getContentPane().setLayout( new BorderLayout() );
		setMinimumSize(new Dimension(300, 300));
		setPreferredSize(new Dimension(1100, 800));

		// Example List
		exampleList = getExampleList();

		// Example search
		exampleSearch = new SearchField(50) {
			@Override
			public boolean showTopic(String topic) {
				return ExampleBox.this.showTopic(topic);
			}
			@Override
			public ArrayList<String> getTopicList(String str) {
				ArrayList<String> ret = new ArrayList<>();
				for (String topic : exampleList) {
					if (!topic.toUpperCase().contains(str.toUpperCase()))
						continue;
					ret.add(topic);
				}
				autoCompleteComparator.setName(str);
				Collections.sort(ret, autoCompleteComparator);
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
		listScroller.setPreferredSize(new Dimension(350, 200));
		getContentPane().add(listScroller, BorderLayout.WEST);

		// Double click opens the indicated example
		list.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent evt) {
				if (evt.getClickCount() > 1) {
					int ind = list.locationToIndex(evt.getPoint());
					if (ind == -1)
						return;
					openExample(exampleList.get(ind) + ".cfg");
					exampleSearch.setText("");
				}
			}
		});

		// Enter key opens the selected example
		list.getInputMap().put(KeyStroke.getKeyStroke("ENTER"), "enter");
		list.getActionMap().put("enter", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				int ind = list.getSelectedIndex();
				openExample(exampleList.get(ind) + ".cfg");
				exampleSearch.setText("");
			}
		});

		// Example preview
		previewLabel = new JLabel("", JLabel.CENTER);
		previewLabel.setBorder(new EmptyBorder(5, 5, 5, 5));
		getContentPane().add(previewLabel, BorderLayout.CENTER);

		// Open button
		JButton openButton = new JButton("Open");
		openButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				openExample(presentExample + ".cfg");
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

		// Focus on the list and select the first example
		list.requestFocusInWindow();
		list.setSelectedIndex(0);
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

	public static ArrayList<String> getExampleList() {
		ArrayList<String> ret = new ArrayList<>();
		for (String name : GUIFrame.getResourceFileNames(EXAMPLES_FOLDER_NAME)) {
			if (name.endsWith(".cfg")) {
				ret.add(name.substring(0, name.length() - 4));
			}
		}
		for (String subfolderName : GUIFrame.getResourceSubfolderNames(EXAMPLES_FOLDER_NAME)) {
			String folderName = EXAMPLES_FOLDER_NAME + "/" + subfolderName;
			for (String name : GUIFrame.getResourceFileNames(folderName)) {
				if (name.endsWith(".cfg")) {
					String topic = subfolderName + "/" + name.substring(0, name.length() - 4);
					ret.add(topic);
				}
			}
		}
		Collections.sort(ret, Input.uiSortOrder);
		return ret;
	}

	/**
	 * Launches the Help tool and searches for topics that contain the specified string.
	 * @param str - string to search
	 */
	public void search(String str) {
		if (str == null || str.isEmpty())
			return;
		this.setVisible(true);
		exampleSearch.search(str);
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

	private boolean showTopic(String topic) {
		try {
			URL url = GUIFrame.class.getResource(EXAMPLES_FOLDER_NAME + "/" + topic + ".cfg");
			if (url == null)
				return false;
			presentExample = topic;
			int ind = exampleList.indexOf(topic);
			list.setSelectedIndex(ind);
			list.ensureIndexIsVisible(ind);

			// Clear the old preview image
			previewLabel.setIcon(null);

			// Get the preview image
			Future<BufferedImage> fi = getPreview(topic);
			fi.blockUntilDone();
			if (fi.failed()) {
				System.out.println(fi.getFailureMessage());
				return false; // Something went wrong...
			}

			// Display the image
			previewIcon.setImage(fi.get());
			previewLabel.setIcon(previewIcon);
			return true;
		}
		catch (Throwable t) {
			return false;
		}
	}

	public Future<BufferedImage> getPreview(String example) {
		synchronized (imageCache) {

			// Return the cached image if available
			Future<BufferedImage> cached = imageCache.get(example);
			if (cached != null) {
				return cached;
			}

			// Create the new model
			JaamSimModel simModel = new JaamSimModel(example + ".cfg");
			simModel.autoLoad();
			InputAgent.readResource(simModel, "<res>/examples/" + example + ".cfg");
			simModel.postLoad();

			// Add labels and sub-models
			Simulation simulation = simModel.getSimulation();
			if (simulation.isShowLabels())
				simModel.showTemporaryLabels();

			// Get the View to render
			View view = null;
			for (View v : simModel.getInstanceIterator(View.class)) {
				view = v;
				break;
			}

			// Render the view offscreen
			if (view == null || !RenderManager.isGood())
				return null;
			CameraInfo camInfo = view.getCameraInfo();
			Future<BufferedImage> fi = RenderManager.inst().renderOffscreen(simModel, 0.0d, camInfo, 640, 480);

			// Save and return the image
			imageCache.put(example, fi);
			return fi;
		}
	}

	private void openExample(String name) {

		// Create the new simulation model
		JaamSimModel simModel = new JaamSimModel(name);
		simModel.autoLoad();
		GUIFrame gui = GUIFrame.getInstance();

		// Add the run manager
		RunManager runMgr = new RunManager(simModel);
		simModel.setConfiguring(true);
		// Set the Control Panel to the new JaamSimModel and reset the user interface
		GUIFrame.setRunManager(runMgr);

		// Load the specified model file
		InputAgent.readResource(simModel, "<res>/examples/" + name);
		simModel.postLoad();
		simModel.setConfiguring(false);
		gui.updateForSimulationState();

		// A RecordEdits marker in the example file must be ignored
		simModel.setRecordEditsFound(false);

		// Add labels and sub-models
		Simulation simulation = simModel.getSimulation();
		if (simulation.isShowLabels())
			simModel.showTemporaryLabels();

		// Display the new model
		FrameBox.setSelectedEntity(simulation, false);

		// Bring the new model to front
		GUIFrame.getInstance().setVisible(true);
	}

}
