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

import javax.swing.AbstractCellEditor;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.table.TableCellEditor;

import com.jaamsim.Commands.KeywordCommand;
import com.jaamsim.basicsim.Entity;
import com.jaamsim.input.Input;
import com.jaamsim.input.InputAgent;
import com.jaamsim.input.InputErrorException;
import com.jaamsim.input.KeywordIndex;
import com.jaamsim.ui.EditBox.EditTable;

public abstract class CellEditor extends AbstractCellEditor implements TableCellEditor {
	protected final EditTable propTable;
	protected Input<?> input;

	private int row;
	private int col;
	private EditTable table;
	protected String retryString;

	public CellEditor(EditTable table) {
		propTable = table;
		this.addCellEditorListener(new CellListener());
	}

	@Override
	public Object getCellEditorValue() {
		return input;
	}

	public String getValue() {
		return "";
	}

	public boolean canRetry() {
		return false;
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
							"Input error:",
							exep,
							"Do you want to continue editing, or reset the input?");
					if (editSelected) {
						// Any editor that supports retry should implement the following
						final int row = editor.getRow();
						final int col = editor.getCol();
						final Input<?> inp = in;
						SwingUtilities.invokeLater(new Runnable() {
							@Override
							public void run() {
								boolean bool = inp.isValid();
								inp.setValid(true); //FIXME required for DropDownMenuEditor
								table.setRetry(newValue, row, col);
								table.editCellAt(row, col);
								inp.setValid(bool);
							}
						});

					} else {
						GUIFrame.updateUI();
					}
					return;
				}

				GUIFrame.showErrorDialog("Input Error",
						"Input error:",
						exep,
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
