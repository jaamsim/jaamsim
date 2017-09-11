/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2009-2011 Ausenco Engineering Canada Inc.
 * Copyright (C) 2017 JaamSim Software Inc.
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

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;

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
public class EventViewer extends OSFixJFrame implements EventTraceListener {
	private static EventViewer myInstance;
	private static ArrayList<EventData> retiredEventDataList;
	private static ArrayList<EventData> eventDataList;

	private static final TableCellRenderer evCellRenderer;
	private static JTable eventList;
	private static JScrollPane sp;
	private static EventManager evtMan;

	private static final String[] headers= {"Ticks", "SimTime", "Priority", "Description", "State"};
	private static final int[] colWidth = {100, 100, 60, 180, 80};

	private static final int MAX_RETIRED_EVENTS = 1000;

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
		setType(Type.UTILITY);
		setAutoRequestFocus(false);
		setDefaultCloseOperation( FrameBox.DISPOSE_ON_CLOSE );
		addWindowListener(FrameBox.getCloseListener("ShowEventViewer"));

		retiredEventDataList = new ArrayList<>();
		eventDataList = new ArrayList<>();

		evtMan = em;
		evtMan.setTraceListener(this);

		// Next Event Button
		JButton nextEventButton = new JButton( "Next Event" );
		nextEventButton.addActionListener( new ActionListener() {
			@Override
			public void actionPerformed( ActionEvent e ) {
				evtMan.nextOneEvent();
			}
		});
		nextEventButton.setToolTipText(GUIFrame.formatToolTip("Next Event",
				"Executes a single event from the future event list."));

		// Next Time Button
		JButton nextTimeButton = new JButton( "Next Time" );
		nextTimeButton.addActionListener( new ActionListener() {
			@Override
			public void actionPerformed( ActionEvent e ) {
				evtMan.nextEventTime();
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

		// Button Bar
		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout( new FlowLayout( FlowLayout.LEFT ) );
		buttonPanel.add( nextEventButton );
		buttonPanel.add( nextTimeButton );
		buttonPanel.add( clearButton );
		getContentPane().add(buttonPanel, BorderLayout.NORTH);

		// Event List
		eventList = new JTable(new DefaultTableModel(0, headers.length));
		eventList.setDefaultRenderer(Object.class, evCellRenderer);
		eventList.setFillsViewportHeight(true);
		eventList.setAutoResizeMode( JTable.AUTO_RESIZE_OFF );
		eventList.getTableHeader().setFont(FrameBox.boldFont);
		eventList.getTableHeader().setReorderingAllowed(false);

		for (int i = 0; i < headers.length; i++) {
			eventList.getColumnModel().getColumn(i).setHeaderValue(headers[i]);
			eventList.getColumnModel().getColumn(i).setPreferredWidth(colWidth[i]);
		}

		sp = new JScrollPane();
		sp.getViewport().add(eventList);
		sp.setPreferredSize(new Dimension( 800, 300 ));
		getContentPane().add(sp, BorderLayout.CENTER);

		// Size and position of the viewer
		pack();
		setLocation(GUIFrame.COL4_START, GUIFrame.BOTTOM_START);
		setSize(GUIFrame.COL4_WIDTH, GUIFrame.HALF_BOTTOM);

		// Display the viewer
		update();
		setVisible(true);
	}

	/**
	 * Returns the only instance of the EventViewer
	 */
	public synchronized static EventViewer getInstance() {
		if (myInstance == null)
			myInstance = new EventViewer(GUIFrame.getInstance().getEventManager());

		return myInstance;
	}

	public static boolean hasInstance() {
		return myInstance != null;
	}

	private synchronized static void killInstance() {
		myInstance = null;
	}

	@Override
	public void dispose() {
		super.dispose();
		killInstance();
		evtMan.setTraceListener(null);
	}

	public void update() {

		// Update the event data
		ArrayList<EventData> newEventDataList = new ArrayList<>(retiredEventDataList);
		newEventDataList.addAll(evtMan.getEventDataList());

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

		// Rebuild the event list with the updated data
		double factor = Unit.getDisplayedUnitFactor(TimeUnit.class);
		DefaultTableModel tableModel = (DefaultTableModel) eventList.getModel();
		String[] data = new String[5];
		for (int i = 0; i < eventDataList.size(); i++) {
			EventData evtData = eventDataList.get(i);
			data[0] = Long.toString(evtData.ticks);
			data[1] = Double.toString(evtMan.ticksToSeconds(evtData.ticks)/factor);
			data[2] = Integer.toString(evtData.priority);
			data[3] = evtData.description;
			data[4] = evtData.status;
			tableModel.insertRow(i, data);
		}
		tableModel.setRowCount(eventDataList.size());

		// Reselect the previously selected row in its new position
		if (selection > -1) {
			eventList.setRowSelectionInterval(selection, selection);
		}
	}

	public void addRetiredEvent(EventData evtData) {
		if (retiredEventDataList.size() >= MAX_RETIRED_EVENTS) {
			retiredEventDataList.remove(0);
		}
		retiredEventDataList.add(evtData);
		update();
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
		addRetiredEvent(new EventData(tick, priority, t.getDescription(), STATE_COMPLETED));
	}

	@Override
	public void traceWait(EventManager e, long curTick, long tick, int priority, ProcessTarget t) {
		update();
	}

	@Override
	public void traceSchedProcess(EventManager e, long curTick, long tick, int priority, ProcessTarget t) {
		update();
	}

	@Override
	public void traceProcessStart(EventManager e, ProcessTarget t, long tick) {
		update();
	}

	@Override
	public void traceProcessEnd(EventManager e, long tick) {
		update();
	}

	@Override
	public void traceInterrupt(EventManager e, long curTick, long tick, int priority, ProcessTarget t) {
		addRetiredEvent(new EventData(curTick, priority, t.getDescription(), STATE_INTERRUPTED));
	}

	@Override
	public void traceKill(EventManager e, long curTick, long tick, int priority, ProcessTarget t) {
		addRetiredEvent(new EventData(curTick, priority, t.getDescription(), STATE_TERMINATED));
	}

	@Override
	public void traceWaitUntil(EventManager e, long tick) {
		update();
	}

	@Override
	public void traceWaitUntilEnded(EventManager e, long tick, ProcessTarget t) {
		addRetiredEvent(new EventData(tick, 0, t.getDescription(), STATE_COMPLETED));
	}

}
