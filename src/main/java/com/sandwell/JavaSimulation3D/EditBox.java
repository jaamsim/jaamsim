/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2005-2011 Ausenco Engineering Canada Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 */

package com.sandwell.JavaSimulation3D;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.lang.reflect.Field;
import java.util.ArrayList;

import javax.swing.AbstractCellEditor;
import javax.swing.DefaultCellEditor;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;
import javax.vecmath.Vector3d;

import com.jaamsim.math.Color4d;
import com.jaamsim.ui.EditBoxColumnRenderer;
import com.jaamsim.ui.FrameBox;
import com.sandwell.JavaSimulation.BooleanInput;
import com.sandwell.JavaSimulation.ColourInput;
import com.sandwell.JavaSimulation.Entity;
import com.sandwell.JavaSimulation.Input;
import com.sandwell.JavaSimulation.InputErrorException;
import com.sandwell.JavaSimulation.Keyword;
import com.sandwell.JavaSimulation.ListInput;
import com.sandwell.JavaSimulation.StringChoiceInput;
import com.sandwell.JavaSimulation.StringListInput;

/**
 * Class to display information about model objects. <br>
 * displays the results of getInfo for an object
 */
public class EditBox extends FrameBox {

	private static EditBox myInstance;  // only one instance allowed to be open
	private static final int ROW_HEIGHT=20;
	private static final int VALUE_COLUMN=2;

	private Entity currentEntity;
	private final JTabbedPane jTabbedPane;

	private int presentPage; // Present tabbed page

	private boolean buildingTable;	    // TRUE if the table is being populated

	private final TableCellRenderer keywordColumnRender = new EditBoxColumnRenderer();

	/**
	 * Widths of columns in the table of keywords as modified by the user in an edit
	 * session. When the EditBox is re-opened for the next edit (during the same session),
	 * the last modified column widths are used.
	 * <pre>
	 * userColWidth[ 0 ] = user modified width of column 0 (keywords column)
	 * userColWidth[ 1 ] = user modified width of column 1 (defaults units column)
	 * userColWidth[ 2 ] = user modified width of column 2 (values column)
	 */
	private int[] userColWidth = { 150, 150, 150 };

	private EditBox() {

		super( "Input Editor" );

		setDefaultCloseOperation(FrameBox.HIDE_ON_CLOSE);

		buildingTable = false;

		// Set the preferred size of the panes
		jTabbedPane = new JTabbedPane();
		jTabbedPane.setPreferredSize( new Dimension( 700, 400 ) );
		getContentPane().add(jTabbedPane);

		// Register a change listener
		jTabbedPane.addChangeListener(new ChangeListener() {

			// This method is called whenever the selected tab changes
			public void stateChanged(ChangeEvent evt) {
				JTabbedPane pane = (JTabbedPane)evt.getSource();

				// JTabbedPane hasn't built yet
				if(buildingTable || pane.getSelectedIndex() < 0)
					return;

				presentPage = jTabbedPane.getSelectedIndex();
				updateValues();
			}
		});

		pack();
		setLocation(220, 710);
		setSize(530, 290);
	}

	public synchronized static EditBox getInstance() {
		if (myInstance == null)
			myInstance = new EditBox();

		return myInstance;
	}

	public static final String getInputDesc(Entity ent, Input<?> in) {
		ArrayList<Field> fields = new ArrayList<Field>();
		Class<?> current = ent.getClass();

		while (true) {
			for (Field f : current.getDeclaredFields()) {
				if (Input.class.isAssignableFrom(f.getType())) {
					fields.add(f);
					try {
						f.setAccessible(true);
						if (in == f.get(ent)) {
							Keyword key = f.getAnnotation(Keyword.class);
							if (key == null)
								return null;

							StringBuilder build = new StringBuilder();
							build.append("<HTML>");
							build.append("<b>Keyword:</b>  ");
							build.append(in.getKeyword());
							build.append("<BR>");
							build.append("<b>Category:</b> ");
							build.append(in.getCategory());
							build.append("<BR><BR>");
							build.append("<b>Description:</b> ");
							for (String line : key.desc().split("\n", 0)) {
								// Replace all <> for html parsing
								String tempLine = line.replaceAll("<", "&lt;");
								tempLine = tempLine.replaceAll(">", "&gt;");

								int len = 0;
								build.append("<BR>");
								// Break the line at 100-char boundaries
								for (String word : tempLine.split(" ", -1)) {
									build.append(word).append(" ");
									len += word.length() + 1;
									if (len > 100) {
										build.append("<BR>");
										len = 0;
									}
								}
							}
							if (key.example().length() > 0) {
								build.append("<BR><BR><b>Example:</b> ");
								for (String each : key.example().split("\n", 0))
									build.append("<BR>").append(each);
							}

							return build.toString();
						}
					}
					catch (IllegalArgumentException e) {}
					catch (IllegalAccessException e) {}
					catch (SecurityException e) {}
				}
			}

			current = current.getSuperclass();
			if (current == Object.class)
				break;
		}

		return null;
	}

	// ========================================
	// HTextField is for the keyword/value grid
	// ----------------------------------------
	static class HTextField extends JTextField {
		public JTable propTable;

		public HTextField(JTable propTable) {
			super();
			this.propTable = propTable;
			propTable.clearSelection();
			this.setBorder(null);
		}

		protected void processFocusEvent( FocusEvent fe ) {
			if ( fe.getID() == FocusEvent.FOCUS_GAINED ) {

				// select entire text string in the cell currently clicked on
				selectAll();
			}
			else if (fe.getID() == FocusEvent.FOCUS_LOST) {
				TableCellEditor tce = ((MyJTable)propTable).getCellEditor();

				// nothing to do
				if(tce == null)
					return;

				Component otherComp = fe.getOppositeComponent();

				// inside the JTable
				if(otherComp == propTable)
					return;

				// from a ColorCell or ListCell to JTabbedPane
				if(otherComp == EditBox.getInstance().getJTabbedPane() ) {
					return;
				}

				// colorButton or listButton is pressed
				if(otherComp != null &&
						this.getParent() == otherComp.getParent() ){
					return;
				}

				// loose focus to error box
				if(otherComp != null) {
					Component parent = otherComp.getParent();
					while(parent != null) {
						if (parent == EditBox.getInstance())
							return;

						parent = parent.getParent();
					}
				}

				// apply the input modification after loosing the focus
				tce.stopCellEditing();

				// no bold keyword when input editor looses the focus
				propTable.clearSelection();
			}
			super.processFocusEvent( fe );
		}
	}

	/**
	 * Build a JTable with number of rows
	 *
	 * @param numberOfRows
	 * @return
	 */
	private JTable buildProbTable( int numberOfRows ) {

		MyJTable propTable;
		propTable = new MyJTable(numberOfRows, 3);

		propTable.setRowHeight( ROW_HEIGHT );
		propTable.setRowSelectionAllowed( false );

		// Set DefaultCellEditor that can process FOCUS events.
		propTable.setDefaultEditor( Object.class, new DefaultCellEditor(
				new HTextField(propTable ) ) );
		DefaultCellEditor dce = (DefaultCellEditor)propTable.getDefaultEditor(Object.class);

		// Set click behavior
		dce.setClickCountToStart(1);

		// Set keyword table column headers
		propTable.getColumnModel().getColumn(0).setHeaderValue("Keyword");
		propTable.getColumnModel().getColumn( 0 ).setWidth( userColWidth[ 0 ] );
		propTable.getColumnModel().getColumn(1).setHeaderValue("Default");
		propTable.getColumnModel().getColumn( 1 ).setWidth( userColWidth[ 1 ] );
		propTable.getColumnModel().getColumn(2).setHeaderValue("Value");
		propTable.getColumnModel().getColumn( 2 ).setWidth( userColWidth[ 2 ] );

		// Listen for table changes
		propTable.getModel().addTableModelListener( new MyTableModelListener() );

		propTable.getColumnModel().getColumn(0).setCellRenderer(keywordColumnRender);
		propTable.getColumnModel().getColumn(1).setCellRenderer(FrameBox.colRenderer) ;
		propTable.getColumnModel().getColumn(2).setCellRenderer(FrameBox.colRenderer) ;

		propTable.getTableHeader().setFont(boldFont);
		propTable.getTableHeader().setReorderingAllowed(false);

		return propTable;
	}

	Entity getCurrentEntity() {
		return currentEntity;
	}

	public JTabbedPane getJTabbedPane() {
		return jTabbedPane;
	}

	public void setEntity(Entity entity) {
		if(currentEntity == entity || ! this.isVisible())
			return;

		if(entity != null && entity.testFlag(Entity.FLAG_GENERATED))
			entity = null;

		// tabbed pane has to be rebuilt
		if( currentEntity == null || entity == null ||
				currentEntity.getClass() != entity.getClass()) {

			buildingTable = true;
			presentPage = 0;
			jTabbedPane.removeAll();
			currentEntity = entity;

			// build all tabs and their prop tables
			if(currentEntity != null) {
				ArrayList<String> category = new ArrayList<String>();
				for (Input<?> in : currentEntity.getEditableInputs()) {
					if (in.getHidden())
						continue;

					if ( category.contains(in.getCategory()) )
						continue;

					category.add( in.getCategory() );
					JScrollPane pane = getScrollPaneFor(in.getCategory());
					if (in.getCategory().equals("Key Inputs")) {
						jTabbedPane.insertTab(in.getCategory(), null, pane, null, 0);
						jTabbedPane.setSelectedIndex(0);
					}
					else {
						jTabbedPane.addTab(in.getCategory(), null, pane, null);
					}
				}
			}
			buildingTable = false;
		}
		currentEntity = entity;
		updateValues();
	}

	private JScrollPane getScrollPaneFor(String category) {

		// Count number of inputs for the present category
		int categoryCount = 0;
		for( Input<?> in : currentEntity.getEditableInputs() ) {
			if (in.getCategory().equals(category) && !in.isLocked() && !in.getHidden()) {
				categoryCount++;
			}
		}

		JTable propTable = this.buildProbTable( categoryCount  );

		int row = 0;

		// fill in keyword and default columns
		for( Input<?> in : currentEntity.getEditableInputs() ) {

			if (!in.getCategory().equals(category) || in.isLocked() || in.getHidden())
				continue;

			propTable.setValueAt(in, row, 0);

			String defValString = EditBox.getDefaultValueStringOf(in);

			String unitString;
			if( defValString.replaceAll( " ","" ).equals( "{}" ) ) {
				unitString = "";
			}
			else {
				unitString = in.getUnits();
			}

			propTable.setValueAt( String.format("%s %s", defValString, unitString), row, 1 );
			row++;
		}

		JScrollPane jScrollPane = new JScrollPane(propTable);
		jScrollPane.getVerticalScrollBar().setUnitIncrement(ROW_HEIGHT);
		jScrollPane.setColumnHeaderView( propTable.getTableHeader());
		return jScrollPane;
	}

	public void updateValues() {

		// table has not built yet
		if(buildingTable || ! this.isVisible())
			return;

		// no entity is selected
		if(currentEntity == null) {
			setTitle("Input Editor");
			return;
		}

		buildingTable = true;

		setTitle( String.format("Input Editor - %s", currentEntity) );

		if (jTabbedPane.getSelectedIndex() == -1) {
			buildingTable = false;
			return;
		}
		String currentCategory = jTabbedPane.getTitleAt(jTabbedPane.getSelectedIndex());
		JTable propTable = (JTable)(((JScrollPane)jTabbedPane.getComponentAt(presentPage)).getViewport()).getComponent(0);

		int row = 0;
		for( Input<?> in : currentEntity.getEditableInputs() ) {

			if (!in.getCategory().equals(currentCategory) || in.isLocked() || in.getHidden())
				continue;

			propTable.setValueAt( String.format("%s", in.getValueString()), row, 2 );
			row++;
		}

		buildingTable = false;
	}

	public void dispose() {
		myInstance = null;
		super.dispose();
	}

	private static String getDefaultValueStringOf(Input<?> in) {
		String defValString = "{ }";
		Object defValue = in.getDefaultValue();
		if (defValue != null) {
			defValString = defValue.toString();
			if(defValue instanceof ArrayList<?>) {
				defValString = defValString.substring(1, defValString.length()-1);
			}
			else if(defValue instanceof Color4d) {
				Color4d col = (Color4d)defValue;
				defValString = String.format( "%.0f %.0f %.0f", col.r * 255, col.g * 255, col.b * 255 );
			}
			else if(defValue instanceof Vector3d) {
				defValString = String.format( "%.3f %.3f %.3f", ((Vector3d)defValue).x, ((Vector3d)defValue).y, ((Vector3d)defValue).z );
			}
			else if(in instanceof StringChoiceInput) {
				return ((StringChoiceInput)in).getDefaultChoice();
			}
			else if (in instanceof BooleanInput) {
				if ((Boolean)defValue)
					return "TRUE";
				else
					return "FALSE";
			}
		}
		return defValString;
	}

	class MyTableModelListener implements TableModelListener {

		/**
		 * Method for handling table edit events
		 */
		public void tableChanged( TableModelEvent e ) {

			// Do not worry about table changes brought on by building a new table
			if(buildingTable) {
				return;
			}

			// Find the data that has changed
			int row = e.getFirstRow();
			int col = e.getColumn();

			TableModel model = (TableModel)e.getSource();
			Object data = model.getValueAt( row, col );
			if ( model.getValueAt( row, 0 ) == null ) {
				return;
			}

			Input<?> in = (Input<?>)model.getValueAt(row, 0);

			// The value has not changed
			if(in.getValueString().equals(model.getValueAt( row, 2 ))) {
				return;
			}

			try {

				String str = data.toString();

				if( str.isEmpty() ) {

					// Back to default value
					str = getDefaultValueStringOf(in);
				}
				InputAgent.processEntity_Keyword_Value(currentEntity, in, str);
			} catch (InputErrorException exep) {
				JOptionPane.showMessageDialog(EditBox.getInstance(),
				   String.format("%s; value will be cleared", exep.getMessage() ),
				   "Input Error", JOptionPane.ERROR_MESSAGE);

				FrameBox.valueUpdate();
				return;
			}
		}
	}

	public static class MyJTable extends AjustToLastColumnTable {
		private DefaultCellEditor dropDownEditor;
		private ColorEditor colorEditor;
		private ListEditor listEditor;

		public boolean isCellEditable( int row, int column ) {
			return ( column == VALUE_COLUMN ); // Only Value column is editable
		}

		public MyJTable(int column, int row) {
			super(column, row);
		}

		public TableCellEditor getCellEditor(int row, int col) {
			Input<?> in = (Input<?>)this.getValueAt( row, 0 );

			// 1) Colour input
			if(in instanceof ColourInput) {
				if(colorEditor == null) {
					colorEditor = new ColorEditor(this);
				}
				return colorEditor;
			}

			// 2) Normal text
			ArrayList<String> array = in.getValidOptions();
			if(array == null)
				return this.getDefaultEditor(Object.class);

			// 3) Multiple selections from a List
			if(in instanceof ListInput) {
				if(listEditor == null) {
					listEditor = new ListEditor(this);
					if(in instanceof StringListInput) {
						listEditor.setCaseSensitive(
								((StringListInput)(in)).getCaseSensitive() );
					}
				}
				listEditor.setListData(array);
				return listEditor;
			}

			// 4) Single selection from a drop down box
			if(dropDownEditor == null) {
				JComboBox dropDown = new JComboBox();
				dropDown.setEditable(true);
				dropDownEditor = new DefaultCellEditor(dropDown);
			}

			// Refresh the content of the combo box
			JComboBox dropDown = (JComboBox) dropDownEditor.getComponent();
			DefaultComboBoxModel model = (DefaultComboBoxModel) dropDown.getModel();
			model.removeAllElements();
			for(String each: array) {

				// Space inside the font name
				if( each.contains(" ") )
					each = String.format("'%s'", each);
				model.addElement(each);
			}
			return new DefaultCellEditor(dropDown);
		}

		public String getToolTipText(MouseEvent e) {
			java.awt.Point p = e.getPoint();
			int rowIndex = rowAtPoint(p);
			int colIndex = columnAtPoint(p);

			// not necessary because reordering of tabs is not allowed
			colIndex = convertColumnIndexToModel(colIndex);

			// Only the keyword column has tooltip
			if (colIndex != 0)
				return null;

			TableModel model = getModel();
			Input<?> in = (Input<?>)model.getValueAt(rowIndex, 0);
			return EditBox.getInputDesc(EditBox.getInstance().getCurrentEntity(), in);
		}
	}

	public static class ColorEditor extends AbstractCellEditor
	implements TableCellEditor, ActionListener {

		private final JPanel jPanel;
		private final HTextField text;
		private final JButton colorButton;
		private JColorChooser colorChooser;
		private JDialog dialog;
		private ColourInput in;
		public JTable propTable;

		public ColorEditor(JTable table) {
			propTable = table;
			jPanel = new JPanel(new BorderLayout());

			text = new HTextField(propTable);
			jPanel.add(text, BorderLayout.WEST);

			colorButton = new JButton(new ImageIcon(
				GUIFrame.class.getResource("/resources/images/dropdown.png")));
			colorButton.addActionListener(this);
			colorButton.setActionCommand("button");
			colorButton.setContentAreaFilled(false);
			jPanel.add(colorButton, BorderLayout.EAST);
		}

		public Object getCellEditorValue() {
			return text.getText();
		}

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

				Color4d col = in.getValue();
				colorChooser.setColor(new Color((float)col.r, (float)col.g, (float)col.b));
				dialog.setLocationRelativeTo((Component)e.getSource());
				dialog.setVisible(true);

				// Apply editing
				stopCellEditing();

				// Focus the cell
				propTable.requestFocusInWindow();
			}
			else {
				Color color = colorChooser.getColor();
				text.setText( String.format("%d %d %d", color.getRed(),
						color.getGreen(), color.getBlue() ) );
			}
		}

		public Component getTableCellEditorComponent(JTable table,
				Object value, boolean isSelected, int row, int column) {

			// set the value
			in = (ColourInput)table.getValueAt( row, 0 );
			text.setText(
				((String)table.getValueAt( row, VALUE_COLUMN )).trim() );

			// right size for jPanel and its components
			Dimension dim = new Dimension(
				  table.getColumnModel().getColumn( VALUE_COLUMN ).getWidth() -
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

	public static class ListEditor extends AbstractCellEditor
	implements TableCellEditor, ActionListener {

		private final JTable propTable;
		private final JPanel jPanel;
		private final HTextField text;
		private final JButton listButton;
		private JDialog dialog;
		private JScrollPane jScroll;
		private JList list;

		private ArrayList<String> tokens;
		private DefaultListModel listModel;
		private CheckBoxMouseAdapter checkBoxMouseAdapter;
		private int i;
		private boolean bool;
		private boolean caseSensitive;

		public ListEditor(JTable table) {

			propTable = table;
			jPanel = new JPanel(new BorderLayout());

			text = new HTextField(propTable);
			jPanel.add(text, BorderLayout.WEST);

			listButton = new JButton(new ImageIcon(
				GUIFrame.class.getResource("/resources/images/dropdown.png")));
			listButton.addActionListener(this);
			listButton.setActionCommand("button");
			listButton.setContentAreaFilled(false);
			jPanel.add(listButton, BorderLayout.EAST);
			caseSensitive = true;
		}

		public Object getCellEditorValue() {
			return text.getText();
		}

		public void actionPerformed(ActionEvent e) {

			// OK button
			if("OK".equals(e.getActionCommand())) {
				dialog.setVisible(false);
				String value = "";
				for(int i = 0; i < list.getModel().getSize(); i++) {
					if(!((JCheckBox)list.getModel().getElementAt(i)).isSelected())
						continue;
					value = String.format("%s%s ", value,
						((JCheckBox)list.getModel().getElementAt(i)).getText());
				}
				text.setText(String.format(" %s", value));
			}

			if(! "button".equals(e.getActionCommand())) {
				return;
			}

			if(dialog == null) {
				dialog = new JDialog(EditBox.getInstance(), "Select items",
						true); // model
				dialog.setSize(190, 300);
				jScroll = new JScrollPane(list);
				dialog.getContentPane().add(jScroll); // top of the JDialog
				JButton okButton = new JButton("Ok");
				okButton.setActionCommand("OK");
				okButton.addActionListener(this);
				dialog.getContentPane().add("South", okButton);
				dialog.setIconImage(GUIFrame.getWindowIcon());
				dialog.setAlwaysOnTop(true);
				tokens = new ArrayList<String>();
			}

			// break the value into single options
			tokens.clear();
			InputAgent.tokenizeString(tokens, text.getText());
			if( !caseSensitive ) {
				for(i = 0; i < tokens.size(); i++ ) {
					tokens.set(i, tokens.get(i).toUpperCase() );
				}
			}

			// checkmark according to the value input
			for(i = 0; i < list.getModel().getSize(); i++) {
				bool = false;
				if( tokens.contains(
					((JCheckBox)list.getModel().getElementAt(i)).getText()) ) {
					bool = true;
				}
				((JCheckBox)list.getModel().getElementAt(i)).setSelected(bool);
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
				listModel = new DefaultListModel();
				list = new JList(listModel);

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

		public Component getTableCellEditorComponent(JTable table,
				Object value, boolean isSelected, int row, int column) {
			text.setText(
					((String)table.getValueAt( row, VALUE_COLUMN )).trim() );

			// right size for jPanel and its components in the cell
			Dimension dim = new Dimension(
					table.getColumnModel().getColumn( VALUE_COLUMN ).getWidth() -
					table.getColumnModel().getColumnMargin(),
					table.getRowHeight());
			jPanel.setPreferredSize(dim);
			dim = new Dimension(dim.width - (dim.height), dim.height);
			text.setPreferredSize(dim);
			dim = new Dimension(dim.height, dim.height);
			listButton.setPreferredSize(dim);

			return jPanel;
		}

		public void setCaseSensitive(boolean bool) {
			caseSensitive = bool;
		}
	}

	/*
	 * renderer for the JList so it shows its items as JCheckBoxes
	 */
	public static class ListRenderer implements ListCellRenderer {
		private JCheckBox checkBox;
		public Component getListCellRendererComponent(JList list, Object value,
				int index, boolean isSelected, boolean cellHasFocus) {
			checkBox = (JCheckBox)value;
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
		public void mousePressed(MouseEvent e) {
			i = ((JList)e.getSource()).locationToIndex(e.getPoint());
			if(i == -1)
				return;

			obj = ((JList)e.getSource()).getModel().getElementAt(i);
			if (obj instanceof JCheckBox) {
				JCheckBox checkbox = (JCheckBox) obj;
				checkbox.setSelected(!checkbox.isSelected());
				((JList)e.getSource()).repaint();
			}
		}
	}
}
