/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2021 JaamSim Software Inc.
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
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.ArrayList;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.border.EmptyBorder;

import com.jaamsim.basicsim.JaamSimModel;
import com.jaamsim.basicsim.Simulation;

public class RunProgressBox extends JFrame {

	private static RunProgressBox myInstance;
	private final ArrayList<JLabel> labelList;
	private final ArrayList<JProgressBar> progressBarList;
	private static String LABEL_FORMAT = "THREAD %s:  scenario %s, replication %s";

	public RunProgressBox() {
		super("Run Progress");
		setType(Type.UTILITY);
		setAutoRequestFocus(false);
		setAlwaysOnTop(true);
		setResizable(false);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);

		JPanel barPanel = new JPanel();
		barPanel.setLayout( new GridLayout(0, 2, 5, 5) );
		barPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

		Simulation simulation = GUIFrame.getJaamSimModel().getSimulation();
		int numberOfThreads = simulation.getNumberOfThreads();
		numberOfThreads = Math.min(numberOfThreads, simulation.getNumberOfRuns());
		labelList = new ArrayList<>(numberOfThreads);
		progressBarList = new ArrayList<>(numberOfThreads);

		// Add a progress bar for each thread
		for (int i = 0; i < numberOfThreads; i++) {

			// Label for the progress bar
			String str = String.format(LABEL_FORMAT, i + 1, "0000", "0000");
			JLabel label = new JLabel(str);
			barPanel.add(label);

			// Progress bar
			JProgressBar bar = new JProgressBar(0, 100);
			bar.setPreferredSize( new Dimension(120, 20) );
			bar.setValue( 0 );
			bar.setStringPainted( true );
			barPanel.add(bar);

			// Save the labels and progress bars for future updates
			labelList.add(label);
			progressBarList.add(bar);
		}

		getContentPane().setLayout( new BorderLayout() );
		getContentPane().add(barPanel, BorderLayout.CENTER);

		pack();
		setLocationRelativeTo(null);
	}

	public synchronized static RunProgressBox getInstance() {
		if (myInstance == null)
			myInstance = new RunProgressBox();
		return myInstance;
	}

	public synchronized static boolean hasInstance() {
		return myInstance != null;
	}

	private synchronized static void killInstance() {
		myInstance = null;
	}

	@Override
	public void dispose() {
		killInstance();
		super.dispose();
	}

	public void update() {
		ArrayList<JaamSimModel> simModelList = GUIFrame.getRunManager().getSimModelList();
		for (int i = 0; i < progressBarList.size(); i++) {
			if (i >= simModelList.size()) {
				labelList.get(i).setText(String.format(LABEL_FORMAT, i + 1, "-", "-"));
				progressBarList.get(i).setValue(0);
				continue;
			}
			JaamSimModel sm = simModelList.get(i);
			String str = String.format(LABEL_FORMAT, i + 1, sm.getScenarioNumber(),
					sm.getReplicationNumber());
			labelList.get(i).setText(str);
			Simulation simulation = sm.getSimulation();
			double simTime = sm.getSimTime();
			int progress = (int) Math.round( simulation.getProgress(simTime) * 100.0d );
			progressBarList.get(i).setValue(progress);
		}
	}

}
