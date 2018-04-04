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
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;

import com.jaamsim.input.ColourInput;
import com.jaamsim.math.Color4d;

/**
 * Handles colour inputs.
 *
 */
public class ColorEditor extends CellEditor
implements ActionListener {

	private final JPanel jPanel;
	private final JTextField text;
	private final JButton colorButton;
	private JColorChooser colorChooser;
	private JDialog dialog;

	public ColorEditor(JTable table) {
		super(table);

		jPanel = new JPanel(new BorderLayout());

		text = new JTextField();
		jPanel.add(text, BorderLayout.WEST);

		colorButton = new JButton(new ImageIcon(
			GUIFrame.class.getResource("/resources/images/dropdown.png")));
		colorButton.addActionListener(this);
		colorButton.setActionCommand("button");
		colorButton.setContentAreaFilled(false);
		jPanel.add(colorButton, BorderLayout.EAST);
	}

	@Override
	public String getValue() {
		return text.getText();
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if("button".equals(e.getActionCommand())) {
			if(colorChooser == null || dialog == null) {
				colorChooser = new JColorChooser();
				dialog = JColorChooser.createDialog(jPanel,
						"Pick a Color",
						true,  //modal
						colorChooser,
						this,  //OK button listener
						null); //no CANCEL button listener
				dialog.setIconImage(GUIFrame.getWindowIcon());
				dialog.setAlwaysOnTop(true);
			}

			Color4d col = ((ColourInput)input).getValue();
			colorChooser.setColor(new Color((float)col.r, (float)col.g, (float)col.b, (float)col.a));
			dialog.setLocationRelativeTo((Component)e.getSource());
			dialog.setVisible(true);

			// Apply editing
			stopCellEditing();

			// Focus the cell
			propTable.requestFocusInWindow();
		}
		else {
			Color color = colorChooser.getColor();
			if (color.getAlpha() == 255) {
				text.setText( String.format("%d %d %d",
						color.getRed(), color.getGreen(), color.getBlue() ) );
				return;
			}
			text.setText( String.format("%d %d %d %d",
					 color.getRed(),color.getGreen(), color.getBlue(), color.getAlpha() ) );
		}
	}

	@Override
	public Component getTableCellEditorComponent(JTable table,
			Object value, boolean isSelected, int row, int column) {

		setTableInfo(table, row, column);

		// set the value
		input = (ColourInput)value;
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
		colorButton.setPreferredSize(dim);

		return jPanel;
	}
}
