/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2014 Ausenco Engineering Canada Inc.
 * Copyright (C) 2019 JaamSim Software Inc.
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

import java.awt.Point;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import com.jaamsim.basicsim.JaamSimModel;
import com.jaamsim.basicsim.Simulation;

public class LogBox extends FrameBox {

	private static LogBox myInstance;

	private static Object logLock = new Object();
	private static StringBuilder logBuilder = new StringBuilder();

	private JTextArea logArea;

	public LogBox() {
		super( "Log Viewer" );
		setDefaultCloseOperation(FrameBox.DISPOSE_ON_CLOSE);
		addWindowListener(FrameBox.getCloseListener("ShowLogViewer"));

		synchronized(logLock) {
			logArea = new JTextArea(logBuilder.toString());
			logArea.setEditable(false);
		}

		JScrollPane scrollPane = new JScrollPane(logArea);

		getContentPane().add( scrollPane );

		Simulation simulation = GUIFrame.getJaamSimModel().getSimulation();
		GUIFrame gui = GUIFrame.getInstance();
		Point pt = gui.getGlobalLocation(simulation.getLogViewerPos().get(0),
				simulation.getLogViewerPos().get(1));
		setLocation(pt);
		setSize(simulation.getLogViewerSize().get(0), simulation.getLogViewerSize().get(1));

		addComponentListener(new ComponentAdapter() {

			@Override
			public void componentMoved(ComponentEvent e) {
				Point pt = gui.getRelativeLocation(getLocation().x, getLocation().y);
				Simulation simulation = GUIFrame.getJaamSimModel().getSimulation();
				simulation.setLogViewerPos(pt.x, pt.y);
			}

			@Override
			public void componentResized(ComponentEvent e) {
				Simulation simulation = GUIFrame.getJaamSimModel().getSimulation();
				simulation.setLogViewerSize(getSize().width, getSize().height);
			}
		});
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

		System.err.println(stackTrace);
	}

	public static void renderLogException(Throwable ex) {

		// Suppress renderer error messages when in batch mode
		JaamSimModel simModel = GUIFrame.getJaamSimModel();
		if (simModel == null || simModel.isBatchRun())
			return;

		logException(ex);
	}

}
