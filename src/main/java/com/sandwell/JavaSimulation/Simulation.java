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
package com.sandwell.JavaSimulation;

import java.io.File;

import javax.swing.JFrame;

import com.jaamsim.events.EventManager;
import com.jaamsim.events.ProcessTarget;
import com.jaamsim.input.BooleanInput;
import com.jaamsim.input.Input;
import com.jaamsim.input.InputAgent;
import com.jaamsim.input.InputErrorException;
import com.jaamsim.input.IntegerInput;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.Output;
import com.jaamsim.input.StringInput;
import com.jaamsim.input.ValueInput;
import com.jaamsim.ui.EditBox;
import com.jaamsim.ui.EntityPallet;
import com.jaamsim.ui.ExceptionBox;
import com.jaamsim.ui.FrameBox;
import com.jaamsim.ui.LogBox;
import com.jaamsim.ui.ObjectSelector;
import com.jaamsim.ui.OutputBox;
import com.jaamsim.ui.PropertyBox;
import com.jaamsim.units.DimensionlessUnit;
import com.jaamsim.units.TimeUnit;
import com.sandwell.JavaSimulation3D.Clock;
import com.sandwell.JavaSimulation3D.GUIFrame;

/**
 * Class Simulation - Sandwell Discrete Event Simulation
 * <p>
 * Class structure defining essential simulation objects.  Eventmanager is
 * instantiated to manage events generated in the simulation.  Function prototypes
 * are defined which any simulation must define in order to run.
 */
public class Simulation extends Entity {
	@Keyword(description = "The initialization period for the simulation run. The model will " +
	                "run for the initialization period and then clear the statistics " +
	                "and execute for the specified run duration. The total length of the " +
	                "simulation run will be the sum of Initialization and Duration.",
	         example = "Simulation Initialization { 720 h }")
	private static final ValueInput initializationTime;

	@Keyword(description = "Date at which the simulation run is started (yyyy-mm-dd). This " +
	                "input has no effect on the simulation results unless the seasonality " +
	                "factors vary from month to month.",
	         example = "Simulation StartDate { 2011-01-01 }")
	private static final StringInput startDate;

	@Keyword(description = "Time at which the simulation run is started (hh:mm).",
	         example = "Simulation StartTime { 2160 h }")
	private static final ValueInput startTimeInput;

	@Keyword(description = "The duration of the simulation run in which all statistics will be recorded.",
	         example = "Simulation Duration { 8760 h }")
	private static final ValueInput runDuration;

	@Keyword(description = "The number of discrete time units in one hour.",
	         example = "Simulation SimulationTimeScale { 4500 }")
	private static final ValueInput simTimeScaleInput;

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
	private static double startTime;
	private static double endTime;

	private static Simulation myInstance;

	private static String modelName = "JaamSim";

	static {
		initializationTime = new ValueInput("InitializationDuration", "Key Inputs", 0.0);
		initializationTime.setUnitType(TimeUnit.class);
		initializationTime.setValidRange(0.0d, Double.POSITIVE_INFINITY);

		runDuration = new ValueInput("RunDuration", "Key Inputs", 31536000.0d);
		runDuration.setUnitType(TimeUnit.class);
		runDuration.setValidRange(1e-15d, Double.POSITIVE_INFINITY);

		startDate = new StringInput("StartDate", "Key Inputs", null);

		startTimeInput = new ValueInput("StartTime", "Key Inputs", 0.0d);
		startTimeInput.setUnitType(TimeUnit.class);
		startTimeInput.setValidRange(0.0d, Double.POSITIVE_INFINITY);

		simTimeScaleInput = new ValueInput("SimulationTimeScale", "Key Inputs", 4000.0d);
		simTimeScaleInput.setUnitType(DimensionlessUnit.class);
		simTimeScaleInput.setValidRange(1e-15d, Double.POSITIVE_INFINITY);

		traceEventsInput = new BooleanInput("TraceEvents", "Key Inputs", false);
		verifyEventsInput = new BooleanInput("VerifyEvents", "Key Inputs", false);

		printInputReport = new BooleanInput("PrintInputReport", "Key Inputs", false);

		realTimeFactor = new IntegerInput("RealTimeFactor", "Key Inputs", DEFAULT_REAL_TIME_FACTOR);
		realTimeFactor.setValidRange(MIN_REAL_TIME_FACTOR, MAX_REAL_TIME_FACTOR);

		realTime = new BooleanInput("RealTime", "Key Inputs", false);

		exitAtStop = new BooleanInput("ExitAtStop", "Key Inputs", false);

		showModelBuilder = new BooleanInput("ShowModelBuilder", "Key Inputs", false);
		showObjectSelector = new BooleanInput("ShowObjectSelector", "Key Inputs", false);
		showInputEditor = new BooleanInput("ShowInputEditor", "Key Inputs", false);
		showOutputViewer = new BooleanInput("ShowOutputViewer", "Key Inputs", false);
		showPropertyViewer = new BooleanInput("ShowPropertyViewer", "Key Inputs", false);
		showLogViewer = new BooleanInput("ShowLogViewer", "Key Inputs", false);

		// Create clock
		Clock.setStartDate(2000, 1, 1);

		// Initialize basic model information
		startTime = 0.0;
		endTime = 8760.0;
	}

	{
		this.addInput(runDuration);
		this.addInput(initializationTime);

		this.addInput(startDate);

		this.addInput(startTimeInput);

		this.addInput(simTimeScaleInput);

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
	public void validate() {
		super.validate();

		if( startDate.getValue() != null && !Tester.isDate( startDate.getValue() ) ) {
			throw new InputErrorException("The value for Start Date must be a valid date.");
		}
	}

	@Override
	public void updateForInput( Input<?> in ) {
		super.updateForInput( in );

		if(in == realTimeFactor || in == realTime) {
			updateRealTime();
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
		simTimeScaleInput.reset();
		traceEventsInput.reset();
		verifyEventsInput.reset();
		printInputReport.reset();
		realTimeFactor.reset();
		realTime.reset();
		updateRealTime();
		exitAtStop.reset();

		startDate.reset();
		startTimeInput.reset();

		showModelBuilder.reset();
		showObjectSelector.reset();
		showInputEditor.reset();
		showOutputViewer.reset();
		showPropertyViewer.reset();
		showLogViewer.reset();

		// Create clock
		Clock.setStartDate(2000, 1, 1);

		// Initialize basic model information
		startTime = 0.0;
		endTime = 8760.0;

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
				InputAgent.doError(e);
				ExceptionBox.instance().setInputError(Entity.getAll().get(i), e);
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

		evt.setSimTimeScale(simTimeScaleInput.getValue());
		setSimTimeScale(simTimeScaleInput.getValue());
		FrameBox.setSecondsPerTick(3600.0d / simTimeScaleInput.getValue());

		if( startDate.getValue() != null ) {
			Clock.getStartingDateFromString( startDate.getValue() );
		}
		double startTimeHours = startTimeInput.getValue() / 3600.0d;
		startTime = Clock.calcTimeForYear_Month_Day_Hour(1, Clock.getStartingMonth(), Clock.getStartingDay(), startTimeHours);
		endTime = startTime + Simulation.getInitializationHours() + Simulation.getRunDurationHours();

		evt.scheduleProcessExternal(0, Entity.PRIO_DEFAULT, false, new InitModelTarget(), null);
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

	private static class StartUpTarget extends ProcessTarget {
		final Entity ent;

		StartUpTarget(Entity ent) {
			this.ent = ent;
		}

		@Override
		public String getDescription() {
			return ent.getInputName() + ".startUp";
		}

		@Override
		public void process() {
			ent.startUp();
		}
	}

	private static class InitModelTarget extends ProcessTarget {
		InitModelTarget() {}

		@Override
		public String getDescription() {
			return "SimulationInit";
		}

		@Override
		public void process() {
			for (int i = 0; i < Entity.getAll().size(); i++) {
				Entity.getAll().get(i).earlyInit();
			}

			long startTick = calculateDelayLength(Simulation.getStartHours());
			for (int i = Entity.getAll().size() - 1; i >= 0; i--) {
				EventManager.scheduleTicks(startTick, 0, false, new StartUpTarget(Entity.getAll().get(i)), null);
			}

			long endTick = calculateDelayLength(Simulation.getEndHours());
			EventManager.scheduleTicks(endTick, Entity.PRIO_DEFAULT, false, new EndModelTarget(), null);
		}
	}

	private static class EndModelTarget extends ProcessTarget {
		EndModelTarget() {}

		@Override
		public String getDescription() {
			return "SimulationEnd";
		}

		@Override
		public void process() {
			EventManager.current().pause();
			for (int i = 0; i < Entity.getAll().size(); i++) {
				Entity.getAll().get(i).doEnd();
			}

			System.out.println( "Made it to do end at" );
			// close warning/error trace file
			InputAgent.closeLogFile();

			if (Simulation.getExitAtStop() || InputAgent.getBatch())
				GUIFrame.shutdown(0);

			EventManager.current().pause();
		}
	}

	/**
	 * Returns the end time of the run.
	 * @return double - the time the current run will stop
	 */
	public static double getEndHours() {
		return endTime;
	}

	/**
	 * Return the run duration for the run (not including intialization)
	 */
	public static double getRunDurationHours() {
		return runDuration.getValue() / 3600.0d;
	}

	/**
	 * Returns the start time of the run.
	 */
	public static double getStartHours() {
		return startTime;
	}

	/**
	 * Return the initialization duration in hours
	 */
	public static double getInitializationHours() {
		return initializationTime.getValue() / 3600.0d;
	}

	static void updateRealTime() {
		GUIFrame.instance().updateForRealTime(realTime.getValue(), realTimeFactor.getValue());
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
