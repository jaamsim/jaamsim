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
package com.jaamsim.ui;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JLabel;

import com.sandwell.JavaSimulation.Simulation;

/**
 * Class to display information about model objects.
 */
public class AboutBox extends FrameBox implements ActionListener {
	private static AboutBox instance;

	public AboutBox() {
		super("About");

		setResizable(false);
		setDefaultCloseOperation(FrameBox.HIDE_ON_CLOSE);

		// setup the layout
		int index = 0;
		GridBagLayout layout = new GridBagLayout();
		getContentPane().setLayout(layout);

		// build the default constraints
		GridBagConstraints constraints = new GridBagConstraints();
		constraints.anchor = GridBagConstraints.WEST;
		constraints.gridx = index++;
		constraints.gridy = 0;

		constraints.insets = new Insets( 15, 75, 0, 75 );

		// display the model's name
		JLabel lab = new JLabel(Simulation.getModelName());
		lab.setFont(boldFont);
		layout.setConstraints( lab, constraints );
		getContentPane().add( lab );

		// display model info
		lab = new JLabel("Version: 2013-27");
		constraints.gridy = index++;
		constraints.insets = new Insets( 0, 75, 0, 75 );
		layout.setConstraints( lab, constraints );
		getContentPane().add( lab );

		lab = new JLabel("Copyright (C) 2013 Ausenco Engineering Canada Inc.");
		constraints.gridy = index++;
		layout.setConstraints( lab, constraints );
		getContentPane().add( lab );

		JButton closeButton = new JButton("OK");
		closeButton.addActionListener(this);

		constraints.gridy = index++;
		constraints.insets = new Insets( 10, 75, 15, 75 );
		constraints.anchor = GridBagConstraints.CENTER;
		layout.setConstraints( closeButton, constraints );
		getContentPane().add( closeButton );

		setSize( 300, 150 );
		pack();
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		setVisible(false);
	}

	public static synchronized AboutBox instance() {
		if (instance == null)
			instance = new AboutBox();

		return instance;
	}
}
