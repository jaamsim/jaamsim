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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import com.jaamsim.input.InputAgent;

/**
 * Implements a graphic interface to display Exceptions to the user
 */
public class ExceptionBox extends javax.swing.JDialog implements ActionListener{

	public ExceptionBox( Throwable e ) {
		this( e, true );
	}

	public ExceptionBox( Throwable e, boolean stackTraceFlag ) {
		super();

		// display debugging information to the developer in the command screen
		if( stackTraceFlag ) {
			e.printStackTrace();
		}

		InputAgent.doError(e);

		//setup the dialog
		setTitle( "TERMINAL ERROR" );
		//setModal( true );
		setDefaultCloseOperation( javax.swing.JDialog.DISPOSE_ON_CLOSE );

		// setup the error mesage area
		javax.swing.JTextArea txt = new javax.swing.JTextArea();
		txt.setText( "An error occurred in the simulation environment.  Please check inputs for an error:\r\n" + e );
		//txt.append( "\r\n\r\nClick OK to Terminate the Application" );
		txt.setMargin( new java.awt.Insets( 10, 10, 10, 10 ) );
		txt.setLineWrap( true );
		txt.setWrapStyleWord( true );
		getContentPane().add( txt );

		// setup the OK button
		javax.swing.JButton okButton = new javax.swing.JButton( "OK" );
		okButton.addActionListener(this);
		getContentPane().add(okButton, java.awt.BorderLayout.SOUTH);

		// set size and display
		setSize( 400, 200 );
		setVisible( true );
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		this.dispose();
	}
}
