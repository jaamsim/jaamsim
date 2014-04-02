/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2014 Ausenco Engineering Canada Inc.
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

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import com.jaamsim.input.InputAgent;
import com.sandwell.JavaSimulation.Simulation;
import com.sandwell.JavaSimulation3D.GUIFrame;

public class LogBox extends FrameBox {

	private static LogBox myInstance;

	private static Object logLock = new Object();
	private static StringBuilder logBuilder = new StringBuilder();

	private JTextArea logArea;

	public LogBox() {
		super( "Log Viewer" );
		setDefaultCloseOperation(FrameBox.DISPOSE_ON_CLOSE);
		this.addWindowListener(new CloseListener());

		synchronized(logLock) {
			logArea = new JTextArea(logBuilder.toString());
			logArea.setEditable(false);
		}

		JScrollPane scrollPane = new JScrollPane(logArea);

		getContentPane().add( scrollPane );

		setLocation(GUIFrame.COL3_START, GUIFrame.LOWER_START);
		setSize(GUIFrame.COL3_WIDTH, GUIFrame.LOWER_HEIGHT);
	}

	/**
	 * Listens for window events for the GUI.
	 *
	 */
	private class CloseListener extends WindowAdapter {
		@Override
		public void windowClosing(WindowEvent e) {
			InputAgent.processEntity_Keyword_Value(Simulation.getInstance(), "ShowLogViewer", "FALSE");
		}
	}

	/**
	 * Returns the only instance of the property box
	 */
	public synchronized static LogBox getInstance() {
		if (myInstance == null)
			myInstance = new LogBox();

		return myInstance;
	}

	private synchronized static void killInstance() {
		myInstance = null;
	}

	@Override
	public void dispose() {
		killInstance();
		super.dispose();
	}

	/**
	 * log a formated string, effectively wrapping String.format
	 * @param format
	 * @param args
	 */
	public static void format(String format, Object... args) {
		logLine(String.format(format, args));
	}

	public static void logLine(final String logLine) {
		synchronized(logLock) {
			logBuilder.append(logLine + "\n");
		}

		System.out.println(logLine);

		// Append to the existing log area if it exists
		if (myInstance != null) {
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					myInstance.logArea.append(logLine + "\n");
				}
			});
		}
	}

	public static void formatRenderLog(String format, Object... args) {
		logLine(String.format(format, args));
	}

	public static void renderLog(String line) {
		logLine(line);
	}

	/**
	 * Effectively prints the stack trace of 'ex' to the log
	 * @param ex
	 */
	public static void logException(Throwable ex) {
		// Below is some ugly java goofiness, but it works
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter( sw );
		ex.printStackTrace( pw );
		pw.flush();

		String stackTrace = sw.toString();
		logLine(stackTrace);
	}

	public static void renderLogException(Throwable ex) {
		logException(ex);
	}

}
