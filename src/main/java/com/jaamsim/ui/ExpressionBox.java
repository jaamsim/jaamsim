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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Collections;

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
import com.jaamsim.basicsim.ObjectType;
import com.jaamsim.input.Input;
import com.jaamsim.input.InputAgent;
import com.jaamsim.input.KeywordIndex;
import com.jaamsim.input.Parser;
import com.jaamsim.units.Unit;

public class ExpressionBox extends JDialog {

	private final Input<?> input;
	private final JTextArea editArea;
	private final JTextField msgText;
	private final JButton acceptButton;
	private final JButton cancelButton;
	private int result;

	public static final int CANCEL_OPTION = 1;  // Cancel button is clicked
	public static final int APPROVE_OPTION = 0; // Accept button is clicked
	public static final int ERROR_OPTION = -1;  // Error occurs or the dialog is dismissed

	public ExpressionBox(Input<?> in, String str) {
		super((JDialog)null, "Expression Builder", true);

		getContentPane().setLayout( new BorderLayout() );
		setPreferredSize(new Dimension(900, 300));
		setIconImage(GUIFrame.getWindowIcon());
		setAlwaysOnTop(true);

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

		// Error message text
		JLabel msgLabel = new JLabel( "Message:" );
		msgText = new JTextField("", 60);
		msgText.setEditable(false);

		// Buttons
		acceptButton = new JButton("Accept");
		cancelButton = new JButton("Cancel");

		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout( new FlowLayout(FlowLayout.CENTER) );
		buttonPanel.add(msgLabel);
		buttonPanel.add(msgText);
		buttonPanel.add(acceptButton);
		buttonPanel.add(cancelButton);
		getContentPane().add(buttonPanel, BorderLayout.SOUTH);

		pack();
		editArea.requestFocusInWindow();

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
			}

			@Override
			public void removeUpdate(DocumentEvent e) {
				tryParse();
			}

			@Override
			public void changedUpdate(DocumentEvent e) {}
	    });
	}

	private void tryParse() {
		try {
			Entity ent = EditBox.getInstance().getCurrentEntity();
			String str = editArea.getText().replace("\n", " ");
			ArrayList<String> tokens = new ArrayList<>();
			Parser.tokenize(tokens, str, true);
			KeywordIndex kw = new KeywordIndex(input.getKeyword(), tokens, null);
			InputAgent.storeAndExecute(new KeywordCommand(ent, kw));
			msgText.setText("");
			acceptButton.setEnabled(true);
		}
		catch (Exception e) {
			msgText.setText(e.getMessage());
			acceptButton.setEnabled(false);
		}
	}

	private void undoEdits() {
		InputAgent.undo();
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
					JPopupMenu entityMenu = new JPopupMenu();
					ArrayList<String> entNameList = new ArrayList<>();
					for (DisplayEntity each: Entity.getClonesOfIterator(DisplayEntity.class)) {
						if (each.testFlag(Entity.FLAG_GENERATED))
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
					JPopupMenu unitMenu = new JPopupMenu();

					// Loop through the unit types that have been defined
					for (String utName : Unit.getUnitTypeList()) {
						ObjectType ot = Input.parseEntity(utName, ObjectType.class);
						final Class<? extends Unit> ut = Input.checkCast(ot.getJavaClass(), Unit.class);

						ArrayList<? extends Unit> unitList = Unit.getUnitList(ut);
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
					JPopupMenu unitTypeMenu = new JPopupMenu();

					// Loop through the unit types that have been defined
					for (String utName : Unit.getUnitTypeList()) {
						ObjectType ot = Input.parseEntity(utName, ObjectType.class);
						final Class<? extends Unit> ut = Input.checkCast(ot.getJavaClass(), Unit.class);
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

	private static ArrayList<ButtonDesc> initObjects = new ArrayList<>();
	private static ArrayList<ButtonDesc> simObjects = new ArrayList<>();
	private static ArrayList<ButtonDesc> expObjects = new ArrayList<>();
	private static ArrayList<ButtonDesc> basicOperators = new ArrayList<>();
	private static ArrayList<ButtonDesc> logicalOperators = new ArrayList<>();

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

		simObjects.add(new ButtonDesc("Sim", "Entity 'Simulation'",
				"The Simulation entity is used to store the inputs and outputs related to the "
						+ "simulation run.",
				null,
				"[Simulation]",
				0,
				"'[Simulation].RunNumber' returns the sequence number for the present "
						+ "simulation run."));

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
				"At present, maps are used only for certain outputs such as 'StateTimes' and a "
						+ "new map cannot be created in an expression. This button is provided "
						+ "only for the purpose of documentation and has no effect other than "
						+ "this pop-up.",
				"",
				0,
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
				"'5[m] / 2[m]' returns 1[m]"));

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
	}

}
