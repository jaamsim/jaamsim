/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2014 Ausenco Engineering Canada Inc.
 * Copyright (C) 2019-2020 JaamSim Software Inc.
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

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import com.jaamsim.basicsim.Log;
import com.jaamsim.basicsim.LogListener;

public class LogBox extends FrameBox {
	private static LogBox myInstance;

	private static final Listener logger = new Listener();

	private JTextArea logArea;

	static {
		Log.addListener(logger);
	}

	private LogBox() {
		super( "Log Viewer" );
		setDefaultCloseOperation(FrameBox.DISPOSE_ON_CLOSE);
		addWindowListener(FrameBox.getCloseListener("ShowLogViewer"));


		logArea = new JTextArea();
		logArea.setEditable(false);

		JScrollPane scrollPane = new JScrollPane(logArea);

		getContentPane().add( scrollPane );

		addComponentListener(FrameBox.getSizePosAdapter(this, "LogViewerSize", "LogViewerPos"));
	}

	/**
	 * Returns the only instance of the property box
	 */
	public synchronized static LogBox getInstance() {
		if (myInstance == null) {
			myInstance = new LogBox();
			logger.init();
		}

		return myInstance;
	}

	private synchronized static void killInstance() {
		myInstance = null;
	}

	private synchronized static boolean hasInstance() {
		return (myInstance != null);
	}

	@Override
	public void dispose() {
		killInstance();
		super.dispose();
	}

	private static class Listener implements Runnable, LogListener {
		private AtomicBoolean isScheduled = new AtomicBoolean();
		private int lastIdx = 0;

		public void init() {
			lastIdx = 0;
			update();
		}

		@Override
		public void update() {
			if (!isScheduled.getAndSet(true))
				SwingUtilities.invokeLater(this);
		}

		@Override
		public void run() {
			isScheduled.set(false);

			if (!hasInstance())
				return;

			ArrayList<String> lines = Log.getLog(lastIdx);
			lastIdx += lines.size();

			for (String line : lines)
				myInstance.logArea.append(line + "\n");
		}
	}
}
