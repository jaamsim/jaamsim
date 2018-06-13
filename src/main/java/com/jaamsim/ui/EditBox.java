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

import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseEvent;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;

import com.jaamsim.Samples.SampleListInput;
import com.jaamsim.StringProviders.StringProvListInput;
import com.jaamsim.basicsim.Entity;
import com.jaamsim.basicsim.Simulation;
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

		setLocation(Simulation.getInputEditorPos().get(0), Simulation.getInputEditorPos().get(1));
		setSize(Simulation.getInputEditorSize().get(0), Simulation.getInputEditorSize().get(1));

		addComponentListener(new ComponentAdapter() {

			@Override
			public void componentMoved(ComponentEvent e) {
				Simulation.setInputEditorPos(getLocation().x, getLocation().y);
			}

			@Override
			public void componentResized(ComponentEvent e) {
				Simulation.setInputEditorSize(getSize().width, getSize().height);
			}
		});
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

				return GUIFrame.formatKeywordToolTip(obj.getClass().getSimpleName(),
						in.getKeyword(), key.description(), in.getValidInputDesc(),
						key.exampleList());
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

	public static String formatErrorText(String str) {
		return String.format("<html><font color=\"red\">%s</font></html>", str);
	}

private static class TabListener implements ChangeListener {
	@Override
	public void stateChanged(ChangeEvent e) {
		GUIFrame.updateUI();
	}
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

		int seq = 100;
		if (cat.equals(Entity.KEY_INPUTS))
			seq = 0;
		else if (cat.equals(Entity.THRESHOLDS))
			seq = 1;
		else if (cat.equals(Entity.MAINTENANCE))
			seq = 2;
		else if (cat.equals(Entity.GRAPHICS))
			seq = 101;

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
		if (in instanceof ColourInput) {
			if(colorEditor == null) {
				colorEditor = new ColorEditor(this);
			}
			ret = colorEditor;
		}

		// 2) File input
		else if (in instanceof FileInput) {
			ret = new FileEditor(this);
		}

		// 3) Expression Builder
		else if (in.useExpressionBuilder()) {
			ret = new ExpressionEditor(this);
		}

		// 4) Normal text
		else if (array == null) {
			ret = new StringEditor(this);
		}

		// 5) Multiple selections from a List
		else if (in instanceof ListInput) {
			if(listEditor == null) {
				listEditor = new ListEditor(this, array);
				if (in instanceof StringListInput) {
					listEditor.setCaseSensitive(
							((StringListInput)(in)).getCaseSensitive() );
				}
				if (in instanceof SampleListInput || in instanceof StringProvListInput) {
					listEditor.setInnerBraces(true);
				}
			}
			ret = listEditor;
		}

		// 6) Single selection from a drop down box
		else {
			ret = new DropDownMenuEditor(this, array);
		}

		// If the user is going to retry a failed edit, update the editor with the old value
		if (ret.canRetry() && retryString != null && row == retryRow && col == retryCol) {
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
