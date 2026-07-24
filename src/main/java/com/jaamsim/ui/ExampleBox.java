/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2020-2026 JaamSim Software Inc.
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.border.EmptyBorder;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import com.jaamsim.Graphics.View;
import com.jaamsim.basicsim.JaamSimModel;
import com.jaamsim.basicsim.ObjectType;
import com.jaamsim.basicsim.RunManager;
import com.jaamsim.basicsim.Simulation;
import com.jaamsim.controllers.CameraControl;
import com.jaamsim.controllers.RenderManager;
import com.jaamsim.input.Input;
import com.jaamsim.input.InputAgent;
import com.jaamsim.render.CameraInfo;
import com.jaamsim.render.Future;

public class ExampleBox extends JDialog {

	private String presentExample;
	private static final LinkedHashMap<String, ExampleModel> examplesMap = new LinkedHashMap<>();
	private final SearchField exampleSearch;
	private final AutoCompleteComparator autoCompleteComparator = new AutoCompleteComparator();

	private final DefaultMutableTreeNode top;
	private final DefaultTreeModel treeModel;
	private final JTree tree;
	private final JScrollPane treeScroller;

	private final JLabel previewLabel;
	private final ImageIcon previewIcon = new ImageIcon();

	private final HashMap<String, Future<BufferedImage>> imageCache = new HashMap<>();

	private static ExampleBox myInstance;

	private static final String DIALOG_NAME = "Examples - JaamSim";
	private static final String DEFAULT_TOPIC = "Factory Example";
	private static final String EXAMPLES_FOLDER_NAME = "/resources/examples";

	private ExampleBox() {
		super((JDialog)null, DIALOG_NAME, false);
		setIconImages(GUIFrame.getWindowIcons());
		setResizable(true);
		setDefaultCloseOperation(HIDE_ON_CLOSE);

		getContentPane().setLayout( new BorderLayout() );
		setMinimumSize(new Dimension(300, 300));
		setPreferredSize(new Dimension(1100, 800));

		// Example Map
		populateExampleMap();

		// Example search
		exampleSearch = new SearchField(50) {
			@Override
			public boolean showTopic(String topic) {
				return ExampleBox.this.showTopic(topic);
			}
			@Override
			public ArrayList<String> getTopicList(String str) {
				ArrayList<String> ret = new ArrayList<>();
				for (String topic : getExampleList()) {
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
		top = new DefaultMutableTreeNode();
		createNodes(top);
		treeModel = new DefaultTreeModel(top);
		tree = new JTree(top);
		tree.setModel(treeModel);
		tree.getSelectionModel().setSelectionMode( TreeSelectionModel.SINGLE_TREE_SELECTION );
		tree.setRootVisible(false);
		tree.setShowsRootHandles(true);

		treeScroller = new JScrollPane(tree);
		treeScroller.setBorder(new EmptyBorder(5, 5, 5, 0));
		treeScroller.setPreferredSize(new Dimension(400, 200));
		getContentPane().add(treeScroller, BorderLayout.WEST);

		tree.addTreeSelectionListener( new TreeSelectionListener() {
			@Override
			public void valueChanged(TreeSelectionEvent e) {
				String topicName = getSelectedTopic();
				showTopic(topicName);
				exampleSearch.setText("");
			}
		});

		// Double click opens the indicated example
		tree.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent evt) {
				if (evt.getClickCount() > 1) {
					String topicName = getSelectedTopic();
					openExample(topicName);
					exampleSearch.setText("");
				}
			}
		});

		// Enter key opens the selected example
		tree.getInputMap().put(KeyStroke.getKeyStroke("ENTER"), "enter");
		tree.getActionMap().put("enter", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String topicName = getSelectedTopic();
				openExample(topicName);
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
				openExample(presentExample);
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
		tree.requestFocusInWindow();
		showTopic(DEFAULT_TOPIC);
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

	private static class ExampleModel {
		final String modelName;
		final ArrayList<String> subfolderNames;
		final ArrayList<String> subfolderLabels;

		public ExampleModel(String name, ArrayList<String> folders) {
			modelName = name;
			subfolderNames = folders;
			subfolderLabels = new ArrayList<String>(folders.size());
			for (String folderName : folders) {
				String folderLabel = folderName.replaceAll("_", " ");
				subfolderLabels.add(folderLabel);
			}
		}

		@Override
		public String toString() {
			return modelName;
		}
	}

	private String getFilePath(String topic) {
		ExampleModel ex = examplesMap.get(topic);
		if (ex == null)
			return null;
		StringBuilder sb = new StringBuilder();
		sb.append("<res>/examples/");
		for (String subfolderName : ex.subfolderNames) {
			sb.append(subfolderName).append("/");
		}
		sb.append(ex.modelName).append(".cfg");
		return sb.toString();
	}

	public static ArrayList<String> getExampleList() {
		if (examplesMap.isEmpty())
			populateExampleMap();
		return new ArrayList<String>(examplesMap.keySet());
	}

	public static void populateExampleMap() {
		ArrayList<ExampleModel> list = new ArrayList<>();

		// Models in the examples folder
		for (String name : GUIFrame.getResourceFileNames(EXAMPLES_FOLDER_NAME)) {
			if (name.endsWith(".cfg")) {
				String modelName = name.substring(0, name.length() - 4);
				ArrayList<String> folders = new ArrayList<String>(0);
				list.add(new ExampleModel(modelName, folders));
			}
		}

		// Models in subfolders
		for (String subfolderName : GUIFrame.getResourceSubfolderNames(EXAMPLES_FOLDER_NAME)) {
			String folderName = EXAMPLES_FOLDER_NAME + "/" + subfolderName;
			for (String name : GUIFrame.getResourceFileNames(folderName)) {
				if (name.endsWith(".cfg")) {
					String modelName = name.substring(0, name.length() - 4);
					ArrayList<String> folders = new ArrayList<String>(1);
					folders.add(subfolderName);
					list.add(new ExampleModel(modelName, folders));
				}
			}
		}

		// Sort the models alphabetically by model name
		Collections.sort(list, Input.uiSortOrder);

		// Add the models to the examples hashmap
		for (ExampleModel ex : list) {
			examplesMap.put(ex.modelName, ex);
		}
	}

	private void createNodes(DefaultMutableTreeNode top) {

		// List of library names
		ArrayList<String> libraryNames = new ArrayList<>();
		JaamSimModel simModel = GUIFrame.getJaamSimModel();
		for (ObjectType ot : simModel.getClonesOfIterator(ObjectType.class)) {
			String name = ot.getLibraryName();
			if (libraryNames.contains(name))
				continue;
			libraryNames.add(name);
		}

		// List of subfolder names
		ArrayList<String> subfolderList = new ArrayList<>();
		for (Map.Entry<String, ExampleModel> entry : examplesMap.entrySet()) {
			ArrayList<String> subfolderLabels = entry.getValue().subfolderLabels;
			if (subfolderLabels.isEmpty() || subfolderList.contains(subfolderLabels.get(0)))
				continue;
			subfolderList.add(subfolderLabels.get(0));
		}

		// Sort the subfolder list into the same sequence as the library names
		Collections.sort(subfolderList, new Comparator<String>() {
			@Override
			public int compare(String str1, String str2) {
				int ind1 = libraryNames.indexOf(str1);
				int ind2 = libraryNames.indexOf(str2);
				if (ind1 == -1) ind1 = Integer.MAX_VALUE;
				if (ind2 == -1) ind2 = Integer.MAX_VALUE;
				return Integer.compare(ind1, ind2);
			}
		});

		// Folders of topics
		for (String subfolderLabel : subfolderList) {

			// Add a node for the folder
			DefaultMutableTreeNode folder = new DefaultMutableTreeNode(subfolderLabel);
			top.add(folder);

			// Add a node for each model in the folder
			for (Map.Entry<String, ExampleModel> entry : examplesMap.entrySet()) {
				ArrayList<String> subfolderLabels = entry.getValue().subfolderLabels;
				if (!subfolderLabels.isEmpty() && subfolderLabels.get(0).equals(subfolderLabel)) {
					DefaultMutableTreeNode topic = new DefaultMutableTreeNode(entry.getValue().modelName);
					folder.add(topic);
				}
			}
		}

		// Individual topics
		for (Map.Entry<String, ExampleModel> entry : examplesMap.entrySet()) {
			if (entry.getValue().subfolderLabels.isEmpty()) {
				DefaultMutableTreeNode topic = new DefaultMutableTreeNode(entry.getValue().modelName);
				top.add(topic);
			}
		}
	}

	private String getSelectedTopic() {
		String ret = "";
		DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
		if (node != null && node.getUserObject() instanceof String) {
			ret = (String) node.getUserObject();
		}
		return ret;
	}

	private TreePath getPathToTopic(String topic, DefaultMutableTreeNode root) {
		ExampleModel ex = examplesMap.get(topic);
		if (ex == null)
			return null;

		ArrayList<DefaultMutableTreeNode> nodeList = new ArrayList<>();
		nodeList.add(root);
		DefaultMutableTreeNode node = root;
		for (String subfolderLabel : ex.subfolderLabels) {
			node = ObjectSelector.getNodeFor_In(subfolderLabel, node);
			if (node == null)
				return null;
			nodeList.add(node);
		}
		node = ObjectSelector.getNodeFor_In(ex.modelName, node);
		if (node == null)
			return null;
		nodeList.add(node);
		return new TreePath(nodeList.toArray());
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
		ArrayList<String> exampleList = getExampleList();

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
			if (examplesMap.get(topic) == null)
				return false;
			presentExample = topic;
			TreePath path = getPathToTopic(topic, top);
			if (path != null) {
				tree.setSelectionPath(path);
				tree.scrollPathToVisible(path);
			}

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

			// Find the example model
			String filePath = getFilePath(example);
			if (filePath == null)
				return null;

			// Create the new model
			JaamSimModel simModel = new JaamSimModel(example + ".cfg");
			simModel.autoLoad();
			InputAgent.readResource(simModel, filePath);
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
			double simTime = 0.0d;
			CameraInfo camInfo = CameraControl.getCameraInfo(view, simTime);
			Future<BufferedImage> fi = RenderManager.inst().renderOffscreen(simModel, simTime, camInfo, 640, 480);

			// Save and return the image
			imageCache.put(example, fi);
			return fi;
		}
	}

	private void openExample(String topic) {

		// Find the example model
		String filePath = getFilePath(topic);
		if (filePath == null)
			return;

		// Create the new simulation model
		JaamSimModel simModel = new JaamSimModel(topic + ".cfg");
		simModel.autoLoad();
		GUIFrame gui = GUIFrame.getInstance();

		// Add the run manager
		RunManager runMgr = new RunManager(simModel);
		simModel.setConfiguring(true);
		// Set the Control Panel to the new JaamSimModel and reset the user interface
		GUIFrame.setRunManager(runMgr);

		// Load the specified model file
		InputAgent.readResource(simModel, filePath);
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
