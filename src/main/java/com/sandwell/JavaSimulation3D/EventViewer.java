/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2009-2011 Ausenco Engineering Canada Inc.
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

import com.jaamsim.ui.FrameBox;
import com.sandwell.JavaSimulation.EventManager;
import com.sandwell.JavaSimulation.Util;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

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
public class EventViewer extends JFrame {
	JTable eventList;
	JScrollPane sp;
	EventManager evtMan;
	java.awt.event.MouseListener propertyListener;

	int numRetiredEvents = 0;

	/** Constructor creates and displays a new ModelBox for the specified DisplayEntity at the specified location **/
	public EventViewer( EventManager em ) {
		super("Event Viewer");
		setDefaultCloseOperation( FrameBox.DISPOSE_ON_CLOSE );

		numRetiredEvents = 0;

		evtMan = em;

		String[] headers = evtMan.getViewerHeaders();
		eventList = new JTable(new DefaultTableModel(0, headers.length));
		eventList.setFillsViewportHeight(true);
		eventList.setAutoResizeMode( JTable.AUTO_RESIZE_OFF );

		sp = new JScrollPane();
		sp.getViewport().add(eventList);
		sp.setPreferredSize(new Dimension( 800, 300 ));
		getContentPane().add(sp, BorderLayout.CENTER);

		for (int i = 0; i < headers.length; i++) {
			eventList.getColumnModel().getColumn(i).setHeaderValue(headers[i]);
			eventList.getColumnModel().getColumn(i).setMinWidth(headers[i].length() * 6);
		}
		// create a close button on the bottom of the screen
		JButton closeButton = new JButton( "Close" );
		closeButton.addActionListener( new ActionListener() {
			@Override
			public void actionPerformed( ActionEvent event ) {
				dispose();
			}
		});

		JButton nextEventButton = new JButton( "Next Event" );
		nextEventButton.addActionListener( new ActionListener() {
			@Override
			public void actionPerformed( ActionEvent e ) {
				evtMan.nextOneEvent();
			}
		});

		JButton nextTimeButton = new JButton( "Next Time" );
		nextTimeButton.addActionListener( new ActionListener() {
			@Override
			public void actionPerformed( ActionEvent e ) {
				evtMan.nextEventTime();
			}
		});

		JButton clearButton = new JButton( "Clear Events" );
		clearButton.addActionListener( new ActionListener() {
			@Override
			public void actionPerformed( ActionEvent e ) {
				numRetiredEvents = 0;
				update();
			}
		});

		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout( new FlowLayout( FlowLayout.CENTER ) );
		buttonPanel.add( nextEventButton );
		buttonPanel.add( nextTimeButton );
		buttonPanel.add( clearButton );
		buttonPanel.add( closeButton );
		getContentPane().add(buttonPanel, BorderLayout.SOUTH);

		update();
		pack();
		setVisible(true);
		evtMan.registerEventViewer( this );
	}

	public void update() {
		String[][] data = evtMan.getViewerData();
		int selection = eventList.getSelectedRow();

		if (selection >= numRetiredEvents) {
			String time = (String)eventList.getValueAt(selection, 0);
			String file = (String)eventList.getValueAt(selection, 7);
			String ent = (String)eventList.getValueAt(selection, 3);
			selection = -1;
			for (int i = 0; i < data.length; i++) {
				String[] row = data[i];
				if (time != "" && row[0].equals(time) && row[3].equals(ent)) {
					selection = numRetiredEvents + i;
					break;
				}
				if (row[7].equals(file) && row[3].equals(ent)) {
					selection = numRetiredEvents + i;
					break;
				}
			}
			if (selection == -1)
				selection = numRetiredEvents;
		}

		for (int i = 0; i < data.length; i++) {
			((DefaultTableModel)eventList.getModel()).insertRow(numRetiredEvents + i, data[i]);
		}
		((DefaultTableModel)eventList.getModel()).setRowCount(numRetiredEvents+data.length);
		if (selection > -1) {
			eventList.setRowSelectionInterval(selection, selection);
		}
	}

	public void addRetiredEvent(String data[], int reason) {
		String color;

		if (reason == 1) {
			color = "rgb(0,255,0)";
		} else if (reason == 2) {
			color = "rgb(255,255,0)";
		} else if (reason == 3) {
			color = "rgb(255,0,0)";
		} else {
			color = "rgb(128,128,128)";
		}

		for (int i = 0; i < data.length; i++) {
			data[i] = Util.HTMLString("courier", null, color, null, null, data[i]) + "                                                                           ";
		}
		((DefaultTableModel)eventList.getModel()).insertRow(numRetiredEvents, data);
		numRetiredEvents++;
	}

	@Override
	public void dispose() {
		super.dispose();
		evtMan.unregisterEventViewer(this);
	}
}
