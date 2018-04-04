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

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.filechooser.FileNameExtensionFilter;

import com.jaamsim.input.FileInput;
import com.jaamsim.input.InputAgent;

/**
 * Handles file inputs.
 *
 */
public class FileEditor extends CellEditor
implements ActionListener {

	private final JPanel jPanel;
	private final JTextField text;
	private final JButton fileButton;
	private FileInput fileInput;
	private static File lastDir;  // last directory accessed by the file chooser

	public FileEditor(JTable table) {
		super(table);

		jPanel = new JPanel(new BorderLayout());

		text = new JTextField();
		jPanel.add(text, BorderLayout.WEST);

		fileButton = new JButton(new ImageIcon(
			GUIFrame.class.getResource("/resources/images/dropdown.png")));
		fileButton.addActionListener(this);
		fileButton.setActionCommand("button");

		jPanel.add(fileButton, BorderLayout.EAST);
	}

	public void setFileInput(FileInput in) {
		fileInput = in;
	}

	@Override
	public String getValue() {
		return text.getText();
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if("button".equals(e.getActionCommand())) {

			// Create a file chooser
			if (lastDir == null)
				lastDir = InputAgent.getConfigFile();
			JFileChooser fileChooser = new JFileChooser(lastDir);

			// Set the file extension filters
			FileNameExtensionFilter[] filters = fileInput.getFileNameExtensionFilters();
			if (filters.length > 0) {
				// Turn off the "All Files" filter
				fileChooser.setAcceptAllFileFilterUsed(false);
				// Include a separate filter for each extension
				for (FileNameExtensionFilter f : filters) {
					fileChooser.addChoosableFileFilter(f);
				}
			}

			// Show the file chooser and wait for selection
			int returnVal = fileChooser.showDialog(null, "Load");

			// Process the selected file
			if (returnVal == JFileChooser.APPROVE_OPTION) {
	            File file = fileChooser.getSelectedFile();
				lastDir = fileChooser.getCurrentDirectory();
				text.setText(file.getPath());
	        }

			// Apply editing
			stopCellEditing();

			// Focus the cell
			propTable.requestFocusInWindow();
		}
	}

	@Override
	public Component getTableCellEditorComponent(JTable table,
			Object value, boolean isSelected, int row, int column) {

		setTableInfo(table, row, column);

		// set the value
		input = (FileInput)value;
		text.setText( input.getValueString() );

		// right size for jPanel and its components
		Dimension dim = new Dimension(
			  table.getColumnModel().getColumn( EditBox.VALUE_COLUMN ).getWidth() -
			  table.getColumnModel().getColumnMargin(),
			  table.getRowHeight());
		jPanel.setPreferredSize(dim);
		dim = new Dimension(dim.width - (dim.height), dim.height);
		text.setPreferredSize(dim);
		dim = new Dimension(dim.height, dim.height);
		fileButton.setPreferredSize(dim);

		return jPanel;
	}
}
