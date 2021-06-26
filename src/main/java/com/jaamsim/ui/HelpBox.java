/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2020-2021 JaamSim Software Inc.
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
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;

import com.jaamsim.input.Input;

public class HelpBox extends JDialog {

	private String presentTopic;
	private final ArrayList<String> topicList = new ArrayList<>();
	private JList<String> list;
	private final JEditorPane editorPane = new JEditorPane();
	private final SearchField topicSearch;

	private static HelpBox myInstance;

	private static final String DIALOG_NAME = "Help - JaamSim";
	private static final String DEFAULT_TOPIC = "Mouse Actions";

	public HelpBox() {
		super((JDialog)null, DIALOG_NAME, false);
		setIconImages(GUIFrame.getWindowIcons());
		setResizable(true);
		setDefaultCloseOperation(HIDE_ON_CLOSE);

		getContentPane().setLayout( new BorderLayout() );
		setMinimumSize(new Dimension(300, 300));
		setPreferredSize(new Dimension(1000, 800));

		// Topics List
		for (String name : GUIFrame.getResourceFileNames("/resources/help")) {
			if (name.endsWith(".htm")) {
				topicList.add(name.substring(0, name.length() - 4));
			}
		}
		Collections.sort(topicList, Input.uiSortOrder);

		// Topics search
		topicSearch = new SearchField(30) {
			@Override
			public boolean showTopic(String topic) {
				HelpBox.this.showTopic(topic);
				return true;
			}
			@Override
			public ArrayList<String> getTopicList(String str) {
				ArrayList<String> ret = new ArrayList<>();
				for (String topic : topicList) {
					if (!topic.toUpperCase().contains(str.toUpperCase()))
						continue;
					ret.add(topic);
				}
				return ret;
			}
		};
		topicSearch.setToolTipText(GUIFrame.formatToolTip("Topic",
				"Title of the help topic to find."));

		// Examples button
		JButton examplesButton = new JButton("Examples");
		examplesButton.setToolTipText(GUIFrame.formatToolTip("Examples",
				"Show example models related to this topic."));
		examplesButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (presentTopic == null)
					return;
				ExampleBox.getInstance().search(presentTopic);
			}
		});

		JPanel textPanel = new JPanel();
		textPanel.setLayout( new FlowLayout(FlowLayout.CENTER, 0, 0) );
		textPanel.add(new JLabel("Find Topic:"));
		textPanel.add(Box.createRigidArea(new Dimension(5, 5)));
		textPanel.add(topicSearch);
		textPanel.add(Box.createRigidArea(new Dimension(20, 5)));
		textPanel.add(examplesButton);
		textPanel.setBorder(new EmptyBorder(10, 5, 5, 5));
		getContentPane().add(textPanel, BorderLayout.NORTH);

		// Topic selector
		String[] topics = new String[topicList.size()];
		topics = topicList.toArray(topics);
		list = new JList<>(topics);
		list.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
		list.addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {
				int ind = list.getSelectedIndex();
				if (ind == -1)
					return;
				showTopic(topicList.get(ind));
				topicSearch.setText("");
			}
		});
		JScrollPane listScroller = new JScrollPane(list);
		listScroller.setBorder(new EmptyBorder(5, 5, 5, 0));
		listScroller.setPreferredSize(new Dimension(250, 200));
		getContentPane().add(listScroller, BorderLayout.WEST);

		// Help text
		editorPane.setContentType("text/html");
		editorPane.setEditable(false);
		//editorPane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);

		// Set the text styles
		HTMLEditorKit editorKit = new HTMLEditorKit();
		editorPane.setEditorKit(editorKit);
		StyleSheet styleSheet = editorKit.getStyleSheet();
		styleSheet.addRule("body {font-family: sans-serif; font-size: 10px;}");
		styleSheet.addRule("h1 {font-size: 16px; color: blue; margin-top: 10; margin-bottom: 2;}");
		styleSheet.addRule("h2 {font-size: 14px; color: blue; margin-top: 10; margin-bottom: 2;}");
		styleSheet.addRule("h3 {font-size: 12px; color: blue; margin-top: 10; margin-bottom: 2;}");
		styleSheet.addRule("h4 {font-size: 10px; color: blue; margin-top: 10; margin-bottom: 2;}");
		styleSheet.addRule(".Main {margin-top: 2; margin-bottom: 10;}");
		styleSheet.addRule(".KeywordChar {color: blue;}");
		styleSheet.addRule(".MsoCaption {font-weight: bold; margin-top: 2; margin-bottom: 2;}");
		styleSheet.addRule(".TableText-Paragraph {font-size: 9px;}");
		styleSheet.addRule(".code {font-family: monospace; font-size: 9px; margin-top: 0; margin-bottom: 10;}");
		styleSheet.addRule(".codeintable {font-family: monospace; font-size: 9px;}");
		styleSheet.addRule(".List-Bullet1 {margin-top: 2; margin-bottom: 10;}");

		JScrollPane scrollPane = new JScrollPane(editorPane);
		scrollPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		getContentPane().add(scrollPane, BorderLayout.CENTER);

		// Close button
		JButton closeButton = new JButton("Close");
		closeButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				setVisible(false);
			}
		});

		// Add the button to the dialog
		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout( new FlowLayout(FlowLayout.CENTER) );
		buttonPanel.add(closeButton);
		getContentPane().add("South", buttonPanel);
		pack();

		// Set initial position in middle of screen
		setLocationRelativeTo(null);
	}

	public synchronized static HelpBox getInstance() {
		if (myInstance == null)
			myInstance = new HelpBox();
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
			if (presentTopic == null)
				topic = DEFAULT_TOPIC;
		}

		// Exact match to a topic
		else if (topicList.contains(str)) {
			topic = str;
		}

		// First topic that contains the string
		else {
			for (String tpc : topicList) {
				if (tpc.toUpperCase().contains(str.toUpperCase())) {
					topic = tpc;
					break;
				}
			}
		}

		// Display the selected topic
		showTopic(topic);
		topicSearch.setText("");
		this.setVisible(true);
	}

	private void showTopic(String topic) {
		try {
			URL url = GUIFrame.class.getResource("/resources/help/" + topic + ".htm");
			if (url == null)
				return;
			editorPane.setPage(url);
			presentTopic = topic;
			int ind = topicList.indexOf(topic);
			list.setSelectedIndex(ind);
			list.ensureIndexIsVisible(ind);
		}
		catch (Throwable t) {}
	}

}
