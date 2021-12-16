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
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.ArrayList;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.border.EmptyBorder;

import com.jaamsim.basicsim.JaamSimModel;
import com.jaamsim.basicsim.Simulation;

public class RunProgressBox extends JFrame {

	private static RunProgressBox myInstance;
	private final ArrayList<JLabel> labelList;
	private final ArrayList<JProgressBar> progressBarList;
	private final JProgressBar overallBar;
	private final JLabel rateLabel;
	private final JLabel remainingTimeLabel;
	private boolean show;

	private long resumeSystemTime;
	private long lastSystemTime;
	private double lastOverallProgress;
	private double progressRate;

	private static String LABEL_FORMAT = "THREAD %s:  scenario %s, replication %s";
	private static String LABEL_FORMAT_SHORT = "THREAD %s:";

	public RunProgressBox() {
		super("Run Progress");
		setType(Type.UTILITY);
		setAutoRequestFocus(false);
		setAlwaysOnTop(true);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);

		init();

		GridLayout grid = new GridLayout(0, 2, 5, 5);
		Dimension barDim = new Dimension(120, 20);
		int gap = 10;      // insets
		int maxBars = 10;  // number of progress bars to display

		JPanel barPanel = new JPanel(grid);
		barPanel.setBorder(new EmptyBorder(0, gap, 0, gap));

		Simulation simulation = GUIFrame.getJaamSimModel().getSimulation();
		int numberOfThreads = simulation.getNumberOfThreads();
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
			bar.setPreferredSize(barDim);
			bar.setValue( 0 );
			bar.setStringPainted( true );
			barPanel.add(bar);

			// Save the labels and progress bars for future updates
			labelList.add(label);
			progressBarList.add(bar);
		}

		getContentPane().setLayout( new BorderLayout() );
		JScrollPane scrollPane = new JScrollPane(barPanel);
		scrollPane.setBorder(new EmptyBorder(gap, 0, gap, 0));
		Dimension dim = scrollPane.getPreferredSize();
		dim.height = Math.min(dim.height, maxBars*barDim.height + (maxBars - 1)*grid.getVgap()
				+ 2*gap);
		scrollPane.setPreferredSize(dim);
		getContentPane().add(scrollPane, BorderLayout.CENTER);

		// Overall progress bar
		JPanel overallBarPanel = new JPanel(grid);
		overallBarPanel.setBorder(new EmptyBorder(gap, gap, gap, gap));
		overallBarPanel.add(new JLabel("OVERALL PROGRESS"));

		overallBar = new JProgressBar(0, 100);
		overallBar.setPreferredSize(barDim);
		overallBar.setValue( 0 );
		overallBar.setStringPainted( true );
		overallBarPanel.add(overallBar);

		getContentPane().add(overallBarPanel, BorderLayout.SOUTH);

		// Run processing rate
		rateLabel = new JLabel("- runs per hour");
		rateLabel.setForeground(Color.RED);
		rateLabel.setToolTipText(GUIFrame.formatToolTip("Run Processing Rate",
				"The rate at which simulation runs are being executed."));
		overallBarPanel.add(rateLabel);

		// Remaining execution time
		remainingTimeLabel = new JLabel("- seconds left");
		remainingTimeLabel.setForeground(Color.RED);
		remainingTimeLabel.setToolTipText(GUIFrame.formatToolTip("Remaining Time",
				"The remaining time required to complete all the simulation runs."));
		overallBarPanel.add(remainingTimeLabel);

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

	public void init() {
		resumeSystemTime = System.currentTimeMillis();
		lastSystemTime = resumeSystemTime;
		lastOverallProgress = GUIFrame.getRunManager().getProgress();
	}

	public void update() {
		try {
			// Progress bar for each thread
			ArrayList<JaamSimModel> simModelList = GUIFrame.getRunManager().getSimModelList();
			for (int i = 0; i < progressBarList.size(); i++) {
				if (i >= simModelList.size()) {
					labelList.get(i).setText(String.format(LABEL_FORMAT_SHORT, i + 1));
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

			// Overall progress bar
			double overallProgress = GUIFrame.getRunManager().getProgress();
			int progress = (int) Math.round( overallProgress * 100.0d );
			overallBar.setValue(progress);

			// Run processing rate
			long millis = System.currentTimeMillis();
			long elapsedMillis = millis - lastSystemTime;
			if ((elapsedMillis > 5000L || millis - resumeSystemTime < 5000L)
					&& GUIFrame.getRunManager().isRunning()) {

				// Determine the processing rate
				progressRate = (overallProgress - lastOverallProgress)*1000.0d/elapsedMillis;
				double processingRate = progressRate * GUIFrame.getJaamSimModel().getSimulation().getNumberOfRuns();
				rateLabel.setText(String.format("%,.1f runs per hour", processingRate * 3600.0d));

				if (elapsedMillis > 5000L) {
					lastSystemTime = millis;
					lastOverallProgress = overallProgress;
				}
			}

			// Remaining execution time
			double remainingTime = (1.0d - overallProgress) / progressRate;
			remainingTimeLabel.setText(GUIFrame.getRemainingTimeString(remainingTime));
		}
		catch (Throwable t) {}
	}

	public boolean getShow() {
		return show;
	}

	public void setShow(boolean bool) {
		show = bool;
		if (bool == isVisible())
			return;
		setVisible(bool);
	}

}
