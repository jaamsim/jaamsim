/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2002-2011 Ausenco Engineering Canada Inc.
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

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;

/**
 * Class to display information about model objects.
 */
public class AboutBox extends JFrame {

	/**
	 * Constructor setting the layout of the box and the window title.
	 */
	public AboutBox() {
		// build the Frame
		super( "About" );
		setResizable( false );
		setDefaultCloseOperation( JFrame.DISPOSE_ON_CLOSE );

		GridBagConstraints constraints;
		GridBagLayout layout;

		// build the BOLD font
		java.awt.Font boldFont = new java.awt.Font( null, java.awt.Font.BOLD, 12 );

		// setup the layout
		int index = 0;
		layout = new GridBagLayout();
		getContentPane().setLayout( layout );

		// build the default constraints
		constraints = new GridBagConstraints();
		constraints.anchor = GridBagConstraints.WEST;
		constraints.gridx = index++;
		constraints.gridy = 0;

		constraints.insets = new Insets( 15, 75, 0, 75 );

		// display the model's name
		JLabel lab = new JLabel(DisplayEntity.simulation.getModelName());
		lab.setFont( boldFont );
		layout.setConstraints( lab, constraints );
		getContentPane().add( lab );

		// display model info
		lab = new JLabel( "version: 0.1");
		constraints.gridy = index++;
		constraints.insets = new Insets( 0, 75, 0, 75 );
		layout.setConstraints( lab, constraints );
		getContentPane().add( lab );

		lab = new JLabel("Copyright (C) 2011 Ausenco Engineering Canada Inc.");
		constraints.gridy = index++;
		layout.setConstraints( lab, constraints );
		getContentPane().add( lab );

		JButton closeButton = new JButton( "OK" );
		closeButton.addActionListener( new ActionListener() {

			public void actionPerformed( ActionEvent e ) {
				setVisible( false );
				dispose();
			}
		} );
		constraints.gridy = index++;
		constraints.insets = new Insets( 10, 75, 15, 75 );
		constraints.anchor = GridBagConstraints.CENTER;
		layout.setConstraints( closeButton, constraints );
		getContentPane().add( closeButton );

		setSize( 300, 150 );
		pack();
		setVisible( true );
	}
}
