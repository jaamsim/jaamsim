/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2018-2020 JaamSim Software Inc.
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
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Collections;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import com.jaamsim.Commands.KeywordCommand;
import com.jaamsim.Graphics.DisplayEntity;
import com.jaamsim.Graphics.EntityLabel;
import com.jaamsim.Graphics.OverlayEntity;
import com.jaamsim.Graphics.Region;
import com.jaamsim.basicsim.Entity;
import com.jaamsim.basicsim.JaamSimModel;
import com.jaamsim.input.ExpEvaluator;
import com.jaamsim.input.ExpParser;
import com.jaamsim.input.ExpResType;
import com.jaamsim.input.ExpResult;
import com.jaamsim.input.Input;
import com.jaamsim.input.InputAgent;
import com.jaamsim.input.KeywordIndex;
import com.jaamsim.input.OutputHandle;
import com.jaamsim.input.Parser;
import com.jaamsim.input.ExpParser.Expression;
import com.jaamsim.units.Unit;

public class ExpressionBox extends JDialog {

	private final Input<?> input;
	private final JTextArea editArea;
	private final JTextField msgText;
	private final JButton acceptButton;
	private final JButton cancelButton;
	private int result;

	private int editMode;
	private static JPopupMenu entityMenu;
	private static JPopupMenu outputMenu;

	private static final int EDIT_MODE_NORMAL = 0;
	private static final int EDIT_MODE_ENTITY = 1;
	private static final int EDIT_MODE_OUTPUT = 2;

	private static final char[] controlChars = {' ', '.', ',', ';', '(', ')', '{', '}', '[', ']', '"', '\'', '#', '\t', '\n'};
	private static final char[] mathChars = { '+', '-', '*', '/', '^', '%', '?', '=', '>', '<', '!', '&', '|'};

	public static final int CANCEL_OPTION = 1;  // Cancel button is clicked
	public static final int APPROVE_OPTION = 0; // Accept button is clicked
	public static final int ERROR_OPTION = -1;  // Error occurs or the dialog is dismissed

	public static final String DIALOG_NAME = "Input Builder";

	public ExpressionBox(Input<?> in, String str) {
		super((JDialog)null, DIALOG_NAME, true);

		getContentPane().setLayout( new BorderLayout() );
		setMinimumSize(new Dimension(900, 300));
		setIconImage(GUIFrame.getWindowIcon());

		// Initial text
		input = in;
		ArrayList<String> tokens = new ArrayList<>();
		Parser.tokenize(tokens, str, true);

		// Input text
		editArea = new JTextArea();
		editArea.setText(Input.getValueString(tokens, true));
		JScrollPane scrollPane = new JScrollPane(editArea);
		scrollPane.setBorder(new EmptyBorder(5, 10, 0, 10));
		getContentPane().add(scrollPane, BorderLayout.CENTER);

		// Button bar
		JToolBar buttonBar = new JToolBar();
		buttonBar.setMargin(new Insets( 1, 1, 1, 1 ));
		buttonBar.setFloatable(false);
		buttonBar.setLayout( new FlowLayout( FlowLayout.LEFT, 0, 0 ) );
		addToolBarButtons(buttonBar);
		getContentPane().add( buttonBar, BorderLayout.NORTH );

		// Result value or error message
		msgText = new JTextField("", 80);
		msgText.setEditable(false);
		msgText.setToolTipText(GUIFrame.formatToolTip("Result",
				"The value returned by a valid input or "
				+ "the error message returned an invalid input."));
		int msgHeight = msgText.getPreferredSize().height;

		// Entity and keyword
		Entity ent = EditBox.getInstance().getCurrentEntity();
		JLabel keyText = new JLabel(String.format("%s - %s", ent, input.getKeyword()));
		keyText.setToolTipText(EditBox.getInputDesc(ent, input));
		keyText.setPreferredSize(new Dimension(keyText.getPreferredSize().width, msgHeight));

		JPanel leftPanel = new JPanel();
		leftPanel.setLayout( new BorderLayout() );
		leftPanel.add(keyText, BorderLayout.NORTH);
		leftPanel.add(msgText, BorderLayout.SOUTH);

		// Buttons
		acceptButton = new JButton("Accept");
		cancelButton = new JButton("Cancel");

		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout( new FlowLayout(FlowLayout.CENTER) );
		buttonPanel.add(acceptButton);
		buttonPanel.add(cancelButton);

		JPanel rightPanel = new JPanel();
		rightPanel.setLayout( new BorderLayout());
		rightPanel.add(Box.createRigidArea(new Dimension(40, msgHeight)), BorderLayout.NORTH);
		rightPanel.add(buttonPanel, BorderLayout.SOUTH);

		// Combined panel with entity, keyword, result, and buttons
		JPanel southPanel = new JPanel();
		southPanel.setLayout( new FlowLayout(FlowLayout.LEFT) );
		southPanel.add(leftPanel);
		southPanel.add(Box.createRigidArea(new Dimension(10, msgHeight)));
		southPanel.add(rightPanel);
		southPanel.setBorder(new EmptyBorder(0, 10, 0, 0));
		getContentPane().add(southPanel, BorderLayout.SOUTH);

		pack();
		editArea.requestFocusInWindow();
		setEditMode(EDIT_MODE_NORMAL);

		// Set the result value or error message for the present input
		tryParse();

		// Window closed event
		this.addWindowListener( new WindowAdapter() {
			@Override
			public void windowClosing( WindowEvent e ) {
				result = ERROR_OPTION;
				setVisible(false);
				undoEdits();
				dispose();
			}
		} );

		// Accept button
		acceptButton.addActionListener( new ActionListener() {
			@Override
			public void actionPerformed( ActionEvent e ) {
				result = APPROVE_OPTION;
				setVisible(false);
				dispose();
			}
		} );

		// Cancel button
		cancelButton.addActionListener( new ActionListener() {
			@Override
			public void actionPerformed( ActionEvent e ) {
				result = CANCEL_OPTION;
				setVisible(false);
				undoEdits();
				dispose();
			}
		} );

		// Listen for changes to the text
		editArea.getDocument().addDocumentListener(new DocumentListener() {

			@Override
			public void insertUpdate(DocumentEvent e) {
				tryParse();

				if (e.getLength() > 1) {
					setEditMode(EDIT_MODE_NORMAL);
					return;
				}

				// Set or terminate the entity/output name selection state
				char c = editArea.getText().charAt(e.getOffset());
				if (c == '[') {
					setEditMode(EDIT_MODE_ENTITY);
				}
				else if (c == '.') {
					setEditMode(EDIT_MODE_OUTPUT);
				}

				// Show the pop-up menus for entity/output selection
				showMenus(e.getOffset());
			}

			@Override
			public void removeUpdate(DocumentEvent e) {
				tryParse();
				if (e.getLength() > 1) {
					setEditMode(EDIT_MODE_NORMAL);
					return;
				}

				// FIXME Bugs in getCaretPosition and getOffset methods:
				// getCaretPosition and e.getOffset work differently depending on whether
				// the Delete key or Backspace key is pressed. For Delete, both methods return
				// the correct value. For Backspace, the two methods return different incorrect
				// values: getOffset returns the 1 + correct value, while getCaretPosition returns
				// 2 + correct value.
				int offset = e.getOffset();
				if (offset != editArea.getCaretPosition())
					offset = offset - 1;
				offset = Math.min(offset, editArea.getText().length() - 1);

				// Show the pop-up menus for entity/output selection
				showMenus(offset);
			}

			@Override
			public void changedUpdate(DocumentEvent e) {
				setEditMode(EDIT_MODE_NORMAL);
			}
	    });
	}

	private void setEditMode(int mode) {
		editMode = mode;
		if (mode != EDIT_MODE_ENTITY && entityMenu != null) {
			entityMenu.setVisible(false);
			entityMenu = null;
		}
		if (mode != EDIT_MODE_OUTPUT && outputMenu != null) {
			outputMenu.setVisible(false);
			outputMenu = null;
		}
	}

	private void tryParse() {

		// Prepare the input string
		Entity ent = EditBox.getInstance().getCurrentEntity();
		String str = editArea.getText().replace("\n", " ");
		if (!str.isEmpty())
			str = input.applyConditioning(str);

		// Load the input
		try {
			KeywordIndex kw = InputAgent.formatInput(input.getKeyword(), str);
			InputAgent.storeAndExecute(new KeywordCommand(ent, kw));
			acceptButton.setEnabled(true);
		}
		catch (Exception e) {
			msgText.setText(formatMessage(false, e.getMessage()));
			acceptButton.setEnabled(false);
			return;
		}

		// Show the present value
		String valStr;
		try {
			double simTime = GUIFrame.getJaamSimModel().getSimTime();
			valStr = input.getPresentValueString(simTime);
		}
		catch (Exception e) {
			valStr = "Cannot evaluate at this time";
		}
		msgText.setText(formatMessage(true, valStr));
	}

	private static String formatMessage(boolean isValid, String str) {
		if (isValid)
			return "PRESENT VALUE: " + str;
		else
			return "ERROR: " + str;
	}

	private void undoEdits() {
		Entity ent = EditBox.getInstance().getCurrentEntity();
		String keyword = input.getKeyword();
		GUIFrame.getInstance().undo(ent, keyword);
	}

	public int showDialog() {

		// Position the editor at the centre of the screen
		Rectangle winSize = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
		setLocation((winSize.width - getWidth())/2, (winSize.height - getHeight())/2);

		// Show the dialog box and wait for editing to finish
		this.setVisible(true);

		// Return how the editing was completed
		return result;
	}

	public String getInputString() {
		return editArea.getText();
	}

	private void addToolBarButtons(JToolBar buttonBar) {

	    Dimension separatorDim = new Dimension(11, 20);
	    Dimension gapDim = new Dimension(5, separatorDim.height);

		// Single quotes and curly braces
		int width = 20;
		buttonBar.add(Box.createRigidArea(gapDim));
		addButtons(buttonBar, initObjects, width, editArea);

		// Object buttons
		width = 35;
		buttonBar.addSeparator(separatorDim);
		addButtons(buttonBar, simObjects, width, editArea);
		buttonBar.add( new EntityButton("Entity", width, editArea) );
		addButtons(buttonBar, expObjects, width, editArea);

		// Unit objects
		buttonBar.addSeparator(separatorDim);
		buttonBar.add( new UnitButton("Unit", width, editArea) );
		buttonBar.add( new UnitTypeButton("Type", width, editArea) );

		// Function button
		buttonBar.addSeparator(separatorDim);
		buttonBar.add( new FunctionButton("Function", width, editArea) );

		// Operator buttons
		width = 20;
		buttonBar.addSeparator(separatorDim);
		addButtons(buttonBar, basicOperators, width, editArea);

		buttonBar.addSeparator(separatorDim);
		addButtons(buttonBar, logicalOperators, width, editArea);
		buttonBar.add(Box.createRigidArea(gapDim));
	}

	public void addButtons(JToolBar buttonBar, ArrayList<ButtonDesc> bdList, int w, JTextArea text) {
		for (ButtonDesc bd : bdList) {
			buttonBar.add( new ExpBuilderButton(bd, w, text) );
		}
	}

	public static class ExpBuilderButton extends JButton {

		private ExpBuilderButton(final ButtonDesc bd, int w, final JTextArea text) {
			super(bd.symbol);
			setMargin(new Insets( 0, 0, 0, 0 ));
			int width = Math.max(w, getPreferredSize().width);
			int height = getPreferredSize().height;
			setPreferredSize(new Dimension(width, height));
			setToolTipText(GUIFrame.formatKeywordToolTip(
					null,
					bd.title,
					bd.description,
					bd.arguments,
					bd.examples) );
			addActionListener( new ActionListener() {

				@Override
				public void actionPerformed( ActionEvent event ) {
					text.insert(bd.insert, text.getCaretPosition());
					text.setCaretPosition(text.getCaretPosition() + bd.insertPos);
					text.requestFocusInWindow();
				}
			} );
		}

	}

	public static class EntityButton extends JButton {

		private EntityButton(String name, int w, final JTextArea text) {
			super(name);
			setMargin(new Insets( 0, 0, 0, 0 ));
			int width = Math.max(w, getPreferredSize().width);
			final int height = getPreferredSize().height;
			setPreferredSize(new Dimension(width, height));
			setToolTipText(GUIFrame.formatKeywordToolTip(
					(String)null,
					"Named Entity",
					"A named Entity is referenced by enclosing the entity name in square brackets.",
					null,
					"[Entity1]"));
			addActionListener( new ActionListener() {

				@Override
				public void actionPerformed( ActionEvent event ) {
					ScrollablePopupMenu entityMenu = new ScrollablePopupMenu();
					ArrayList<String> entNameList = new ArrayList<>();
					JaamSimModel simModel = GUIFrame.getJaamSimModel();
					for (DisplayEntity each: simModel.getClonesOfIterator(DisplayEntity.class)) {
						if (!each.isRegistered())
							continue;

						if (each instanceof OverlayEntity || each instanceof Region
								|| each instanceof EntityLabel)
							continue;

						entNameList.add(each.getName());
					}
					Collections.sort(entNameList, Input.uiSortOrder);

					for (final String entName : entNameList) {
						JMenuItem item = new JMenuItem(entName);
						item.addActionListener( new ActionListener() {

							@Override
							public void actionPerformed( ActionEvent event ) {
								String str = String.format("[%s]", entName);
								text.insert(str, text.getCaretPosition());
								text.requestFocusInWindow();
							}
						} );
						entityMenu.add(item);
					}
					entityMenu.show(EntityButton.this, 0, height);
				}
			} );
		}

	}

	public static class UnitButton extends JButton {

		private UnitButton(String name, int w, final JTextArea text) {
			super(name);
			setMargin(new Insets( 0, 0, 0, 0 ));
			int width = Math.max(w, getPreferredSize().width);
			final int height = getPreferredSize().height;
			setPreferredSize(new Dimension(width, height));
			setToolTipText(GUIFrame.formatKeywordToolTip(
					null,
					"Unit",
					"Units are assigned to a number by following it with the unit name enclosed by "
							+ "square brackets. Units are grouped by the type of unit, such as "
							+ "TimeUnit and DistanceUnit.",
					null,
					"[s] indicates the units of seconds.",
					"[m] indicates the units of metres."));
			addActionListener( new ActionListener() {

				@Override
				public void actionPerformed( ActionEvent event ) {
					ScrollablePopupMenu unitMenu = new ScrollablePopupMenu();

					// Loop through the unit types that have been defined
					for (String utName : Unit.getUnitTypeList(GUIFrame.getJaamSimModel())) {
						final Class<? extends Unit> ut = Input.parseUnitType(GUIFrame.getJaamSimModel(), utName);
						ArrayList<? extends Unit> unitList = Unit.getUnitList(GUIFrame.getJaamSimModel(), ut);
						if (unitList.isEmpty())
							continue;

						// For each unit type create a sub-menu of units from which to select
						JMenu subMenu = new JMenu(utName);
						for (final Unit u : unitList) {
							JMenuItem item = new JMenuItem(u.getName());
							item.addActionListener( new ActionListener() {

								@Override
								public void actionPerformed( ActionEvent event ) {
									String str = String.format("[%s]", u.getName());
									text.insert(str, text.getCaretPosition());
									text.requestFocusInWindow();
								}
							} );
							subMenu.add(item);
						}
						unitMenu.add(subMenu);
					}
					unitMenu.show(UnitButton.this, 0, height);
				}
			} );
		}

	}

	public static class UnitTypeButton extends JButton {

		private UnitTypeButton(String name, int w, final JTextArea text) {
			super(name);
			setMargin(new Insets( 0, 0, 0, 0 ));
			int width = Math.max(w, getPreferredSize().width);
			final int height = getPreferredSize().height;
			setPreferredSize(new Dimension(width, height));
			setToolTipText(GUIFrame.formatKeywordToolTip(
					null,
					"Unit Type",
					"The unit type is required for attribute and custom output definitions.",
					null,
					"TimeUnit indicates a quantity with the units of time.",
					"DistanceUnit indicates a quantity with the units of distance.",
					"DimensionlessUnit indicates a quantity that is a pure number."));
			addActionListener( new ActionListener() {

				@Override
				public void actionPerformed( ActionEvent event ) {
					ScrollablePopupMenu unitTypeMenu = new ScrollablePopupMenu();

					// Loop through the unit types that have been defined
					for (String utName : Unit.getUnitTypeList(GUIFrame.getJaamSimModel())) {
						final Class<? extends Unit> ut = Input.parseUnitType(GUIFrame.getJaamSimModel(), utName);
						JMenuItem item = new JMenuItem(ut.getSimpleName());
						item.addActionListener( new ActionListener() {

							@Override
							public void actionPerformed( ActionEvent event ) {
								String str = String.format(" %s ", ut.getSimpleName());
								text.insert(str, text.getCaretPosition());
								text.requestFocusInWindow();
							}
						} );
						unitTypeMenu.add(item);
					}
					unitTypeMenu.show(UnitTypeButton.this, 0, height);
				}
			});
		}

	}

	public static class FunctionButton extends JButton {

		private FunctionButton(String name, int w, final JTextArea text) {
			super(name);
			setMargin(new Insets( 0, 0, 0, 0 ));
			int width = Math.max(w, getPreferredSize().width);
			final int height = getPreferredSize().height;
			setPreferredSize(new Dimension(width, height));
			setToolTipText(GUIFrame.formatKeywordToolTip(
					null,
					"Functions",
					"Various functions provided with JaamSim.",
					null));
			addActionListener( new ActionListener() {

				@Override
				public void actionPerformed( ActionEvent event ) {
					ScrollablePopupMenu funcMenu = new ScrollablePopupMenu();

					// Loop through the unit types that have been defined
					for (final String funcName : ExpParser.getFunctionNames()) {
						JMenuItem item = new JMenuItem(funcName);
						final ButtonDesc bd = getButtonDesc(functions, funcName);
						if (bd != null) {
							item.setToolTipText(GUIFrame.formatKeywordToolTip(
									null,
									bd.title,
									bd.description,
									bd.arguments,
									bd.examples));
						}
						item.addActionListener( new ActionListener() {

							@Override
							public void actionPerformed( ActionEvent event ) {
								String str = funcName + "()";
								int offset = -1;
								if (bd != null) {
									str = bd.insert;
									offset = bd.insertPos;
								}
								text.insert(str, text.getCaretPosition());
								text.setCaretPosition(text.getCaretPosition() + offset);
								text.requestFocusInWindow();
							}
						} );
						funcMenu.add(item);
					}
					funcMenu.show(FunctionButton.this, 0, height);
				}
			});
		}

	}

	private void showMenus(int ind1) {
		String text = editArea.getText();

		if (editMode == EDIT_MODE_ENTITY) {

			// Determine the partial name for the entity
			final int ind0 = text.lastIndexOf('[', ind1);
			if (ind0 == -1) {
				setEditMode(EDIT_MODE_NORMAL);
				return;
			}
			String name = "";
			if (ind1 > ind0) {
				name = text.substring(ind0 + 1, ind1 + 1);
			}

			// Does the name contain any invalid characters?
			if (!name.isEmpty() && !InputAgent.isValidName(name)) {
				setEditMode(EDIT_MODE_NORMAL);
				return;
			}

			// Show the entity name pop-up
			showEntityMenu(name, ind0, ind1);
			return;
		}

		if (editMode == EDIT_MODE_OUTPUT) {

			// Find the entity name
			int dotIndex = text.lastIndexOf('.', ind1);
			Entity ent = getEntityReference(text, dotIndex);

			if (ent == null) {
				setEditMode(EDIT_MODE_NORMAL);
				return;
			}

			// Find the partial output name
			String name = "";
			if (ind1 > dotIndex) {
				name = text.substring(dotIndex + 1, ind1 + 1);
			}

			// Does the name contain any invalid characters?
			for (char c : name.toCharArray()) {
				if (isControlChar(c) || isMathChar(c)) {
					setEditMode(EDIT_MODE_NORMAL);
					return;
				}
			}

			// Show the output name pop-up
			showOutputMenu(ent, name, dotIndex, ind1);
			return;
		}
	}

	private void showEntityMenu(String name, final int ind0, final int ind1) {
		if (entityMenu != null)
			entityMenu.setVisible(false);
		entityMenu = new ScrollablePopupMenu();
		ArrayList<String> nameList = new ArrayList<>();
		JaamSimModel simModel = GUIFrame.getJaamSimModel();
		for (DisplayEntity each: simModel.getClonesOfIterator(DisplayEntity.class)) {
			if (!each.isRegistered())
				continue;

			if (each instanceof OverlayEntity || each instanceof Region
					|| each instanceof EntityLabel)
				continue;

			if (!each.getName().toUpperCase().contains(name.toUpperCase()))
				continue;

			nameList.add(each.getName());
		}
		String simName = GUIFrame.getJaamSimModel().getSimulation().getName();
		if (simName.toUpperCase().contains(name.toUpperCase())) {
			nameList.add(simName);
		}
		Collections.sort(nameList, Input.uiSortOrder);

		for (final String entName : nameList) {
			JMenuItem item = new JMenuItem(entName);
			item.addActionListener( new ActionListener() {

				@Override
				public void actionPerformed( ActionEvent event ) {
					entityMenu = null;
					String str = String.format("[%s]", entName);
					editArea.replaceRange(str, ind0, ind1 + 1);
					editArea.requestFocusInWindow();
					setEditMode(EDIT_MODE_NORMAL);
				}
			} );
			entityMenu.add(item);
		}
		Point p = editArea.getCaret().getMagicCaretPosition();
		if (p == null)
			p = new Point();  // p is null after text is selected and the '[' key is pressed
		int height = editArea.getFontMetrics(editArea.getFont()).getHeight();
		entityMenu.setFocusable(false);
		entityMenu.show(editArea, p.x, p.y + height);
	}

	private void showOutputMenu(Entity ent, String name, final int ind0, final int ind1) {
		if (outputMenu != null)
			outputMenu.setVisible(false);
		outputMenu = new ScrollablePopupMenu();

		// Sub-model components
		ArrayList<String> compList = new ArrayList<>();
		for (Entity comp : ent.getChildren()) {
			if (!comp.isRegistered())
				continue;
			if (!comp.getLocalName().toUpperCase().contains(name.toUpperCase()))
				continue;
			compList.add(comp.getLocalName());
		}
		Collections.sort(compList);

		for (String compName : compList) {
			String str = String.format("[%s]", compName);
			JMenuItem item = new JMenuItem(str);
			item.addActionListener( new ActionListener() {

				@Override
				public void actionPerformed( ActionEvent event ) {
					outputMenu = null;
					editArea.replaceRange(item.getText(), ind0 + 1, ind1 + 1);
					editArea.requestFocusInWindow();
					setEditMode(EDIT_MODE_NORMAL);
				}
			} );
			outputMenu.add(item);
		}

		// Outputs
		ArrayList<OutputHandle> handles = new ArrayList<>();
		for (OutputHandle hand : OutputHandle.getOutputHandleList(ent)) {
			if (hand.getName().contains(" "))
				continue;

			if (!hand.getName().toUpperCase().contains(name.toUpperCase()))
				continue;

			handles.add(hand);
		}
		Collections.sort(handles, Input.uiSortOrder);

		for (final OutputHandle hand : handles) {
			JMenuItem item = new JMenuItem(hand.getName());
			item.setToolTipText(GUIFrame.formatOutputToolTip(
					hand.getName(),
					hand.getDescription()) );
			item.addActionListener( new ActionListener() {

				@Override
				public void actionPerformed( ActionEvent event ) {
					outputMenu = null;
					editArea.replaceRange(hand.getName(), ind0 + 1, ind1 + 1);
					editArea.requestFocusInWindow();
					setEditMode(EDIT_MODE_NORMAL);
				}
			} );
			outputMenu.add(item);
		}

		Point p = editArea.getCaret().getMagicCaretPosition();
		if (p == null)
			p = new Point();
		int height = editArea.getFontMetrics(editArea.getFont()).getHeight();
		outputMenu.setFocusable(false);
		outputMenu.show(editArea, p.x, p.y + height);
	}

	private Entity getEntityReference(String text, int dotIndex) {

		// Find the previous part of the text that might correspond to an entity
		for (int i = dotIndex - 1; i >= 0; i--) {
			String expString = text.substring(i, dotIndex);
			//System.out.println(expString);

			// Try to evaluate the string as an expression that returns an entity
			Entity thisEnt = EditBox.getInstance().getCurrentEntity();
			double simTime = GUIFrame.getJaamSimModel().getSimTime();
			try {
				ExpEvaluator.EntityParseContext pc = ExpEvaluator.getParseContext(thisEnt, expString);
				Expression exp = ExpParser.parseExpression(pc, expString);
				ExpParser.assertResultType(exp, ExpResType.ENTITY);

				ExpResult res = ExpEvaluator.evaluateExpression(exp, simTime);
				//System.out.println(res.entVal);
				return res.entVal;
			}
			catch (Throwable e) {}
		}
		return null;
	}

	public boolean isControlChar(char ch) {
		return containsChar(controlChars, ch);
	}

	public boolean isMathChar(char ch) {
		return containsChar(mathChars, ch);
	}

	private boolean containsChar(char[] chars, char ch) {
		for (char c : chars) {
			if (c == ch)
				return true;
		}
		return false;
	}

	private static class ButtonDesc {

		final String symbol;  // name of the object to be inserted
		final String title;  // title for the pop-up
		final String description;  // description to appear in the pop-up
		final String arguments;  // further description to appear in the pop-up
		final String insert;  // text to insert in the expression when the button is clicked
		final int insertPos;  // change in cursor position after inserting the text
		final String[] examples;  // examples of the object to appear in the pop-up

		public ButtonDesc(String symb, String ttl, String desc, String args, String... examps) {
			this(symb, ttl, desc, args, symb, 0, examps);
		}

		public ButtonDesc(String symb, String ttl, String desc, String args, String ins, int insPos, String... examps) {
			symbol = symb;
			title = ttl;
			description = desc;
			arguments = args;
			insert = ins;
			insertPos = insPos;
			examples = examps;
		}

	}

	public static ButtonDesc getButtonDesc(ArrayList<ButtonDesc> list, String name) {
		for (ButtonDesc bd : list) {
			if (bd.symbol.equals(name)) {
				return bd;
			}
		}
		return null;
	}

	private static ArrayList<ButtonDesc> initObjects = new ArrayList<>();
	private static ArrayList<ButtonDesc> simObjects = new ArrayList<>();
	private static ArrayList<ButtonDesc> expObjects = new ArrayList<>();
	private static ArrayList<ButtonDesc> basicOperators = new ArrayList<>();
	private static ArrayList<ButtonDesc> logicalOperators = new ArrayList<>();
	private static ArrayList<ButtonDesc> functions = new ArrayList<>();

	static {

		// INITIAL MISC. OBJECTS

		initObjects.add(new ButtonDesc("' '", "Single Quotation Marks (' ')",
				"An expression that includes spaces, curly brackets, or double quotes must be enclosed by a pair of single quotes.",
				null,
				"''",
				-1,
				"1[m]+2[m] returns 3[m].",
				"'1[m] + 2[m]' returns 3[m].",
				"'\"abc\"' returns \"abc\"."));

		initObjects.add(new ButtonDesc("{ }", "Curly Braces ('{ }')",
				"Keywords that involve a list of input items, such AttributeDefinitionList, "
						+ "CustomOutputList, and RunOutputList, require that each item is "
						+ "enclosed by curly braces.",
				null,
				"{  }",
				-2,
				"{ attrib1 5 } { attrib2 10[m] } entered to the AttributeDefinitionList keyword "
						+ "defines two attributes attrib1 and attrib2 with initial values of 5 "
						+ "and 10 metres respectively."));

		// SIMULATION OBJECTS

		simObjects.add(new ButtonDesc("this", "Entity 'this'",
				"If an expression is used as the input to an entity, the identifier 'this' can "
						+ "be used in the expression instead of the entity's name.",
				null,
				"this",
				0,
				"'this.Name' returns the name of the entity."));

		simObjects.add(new ButtonDesc("sub", "Entity 'sub'",
				"If an expression is used as the input to an entity, the identifier 'sub' can "
						+ "be used in the expression instead of the name of the sub-model "
						+ "containing the entity. It is equivalent to entering 'this.Parent'.",
				null,
				"sub",
				0,
				"'sub.Name' returns the name of the sub-model containing the entity."));

		simObjects.add(new ButtonDesc("Sim", "Entity 'Simulation'",
				"The Simulation entity is used to store the inputs and outputs related to the "
						+ "simulation run.",
				null,
				"[Simulation]",
				0,
				"'[Simulation].RunNumber' returns the sequence number for the present "
						+ "simulation run."));

		simObjects.add(new ButtonDesc("null", "Entity 'null'",
				"The null entity can be used to test whether an output that returns an object "
						+ "has been set.",
				null,
				"null",
				0,
				"'[Server1].obj != null' returns TRUE (i.e. 1) if the output 'obj' for Server1 "
						+ "has been set."));

		// EXPRESSION OBJECTS

		expObjects.add(new ButtonDesc("String", "String",
				"Strings are enclosed by double quotation marks.",
				"A valid string can include spaces, punctuation, and special characters "
						+ "as required.",
				"\"\"",
				-1,
				"\"The quick red fox.\""));

		expObjects.add(new ButtonDesc("Array", "Array",
				"Arrays are enclosed by curly braces, with individual entries separated by "
						+ "commas. Entries in an array are referenced by specifying an index "
						+ "enclosed by round brackets. The index value can be either a constant "
						+ "or an expression that returns a dimensionless number. A non-integer "
						+ "value for the index will be truncated. Entries in a nested array are "
						+ "referenced by providing multiple indices enclosed by separate pairs of "
						+ "brackets.",
				"A valid array can include any combination of numbers with or without units, "
						+ "strings, entities, arrays, or lambda functions.",
				"{}",
				-1,
				"'{ 5, 6, 7 }(2)' returns 6.",
				"'{ [Entity1], 2[m], \"abc\" }(3)' returns \"abc\".",
				"'{ {5, 6}, {7, 8} }(2)(1)' returns 7"));

		expObjects.add(new ButtonDesc("Map", "Map",
				"A map is similar to an array except that its entries are indexed by a key, "
						+ "such as a string, instead of an integer. The entries in a map can be "
						+ "numbers, strings, entities, arrays, or maps. "
						+ "An entry in a map is referenced by specifying its key "
						+ "(usually a string) enclosed by round brackets.",
				"Maps are enclosed by curly braces with individual entries separated by commas. "
						+ "Each entry consists of a string (the key) followed an equal sign and "
						+ "the value for that key.",
				"{\"a\"=}",
				-1,
				"'{ \"a\"=1, \"b\"=2 }(\"a\")' returns 1.0",
				"'[Server1].StateTimes(\"Idle\")' returns the total time that Server1 has been in "
						+ "the state \"Idle\""));

		expObjects.add(new ButtonDesc("Lambda", "Lambda Function or Functional",
				"A lambda function is an expression that takes one or more input variables and "
						+ "returns a number, string, object, array, map, or another lambda "
						+ "function. Lambda functions can be used with higher-order functions to "
						+ "perform complex calculations that would otherwise require a loop "
						+ "structure.",
				"Input variables are enclosed by bars and separated by commas. The expression "
						+ "that generates the returned value is enclosed by brackets. Input "
						+ "variables can be a number, string, array, map, or another lambda "
						+ "function. The object returned can be any of these same types of "
						+ "objects. A lambda function can be evaluated by providing input values "
						+ "enclosed by brackets.",
				"|x|()",
				-1,
				"|x|(2*x) is a lambda function that returns two times its input value.",
				"|x|(2*x)(3) returns 6.",
				"|x, y|(x + y) is a lambda function that returns the sum of its two input values.",
				"|x, y|(x + y)(1, 2) returns 3"));

		expObjects.add(new ButtonDesc("Local", "Local Variable",
				"Local variables can be defined within an expression to improve readability and "
						+ "to avoid repeated calculations.",
				"A local variable can take the value of any valid type, i.e. a number, string, "
						+ "object, array, or lambda function.",
				"x = ;",
				-1,
				"'x = 1; 2 * x' returns 2.",
				"'x = \"abc\"; y = \"def\"; x + y' returns \"abcdef\"."));

		// BASIC MATHEMATICAL OPERATORS

		basicOperators.add(new ButtonDesc("( )", "Round Brackets '( )'",
				"The standard rules for mathematical order of operation are respected when an "
						+ "expression is evaluated. "
						+ "Round brackets can be used to modify the order of operation.",
				"Round brackets are also used to specify the index for an array or the arguments "
						+ "for a lambda function.",
				"()",
				-1,
				"'1 + 2 * 3' returns 7.",
				"'(1 + 2) * 3' returns 9.",
				"'2 * 3 ^ 2' returns 18",
				"{5, 6, 7}(2) returns 6"));

		basicOperators.add(new ButtonDesc("##", "Comment ('##')",
				"One or more comments can be added to an expression to improve readability. "
						+ "Any text enclosed by two hash marks is interpreted as a comment.",
				"An expression containing a comment must be enclosed by single quotes.",
				"##",
				-1,
				"'1 #Comment A# + 2 * 3 #Comment B#' returns 7."));

		basicOperators.add(new ButtonDesc("+", "Addition/Concatenation operator ('+')",
				"For numbers, the second number is added to the first. "
						+ "For strings, the second string is concatenated to the first. "
						+ "For arrays, the second array is appended to the first.",
				"Accepts two numbers, two strings, or two arrays. If the numbers have units, the "
						+ "units must be the same, and the number returned will have that unit.",
				"'1 + 2' returns 3.",
				"'1[m] + 2[m]' returns 3[m].",
				"'\"abc\" + \"def\"' returns \"abcdef\".",
				"'{1, 2, 3} + {4, 5, 6}' returns {1, 2, 3, 4, 5, 6}"));

		basicOperators.add(new ButtonDesc("-", "Subtraction operator ('-')",
				"Subtracts the second number from the first.",
				"Accepts two numbers with the same units.",
				"'3 - 1' returns 2.",
				"'3[m] - 1[m]' returns 2[m]."));

		basicOperators.add(new ButtonDesc("*", "Multiplication operator ('*')",
				"Multiplies the first number by the second. The units for the resulting number "
						+ "are calculated from the units for the two numbers.",
				"Accepts two numbers with compatible units.",
				"'2 * 3' returns 6.",
				"'2[m/s] * 3[s]' returns 6[m]."));

		basicOperators.add(new ButtonDesc("/", "Division operator ('/')",
				"Divides the first number by the second. The units for the resulting number "
					+ "are calculated from the units for the two numbers.",
				"Accepts two numbers with compatible units.",
				"'6 / 3' returns 2.",
				"'6[m] / 3[s]' returns 2[m/s]"));

		basicOperators.add(new ButtonDesc("^", "Exponentation operator ('^')",
				"Raises the first number to the power the second.",
				"Both numbers must be dimensionless.",
				"'3 ^ 2' returns 9."));

		basicOperators.add(new ButtonDesc("?", "Conditional or Tenary operator ('?')",
				"Calculates a result using two arguments on the right-hand side. "
						+ "The argument on the left-hand side is a number that represents a "
						+ "Boolean condition (0 = false, non-zero = true). "
						+ "The first argument on the right is returned if the condition is true. "
						+ "The second argument is returned if the condition is false.",
				"The argument on the left-hand side of the operator must be a dimensionless "
						+ "number. The two arguments on the right-hand side can be numbers, "
						+ "strings, entities, etc. as long as both are the same type.",
				"? 1 : 0",
				-4,
				"'0 ? 2 : 3' returns 3",
				"'1 ? 2 : 3' returns 2"));

		basicOperators.add(new ButtonDesc("%", "Modulo or Remainder operator ('%')",
				"Returns the remainder after division of the first number by the second.",
				"Accepts two numbers with or without units. If the numbers have units, the units "
						+ "must be the same, and the number returned will have that unit.",
				"'5 % 2' returns 1.",
				"'5[m] % 2[m]' returns 1[m]"));

		// LOGICAL OPERATORS

		logicalOperators.add(new ButtonDesc("==", "Equal-to operator ('==')",
				"Compares the two arguments for equality. "
						+ "Returns 1 if the arguments are equal. "
						+ "Returns 0 if the arguments are unequal.",
				"The two arguments can be numbers with or without units, strings, objects, etc. "
						+ "If the arguments are numbers with units, the units must be the same.",
				"'4 == 4' returns 1.",
				"'5 == 4' returns 0.",
				"'5[m] == 5[m]' returns 1",
				"'[Entity1] == [Entity1]' returns 1"));

		logicalOperators.add(new ButtonDesc("!=", "Not-equal-to operator ('!=')",
				"Compares the two arguments for non-equality. "
						+ "Returns 1 if the arguments are not equal. "
						+ "Returns 0 if the arguments are equal.",
				"The two arguments can be numbers with or without units, strings, objects, etc. "
						+ "If the arguments are numbers with units, the units must be the same.",
				"'4 != 4' returns 0.",
				"'5 != 4' returns 1.",
				"'5[m] != 5[m]' returns 0",
				"'[Entity1] != [Entity1]' returns 0"));

		logicalOperators.add(new ButtonDesc("<", "Less-than operator ('<')",
				"Compares the magnitude of two numbers. "
						+ "Returns 1 if the first number is less than the first. "
						+ "Returns 0 if the first number is greater than or equal to the second.",
				"The two arguments can be numbers with or without units. "
						+ "If the numbers have units, the units must be the same.",
				"'1 < 2' returns 1.",
				"'1[m] < 2[m]' returns 1."));

		logicalOperators.add(new ButtonDesc("<=", "Less-than-or-equal-to operator ('<=')",
				"Compares the magnitude of two numbers. "
						+ "Returns 1 if the first number is less than or equal to the second. "
						+ "Returns 0 if the first number is greater than the second.",
				"The two arguments can be numbers with or without units. "
						+ "If the numbers have units, the units must be the same.",
				"'1 <= 1' returns 1.",
				"'1[m] <= 1[m]' returns 1."));

		logicalOperators.add(new ButtonDesc(">", "Greater-than operator ('>')",
				"Compares the magnitude of two numbers. "
						+ "Returns 1 if the first number is greater than the first. "
						+ "Returns 0 if the first number is less than or equal "
						+ "to the second.",
				"The two arguments can be numbers with or without units. "
						+ "If the numbers have units, the units must be the same.",
				"'2 > 1' returns 1.",
				"'2[m] > 1[m]' returns 1."));

		logicalOperators.add(new ButtonDesc(">=", "Greater-than-or-equal-to operator ('>=')",
				"Compares the magnitude of two numbers. "
						+ "Returns 1 if the first number is greater than or equal to the second. "
						+ "Returns 0 if the first number is less than the second.",
				"The two arguments can be numbers with or without units. "
						+ "If the numbers have units, the units must be the same.",
				"'1 >= 1' returns 1.",
				"'1[m] >= 1[m]' returns 1."));

		logicalOperators.add(new ButtonDesc("&&", "Logical AND operator ('&&')",
				"Returns 1 if both the first and second numbers are non-zero (true). "
						+ "Returns 0 if the either number is zero (false). "
						+ "Short-circuited evaluation is used, i.e. "
						+ "if the first argument is zero, the operator returns 0 without "
						+ "evaluating the second argument.",
				"The two arguments must be dimensionless numbers.",
				"'1 && 1' returns 1.",
				"'0 && 1' returns 0."));

		logicalOperators.add(new ButtonDesc("||", "Logical OR operator ('||')",
				"Returns 1 is returned if either the first or second numbers are non-zero (true). "
						+ "Returns 0 is if both numbers are zero (false). "
						+ "Short-circuited evaluation is used, i.e. "
						+ "if the first argument is non-zero, the operator returns 1 without "
						+ "evaluating the second argument.",
				"The two arguments must be dimensionless numbers.",
				"'1 || 0' returns 1.",
				"'0 || 0' returns 0."));

		logicalOperators.add(new ButtonDesc("!", "Logical NOT operator ('!')",
				"Returns 1 if the number on the right-hand side is zero (false). "
						+ "Returns 0 if the number on the right-hand side is non-zero (true). "
						+ "Calculates a result without an argument on the left-hand side. ",
				"The argument must be a dimensionless number.",
				"'!0' returns 1.",
				"'!1' returns 0."));

		// FUNCTIONS

		functions.add(new ButtonDesc("PI", "Mathematical constant ('PI')",
				"Returns 3.14159...",
				null,
				"PI()",
				0,
				"'PI() + 1' returns 4.14159..."));

		functions.add(new ButtonDesc("E", "Mathematical constant ('E')",
				"Returns 2.71828...",
				null,
				"E()",
				0,
				"'E() + 1' returns 3.71828..."));

		functions.add(new ButtonDesc("min", "Minimum function ('min')",
				"Returns the smallest of a list of values.",
				"Accepts a list of numbers with or without units. "
						+ "Returns a number with the same units.",
				"min()",
				-1,
				"'min(2, 1, 3)' returns 1",
				"'min(2[s], 1[s], 3[s])' returns 1[s]"));

		functions.add(new ButtonDesc("max", "Maximum function ('max')",
				"Returns the largest of a list of values.",
				"Accepts a list of numbers with or without units. "
						+ "Returns a number with the same units.",
				"max()",
				-1,
				"'max(2, 1, 3)' returns 3",
				"'max(2[s], 1[s], 3[s])' returns 3[s]"));

		functions.add(new ButtonDesc("indexOfMin", "IndexOfMin function ('indexOfMin')",
				"Returns the position of the minimum in a list of values.",
				"Accepts a list of numbers with or without units. "
						+ "Returns a dimensionless integer.",
				"indexOfMin()",
				-1,
				"'indexOfMin(2, 1, 3)' returns 2",
				"'indexOfMin(2[s], 1[s], 3[s])' returns 2"));

		functions.add(new ButtonDesc("indexOfMax", "IndexOfMax function ('indexOfMax')",
				"Returns the position of the maximum in a list of values.",
				"Accepts a list of numbers with or without units. "
						+ "Returns a dimensionless integer.",
				"indexOfMax()",
				-1,
				"'indexOfMax(2, 1, 3)' returns 3",
				"'indexOfMax(2[s], 1[s], 3[s])' returns 3"));

		functions.add(new ButtonDesc("abs", "Absolute value function ('abs')",
				"Returns the absolute value of a number with or without units.",
				"Accepts a number with or without units. Returns a number with the same units.",
				"abs()",
				-1,
				"'abs(2)' returns 2",
				"'abs(-2)' returns 2",
				"'abs(-2[s])' returns 2[s]"));

		functions.add(new ButtonDesc("ceil", "Ceiling function ('ceil')",
				"Returns the smallest (closest to negative infinity) integer that is greater than "
						+ "or equal to the argument.",
				"Accepts a number with or without units. Returns an integer with the same units.",
				"ceil()",
				-1,
				"'ceil(2.4)' returns 3",
				"'ceil(-2.4)' returns -2",
				"'ceil(2.4[s])' returns 3[s]"));

		functions.add(new ButtonDesc("floor", "Floor function ('floor')",
				"Returns the largest (closest to positive infinity) integer that is less than "
						+ "or equal to the argument.",
				"Accepts a number with or without units. Returns an integer with the same units.",
				"floor()",
				-1,
				"'floor(2.4)' returns 2",
				"'floor(-2.4)' returns -3",
				"'floor(2.4[s])' returns 2[s]"));

		functions.add(new ButtonDesc("round", "Round function ('round')",
				"Returns the closest integer to the argument, with ties rounding to positive infinity.",
				"Accepts a number with or without units. Returns an integer with the same units.",
				"round()",
				-1,
				"'round(2.4)' returns 2",
				"'round(2.5)' returns 3",
				"'round(-2.4)' returns -2",
				"'round(-2.5)' returns -2",
				"'round(2.4[s])' returns 2[s]"));

		functions.add(new ButtonDesc("signum", "Signum function ('signum')",
				"Returns zero if the argument is zero, 1.0 if the argument is greater than zero, "
						+ "and -1.0 if the argument is less than zero.",
				"Accepts a number with or without units. Returns a dimensionless integer.",
				"signum()",
				-1,
				"'signum(0.0)' returns 0",
				"'signum(2.4)' returns 1",
				"'signum(-2.4)' returns -1",
				"'signum(2.4[s])' returns 1"));

		functions.add(new ButtonDesc("sqrt", "Square root function ('sqrt')",
				"Returns the square root of a number.",
				"Accepts a non-negative, dimensionless number and returns a non-negative, "
						+ "dimensionless number.",
				"sqrt()",
				-1,
				"'sqrt(4.0)' returns 2.0"));

		functions.add(new ButtonDesc("cbrt", "Cube root function ('cbrt')",
				"Returns the cube root of a number.",
				"Accepts a dimensionless number and returns a dimensionless number.",
				"cbrt()",
				-1,
				"'cbrt(8.0)' returns 2.0",
				"'cbrt(-8.0)' returns -2.0"));

		functions.add(new ButtonDesc("choose", "Choose function ('choose')",
				"Returns a value selected from a list using an index.",
				"Accepts an integer followed by a list of numbers (with or without units), "
						+ "strings, entities, arrays, or lambda functions. ",
				"choose()",
				-1,
				"'choose(2, 1[s], 2[s], 3[s])' returns 2[s].",
				"'choose(2, \"abc\", \"def\", \"ghi\")' returns \"def\".",
				"'choose(2, [Entity1], [Entity2], [Entity3])' returns [Entity2]."));

		functions.add(new ButtonDesc("exp", "Exponential function ('exp')",
				"Returns the exponential of the input value.",
				"Accepts a dimensionless number and returns a non-negative, dimensionless number.",
				"exp()",
				-1,
				"'exp(1.0)' returns 2.71828..."));

		functions.add(new ButtonDesc("ln", "Natural logarithm function ('ln')",
				"Returns the natural logarithm of the input value.",
				"Accepts a non-negative, dimensionless number and returns a dimensionless number.",
				"ln()",
				-1,
				"'ln(2.71828)' returns 0.999999"));

		functions.add(new ButtonDesc("log", "Base 10 logarithm function ('log')",
				"Returns the base 10 logarithm of the input value.",
				"Accepts a non-negative, dimensionless number and returns a dimensionless number.",
				"log()",
				-1,
				"'log(100.0)' returns 2.0"));

		functions.add(new ButtonDesc("sin", "Sine function ('sin')",
				"Returns the sine function of the input value.",
				"Accepts a dimensionless number or one with the units of AngleUnit and returns a "
						+ "dimensionless number between -1.0 and 1.0.",
				"sin()",
				-1,
				"'sin(30[deg])' returns 0.5"));

		functions.add(new ButtonDesc("cos", "Cosine function ('cos')",
				"Returns the cosine function of the input value.",
				"Accepts a dimensionless number or one with the units of AngleUnit and returns a "
						+ "dimensionless number between -1.0 and 1.0.",
				"cos()",
				-1,
				"'cos(60[deg])' returns 0.5"));

		functions.add(new ButtonDesc("tan", "Tangent function ('tan')",
				"Returns the tangent function of the input value.",
				"Accepts a dimensionless number or one with the units of AngleUnit and returns a "
						+ "dimensionless number.",
				"tan()",
				-1,
				"'tan(45[deg])' returns 1.0"));

		functions.add(new ButtonDesc("asin", "Arcsine function ('asin')",
				"Returns the arcsine function of the input value.",
				"Accepts a dimensionless number between -1.0 and 1.0, and returns number with "
						+ "units of AngleUnit.",
				"asin()",
				-1,
				"'asin(0.5)' returns 30[deg]"));

		functions.add(new ButtonDesc("acos", "Arccosine function ('acos')",
				"Returns the arccosine function of the input value.",
				"Accepts a dimensionless number between -1.0 and 1.0, and returns number with "
						+ "units of AngleUnit.",
				"acos()",
				-1,
				"'acos(0.5)' returns 60[deg]"));

		functions.add(new ButtonDesc("atan", "Arctangent function ('atan')",
				"Returns the arctangent function of the input value.",
				"Accepts a dimensionless number and returns number with units of AngleUnit.",
				"atan()",
				-1,
				"'atan(1.0)' returns 45[deg]"));

		functions.add(new ButtonDesc("atan2", "Two-argument arctangent function ('atan2')",
				"For Cartesian coordinates x and y, atan2(x,y) returns the angle for the "
						+ "corresponding polar coordinates.",
				"Accepts two dimensionless numbers and returns number with units of AngleUnit.",
				"atan2()",
				-1,
				"'atan2(1.0, -1.0)' returns 135[deg]"));

		functions.add(new ButtonDesc("notNull", "Not-null function ('notNull')",
				"Determines whether an entity exists. It can be used to test whether an output "
						+ "such as obj has been set.",
				"Accepts an entity and returns a dimensionless number (either 0 or 1).",
				"notNull()",
				-1,
				"'notNull([Server1].obj)' returns 1 if the output obj has been set."));

		functions.add(new ButtonDesc("format", "Format function ('format')",
				"Constructs a string using a format string and one or more additional arguments. "
						+ "Each argument can be a dimensionless number, string, entity, array, "
						+ "or map. The function mirrors the String.format method provided in "
						+ "Java. Each entry of % in the format string indicates a format code "
						+ "that inserts the next argument in the resulting output text. "
						+ "The full set of Java format codes is supported. The two most relevant "
						+ "are: %s - displays any argument as a string, and %.nf - displays a "
						+ "number with n decimal places.",
				"Accepts a string followed by one or more arguments that can be dimensionless "
						+ "numbers, strings, entities, arrays, or maps. Returns a string.",
				"format()",
				-1,
				"'format(\"x = %s\", [Entity1])' returns \"x = [Entity1]\".",
				"'format(\"x = %s\", 5)' returns \"x = 5.0\".",
				"'format(\"x = %.3f\", 5)' returns \"x = 5.000\".",
				"'format(\"x = %.0f cm\", 5[m]/1[cm])' returns \"x = 500 cm\"."));

		functions.add(new ButtonDesc("size", "Size function ('size')",
				"Returns the number of entries in an array or map.",
				"Accepts an array or map and returns a dimensionless, non-negative number.",
				"size()",
				-1,
				"'size( {5, -1, 2} )' returns 3."));

		functions.add(new ButtonDesc("minCol", "Minimum function for a Collection ('minCol')",
				"Returns the smallest entry in an array or map.",
				"Accepts an array or map of numbers with or without units. "
						+ "Returns a number in the same units.",
				"minCol()",
				-1,
				"'minCol( {5, -1, 2} )' returns -1."));

		functions.add(new ButtonDesc("maxCol", "Minimum function for a Collection ('maxCol')",
				"Returns the largest entry in an array or map.",
				"Accepts an array or map of numbers with or without units. "
						+ "Returns a number in the same units.",
				"maxCol()",
				-1,
				"'maxCol( {5, -1, 2} )' returns 5."));

		functions.add(new ButtonDesc("sum", "Sum function for a Collection ('sum')",
				"Returns the sum of the entries in an array or map.",
				"Accepts an array or map of numbers with or without units. "
						+ "Returns a number in the same units.",
				"sum()",
				-1,
				"'sum( {5, -1, 2} )' returns 6."));

		functions.add(new ButtonDesc("indexOfMinCol", "IndexOfMin function for a Collection "
						+ "('indexOfMinCol')",
				"Returns index or key for the smallest entry in an array or map.",
				"Accepts an array or map of numbers with or without units. "
						+ "Returns a postive integer or a key for the map.",
				"indexOfMinCol()",
				-1,
				"'indexOfMinCol( {5, -1, 2} )' returns 2."));

		functions.add(new ButtonDesc("indexOfMaxCol", "IndexOfMax function for a Collection "
						+ "('indexOfMaxCol')",
				"Returns index or key for the largest entry in an array or map.",
				"Accepts an array or map of numbers with or without units. "
						+ "Returns a postive integer or a key for the map.",
				"indexOfMaxCol()",
				-1,
				"'indexOfMaxCol( {5, -1, 2} )' returns 1."));

		functions.add(new ButtonDesc("indexOf", "IndexOf function ('indexOf')",
				"Returns the index or key for the entry in an array or map that is equal to a "
						+ "specified value.",
				"Accepts an array or map of numbers, strings, or objects followed by a number, "
						+ "string, or object. "
						+ "Returns a dimensionless integer or key. "
						+ "Zero is returned if the value is not included in the array or map.",
				"indexOf()",
				-1,
				"'indexOf( {2, 1, 3}, 1 )' returns 2",
				"'indexOf( {2, 1, 3}, 5 )' returns 0",
				"'indexOf( {2[s], 1[s], 3[s]}, 1[s] )' returns 2",
				"'indexOf( {[Object2], [Object1], [Object3]}, [Object1] )' returns 2"));

		functions.add(new ButtonDesc("indexOfNearest", "indexOfNearest function for a Collection "
						+ "('indexOfNearest')",
				"Returns index or key for the entry in an array or map that is closest to a "
						+ "specified number.",
				"Accepts an array or map of numbers with or without units, followed by number "
						+ "with the same units. Returns a postive integer or a key for the map.",
				"indexOfNearest()",
				-1,
				"'indexOfNearest( {5, -1, 2}, 1.5 )' returns 3."));

		functions.add(new ButtonDesc("range", "Range function ('range')",
				"Returns an array of numerical values that can be used as an input to a "
						+ "higher-order function.",
				"Accepts one, two, or three numerical arguments corresponding to the initial "
						+ "value, the final value, and the increment between values.",
				"range()",
				-1,
				"'range(3)' returns {1, 2, 3}.",
				"'range(2, 4)' returns {2, 3, 4}.",
				"'range(2, 3, 0.5)' returns {2.0, 2.5, 3.0}.",
				"'range(2, 1)' returns {}."));

		functions.add(new ButtonDesc("map", "Map higher-order function ('map')",
				"Applies a one-input lambda function to each element of an array and returns an "
						+ "array with the resulting values. If a two-input lambda function is "
						+ "specified, the second input is the index of the element in the array.",
				"Accepts a lamdba function and an array whose entries are suitable as inputs to "
						+ "the lambda function. Returns a array containing the outputs of the "
						+ "lambda function.",
				"map()",
				-1,
				"'map( |x|(2 * x), {1, 2} )' returns {2, 4}.",
				"'map( |x, i|(2 * i), {5, 8} )' returns {2, 4}."));

		functions.add(new ButtonDesc("filter", "Filter higher-order function ('filter')",
				"Applies a one-input lambda function to each element of an array and returns an "
						+ "array with only the ones that return a non-zero number (true). "
						+ "If a two-input lambda function is specified, the second input is the "
						+ "index of the element in the array.",
				"Accepts a lamdba function and an array whose entries are suitable as inputs to "
						+ "the lambda function. Returns a array containing a sub-set of the input "
						+ "array.",
				"filter()",
				-1,
				"'filter( |x|(x > 2), {1, 2, 3, 4} )' returns {3, 4}.",
				"'filter( |x, i|(i > 2), {5, 6, 7, 8} )' returns {7, 8}."));

		functions.add(new ButtonDesc("reduce", "Reduce higher-order function ('reduce')",
				"Applies the first input of a two-input lambda function to each element of an "
						+ "array. The second input to the reduce function is the initial value "
						+ "for an internal value maintained by the function during the "
						+ "calculation. The result of the calculation for each element is "
						+ "assigned to this internal value. After the last element is processed, "
						+ "the internal value is returned.",
				"Accepts three inputs: the lambda function, the initial value to be assigned to "
						+ "the internal value, and the array to be processed.",
				"reduce()",
				-1,
				"'reduce( |x, accum|(x + accum), 0, {1, 2, 3} )' returns 6.",
				"'reduce( |x, accum|(max(x, accum)), 0, {1, 2, 3})' returns 3.",
				"'reduce( |x, accum|(x || accum), 0, {0, 1, 0})' returns 1."));

		functions.add(new ButtonDesc("sort", "Sort higher-order function ('sort')",
				"Applies a two-input lambda function to the elements of an array and returns an "
						+ "array that has been re-ordered so that the lambda function returns a "
						+ "non-zero number (true) for each adjacent pair of elements. The lambda "
						+ "function must return 0 (false) for entries that are equal.",
				"Accepts a lamdba function and an array whose entries are suitable as inputs to "
						+ "the lambda function. Returns a array containing a re-ordered version "
						+ "of the input array.",
				"sort()",
				-1,
				"'sort(|x, y|(x > y),{2, 3, 1})' returns {3, 2, 1}."));

		functions.add(new ButtonDesc("parseNumber", "Parse number function ('parseNumber')",
				"Converts a string to a floating point number.",
				"Accepts a string that represents a valid floating point number. "
						+ "Returns the dimensionless number represented by the string.",
				"parseNumber()",
				-1,
				"'parseNumber(\"1.5\")' returns 1.5"));

		functions.add(new ButtonDesc("substring", "Substring function ('substring')",
				"Returns a string that is a substring of the specified string. "
						+ "The substring begins at the specified beginIndex and extends to the "
						+ "character at index endIndex - 1. "
						+ "Thus the length of the substring is endIndex-beginIndex.",
				"Accepts a string followed by the beginIndex and the endIndex values. "
						+ "If the endIndex is omitted, the substring extends to the end of the "
						+ "string. Returns a string containing the specified portion of the "
						+ "original string.",
				"substring()",
				-1,
				"'substring(\"abcdefg\", 3)' returns \"cdefg\"",
				"'substring(\"abcdefg\", 3, 6)' returns \"cde\""));

		functions.add(new ButtonDesc("indexOfStr", "IndexOf function for Strings ('indexOfStr')",
				"Returns the index within the specifed string of the first occurrence of the "
						+ "specified substring, starting at the specified index. "
						+ "Zero is returned if the substring is not found.",
				"Accepts a string followed by the substring and the fromIndex value. "
						+ "If the fromIndex is omitted, the entire string is searched. "
						+ "Returns a dimensionless number.",
				"indexOfStr()",
				-1,
				"'indexOfStr(\"abcdefg\", \"cd\")' returns 3",
				"'indexOfStr(\"ffaffbc\", \"ff\", 3)' returns 4"));

		functions.add(new ButtonDesc("toUpperCase", "UpperCase function ('toUpperCase')",
				"Converts the specified string to upper case characters.",
				"Accepts a string and returns a string.",
				"toUpperCase()",
				-1,
				"'toUpperCase(\"abc\")' returns \"ABC\""));

		functions.add(new ButtonDesc("toLowerCase", "LowerCase function ('toLowerCase')",
				"Converts the specified string to lower case characters.",
				"Accepts a string and returns a string.",
				"toLowerCase()",
				-1,
				"'toLowerCase(\"ABC\")' returns \"abc\""));

		functions.add(new ButtonDesc("trim", "Trim function ('trim')",
				"Removes leading and trailing whitespace from the specified string.",
				"Accepts a string and returns a string.",
				"trim()",
				-1,
				"'trim(\"  abc  \")' returns \"abc\""));

		functions.add(new ButtonDesc("split", "Split function ('split')",
				"Divides the specified string into an array of substrings around matches to a "
						+ "specified 'regex' string. "
						+ "An optional third 'limit' argument specifies the maximum number of "
						+ "substrings to return. "
						+ "The function mirrors the split method provided in Java. "
						+ "See the Java documentation for 'regex' string (regular expression).",
				"Accepts a string followed by a 'regex' string. "
						+ "An optional 'limit' number can be included. "
						+ "Returns an array of strings.",
				"split()",
				-1,
				"'split(\"ab:cd:ef\", \":\")' returns {\"ab\", \"cd\", \"ef\"}",
				"'split(\"ab.cd.ef\", \"\\.\")' returns {\"ab\", \"cd\", \"ef\"}",
				"'split(\"ab.cd.ef\", \"\\.\", 2)' returns {\"ab\", \"cd.ef\"}"));

		functions.add(new ButtonDesc("length", "Length function ('length')",
				"Returns the length of the specified string. "
						+ "The length is equal to the number of characters in the string.",
				"Accepts a string and returns a dimensionless integer.",
				"length()",
				-1,
				"'length(\"abc\")' returns 3"));

	}

}
