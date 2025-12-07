/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2005-2013 Ausenco Engineering Canada Inc.
 * Copyright (C) 2016-2024 JaamSim Software Inc.
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
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.net.URI;

import javax.swing.AbstractAction;
import javax.swing.AbstractCellEditor;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
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
import com.jaamsim.input.ParseContext;
import com.jaamsim.ui.EditBox.EditTable;

public abstract class CellEditor extends AbstractCellEditor implements TableCellEditor, ActionListener {
	protected Input<?> input;

	private final JPanel jPanel;
	private final JTextField text;
	private final JButton button;

	private int row;
	private int col;
	private EditTable table;
	protected String retryString;

	public CellEditor(int width, int height, boolean showButton) {
		this.addCellEditorListener(new CellListener());

		// Table cell
		jPanel = new JPanel(new BorderLayout());
		jPanel.setPreferredSize(new Dimension(width, height));

		// Editable text
		text = new JTextField();
		jPanel.add(text, BorderLayout.WEST);

		// If in retry mode, edit the cell value immediately
		jPanel.addFocusListener(new FocusListener() {
			@Override
			public void focusGained(FocusEvent e) {
				if (retryString == null)
					return;
				text.requestFocusInWindow();
			}
			@Override
			public void focusLost(FocusEvent e) {}
		});

		// Accept an input change if the focus is lost
		text.addFocusListener(new FocusListener() {
			@Override
			public void focusGained(FocusEvent e) {}
			@Override
			public void focusLost(FocusEvent e) {
				fireEditingStopped();
			}
		});

		// If text is entered, over-write the present value
		jPanel.addKeyListener(new KeyListener() {
			@Override
			public void keyTyped(KeyEvent e) {}
			@Override
			public void keyPressed(KeyEvent e) {
				// Alphanumeric key
				char keyChar = e.getKeyChar();
				if (keyChar != KeyEvent.CHAR_UNDEFINED && !Character.isISOControl(keyChar)
						&& !e.isControlDown() && !e.isAltDown()) {
					text.requestFocusInWindow();
					SwingUtilities.invokeLater(new Runnable() {
						@Override
						public void run() {
							text.setText(String.valueOf(keyChar));
						}
					});
					return;
				}
			}
			@Override
			public void keyReleased(KeyEvent e) {}
		});

		// Delete
		jPanel.getInputMap().put(KeyStroke.getKeyStroke("DELETE"), "delete");
		jPanel.getActionMap().put("delete", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				text.setText("");
				fireEditingStopped();
			}
		});

		// Backspace
		jPanel.getInputMap().put(KeyStroke.getKeyStroke("BACK_SPACE"), "backspace");
		jPanel.getActionMap().put("backspace", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				text.setText("");
				text.requestFocusInWindow();
			}
		});

		// Copy
		jPanel.getInputMap().put(KeyStroke.getKeyStroke("control C"), "copy");
		jPanel.getActionMap().put("copy", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				GUIFrame.copyToClipboard(text.getText());
			}
		});

		// Paste
		jPanel.getInputMap().put(KeyStroke.getKeyStroke("control V"), "paste");
		jPanel.getActionMap().put("paste", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String str = GUIFrame.getStringFromClipboard();
				if (str == null)
					return;
				text.setText(str);
				fireEditingStopped();
			}
		});

		// Cut
		jPanel.getInputMap().put(KeyStroke.getKeyStroke("control X"), "cut");
		jPanel.getActionMap().put("cut", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				GUIFrame.copyToClipboard(text.getText());
				text.setText("");
				fireEditingStopped();
			}
		});

		// Undo
		jPanel.getInputMap().put(KeyStroke.getKeyStroke("control Z"), "undo");
		jPanel.getActionMap().put("undo", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				GUIFrame.getInstance().invokeUndo();
			}
		});

		// Redo
		jPanel.getInputMap().put(KeyStroke.getKeyStroke("control Y"), "redo");
		jPanel.getActionMap().put("redo", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				GUIFrame.getInstance().invokeRedo();
			}
		});

		// Find
		jPanel.getInputMap().put(KeyStroke.getKeyStroke("control F"), "find");
		jPanel.getActionMap().put("find", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				FindBox.getInstance().search(text.getText());
			}
		});

		// Help
		jPanel.getInputMap().put(KeyStroke.getKeyStroke("F1"), "help");
		jPanel.getActionMap().put("help", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String topic = "";
				Entity ent = table.getEntity();
				if (ent != null) {
					topic = ent.getObjectType().getName();
					Entity inpEnt = ent.getJaamSimModel().getNamedEntity(text.getText());
					if (inpEnt != null) {
						topic = inpEnt.getObjectType().getName();
					}
				}
				HelpBox.getInstance().showDialog(topic);
			}
		});

		// Edit
		jPanel.getInputMap().put(KeyStroke.getKeyStroke("F2"), "edit");
		jPanel.getActionMap().put("edit", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				text.requestFocusInWindow();
			}
		});

		// Escape
		jPanel.getInputMap().put(KeyStroke.getKeyStroke("ESCAPE"), "escape");
		jPanel.getActionMap().put("escape", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {}
		});

		// Escape while editing
		text.getInputMap().put(KeyStroke.getKeyStroke("ESCAPE"), "escape");
		text.getActionMap().put("escape", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Input<?> in = (Input<?>) getCellEditorValue();
				text.setText(in.getValueString());
				table.requestFocusInWindow();
			}
		});

		// Find while editing
		text.getInputMap().put(KeyStroke.getKeyStroke("control F"), "find");
		text.getActionMap().put("find", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				FindBox.getInstance().search(text.getSelectedText());
			}
		});

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

		// Context menu
		text.addMouseListener(new MyMouseListener());
	}

	class MyMouseListener implements MouseListener {
		private final JPopupMenu menu= new JPopupMenu();
		@Override
		public void mouseClicked(MouseEvent e) {}
		@Override
		public void mouseEntered(MouseEvent e) {}
		@Override
		public void mouseExited(MouseEvent e) {}
		@Override
		public void mousePressed(MouseEvent e) {}

		@Override
		public void mouseReleased(MouseEvent e) {
			// Show context menu for right-click
			if (e.getButton() != MouseEvent.BUTTON3)
				return;
			String str = text.getText();
			if (str == null)
				return;
			menu.removeAll();

			// Cut
			JMenuItem cutMenuItem = new JMenuItem("Cut");
			cutMenuItem.setAccelerator(KeyStroke.getKeyStroke(
			        KeyEvent.VK_X, ActionEvent.CTRL_MASK));
			cutMenuItem.addActionListener( new ActionListener() {
				@Override
				public void actionPerformed( ActionEvent event ) {
					GUIFrame.copyToClipboard(str);
					text.setText("");
					fireEditingStopped();
				}
			} );
			menu.add(cutMenuItem);

			// Copy
			JMenuItem copyMenuItem = new JMenuItem("Copy");
			copyMenuItem.setIcon( new ImageIcon(
					GUIFrame.class.getResource("/resources/images/Copy-16.png")) );
			copyMenuItem.setAccelerator(KeyStroke.getKeyStroke(
			        KeyEvent.VK_C, ActionEvent.CTRL_MASK));
			copyMenuItem.addActionListener( new ActionListener() {
				@Override
				public void actionPerformed( ActionEvent event ) {
					GUIFrame.copyToClipboard(str);
				}
			} );
			menu.add(copyMenuItem);

			// Paste
			JMenuItem pasteMenuItem = new JMenuItem("Paste");
			pasteMenuItem.setIcon( new ImageIcon(
					GUIFrame.class.getResource("/resources/images/Paste-16.png")) );
			pasteMenuItem.setAccelerator(KeyStroke.getKeyStroke(
			        KeyEvent.VK_V, ActionEvent.CTRL_MASK));
			pasteMenuItem.addActionListener( new ActionListener() {
				@Override
				public void actionPerformed( ActionEvent event ) {
					String str = GUIFrame.getStringFromClipboard();
					if (str == null)
						return;
					text.setText(str);
					fireEditingStopped();
				}
			} );
			menu.add(pasteMenuItem);

			// Delete
			JMenuItem deleteMenuItem = new JMenuItem("Delete");
			deleteMenuItem.setAccelerator(KeyStroke.getKeyStroke(
			        KeyEvent.VK_DELETE, 0));
			deleteMenuItem.addActionListener( new ActionListener() {
				@Override
				public void actionPerformed( ActionEvent event ) {
					text.setText("");
					fireEditingStopped();
				}
			} );
			menu.add( deleteMenuItem );
			menu.addSeparator();

			// Find
			JMenuItem findMenuItem = new JMenuItem("Find");
			findMenuItem.setIcon( new ImageIcon(
					GUIFrame.class.getResource("/resources/images/Find-16.png")) );
			findMenuItem.setAccelerator(KeyStroke.getKeyStroke(
			        KeyEvent.VK_F, ActionEvent.CTRL_MASK));
			findMenuItem.addActionListener( new ActionListener() {
				@Override
				public void actionPerformed( ActionEvent event ) {
					FindBox.getInstance().search(str);
				}
			} );
			menu.add(findMenuItem);
			menu.show(e.getComponent(), e.getX(), e.getY());
		}
	}

	@Override
	public Object getCellEditorValue() {
		return input;
	}

	public void setValue(String str) {
		text.setText(str.replace('\n', ' '));
		table.selectNextCell();
	}

	public String getValue() {
		return text.getText();
	}

	public boolean canRetry() {
		return false;
	}

	public void updateValue() {
		if (text.hasFocus() || retryString != null)
			return;
		text.setText(input.getValueString());
	}

	protected void launchExpressionBox() {

		// Use the input from the Input Editor if it has been changed already,
		// otherwise use the input's value which includes any newline characters
		String str = input.getValueString();
		if (!str.replace('\n', ' ').equals(getValue()))
			str = getValue();

		// Launch the dialog box and wait for editing to finish
		ExpressionBox expDialog = ExpressionBox.getInstance(input, str);
		int result = expDialog.showDialog();

		// Return the new expression
		if (result == ExpressionBox.APPROVE_OPTION) {
			setValue(expDialog.getInputString());
		}
		else {
			// Reset the original value
			setValue(str);
		}

		// Apply editing
		stopCellEditing();
	}

	protected void setInputValue(String str) {

		// Adjust the user's entry to standardise the syntax
		str = str.trim();
		if (!str.isEmpty())
			str = input.applyConditioning(str);

		// New value is the same as the inherited value
		if (input.getInheritedValueString().replace('\n', ' ').equals(str))
			str = "";

		// The value has not changed
		if (input.getValueString().replace('\n', ' ').equals(str) && input.isValid()) {
			return;
		}

		// Parse the keyword inputs
		ParseContext pc = null;
		if (GUIFrame.getJaamSimModel().getConfigFile() != null) {
			URI configDirURI = GUIFrame.getJaamSimModel().getConfigFile().getParentFile().toURI();
			pc = new ParseContext(configDirURI, null);
		}
		Entity ent = table.getEntity();
		KeywordIndex kw = InputAgent.formatInput(input.getKeyword(), str, pc);
		ent.getJaamSimModel().storeAndExecute(new KeywordCommand(ent, kw));
		input.setValid(true);
	}

	@Override
	public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {

		setTableInfo(table, row, column);

		// set the value
		input = (Input<?>)value;
		String val = input.getValueString();
		if (canRetry() && retryString != null) {
			val = retryString;
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
		private boolean isErrorDialogShown = false;

		@Override
		public void editingCanceled(ChangeEvent evt) {}

		@Override
		public void editingStopped(ChangeEvent evt) {
			if (isErrorDialogShown || ExpressionBox.isInstanceVisible())
				return;

			CellEditor editor = (CellEditor)evt.getSource();
			Entity ent = editor.getTable().getEntity();
			final String newValue = editor.getValue();
			try {
				editor.setInputValue(newValue);
			}
			catch (InputErrorException exep) {
				boolean entityChanged = (EditBox.getInstance().getCurrentEntity() !=  ent);

				// Reset the Input Editor to the original tab
				final EditTable table = editor.getTable();
				if (!entityChanged) {
					EditBox.getInstance().setTab(table.getTab());
				}

				if (editor.canRetry() && !entityChanged) {
					isErrorDialogShown = true;  // trap call to editingStopped on loss of focus
					boolean editSelected = GUIFrame.showErrorEditDialog("Input Error",
							exep.source,
							exep.position,
							"Input error:",
							exep.getMessage(),
							"Do you want to continue editing, or reset the input?");
					isErrorDialogShown = false;
					if (editSelected) {
						// Any editor that supports retry should implement the following
						final int row = editor.getRow();
						final int col = editor.getCol();
						SwingUtilities.invokeLater(new Runnable() {
							@Override
							public void run() {
								table.setRetry(newValue, row, col);
							}
						});

					} else {
						GUIFrame.updateUI();
					}
					return;
				}

				isErrorDialogShown = true;  // trap call to editingStopped on loss of focus
				GUIFrame.showErrorDialog("Input Error",
						exep.source,
						exep.position,
						"Input error:",
						exep.getMessage(),
						"Value will be cleared.");
				isErrorDialogShown = false;

				GUIFrame.updateUI();
				return;
			}
			finally {
				editor.getTable().setPresentCellEditor(null);
				editor.getTable().requestFocusInWindow();
			}
		}
	}

}
