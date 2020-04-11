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
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;

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
	private static ArrayList<EventData> retiredEventDataList;
	private static ArrayList<EventData> eventDataList;
	private static boolean dirty;
	private static String timeUnit;
	private static HashMap <String, Long> nanosMap;

	private static final TableCellRenderer evCellRenderer;
	private static JTabbedPane jTabbedFrame;
	private static JTable eventList;
	private static JScrollPane sp;
	private static JTable condList;
	private static JScrollPane condSp;
	private static JTable profList;
	private static JScrollPane profSp;
	private static EventManager evtMan;

	private static long nanoseconds;
	private static EventData retiredEvent;

	private static final String[] headers= {"Ticks", "Time", "Pri", "Description", "State", "Nanos"};
	private static final int[] colWidth = {100, 80, 30, 160, 80, 60};

	private static final int MAX_RETIRED_EVENTS = 1000;
	private static final int SCROLL_POSITION = 5;

	private static final String STATE_COMPLETED   = "Completed";    // event executed
	private static final String STATE_INTERRUPTED = "Interrupted";  // event executed early
	private static final String STATE_TERMINATED  = "Terminated";   // event killed

	private static final Color COLOR_COMPLETED   = new Color(144, 238, 144);  // light green
	private static final Color COLOR_INTERRUPTED = new Color(224, 255, 255);  // light cyan
	private static final Color COLOR_TERMINATED  = new Color(240, 128, 128);  // light coral

	static {
		evCellRenderer = new EventViewerCellRenderer();
	}

	public EventViewer( EventManager em ) {
		super("Event Viewer");
		setDefaultCloseOperation( FrameBox.DISPOSE_ON_CLOSE );
		addWindowListener(FrameBox.getCloseListener("ShowEventViewer"));

		retiredEventDataList = new ArrayList<>();
		eventDataList = new ArrayList<>();
		nanosMap = new HashMap <>();

		evtMan = em;
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
				update();
			}
		});
		clearButton.setToolTipText(GUIFrame.formatToolTip("Clear Events",
				"Removes the completed events from the viewer."));

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
		getContentPane().add(buttonPanel, BorderLayout.NORTH);

		// Event List
		eventList = new EventTable(new DefaultTableModel(0, headers.length));
		sp = new JScrollPane();
		sp.getViewport().add(eventList);
		sp.setPreferredSize(new Dimension( 800, 300 ));
		jTabbedFrame.addTab("Future Events", null, sp, null);

		// Conditionals List
		condList = new JTable(new DefaultTableModel(0, 1));
		condList.getColumnModel().getColumn(0).setHeaderValue("Description");
		condList.getTableHeader().setFont(FrameBox.boldFont);
		condList.setDefaultRenderer(Object.class, colRenderer);
		condSp = new JScrollPane();
		condSp.getViewport().add(condList);
		condSp.setPreferredSize(new Dimension( 800, 300 ));
		jTabbedFrame.addTab("Conditional Events", null, condSp, null);

		// Profiler List
		profList = new JTable(new DefaultTableModel(0, 2));
		profList.getColumnModel().getColumn(0).setHeaderValue("Event Type");
		profList.getColumnModel().getColumn(1).setHeaderValue("Percent of Total Time");
		profList.getTableHeader().setFont(FrameBox.boldFont);
		profList.setDefaultRenderer(Object.class, colRenderer);
		profSp = new JScrollPane();
		profSp.getViewport().add(profList);
		profSp.setPreferredSize(new Dimension( 800, 300 ));
		jTabbedFrame.addTab("Execution Time Profile", null, profSp, null);

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

	@Override
	public void reset() {
		super.reset();
		retiredEventDataList.clear();
		setDirty(true);
	}

	@Override
	public void dispose() {
		super.dispose();
		killInstance();
		evtMan.setTraceListener(null);
	}

	private static class EventTable extends JTable {
		public EventTable(TableModel model) {
			super(model);

			setDefaultRenderer(Object.class, evCellRenderer);
			setFillsViewportHeight(true);

			for (int i = 0; i < headers.length; i++) {
				getColumnModel().getColumn(i).setHeaderValue(headers[i]);
				getColumnModel().getColumn(i).setWidth(colWidth[i]);
			}

			getColumnModel().getColumn(1).setHeaderValue(String.format("%s (%s)",
					headers[1], timeUnit));

			this.getTableHeader().setFont(FrameBox.boldFont);
			this.getTableHeader().setReorderingAllowed(false);
		}

		@Override
		public void doLayout() {
			FrameBox.fitTableToLastColumn(this);
		}
	}

	private static class TabListener implements ChangeListener {
		@Override
		public void stateChanged(ChangeEvent e) {
			GUIFrame.updateUI();
		}
	}

	@Override
	public void updateValues(double simTime) {
		if (!this.isVisible())
			return;

		if (isDirty() || !Unit.getDisplayedUnit(TimeUnit.class).equals(timeUnit)) {
			setDirty(false);
			update();
			updateProfile();
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

	public void update() {

		// Try to update the event data. If unsuccessful, try again later.
		ArrayList<EventData> newEventDataList = new ArrayList<>(retiredEventDataList);
		try {
			newEventDataList.addAll(evtMan.getEventDataList());
		}
		catch (Exception e) {
			setDirty(true);
			return;
		}

		// Find the selected row in the updated event data
		int selection = eventList.getSelectedRow();
		if (selection > -1) {
			EventData selectedEventData = eventDataList.get(selection);

			// Find this event in the new data
			selection = -1;
			for (int i = 0; i < newEventDataList.size(); i++) {
				if (newEventDataList.get(i).ticks > selectedEventData.ticks) {
					break;
				}
				if (newEventDataList.get(i).equals(selectedEventData)) {
					selection = i;
					break;
				}
			}
		}
		eventDataList = newEventDataList;

		// Update the header if the time unit has changed
		if (!Unit.getDisplayedUnit(TimeUnit.class).equals(timeUnit)) {
			timeUnit = Unit.getDisplayedUnit(TimeUnit.class);
			eventList.getColumnModel().getColumn(1).setHeaderValue(String.format("%s (%s)",
					headers[1], timeUnit));
			repaint();
		}

		// Rebuild the event list with the updated data
		double factor = Unit.getDisplayedUnitFactor(TimeUnit.class);
		DefaultTableModel tableModel = (DefaultTableModel) eventList.getModel();
		String[] data = new String[6];
		for (int i = 0; i < eventDataList.size(); i++) {
			EventData evtData = eventDataList.get(i);
			data[0] = Long.toString(evtData.ticks);
			data[1] = Double.toString(evtMan.ticksToSeconds(evtData.ticks)/factor);
			data[2] = Integer.toString(evtData.priority);
			data[3] = evtData.description;
			data[4] = evtData.status;
			data[5] = evtData.nanoseconds >= 0 ? Long.toString(evtData.nanoseconds) : "";
			tableModel.insertRow(i, data);
		}
		tableModel.setRowCount(eventDataList.size());

		// Reselect the previously selected row in its new position
		if (selection > -1) {
			eventList.setRowSelectionInterval(selection, selection);
		}

		// Scroll to show either the selected row or the next event to be executed
		int line = selection;
		if (selection == -1) {
			line = retiredEventDataList.size();
		}
		line = Math.min(line + SCROLL_POSITION, eventDataList.size()-1);
		eventList.scrollRectToVisible(eventList.getCellRect(line, 0, true));

		// Update the conditionals
		ArrayList<String> condDataList = evtMan.getConditionalDataList();
		tableModel = (DefaultTableModel) condList.getModel();
		String[] condData = new String[1];
		for (int i = 0; i < condDataList.size(); i++) {
			condData[0] = condDataList.get(i);
			tableModel.insertRow(i, condData);
		}
		tableModel.setRowCount(condDataList.size());
	}

	public void updateProfile() {

		// Make a copy of the hashmap entries to avoid concurrent modification exceptions
		ArrayList<Entry<String, Long>> nanosList;
		try {
			nanosList = new ArrayList<>(nanosMap.entrySet());
		}
		catch (Exception e) {
			setDirty(true);
			return;
		}

		// Sort the event type in order of decreasing total nanoseconds
		Collections.sort(nanosList, new Comparator<Entry<String, Long>>() {
			@Override
			public int compare(Entry<String, Long> o1, Entry<String, Long> o2) {
				return Long.compare(o2.getValue(), o1.getValue());
			}
		});

		// Calculate the total nanoseconds for all the events
		Long totalNanos = 0L;
		for (Entry<String, Long> nanosData : nanosList) {
			totalNanos += nanosData.getValue();
		}

		// Build the table entries
		DefaultTableModel tableModel = (DefaultTableModel) profList.getModel();
		String[] data = new String[2];
		for (int i = 0; i < nanosList.size(); i++) {
			Entry<String, Long> nanosData = nanosList.get(i);
			data[0] = nanosData.getKey();
			data[1] = String.format("%.3f%%", 100.0d * nanosData.getValue() / totalNanos);
			tableModel.insertRow(i, data);
		}
		tableModel.setRowCount(nanosList.size());
	}

	public void addRetiredEvent(EventData evtData) {

		// Save the last event that has started execution and begin measuring its execution time
		retiredEvent = evtData;
		nanoseconds = System.nanoTime();

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

		// Accumulate the total time for each type of event
		Long val = nanosMap.get(retiredEvent.description);
		if (val == null)
			val = new Long(0L);
		val += retiredEvent.nanoseconds;
		nanosMap.put(retiredEvent.description, val);

		// Clear the last retired event
		retiredEvent = null;
	}

	public static Color getColor(int i) {
		switch (eventDataList.get(i).status) {
			case STATE_COMPLETED:   return COLOR_COMPLETED;
			case STATE_INTERRUPTED: return COLOR_INTERRUPTED;
			case STATE_TERMINATED:  return COLOR_TERMINATED;
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
		recordNanos();
		setDirty(true);
	}

	@Override
	public void traceProcessStart(EventManager e, ProcessTarget t, long tick) {
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
		recordNanos();
		addRetiredEvent(new EventData(curTick, priority, t.getDescription(), STATE_INTERRUPTED));
		setDirty(true);
	}

	@Override
	public void traceKill(EventManager e, long curTick, long tick, int priority, ProcessTarget t) {
		recordNanos();
		addRetiredEvent(new EventData(curTick, priority, t.getDescription(), STATE_TERMINATED));
		setDirty(true);
	}

	@Override
	public void traceWaitUntil(EventManager e, long tick) {
		recordNanos();
		setDirty(true);
	}

	@Override
	public void traceWaitUntilEnded(EventManager e, long tick, ProcessTarget t) {
		recordNanos();
		addRetiredEvent(new EventData(tick, 0, t.getDescription(), STATE_COMPLETED));
		setDirty(true);
	}

}
