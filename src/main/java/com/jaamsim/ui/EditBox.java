/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2005-2013 Ausenco Engineering Canada Inc.
 * Copyright (C) 2016-2025 JaamSim Software Inc.
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

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseEvent;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import javax.swing.AbstractAction;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JViewport;
import javax.swing.KeyStroke;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;

import com.jaamsim.ColourProviders.ColourProvInput;
import com.jaamsim.Samples.SampleListInput;
import com.jaamsim.StringProviders.StringProvListInput;
import com.jaamsim.basicsim.Entity;
import com.jaamsim.input.ColourInput;
import com.jaamsim.input.FileInput;
import com.jaamsim.input.Input;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.ListInput;
import com.jaamsim.input.StringListInput;

/**
 * Class to display information about model objects. <br>
 * displays the results of getInfo for an object
 */
public class EditBox extends FrameBox {

	public static final int ROW_HEIGHT=20;
	public static final int VALUE_COLUMN=2;
	public static final String NONE = EditBox.formatEditorText("None");
	public static final String REQD = EditBox.formatEditorText("Required Input");

	private static EditBox myInstance;  // only one instance allowed to be open
	private Entity currentEntity;
	private final JTabbedPane jTabbedFrame;

	private final ArrayList<EditTable> editTableList;

	private final TableCellRenderer columnRender = new EditBoxColumnRenderer();

	private String lastCategory = null;
	private String lastKeyword = null;
	private Point lastViewPosition = null;

	private EditBox() {
		super( "Input Editor" );
		setDefaultCloseOperation(FrameBox.DISPOSE_ON_CLOSE);
		addWindowListener(FrameBox.getCloseListener("ShowInputEditor"));

		editTableList = new ArrayList<>();

		// Provide tabs for the editor
		jTabbedFrame = new JTabbedPane();
		getContentPane().add(jTabbedFrame);

		// Save changes to the editor's size and position
		addComponentListener(FrameBox.getSizePosAdapter(this, "InputEditorSize", "InputEditorPos"));
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

		// Save the previous category name and scroll position
		if (currentEntity != null) {
			int idx = jTabbedFrame.getSelectedIndex();
			if (idx > -1) {
				lastCategory = jTabbedFrame.getTitleAt(idx);
				JScrollPane jScrollPane = (JScrollPane) jTabbedFrame.getComponentAt(idx);
				lastViewPosition = jScrollPane.getViewport().getViewPosition();
			}
		}

		if (jTabbedFrame == null)
			return;
		jTabbedFrame.removeAll();

		currentEntity = entity;
		if (currentEntity == null) {
			setTitle("Input Editor");
			return;
		}
		setTitle("Input Editor - " + currentEntity.getName());

		int initialTab = 0;
		int curTab = 0;
		editTableList.clear();
		for (CategoryInputs each : getInputs(currentEntity)) {
			EditTableModel mod = new EditTableModel(each);
			EditTable propTable = new EditTable(currentEntity, curTab, mod, columnRender);
			editTableList.add(propTable);
			JScrollPane jScrollPane = new JScrollPane(propTable);
			jScrollPane.getVerticalScrollBar().setUnitIncrement(ROW_HEIGHT);
			jScrollPane.setColumnHeaderView( propTable.getTableHeader());

			jTabbedFrame.addTab(each.category, null, jScrollPane, null);
			if (each.category.equals(lastCategory))
				initialTab = curTab;

			curTab++;
		}

		// Set the selected category tab
		if (jTabbedFrame.getTabCount() > 0)
			jTabbedFrame.setSelectedIndex(initialTab);

		// Set the selected keyword
		EditTable table = editTableList.get(initialTab);
		int row = table.getRowForKeyword(lastKeyword);
		table.getSelectionModel().setSelectionInterval(row, row);

		// Scroll the table to the same position
		if (row == 0 || lastViewPosition == null) {
			Rectangle cellRectangle = table.getCellRect(row, 0, true);
			table.scrollRectToVisible(cellRectangle);
		}
		else {
			JViewport viewport = (JViewport) table.getParent();
			viewport.setViewPosition(lastViewPosition);
		}
	}

	public void setLastKeyword(String keyword) {
		lastKeyword = keyword;
	}

	@Override
	public void updateValues(double simTime) {
		if(currentEntity == null)
			return;

		JScrollPane scrollPane = (JScrollPane) jTabbedFrame.getSelectedComponent();
		EditTable propTable = (EditTable)(scrollPane.getViewport().getComponent(0));

		int row = propTable.getSelectedRow();
		int horizontalVal = scrollPane.getHorizontalScrollBar().getValue();
		int verticalVal = scrollPane.getVerticalScrollBar().getValue();

		// Redraw the table
		((EditTableModel)propTable.getModel()).fireTableDataChanged();

		// Restore the selected cell
		if (row != -1)
			propTable.changeSelection(row, VALUE_COLUMN, false, false);

		// Restore the scroll bar positions
		scrollPane.getHorizontalScrollBar().setValue(horizontalVal);
		scrollPane.getVerticalScrollBar().setValue(verticalVal);

		// Update the value in the selected cell
		if (propTable.getPresentCellEditor() != null) {
			propTable.getPresentCellEditor().updateValue();
		}
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

				String[] examples = key.exampleList();
				if (examples.length == 0)
					examples = in.getExamples();

				return GUIFrame.formatKeywordToolTip(obj.getClass().getSimpleName(),
						in.getKeyword(), key.description(), in.getValidInputDesc(),
						examples);
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

	public void setTab(int tab) {
		jTabbedFrame.setSelectedIndex(tab);
	}

	public static String formatEditorText(String str) {
		return String.format("<html><i><font color=\"gray\">%s</font></i></html>", str);
	}

	public static String formatErrorText(String str) {
		return String.format("<html><font color=\"red\">%s</font></html>", str);
	}

	public static String formatLockedText(String str) {
		str = GUIFrame.html_replace(str);
		return String.format("<html><font color=\"gray\">%s</font></html>", str);
	}

	public static String formatInheritedText(String str) {
		str = GUIFrame.html_replace(str);
		return String.format("<html><font color=\"blue\">%s</font></html>", str);
	}

private static class CategoryInputs {
	final String category;
	final ArrayList<Input<?>> inputs;
	final int sequence;

	CategoryInputs(String cat, int seq, ArrayList<Input<?>> ins) {
		category = cat;
		sequence = seq;
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

		Collections.sort(catInputsList, categorySortOrder);

		return catInputsList;
	}

	private static void add(ArrayList<CategoryInputs> list, ArrayList<Input<?>> inputs, String cat) {

		int seq = 2;
		if (cat.equals(Entity.KEY_INPUTS))
			seq = 0;
		else if (cat.equals(Entity.OPTIONS))
			seq = 1;
		else if (cat.equals(Entity.THRESHOLDS))
			seq = 3;
		else if (cat.equals(Entity.MAINTENANCE))
			seq = 4;
		else if (cat.equals(Entity.FORMAT))
			seq = 5;
		else if (cat.equals(Entity.GRAPHICS))
			seq = 6;

		list.add(new CategoryInputs(cat, seq, inputs));
	}

	private static class CategoryComparator implements Comparator<CategoryInputs> {
		@Override
		public int compare(CategoryInputs cat0, CategoryInputs cat1) {
			return Integer.compare(cat0.sequence, cat1.sequence);
		}
	}

	public static final Comparator<CategoryInputs> categorySortOrder = new CategoryComparator();

public static class EditTable extends JTable {
	private final Entity entity;
	private final int tab;

	static int col1Width = 150;
	static int col2Width = 100;
	static int col3Width = 150;

	private String retryString;
	private int retryRow;
	private int retryCol;

	private CellEditor presentCellEditor;

	public EditTable(Entity ent, int tb, EditTableModel mod, TableCellRenderer colRender) {
		super(mod);
		entity = ent;
		tab = tb;

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

		this.setPresentCellEditor(null);

		addFocusListener(new FocusListener() {
			@Override
			public void focusGained(FocusEvent e) {

				// Re-select the cell if an entry is being edited after an error
				if (retryString != null) {
					changeSelection(retryRow, retryCol, false, false);
				}

				// Start the CellEditor for the selected input
				int row = getSelectedRow();
				if (row == -1)
					return;
				if (presentCellEditor == null || retryString != null)
					editCellAt(row, VALUE_COLUMN);

				// Direct the inputs to the CellEditor
				if (getEditorComponent() == null)
					return;
				getEditorComponent().requestFocusInWindow();

				// Save the keyword for the input that is being edited
				Input<?> in = (Input<?>) getValueAt(row, 0);
				if (in != null) {
					EditBox.getInstance().setLastKeyword(in.getKeyword());
				}
			}
			@Override
			public void focusLost(FocusEvent e) {}
		});

		// Prevent the default action of selecting first row when Enter is pressed on the last row
		getInputMap(JTable.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(
				KeyStroke.getKeyStroke("ENTER"), "enter");
		getActionMap().put("enter", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (presentCellEditor != null)
					presentCellEditor.stopCellEditing();
				selectNextCell();
			}
		});
	}

	@Override
	public boolean isCellEditable( int row, int column ) {
		return ( column == VALUE_COLUMN ); // Only Value column is editable
	}

	@Override
	public TableCellEditor getCellEditor(int row, int col) {
		Input<?> in = (Input<?>)this.getValueAt(row, col);

		// Is the input locked at its present value?
		if (in.isLocked())
			return null;

		CellEditor ret;

		ArrayList<String> array = in.getValidOptions(entity);
		int width = getColumnModel().getColumn(VALUE_COLUMN).getWidth()
				- getColumnModel().getColumnMargin();
		int height = getRowHeight();

		// 1) Colour input
		if (in instanceof ColourInput || in instanceof ColourProvInput) {
			ret = new ColorEditor(width, height);
		}

		// 2) File input
		else if (in instanceof FileInput) {
			ret = new FileEditor(width, height);
		}

		// 3) Multiple selections from a List
		else if (in instanceof ListInput && array != null && !array.isEmpty()) {
			ListEditor listEditor = new ListEditor(width, height, array);
			if (in instanceof StringListInput) {
				listEditor.setCaseSensitive( ((StringListInput)(in)).getCaseSensitive() );
			}
			if (in instanceof SampleListInput || in instanceof StringProvListInput) {
				listEditor.setInnerBraces(true);
			}
			ret = listEditor;
		}

		// 4) Expression Builder
		else if (in.useExpressionBuilder()) {
			ret = new ExpressionEditor(width, height);
		}

		// 5) Normal text
		else if (array == null) {
			ret = new StringEditor(width, height);
		}

		// 6) Single selection from a drop down box
		else {
			ret = new DropDownMenuEditor(width, height, array);
		}

		// If the user is going to retry a failed edit, update the editor with the old value
		if (ret.canRetry() && retryString != null && row == retryRow && col == retryCol) {
			ret.setRetry(retryString);
		}
		retryString = null;

		setPresentCellEditor(ret);
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
			return "";

		TableModel model = getModel();
		Input<?> in = (Input<?>)model.getValueAt(rowIndex, 0);
		return EditBox.getInputDesc(EditBox.getInstance().getCurrentEntity(), in);
	}

	@Override
	public Point getToolTipLocation(MouseEvent e) {
		int row = rowAtPoint(e.getPoint());
		int y = getCellRect(row, 0, true).getLocation().y;
		return new Point(col1Width + col2Width, y);
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

	public Entity getEntity() {
		return entity;
	}

	public int getTab() {
		return tab;
	}

	public CellEditor getPresentCellEditor() {
		return presentCellEditor;
	}

	public void setPresentCellEditor(CellEditor editor) {
		presentCellEditor = editor;
	}

	public void selectNextCell() {
		int row = getSelectedRow();
		row = Math.min(row + 1, getModel().getRowCount() - 1);
		changeSelection(row, VALUE_COLUMN, false, false);
	}

	public int getRowForKeyword(String keyword) {
		int selectedRow = 0;
		for (int row = 0; row < getModel().getRowCount(); row++) {
			Input<?> in = (Input<?>) getModel().getValueAt(row, 0);
			if (in.getKeyword().equals(keyword)) {
				selectedRow = row;
				break;
			}
		}
		return selectedRow;
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
