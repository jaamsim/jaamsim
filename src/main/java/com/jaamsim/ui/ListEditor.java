/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2005-2013 Ausenco Engineering Canada Inc.
 * Copyright (C) 2016-2018 JaamSim Software Inc.
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
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListCellRenderer;

import com.jaamsim.input.Parser;

/**
 * Handles inputs where a list of entities can be selected.
 *
 */
public class ListEditor extends ChooserEditor {

	private JDialog dialog;
	private JScrollPane jScroll;
	private JList<JCheckBox> list;

	private ArrayList<String> tokens;
	private DefaultListModel<JCheckBox> listModel;
	private CheckBoxMouseAdapter checkBoxMouseAdapter;
	private int i;
	private boolean caseSensitive;
	private boolean innerBraces;

	public ListEditor(JTable table) {
		super(table, true);
		caseSensitive = true;
		innerBraces = false;
	}

	@Override
	public void actionPerformed(ActionEvent e) {

		// Accept button
		if ("Accept".equals(e.getActionCommand())) {
			dialog.setVisible(false);
			StringBuilder sb = new StringBuilder();
			for(int i = 0; i < list.getModel().getSize(); i++) {
				if(!list.getModel().getElementAt(i).isSelected())
					continue;
				String str = list.getModel().getElementAt(i).getText();
				if (innerBraces)
					sb.append("{ ").append(str).append(" } ");
				else
					sb.append(str).append(" ");
			}
			setValue(sb.toString());
		}

		// Cancel button
		if ("Cancel".equals(e.getActionCommand())) {
			dialog.setVisible(false);
		}

		if (!"button".equals(e.getActionCommand())) {
			return;
		}

		if (dialog == null) {
			dialog = new JDialog(EditBox.getInstance(), "Select items",
					true); // model
			dialog.setSize(190, 300);
			jScroll = new JScrollPane(list);
			dialog.getContentPane().add(jScroll); // top of the JDialog

			JButton acceptButton = new JButton("Accept");
			acceptButton.setActionCommand("Accept");
			acceptButton.addActionListener(this);

			JButton cancelButton = new JButton("Cancel");
			cancelButton.setActionCommand("Cancel");
			cancelButton.addActionListener(this);

			JPanel buttonPanel = new JPanel();
			buttonPanel.setLayout( new FlowLayout(FlowLayout.CENTER) );
			buttonPanel.add(acceptButton);
			buttonPanel.add(cancelButton);
			dialog.getContentPane().add("South", buttonPanel);

			dialog.setIconImage(GUIFrame.getWindowIcon());
			dialog.setAlwaysOnTop(true);
			tokens = new ArrayList<>();
		}

		// break the value into single options
		tokens.clear();
		Parser.tokenize(tokens, getValue(), true);
		if( !caseSensitive ) {
			for(i = 0; i < tokens.size(); i++ ) {
				tokens.set(i, tokens.get(i).toUpperCase() );
			}
		}

		// checkmark according to the value input
		for(i = 0; i < list.getModel().getSize(); i++) {
			JCheckBox box = list.getModel().getElementAt(i);
			box.setSelected(tokens.contains(box.getText()));
		}
		dialog.setLocationRelativeTo((Component)e.getSource());
		dialog.setVisible(true);

		// Apply editing
		stopCellEditing();

		// Focus the cell
		propTable.requestFocusInWindow();
	}

	// Set the items in the list
	public void setListData(ArrayList<String> aList) {
		if(list == null) {
			listModel = new DefaultListModel<>();
			list = new JList<>(listModel);

			// render items as JCheckBox and make clicking work for them
			list.setCellRenderer( new ListRenderer() );
			checkBoxMouseAdapter = new CheckBoxMouseAdapter();
			list.addMouseListener(checkBoxMouseAdapter);
		}
		listModel.clear();
		for(String each: aList) {
			JCheckBox checkBox = new JCheckBox(each);
			listModel.addElement(checkBox);
		}
	}

	public void setCaseSensitive(boolean bool) {
		caseSensitive = bool;
	}

	public void setInnerBraces(boolean bool) {
		innerBraces = bool;
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
