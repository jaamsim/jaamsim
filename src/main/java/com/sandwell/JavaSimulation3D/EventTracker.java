/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2003-2011 Ausenco Engineering Canada Inc.
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

import com.sandwell.JavaSimulation.Entity;
import com.sandwell.JavaSimulation.Input;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 * Class to displays events executed by event Manager for a specific object
 */
public class EventTracker extends JDialog {
	public EventTracker() {
		// build the Dialog
		this.setTitle("Track Events");
		this.setModal(false);
		setDefaultCloseOperation( JFrame.DISPOSE_ON_CLOSE );

		// add the scroll pane to the dialog
		getContentPane().setLayout( new BorderLayout() );
		JPanel mp = new JPanel();
		mp.add( new JLabel( "Object to track:" ) );
		final JTextField objName = new JTextField();
		objName.setPreferredSize( new Dimension( 80, 16 ) );
		mp.add( objName );
		mp.setLayout( new FlowLayout() );
		getContentPane().add( mp );

		// create a close button on the bottom of the screen
		JButton closeButton = new JButton( "Close" );
		closeButton.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed( ActionEvent event ) {
				dispose();
			}
		} );

		JButton trackButton = new JButton( "Track" );
		trackButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Entity thisEnt = Input.tryParseEntity(objName.getText(), Entity.class);
				if (thisEnt == null) {
					JOptionPane.showMessageDialog(null, "Entity: " + objName.getText() + " not found", "Notice", JOptionPane.INFORMATION_MESSAGE);
				}
				else {
					thisEnt.setFlag(Entity.FLAG_TRACKEVENTS);
				}
			}
		});

		// build control buttons at the bottom of the dialog
		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout( new FlowLayout( FlowLayout.CENTER ) );
		buttonPanel.add( trackButton );
		buttonPanel.add( closeButton );
		getContentPane().add( "South", buttonPanel );

		addWindowListener( new java.awt.event.WindowAdapter() {

			@Override
			public void windowClosed( java.awt.event.WindowEvent e ) {
				for (Entity each : Entity.getAll()) {
					each.clearFlag(Entity.FLAG_TRACKEVENTS);
				}
			}
		} );

		// size and display the dialog
		pack();
		setVisible( true );
	}
}
