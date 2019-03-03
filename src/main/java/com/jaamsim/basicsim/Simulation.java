/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2002-2011 Ausenco Engineering Canada Inc.
 * Copyright (C) 2016-2019 JaamSim Software Inc.
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

import com.jaamsim.ProbabilityDistributions.RandomStreamUser;
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
import com.jaamsim.input.IntegerInput;
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
import com.jaamsim.ui.EventViewer;
import com.jaamsim.ui.GUIFrame;
import com.jaamsim.ui.LogBox;
import com.jaamsim.ui.ObjectSelector;
import com.jaamsim.ui.OutputBox;
import com.jaamsim.ui.PropertyBox;
import com.jaamsim.units.DimensionlessUnit;
import com.jaamsim.units.DistanceUnit;
import com.jaamsim.units.TimeUnit;
import com.jaamsim.units.Unit;

/**
 * Simulation provides the basic structure for the Entity model lifetime of earlyInit,
 * startUp and doEndAt.  The initial processtargets required to start the model are
 * added to the eventmanager here.  This class also acts as a bridge to the UI by
 * providing controls for the various windows.
 */
public class Simulation extends Entity {

	// Key Inputs tab
	@Keyword(description = "The duration of the simulation run in which all statistics will be recorded.",
	         exampleList = {"8760 h"})
	private static final ValueInput runDuration;

	@Keyword(description = "The initialization interval for the simulation run. The model will "
	                     + "run for the InitializationDuration interval and then clear the "
	                     + "statistics and execute for the specified RunDuration interval. "
	                     + "The total length of the simulation run will be the sum of the "
	                     + "InitializationDuration and RunDuration inputs.",
	         exampleList = {"720 h"})
	private static final ValueInput initializationTime;

	@Keyword(description = "An optional expression that pauses the run when TRUE is returned.",
	         exampleList = {"'[Queue1].QueueLength > 20'"})
	private static final SampleInput pauseConditionInput;

	@Keyword(description = "If TRUE, the simulation run will be terminated when the "
	                     + "PauseCondition expression returns TRUE. If multiple runs have been "
	                     + "specified, then the next run will be started. If no more runs have "
	                     + "been specified, the simulation will be paused or terminated "
	                     + "depending on the input to the ExitAtStop keyword.",
	         exampleList = {"TRUE"})
	private static final BooleanInput exitAtPauseCondition;

	@Keyword(description = "If TRUE, the program will be closed on completion of the last "
	                     + "simulation run. Otherwise, the last run will be paused.",
	         exampleList = {"TRUE"})
	private static final BooleanInput exitAtStop;

	@Keyword(description = "Global seed that sets the substream for each probability "
	                     + "distribution. Must be an integer >= 0. GlobalSubstreamSeed works "
	                     + "together with each probability distribution's RandomSeed keyword to "
	                     + "determine its random sequence. It allows the user to change all the "
	                     + "random sequences in a model with a single input. To run multiple "
	                     + "replications, set the appropriate inputs under the Multiple Runs tab "
	                     + "and then set the GlobalSubstreamSeed input to the run number or to "
	                     + "one of the run indices.",
	         exampleList = {"5", "[Simulation].RunNumber", "[Simulation].RunIndex(3)"})
	private static final SampleInput globalSeedInput;

	@Keyword(description = "If TRUE, a full output report is printed to the file "
	                     + "<configuration file name>.rep at the end of the simulation run.",
	         exampleList = {"TRUE"})
	private static final BooleanInput printReport;

	@Keyword(description = "The directory in which to place the output report. Defaults to the "
	                     + "directory containing the configuration file for the run.",
	         exampleList = {"'c:/reports/'"})
	private static final DirInput reportDirectory;

	@Keyword(description = "The unit types for the selected outputs for the simulation run. "
	                     + "Use DimensionlessUnit for non-numeric outputs such as strings, "
	                     + "entities, and arrays. "
	                     + "If the RunOutputList keyword has more entries than the UnitTypesList "
	                     + "keyword, then the last unit type in the list is applied to the "
	                     + "remaining RunOutputList entries.\n\n"
	                     + "It is best to leave this input blank and use only dimensionless "
	                     + "quantities and non-numeric outputs in the RunOutputList.",
	         exampleList = {"DistanceUnit  SpeedUnit"})
	private static final UnitTypeListInput unitTypeList;

	@Keyword(description = "One or more selected outputs to be printed at the end of each "
	                     + "simulation run. Each output is specified by an expression. In script "
	                     + "mode (-s tag), the selected outputs are printed to the command line "
	                     + "(standard out). Otherwise, they are printed to the file "
	                     + "<configuration file name>.dat.\n\n"
	                     + "It is best to include only dimensionless quantities and non-numeric "
	                     + "outputs in the RunOutputList. "
	                     + "An output with dimensions can be made non-dimensional by dividing it "
	                     + "by 1 in the desired unit, e.g. '[Queue1].AverageQueueTime / 1[h]' is "
	                     + "the average queue time in hours.\n\n"
	                     + "If a number with dimensions is to be recorded, its unit type must "
	                     + "first be entered in the correct position in the input to the "
	                     + "UnitTypeList keyword.",
	         exampleList = {"{ [Simulation].RunNumber } { '[Queue1].AverageQueueTime / 1[h]' }"})
	protected static final StringProvListInput runOutputList;

	@Keyword(description = "The length of time represented by one simulation tick.",
	         exampleList = {"1e-6 s"})
	private static final ValueInput tickLengthInput;

	// Multiple Runs tab
	@Keyword(description = "Defines the number of run indices and the maximum value N for each "
	                     + "index. When making multiple runs, each index will be iterated from "
	                     + "1 to N starting with the last index. One run will be executed for "
	                     + "every combination of the run index values. For example, if three run "
	                     + "indices are defined with ranges of 3, 5, and 10, then at total of "
	                     + "3*5*10 = 150 runs will be executed.",
	         exampleList = {"3 5 10"})
	private static final IntegerListInput runIndexDefinitionList;

	@Keyword(description = "The first run number to be executed. The value can be entered as "
	                     + "either an integer or as the equivalent combination of run indices. "
	                     + "For example, if there are three run indices with ranges of "
	                     + "3, 5, and 10, then run number 22 can be expressed as 1-3-2 because "
	                     + "22 = (1-1)*5*10 + (3-1)*10 + 2.",
	         exampleList = {"22", "1-3-2"})
	private static final RunNumberInput startingRunNumber;

	@Keyword(description = "The last run number to be executed. The value can be entered as "
	                     + "either an integer or as the equivalent combination of run indices. "
	                     + "For example, if there are three run indices with ranges of "
	                     + "3, 5, and 10, then run number 78 can be expressed as 2-3-8 because "
	                     + "78 = (2-1)*5*10 + (3-1)*10 + 8.",
	         exampleList = {"78", "2-3-8"})
	private static final RunNumberInput endingRunNumber;

	// GUI tab
	@Keyword(description = "An optional list of units to be used for displaying model outputs.",
	         exampleList = {"h kt"})
	private static final EntityListInput<? extends Unit> displayedUnits;

	@Keyword(description = "If TRUE, a dragged object will be positioned to the nearest grid "
	                     + "point.",
	         exampleList = {"TRUE"})
	private static final BooleanInput snapToGrid;

	@Keyword(description = "The distance between snap grid points.",
	         exampleList = {"1 m"})
	private static final ValueInput snapGridSpacing;

	@Keyword(description = "The distance moved by the selected entity when the an arrow key is "
	                     + "pressed. Defaults to the SnapGridSpacing value.",
	         exampleList = {"1 cm"})
	private static final ValueInput incrementSize;

	@Keyword(description = "If TRUE, the simulation is executed a constant multiple of real time. "
	                     + "Otherwise, the run is executed as fast as possible, limited only by "
	                     + "processor speed.",
	         exampleList = {"TRUE"})
	private static final BooleanInput realTime;

	@Keyword(description = "The target ratio of elapsed simulation time to elapsed real time.",
	         exampleList = {"1200"})
	private static final ValueInput realTimeFactor;

	public static final double DEFAULT_REAL_TIME_FACTOR = 1;
	public static final double MIN_REAL_TIME_FACTOR = 1e-6;
	public static final double MAX_REAL_TIME_FACTOR = 1e6;

	@Keyword(description = "The time at which the simulation will be paused.",
	         exampleList = {"200 h"})
	private static final ValueInput pauseTime;

	@Keyword(description = "If TRUE, the Model Builder tool is shown on startup.",
	         exampleList = {"TRUE"})
	private static final BooleanInput showModelBuilder;

	@Keyword(description = "If TRUE, the Object Selector tool is shown on startup.",
	         exampleList = {"TRUE"})
	private static final BooleanInput showObjectSelector;

	@Keyword(description = "If TRUE, the Input Editor tool is shown on startup.",
	         exampleList = {"TRUE"})
	private static final BooleanInput showInputEditor;

	@Keyword(description = "If TRUE, the Output Viewer tool is shown on startup.",
	         exampleList = {"TRUE"})
	private static final BooleanInput showOutputViewer;

	@Keyword(description = "If TRUE, the Property Viewer tool is shown on startup.",
	         exampleList = {"TRUE"})
	private static final BooleanInput showPropertyViewer;

	@Keyword(description = "If TRUE, the Log Viewer tool is shown on startup.",
	         exampleList = {"TRUE"})
	private static final BooleanInput showLogViewer;

	@Keyword(description = "If TRUE, the Event Viewer tool is shown on startup.",
	         exampleList = {"TRUE"})
	private static final BooleanInput showEventViewer;

	@Keyword(description = "The position of the upper left corner of the Model Builder window "
	                     + "in pixels measured from the top left corner of the screen.",
	         exampleList = {"220 110"})
	private static final IntegerListInput modelBuilderPos;

	@Keyword(description = "The size of the Model Builder window in pixels (width, height).",
	         exampleList = {"500 300"})
	private static final IntegerListInput modelBuilderSize;

	@Keyword(description = "The position of the upper left corner of the Object Selector window "
	                     + "in pixels measured from the top left corner of the screen.",
	         exampleList = {"220 110"})
	private static final IntegerListInput objectSelectorPos;

	@Keyword(description = "The size of the Object Selector window in pixels (width, height).",
	         exampleList = {"500 300"})
	private static final IntegerListInput objectSelectorSize;

	@Keyword(description = "The position of the upper left corner of the Input Editor window "
	                     + "in pixels measured from the top left corner of the screen.",
	         exampleList = {"220 110"})
	private static final IntegerListInput inputEditorPos;

	@Keyword(description = "The size of the Input Editor window in pixels (width, height).",
	         exampleList = {"500 300"})
	private static final IntegerListInput inputEditorSize;

	@Keyword(description = "The position of the upper left corner of the Output Viewer window "
	                     + "in pixels measured from the top left corner of the screen.",
	         exampleList = {"220 110"})
	private static final IntegerListInput outputViewerPos;

	@Keyword(description = "The size of the Output Viewer window in pixels (width, height).",
	         exampleList = {"500 300"})
	private static final IntegerListInput outputViewerSize;

	@Keyword(description = "The position of the upper left corner of the Property Viewer window "
	                     + "in pixels measured from the top left corner of the screen.",
	         exampleList = {"220 110"})
	private static final IntegerListInput propertyViewerPos;

	@Keyword(description = "The size of the Property Viewer window in pixels (width, height).",
	         exampleList = {"500 300"})
	private static final IntegerListInput propertyViewerSize;

	@Keyword(description = "The position of the upper left corner of the Log Viewer window "
	                     + "in pixels measured from the top left corner of the screen.",
	         exampleList = {"220 110"})
	private static final IntegerListInput logViewerPos;

	@Keyword(description = "The size of the Log Viewer window in pixels (width, height).",
	         exampleList = {"500 300"})
	private static final IntegerListInput logViewerSize;

	@Keyword(description = "The position of the upper left corner of the Event Viewer window "
	                     + "in pixels measured from the top left corner of the screen.",
	         exampleList = {"220 110"})
	private static final IntegerListInput eventViewerPos;

	@Keyword(description = "The size of the Event Viewer window in pixels (width, height).",
	         exampleList = {"500 300"})
	private static final IntegerListInput eventViewerSize;

	@Keyword(description = "The width of the Control Panel window in pixels.",
	         exampleList = {"1920"})
	private static final IntegerInput controlPanelWidth;

	@Keyword(description = "Time at which the simulation run is started (hh:mm).",
	         exampleList = {"2160 h"})
	private static final ValueInput startTimeInput;

	// Hidden keywords
	@Keyword(description = "If TRUE, then the input report file will be printed after loading "
	                     + "the configuration file.  The input report can always be generated "
	                     + "when needed by selecting \"Print Input Report\" under the File menu.",
	         exampleList = {"TRUE"})
	private static final BooleanInput printInputReport;

	@Keyword(description = "This is placeholder description text",
	         exampleList = {"TRUE"})
	private static final BooleanInput traceEventsInput;

	@Keyword(description = "This is placeholder description text",
	         exampleList = {"TRUE"})
	private static final BooleanInput verifyEventsInput;

	private static double startTime; // simulation time (seconds) for the start of the run (not necessarily zero)
	private static double endTime;   // simulation time (seconds) for the end of the run
	private static int runNumber;    // labels each run when multiple runs are being made
	private static IntegerVector runIndexList;

	private static Simulation myInstance;

	private static String modelName = "JaamSim";

	static {

		// Key Inputs tab
		runDuration = new ValueInput("RunDuration", KEY_INPUTS, 31536000.0d);
		runDuration.setUnitType(TimeUnit.class);
		runDuration.setValidRange(1e-15d, Double.POSITIVE_INFINITY);

		initializationTime = new ValueInput("InitializationDuration", KEY_INPUTS, 0.0);
		initializationTime.setUnitType(TimeUnit.class);
		initializationTime.setValidRange(0.0d, Double.POSITIVE_INFINITY);

		pauseConditionInput = new SampleInput("PauseCondition", KEY_INPUTS, null);
		pauseConditionInput.setUnitType(DimensionlessUnit.class);

		exitAtPauseCondition = new BooleanInput("ExitAtPauseCondition", KEY_INPUTS, false);

		exitAtStop = new BooleanInput("ExitAtStop", KEY_INPUTS, false);

		globalSeedInput = new SampleInput("GlobalSubstreamSeed", KEY_INPUTS, new SampleConstant(0));
		globalSeedInput.setUnitType(DimensionlessUnit.class);
		globalSeedInput.setValidRange(0, Integer.MAX_VALUE);

		printReport = new BooleanInput("PrintReport", KEY_INPUTS, false);

		reportDirectory = new DirInput("ReportDirectory", KEY_INPUTS, null);
		reportDirectory.setDefaultText("Configuration File Directory");

		unitTypeList = new UnitTypeListInput("UnitTypeList", KEY_INPUTS, null);

		runOutputList = new StringProvListInput("RunOutputList", KEY_INPUTS, null);
		runOutputList.setUnitType(DimensionlessUnit.class);

		tickLengthInput = new ValueInput("TickLength", KEY_INPUTS, 1e-6d);
		tickLengthInput.setUnitType(TimeUnit.class);
		tickLengthInput.setValidRange(1e-12d, Double.POSITIVE_INFINITY);

		// Multiple Runs tab
		IntegerVector defRangeList = new IntegerVector();
		defRangeList.add(1);
		runIndexDefinitionList = new IntegerListInput("RunIndexDefinitionList", MULTIPLE_RUNS, defRangeList);

		startingRunNumber = new RunNumberInput("StartingRunNumber", MULTIPLE_RUNS, 1);

		endingRunNumber = new RunNumberInput("EndingRunNumber", MULTIPLE_RUNS, 1);

		// GUI tab
		displayedUnits = new EntityListInput<>(Unit.class, "DisplayedUnits", GUI, new ArrayList<Unit>());
		displayedUnits.setDefaultText("SI Units");
		displayedUnits.setPromptReqd(false);
		displayedUnits.setHidden(true);

		realTime = new BooleanInput("RealTime", GUI, false);
		realTime.setPromptReqd(false);
		realTime.setHidden(true);

		snapToGrid = new BooleanInput("SnapToGrid", GUI, false);
		snapToGrid.setPromptReqd(false);
		snapToGrid.setHidden(true);

		snapGridSpacing = new ValueInput("SnapGridSpacing", GUI, 0.1d);
		snapGridSpacing.setUnitType(DistanceUnit.class);
		snapGridSpacing.setValidRange(1.0e-6, Double.POSITIVE_INFINITY);
		snapGridSpacing.setPromptReqd(false);
		snapGridSpacing.setHidden(true);

		incrementSize = new ValueInput("IncrementSize", GUI, 0.1d);
		incrementSize.setUnitType(DistanceUnit.class);
		incrementSize.setValidRange(1.0e-6, Double.POSITIVE_INFINITY);
		incrementSize.setPromptReqd(false);
		incrementSize.setHidden(true);

		realTimeFactor = new ValueInput("RealTimeFactor", GUI, DEFAULT_REAL_TIME_FACTOR);
		realTimeFactor.setValidRange(MIN_REAL_TIME_FACTOR, MAX_REAL_TIME_FACTOR);
		realTimeFactor.setPromptReqd(false);
		realTimeFactor.setHidden(true);

		pauseTime = new ValueInput("PauseTime", GUI, Double.POSITIVE_INFINITY);
		pauseTime.setUnitType(TimeUnit.class);
		pauseTime.setValidRange(0.0d, Double.POSITIVE_INFINITY);
		pauseTime.setPromptReqd(false);
		pauseTime.setHidden(true);

		showModelBuilder = new BooleanInput("ShowModelBuilder", GUI, false);
		showModelBuilder.setPromptReqd(false);
		showModelBuilder.setHidden(true);

		showObjectSelector = new BooleanInput("ShowObjectSelector", GUI, false);
		showObjectSelector.setPromptReqd(false);
		showObjectSelector.setHidden(true);

		showInputEditor = new BooleanInput("ShowInputEditor", GUI, false);
		showInputEditor.setPromptReqd(false);
		showInputEditor.setHidden(true);

		showOutputViewer = new BooleanInput("ShowOutputViewer", GUI, false);
		showOutputViewer.setPromptReqd(false);
		showOutputViewer.setHidden(true);

		showPropertyViewer = new BooleanInput("ShowPropertyViewer", GUI, false);
		showPropertyViewer.setPromptReqd(false);
		showPropertyViewer.setHidden(true);

		showLogViewer = new BooleanInput("ShowLogViewer", GUI, false);
		showLogViewer.setPromptReqd(false);
		showLogViewer.setHidden(true);

		showEventViewer = new BooleanInput("ShowEventViewer", GUI, false);
		showEventViewer.setPromptReqd(false);
		showEventViewer.setHidden(true);

		final IntegerVector def = new IntegerVector(2);
		def.add(1);
		def.add(1);

		modelBuilderPos = new IntegerListInput("ModelBuilderPos", GUI, def);
		modelBuilderPos.setValidCount(2);
		modelBuilderPos.setValidRange(-8192, 8192);
		modelBuilderPos.setPromptReqd(false);
		modelBuilderPos.setHidden(true);

		modelBuilderSize = new IntegerListInput("ModelBuilderSize", GUI, def);
		modelBuilderSize.setValidCount(2);
		modelBuilderSize.setValidRange(1, 8192);
		modelBuilderSize.setPromptReqd(false);
		modelBuilderSize.setHidden(true);

		objectSelectorPos = new IntegerListInput("ObjectSelectorPos", GUI, def);
		objectSelectorPos.setValidCount(2);
		objectSelectorPos.setValidRange(-8192, 8192);
		objectSelectorPos.setPromptReqd(false);
		objectSelectorPos.setHidden(true);

		objectSelectorSize = new IntegerListInput("ObjectSelectorSize", GUI, def);
		objectSelectorSize.setValidCount(2);
		objectSelectorSize.setValidRange(1, 8192);
		objectSelectorSize.setPromptReqd(false);
		objectSelectorSize.setHidden(true);

		inputEditorPos = new IntegerListInput("InputEditorPos", GUI, def);
		inputEditorPos.setValidCount(2);
		inputEditorPos.setValidRange(-8192, 8192);
		inputEditorPos.setPromptReqd(false);
		inputEditorPos.setHidden(true);

		inputEditorSize = new IntegerListInput("InputEditorSize", GUI, def);
		inputEditorSize.setValidCount(2);
		inputEditorSize.setValidRange(1, 8192);
		inputEditorSize.setPromptReqd(false);
		inputEditorSize.setHidden(true);

		outputViewerPos = new IntegerListInput("OutputViewerPos", GUI, def);
		outputViewerPos.setValidCount(2);
		outputViewerPos.setValidRange(-8192, 8192);
		outputViewerPos.setPromptReqd(false);
		outputViewerPos.setHidden(true);

		outputViewerSize = new IntegerListInput("OutputViewerSize", GUI, def);
		outputViewerSize.setValidCount(2);
		outputViewerSize.setValidRange(1, 8192);
		outputViewerSize.setPromptReqd(false);
		outputViewerSize.setHidden(true);

		propertyViewerPos = new IntegerListInput("PropertyViewerPos", GUI, def);
		propertyViewerPos.setValidCount(2);
		propertyViewerPos.setValidRange(-8192, 8192);
		propertyViewerPos.setPromptReqd(false);
		propertyViewerPos.setHidden(true);

		propertyViewerSize = new IntegerListInput("PropertyViewerSize", GUI, def);
		propertyViewerSize.setValidCount(2);
		propertyViewerSize.setValidRange(1, 8192);
		propertyViewerSize.setPromptReqd(false);
		propertyViewerSize.setHidden(true);

		logViewerPos = new IntegerListInput("LogViewerPos", GUI, def);
		logViewerPos.setValidCount(2);
		logViewerPos.setValidRange(-8192, 8192);
		logViewerPos.setPromptReqd(false);
		logViewerPos.setHidden(true);

		logViewerSize = new IntegerListInput("LogViewerSize", GUI, def);
		logViewerSize.setValidCount(2);
		logViewerSize.setValidRange(1, 8192);
		logViewerSize.setPromptReqd(false);
		logViewerSize.setHidden(true);

		eventViewerPos = new IntegerListInput("EventViewerPos", GUI, def);
		eventViewerPos.setValidCount(2);
		eventViewerPos.setValidRange(-8192, 8192);
		eventViewerPos.setPromptReqd(false);
		eventViewerPos.setHidden(true);

		eventViewerSize = new IntegerListInput("EventViewerSize", GUI, def);
		eventViewerSize.setValidCount(2);
		eventViewerSize.setValidRange(1, 8192);
		eventViewerSize.setPromptReqd(false);
		eventViewerSize.setHidden(true);

		controlPanelWidth = new IntegerInput("ControlPanelWidth", GUI, Integer.valueOf(1));
		controlPanelWidth.setValidRange(1, 8192);
		controlPanelWidth.setPromptReqd(false);
		controlPanelWidth.setHidden(true);

		// Hidden keywords
		startTimeInput = new ValueInput("StartTime", KEY_INPUTS, 0.0d);
		startTimeInput.setUnitType(TimeUnit.class);
		startTimeInput.setValidRange(0.0d, Double.POSITIVE_INFINITY);

		traceEventsInput = new BooleanInput("TraceEvents", KEY_INPUTS, false);
		verifyEventsInput = new BooleanInput("VerifyEvents", KEY_INPUTS, false);

		printInputReport = new BooleanInput("PrintInputReport", KEY_INPUTS, false);

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
		this.addInput(showEventViewer);
		this.addInput(modelBuilderPos);
		this.addInput(modelBuilderSize);
		this.addInput(objectSelectorPos);
		this.addInput(objectSelectorSize);
		this.addInput(inputEditorPos);
		this.addInput(inputEditorSize);
		this.addInput(outputViewerPos);
		this.addInput(outputViewerSize);
		this.addInput(propertyViewerPos);
		this.addInput(propertyViewerSize);
		this.addInput(logViewerPos);
		this.addInput(logViewerSize);
		this.addInput(eventViewerPos);
		this.addInput(eventViewerSize);
		this.addInput(controlPanelWidth);

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

		// Set the initial value for snap grid spacing
		if (GUIFrame.getInstance() != null)
			GUIFrame.getInstance().updateForSnapGridSpacing(snapGridSpacing.getDefaultString());
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
			if (GUIFrame.getInstance() != null)
				GUIFrame.getInstance().updateForPauseTime(pauseTime.getValueString());
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

		if (in == snapGridSpacing) {
			if (GUIFrame.getInstance() != null)
				GUIFrame.getInstance().updateForSnapGridSpacing(snapGridSpacing.getValueString());
			return;
		}

		if (in == snapToGrid) {
			if (GUIFrame.getInstance() != null)
				GUIFrame.getInstance().updateForSnapToGrid();
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
			return;
		}

		if (in == showOutputViewer) {
			if (GUIFrame.getInstance() != null)
				setWindowVisible(OutputBox.getInstance(), showOutputViewer.getValue());
			return;
		}

		if (in == showPropertyViewer) {
			if (GUIFrame.getInstance() != null)
				setWindowVisible(PropertyBox.getInstance(), showPropertyViewer.getValue());
			return;
		}

		if (in == showLogViewer) {
			if (GUIFrame.getInstance() != null)
				setWindowVisible(LogBox.getInstance(), showLogViewer.getValue());
			return;
		}

		if (in == showEventViewer) {
			if (GUIFrame.getInstance() != null) {
				if (showEventViewer.getValue())
					setWindowVisible(EventViewer.getInstance(), true);
				else if (EventViewer.hasInstance())
					EventViewer.getInstance().dispose();
			}
			return;
		}

		if (in == modelBuilderPos) {
			IntegerVector pos = modelBuilderPos.getValue();
			EntityPallet.getInstance().setLocation(pos.get(0), pos.get(1));
			return;
		}

		if (in == modelBuilderSize) {
			IntegerVector size = modelBuilderSize.getValue();
			EntityPallet.getInstance().setSize(size.get(0), size.get(1));
			return;
		}

		if (in == objectSelectorPos) {
			IntegerVector pos = objectSelectorPos.getValue();
			ObjectSelector.getInstance().setLocation(pos.get(0), pos.get(1));
			return;
		}

		if (in == objectSelectorSize) {
			IntegerVector size = objectSelectorSize.getValue();
			ObjectSelector.getInstance().setSize(size.get(0), size.get(1));
			return;
		}

		if (in == inputEditorPos) {
			IntegerVector pos = inputEditorPos.getValue();
			EditBox.getInstance().setLocation(pos.get(0), pos.get(1));
			return;
		}

		if (in == inputEditorSize) {
			IntegerVector size = inputEditorSize.getValue();
			EditBox.getInstance().setSize(size.get(0), size.get(1));
			return;
		}

		if (in == outputViewerPos) {
			IntegerVector pos = outputViewerPos.getValue();
			OutputBox.getInstance().setLocation(pos.get(0), pos.get(1));
			return;
		}

		if (in == outputViewerSize) {
			IntegerVector size = outputViewerSize.getValue();
			OutputBox.getInstance().setSize(size.get(0), size.get(1));
			return;
		}

		if (in == propertyViewerPos) {
			IntegerVector pos = propertyViewerPos.getValue();
			PropertyBox.getInstance().setLocation(pos.get(0), pos.get(1));
			return;
		}

		if (in == propertyViewerSize) {
			IntegerVector size = propertyViewerSize.getValue();
			PropertyBox.getInstance().setSize(size.get(0), size.get(1));
			return;
		}

		if (in == logViewerPos) {
			IntegerVector pos = logViewerPos.getValue();
			LogBox.getInstance().setLocation(pos.get(0), pos.get(1));
			return;
		}

		if (in == logViewerSize) {
			IntegerVector size = logViewerSize.getValue();
			LogBox.getInstance().setSize(size.get(0), size.get(1));
			return;
		}

		if (in == eventViewerPos) {
			if (EventViewer.hasInstance()) {
				IntegerVector pos = eventViewerPos.getValue();
				EventViewer.getInstance().setLocation(pos.get(0), pos.get(1));
			}
			return;
		}

		if (in == eventViewerSize) {
			if (EventViewer.hasInstance()) {
				IntegerVector size = eventViewerSize.getValue();
				EventViewer.getInstance().setSize(size.get(0), size.get(1));
			}
			return;
		}

		if (in == controlPanelWidth) {
			if (GUIFrame.getInstance() != null) {
				int width = controlPanelWidth.getValue();
				int height = GUIFrame.getInstance().getSize().height;
				GUIFrame.getInstance().setSize(width, height);
			}
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
			InputAgent.applyArgs(getInstance(), inp.getKeyword());
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
	public void start(EventManager evt) {

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
		else if (Simulation.showEventViewer.getValue()) {
			evt.setTraceListener(EventViewer.getInstance());
		}

		evt.setTickLength(tickLengthInput.getValue());

		startTime = startTimeInput.getValue();
		endTime = startTime + Simulation.getInitializationTime() + Simulation.getRunDuration();

		Simulation.setRunNumber(startingRunNumber.getValue());
		startRun(evt);
	}

	/**
	 * Starts a single simulation run.
	 * @param evt - EventManager for the run.
	 */
	private void startRun(EventManager evt) {
		if (GUIFrame.getInstance() != null)
			GUIFrame.getInstance().initSpeedUp(0.0d);
		evt.scheduleProcessExternal(0, 0, false, new InitModelTarget(getInstance()), null);
		evt.resume(evt.secondsToNearestTick(Simulation.getPauseTime()));
	}

	/**
	 * Ends a single simulation run and if appropriate restarts the model for the next run.
	 */
	public void endRun() {

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
			end();
			return;
		}

		// Start the next run
		final EventManager currentEvt = EventManager.current();
		Simulation.setRunNumber(runNumber + 1);
		GUIFrame.getJaamSimModel().reset();
		new Thread(new Runnable() {
			@Override
			public void run() {
				startRun(currentEvt);
			}
		}).start();
	}

	/**
	 * Ends a set of simulation runs.
	 */
	private void end() {

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
		GUIFrame.getJaamSimModel().reset();

		// Reset the run number and run indices
		Simulation.setRunNumber(startingRunNumber.getValue());

		// Close the output reports
		InputAgent.stop();
	}

	/**
	 * Returns whether a paused simulation can be resumed.
	 * @param simTicks - present simulation time in clock ticks
	 * @return true if the simulation can be resumed
	 */
	public static boolean canResume(long simTicks) {
		double totalDur = getRunDuration() + getInitializationTime();
		return simTicks < EventManager.secsToNearestTick(totalDur);
	}

	public static int getSubstreamNumber() {
		return (int)globalSeedInput.getValue().getNextSample(0.0);
	}

	/**
	 * Returns the largest random seed used by the objects in the simulation.
	 * @return largest random seed
	 */
	public static int getLargestStreamNumber() {
		int seed = 0;
		for (Entity each : Entity.getClonesOfIterator(Entity.class, RandomStreamUser.class)) {
			RandomStreamUser user = (RandomStreamUser) each;
			seed = Math.max(seed, user.getStreamNumber());
		}
		return seed;
	}

	/**
	 * Returns a list of objects that use the specified random stream.
	 * @param seed - random stream number
	 * @return users of the random stream
	 */
	public static ArrayList<RandomStreamUser> getRandomStreamUsers(int seed) {
		ArrayList<RandomStreamUser> ret = new ArrayList<>();
		for (Entity each : Entity.getClonesOfIterator(Entity.class, RandomStreamUser.class)) {
			RandomStreamUser user = (RandomStreamUser) each;
			if (user.getStreamNumber() == seed) {
				ret.add(user);
			}
		}
		return ret;
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
		if (incrementSize.isDefault())
			return snapGridSpacing.getValue();
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

	private final PauseModelTarget pauseModel = new PauseModelTarget(this);

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

	public static Vec3d getSnapGridPosition(Vec3d pos) {
		return getSnapGridPosition(pos, snapGridSpacing.getValue());
	}

	public static Vec3d getSnapGridPosition(Vec3d newPos, Vec3d oldPos, boolean shift) {
		return getSnapGridPosition(newPos, oldPos, shift, snapGridSpacing.getValue());
	}

	/**
	 * Returns the nearest point on the snap grid to the given coordinate.
	 * To avoid dithering, the new position must be at least one grid space
	 * from the old position.
	 * @param newPos - new coordinate for the object
	 * @param oldPos - present coordinate for the object
	 * @param spacing - distance between adjacent grid points
	 * @return newest snap grid point.
	 */
	public static Vec3d getSnapGridPosition(Vec3d newPos, Vec3d oldPos, double spacing) {
		Vec3d ret = new Vec3d(newPos);
		if (Math.abs(newPos.x - oldPos.x) < spacing)
			ret.x = oldPos.x;
		if (Math.abs(newPos.y - oldPos.y) < spacing)
			ret.y = oldPos.y;
		if (Math.abs(newPos.z - oldPos.z) < spacing)
			ret.z = oldPos.z;
		return Simulation.getSnapGridPosition(ret, spacing);
	}

	/**
	 * Returns the nearest point on the snap grid to the given coordinate.
	 * @param pos - position to be adjusted
	 * @param spacing - distance between adjacent grid points
	 * @return nearest snap grid point.
	 */
	public static Vec3d getSnapGridPosition(Vec3d pos, double spacing) {
		Vec3d ret = new Vec3d(pos);
		ret.x = spacing*Math.rint(ret.x/spacing);
		ret.y = spacing*Math.rint(ret.y/spacing);
		ret.z = spacing*Math.rint(ret.z/spacing);
		return ret;
	}

	public static Vec3d getSnapGridPosition(Vec3d newPos, Vec3d oldPos, boolean shift, double spacing) {
		Vec3d ret = getSnapGridPosition(newPos, oldPos, spacing);
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

	public static void setWindowDefaults() {
		modelBuilderPos.setDefaultValue(GUIFrame.COL1_START, GUIFrame.TOP_START);
		modelBuilderSize.setDefaultValue(GUIFrame.COL1_WIDTH, GUIFrame.HALF_TOP);
		objectSelectorPos.setDefaultValue(GUIFrame.COL1_START, GUIFrame.BOTTOM_START);
		objectSelectorSize.setDefaultValue(GUIFrame.COL1_WIDTH, GUIFrame.HALF_BOTTOM);
		inputEditorPos.setDefaultValue(GUIFrame.COL2_START, GUIFrame.LOWER_START);
		inputEditorSize.setDefaultValue(GUIFrame.COL2_WIDTH, GUIFrame.LOWER_HEIGHT);
		outputViewerPos.setDefaultValue(GUIFrame.COL3_START, GUIFrame.LOWER_START);
		outputViewerSize.setDefaultValue(GUIFrame.COL3_WIDTH, GUIFrame.LOWER_HEIGHT);
		propertyViewerPos.setDefaultValue(GUIFrame.COL4_START, GUIFrame.TOP_START);
		propertyViewerSize.setDefaultValue(GUIFrame.COL4_WIDTH, GUIFrame.HALF_TOP);
		logViewerPos.setDefaultValue(GUIFrame.COL4_START, GUIFrame.BOTTOM_START);
		logViewerSize.setDefaultValue(GUIFrame.COL4_WIDTH, GUIFrame.HALF_BOTTOM);
		eventViewerPos.setDefaultValue(GUIFrame.COL4_START, GUIFrame.BOTTOM_START);
		eventViewerSize.setDefaultValue(GUIFrame.COL4_WIDTH, GUIFrame.HALF_BOTTOM);
		controlPanelWidth.setDefaultValue(GUIFrame.DEFAULT_GUI_WIDTH);
	}

	public static void resetWindowPositionsAndSizes() {
		InputAgent.applyArgs(getInstance(), modelBuilderPos.getKeyword());
		InputAgent.applyArgs(getInstance(), modelBuilderSize.getKeyword());
		InputAgent.applyArgs(getInstance(), objectSelectorPos.getKeyword());
		InputAgent.applyArgs(getInstance(), objectSelectorSize.getKeyword());
		InputAgent.applyArgs(getInstance(), inputEditorPos.getKeyword());
		InputAgent.applyArgs(getInstance(), inputEditorSize.getKeyword());
		InputAgent.applyArgs(getInstance(), outputViewerPos.getKeyword());
		InputAgent.applyArgs(getInstance(), outputViewerSize.getKeyword());
		InputAgent.applyArgs(getInstance(), propertyViewerPos.getKeyword());
		InputAgent.applyArgs(getInstance(), propertyViewerSize.getKeyword());
		InputAgent.applyArgs(getInstance(), logViewerPos.getKeyword());
		InputAgent.applyArgs(getInstance(), logViewerSize.getKeyword());
		InputAgent.applyArgs(getInstance(), eventViewerPos.getKeyword());
		InputAgent.applyArgs(getInstance(), eventViewerSize.getKeyword());
		InputAgent.applyArgs(getInstance(), controlPanelWidth.getKeyword());
	}

	public static IntegerVector getModelBuilderPos() {
		return modelBuilderPos.getValue();
	}

	public static IntegerVector getModelBuilderSize() {
		return modelBuilderSize.getValue();
	}

	public static IntegerVector getObjectSelectorPos() {
		return objectSelectorPos.getValue();
	}

	public static IntegerVector getObjectSelectorSize() {
		return objectSelectorSize.getValue();
	}

	public static IntegerVector getInputEditorPos() {
		return inputEditorPos.getValue();
	}

	public static IntegerVector getInputEditorSize() {
		return inputEditorSize.getValue();
	}

	public static IntegerVector getOutputViewerPos() {
		return outputViewerPos.getValue();
	}

	public static IntegerVector getOutputViewerSize() {
		return outputViewerSize.getValue();
	}

	public static IntegerVector getPropertyViewerPos() {
		return propertyViewerPos.getValue();
	}

	public static IntegerVector getPropertyViewerSize() {
		return propertyViewerSize.getValue();
	}

	public static IntegerVector getLogViewerPos() {
		return logViewerPos.getValue();
	}

	public static IntegerVector getLogViewerSize() {
		return logViewerSize.getValue();
	}

	public static IntegerVector getEventViewerPos() {
		return eventViewerPos.getValue();
	}

	public static IntegerVector getEventViewerSize() {
		return eventViewerSize.getValue();
	}

	public static void setModelBuilderPos(int x, int y) {
		if (modelBuilderPos.getValue().get(0) == x && modelBuilderPos.getValue().get(1) == y)
			return;
		InputAgent.applyIntegers(getInstance(), modelBuilderPos.getKeyword(), x, y);
	}

	public static void setModelBuilderSize(int x, int y) {
		if (modelBuilderSize.getValue().get(0) == x && modelBuilderSize.getValue().get(1) == y)
			return;
		InputAgent.applyIntegers(getInstance(), modelBuilderSize.getKeyword(), x, y);
	}

	public static void setObjectSelectorPos(int x, int y) {
		if (objectSelectorPos.getValue().get(0) == x && objectSelectorPos.getValue().get(1) == y)
			return;
		InputAgent.applyIntegers(getInstance(), objectSelectorPos.getKeyword(), x, y);
	}

	public static void setObjectSelectorSize(int x, int y) {
		if (objectSelectorSize.getValue().get(0) == x && objectSelectorSize.getValue().get(1) == y)
			return;
		InputAgent.applyIntegers(getInstance(), objectSelectorSize.getKeyword(), x, y);
	}

	public static void setInputEditorPos(int x, int y) {
		if (inputEditorPos.getValue().get(0) == x && inputEditorPos.getValue().get(1) == y)
			return;
		InputAgent.applyIntegers(getInstance(), inputEditorPos.getKeyword(), x, y);
	}

	public static void setInputEditorSize(int x, int y) {
		if (inputEditorSize.getValue().get(0) == x && inputEditorSize.getValue().get(1) == y)
			return;
		InputAgent.applyIntegers(getInstance(), inputEditorSize.getKeyword(), x, y);
	}

	public static void setOutputViewerPos(int x, int y) {
		if (outputViewerPos.getValue().get(0) == x && outputViewerPos.getValue().get(1) == y)
			return;
		InputAgent.applyIntegers(getInstance(), outputViewerPos.getKeyword(), x, y);
	}

	public static void setOutputViewerSize(int x, int y) {
		if (outputViewerSize.getValue().get(0) == x && outputViewerSize.getValue().get(1) == y)
			return;
		InputAgent.applyIntegers(getInstance(), outputViewerSize.getKeyword(), x, y);
	}

	public static void setPropertyViewerPos(int x, int y) {
		if (propertyViewerPos.getValue().get(0) == x && propertyViewerPos.getValue().get(1) == y)
			return;
		InputAgent.applyIntegers(getInstance(), propertyViewerPos.getKeyword(), x, y);
	}

	public static void setPropertyViewerSize(int x, int y) {
		if (propertyViewerSize.getValue().get(0) == x && propertyViewerSize.getValue().get(1) == y)
			return;
		InputAgent.applyIntegers(getInstance(), propertyViewerSize.getKeyword(), x, y);
	}

	public static void setLogViewerPos(int x, int y) {
		if (logViewerPos.getValue().get(0) == x && logViewerPos.getValue().get(1) == y)
			return;
		InputAgent.applyIntegers(getInstance(), logViewerPos.getKeyword(), x, y);
	}

	public static void setLogViewerSize(int x, int y) {
		if (logViewerSize.getValue().get(0) == x && logViewerSize.getValue().get(1) == y)
			return;
		InputAgent.applyIntegers(getInstance(), logViewerSize.getKeyword(), x, y);
	}

	public static void setEventViewerPos(int x, int y) {
		if (eventViewerPos.getValue().get(0) == x && eventViewerPos.getValue().get(1) == y)
			return;
		InputAgent.applyIntegers(getInstance(), eventViewerPos.getKeyword(), x, y);
	}

	public static void setEventViewerSize(int x, int y) {
		if (eventViewerSize.getValue().get(0) == x && eventViewerSize.getValue().get(1) == y)
			return;
		InputAgent.applyIntegers(getInstance(), eventViewerSize.getKeyword(), x, y);
	}

	public static void setControlPanelWidth(int width) {
		if (controlPanelWidth.getValue() == width)
			return;
		// Temporary workaround for the callback being done before autoload.cfg is loaded
		if (getInstance() == null)
			return;
		InputAgent.applyIntegers(getInstance(), controlPanelWidth.getKeyword(), width);
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
		if (EventViewer.hasInstance())
			setWindowVisible(EventViewer.getInstance(), showEventViewer.getValue());
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
		if (EventViewer.hasInstance())
			setWindowVisible(EventViewer.getInstance(), false);
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
