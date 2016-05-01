/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2005-2013 Ausenco Engineering Canada Inc.
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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;

import javax.swing.AbstractCellEditor;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.SwingUtilities;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;

import com.jaamsim.Samples.SampleListInput;
import com.jaamsim.StringProviders.StringProvListInput;
import com.jaamsim.basicsim.Entity;
import com.jaamsim.input.ColourInput;
import com.jaamsim.input.ExpressionInput;
import com.jaamsim.input.FileInput;
import com.jaamsim.input.Input;
import com.jaamsim.input.InputAgent;
import com.jaamsim.input.InputErrorException;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.KeywordIndex;
import com.jaamsim.input.ListInput;
import com.jaamsim.input.Parser;
import com.jaamsim.input.StringInput;
import com.jaamsim.input.StringListInput;
import com.jaamsim.math.Color4d;

/**
 * Class to display information about model objects. <br>
 * displays the results of getInfo for an object
 */
public class EditBox extends FrameBox {

	private static final int ROW_HEIGHT=20;
	private static final int VALUE_COLUMN=2;
	public static final String NONE = EditBox.formatEditorText("None");
	public static final String REQD = EditBox.formatEditorText("Required Input");

	private static EditBox myInstance;  // only one instance allowed to be open
	private Entity currentEntity;
	private final JTabbedPane jTabbedFrame;

	private final TableCellRenderer columnRender = new EditBoxColumnRenderer();

	private String lastCategory = null;

	private EditBox() {
		super( "Input Editor" );
		setDefaultCloseOperation(FrameBox.DISPOSE_ON_CLOSE);
		addWindowListener(FrameBox.getCloseListener("ShowInputEditor"));

		// Set the preferred size of the panes
		jTabbedFrame = new JTabbedPane();
		jTabbedFrame.addChangeListener(new TabListener());
		getContentPane().add(jTabbedFrame);

		setLocation(GUIFrame.COL2_START, GUIFrame.LOWER_START);
		setSize(GUIFrame.COL2_WIDTH, GUIFrame.LOWER_HEIGHT);
	}

	public synchronized static EditBox getInstance() {
		if (myInstance == null)
			myInstance = new EditBox();

		return myInstance;
	}

	@Override
	public void setEntity(Entity entity) {
		if (entity == currentEntity) {
			// Reset the title in case the entity's name has changed
			if (currentEntity != null)
				setTitle("Input Editor - " + currentEntity.getName());
			return;
		}

		if (currentEntity != null) {
			int idx = jTabbedFrame.getSelectedIndex();
			if (idx > -1)
				lastCategory = jTabbedFrame.getTitleAt(idx);
		}

		jTabbedFrame.removeAll();

		currentEntity = entity;
		if (currentEntity == null) {
			setTitle("Input Editor");
			return;
		}
		setTitle("Input Editor - " + currentEntity.getName());

		int initialTab = 0;
		int curTab = 0;
		for (CategoryInputs each : getInputs(currentEntity)) {
			EditTableModel mod = new EditTableModel(each);
			JTable propTable = new EditTable(mod, columnRender);
			JScrollPane jScrollPane = new JScrollPane(propTable);
			jScrollPane.getVerticalScrollBar().setUnitIncrement(ROW_HEIGHT);
			jScrollPane.setColumnHeaderView( propTable.getTableHeader());

			jTabbedFrame.addTab(each.category, null, jScrollPane, null);
			if (each.category.equals(lastCategory))
				initialTab = curTab;

			curTab++;
		}

		if (jTabbedFrame.getTabCount() > 0) {
			jTabbedFrame.setSelectedIndex(initialTab);
		}
	}

	@Override
	public void updateValues(double simTime) {
		if(currentEntity == null)
			return;

		JTable propTable = (JTable)(((JScrollPane)jTabbedFrame.getSelectedComponent()).getViewport().getComponent(0));
		((EditTableModel)propTable.getModel()).fireTableDataChanged();
	}

	private synchronized static void killInstance() {
		myInstance = null;
	}

	@Override
	public void dispose() {
		killInstance();
		super.dispose();
	}

	public static final String scanClassForInputDesc(Class<?> curClass, Object obj, Input<?> in) {
		for (Field f : curClass.getDeclaredFields()) {
			if (!Input.class.isAssignableFrom(f.getType())) {
				continue;
			}

			try {
				f.setAccessible(true);
				if (in != f.get(obj)) {
					continue;
				}

				Keyword key = f.getAnnotation(Keyword.class);
				if (key == null)
					return null;

				return GUIFrame.formatKeywordToolTip(obj.getClass().getSimpleName(), in.getKeyword(),
						key.description(), key.example(), key.exampleList());
			}
			catch (IllegalArgumentException e) {}
			catch (IllegalAccessException e) {}
			catch (SecurityException e) {}
		}
		return null;

	}

	public static final String getInputDesc(Entity ent, Input<?> in) {
		Class<?> current = ent.getClass();

		while (true) {
			String desc = scanClassForInputDesc(current, ent, in);
			if (desc != null) {
				return desc;
			}

			current = current.getSuperclass();
			if (current == Object.class)
				break;
		}

		return null;
	}

	Entity getCurrentEntity() {
		return currentEntity;
	}

	public static String formatEditorText(String str) {
		return String.format("<html><i><font color=\"gray\">%s</font></i></html>", str);
	}

/**
 * Handles inputs that are edited in place.
 *
 */
public static class StringEditor extends CellEditor {
	private final JTextField text;

	public StringEditor(JTable table) {
		super(table);
		text = new JTextField();
	}

	@Override
	public Component getTableCellEditorComponent(JTable table,
			Object value, boolean isSelected, int row, int column) {

		setTableInfo(table, row, column);

		input = (Input<?>)value;
		String val = input.getValueString();
		if (retryString != null) {
			val = retryString;
			retryString = null;
		}
		text.setText( val );
		return text;
	}

	@Override
	public String getValue() {
		return text.getText();
	}
	@Override
	public boolean canRetry() {
		return true;
	}

}

/**
 * Handles file inputs.
 *
 */
public static class FileEditor extends CellEditor
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

	private void setFileInput(FileInput in) {
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
			  table.getColumnModel().getColumn( VALUE_COLUMN ).getWidth() -
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

/**
 * Handles colour inputs.
 *
 */
public static class ColorEditor extends CellEditor
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

/**
 * Handles inputs with drop-down menus.
 *
 */
public static class DropDownMenuEditor extends CellEditor
implements ActionListener {

	private final JComboBox<String> dropDown;

	private boolean retrying;

	public DropDownMenuEditor(JTable table, ArrayList<String> aList) {
		super(table);

		dropDown = new JComboBox<>();
		dropDown.setEditable(true);
		DefaultComboBoxModel<String> model = (DefaultComboBoxModel<String>) dropDown.getModel();

		// Populate the items in the combo box
		for(String each: aList) {

			// Space inside the font name
			if( each.contains(" ") )
				each = String.format("'%s'", each);
			model.addElement(each);
		}

		dropDown.setActionCommand("comboBoxChanged");
		dropDown.addActionListener(this); // Now it is safe to listen
	}

	@Override
	public void actionPerformed(ActionEvent e) {

		// If combo box is edited, this method invokes twice
		if(e.getActionCommand().equals("comboBoxChanged") && !retrying)
			stopCellEditing();
	}

	@Override
	public Component getTableCellEditorComponent(JTable table,
			Object value, boolean isSelected, int row, int column) {

		setTableInfo(table, row, column);

		input = (Input<?>)value;
		String text = input.getValueString();

		if (retryString != null) {
			text = retryString;
			retrying = true;
		}
		retryString = null;

		dropDown.setSelectedItem(text);
		retrying = false;

		return dropDown;
	}

	@Override
	public String getValue() {
		return (String)dropDown.getSelectedItem();
	}

	@Override
	public boolean canRetry() {
		return true;
	}

}

/**
 * Handles inputs where a list of entities can be selected.
 *
 */
public static class ListEditor extends CellEditor
implements ActionListener {

	private final JPanel jPanel;
	private final JTextField text;
	private final JButton listButton;
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
		super(table);

		jPanel = new JPanel(new BorderLayout());

		text = new JTextField();
		jPanel.add(text, BorderLayout.WEST);

		listButton = new JButton(new ImageIcon(
			GUIFrame.class.getResource("/resources/images/dropdown.png")));
		listButton.addActionListener(this);
		listButton.setActionCommand("button");
		listButton.setContentAreaFilled(false);
		jPanel.add(listButton, BorderLayout.EAST);
		caseSensitive = true;
		innerBraces = false;
	}

	@Override
	public String getValue() {
		return text.getText();
	}

	@Override
	public void actionPerformed(ActionEvent e) {

		// OK button
		if("OK".equals(e.getActionCommand())) {
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
			text.setText(sb.toString());
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
			tokens = new ArrayList<>();
		}

		// break the value into single options
		tokens.clear();
		Parser.tokenize(tokens, text.getText());
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

	@Override
	public Component getTableCellEditorComponent(JTable table,
			Object value, boolean isSelected, int row, int column) {

		setTableInfo(table, row, column);

		input = (Input<?>)value;
		text.setText( input.getValueString() );

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

	public void setInnerBraces(boolean bool) {
		innerBraces = bool;
	}
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

public static abstract class CellEditor extends AbstractCellEditor implements TableCellEditor {
	protected final JTable propTable;
	protected Input<?> input;

	private int row;
	private int col;
	private JTable table;
	protected String retryString;

	public CellEditor(JTable table) {
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
	final public JTable getTable() { return table; }

	final public void setRetry(String retryString) {
		this.retryString = retryString;
	}

	protected void setTableInfo(JTable table, int row, int col) {
		this.table = table;
		this.row = row;
		this.col = col;
	}
}

public static class CellListener implements CellEditorListener {

	@Override
	public void editingCanceled(ChangeEvent evt) {}

	@Override
	public void editingStopped(ChangeEvent evt) {

		CellEditor editor = (CellEditor)evt.getSource();
		Input<?> in = (Input<?>)editor.getCellEditorValue();

		final String newValue = editor.getValue();

		// The value has not changed
		if ( in.getValueString().equals(newValue) )
			return;

		// Adjust the user's entry to standardise the syntax
		try {
			String str = newValue.trim();
			Class<?> klass = in.getClass();

			// Add single quotes to String inputs
			if (klass == StringInput.class || klass == FileInput.class || klass == ExpressionInput.class) {
				if (Parser.needsQuoting(str) && !Parser.isQuoted(str))
					str = Parser.addQuotes(str);
			}
			// Keyword
			ArrayList<String> tokens = new ArrayList<>();
			Parser.tokenize(tokens, str, true);

			// Parse the keyword inputs
			KeywordIndex kw = new KeywordIndex(in.getKeyword(), tokens, null);
			InputAgent.processKeyword(EditBox.getInstance().getCurrentEntity(), kw);
		}
		catch (InputErrorException exep) {
			if (editor.canRetry()) {
				String errorString = String.format("Input error:\n%s\n Do you want to continue editing, or reset the input?", exep.getMessage());
				String[] options = { "Edit", "Reset" };
				int reply = JOptionPane.showOptionDialog(null, errorString, "Input Error", JOptionPane.OK_CANCEL_OPTION,
						JOptionPane.ERROR_MESSAGE, null, options, options[0]);
				if (reply == JOptionPane.OK_OPTION) {
					// Any editor that supports retry should implement the following
					final int row = editor.getRow();
					final int col = editor.getCol();
					final EditTable table = (EditTable)editor.getTable();
					SwingUtilities.invokeLater(new Runnable() {
						@Override
						public void run() {
							table.setRetry(newValue, row, col);
							table.editCellAt(row, col);
						}
					});

				} else {
					FrameBox.valueUpdate();
				}
				return;
			}

			GUIFrame.showErrorDialog("Input Error",
					"%s\n" + "Value will be cleared.", exep.getMessage());

			FrameBox.valueUpdate();
			return;

		}
	}
}

private static class TabListener implements ChangeListener {
	@Override
	public void stateChanged(ChangeEvent e) {
		FrameBox.valueUpdate();
	}
}

private static class CategoryInputs {
	final String category;
	final ArrayList<Input<?>> inputs;

	CategoryInputs(String cat, ArrayList<Input<?>> ins) {
		category = cat;
		inputs = ins;
	}
}

	private static ArrayList<CategoryInputs> getInputs(Entity ent) {
		// create a list of Inputs sorted by category
		ArrayList<Input<?>> sortedInputs = new ArrayList<>();
		for (Input<?> in : ent.getEditableInputs()) {
			if (in.getHidden() || in.isSynonym())
				continue;

			int index = sortedInputs.size();
			for (int i = sortedInputs.size() - 1; i >= 0; i--) {
				Input<?> ei = sortedInputs.get(i);
				if (ei.getCategory().equals(in.getCategory())) {
					index = i + 1;
					break;
				}
			}
			sortedInputs.add(index, in);
		}

		String cat = "";
		ArrayList<CategoryInputs> catInputsList = new ArrayList<>();
		ArrayList<Input<?>> inputs = new ArrayList<>();

		// assuming that editable inputs of the same category are adjacent
		for (Input<?> in : sortedInputs) {
			// the first time entering the loop
			if (cat.isEmpty())
				cat = in.getCategory();

			// new category (the previous category inputs is done so add it to the list)
			if ( !cat.equals(in.getCategory()) ) {
				add(catInputsList, inputs, cat);
				inputs = new ArrayList<>();
				cat = in.getCategory();
			}
			inputs.add(in);
		}

		if (inputs.size() != 0) {
			add(catInputsList, inputs, cat);
		}

		return catInputsList;
	}

	private static void add(ArrayList<CategoryInputs> list, ArrayList<Input<?>> inputs, String cat) {
		CategoryInputs catInputs = new CategoryInputs(cat, inputs);

		// Ensure key inputs are always the first tab
		if (catInputs.category.equals("Key Inputs"))
			list.add(0, catInputs);
		else
			list.add(catInputs);
	}

public static class EditTable extends JTable {
	static int col1Width = 150;
	static int col2Width = 100;
	static int col3Width = 150;

	private ColorEditor colorEditor;
	private ListEditor listEditor;

	private String retryString;
	private int retryRow;
	private int retryCol;

	@Override
	public boolean isCellEditable( int row, int column ) {
		return ( column == VALUE_COLUMN ); // Only Value column is editable
	}

	public EditTable(EditTableModel mod, TableCellRenderer colRender) {
		super(mod);

		this.setRowHeight(ROW_HEIGHT);
		this.setRowSelectionAllowed(false);

		getColumnModel().getColumn(0).setWidth(col1Width);
		getColumnModel().getColumn(1).setWidth(col2Width);
		getColumnModel().getColumn(2).setWidth(col3Width);

		getColumnModel().getColumn(0).setCellRenderer(colRender);
		getColumnModel().getColumn(1).setCellRenderer(colRender);
		getColumnModel().getColumn(2).setCellRenderer(colRender);

		this.getTableHeader().setFont(FrameBox.boldFont);
		this.getTableHeader().setReorderingAllowed(false);
	}

	@Override
	public TableCellEditor getCellEditor(int row, int col) {
		Input<?> in = (Input<?>)this.getValueAt(row, col);

		CellEditor ret;

		ArrayList<String> array = in.getValidOptions();

		// 1) Colour input
		if(in instanceof ColourInput) {
			if(colorEditor == null) {
				colorEditor = new ColorEditor(this);
			}
			ret = colorEditor;
		}

		// 2) File input
		else if(in instanceof FileInput) {
			FileEditor fileEditor = new FileEditor(this);
			fileEditor.setFileInput((FileInput)in);
			ret = fileEditor;
		}

		// 3) Normal text
		else if(array == null) {
			ret = new StringEditor(this);
		}

		// 4) Multiple selections from a List
		else if(in instanceof ListInput) {
			if(listEditor == null) {
				listEditor = new ListEditor(this);
				if(in instanceof StringListInput) {
					listEditor.setCaseSensitive(
							((StringListInput)(in)).getCaseSensitive() );
				}
				if (in instanceof SampleListInput || in instanceof StringProvListInput) {
					listEditor.setInnerBraces(true);
				}
			}
			listEditor.setListData(array);
			ret = listEditor;
		}

		// 5) Single selection from a drop down box
		else {
			ret = new DropDownMenuEditor(this, array);
		}

		// If the user is going to retry a failed edit, update the editor with the old value
		if (ret.canRetry() && retryString != null && row == retryRow && col == retryCol ) {
			ret.setRetry(retryString);
		}
		retryString = null;
		return ret;
	}

	@Override
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

	@Override
	public void doLayout() {
		col1Width = getColumnModel().getColumn(0).getWidth();
		col2Width = getColumnModel().getColumn(1).getWidth();
		col3Width = getColumnModel().getColumn(2).getWidth();

		FrameBox.fitTableToLastColumn(this);
	}

	public void setRetry(String rs, int row, int col) {
		retryString = rs;
		retryRow = row;
		retryCol = col;
	}
}


private static class EditTableModel extends AbstractTableModel {
	CategoryInputs inputs;

	EditTableModel(CategoryInputs ci) {
		inputs = ci;
	}

	@Override
	public int getColumnCount() {
		return 3;
	}

	@Override
	public String getColumnName(int column) {
		switch (column) {
		case 0: return "Keyword";
		case 1: return "Default";
		case 2: return "Value";
		}

		return "Unknown";
	}

	@Override
	public int getRowCount() {
		return inputs.inputs.size();
	}

	@Override
	public boolean isCellEditable(int row, int column) {
		return (column == VALUE_COLUMN); // Only Value column is editable
	}

	@Override
	public Object getValueAt(int row, int col) {
		return inputs.inputs.get(row);
	}
}

}
