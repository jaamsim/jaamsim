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
package com.jaamsim.basicsim;

import java.io.File;

import javax.swing.JFrame;

import com.jaamsim.events.EventManager;
import com.jaamsim.input.BooleanInput;
import com.jaamsim.input.DirInput;
import com.jaamsim.input.Input;
import com.jaamsim.input.InputAgent;
import com.jaamsim.input.IntegerInput;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.Output;
import com.jaamsim.input.ValueInput;
import com.jaamsim.ui.EditBox;
import com.jaamsim.ui.EntityPallet;
import com.jaamsim.ui.FrameBox;
import com.jaamsim.ui.GUIFrame;
import com.jaamsim.ui.LogBox;
import com.jaamsim.ui.ObjectSelector;
import com.jaamsim.ui.OutputBox;
import com.jaamsim.ui.PropertyBox;
import com.jaamsim.units.TimeUnit;

/**
 * Simulation provides the basic structure for the Entity model lifetime of earlyInit,
 * startUp and doEndAt.  The initial processtargets required to start the model are
 * added to the eventmanager here.  This class also acts as a bridge to the UI by
 * providing controls for the various windows.
 */
public class Simulation extends Entity {
	@Keyword(description = "The initialization period for the simulation run. The model will " +
	                "run for the initialization period and then clear the statistics " +
	                "and execute for the specified run duration. The total length of the " +
	                "simulation run will be the sum of Initialization and Duration.",
	         example = "Simulation Initialization { 720 h }")
	private static final ValueInput initializationTime;

	@Keyword(description = "Time at which the simulation run is started (hh:mm).",
	         example = "Simulation StartTime { 2160 h }")
	private static final ValueInput startTimeInput;

	@Keyword(description = "The duration of the simulation run in which all statistics will be recorded.",
	         example = "Simulation Duration { 8760 h }")
	private static final ValueInput runDuration;

	@Keyword(description = "The time at which the simulation will be paused.",
	         example = "Simulation PauseTime { 200 h }")
	private static final ValueInput pauseTime;

	@Keyword(description = "Indicates whether an output report will be printed at the end of the simulation run.",
	         example = "Simulation PrintReport { TRUE }")
	private static final BooleanInput printReport;

	@Keyword(description = "The directory in which to place the output report.\n" +
			"Defaults to the directory containing the configuration file for the run.",
			example = "ReportGenerator1 ReportDirectory { 'c:\reports\' }")
	private static final DirInput reportDirectory;

	@Keyword(description = "The length of time represented by one simulation tick.",
	         example = "Simulation TickLength { 1e-6 s }")
	private static final ValueInput tickLengthInput;

	@Keyword(description = "If the value is TRUE, then the input report file will be printed after loading the " +
	                "configuration file.  The input report can always be generated when needed by selecting " +
	                "\"Print Input Report\" under the File menu.",
	         example = "Simulation PrintInputReport { TRUE }")
	private static final BooleanInput printInputReport;

	@Keyword(description = "This is placeholder description text",
	         example = "This is placeholder example text")
	private static final BooleanInput traceEventsInput;

	@Keyword(description = "This is placeholder description text",
	         example = "This is placeholder example text")
	private static final BooleanInput verifyEventsInput;

	@Keyword(description = "The real time speed up factor",
	         example = "Simulation RealTimeFactor { 1200 }")
	private static final IntegerInput realTimeFactor;

	public static final int DEFAULT_REAL_TIME_FACTOR = 10000;
	public static final int MIN_REAL_TIME_FACTOR = 1;
	public static final int MAX_REAL_TIME_FACTOR= 1000000;

	@Keyword(description = "A Boolean to turn on or off real time in the simulation run",
	         example = "Simulation RealTime { TRUE }")
	private static final BooleanInput realTime;

	@Keyword(description = "Indicates whether to close the program on completion of the simulation run.",
	         example = "Simulation ExitAtStop { TRUE }")
	private static final BooleanInput exitAtStop;

	@Keyword(description = "Indicates whether the Model Builder tool should be shown on startup.",
	         example = "Simulation ShowModelBuilder { TRUE }")
	private static final BooleanInput showModelBuilder;

	@Keyword(description = "Indicates whether the Object Selector tool should be shown on startup.",
	         example = "Simulation ShowObjectSelector { TRUE }")
	private static final BooleanInput showObjectSelector;

	@Keyword(description = "Indicates whether the Input Editor tool should be shown on startup.",
	         example = "Simulation ShowInputEditor { TRUE }")
	private static final BooleanInput showInputEditor;

	@Keyword(description = "Indicates whether the Output Viewer tool should be shown on startup.",
	         example = "Simulation ShowOutputViewer { TRUE }")
	private static final BooleanInput showOutputViewer;

	@Keyword(description = "Indicates whether the Output Viewer tool should be shown on startup.",
	         example = "Simulation ShowPropertyViewer { TRUE }")
	private static final BooleanInput showPropertyViewer;

	@Keyword(description = "Indicates whether the Log Viewer tool should be shown on startup.",
	         example = "Simulation ShowLogViewer { TRUE }")
	private static final BooleanInput showLogViewer;

	private static double timeScale; // the scale from discrete to continuous time
	private static double startTime; // simulation time (seconds) for the start of the run (not necessarily zero)
	private static double endTime;   // simulation time (seconds) for the end of the run

	private static Simulation myInstance;

	private static String modelName = "JaamSim";

	static {
		initializationTime = new ValueInput("InitializationDuration", "Key Inputs", 0.0);
		initializationTime.setUnitType(TimeUnit.class);
		initializationTime.setValidRange(0.0d, Double.POSITIVE_INFINITY);

		runDuration = new ValueInput("RunDuration", "Key Inputs", 31536000.0d);
		runDuration.setUnitType(TimeUnit.class);
		runDuration.setValidRange(1e-15d, Double.POSITIVE_INFINITY);

		pauseTime = new ValueInput("PauseTime", "GUI", Double.POSITIVE_INFINITY);
		pauseTime.setUnitType(TimeUnit.class);
		pauseTime.setValidRange(0.0d, Double.POSITIVE_INFINITY);

		printReport = new BooleanInput("PrintReport", "Key Inputs", false);

		reportDirectory = new DirInput("ReportDirectory", "Key Inputs", null);

		startTimeInput = new ValueInput("StartTime", "Key Inputs", 0.0d);
		startTimeInput.setUnitType(TimeUnit.class);
		startTimeInput.setValidRange(0.0d, Double.POSITIVE_INFINITY);

		tickLengthInput = new ValueInput("TickLength", "Key Inputs", 1e-6d);
		tickLengthInput.setUnitType(TimeUnit.class);
		tickLengthInput.setValidRange(1e-9d, 5.0d);

		traceEventsInput = new BooleanInput("TraceEvents", "Key Inputs", false);
		verifyEventsInput = new BooleanInput("VerifyEvents", "Key Inputs", false);

		printInputReport = new BooleanInput("PrintInputReport", "Key Inputs", false);

		realTimeFactor = new IntegerInput("RealTimeFactor", "GUI", DEFAULT_REAL_TIME_FACTOR);
		realTimeFactor.setValidRange(MIN_REAL_TIME_FACTOR, MAX_REAL_TIME_FACTOR);

		realTime = new BooleanInput("RealTime", "GUI", false);

		exitAtStop = new BooleanInput("ExitAtStop", "Key Inputs", false);

		showModelBuilder = new BooleanInput("ShowModelBuilder", "GUI", false);
		showObjectSelector = new BooleanInput("ShowObjectSelector", "GUI", false);
		showInputEditor = new BooleanInput("ShowInputEditor", "GUI", false);
		showOutputViewer = new BooleanInput("ShowOutputViewer", "GUI", false);
		showPropertyViewer = new BooleanInput("ShowPropertyViewer", "GUI", false);
		showLogViewer = new BooleanInput("ShowLogViewer", "GUI", false);

		// Initialize basic model information
		startTime = 0.0;
		endTime = 8760.0*3600.0;
	}

	{
		this.addInput(runDuration);
		this.addInput(initializationTime);
		this.addInput(pauseTime);
		this.addInput(printReport);
		this.addInput(reportDirectory);

		this.addInput(startTimeInput);

		this.addInput(tickLengthInput);

		this.addInput(traceEventsInput);
		this.addInput(verifyEventsInput);

		this.addInput(printInputReport);

		this.addInput(realTimeFactor);
		this.addInput(realTime);

		this.addInput(exitAtStop);

		this.addInput(showModelBuilder);
		this.addInput(showObjectSelector);
		this.addInput(showInputEditor);
		this.addInput(showOutputViewer);
		this.addInput(showPropertyViewer);
		this.addInput(showLogViewer);

		attributeDefinitionList.setHidden(true);
		startTimeInput.setHidden(true);
		traceEventsInput.setHidden(true);
		verifyEventsInput.setHidden(true);
		printInputReport.setHidden(true);
	}

	public Simulation() {}

	public static Simulation getInstance() {
		if (myInstance == null) {
			for (Entity ent : Entity.getAll()) {
				if (ent instanceof Simulation ) {
					myInstance = (Simulation) ent;
					break;
				}
			}
		}
		return myInstance;
	}

	@Override
	public void updateForInput( Input<?> in ) {
		super.updateForInput( in );

		if(in == realTimeFactor || in == realTime) {
			updateRealTime();
			return;
		}

		if (in == pauseTime) {
			updatePauseTime();
			return;
		}

		if (in == reportDirectory) {
			InputAgent.setReportDirectory(reportDirectory.getDir());
			return;
		}

		if (in == showModelBuilder) {
			setWindowVisible(EntityPallet.getInstance(), showModelBuilder.getValue());
			return;
		}

		if (in == showObjectSelector) {
			setWindowVisible(ObjectSelector.getInstance(), showObjectSelector.getValue());
			return;
		}

		if (in == showInputEditor) {
			setWindowVisible(EditBox.getInstance(), showInputEditor.getValue());
			FrameBox.reSelectEntity();
			return;
		}

		if (in == showOutputViewer) {
			setWindowVisible(OutputBox.getInstance(), showOutputViewer.getValue());
			FrameBox.reSelectEntity();
			return;
		}

		if (in == showPropertyViewer) {
			setWindowVisible(PropertyBox.getInstance(), showPropertyViewer.getValue());
			FrameBox.reSelectEntity();
			return;
		}

		if (in == showLogViewer) {
			setWindowVisible(LogBox.getInstance(), showLogViewer.getValue());
			FrameBox.reSelectEntity();
			return;
		}
	}

	public static void clear() {
		initializationTime.reset();
		runDuration.reset();
		pauseTime.reset();
		tickLengthInput.reset();
		traceEventsInput.reset();
		verifyEventsInput.reset();
		printInputReport.reset();
		realTimeFactor.reset();
		realTime.reset();
		updateRealTime();
		exitAtStop.reset();

		startTimeInput.reset();

		showModelBuilder.reset();
		showObjectSelector.reset();
		showInputEditor.reset();
		showOutputViewer.reset();
		showPropertyViewer.reset();
		showLogViewer.reset();

		// Initialize basic model information
		startTime = 0.0;
		endTime = 8760.0*3600.0;

		// close warning/error trace file
		InputAgent.closeLogFile();

		// Kill all entities except simulation
		while(Entity.getAll().size() > 0) {
			Entity ent = Entity.getAll().get(Entity.getAll().size()-1);
			ent.kill();
		}
	}

	/**
	 *	Initializes and starts the model
	 *		1) Initializes EventManager to accept events.
	 *		2) calls startModel() to allow the model to add its starting events to EventManager
	 *		3) start EventManager processing events
	 */
	public static void start(EventManager evt) {
		// Validate each entity based on inputs only
		for (int i = 0; i < Entity.getAll().size(); i++) {
			try {
				Entity.getAll().get(i).validate();
			}
			catch (Throwable e) {
				LogBox.format("%s: Validation error- %s", Entity.getAll().get(i).getName(), e.getMessage());
				GUIFrame.showErrorDialog("Input Error Detected During Validation",
				                         "%s: %-70s",
				                         Entity.getAll().get(i).getName(), e.getMessage());

				GUIFrame.instance().updateForSimulationState(GUIFrame.SIM_STATE_CONFIGURED);
				return;
			}
		}

		InputAgent.prepareReportDirectory();
		evt.clear();
		evt.setTraceListener(null);

		if( Simulation.traceEvents() ) {
			String evtName = InputAgent.getConfigFile().getParentFile() + File.separator + InputAgent.getRunName() + ".evt";
			EventRecorder rec = new EventRecorder(evtName);
			evt.setTraceListener(rec);
		}
		else if( Simulation.verifyEvents() ) {
			String evtName = InputAgent.getConfigFile().getParentFile() + File.separator + InputAgent.getRunName() + ".evt";
			EventTracer trc = new EventTracer(evtName);
			evt.setTraceListener(trc);
		}

		evt.setTickLength(tickLengthInput.getValue());
		setSimTimeScale(evt.secondsToNearestTick(3600.0d));
		FrameBox.setSecondsPerTick(tickLengthInput.getValue());

		startTime = startTimeInput.getValue();
		endTime = startTime + Simulation.getInitializationTime() + Simulation.getRunDuration();

		evt.scheduleProcessExternal(0, Entity.PRIO_DEFAULT, false, new InitModelTarget(), null);
		evt.resume(evt.secondsToNearestTick(Simulation.getPauseTime()));
	}

	public static boolean getPrintReport() {
		return printReport.getValue();
	}

	public static boolean traceEvents() {
		return traceEventsInput.getValue();
	}

	public static boolean verifyEvents() {
		return verifyEventsInput.getValue();
	}

	static void setSimTimeScale(double scale) {
		timeScale = scale;
	}

	public static double getSimTimeFactor() {
		return timeScale;
	}

	public static double getEventTolerance() {
		return (1.0d / getSimTimeFactor());
	}

	public static double getPauseTime() {
		return pauseTime.getValue();
	}

	/**
	 * Returns the start time of the run.
	 * @return - simulation time in seconds for the start of the run.
	 */
	public static double getStartTime() {
		return startTime;
	}

	/**
	 * Returns the end time of the run.
	 * @return - simulation time in seconds when the current run will stop.
	 */
	public static double getEndTime() {
		return endTime;
	}

	/**
	 * Return the run duration for the run (not including intialization)
	 */
	public static double getRunDurationHours() {
		return Simulation.getRunDuration() / 3600.0d;
	}

	/**
	 * Returns the start time of the run.
	 */
	public static double getStartHours() {
		return startTime/3600.0d;
	}

	/**
	 * Return the initialization duration in hours
	 */
	public static double getInitializationHours() {
		return Simulation.getInitializationTime() / 3600.0d;
	}

	/**
	 * Returns the duration of the run (not including intialization)
	 */
	public static double getRunDuration() {
		return runDuration.getValue();
	}

	/**
	 * Returns the duration of the initialization period
	 */
	public static double getInitializationTime() {
		return initializationTime.getValue();
	}

	static void updateRealTime() {
		GUIFrame.instance().updateForRealTime(realTime.getValue(), realTimeFactor.getValue());
	}

	static void updatePauseTime() {
		GUIFrame.instance().updateForPauseTime(pauseTime.getValueString());
	}

	public static void setModelName(String newModelName) {
		modelName = newModelName;
	}

	public static String getModelName() {
		return modelName;
	}

	public static boolean getExitAtStop() {
		return exitAtStop.getValue();
	}

	public static boolean getPrintInputReport() {
		return printInputReport.getValue();
	}

	private static void setWindowVisible(JFrame f, boolean visible) {
		f.setVisible(visible);
		if (visible)
			f.toFront();
	}

	/**
	 * Re-open any Tools windows that have been closed temporarily.
	 */
	public static void showActiveTools() {
		setWindowVisible(EntityPallet.getInstance(), showModelBuilder.getValue());
		setWindowVisible(ObjectSelector.getInstance(), showObjectSelector.getValue());
		setWindowVisible(EditBox.getInstance(), showInputEditor.getValue());
		setWindowVisible(OutputBox.getInstance(), showOutputViewer.getValue());
		setWindowVisible(PropertyBox.getInstance(), showPropertyViewer.getValue());
		setWindowVisible(LogBox.getInstance(), showLogViewer.getValue());
	}

	/**
	 * Closes all the Tools windows temporarily.
	 */
	public static void closeAllTools() {
		setWindowVisible(EntityPallet.getInstance(), false);
		setWindowVisible(ObjectSelector.getInstance(), false);
		setWindowVisible(EditBox.getInstance(), false);
		setWindowVisible(OutputBox.getInstance(), false);
		setWindowVisible(PropertyBox.getInstance(), false);
		setWindowVisible(LogBox.getInstance(), false);
	}

	@Output(name = "Configuration File",
			 description = "The present configuration file.")
	public String getConfigFileName(double simTime) {
		return InputAgent.getConfigFile().getPath();
	}
}
