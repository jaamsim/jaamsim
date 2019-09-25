/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2005-2013 Ausenco Engineering Canada Inc.
 * Copyright (C) 2016-2019 JaamSim Software Inc.
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
import java.awt.event.ActionListener;

import javax.swing.AbstractCellEditor;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.plaf.basic.BasicArrowButton;
import javax.swing.table.TableCellEditor;

import com.jaamsim.Commands.KeywordCommand;
import com.jaamsim.basicsim.Entity;
import com.jaamsim.input.Input;
import com.jaamsim.input.InputAgent;
import com.jaamsim.input.InputErrorException;
import com.jaamsim.input.KeywordIndex;
import com.jaamsim.ui.EditBox.EditTable;

public abstract class CellEditor extends AbstractCellEditor implements TableCellEditor, ActionListener {
	protected final EditTable propTable;
	protected Input<?> input;

	private final JPanel jPanel;
	private final JTextField text;
	private final JButton button;

	private int row;
	private int col;
	private EditTable table;
	protected String retryString;

	public CellEditor(EditTable table, boolean showButton) {
		propTable = table;
		this.addCellEditorListener(new CellListener());

		// Table cell
		jPanel = new JPanel(new BorderLayout());
		int height = table.getRowHeight();
		int width = table.getColumnModel().getColumn(EditBox.VALUE_COLUMN).getWidth() -
				table.getColumnModel().getColumnMargin();
		jPanel.setPreferredSize(new Dimension(width, height));

		// Editable text
		text = new JTextField();
		jPanel.add(text, BorderLayout.WEST);

		// Dropdown button
		int buttonWidth = 0;
		if (showButton) {
			button = new BasicArrowButton(BasicArrowButton.SOUTH,
					UIManager.getColor("ComboBox.buttonBackground"),  // FIXME does not respect look and feel
					UIManager.getColor("ComboBox.buttonBackground"),  // "ComboBox.buttonShadow"
					UIManager.getColor("ComboBox.buttonDarkShadow"),
					UIManager.getColor("ComboBox.buttonBackground")); // "ComboBox.buttonHighlight"
			button.addActionListener(this);
			button.setActionCommand("button");
			buttonWidth = button.getPreferredSize().width;
			jPanel.add(button, BorderLayout.EAST);
		}
		else {
			button = null;
		}

		text.setPreferredSize(new Dimension(width - buttonWidth, height));
	}

	@Override
	public Object getCellEditorValue() {
		return input;
	}

	public void setValue(String str) {
		text.setText(str);
	}

	public String getValue() {
		return text.getText();
	}

	public boolean canRetry() {
		return false;
	}

	@Override
	public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {

		setTableInfo(table, row, column);

		// set the value
		input = (Input<?>)value;
		String val = input.getValueString();
		if (canRetry() && retryString != null) {
			val = retryString;
			retryString = null;
		}
		text.setText(val);

		return jPanel;
	}

	final public int getRow() { return row; }
	final public int getCol() { return col; }
	final public EditTable getTable() { return table; }

	final public void setRetry(String retryString) {
		this.retryString = retryString;
	}

	protected void setTableInfo(JTable table, int row, int col) {
		this.table = (EditTable) table;
		this.row = row;
		this.col = col;
	}
	
	public static class CellListener implements CellEditorListener {

		@Override
		public void editingCanceled(ChangeEvent evt) {}

		@Override
		public void editingStopped(ChangeEvent evt) {

			CellEditor editor = (CellEditor)evt.getSource();
			Input<?> in = (Input<?>)editor.getCellEditorValue();
			Entity ent = editor.getTable().getEntity();

			final String newValue = editor.getValue();

			// The value has not changed
			if (in.getValueString().equals(newValue) && in.isValid()) {
				editor.propTable.setPresentCellEditor(null);
				return;
			}

			// Adjust the user's entry to standardise the syntax
			String str = newValue.trim();
			if (!str.isEmpty())
				str = in.applyConditioning(str);

			try {
				// Parse the keyword inputs
				KeywordIndex kw = InputAgent.formatInput(in.getKeyword(), str);
				InputAgent.storeAndExecute(new KeywordCommand(ent, kw));
				in.setValid(true);
			}
			catch (InputErrorException exep) {
				boolean entityChanged = (EditBox.getInstance().getCurrentEntity() !=  ent);

				// Reset the Input Editor to the original tab
				final EditTable table = editor.getTable();
				if (!entityChanged) {
					EditBox.getInstance().setTab(table.getTab());
				}

				if (editor.canRetry() && !entityChanged) {
					boolean editSelected = GUIFrame.showErrorEditDialog("Input Error",
							exep.source,
							exep.position,
							"Input error:",
							exep.getMessage(),
							"Do you want to continue editing, or reset the input?");
					if (editSelected) {
						// Any editor that supports retry should implement the following
						final int row = editor.getRow();
						final int col = editor.getCol();
						SwingUtilities.invokeLater(new Runnable() {
							@Override
							public void run() {
								table.setRetry(newValue, row, col);
								table.editCellAt(row, col);
							}
						});

					} else {
						GUIFrame.updateUI();
					}
					return;
				}

				GUIFrame.showErrorDialog("Input Error",
						exep.source,
						exep.position,
						"Input error:",
						exep.getMessage(),
						"Value will be cleared.");

				GUIFrame.updateUI();
				return;
			}
			finally {
				editor.propTable.setPresentCellEditor(null);
			}
		}
	}

}
