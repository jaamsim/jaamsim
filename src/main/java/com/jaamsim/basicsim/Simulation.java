/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2002-2011 Ausenco Engineering Canada Inc.
 * Copyright (C) 2016 JaamSim Software Inc.
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
package com.jaamsim.basicsim;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

import javax.swing.JFrame;

import com.jaamsim.Samples.SampleConstant;
import com.jaamsim.Samples.SampleInput;
import com.jaamsim.StringProviders.StringProvListInput;
import com.jaamsim.datatypes.IntegerVector;
import com.jaamsim.events.Conditional;
import com.jaamsim.events.EventManager;
import com.jaamsim.input.BooleanInput;
import com.jaamsim.input.DirInput;
import com.jaamsim.input.EntityListInput;
import com.jaamsim.input.Input;
import com.jaamsim.input.InputAgent;
import com.jaamsim.input.IntegerListInput;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.Output;
import com.jaamsim.input.RunNumberInput;
import com.jaamsim.input.UnitTypeListInput;
import com.jaamsim.input.ValueInput;
import com.jaamsim.math.Vec3d;
import com.jaamsim.ui.AboutBox;
import com.jaamsim.ui.EditBox;
import com.jaamsim.ui.EntityPallet;
import com.jaamsim.ui.FrameBox;
import com.jaamsim.ui.GUIFrame;
import com.jaamsim.ui.LogBox;
import com.jaamsim.ui.ObjectSelector;
import com.jaamsim.ui.OutputBox;
import com.jaamsim.ui.PropertyBox;
import com.jaamsim.units.DimensionlessUnit;
import com.jaamsim.units.DistanceUnit;
import com.jaamsim.units.TimeUnit;
import com.jaamsim.units.Unit;
import com.jaamsim.units.UserSpecifiedUnit;

/**
 * Simulation provides the basic structure for the Entity model lifetime of earlyInit,
 * startUp and doEndAt.  The initial processtargets required to start the model are
 * added to the eventmanager here.  This class also acts as a bridge to the UI by
 * providing controls for the various windows.
 */
public class Simulation extends Entity {

	// Key Inputs tab
	@Keyword(description = "The duration of the simulation run in which all statistics will be recorded.",
	             example = "Simulation RunDuration { 8760 h }")
	private static final ValueInput runDuration;

	@Keyword(description = "The initialization interval for the simulation run. The model will "
	                     + "run for the InitializationDuration interval and then clear the "
	                     + "statistics and execute for the specified RunDuration interval. "
	                     + "The total length of the simulation run will be the sum of the "
	                     + "InitializationDuration and RunDuration inputs.",
	             example = "Simulation InitializationDuration { 720 h }")
	private static final ValueInput initializationTime;

	@Keyword(description = "An optional expression that pauses the run when TRUE is returned.",
	             example = "Simulation PauseCondition { '[Queue1].QueueLength > 20'}")
	private static final SampleInput pauseConditionInput;

	@Keyword(description = "If TRUE, the simulation run will be terminated when the "
	                     + "PauseCondition expression returns TRUE. If multiple runs have been "
	                     + "specified, then the next run will be started. If no more runs have "
	                     + "been specified, the simulation will be paused or terminated "
	                     + "depending on the input to the ExitAtStop keyword.",
	             example = "Simulation ExitAtPauseCondition { TRUE }")
	private static final BooleanInput exitAtPauseCondition;

	@Keyword(description = "If TRUE, the program will be closed on completion of the last "
	                     + "simulation run. Otherwise, the last run will be paused.",
	             example = "Simulation ExitAtStop { TRUE }")
	private static final BooleanInput exitAtStop;

	@Keyword(description = "Global seed that sets the substream for each probability "
	                     + "distribution. Must be an integer >= 0. GlobalSubstreamSeed works "
	                     + "together with each probability distribution's RandomSeed keyword to "
	                     + "determine its random sequence. It allows the user to change all the "
	                     + "random sequences in a model with a single input. To run multiple "
	                     + "replications, set the appropriate inputs under the Multiple Runs tab "
	                     + "and then set the GlobalSubstreamSeed input to the run number or to "
	                     + "one of the run indices.",
	             example = "Simulation GlobalSubstreamSeed { 5 }\n"
	                     + "Simulation GlobalSubstreamSeed { [Simulation].RunNumber }\n"
	                     + "Simulation GlobalSubstreamSeed { [Simulation].RunIndex(3) }")
	private static final SampleInput globalSeedInput;

	@Keyword(description = "If TRUE, a full output report is printed to the file "
	                     + "<configuration file name>.rep at the end of the simulation run.",
	             example = "Simulation PrintReport { TRUE }")
	private static final BooleanInput printReport;

	@Keyword(description = "The directory in which to place the output report. Defaults to the "
	                     + "directory containing the configuration file for the run.",
	             example = "Simulation ReportDirectory { 'c:\reports\' }")
	private static final DirInput reportDirectory;

	@Keyword(description = "The unit types for the selected outputs for the simulation run. "
	                     + "Use DimensionlessUnit for a text output.",
	             example = "Simulation UnitTypeList { DistanceUnit  SpeedUnit }")
	private static final UnitTypeListInput unitTypeList;

	@Keyword(description = "One or more selected outputs to be printed at the end of each "
	                     + "simulation run. Each output is specified by an expression. In script "
	                     + "mode (-s tag), the selected outputs are printed to the command line "
	                     + "(standard out). Otherwise, they are printed to the file "
	                     + "<configuration file name>.dat.",
	             example = "Simulation RunOutputList { { [Entity1].Out1 } { [Entity2].Out2 } }")
	protected static final StringProvListInput runOutputList;

	@Keyword(description = "The length of time represented by one simulation tick.",
	             example = "Simulation TickLength { 1e-6 s }")
	private static final ValueInput tickLengthInput;

	// Multiple Runs tab
	@Keyword(description = "Defines the number of run indices and the maximum value N for each "
	                     + "index. When making multiple runs, each index will be iterated from "
	                     + "1 to N starting with the last index. One run will be executed for "
	                     + "every combination of the run index values. For example, if three run "
	                     + "indices are defined with ranges of 3, 5, and 10, then at total of "
	                     + "3*5*10 = 150 runs will be executed.",
	             example = "Simulation RunIndexDefinitionList { 3 5 10 }")
	private static final IntegerListInput runIndexDefinitionList;

	@Keyword(description = "The first run number to be executed. The value can be entered as "
	                     + "either an integer or as the equivalent combination of run indices. "
	                     + "For example, if there are three run indices with ranges of "
	                     + "3, 5, and 10, then run number 22 can be expressed as 1-3-2 because "
	                     + "22 = (1-1)*5*10 + (3-1)*10 + 2.",
	             example = "Simulation StartingRunNumber { 22 }\n"
	                     + "Simulation StartingRunNumber { 1-3-2 }")
	private static final RunNumberInput startingRunNumber;

	@Keyword(description = "The last run number to be executed. The value can be entered as "
	                     + "either an integer or as the equivalent combination of run indices. "
	                     + "For example, if there are three run indices with ranges of "
	                     + "3, 5, and 10, then run number 78 can be expressed as 2-3-8 because "
	                     + "78 = (2-1)*5*10 + (3-1)*10 + 8.",
	             example = "Simulation EndingRunNumber { 78 }\n"
	                     + "Simulation EndingRunNumber { 2-3-8 }")
	private static final RunNumberInput endingRunNumber;

	// GUI tab
	@Keyword(description = "An optional list of units to be used for displaying model outputs.",
	             example = "Simulation DisplayedUnits { h kt }")
	private static final EntityListInput<? extends Unit> displayedUnits;

	@Keyword(description = "If TRUE, a dragged object will be positioned to the nearest grid "
	                     + "point.",
	             example = "Simulation SnapToGrid { TRUE }")
	private static final BooleanInput snapToGrid;

	@Keyword(description = "The distance between snap grid points.",
	             example = "Simulation SnapGridSpacing { 1 m }")
	private static final ValueInput snapGridSpacing;

	@Keyword(description = "The distance moved by the selected entity when the an arrow key is "
	                     + "pressed.",
	             example = "Simulation IncrementSize { 1 cm }")
	private static final ValueInput incrementSize;

	@Keyword(description = "If TRUE, the simulation is executed a constant multiple of real time. "
	                     + "Otherwise, the run is executed as fast as possible, limited only by "
	                     + "processor speed.",
	             example = "Simulation RealTime { TRUE }")
	private static final BooleanInput realTime;

	@Keyword(description = "The target ratio of elapsed simulation time to elapsed real time.",
	             example = "Simulation RealTimeFactor { 1200 }")
	private static final ValueInput realTimeFactor;

	public static final double DEFAULT_REAL_TIME_FACTOR = 1;
	public static final double MIN_REAL_TIME_FACTOR = 1e-6;
	public static final double MAX_REAL_TIME_FACTOR = 1e6;

	@Keyword(description = "The time at which the simulation will be paused.",
	             example = "Simulation PauseTime { 200 h }")
	private static final ValueInput pauseTime;

	@Keyword(description = "If TRUE, the Model Builder tool is shown on startup.",
	             example = "Simulation ShowModelBuilder { TRUE }")
	private static final BooleanInput showModelBuilder;

	@Keyword(description = "If TRUE, the Object Selector tool is shown on startup.",
	             example = "Simulation ShowObjectSelector { TRUE }")
	private static final BooleanInput showObjectSelector;

	@Keyword(description = "If TRUE, the Input Editor tool is shown on startup.",
	             example = "Simulation ShowInputEditor { TRUE }")
	private static final BooleanInput showInputEditor;

	@Keyword(description = "If TRUE, the Output Viewer tool is shown on startup.",
	             example = "Simulation ShowOutputViewer { TRUE }")
	private static final BooleanInput showOutputViewer;

	@Keyword(description = "If TRUE, the Property Viewer tool is shown on startup.",
	             example = "Simulation ShowPropertyViewer { TRUE }")
	private static final BooleanInput showPropertyViewer;

	@Keyword(description = "If TRUE, the Log Viewer tool is shown on startup.",
	             example = "Simulation ShowLogViewer { TRUE }")
	private static final BooleanInput showLogViewer;

	@Keyword(description = "Time at which the simulation run is started (hh:mm).",
	             example = "Simulation StartTime { 2160 h }")
	private static final ValueInput startTimeInput;

	// Hidden keywords
	@Keyword(description = "If TRUE, then the input report file will be printed after loading "
	                     + "the configuration file.  The input report can always be generated "
	                     + "when needed by selecting \"Print Input Report\" under the File menu.",
	             example = "Simulation PrintInputReport { TRUE }")
	private static final BooleanInput printInputReport;

	@Keyword(description = "This is placeholder description text",
	             example = "This is placeholder example text")
	private static final BooleanInput traceEventsInput;

	@Keyword(description = "This is placeholder description text",
	             example = "This is placeholder example text")
	private static final BooleanInput verifyEventsInput;

	private static double startTime; // simulation time (seconds) for the start of the run (not necessarily zero)
	private static double endTime;   // simulation time (seconds) for the end of the run
	private static int runNumber;    // labels each run when multiple runs are being made
	private static IntegerVector runIndexList;

	private static Simulation myInstance;

	private static String modelName = "JaamSim";

	static {

		// Key Inputs tab
		runDuration = new ValueInput("RunDuration", "Key Inputs", 31536000.0d);
		runDuration.setUnitType(TimeUnit.class);
		runDuration.setValidRange(1e-15d, Double.POSITIVE_INFINITY);

		initializationTime = new ValueInput("InitializationDuration", "Key Inputs", 0.0);
		initializationTime.setUnitType(TimeUnit.class);
		initializationTime.setValidRange(0.0d, Double.POSITIVE_INFINITY);

		pauseConditionInput = new SampleInput("PauseCondition", "Key Inputs", null);
		pauseConditionInput.setUnitType(DimensionlessUnit.class);

		exitAtPauseCondition = new BooleanInput("ExitAtPauseCondition", "Key Inputs", false);

		exitAtStop = new BooleanInput("ExitAtStop", "Key Inputs", false);

		globalSeedInput = new SampleInput("GlobalSubstreamSeed", "Key Inputs", new SampleConstant(0));
		globalSeedInput.setUnitType(DimensionlessUnit.class);
		globalSeedInput.setValidRange(0, Integer.MAX_VALUE);

		printReport = new BooleanInput("PrintReport", "Key Inputs", false);

		reportDirectory = new DirInput("ReportDirectory", "Key Inputs", null);
		reportDirectory.setDefaultText("Configuration File Directory");

		unitTypeList = new UnitTypeListInput("UnitTypeList", "Key Inputs", null);

		runOutputList = new StringProvListInput("RunOutputList", "Key Inputs", null);
		runOutputList.setUnitType(UserSpecifiedUnit.class);

		tickLengthInput = new ValueInput("TickLength", "Key Inputs", 1e-6d);
		tickLengthInput.setUnitType(TimeUnit.class);
		tickLengthInput.setValidRange(1e-9d, 5.0d);

		// Multiple Runs tab
		IntegerVector defRangeList = new IntegerVector();
		defRangeList.add(1);
		runIndexDefinitionList = new IntegerListInput("RunIndexDefinitionList", "Multiple Runs", defRangeList);

		startingRunNumber = new RunNumberInput("StartingRunNumber", "Multiple Runs", 1);

		endingRunNumber = new RunNumberInput("EndingRunNumber", "Multiple Runs", 1);

		// GUI tab
		displayedUnits = new EntityListInput<>(Unit.class, "DisplayedUnits", "GUI", new ArrayList<Unit>());
		displayedUnits.setDefaultText("SI Units");
		displayedUnits.setPromptReqd(false);

		realTime = new BooleanInput("RealTime", "GUI", false);
		realTime.setPromptReqd(false);

		snapToGrid = new BooleanInput("SnapToGrid", "GUI", false);
		snapToGrid.setPromptReqd(false);

		snapGridSpacing = new ValueInput("SnapGridSpacing", "GUI", 0.1d);
		snapGridSpacing.setUnitType(DistanceUnit.class);
		snapGridSpacing.setValidRange(1.0e-6, Double.POSITIVE_INFINITY);
		snapGridSpacing.setPromptReqd(false);

		incrementSize = new ValueInput("IncrementSize", "GUI", 0.1d);
		incrementSize.setUnitType(DistanceUnit.class);
		incrementSize.setValidRange(1.0e-6, Double.POSITIVE_INFINITY);
		incrementSize.setPromptReqd(false);

		realTimeFactor = new ValueInput("RealTimeFactor", "GUI", DEFAULT_REAL_TIME_FACTOR);
		realTimeFactor.setValidRange(MIN_REAL_TIME_FACTOR, MAX_REAL_TIME_FACTOR);
		realTimeFactor.setPromptReqd(false);

		pauseTime = new ValueInput("PauseTime", "GUI", Double.POSITIVE_INFINITY);
		pauseTime.setUnitType(TimeUnit.class);
		pauseTime.setValidRange(0.0d, Double.POSITIVE_INFINITY);
		pauseTime.setPromptReqd(false);

		showModelBuilder = new BooleanInput("ShowModelBuilder", "GUI", false);
		showModelBuilder.setPromptReqd(false);

		showObjectSelector = new BooleanInput("ShowObjectSelector", "GUI", false);
		showObjectSelector.setPromptReqd(false);

		showInputEditor = new BooleanInput("ShowInputEditor", "GUI", false);
		showInputEditor.setPromptReqd(false);

		showOutputViewer = new BooleanInput("ShowOutputViewer", "GUI", false);
		showOutputViewer.setPromptReqd(false);

		showPropertyViewer = new BooleanInput("ShowPropertyViewer", "GUI", false);
		showPropertyViewer.setPromptReqd(false);

		showLogViewer = new BooleanInput("ShowLogViewer", "GUI", false);
		showLogViewer.setPromptReqd(false);

		// Hidden keywords
		startTimeInput = new ValueInput("StartTime", "Key Inputs", 0.0d);
		startTimeInput.setUnitType(TimeUnit.class);
		startTimeInput.setValidRange(0.0d, Double.POSITIVE_INFINITY);

		traceEventsInput = new BooleanInput("TraceEvents", "Key Inputs", false);
		verifyEventsInput = new BooleanInput("VerifyEvents", "Key Inputs", false);

		printInputReport = new BooleanInput("PrintInputReport", "Key Inputs", false);

		// Initialize basic model information
		startTime = 0.0;
		endTime = 8760.0*3600.0;
		runNumber = 1;
		runIndexList = new IntegerVector();
		runIndexList.add(1);
	}

	{
		// Key Inputs tab
		this.addInput(runDuration);
		this.addInput(initializationTime);
		this.addInput(pauseConditionInput);
		this.addInput(exitAtPauseCondition);
		this.addInput(exitAtStop);
		this.addInput(globalSeedInput);
		this.addInput(printReport);
		this.addInput(reportDirectory);
		this.addInput(unitTypeList);
		this.addInput(runOutputList);
		this.addInput(tickLengthInput);

		// Multiple Runs tab
		this.addInput(runIndexDefinitionList);
		this.addInput(startingRunNumber);
		this.addInput(endingRunNumber);

		// GUI tab
		this.addInput(displayedUnits);
		this.addInput(snapToGrid);
		this.addInput(snapGridSpacing);
		this.addInput(incrementSize);
		this.addInput(realTime);
		this.addInput(realTimeFactor);
		this.addInput(pauseTime);
		this.addInput(showModelBuilder);
		this.addInput(showObjectSelector);
		this.addInput(showInputEditor);
		this.addInput(showOutputViewer);
		this.addInput(showPropertyViewer);
		this.addInput(showLogViewer);

		// Hidden keywords
		this.addInput(startTimeInput);
		this.addInput(traceEventsInput);
		this.addInput(verifyEventsInput);
		this.addInput(printInputReport);

		// Hide various keywords
		startTimeInput.setHidden(true);
		traceEventsInput.setHidden(true);
		verifyEventsInput.setHidden(true);
		printInputReport.setHidden(true);

		// Set the entity corresponding to "this" for keywords that can accept an expression
		pauseConditionInput.setEntity(Simulation.getInstance());
		globalSeedInput.setEntity(Simulation.getInstance());
		runOutputList.setEntity(Simulation.getInstance());

		// Set the default unit type for the custom output report
		ArrayList<Class<? extends Unit>> defList = new ArrayList<>();
		defList.add(DimensionlessUnit.class);
		unitTypeList.setDefaultValue(defList);
	}

	public Simulation() {}

	public static Simulation getInstance() {
		if (myInstance == null) {
			for (Entity ent : Entity.getClonesOfIterator(Entity.class)) {
				if (ent instanceof Simulation) {
					myInstance = (Simulation)ent;
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

		if (in == unitTypeList) {
			runOutputList.setUnitTypeList(unitTypeList.getUnitTypeList());
			return;
		}

		if (in == runIndexDefinitionList) {
			Simulation.setRunNumber(runNumber);
			startingRunNumber.setRunIndexRangeList(runIndexDefinitionList.getValue());
			endingRunNumber.setRunIndexRangeList(runIndexDefinitionList.getValue());
			return;
		}

		if (in == startingRunNumber) {
			Simulation.setRunNumber(startingRunNumber.getValue());
			return;
		}

		if (in == displayedUnits) {
			Unit.setPreferredUnitList(displayedUnits.getValue());
			return;
		}

		if (in == showModelBuilder) {
			if (GUIFrame.getInstance() != null)
				setWindowVisible(EntityPallet.getInstance(), showModelBuilder.getValue());
			return;
		}

		if (in == showObjectSelector) {
			if (GUIFrame.getInstance() != null)
				setWindowVisible(ObjectSelector.getInstance(), showObjectSelector.getValue());
			return;
		}

		if (in == showInputEditor) {
			if (GUIFrame.getInstance() != null)
				setWindowVisible(EditBox.getInstance(), showInputEditor.getValue());
			FrameBox.reSelectEntity();
			return;
		}

		if (in == showOutputViewer) {
			if (GUIFrame.getInstance() != null)
				setWindowVisible(OutputBox.getInstance(), showOutputViewer.getValue());
			FrameBox.reSelectEntity();
			return;
		}

		if (in == showPropertyViewer) {
			if (GUIFrame.getInstance() != null)
				setWindowVisible(PropertyBox.getInstance(), showPropertyViewer.getValue());
			FrameBox.reSelectEntity();
			return;
		}

		if (in == showLogViewer) {
			if (GUIFrame.getInstance() != null)
				setWindowVisible(LogBox.getInstance(), showLogViewer.getValue());
			FrameBox.reSelectEntity();
			return;
		}
	}

	@Override
	public void validate() {
		super.validate();

		double maxRunDuration = Long.MAX_VALUE*tickLengthInput.getValue();
		if (runDuration.getValue() > maxRunDuration) {
			throw new ErrorException("RunDuration exceeds the maximum value of %g seconds.\n"
					+ "Received: %g seconds.\n"
					+ "The maximum value can be increased by increasing the TickLength input.\n"
					+ "Present value: %g seconds.",
					maxRunDuration, runDuration.getValue(), tickLengthInput.getValue());
		}
	}

	/**
	 * Clears the Simulation prior to loading a new model
	 */
	public static void clear() {

		// Reset all Simulation inputs to their default values
		for (Input<?> inp : Simulation.getInstance().getEditableInputs()) {
			inp.reset();
		}

		updateRealTime();

		// Initialize basic model information
		startTime = 0.0;
		endTime = 8760.0*3600.0;
		myInstance = null;

		// close warning/error trace file
		InputAgent.closeLogFile();

		// Kill all entities except simulation
		while(Entity.getAll().size() > 0) {
			Entity ent = Entity.getAll().get(Entity.getAll().size()-1);
			ent.kill();
		}

		// Reset the run number and run indices
		Simulation.setRunNumber(1);
	}

	/**
	 *	Initializes and starts the model
	 *		1) Initializes EventManager to accept events.
	 *		2) calls startModel() to allow the model to add its starting events to EventManager
	 *		3) start EventManager processing events
	 */
	public static void start(EventManager evt) {
		// Validate each entity based on inputs only
		for (Entity each : Entity.getClonesOfIterator(Entity.class)) {
			try {
				each.validate();
			}
			catch (Throwable e) {
				InputAgent.logMessage("Validation Error - %s: %s", each.getName(), e.getMessage());
				GUIFrame.showErrorDialog("Input Error",
						"JaamSim has detected the following input error during validation:",
						String.format("%s: %-70s", each.getName(), e.getMessage()),
						"The error must be corrected before the simulation can be started.");

				GUIFrame.updateForSimState(GUIFrame.SIM_STATE_CONFIGURED);
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

		startTime = startTimeInput.getValue();
		endTime = startTime + Simulation.getInitializationTime() + Simulation.getRunDuration();

		Simulation.setRunNumber(startingRunNumber.getValue());
		Simulation.startRun(evt);
	}

	/**
	 * Starts a single simulation run.
	 * @param evt - EventManager for the run.
	 */
	private static void startRun(EventManager evt) {
		evt.scheduleProcessExternal(0, 0, false, new InitModelTarget(), null);
		evt.resume(evt.secondsToNearestTick(Simulation.getPauseTime()));
	}

	/**
	 * Ends a single simulation run and if appropriate restarts the model for the next run.
	 */
	public static void endRun() {

		// Execute the end of run method for each entity
		for (Entity each : Entity.getClonesOfIterator(Entity.class)) {
			each.doEnd();
		}

		// Print the output report
		if (printReport.getValue())
			InputAgent.printReport(EventManager.simSeconds());

		// Print the selected outputs
		if (runOutputList.getValue() != null) {
			InputAgent.printRunOutputs(EventManager.simSeconds());
		}

		// Increment the run number and check for last run
		if (Simulation.isLastRun()) {
			Simulation.end();
			return;
		}

		// Start the next run
		final EventManager currentEvt = EventManager.current();
		Simulation.setRunNumber(runNumber + 1);
		Simulation.stopRun(currentEvt);
		new Thread(new Runnable() {
			@Override
			public void run() {
				Simulation.startRun(currentEvt);
			}
		}).start();
	}

	/**
	 * Ends a set of simulation runs.
	 */
	private static void end() {

		// Close warning/error trace file
		LogBox.logLine("Made it to do end at");
		InputAgent.closeLogFile();

		// Always terminate the run when in batch mode
		if (InputAgent.getBatch() || exitAtStop.getValue())
			GUIFrame.shutdown(0);

		EventManager.current().pause();
	}

	/**
	 * Stops and resets the simulation model to zero simulation time.
	 * @param evt - EventManager for the run.
	 */
	public static void stop(EventManager evt) {

		// Stop the present simulation run
		Simulation.stopRun(evt);

		// Reset the run number and run indices
		Simulation.setRunNumber(startingRunNumber.getValue());

		// Close the output reports
		InputAgent.stop();
	}

	/**
	 * Stops the present simulation run when multiple runs are to be executed.
	 * @param evt - EventManager for the run.
	 */
	private static void stopRun(EventManager evt) {

		// Stop the simulation and clear the event list
		evt.pause();
		evt.clear();

		// Destroy the entities that were generated during the run
		for (int i = 0; i < Entity.getAll().size();) {
			Entity ent = Entity.getAll().get(i);
			if (ent.testFlag(Entity.FLAG_GENERATED))
				ent.kill();
			else
				i++;
		}

		// Re-initialise the model
		for (Entity each : Entity.getClonesOfIterator(Entity.class)) {
			// Try/catch is required because some earlyInit methods use simTime which is only
			// available from a process thread
			try {
				each.earlyInit();
			} catch (Exception e) {}
		}

		// Initialise each entity a second time
		for (Entity each : Entity.getClonesOfIterator(Entity.class)) {
			try {
				each.lateInit();
			} catch (Exception e) {}
		}
	}

	public static int getSubstreamNumber() {
		return (int)globalSeedInput.getValue().getNextSample(0.0);
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

	public static double getTickLength() {
		return tickLengthInput.getValue();
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

	public static StringProvListInput getRunOutputList() {
		return runOutputList;
	}

	public static double getIncrementSize() {
		return incrementSize.getValue();
	}

	public static boolean isSnapToGrid() {
		return snapToGrid.getValue();
	}

	public static double getSnapGridSpacing() {
		return snapGridSpacing.getValue();
	}

	public static boolean getExitAtPauseCondition() {
		return exitAtPauseCondition.getValue();
	}

	public void doPauseCondition() {
		if (pauseConditionInput.getValue() != null)
			EventManager.scheduleUntil(pauseModel, pauseCondition, null);
	}

	private final PauseModelTarget pauseModel = new PauseModelTarget();

	static class PauseConditional extends Conditional {
		@Override
		public boolean evaluate() {
			if (pauseConditionInput.getValue() == null)
				return false;
			double simTime = EventManager.simSeconds();
			return pauseConditionInput.getValue().getNextSample(simTime) != 0.0d;
		}
	}
	private final Conditional pauseCondition = new PauseConditional();

	/**
	 * Returns the nearest point on the snap grid to the given coordinate.
	 * To avoid dithering, the new position must be at least one grid space
	 * from the old position.
	 * @param newPos - new coordinate for the object
	 * @param oldPos - present coordinate for the object
	 * @return newest snap grid point.
	 */
	public static Vec3d getSnapGridPosition(Vec3d newPos, Vec3d oldPos) {
		double spacing = snapGridSpacing.getValue();
		Vec3d ret = new Vec3d(newPos);
		if (Math.abs(newPos.x - oldPos.x) < spacing)
			ret.x = oldPos.x;
		if (Math.abs(newPos.y - oldPos.y) < spacing)
			ret.y = oldPos.y;
		if (Math.abs(newPos.z - oldPos.z) < spacing)
			ret.z = oldPos.z;
		return Simulation.getSnapGridPosition(ret);
	}

	/**
	 * Returns the nearest point on the snap grid to the given coordinate.
	 * @param pos - position to be adjusted
	 * @return nearest snap grid point.
	 */
	public static Vec3d getSnapGridPosition(Vec3d pos) {
		double spacing = snapGridSpacing.getValue();
		Vec3d ret = new Vec3d(pos);
		ret.x = spacing*Math.rint(ret.x/spacing);
		ret.y = spacing*Math.rint(ret.y/spacing);
		ret.z = spacing*Math.rint(ret.z/spacing);
		return ret;
	}

	public static Vec3d getSnapGridPosition(Vec3d newPos, Vec3d oldPos, boolean shift) {
		Vec3d ret = getSnapGridPosition(newPos, oldPos);
		if (shift) {
			ret.x = oldPos.x;
			ret.y = oldPos.y;
		}
		else {
			ret.z = oldPos.z;
		}
		return ret;
	}

	static void updateRealTime() {
		GUIFrame.updateForRealTime(realTime.getValue(), realTimeFactor.getValue());
	}

	static void updatePauseTime() {
		GUIFrame.updateForPauseTime(pauseTime.getValueString());
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

	public static boolean isRealTime() {
		return realTime.getValue();
	}

	public static void setWindowVisible(JFrame f, boolean visible) {
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
		if (GUIFrame.getInstance() == null)
			return;
		setWindowVisible(EntityPallet.getInstance(), false);
		setWindowVisible(ObjectSelector.getInstance(), false);
		setWindowVisible(EditBox.getInstance(), false);
		setWindowVisible(OutputBox.getInstance(), false);
		setWindowVisible(PropertyBox.getInstance(), false);
		setWindowVisible(LogBox.getInstance(), false);
	}

	private static void setRunNumber(int n) {
		runNumber = n;
		runIndexList = Simulation.getRunIndexList(n, runIndexDefinitionList.getValue());
	}

	/**
	 * Returns the run indices that correspond to a given run number.
	 * @param n - run number.
	 * @param rangeList - maximum value for each index.
	 * @return run indices.
	 */
	public static IntegerVector getRunIndexList(int n, IntegerVector rangeList) {
		IntegerVector indexList = new IntegerVector(rangeList.size());
		indexList.fillWithEntriesOf(rangeList.size(), 0);
		int denom = 1;
		for (int i=rangeList.size()-1; i>=0; i--) {
			indexList.set(i, (n-1)/denom % rangeList.get(i) + 1);
			denom *= rangeList.get(i);
		}
		return indexList;
	}

	/**
	 * Returns the run number that corresponds to a given set of run indices.
	 * @param indexList - run indices.
	 * @param rangeList - maximum value for each index.
	 * @return run number.
	 */
	public static int getRunNumber(IntegerVector indexList, IntegerVector rangeList) {
		int n = 1;
		int factor = 1;
		for (int i=indexList.size()-1; i>=0; i--) {
			n += (indexList.get(i)-1)*factor;
			factor *= rangeList.get(i);
		}
		return n;
	}

	/**
	 * Returns the input format used to specify a set of run indices.
	 * @param indexList - run indices.
	 * @return run code.
	 */
	public static String getRunCode(IntegerVector indexList) {
		StringBuilder sb = new StringBuilder();
		sb.append(indexList.get(0));
		for (int i=1; i<indexList.size(); i++) {
			sb.append("-").append(indexList.get(i));
		}
		return sb.toString();
	}

	public static String getRunCode() {
		return Simulation.getRunCode(runIndexList);
	}

	public static String getRunHeader() {
		return String.format("##### RUN %s #####", Simulation.getRunCode());
	}

	public static boolean isMultipleRuns() {
		return endingRunNumber.getValue() > startingRunNumber.getValue();
	}

	public static boolean isFirstRun() {
		return runNumber == startingRunNumber.getValue();
	}

	public static boolean isLastRun() {
		return runNumber >= endingRunNumber.getValue();
	}

	@Output(name = "Software Name",
	 description = "The licensed name for the simulation software.",
	  reportable = true,
	    sequence = 0)
	public String getSoftwareName(double simTime) {
		return modelName;
	}

	@Output(name = "Software Version",
	 description = "The release number for the simulation software.",
	  reportable = true,
	    sequence = 1)
	public String getSoftwareVersion(double simTime) {
		return AboutBox.version;
	}

	@Output(name = "Configuration File",
	 description = "The configuration file that has been loaded.",
	  reportable = true,
	    sequence = 2)
	public String getConfigFileName(double simTime) {
		if (InputAgent.getConfigFile() != null)
			return InputAgent.getConfigFile().getPath();

		return "";
	}

	@Output(name = "RunNumber",
	 description = "The counter used to indentify an individual simulation run when multiple runs "
	             + "are being made.",
	    unitType = DimensionlessUnit.class,
	  reportable = true,
	    sequence = 3)
	public int getRunNumber(double simTime) {
		return runNumber;
	}

	@Output(name = "RunIndex",
	 description = "The list of run indices that correspond to the run number.",
	    unitType = DimensionlessUnit.class,
	  reportable = true,
	    sequence = 4)
	public IntegerVector getRunIndex(double simTime) {
		return runIndexList;
	}

	@Output(name = "Present Time and Date",
	 description = "The present local time and date.",
	  reportable = true,
	    sequence = 5)
	public String getPresentTime(double simTime) {
		String timeStamp = new SimpleDateFormat("MMM dd, yyyy HH:mm").format(Calendar.getInstance().getTime());
		return timeStamp;
	}

	@Output(name = "Initialization Duration",
	 description = "The length of time the model was executed prior to the start of statistics "
	             + "collection.",
	    unitType = TimeUnit.class,
	  reportable = true,
	    sequence = 6)
	public double getInitializationDuration(double simTime) {
		return initializationTime.getValue();
	}

	@Output(name = "Run Duration",
	 description = "The length of time over which statistics were collected.",
	    unitType = TimeUnit.class,
	  reportable = true,
	    sequence = 7)
	public double getRunDuration(double simTime) {
		return runDuration.getValue();
	}

	@Output(name = "Present Simulation Time",
	 description = "The value for the simulation clock at the present time.",
	    unitType = TimeUnit.class,
	  reportable = true,
	    sequence = 8)
	public double getPresentSimulationTime(double simTime) {
		return simTime;
	}

}
