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
package com.jaamsim.ui;

import javax.swing.JOptionPane;
import javax.swing.JTextArea;

import com.jaamsim.input.InputAgent;
import com.sandwell.JavaSimulation.Entity;

/**
 * Implements a graphic interface to display Exceptions to the user
 */
public class ExceptionBox extends FrameBox {
	private static ExceptionBox instance;

	private final JTextArea txt;

	private ExceptionBox() {
		super("FATAL ERROR");

		setDefaultCloseOperation(FrameBox.HIDE_ON_CLOSE);
		setAlwaysOnTop(true);

		// setup the error mesage area
		txt = new JTextArea();
		txt.setText( "An error occurred in the simulation environment.  Please check inputs for an error:\r\n");
		txt.setMargin( new java.awt.Insets( 10, 10, 10, 10 ) );
		txt.setLineWrap( true );
		txt.setWrapStyleWord( true );
		getContentPane().add( txt );

		// setup the OK button
		javax.swing.JButton okButton = new javax.swing.JButton("OK");
		okButton.addActionListener(new java.awt.event.ActionListener() {

			@Override
			public void actionPerformed(java.awt.event.ActionEvent ae) {
				ExceptionBox.this.setVisible(false);

			}
		});
		getContentPane().add(okButton, java.awt.BorderLayout.SOUTH);

		// set size and display
		setSize( 400, 200 );
	}

	public void setErrorBox(String fmt, Object... args) {
		setTitle("INPUT ERROR");

		txt.setText(String.format(fmt, args));
		this.setVisible(true);
	}

	public void setError(Throwable e) {
		InputAgent.doError(e);

		StringBuilder s = new StringBuilder("An error occurred\n\n");
		s.append("Message: ").append(e.getMessage());
		s.append(" (").append(e.getClass().getSimpleName()).append(")\n\n");
		s.append("Debug Information:\n\n");
		for (StackTraceElement elm : e.getStackTrace())
			s.append(elm.toString()).append("\n");

		txt.setText(s.toString());
		setTitle("FATAL ERROR");
		this.setVisible(true);
	}

	public static synchronized ExceptionBox instance() {
		if (instance == null)
			instance = new ExceptionBox();

		return instance;
	}

	public void setInputError(Entity ent, Throwable e) {
		JOptionPane.showMessageDialog(EditBox.getInstance(),
				   String.format("Reason: %-70s", e.getMessage()),
				   String.format("Input Validation Failure - %s", ent.getInputName()),
				   JOptionPane.ERROR_MESSAGE);
	}
}
