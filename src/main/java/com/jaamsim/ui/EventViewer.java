/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2009-2011 Ausenco Engineering Canada Inc.
 * Copyright (C) 2017-2020 JaamSim Software Inc.
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
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map.Entry;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JToggleButton;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

import com.jaamsim.basicsim.Entity;
import com.jaamsim.events.EventData;
import com.jaamsim.events.EventManager;
import com.jaamsim.events.EventTraceListener;
import com.jaamsim.events.ProcessTarget;
import com.jaamsim.units.TimeUnit;
import com.jaamsim.units.Unit;

/**
 * Class to display, and control the execution of events in the EventManager
 * <p>
 * EventViewer displays:<br>
 * the curent event time<br>
 * the events for the current event time<br>
 * the wait until events<br>
 * the pending events for all upcoming event times<br>
 * <p>
 * EventViewer provides:<br>
 * the ability to view pending events <br>
 * the ability to execute all events for the next timestep (including wait until events)<br>
 * the ability to execute a single event <br>
 * the ability to execute only waituntil events <br>
 */
public class EventViewer extends FrameBox implements EventTraceListener {
	private static EventViewer myInstance;
	private ArrayList<EventData> retiredEventDataList;
	private boolean dirty;
	private String timeUnit;
	private HashMap <String, ProfileData> nanosMap;
	private HashMap <String, ProfileData> classNanosMap;
	private double startTime;

	private JTabbedPane jTabbedFrame;
	private JTable eventList;
	private JScrollPane sp;
	private JTable condList;
	private JScrollPane condSp;
	private JTable profList;
	private JScrollPane profSp;
	private EventManager evtMan;
	private JToggleButton conditionalsButton;
	private JToggleButton classButton;

	private long nanoseconds;
	private EventData retiredEvent;

	private static final String[] headers= {"Ticks", "Time", "Pri", "Description", "State", "Nanos"};
	private static final int[] colWidth = {100, 80, 30, 160, 80, 60};

	private static final String[] profHeaders= {"Event Type", "% of Total Time", "Rate", "Avg. Nanos"};
	private static final int[] profColWidth = {200, 100, 100, 100};

	private static final int MAX_RETIRED_EVENTS = 1000;
	private static final int SCROLL_POSITION = 5;

	private static final String STATE_COMPLETED   = "Completed";    // event executed
	private static final String STATE_INTERRUPTED = "Interrupted";  // event executed early
	private static final String STATE_TERMINATED  = "Terminated";   // event killed
	private static final String STATE_EVALUATED   = "Evaluated";    // condition evaluated

	private static final Color COLOR_COMPLETED   = new Color(144, 238, 144);  // light green
	private static final Color COLOR_INTERRUPTED = new Color(224, 255, 255);  // light cyan
	private static final Color COLOR_TERMINATED  = new Color(240, 128, 128);  // light coral
	private static final Color COLOR_EVALUATED   = new Color(255, 255, 224);  // light yellow

	public EventViewer( EventManager em ) {
		super("Event Viewer");
		setDefaultCloseOperation( FrameBox.DISPOSE_ON_CLOSE );
		addWindowListener(FrameBox.getCloseListener("ShowEventViewer"));

		retiredEventDataList = new ArrayList<>();
		nanosMap = new HashMap <>();
		classNanosMap = new HashMap <>();
		startTime = GUIFrame.getJaamSimModel().getSimTime();

		evtMan = em;
		evtMan.pause();
		evtMan.setTraceListener(this);

		timeUnit = Unit.getDisplayedUnit(TimeUnit.class);

		// Next Event Button
		JButton nextEventButton = new JButton( "Next Event" );
		nextEventButton.addActionListener( new ActionListener() {
			@Override
			public void actionPerformed( ActionEvent e ) {
				evtMan.nextOneEvent(GUIFrame.getJaamSimModel().getSimulation().getPauseTime());
			}
		});
		nextEventButton.setToolTipText(GUIFrame.formatToolTip("Next Event",
				"Executes a single event from the future event list."));

		// Next Time Button
		JButton nextTimeButton = new JButton( "Next Time" );
		nextTimeButton.addActionListener( new ActionListener() {
			@Override
			public void actionPerformed( ActionEvent e ) {
				evtMan.nextEventTime(GUIFrame.getJaamSimModel().getSimulation().getPauseTime());
			}
		});
		nextTimeButton.setToolTipText(GUIFrame.formatToolTip("Next Time",
				"Executes all the events from the future event list that are scheduled for the "
				+ "next event time. The conditional events are then executed along with any new "
				+ "events that have been scheduled for this time."));

		// Clear Events Button
		JButton clearButton = new JButton( "Clear Events" );
		clearButton.addActionListener( new ActionListener() {
			@Override
			public void actionPerformed( ActionEvent e ) {
				retiredEventDataList.clear();
				updateEvents();
			}
		});
		clearButton.setToolTipText(GUIFrame.formatToolTip("Clear Events",
				"Removes the completed events from the viewer."));

		// Hide Conditionals Button
		conditionalsButton = new JToggleButton( "Hide Conditional Evaluation" );
		conditionalsButton.addActionListener( new ActionListener() {
			@Override
			public void actionPerformed( ActionEvent e ) {
				nextEventButton.requestFocusInWindow();
				updateEvents();
			}
		});
		conditionalsButton.setToolTipText(GUIFrame.formatToolTip("Hide Conditional Evaluation",
				"Hides the entries for the evaluation of conditional events."));

		// Tabs
		jTabbedFrame = new JTabbedPane();
		jTabbedFrame.addChangeListener(new TabListener());
		getContentPane().add(jTabbedFrame);

		// Button Bar
		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout( new FlowLayout( FlowLayout.LEFT ) );
		buttonPanel.add( nextEventButton );
		buttonPanel.add( nextTimeButton );
		buttonPanel.add( clearButton );
		buttonPanel.add( conditionalsButton );

		// Event List
		eventList = new EventTable(new DefaultTableModel(0, headers.length));
		sp = new JScrollPane();
		sp.getViewport().add(eventList);
		sp.setPreferredSize(new Dimension( 800, 300 ));

		// Event Pane
		JPanel eventPanel = new JPanel();
		eventPanel.setLayout( new BorderLayout() );
		eventPanel.add(buttonPanel, BorderLayout.NORTH);
		eventPanel.add(sp, BorderLayout.CENTER);
		jTabbedFrame.addTab("Future Events", null, eventPanel, null);

		// Conditionals List
		condList = new JTable(new DefaultTableModel(0, 1));
		condList.getColumnModel().getColumn(0).setHeaderValue("Description");
		condList.getTableHeader().setFont(FrameBox.boldFont);
		condList.setDefaultRenderer(Object.class, colRenderer);
		condSp = new JScrollPane();
		condSp.getViewport().add(condList);
		condSp.setPreferredSize(new Dimension( 800, 300 ));
		jTabbedFrame.addTab("Conditional Events", null, condSp, null);

		// Clear Results Button
		JButton clearProfButton = new JButton( "Clear Results" );
		clearProfButton.addActionListener( new ActionListener() {
			@Override
			public void actionPerformed( ActionEvent e ) {
				nanosMap.clear();
				classNanosMap.clear();
				startTime = GUIFrame.getJaamSimModel().getSimTime();
				updateProfile();
			}
		});
		clearProfButton.setToolTipText(GUIFrame.formatToolTip("Clear Results",
				"Removes the execution time results."));

		// Show Class Results Button
		classButton = new JToggleButton( "Show Class Results" );
		classButton.addActionListener( new ActionListener() {
			@Override
			public void actionPerformed( ActionEvent e ) {
				clearProfButton.requestFocusInWindow();
				updateProfile();
			}
		});
		classButton.setToolTipText(GUIFrame.formatToolTip("Show Class Results",
				"Displays the execution time results by class."));

		// Profiler Button Bar
		JPanel profButtonPanel = new JPanel();
		profButtonPanel.setLayout( new FlowLayout( FlowLayout.LEFT ) );
		profButtonPanel.add( clearProfButton );
		profButtonPanel.add( classButton );

		// Profiler List
		profList = new ProfileTable(new DefaultTableModel(0, profHeaders.length));
		profList.setDefaultRenderer(Object.class, colRenderer);
		profSp = new JScrollPane();
		profSp.getViewport().add(profList);
		profSp.setPreferredSize(new Dimension( 800, 300 ));

		// Profiler Pane
		JPanel profPanel = new JPanel();
		profPanel.setLayout( new BorderLayout() );
		profPanel.add(profButtonPanel, BorderLayout.NORTH);
		profPanel.add(profSp, BorderLayout.CENTER);
		jTabbedFrame.addTab("Execution Time Profile", null, profPanel, null);

		pack();

		addComponentListener(FrameBox.getSizePosAdapter(this, "EventViewerSize", "EventViewerPos"));

		// Display the viewer
		setVisible(true);
		setDirty(true);
	}

	/**
	 * Returns the only instance of the EventViewer
	 */
	public synchronized static EventViewer getInstance() {
		if (myInstance == null)
			myInstance = new EventViewer(GUIFrame.getJaamSimModel().getEventManager());

		return myInstance;
	}

	public static boolean hasInstance() {
		return myInstance != null;
	}

	private synchronized static void killInstance() {
		myInstance = null;
	}

	private boolean isHideConditionals() {
		return conditionalsButton.isSelected();
	}

	@Override
	public void reset() {
		super.reset();
		retiredEventDataList.clear();
		nanosMap.clear();
		classNanosMap.clear();
		startTime = 0.0d;
		setDirty(true);
	}

	@Override
	public void dispose() {
		super.dispose();
		killInstance();
		evtMan.pause();
		evtMan.setTraceListener(null);
	}

	private class EventTable extends JTable {
		public EventTable(TableModel model) {
			super(model);
			setFillsViewportHeight(true);

			for (int i = 0; i < headers.length; i++) {
				getColumnModel().getColumn(i).setHeaderValue(headers[i]);
				getColumnModel().getColumn(i).setWidth(colWidth[i]);
			}

			getColumnModel().getColumn(1).setHeaderValue(String.format("%s (%s)",
					headers[1], timeUnit));

			this.getTableHeader().setFont(FrameBox.boldFont);
			this.getTableHeader().setReorderingAllowed(false);

			setDefaultRenderer(Object.class, new DefaultCellRenderer() {
				@Override
				public Component getTableCellRendererComponent(JTable table, Object value,
				                                               boolean isSelected, boolean hasFocus,
				                                               int row, int column) {
					Component cell = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

					if (row != table.getSelectedRow())
						cell.setBackground(getColor(row));
					return cell;
				}
			});
		}

		@Override
		public void doLayout() {
			FrameBox.fitTableToLastColumn(this);
		}
	}

	private class ProfileTable extends JTable {
		public ProfileTable(TableModel model) {
			super(model);

			setFillsViewportHeight(true);

			for (int i = 0; i < profHeaders.length; i++) {
				getColumnModel().getColumn(i).setHeaderValue(profHeaders[i]);
				getColumnModel().getColumn(i).setWidth(profColWidth[i]);
			}

			getColumnModel().getColumn(2).setHeaderValue(String.format("%s (/%s)",
					profHeaders[2], timeUnit));

			this.getTableHeader().setFont(FrameBox.boldFont);
			this.getTableHeader().setReorderingAllowed(false);
		}

		@Override
		public void doLayout() {
			FrameBox.fitTableToLastColumn(this);
		}
	}

	private class TabListener implements ChangeListener {
		@Override
		public void stateChanged(ChangeEvent e) {
			dirty = true;
			GUIFrame.updateUI();
		}
	}

	@Override
	public void updateValues(double simTime) {
		if (!this.isVisible())
			return;

		// Update the headers if the time unit has changed
		if (!Unit.getDisplayedUnit(TimeUnit.class).equals(timeUnit)) {
			timeUnit = Unit.getDisplayedUnit(TimeUnit.class);
			eventList.getColumnModel().getColumn(1).setHeaderValue(String.format("%s (%s)",
					headers[1], timeUnit));
			profList.getColumnModel().getColumn(2).setHeaderValue(String.format("%s (/%s)",
					profHeaders[2], timeUnit));
			repaint();
			setDirty(true);
		}

		if (isDirty() || !Unit.getDisplayedUnit(TimeUnit.class).equals(timeUnit)) {
			setDirty(false);
			switch (jTabbedFrame.getSelectedIndex()) {
			case 0:
				updateEvents();
				return;
			case 1:
				updateConditionals();
				return;
			case 2:
				updateProfile();
				return;
			default: return;
			}
		}
	}

	private void setDirty(boolean bool) {
		dirty = bool;
		if (bool) {
			GUIFrame.updateUI();
		}
	}

	private boolean isDirty() {
		return dirty;
	}

	public void updateEvents() {

		// Try to update the event data. If unsuccessful, try again later.
		ArrayList<EventData> eventDataList = new ArrayList<>(retiredEventDataList);
		try {
			eventDataList.addAll(evtMan.getEventDataList());
		}
		catch (Exception e) {
			setDirty(true);
			return;
		}

		// Find the selected row in the updated event data
		int selection = eventList.getSelectedRow();
		EventData selectedEventData = null;
		if (selection > -1) {
			DefaultTableModel tableModel = (DefaultTableModel) eventList.getModel();
			long ticks = Long.parseLong((String) tableModel.getValueAt(selection, 0));
			int pri = Integer.parseInt((String) tableModel.getValueAt(selection, 2));
			String desc = (String) tableModel.getValueAt(selection, 3);
			selectedEventData = new EventData(ticks, pri, desc, "", 0L);
		}

		// Rebuild the event list with the updated data
		double factor = Unit.getDisplayedUnitFactor(TimeUnit.class);
		DefaultTableModel tableModel = (DefaultTableModel) eventList.getModel();
		String[] data = new String[6];
		int rowCount = 0;
		int indNextEvt = -1;
		selection = -1;
		for (int i = 0; i < eventDataList.size(); i++) {
			EventData evtData = eventDataList.get(i);
			if (isHideConditionals() && evtData.status == STATE_EVALUATED)
				continue;
			if (indNextEvt == -1 && evtData.status.isEmpty())
				indNextEvt = rowCount;
			if (evtData.equals(selectedEventData) && evtData.status.isEmpty())
				selection = rowCount;
			data[0] = Long.toString(evtData.ticks);
			data[1] = Double.toString(evtMan.ticksToSeconds(evtData.ticks)/factor);
			data[2] = Integer.toString(evtData.priority);
			data[3] = evtData.description;
			data[4] = evtData.status;
			data[5] = evtData.nanoseconds >= 0 ? Long.toString(evtData.nanoseconds) : "";
			tableModel.insertRow(rowCount, data);
			rowCount++;
		}
		tableModel.setRowCount(rowCount);

		// Reselect the previously selected row in its new position
		if (selection > -1) {
			eventList.setRowSelectionInterval(selection, selection);
		}

		// Scroll to show either the selected row or the next event to be executed
		int line = selection;
		if (selection == -1) {
			line = indNextEvt;
		}
		line = Math.min(line + SCROLL_POSITION, rowCount - 1);
		eventList.scrollRectToVisible(eventList.getCellRect(line, 0, true));
		// Second call is req'd to get correct result when conditional evaluations are excluded
		eventList.scrollRectToVisible(eventList.getCellRect(line, 0, true));
	}

	public void updateConditionals() {

		// Make a copy of the conditional data to avoid concurrent modification exceptions
		ArrayList<String> condDataList;
		try {
			condDataList = evtMan.getConditionalDataList();
		}
		catch (Exception e) {
			setDirty(true);
			return;
		}

		// Build the table entries
		DefaultTableModel tableModel = (DefaultTableModel) condList.getModel();
		String[] condData = new String[1];
		for (int i = 0; i < condDataList.size(); i++) {
			condData[0] = condDataList.get(i);
			tableModel.insertRow(i, condData);
		}
		tableModel.setRowCount(condDataList.size());
	}

	public void updateProfile() {

		// Make a copy of the hashmap entries to avoid concurrent modification exceptions
		ArrayList<Entry<String, ProfileData>> nanosList;
		try {
			if (classButton.isSelected())
				nanosList = new ArrayList<>(classNanosMap.entrySet());
			else
				nanosList = new ArrayList<>(nanosMap.entrySet());
		}
		catch (Exception e) {
			setDirty(true);
			return;
		}

		// Sort the event type in order of decreasing total nanoseconds
		Collections.sort(nanosList, new Comparator<Entry<String, ProfileData>>() {
			@Override
			public int compare(Entry<String, ProfileData> o1, Entry<String, ProfileData> o2) {
				return Long.compare(o2.getValue().nanoseconds, o1.getValue().nanoseconds);
			}
		});

		// Calculate the total nanoseconds for all the events
		Long totalNanos = 0L;
		for (Entry<String, ProfileData> nanosData : nanosList) {
			totalNanos += nanosData.getValue().nanoseconds;
		}

		// Build the table entries
		double factor = Unit.getDisplayedUnitFactor(TimeUnit.class);
		double dur = (GUIFrame.getJaamSimModel().getSimTime() - startTime)/factor;
		DefaultTableModel tableModel = (DefaultTableModel) profList.getModel();
		String[] data = new String[4];
		for (int i = 0; i < nanosList.size(); i++) {
			Entry<String, ProfileData> nanosData = nanosList.get(i);
			data[0] = nanosData.getKey();
			data[1] = String.format("%.3f%%", 100.0d * nanosData.getValue().nanoseconds / totalNanos);
			data[2] = String.format("%.1f", nanosData.getValue().getAvgRate(dur));
			data[3] = String.format("%.0f", nanosData.getValue().getAvgNanos());
			tableModel.insertRow(i, data);
		}
		tableModel.setRowCount(nanosList.size());
	}

	public void addRetiredEvent(EventData evtData) {

		// Save the last event that has started execution and begin measuring its execution time
		retiredEvent = evtData;
		nanoseconds = System.nanoTime();

		recordRetiredEvent(evtData);
	}

	private void recordRetiredEvent(EventData evtData) {
		// Save a list of the last N events that have been executed
		if (retiredEventDataList.size() >= MAX_RETIRED_EVENTS) {
			retiredEventDataList.remove(0);
		}
		retiredEventDataList.add(evtData);
	}

	public void recordNanos() {

		// Set the elapsed time for the last retired event
		if (retiredEvent == null)
			return;
		retiredEvent.setNanoseconds(System.nanoTime() - nanoseconds);

		// Set the key for a generated entity based on its prototype
		String key = retiredEvent.description;
		String classKey = key;
		int ind = key.lastIndexOf(".");
		if (ind >= 0) {
			String entName = key.substring(0, ind);
			String method = key.substring(ind);
			Entity ent = GUIFrame.getJaamSimModel().getNamedEntity(entName);
			if (ent == null) {
				// Replace the trailing digits with a single asterisk
				String protoName = entName.replaceFirst("\\d*$", "\\*");
				key = protoName + method;
				classKey = key;
			}
			else {
				classKey = ent.getClass().getSimpleName() + method;
			}
		}

		// Accumulate the total time for each type of event
		ProfileData val = nanosMap.get(key);
		if (val == null) {
			val = new ProfileData();
			nanosMap.put(key, val);
		}
		val.recordNanos(retiredEvent.nanoseconds);

		// Accumulate the total time by class
		val = classNanosMap.get(classKey);
		if (val == null) {
			val = new ProfileData();
			classNanosMap.put(classKey, val);
		}
		val.recordNanos(retiredEvent.nanoseconds);

		// Clear the last retired event
		retiredEvent = null;
	}

	static class ProfileData {
		public long nanoseconds;
		public long count;

		public ProfileData() {
			nanoseconds = 0L;
			count = 0L;
		}

		public void recordNanos(long nanos) {
			nanoseconds += nanos;
			count++;
		}

		public double getAvgNanos() {
			return (double) nanoseconds / count;
		}

		public double getAvgRate(double dur) {
			return count / dur;
		}
	}

	public Color getColor(int i) {
		String status = (String) eventList.getModel().getValueAt(i, 4);
		switch (status) {
			case STATE_COMPLETED:   return COLOR_COMPLETED;
			case STATE_INTERRUPTED: return COLOR_INTERRUPTED;
			case STATE_TERMINATED:  return COLOR_TERMINATED;
			case STATE_EVALUATED:   return COLOR_EVALUATED;
			default: return null;
		}
	}

	@Override
	public void traceEvent(EventManager e, long curTick, long tick, int priority, ProcessTarget t) {
		recordNanos();
		addRetiredEvent(new EventData(tick, priority, t.getDescription(), STATE_COMPLETED));
		setDirty(true);
	}

	@Override
	public void traceWait(EventManager e, long curTick, long tick, int priority, ProcessTarget t) {
		recordNanos();
		setDirty(true);
	}

	@Override
	public void traceSchedProcess(EventManager e, long curTick, long tick, int priority, ProcessTarget t) {
		// Don't record anything, but mark dirty as there is a new event somewhere in the event list
		setDirty(true);
	}

	@Override
	public void traceProcessStart(EventManager e, ProcessTarget t, long tick) {
		// FIXME: after returning from the started process we no longer accrue time to the original executing task
		recordNanos();
		setDirty(true);
	}

	@Override
	public void traceProcessEnd(EventManager e, long tick) {
		recordNanos();
		setDirty(true);
	}

	@Override
	public void traceInterrupt(EventManager e, long curTick, long tick, int priority, ProcessTarget t) {
		// FIXME: after returning from the interrupted event we no longer accrue time to the original executing task
		recordNanos();
		addRetiredEvent(new EventData(curTick, priority, t.getDescription(), STATE_INTERRUPTED));
		setDirty(true);
	}

	@Override
	public void traceKill(EventManager e, long curTick, long tick, int priority, ProcessTarget t) {
		recordRetiredEvent(new EventData(curTick, priority, t.getDescription(), STATE_TERMINATED));
		setDirty(true);
	}

	@Override
	public void traceWaitUntil(EventManager e, long tick) {
		recordNanos();
		setDirty(true);
	}

	@Override
	public void traceWaitUntilEnded(EventManager e, long tick, ProcessTarget t) {}

	@Override
	public void traceConditionalEval(EventManager e, long tick, ProcessTarget t) {
		recordNanos();
		addRetiredEvent(new EventData(tick, 0, t.getDescription(), STATE_EVALUATED));
		setDirty(true);
	}

	@Override
	public void traceConditionalEvalEnded(EventManager e, long tick, ProcessTarget t) {
		recordNanos();
		setDirty(true);
	}

}
