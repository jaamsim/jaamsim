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
import java.util.Arrays;
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

import com.jaamsim.basicsim.Entity;
import com.jaamsim.events.EventData;
import com.jaamsim.events.EventManager;
import com.jaamsim.events.EventTraceListener;
import com.jaamsim.events.ProcessTarget;
import com.jaamsim.units.TimeUnit;

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
	private final ArrayList<EventData> pendingEvents;
	private final ArrayList<String> condEvents;
	private final RetiredEventData[] retiredEventRing;
	private int firstRetiredEvent;
	private int lastRetiredEvent;
	private boolean dirty;
	private String timeUnit;
	private HashMap <String, ProfileData> nanosMap;
	private HashMap <String, ProfileData> classNanosMap;
	private double startTime;

	private JTabbedPane jTabbedFrame;
	private EventTable eventList;
	private JScrollPane sp;
	private JTable condList;
	private JScrollPane condSp;
	private ProfileTable profList;
	private JScrollPane profSp;
	private EventManager evtMan;
	private JToggleButton conditionalsButton;
	private JToggleButton classButton;

	private int level;
	private long nanoseconds;
	private RetiredEventData retiredEvent;

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

		pendingEvents = new ArrayList<>();
		condEvents = new ArrayList<>();
		retiredEventRing = new RetiredEventData[1024];
		firstRetiredEvent = 0;
		lastRetiredEvent = 0;
		nanosMap = new HashMap <>();
		classNanosMap = new HashMap <>();
		startTime = GUIFrame.getJaamSimModel().getSimTime();

		evtMan = em;
		evtMan.pause();
		evtMan.setTraceListener(this);

		timeUnit = GUIFrame.getJaamSimModel().getDisplayedUnit(TimeUnit.class);

		// Next Event Button
		JButton nextEventButton = new JButton( "Next Event" );
		nextEventButton.addActionListener( new ActionListener() {
			@Override
			public void actionPerformed( ActionEvent e ) {
				evtMan.resumeSeconds(GUIFrame.getJaamSimModel().getSimulation().getPauseTime(), true, false);
			}
		});
		nextEventButton.setToolTipText(GUIFrame.formatToolTip("Next Event",
				"Executes a single event from the future event list."));

		// Next Time Button
		JButton nextTimeButton = new JButton( "Next Time" );
		nextTimeButton.addActionListener( new ActionListener() {
			@Override
			public void actionPerformed( ActionEvent e ) {
				evtMan.resumeSeconds(GUIFrame.getJaamSimModel().getSimulation().getPauseTime(), false, true);
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
				Arrays.fill(retiredEventRing, null);
				firstRetiredEvent = 0;
				lastRetiredEvent = 0;
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
		eventList = new EventTable(timeUnit);
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
		profList = new ProfileTable(timeUnit);
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
		Arrays.fill(retiredEventRing, null);
		firstRetiredEvent = 0;
		lastRetiredEvent = 0;
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

	private static final class EventTable extends JTable {

		private static final String[] headers= {"Ticks", "Time", "Pri", "Description", "State", "Nanos"};
		private static final int[] colWidth = {100, 80, 30, 160, 80, 60};

		public EventTable(String tu) {
			super(new DefaultTableModel(0, headers.length));
			setFillsViewportHeight(true);

			for (int i = 0; i < headers.length; i++) {
				getColumnModel().getColumn(i).setHeaderValue(headers[i]);
				getColumnModel().getColumn(i).setWidth(colWidth[i]);
			}

			this.updateHeader(tu);

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

		public void updateHeader(String tu) {
			String header = String.format("%s (%s)", headers[1], tu);
			getColumnModel().getColumn(1).setHeaderValue(header);
		}

		@Override
		public void doLayout() {
			FrameBox.fitTableToLastColumn(this);
		}

		public Color getColor(int i) {
			String status = (String)this.getModel().getValueAt(i, 4);
			switch (status) {
				case STATE_COMPLETED:   return COLOR_COMPLETED;
				case STATE_INTERRUPTED: return COLOR_INTERRUPTED;
				case STATE_TERMINATED:  return COLOR_TERMINATED;
				case STATE_EVALUATED:   return COLOR_EVALUATED;
				default: return null;
			}
		}
	}

	private static final class ProfileTable extends JTable {
		private static final String[] profHeaders= {"Event Type", "% of Total Time", "Rate", "Avg. Nanos"};
		private static final int[] profColWidth = {200, 100, 100, 100};

		public ProfileTable(String tu) {
			super(new DefaultTableModel(0, profHeaders.length));

			setFillsViewportHeight(true);

			for (int i = 0; i < profHeaders.length; i++) {
				getColumnModel().getColumn(i).setHeaderValue(profHeaders[i]);
				getColumnModel().getColumn(i).setWidth(profColWidth[i]);
			}

			this.updateHeader(tu);

			this.getTableHeader().setFont(FrameBox.boldFont);
			this.getTableHeader().setReorderingAllowed(false);
		}

		public void updateHeader(String tu) {
			String header = String.format("%s (/%s)", profHeaders[2], tu);
			getColumnModel().getColumn(2).setHeaderValue(header);
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
		if (!GUIFrame.getJaamSimModel().getDisplayedUnit(TimeUnit.class).equals(timeUnit)) {
			timeUnit = GUIFrame.getJaamSimModel().getDisplayedUnit(TimeUnit.class);
			eventList.updateHeader(timeUnit);
			profList.updateHeader(timeUnit);
			repaint();
			setDirty(true);
		}

		if (isDirty() || !GUIFrame.getJaamSimModel().getDisplayedUnit(TimeUnit.class).equals(timeUnit)) {
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

	static class RetiredEventData extends EventData {
		final String status;
		long nanos;

		public RetiredEventData(long tk, int pri, String desc, String stat) {
			super(tk, pri, desc);
			status = stat;
			nanos = -1L;
		}

		void setNanoseconds(long ns) {
			nanos = ns;
		}
	}

	public void updateEvents() {

		// Try to update the event data. If unsuccessful, try again later.
		try {
			pendingEvents.clear();
			evtMan.getEventDataList(pendingEvents);
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
			selectedEventData = new EventData(ticks, pri, desc);
		}

		// Rebuild the event list with the updated data
		double factor = GUIFrame.getJaamSimModel().getDisplayedUnitFactor(TimeUnit.class);
		DefaultTableModel tableModel = (DefaultTableModel) eventList.getModel();
		String[] data = new String[6];
		int rowCount = 0;
		selection = -1;
		for (int i = firstRetiredEvent; i != lastRetiredEvent; i = (i +1) % retiredEventRing.length) {
			RetiredEventData evtData = retiredEventRing[i];
			if (isHideConditionals() && evtData.status == STATE_EVALUATED)
				continue;
			data[0] = Long.toString(evtData.ticks);
			data[1] = Double.toString(evtMan.ticksToSeconds(evtData.ticks)/factor);
			data[2] = Integer.toString(evtData.priority);
			data[3] = evtData.description;
			data[4] = evtData.status;
			data[5] = evtData.nanos >= 0 ? Long.toString(evtData.nanos) : "";
			tableModel.insertRow(rowCount, data);
			rowCount++;
		}

		// after traversing all the retired events save the first row for a pending event
		int indNextEvt = rowCount;
		for (int i = 0; i < pendingEvents.size(); i++) {
			EventData evtData = pendingEvents.get(i);
			if (evtData.equals(selectedEventData))
				selection = rowCount;
			data[0] = Long.toString(evtData.ticks);
			data[1] = Double.toString(evtMan.ticksToSeconds(evtData.ticks)/factor);
			data[2] = Integer.toString(evtData.priority);
			data[3] = evtData.description;
			data[4] = "";
			data[5] = "";
			tableModel.insertRow(rowCount, data);
			rowCount++;
		}
		pendingEvents.clear();
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
		try {
			condEvents.clear();
			evtMan.getConditionalDataList(condEvents);
		}
		catch (Exception e) {
			setDirty(true);
			return;
		}

		// Build the table entries
		DefaultTableModel tableModel = (DefaultTableModel) condList.getModel();
		String[] condData = new String[1];
		for (int i = 0; i < condEvents.size(); i++) {
			condData[0] = condEvents.get(i);
			tableModel.insertRow(i, condData);
		}
		tableModel.setRowCount(condEvents.size());
		condEvents.clear();
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
		double factor = GUIFrame.getJaamSimModel().getDisplayedUnitFactor(TimeUnit.class);
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

	private void recordRetiredEvent(RetiredEventData evtData) {
		retiredEventRing[lastRetiredEvent] = evtData;
		lastRetiredEvent = (lastRetiredEvent + 1) % retiredEventRing.length;
		if (lastRetiredEvent == firstRetiredEvent)
			firstRetiredEvent = (firstRetiredEvent + 1) % retiredEventRing.length;
	}

	public void recordNanos() {
		if (level != 0)
			return;
		retiredEvent.setNanoseconds(System.nanoTime() - nanoseconds);

		// Set the key for a generated entity based on its prototype
		String key = retiredEvent.description;
		String classKey = key;
		int ind = key.lastIndexOf('.');
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
		val.recordNanos(retiredEvent.nanos);

		// Accumulate the total time by class
		val = classNanosMap.get(classKey);
		if (val == null) {
			val = new ProfileData();
			classNanosMap.put(classKey, val);
		}
		val.recordNanos(retiredEvent.nanos);

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

	@Override
	public void traceEvent(long tick, int priority, ProcessTarget t) {
		level = 1;
		retiredEvent = new RetiredEventData(tick, priority, t.getDescription(), STATE_COMPLETED);
		nanoseconds = System.nanoTime();

		recordRetiredEvent(retiredEvent);
		setDirty(true);
	}

	@Override
	public void traceWait(long tick, int priority, ProcessTarget t) {
		level--;
		recordNanos();
		setDirty(true);
	}

	@Override
	public void traceSchedProcess(long tick, int priority, ProcessTarget t) {
		// Don't record anything, but mark dirty as there is a new event somewhere in the event list
		setDirty(true);
	}

	@Override
	public void traceProcessStart(ProcessTarget t) {
		level++;
		setDirty(true);
	}

	@Override
	public void traceProcessEnd() {
		level--;
		recordNanos();
		setDirty(true);
	}

	@Override
	public void traceInterrupt(long tick, int priority, ProcessTarget t) {
		level++;
		recordRetiredEvent(new RetiredEventData(EventManager.current().getTicks(), priority, t.getDescription(), STATE_INTERRUPTED));
		setDirty(true);
	}

	@Override
	public void traceKill(long tick, int priority, ProcessTarget t) {
		recordRetiredEvent(new RetiredEventData(EventManager.current().getTicks(), priority, t.getDescription(), STATE_TERMINATED));
		setDirty(true);
	}

	@Override
	public void traceWaitUntil() {
		level--;
		recordNanos();
		setDirty(true);
	}

	@Override
	public void traceSchedUntil(ProcessTarget t) {
		setDirty(true);
	}

	@Override
	public void traceConditionalEval(ProcessTarget t) {
		level = 1;
		retiredEvent = new RetiredEventData(EventManager.current().getTicks(), 0, t.getDescription(), STATE_EVALUATED);
		nanoseconds = System.nanoTime();

		recordRetiredEvent(retiredEvent);
		setDirty(true);
	}

	@Override
	public void traceConditionalEvalEnded(boolean wakeup, ProcessTarget t) {
		level--;
		recordNanos();
		setDirty(true);
	}

}
