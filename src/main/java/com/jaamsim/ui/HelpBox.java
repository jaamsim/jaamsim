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
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.plaf.basic.BasicArrowButton;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;

import com.jaamsim.input.Input;

public class HelpBox extends JDialog {

	private String presentTopic;
	private final ArrayList<String> topicList = new ArrayList<>();
	private JList<String> list;
	private final JEditorPane editorPane = new JEditorPane();
	private final JTextField topicSearch;
	private final ArrayList<String> prevTopics = new ArrayList<>();  // previous topics viewed
	private final Dimension itemSize;
	private ScrollablePopupMenu topicMenu;

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
		topicSearch = new JTextField("", 30);
		topicSearch.setToolTipText(GUIFrame.formatToolTip("Topic",
				"Title of the help topic to find."));

		// Recent topics
		JButton dropdown = new BasicArrowButton(BasicArrowButton.SOUTH) {
	        @Override
			public Dimension getPreferredSize() {
	            return new Dimension(16, topicSearch.getPreferredSize().height);
	        }
		};
		dropdown.setToolTipText(GUIFrame.formatToolTip("Previous Topics",
				"Help topics that have been found previously."));

		JPanel textPanel = new JPanel();
		textPanel.setLayout( new FlowLayout(FlowLayout.CENTER, 0, 0) );
		textPanel.add(new JLabel("Find Topic:"));
		textPanel.add(Box.createRigidArea(new Dimension(5, 5)));
		textPanel.add(topicSearch);
		textPanel.add(dropdown);
		textPanel.setBorder(new EmptyBorder(10, 5, 5, 5));
		getContentPane().add(textPanel, BorderLayout.NORTH);

		itemSize = topicSearch.getPreferredSize();
		itemSize.width += dropdown.getPreferredSize().width;

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

		// Dropdown button pressed
		dropdown.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				topicMenu = new ScrollablePopupMenu();
				for (final String topic : prevTopics) {
					JMenuItem item = new JMenuItem(topic);
					item.setPreferredSize(itemSize);
					item.addActionListener( new ActionListener() {

						@Override
						public void actionPerformed( ActionEvent event ) {
							topicMenu = null;
							showAndSaveTopic(topic);
						}
					} );
					topicMenu.add(item);
				}
				topicMenu.show(topicSearch, 0, topicSearch.getHeight());
			}
		});

		// Return pressed - pick the first topic that contains the string
		topicSearch.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String str = topicSearch.getText().trim();
				for (String topic : topicList) {
					if (!topic.toUpperCase().contains(str.toUpperCase()))
						continue;
					showAndSaveTopic(topic);
					return;
				}
			}
		});

		// Down arrow pressed
		topicSearch.addKeyListener(new KeyListener() {
			@Override
			public void keyTyped(KeyEvent e) {}

			@Override
			public void keyPressed(KeyEvent e) {
				int keyCode = e.getKeyCode();
				if (keyCode == KeyEvent.VK_DOWN && topicMenu != null) {
					topicMenu.setVisible(false);
					String name = topicSearch.getText().trim();
					showTopicMenu(name, true);

					// Set the pop-up menu to the second item on the list
					SwingUtilities.invokeLater(new Runnable() {
						@Override
						public void run() {
							topicMenu.dispatchEvent(new KeyEvent(topicMenu, KeyEvent.KEY_PRESSED, 0, 0, KeyEvent.VK_DOWN, '\0'));
						}
					});
				}
			}

			@Override
			public void keyReleased(KeyEvent e) {}
		});

		// Listen for changes to the text
		topicSearch.getDocument().addDocumentListener(new DocumentListener() {

			@Override
			public void insertUpdate(DocumentEvent e) {
				if (e.getLength() != 1)  // single character entered
					return;
				String topic = topicSearch.getText().trim();
				myInstance.showTopicMenu(topic, false);
			}

			@Override
			public void removeUpdate(DocumentEvent e) {
				String topic = topicSearch.getText().trim();
				myInstance.showTopicMenu(topic, false);
			}

			@Override
			public void changedUpdate(DocumentEvent e) {}
	    });
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

	public void showDialog(String topic) {
		if (topic.isEmpty() && presentTopic == null)
			topic = DEFAULT_TOPIC;
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

	private void showAndSaveTopic(String topic) {
		showTopic(topic);
		topicSearch.setText(topic);
		prevTopics.remove(topic);
		prevTopics.add(0, topic);
	}

	private void showTopicMenu(String str, boolean focusable) {
		if (str.isEmpty()) {
			if (topicMenu != null) {
				topicMenu.setVisible(false);
				topicMenu = null;
			}
			return;
		}

		// List the topics that contain the string
		ArrayList<String> shortList = new ArrayList<>();
		for (String topic: topicList) {
			if (!topic.toUpperCase().contains(str.toUpperCase()))
				continue;
			shortList.add(topic);
		}

		// Build the menu of matching topics
		topicMenu = new ScrollablePopupMenu();
		boolean first = true;
		for (final String topic : shortList) {
			JMenuItem item = new JMenuItem(topic);
			item.setPreferredSize(itemSize);
			item.addActionListener( new ActionListener() {
				@Override
				public void actionPerformed( ActionEvent event ) {
					topicMenu = null;
					showAndSaveTopic(topic);
				}
			} );
			topicMenu.add(item);
			if (first && !focusable) {
				item.setArmed(true);
				first = false;
			}
		}
		if (!focusable)
			topicMenu.setFocusable(false);
		topicMenu.show(topicSearch, 0, topicSearch.getHeight());
	}

}
