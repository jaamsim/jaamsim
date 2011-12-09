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
 */package com.sandwell.JavaSimulation3D;

import com.sandwell.JavaSimulation.*;
import com.sandwell.JavaSimulation.Package;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;

import javax.media.j3d.ColoringAttributes;
import javax.swing.DefaultCellEditor;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableModel;

import javax.swing.event.ChangeEvent;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeListener;
import javax.vecmath.Color3f;

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
	private final HelpKeyListener helpKeyListener;

	private int presentPage; // Present tabbed page

	private boolean buildingTable;	    // TRUE if the table is being populated

	private int rowLastVisited;
	private String selectedKeyword;

	/**
	 * Widths of columns in the table of keywords as modified by the user in an edit
	 * session. When the EditBox is re-opened for the next edit (during the same session),
	 * the last modified column widths are used.
	 * <pre>
	 * userColWidth[ 0 ] = user modified width of column 0 (keywords column)
	 * userColWidth[ 1 ] = user modified width of column 1 (defaults units column)
	 * userColWidth[ 2 ] = user modified width of column 2 (values column)
	 */
	private int[] userColWidth = { 200, 100, 500 };

	private EditBox() {

		super( "Input Editor" );
		helpKeyListener = new HelpKeyListener(this);

		setDefaultCloseOperation(FrameBox.HIDE_ON_CLOSE);

		buildingTable = false;

		// Set the preferred size of the panes
		jTabbedPane = new JTabbedPane();
		jTabbedPane.setBackground( INACTIVE_TAB_COLOR );
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

				jTabbedPane.setBackgroundAt( presentPage, INACTIVE_TAB_COLOR );
				presentPage = jTabbedPane.getSelectedIndex();
				jTabbedPane.setBackgroundAt( presentPage, ACTIVE_TAB_COLOR );
				updateValues();
			}
		});

		JLabel helpLabel = new JLabel( "Press F1 for help on any cell", JLabel.CENTER );
		helpLabel.setFont(helpLabel.getFont().deriveFont(9f)); // smaller font size
		getContentPane().add("South", helpLabel);

		pack();
		setLocation(220, 710);
		setSize(1060, 290);
	}


	private static class HelpKeyListener implements KeyListener {
		private final EditBox box;

		HelpKeyListener(EditBox aBox) {
			box = aBox;
		}

		public void keyReleased(KeyEvent e) {
			if (e.getKeyCode() == KeyEvent.VK_F1) {
				box.doHelp();
			}
		}

		public void keyPressed(KeyEvent e) {}
		public void keyTyped(KeyEvent e) {}
	}

	protected void  doHelp() {
		if (currentEntity != null) {
			Simulation.spawnHelp("", "");
			return;
		}

		// Determine the package of the entity
		Package cat = null;
		for( ObjectType type : ObjectType.getAll() ) {
			if( type.getJavaClass() == currentEntity.getClass() ) {
				cat = type.getPackage();
				break;
			}
		}
		if (cat != null && selectedKeyword != null && cat.getHelpSection() != null) {
			String helpName = "::" + cat.getHelpSection().trim() + "#mo_topic_" + selectedKeyword.trim();
			Simulation.spawnHelp(cat.getFilePrefix(), helpName);
		}
	}

	public synchronized static EditBox getInstance() {
		if (myInstance == null)
			myInstance = new EditBox();

		myInstance.setEntity(ObjectSelector.currentEntity);
		return myInstance;
	}

	// ========================================
	// HTextField is for the keyword/value grid
	// ----------------------------------------
	class HTextField extends JTextField {
		public EditBox editBox;
		public JTable propTable;

		public HTextField( EditBox eBox, JTable propTable ) {
			super();
			editBox = eBox;
			this.propTable = propTable;
			propTable.clearSelection();
			this.addKeyListener(helpKeyListener);
		}

		protected void processFocusEvent( FocusEvent fe ) {
			if ( fe.getID() == FocusEvent.FOCUS_GAINED ) {

				if (propTable.getSelectedRow() > -1) {
					Object val = propTable.getValueAt(propTable.getSelectedRow(), 0);
					if (val != null)
						editBox.selectedKeyword = val.toString();
				}
				editBox.rowLastVisited = propTable.getEditingRow();

				// Show in bold font the key string corresponding to the currently selected value
				CellRenderer boldRenderer = editBox.new CellRenderer(true);
				propTable.getColumnModel().getColumn( 0 ).setCellRenderer( boldRenderer ) ;

				// De-select previously selected cell value, if any
				propTable.clearSelection();

				// Select entire text string in the cell currently clicked on
				selectAll();
			}
			else if ( fe.getID() == FocusEvent.FOCUS_LOST  &&
					(fe.getOppositeComponent() != propTable || propTable.getSelectedColumn() != VALUE_COLUMN) ) {

				// Change entire table to plain font
				// Note: all 3 lines of code below are required
				propTable.setFont( propTable.getFont().deriveFont( Font.PLAIN ) );
				CellRenderer plainRenderer = editBox.new CellRenderer(false);
				propTable.getColumnModel().getColumn( 0 ).setCellRenderer( plainRenderer ) ;

				// Make the input modification is applied after loosing the focus
				DefaultCellEditor dce = (DefaultCellEditor)propTable.getDefaultEditor(Object.class);
				dce.stopCellEditing();
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

		JTable propTable;
		propTable = new JTable( new javax.swing.table.DefaultTableModel( numberOfRows, 3 ) {

			public boolean isCellEditable( int row, int column ) {
				return ( column == VALUE_COLUMN ); // Since the table is tabbed, all the values are editable
			}

		});

		propTable.setRowHeight( ROW_HEIGHT );
		propTable.setRowMargin( 1 );
		propTable.getColumnModel().setColumnMargin( 20 );
		propTable.setSelectionBackground( Color.WHITE );
		propTable.setSelectionForeground( Color.BLACK );
		propTable.setAutoResizeMode( JTable.AUTO_RESIZE_NEXT_COLUMN );
		propTable.setRowSelectionAllowed( false );

		// Set DefaultCellEditor that can process FOCUS events.
		propTable.setDefaultEditor( Object.class, new DefaultCellEditor( new HTextField( this, propTable ) ) );
		DefaultCellEditor dce = (DefaultCellEditor)propTable.getDefaultEditor(Object.class);

		// Set click behavior
		dce.setClickCountToStart(1);

		// Set keyword table column headers
		propTable.getColumnModel().getColumn( 0 ).setHeaderValue( "<html> <b> Keyword </b>" );
		propTable.getColumnModel().getColumn( 0 ).setPreferredWidth( userColWidth[ 0 ] );
		propTable.getColumnModel().getColumn( 1 ).setHeaderValue( "<html> <b> Default </b>" );
		propTable.getColumnModel().getColumn( 1 ).setPreferredWidth( userColWidth[ 1 ] );
		propTable.getColumnModel().getColumn( 2 ).setHeaderValue( "<html> <b> Value </b>" );
		propTable.getColumnModel().getColumn( 2 ).setPreferredWidth( userColWidth[ 2 ] );
		propTable.addKeyListener(helpKeyListener);

		// Listen for table changes
		propTable.getModel().addTableModelListener( new MyTableModelListener() );

		return propTable;
	}

	public void setEntity(Entity entity) {
		if(currentEntity == entity)
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
				for( Input<?> in : currentEntity.getEditableInputs() ) {
					if ( category.contains(in.getCategory()) )
						continue;

					category.add( in.getCategory() );
					jTabbedPane.addTab(in.getCategory(), getScrollPaneFor(in.getCategory()));
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
			if( in.getCategory().equals( category ) &&  ! in.isLocked() ) {
				categoryCount++;
			}
		}

		JTable propTable = this.buildProbTable( categoryCount  );
		propTable.getTableHeader().setBackground(HEADER_COLOR);

		int row = 0;

		// fill in keyword and default columns
		for( Input<?> in : currentEntity.getEditableInputs() ) {

			if( ! in.getCategory().equals(category) || in.isLocked() )
				continue;

			propTable.setValueAt( String.format("%s", in.getKeyword()), row, 0 );

			String defValString = EditBox.getDefaultValueStringOf(in);

			propTable.setValueAt( String.format("%s %s", defValString, in.getUnits()), row, 1 );
			row++;
		}

		JPanel jp = new JPanel();
		jp.setLayout(new BorderLayout(5,5));
		jp.add(propTable, BorderLayout.CENTER);
		JScrollPane jScrollPane = new JScrollPane(jp);
		jScrollPane.getVerticalScrollBar().setUnitIncrement(ROW_HEIGHT);
		jScrollPane.setColumnHeaderView( propTable.getTableHeader());
		return jScrollPane;
	}

	public void updateValues() {

		// table has not built yet
		if(buildingTable)
			return;

		// no entity is selected
		if(currentEntity == null) {
			setTitle("Input Editor");
			return;
		}

		buildingTable = true;

		setTitle( String.format("Input Editor - %s", currentEntity) );

		String currentCategory = jTabbedPane.getTitleAt(jTabbedPane.getSelectedIndex());
		JTable propTable = (JTable)((JPanel)((JScrollPane)jTabbedPane.getComponentAt(presentPage)).getViewport().getComponent(0)).getComponent(0);

		int row = 0;
		for( Input<?> in : currentEntity.getEditableInputs() ) {

			if( ! in.getCategory().equals(currentCategory) || in.isLocked() )
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

	public class CellRenderer extends DefaultTableCellRenderer {
		boolean bold;
		boolean italic;
		boolean underline;

		public CellRenderer(boolean setBold) {
			this(setBold, false, false );
		}

		public CellRenderer(boolean setBold, boolean setItalic, boolean setUnderline ) {
			bold      = setBold;
			italic    = setItalic;
			underline = setUnderline;
		}

		public Component getTableCellRendererComponent
		(JTable table, Object value, boolean isSelected,
				boolean hasFocus, int row, int column)
		{
			Component cell = super.getTableCellRendererComponent
			(table, value, isSelected, hasFocus, row, column);

			JTableHeader header = table.getTableHeader();
			final Font boldFont = header.getFont().deriveFont(Font.BOLD);
			final Font plainFont = header.getFont().deriveFont(Font.PLAIN);

			cell.setFont( plainFont ) ; //set the font here

			if ( row == EditBox.this.rowLastVisited ) {
				if ( bold ) {
					cell.setFont( boldFont );
				}
				else {
					cell.setFont( plainFont );
				}
			}

			return cell;
		}
	}


	private void updateGraphics() {
		if( currentEntity instanceof DisplayEntity ) {
			((DisplayEntity)currentEntity).enterRegion();
			((DisplayEntity)currentEntity).initializeGraphics();
			((DisplayEntity)currentEntity).setupGraphics();
			((DisplayEntity)currentEntity).updateGraphics();
			GraphicsUpdateBehavior.forceUpdate = true;
		}
	}

	private static String getDefaultValueStringOf(Input<?> in) {
		String defValString = "{ }";
		Object defValue = in.getDefaultValue();
		if (defValue != null) {
			defValString = defValue.toString();
			if(defValue instanceof ArrayList<?>) {
				defValString = defValString.substring(1, defValString.length()-1);
			}
			else if(defValue instanceof ColoringAttributes) {
				Color3f color3f = new Color3f();
				((ColoringAttributes)defValue).getColor(color3f);
				defValString = color3f.toString();
			}
		}
		return defValString;
	}

	public static void processEntity_Keyword_Value(Entity ent, String keyword, String value){
		Input<?> in = ent.getInput( keyword );
		in.setEdited(true);

		ArrayList<String> tokens = new ArrayList<String>();
		InputAgent.tokenizeString(tokens, value);
		if(! InputAgent.enclosedByBraces(tokens) ) {
			tokens.add(0, "{");
			tokens.add("}");
		}
		tokens.add(0, ent.getInputName());
		tokens.add(1, keyword);

		Vector data = new Vector(tokens.size());
		data.addAll(tokens);
		InputAgent.processData(data);
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

			String currentKeyword = ((String)model.getValueAt( row, 0 )).trim();
			Input<?> in = currentEntity.getInput( currentKeyword );

			// The value has not changed
			if(in.getValueString().equals(model.getValueAt( row, 2 ))) {
				return;
			}

			try {

				// Back to default value
				if( data.toString().isEmpty() ) {
					processEntity_Keyword_Value(currentEntity, currentKeyword, getDefaultValueStringOf(in));
				}
				else {
					processEntity_Keyword_Value(currentEntity, currentKeyword, data.toString());
				}
			} catch (InputErrorException exep) {

				JOptionPane pane = new JOptionPane( String.format("%s; value will be cleared", exep.getMessage()),
						JOptionPane.ERROR_MESSAGE );
				JDialog errorBox = pane.createDialog( EditBox.this, "Input Error" );
				errorBox.setModal(true);
				errorBox.setVisible(true);
				FrameBox.valueUpdate();
				return;
			}
			if (in.getCategory().equals("Graphics") ) {
				updateGraphics();
			}
		}
	}
}
