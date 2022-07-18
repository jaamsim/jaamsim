/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2004-2013 Ausenco Engineering Canada Inc.
 * Copyright (C) 2019-2020 JaamSim Software Inc.
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

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.ConcurrentModificationException;

import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;

import com.jaamsim.Graphics.PolylineInfo;
import com.jaamsim.basicsim.Entity;

/**
 * Class to display information about model objects. <br>
 * displays the results of getInfo for an object
 */
public class PropertyBox extends FrameBox {
	private static PropertyBox myInstance;  // only one instance allowed to be open
	private Entity currentEntity;
	private final JTabbedPane jTabbedFrame = new JTabbedPane();
	private static int MAX_ARRAY_ENTRIES_TO_DISPLAY = 10;

	public PropertyBox() {
		super("Property Viewer");
		setDefaultCloseOperation(FrameBox.DISPOSE_ON_CLOSE);
		addWindowListener(FrameBox.getCloseListener("ShowPropertyViewer"));

		jTabbedFrame.addChangeListener(new TabListener());
		getContentPane().add(jTabbedFrame);

		addComponentListener(FrameBox.getSizePosAdapter(this, "PropertyViewerSize", "PropertyViewerPos"));
	}

	/**
	 * Returns the only instance of the property box
	 */
	public synchronized static PropertyBox getInstance() {
		if (myInstance == null)
			myInstance = new PropertyBox();

		return myInstance;
	}

	@Override
	public void setEntity(Entity entity) {

		if (currentEntity == entity)
			return;

		int prevTab = jTabbedFrame.getSelectedIndex();
		jTabbedFrame.removeAll();

		currentEntity = entity;
		if (currentEntity == null) {
			setTitle("Property Viewer");
			return;
		}
		setTitle("Property Viewer - " + currentEntity.getName());

		ArrayList<ClassFields> cFields = getFields(entity);
		for (int i = 0; i < cFields.size(); i++) {
			// The properties in the current page
			ClassFields cf = cFields.get(i);
			PropertyTableModel mod = new PropertyTableModel(entity, cf.fields);
			PropertyTable tab = new PropertyTable(mod);
			JScrollPane scroll = new JScrollPane(tab);

			jTabbedFrame.addTab(cf.klass.getSimpleName(), scroll);
		}

		if( prevTab > -1 && jTabbedFrame.getTabCount() > prevTab - 1 ) {
			jTabbedFrame.setSelectedIndex(prevTab);
		}
	}

	@Override
	public void updateValues(double simTime) {
		if(currentEntity == null)
			return;

		JTable propTable = (JTable)(((JScrollPane)jTabbedFrame.getSelectedComponent()).getViewport().getComponent(0));
		((PropertyTableModel)propTable.getModel()).fireTableDataChanged();
	}

	private synchronized static void killInstance() {
		myInstance = null;
	}

	@Override
	public void dispose() {
		killInstance();
		super.dispose();
	}

private static class TabListener implements ChangeListener {
	@Override
	public void stateChanged(ChangeEvent e) {
		GUIFrame.updateUI();
	}
}

private static class ClassFields implements Comparator<Field> {
	final Class<?> klass;
	final ArrayList<Field> fields;

	ClassFields(Class<?> aKlass) {
		klass = aKlass;

		Field[] myFields = klass.getDeclaredFields();
		fields = new ArrayList<>(myFields.length);
		for (Field each : myFields) {
			String name = each.getName();
			// Static variables are all capitalized (ignore them)
			if (name.toUpperCase().equals(name))
				continue;

			each.setAccessible(true);
			fields.add(each);
		}
		Collections.sort(fields, this);
	}

	@Override
	public int compare(Field f1, Field f2) {
		return f1.getName().compareToIgnoreCase(f2.getName());
	}
}

	private static ArrayList<ClassFields> getFields(Entity object) {
		if (object == null)
			return new ArrayList<>(0);

		return getFields(object.getClass());
	}

	private static ArrayList<ClassFields> getFields(Class<?> klass) {
		if (klass == null || klass.getSuperclass() == null)
			return new ArrayList<>();

		ArrayList<ClassFields> cFields = getFields(klass.getSuperclass());
		cFields.add(new ClassFields(klass));
		return cFields;
	}

	static String format(Object value) {
		if (value == null)
			return "<null>";

		if (value instanceof double[])
			return Arrays.toString((double[])value);

		if (value instanceof double[][])
			return Arrays.deepToString((double[][])value);

		if (value instanceof int[])
			return Arrays.toString((int[])value);

		if (value instanceof long[])
			return Arrays.toString((long[])value);

		if (value instanceof PolylineInfo[]) {
			return Arrays.toString((PolylineInfo[])value);
		}

		try {
			// ArrayLists must be converted to a String one element at a time
			if (value instanceof ArrayList) {
				@SuppressWarnings("unchecked")
				ArrayList<Object> array = (ArrayList<Object>) value;
				if (array.isEmpty())
					return "[]";
				StringBuilder sb = new StringBuilder();
				sb.append("[");
				if (array.get(0) == null) {
					sb.append("null");
				}
				else {
					sb.append(array.get(0).toString());
				}

				int n = Math.min(MAX_ARRAY_ENTRIES_TO_DISPLAY, array.size());
				for (int i=1; i<n; i++) {
					sb.append(", ");
					if (array.get(i) == null) {
						sb.append("null");
					}
					else {
						sb.append(array.get(i).toString());
					}
				}
				if (n < array.size())
					sb.append(", ... ");
				sb.append("]");
				return sb.toString();
			}

			// Every other type of object
			return value.toString();
		}
		catch (ConcurrentModificationException e) {
			return "";
		}
	}

private static class PropertyTable extends JTable {
	static int col1Width = 150;
	static int col2Width = 100;
	static int col3Width = 100;

	PropertyTable(TableModel model) {
		super(model);

		setDefaultRenderer(Object.class, colRenderer);

		getColumnModel().getColumn(0).setWidth(col1Width);
		getColumnModel().getColumn(1).setWidth(col2Width);
		getColumnModel().getColumn(2).setWidth(col3Width);

		getTableHeader().setFont(FrameBox.boldFont);
		getTableHeader().setReorderingAllowed(false);
	}

	@Override
	public void doLayout() {
		col1Width = getColumnModel().getColumn(0).getWidth();
		col2Width = getColumnModel().getColumn(1).getWidth();
		col3Width = getColumnModel().getColumn(2).getWidth();

		FrameBox.fitTableToLastColumn(this);
	}
}

private static class PropertyTableModel extends AbstractTableModel {
	Entity ent;
	ArrayList<Field> fields;

	PropertyTableModel(Entity e, ArrayList<Field> cf) {
		ent = e;
		fields = cf;
	}

	@Override
	public int getColumnCount() {
		return 3;
	}

	@Override
	public String getColumnName(int column) {
		switch (column) {
		case 0: return "Property";
		case 1: return "Type";
		case 2: return "Value";
		}

		return "Unknown";
	}

	@Override
	public int getRowCount() {
		return fields.size();
	}

	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex) {
		return false;
	}

	@Override
	public Object getValueAt(int row, int col) {
		Field field = fields.get(row);
		if (col == 0)
			return field.getName();

		if (col == 1)
			return field.getType().getSimpleName();

		try {
			Object o = field.get(ent);
			return format(o);
		}
		catch (SecurityException e) {}
		catch (IllegalArgumentException e) {}
		catch (IllegalAccessException e) {}

		System.out.println("Failure to reflect field:" + field.getName());
		return "Failure to reflect field value";
	}
}
}
