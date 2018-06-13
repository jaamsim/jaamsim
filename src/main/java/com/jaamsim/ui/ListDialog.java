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

import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;

public class ListDialog extends JDialog {

	private ArrayList<String> selectedList;
	private int result;

	public static final int CANCEL_OPTION = 1;  // Cancel button is clicked
	public static final int APPROVE_OPTION = 0; // Accept button is clicked
	public static final int ERROR_OPTION = -1;  // Error occurs or the dialog is dismissed

	public ListDialog(Frame owner, String title, boolean modal,
			ArrayList<String> optionList, ArrayList<String> initList) {
		super(owner, title, modal);
		selectedList = new ArrayList<>();
		setSize(190, 300);

		// Build a list of checkboxes for the options
		final DefaultListModel<JCheckBox> listModel = new DefaultListModel<>();
		listModel.clear();
		for (String str: optionList) {
			JCheckBox checkBox = new JCheckBox(str);
			listModel.addElement(checkBox);
			checkBox.setSelected(initList.contains(str));
		}

		JList<JCheckBox> list = new JList<>(listModel);
		list.setCellRenderer(new ListRenderer());
		list.addMouseListener(new CheckBoxMouseAdapter());

		// Add the checkbox list to the dialog
		JScrollPane jScroll = new JScrollPane(list);
		getContentPane().add(jScroll);

		// Accept button
		JButton acceptButton = new JButton("Accept");
		acceptButton.addActionListener( new ActionListener() {
			@Override
			public void actionPerformed( ActionEvent e ) {
				result = APPROVE_OPTION;
				setVisible(false);
				for (int i = 0; i < listModel.getSize(); i++) {
					if (!listModel.getElementAt(i).isSelected())
						continue;
					String str = listModel.getElementAt(i).getText();
					selectedList.add(str);
				}
			}
		});

		// Cancel button
		JButton cancelButton = new JButton("Cancel");
		cancelButton.addActionListener( new ActionListener() {
			@Override
			public void actionPerformed( ActionEvent e ) {
				result = CANCEL_OPTION;
				setVisible(false);
			}
		});

		// Add the buttons to the dialog
		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout( new FlowLayout(FlowLayout.CENTER) );
		buttonPanel.add(acceptButton);
		buttonPanel.add(cancelButton);
		getContentPane().add("South", buttonPanel);

		// Window closed event
		this.addWindowListener( new WindowAdapter() {
			@Override
			public void windowClosing( WindowEvent e ) {
				result = ERROR_OPTION;
				setVisible(false);
				dispose();
			}
		} );

		setIconImage(GUIFrame.getWindowIcon());
		setAlwaysOnTop(true);
	}

	public int showDialog() {

		// Show the dialog box and wait for editing to finish
		this.setVisible(true);

		// Return how the editing was completed
		return result;
	}

	public ArrayList<String> getList() {
		return selectedList;
	}

	/*
	 * renderer for the JList so it shows its items as JCheckBoxes
	 */
	public static class ListRenderer implements ListCellRenderer<JCheckBox> {
		private JCheckBox checkBox;
		@Override
		public Component getListCellRendererComponent(JList<? extends JCheckBox> list, JCheckBox value,
				int index, boolean isSelected, boolean cellHasFocus) {
			checkBox = value;
			if (isSelected) {
				checkBox.setBackground(list.getSelectionBackground());
				checkBox.setForeground(list.getSelectionForeground());
			}
			else {
				checkBox.setBackground(list.getBackground());
				checkBox.setForeground(list.getForeground());
			}
			return checkBox;
		}
	}

	/*
	 * pressing mouse in the JList should select/unselect JCheckBox
	 */
	public static class CheckBoxMouseAdapter extends MouseAdapter {
		private int i;
		private Object obj;
		@Override
		public void mousePressed(MouseEvent e) {
			i = ((JList<?>)e.getSource()).locationToIndex(e.getPoint());
			if(i == -1)
				return;

			obj = ((JList<?>)e.getSource()).getModel().getElementAt(i);
			if (obj instanceof JCheckBox) {
				JCheckBox checkbox = (JCheckBox) obj;
				checkbox.setSelected(!checkbox.isSelected());
				((JList<?>)e.getSource()).repaint();
			}
		}
	}

}
