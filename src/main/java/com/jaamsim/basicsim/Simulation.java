/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2002-2011 Ausenco Engineering Canada Inc.
 * Copyright (C) 2016-2024 JaamSim Software Inc.
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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

import javax.swing.SwingUtilities;

import com.jaamsim.Commands.KeywordCommand;
import com.jaamsim.Samples.SampleInput;
import com.jaamsim.StringProviders.StringProvListInput;
import com.jaamsim.datatypes.IntegerVector;
import com.jaamsim.input.BooleanInput;
import com.jaamsim.input.DateInput;
import com.jaamsim.input.DirInput;
import com.jaamsim.input.EntityListInput;
import com.jaamsim.input.Input;
import com.jaamsim.input.InputAgent;
import com.jaamsim.input.InputCallback;
import com.jaamsim.input.InputErrorException;
import com.jaamsim.input.IntegerInput;
import com.jaamsim.input.IntegerListInput;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.KeywordIndex;
import com.jaamsim.input.Output;
import com.jaamsim.input.RunNumberInput;
import com.jaamsim.input.UnitTypeListInput;
import com.jaamsim.input.ValueInput;
import com.jaamsim.math.Vec3d;
import com.jaamsim.ui.AboutBox;
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
	private final SampleInput runDuration;

	@Keyword(description = "The initialization interval for the simulation run. The model will "
	                     + "run for the InitializationDuration interval and then clear the "
	                     + "statistics and execute for the specified RunDuration interval. "
	                     + "The total length of the simulation run will be the sum of the "
	                     + "InitializationDuration and RunDuration inputs.",
	         exampleList = {"720 h"})
	private final SampleInput initializationTime;

	@Keyword(description = "If TRUE, the simulation uses the standard Gregorian calendar that "
	                     + "includes leap years. "
	                     + "If FALSE, the simulation uses a simplified calendar that has a fixed "
	                     + "365 days per year.")
	private final BooleanInput gregorianCalendar;

	@Keyword(description = "The calendar date and time that corresponds to zero simulation time.",
	         exampleList = {"2000-09-01", "'2000-09-01 00:08:00'"} )
	private final DateInput startDate;

	@Keyword(description = "An optional expression that pauses the run when TRUE is returned.",
	         exampleList = {"'[Queue1].QueueLength > 20'"})
	private final SampleInput pauseConditionInput;

	@Keyword(description = "If TRUE, the simulation run will be terminated when the "
	                     + "PauseCondition expression returns TRUE. If multiple runs have been "
	                     + "specified, then the next run will be started. If no more runs have "
	                     + "been specified, the simulation will be paused or terminated "
	                     + "depending on the input to the ExitAtStop keyword.")
	private final BooleanInput exitAtPauseCondition;

	@Keyword(description = "If TRUE, the program will be closed on completion of the last "
	                     + "simulation run. Otherwise, the last run will be paused.")
	private final BooleanInput exitAtStop;

	@Keyword(description = "Substream number applied to each probability distribution in the "
	                     + "model. "
	                     + "Accepts an integer value >= 0.\n\n"
	                     + "GlobalSubstreamSeed works together with the 'RandomSeed' input for "
	                     + "each probability distribution to determine its random sequence. "
	                     + "It allows the user to change all the random sequences in a model with "
	                     + "a single input.\n\n"
	                     + "The default value is the replication number for the simulation run "
	                     + "being executed, which is appropriate for most applications.",
	         exampleList = {"5", "'this.ReplicationNumber + 100'"})
	private final SampleInput globalSeedInput;

	@Keyword(description = "If TRUE, a full output report is printed to the file "
	                     + "<configuration file name>.rep at the end of the simulation run.")
	private final BooleanInput printReport;

	@Keyword(description = "The directory in which to place the output report. Defaults to the "
	                     + "directory containing the configuration file for the run.",
	         exampleList = {"'c:/reports/'"})
	private final DirInput reportDirectory;

	@Keyword(description = "Not Used")
	private final UnitTypeListInput unitTypeList;

	@Keyword(description = "One or more selected outputs to be printed in tabular form to a "
	                     + "custom output report. "
	                     + "One row is printed to the report at the end of each simulation run. "
	                     + "An additional row is printed at the end of each scenario that "
	                     + "displays the aggregate average value and the 95% confidence interval "
	                     + "for each output."
	                     + "\n\n"
	                     + "In script mode (-s tag), the selected outputs are printed to the "
	                     + "command line (standard out). "
	                     + "Otherwise, they are printed to the file <configuration file name>.dat."
	                     + "\n\n"
	                     + "It is best to include only dimensionless quantities and non-numeric "
	                     + "outputs in the RunOutputList. "
	                     + "An output with dimensions can be made non-dimensional by dividing it "
	                     + "by 1 in the desired unit, e.g. '[Queue1].AverageQueueTime / 1[h]' is "
	                     + "the average queue time in hours. "
	                     + "A dimensional number will be displayed along with its unit. "
	                     + "The 'format' function can be used if a fixed number of decimal places "
	                     + "is required.",
	         exampleList = {"{ [Queue1].QueueLengthAverage } { [Queue1].AverageQueueTime/1[h] }"})
	protected final StringProvListInput runOutputList;

	@Keyword(description = "One or more selected outputs that specify the key inputs to each "
	                     + "scenario. "
	                     + "These values appear at the beginning of each row of the custom output "
	                     + "report.",
	         exampleList = {"{ [RunInputs].IAT/1[min] } { [RunInputs].ServiceTime/1[min] }"})
	protected final StringProvListInput runParameterList;

	@Keyword(description = "The maximum number of entities to display in the view windows. "
	                     + "A model can contain more than this number of entities, but only this "
	                     + "number will be displayed. "
	                     + "This limit prevents JaamSim from becoming unresponsive when the "
	                     + "number of entities in a model exceeds the graphics capabilities of "
	                     + "the computer.",
	         exampleList = {"100000"})
	private final IntegerInput maxEntitiesToDisplay;

	@Keyword(description = "If TRUE, the 'Trace' keyword for each object is activated. "
	                     + "For an object to be traced, both the 'EnableTracing' input for "
	                     + "Simulation and the 'Trace' input for the object must be set to TRUE. "
	                     + "Trace outputs are written to standard out and can used by a "
	                     + "programmer to track the internal logic for one or more selected "
	                     + "objects as the model is executed. "
	                     + "Tracing is an essential tool for testing and debugging the Java code "
	                     + "for JaamSim.")
	private final BooleanInput enableTracing;

	@Keyword(description = "If TRUE, an additional output file is generated that traces the exact "
	                     + "sequence of events executed by the model. "
	                     + "The event file is named <configuration file name>.evt and is placed "
	                     + "in the same folder as the configuration file.")
	private final BooleanInput traceEventsInput;

	@Keyword(description = "If TRUE, the events executed by the model are compared to those in an "
	                     + "event file that was generated previously using the TraceEvents "
	                     + "keyword. "
	                     + "An error message is generated if an event executed by the model "
	                     + "differs in any way from the ones specified by the event file. "
	                     + "The event file must be named <configuration file name>.evt and placed "
	                     + "in the same folder as the configuration file.")
	private final BooleanInput verifyEventsInput;

	@Keyword(description = "The length of time represented by one simulation tick.",
	         exampleList = {"1e-6 s"})
	private final ValueInput tickLengthInput;

	// Multiple Runs tab
	@Keyword(description = "Defines the number of scenario indices and the maximum value N for "
	                     + "each index. "
	                     + "When running multiple scenarios, each index will be iterated from "
	                     + "1 to N starting with the last index. "
	                     + "One scenario will be executed for every combination of the scenario "
	                     + "index values. "
	                     + "For example, if three scenario indices are defined with ranges of 3, "
	                     + "5, and 10, then a total of 3*5*10 = 150 scenarios will be executed.\n\n"
	                     + "If left blank, a single scenario index is defined and there are no "
	                     + "restrictions on the values that can be assigned to the "
	                     + "StartingScenarioNumber and EndingScenarioNumber inputs.",
	         exampleList = {"3 5 10"})
	private final IntegerListInput scenarioIndexDefinitionList;

	@Keyword(description = "The first scenario number to be executed.",
	         exampleList = {"22", "1-3-2"})
	private final RunNumberInput startingScenarioNumber;

	@Keyword(description = "The last scenario number to be executed.\n\n"
	                     + "The default value is the 'StartingScenarioNumber' input.",
	         exampleList = {"78", "2-3-8"})
	private final RunNumberInput endingScenarioNumber;

	@Keyword(description = "The number of replications to perform for each scenario.",
	         exampleList = {"10"})
	private final SampleInput numberOfReplications;

	@Keyword(description = "The number of simulation runs to perform simultaneously while "
	                     + "executing the specified number of scenarios and replications.",
	         exampleList = {"10"})
	private final IntegerInput numberOfThreads;

	@Keyword(description = "If TRUE, the run output report will include an entry for each "
	                     + "replication that was performed. "
	                     + "If FALSE, the report will show entries only for the scenarios.")
	private final BooleanInput printReplications;

	@Keyword(description = "If TRUE, the run output report will include the 95% confidence "
	                     + "intervals for the outputs defined by the input to RunOutputList "
	                     + "keyword. "
	                     + "The confidence intervals are calculated using the factor for the "
	                     + "Student's T distribution corresponding to 95% confidence and the "
	                     + "standard deviation for the output values over the replications.")
	private final BooleanInput printConfidenceIntervals;

	@Keyword(description = "If TRUE, the first column of the run output report will show "
	                     + "the scenario number for each simulation run. "
	                     + "If the PrintReplications input is also TRUE, then the second column "
	                     + "will show the replication number.")
	private final BooleanInput printRunLabels;

	// GUI tab
	@Keyword(description = "An optional list of units to be used for displaying model outputs.",
	         exampleList = {"h kt"})
	private final EntityListInput<? extends Unit> displayedUnits;

	@Keyword(description = "If TRUE, a dragged object will be positioned to the nearest grid "
	                     + "point.")
	private final BooleanInput snapToGrid;

	@Keyword(description = "The distance between snap grid points.",
	         exampleList = {"1 m"})
	private final ValueInput snapGridSpacing;

	@Keyword(description = "The distance moved by the selected entity when the an arrow key is "
	                     + "pressed. Defaults to the SnapGridSpacing value.",
	         exampleList = {"1 cm"})
	private final ValueInput incrementSize;

	@Keyword(description = "If TRUE, the simulation is executed a constant multiple of real time. "
	                     + "Otherwise, the run is executed as fast as possible, limited only by "
	                     + "processor speed.")
	private final BooleanInput realTime;

	@Keyword(description = "The target ratio of elapsed simulation time to elapsed real time.",
	         exampleList = {"1200"})
	private final ValueInput realTimeFactor;

	public static final double DEFAULT_REAL_TIME_FACTOR = 1;
	public static final double MIN_REAL_TIME_FACTOR = 1e-6;
	public static final double MAX_REAL_TIME_FACTOR = 1e6;

	@Keyword(description = "The time at which the simulation will be paused.",
	         exampleList = {"200 h"})
	private final ValueInput pauseTime;

	@Keyword(description = "The state of the 'Show Labels' button on the Control Panel.")
	private final BooleanInput showLabels;

	@Keyword(description = "The state of the 'Show SubModels' button on the Control Panel.")
	private final BooleanInput showSubModels;

	@Keyword(description = "The state of the 'Presentation Mode' button on the Control Panel.")
	private final BooleanInput presentationMode;

	@Keyword(description = "The state of the 'LockWindows' button on the Control Panel.")
	private final BooleanInput lockWindows;

	@Keyword(description = "The state of the 'Show References' button on the Control Panel.")
	private final BooleanInput showReferences;

	@Keyword(description = "The state of the 'Show Entity Flow' button on the Control Panel.")
	private final BooleanInput showEntityFlow;

	@Keyword(description = "If TRUE, the Model Builder tool is shown on startup.")
	private final BooleanInput showModelBuilder;

	@Keyword(description = "If TRUE, the Object Selector tool is shown on startup.")
	private final BooleanInput showObjectSelector;

	@Keyword(description = "If TRUE, the Input Editor tool is shown on startup.")
	private final BooleanInput showInputEditor;

	@Keyword(description = "If TRUE, the Output Viewer tool is shown on startup.")
	private final BooleanInput showOutputViewer;

	@Keyword(description = "If TRUE, the Property Viewer tool is shown on startup.")
	private final BooleanInput showPropertyViewer;

	@Keyword(description = "If TRUE, the Log Viewer tool is shown on startup.")
	private final BooleanInput showLogViewer;

	@Keyword(description = "If TRUE, the Event Viewer tool is shown on startup.")
	private final BooleanInput showEventViewer;

	@Keyword(description = "The position of the upper left corner of the Model Builder window "
	                     + "in pixels measured from the top left corner of the screen.",
	         exampleList = {"220 110"})
	private final IntegerListInput modelBuilderPos;

	@Keyword(description = "The size of the Model Builder window in pixels (width, height).",
	         exampleList = {"500 300"})
	private final IntegerListInput modelBuilderSize;

	@Keyword(description = "The position of the upper left corner of the Object Selector window "
	                     + "in pixels measured from the top left corner of the screen.",
	         exampleList = {"220 110"})
	private final IntegerListInput objectSelectorPos;

	@Keyword(description = "The size of the Object Selector window in pixels (width, height).",
	         exampleList = {"500 300"})
	private final IntegerListInput objectSelectorSize;

	@Keyword(description = "The position of the upper left corner of the Input Editor window "
	                     + "in pixels measured from the top left corner of the screen.",
	         exampleList = {"220 110"})
	private final IntegerListInput inputEditorPos;

	@Keyword(description = "The size of the Input Editor window in pixels (width, height).",
	         exampleList = {"500 300"})
	private final IntegerListInput inputEditorSize;

	@Keyword(description = "The position of the upper left corner of the Output Viewer window "
	                     + "in pixels measured from the top left corner of the screen.",
	         exampleList = {"220 110"})
	private final IntegerListInput outputViewerPos;

	@Keyword(description = "The size of the Output Viewer window in pixels (width, height).",
	         exampleList = {"500 300"})
	private final IntegerListInput outputViewerSize;

	@Keyword(description = "The position of the upper left corner of the Property Viewer window "
	                     + "in pixels measured from the top left corner of the screen.",
	         exampleList = {"220 110"})
	private final IntegerListInput propertyViewerPos;

	@Keyword(description = "The size of the Property Viewer window in pixels (width, height).",
	         exampleList = {"500 300"})
	private final IntegerListInput propertyViewerSize;

	@Keyword(description = "The position of the upper left corner of the Log Viewer window "
	                     + "in pixels measured from the top left corner of the screen.",
	         exampleList = {"220 110"})
	private final IntegerListInput logViewerPos;

	@Keyword(description = "The size of the Log Viewer window in pixels (width, height).",
	         exampleList = {"500 300"})
	private final IntegerListInput logViewerSize;

	@Keyword(description = "The position of the upper left corner of the Event Viewer window "
	                     + "in pixels measured from the top left corner of the screen.",
	         exampleList = {"220 110"})
	private final IntegerListInput eventViewerPos;

	@Keyword(description = "The size of the Event Viewer window in pixels (width, height).",
	         exampleList = {"500 300"})
	private final IntegerListInput eventViewerSize;

	@Keyword(description = "The width of the Control Panel window in pixels.",
	         exampleList = {"1920"})
	private final IntegerInput controlPanelWidth;

	@Keyword(description = "Time at which the simulation run is started (hh:mm).",
	         exampleList = {"2160 h"})
	private final ValueInput startTimeInput;

	// Hidden keywords
	@Keyword(description = "If TRUE, then the input report file will be printed after loading "
	                     + "the configuration file.  The input report can always be generated "
	                     + "when needed by selecting \"Print Input Report\" under the File menu.")
	private final BooleanInput printInputReport;

	{
		// Key Inputs tab
		runDuration = new SampleInput("RunDuration", KEY_INPUTS, 31536000.0d);
		runDuration.setUnitType(TimeUnit.class);
		runDuration.setValidRange(1e-15d, Double.POSITIVE_INFINITY);
		runDuration.setOutput(true);
		this.addInput(runDuration);

		initializationTime = new SampleInput("InitializationDuration", KEY_INPUTS, 0.0);
		initializationTime.setUnitType(TimeUnit.class);
		initializationTime.setValidRange(0.0d, Double.POSITIVE_INFINITY);
		initializationTime.setOutput(true);
		this.addInput(initializationTime);

		gregorianCalendar = new BooleanInput("GregorianCalendar", OPTIONS, false);
		gregorianCalendar.setCallback(calendarCallback);
		this.addInput(gregorianCalendar);

		startDate = new DateInput("StartDate", OPTIONS, new SimDate(1970, 1, 1));
		startDate.setCallback(calendarCallback);
		this.addInput(startDate);

		pauseConditionInput = new SampleInput("PauseCondition", OPTIONS, Double.NaN);
		pauseConditionInput.setUnitType(DimensionlessUnit.class);
		this.addInput(pauseConditionInput);

		exitAtPauseCondition = new BooleanInput("ExitAtPauseCondition", OPTIONS, false);
		this.addInput(exitAtPauseCondition);

		exitAtStop = new BooleanInput("ExitAtStop", KEY_INPUTS, false);
		this.addInput(exitAtStop);

		globalSeedInput = new SampleInput("GlobalSubstreamSeed", KEY_INPUTS, 0);
		globalSeedInput.setUnitType(DimensionlessUnit.class);
		globalSeedInput.setIntegerValue(true);
		globalSeedInput.setValidRange(0, Integer.MAX_VALUE);
		globalSeedInput.setDefaultText("this.ReplicationNumber");
		this.addInput(globalSeedInput);

		printReport = new BooleanInput("PrintReport", KEY_INPUTS, false);
		this.addInput(printReport);

		reportDirectory = new DirInput("ReportDirectory", KEY_INPUTS, null);
		reportDirectory.setDefaultText("Configuration File Directory");
		reportDirectory.setCallback(reportDirectoryCallback);
		this.addInput(reportDirectory);

		unitTypeList = new UnitTypeListInput("UnitTypeList", KEY_INPUTS, null);
		unitTypeList.setHidden(true);
		this.addInput(unitTypeList);

		runOutputList = new StringProvListInput("RunOutputList", KEY_INPUTS, null);
		this.addInput(runOutputList);

		runParameterList = new StringProvListInput("RunParameterList", KEY_INPUTS, null);
		this.addInput(runParameterList);

		maxEntitiesToDisplay = new IntegerInput("MaxEntitiesToDisplay", OPTIONS, 10000);
		maxEntitiesToDisplay.setValidRange(0, Integer.MAX_VALUE);
		this.addInput(maxEntitiesToDisplay);

		enableTracing = new BooleanInput("EnableTracing", OPTIONS, false);
		enableTracing.setCallback(enableTracingCallback);
		this.addInput(enableTracing);

		traceEventsInput = new BooleanInput("TraceEvents", OPTIONS, false);
		this.addInput(traceEventsInput);

		verifyEventsInput = new BooleanInput("VerifyEvents", OPTIONS, false);
		this.addInput(verifyEventsInput);

		tickLengthInput = new ValueInput("TickLength", OPTIONS, 1e-6d);
		tickLengthInput.setUnitType(TimeUnit.class);
		tickLengthInput.setValidRange(1e-12d, Double.POSITIVE_INFINITY);
		this.addInput(tickLengthInput);

		// Multiple Runs tab
		scenarioIndexDefinitionList = new IntegerListInput("ScenarioIndexDefinitionList", MULTIPLE_RUNS, new IntegerVector());
		scenarioIndexDefinitionList.setCallback(scenarioIndexDefinitionListCallback);
		this.addInput(scenarioIndexDefinitionList);
		this.addSynonym(scenarioIndexDefinitionList, "RunIndexDefinitionList");

		startingScenarioNumber = new RunNumberInput("StartingScenarioNumber", MULTIPLE_RUNS, 1);
		startingScenarioNumber.setUnitType(DimensionlessUnit.class);
		startingScenarioNumber.setIntegerValue(true);
		startingScenarioNumber.setValidRange(1, Integer.MAX_VALUE);
		startingScenarioNumber.setCallback(startingScenarioNumberCallback);
		this.addInput(startingScenarioNumber);
		this.addSynonym(startingScenarioNumber, "StartingRunNumber");

		endingScenarioNumber = new RunNumberInput("EndingScenarioNumber", MULTIPLE_RUNS, 1);
		endingScenarioNumber.setUnitType(DimensionlessUnit.class);
		endingScenarioNumber.setIntegerValue(true);
		endingScenarioNumber.setValidRange(1, Integer.MAX_VALUE);
		endingScenarioNumber.setDefaultText("StartingScenarioNumber");
		this.addInput(endingScenarioNumber);
		this.addSynonym(endingScenarioNumber, "EndingRunNumber");

		numberOfReplications = new SampleInput("NumberOfReplications", MULTIPLE_RUNS, 1);
		numberOfReplications.setUnitType(DimensionlessUnit.class);
		numberOfReplications.setIntegerValue(true);
		numberOfReplications.setValidRange(1, Integer.MAX_VALUE);
		numberOfReplications.setOutput(true);
		this.addInput(numberOfReplications);

		numberOfThreads = new IntegerInput("NumberOfThreads", MULTIPLE_RUNS, 1);
		numberOfThreads.setValidRange(1, Integer.MAX_VALUE);
		this.addInput(numberOfThreads);

		printReplications = new BooleanInput("PrintReplications", MULTIPLE_RUNS, true);
		this.addInput(printReplications);

		printConfidenceIntervals = new BooleanInput("PrintConfidenceIntervals", MULTIPLE_RUNS, true);
		this.addInput(printConfidenceIntervals);

		printRunLabels = new BooleanInput("PrintRunLabels", MULTIPLE_RUNS, true);
		this.addInput(printRunLabels);

		// GUI tab
		displayedUnits = new EntityListInput<>(Unit.class, "DisplayedUnits", GUI, new ArrayList<Unit>());
		displayedUnits.setDefaultText("SI Units");
		displayedUnits.setPromptReqd(false);
		displayedUnits.setHidden(true);
		displayedUnits.setCallback(displayedUnitsCallback);
		this.addInput(displayedUnits);

		realTime = new BooleanInput("RealTime", GUI, false);
		realTime.setPromptReqd(false);
		realTime.setHidden(true);
		this.addInput(realTime);

		snapToGrid = new BooleanInput("SnapToGrid", GUI, false);
		snapToGrid.setPromptReqd(false);
		snapToGrid.setHidden(true);
		this.addInput(snapToGrid);

		snapGridSpacing = new ValueInput("SnapGridSpacing", GUI, 0.1d);
		snapGridSpacing.setUnitType(DistanceUnit.class);
		snapGridSpacing.setValidRange(1.0e-6, Double.POSITIVE_INFINITY);
		snapGridSpacing.setPromptReqd(false);
		snapGridSpacing.setHidden(true);
		this.addInput(snapGridSpacing);

		incrementSize = new ValueInput("IncrementSize", GUI, 0.1d);
		incrementSize.setUnitType(DistanceUnit.class);
		incrementSize.setValidRange(1.0e-6, Double.POSITIVE_INFINITY);
		incrementSize.setPromptReqd(false);
		incrementSize.setHidden(true);
		this.addInput(incrementSize);

		realTimeFactor = new ValueInput("RealTimeFactor", GUI, DEFAULT_REAL_TIME_FACTOR);
		realTimeFactor.setValidRange(MIN_REAL_TIME_FACTOR, MAX_REAL_TIME_FACTOR);
		realTimeFactor.setPromptReqd(false);
		realTimeFactor.setHidden(true);
		this.addInput(realTimeFactor);

		pauseTime = new ValueInput("PauseTime", GUI, Double.POSITIVE_INFINITY);
		pauseTime.setUnitType(TimeUnit.class);
		pauseTime.setValidRange(0.0d, Double.POSITIVE_INFINITY);
		pauseTime.setPromptReqd(false);
		pauseTime.setHidden(true);
		this.addInput(pauseTime);

		showLabels = new BooleanInput("ShowLabels", GUI, false);
		showLabels.setPromptReqd(false);
		showLabels.setHidden(true);
		this.addInput(showLabels);

		showSubModels = new BooleanInput("ShowSubModels", GUI, false);
		showSubModels.setPromptReqd(false);
		showSubModels.setHidden(true);
		this.addInput(showSubModels);

		presentationMode = new BooleanInput("PresentationMode", GUI, false);
		presentationMode.setPromptReqd(false);
		presentationMode.setHidden(true);
		this.addInput(presentationMode);

		lockWindows = new BooleanInput("LockWindows", GUI, false);
		lockWindows.setPromptReqd(false);
		lockWindows.setHidden(true);
		lockWindows.setCallback(lockWindowsCallback);
		this.addInput(lockWindows);

		showReferences = new BooleanInput("ShowReferences", GUI, false);
		showReferences.setPromptReqd(false);
		showReferences.setHidden(true);
		this.addInput(showReferences);

		showEntityFlow = new BooleanInput("ShowEntityFlow", GUI, false);
		showEntityFlow.setPromptReqd(false);
		showEntityFlow.setHidden(true);
		this.addInput(showEntityFlow);

		showModelBuilder = new BooleanInput("ShowModelBuilder", GUI, false);
		showModelBuilder.setPromptReqd(false);
		showModelBuilder.setHidden(true);
		this.addInput(showModelBuilder);

		showObjectSelector = new BooleanInput("ShowObjectSelector", GUI, false);
		showObjectSelector.setPromptReqd(false);
		showObjectSelector.setHidden(true);
		this.addInput(showObjectSelector);

		showInputEditor = new BooleanInput("ShowInputEditor", GUI, false);
		showInputEditor.setPromptReqd(false);
		showInputEditor.setHidden(true);
		this.addInput(showInputEditor);

		showOutputViewer = new BooleanInput("ShowOutputViewer", GUI, false);
		showOutputViewer.setPromptReqd(false);
		showOutputViewer.setHidden(true);
		this.addInput(showOutputViewer);

		showPropertyViewer = new BooleanInput("ShowPropertyViewer", GUI, false);
		showPropertyViewer.setPromptReqd(false);
		showPropertyViewer.setHidden(true);
		this.addInput(showPropertyViewer);

		showLogViewer = new BooleanInput("ShowLogViewer", GUI, false);
		showLogViewer.setPromptReqd(false);
		showLogViewer.setHidden(true);
		this.addInput(showLogViewer);

		showEventViewer = new BooleanInput("ShowEventViewer", GUI, false);
		showEventViewer.setPromptReqd(false);
		showEventViewer.setHidden(true);
		this.addInput(showEventViewer);

		final IntegerVector def = new IntegerVector(2);
		def.add(1);
		def.add(1);

		modelBuilderPos = new IntegerListInput("ModelBuilderPos", GUI, def);
		modelBuilderPos.setValidCount(2);
		modelBuilderPos.setValidRange(-8192, 8192);
		modelBuilderPos.setPromptReqd(false);
		modelBuilderPos.setHidden(true);
		this.addInput(modelBuilderPos);

		modelBuilderSize = new IntegerListInput("ModelBuilderSize", GUI, def);
		modelBuilderSize.setValidCount(2);
		modelBuilderSize.setValidRange(1, 8192);
		modelBuilderSize.setPromptReqd(false);
		modelBuilderSize.setHidden(true);
		this.addInput(modelBuilderSize);

		objectSelectorPos = new IntegerListInput("ObjectSelectorPos", GUI, def);
		objectSelectorPos.setValidCount(2);
		objectSelectorPos.setValidRange(-8192, 8192);
		objectSelectorPos.setPromptReqd(false);
		objectSelectorPos.setHidden(true);
		this.addInput(objectSelectorPos);

		objectSelectorSize = new IntegerListInput("ObjectSelectorSize", GUI, def);
		objectSelectorSize.setValidCount(2);
		objectSelectorSize.setValidRange(1, 8192);
		objectSelectorSize.setPromptReqd(false);
		objectSelectorSize.setHidden(true);
		this.addInput(objectSelectorSize);

		inputEditorPos = new IntegerListInput("InputEditorPos", GUI, def);
		inputEditorPos.setValidCount(2);
		inputEditorPos.setValidRange(-8192, 8192);
		inputEditorPos.setPromptReqd(false);
		inputEditorPos.setHidden(true);
		this.addInput(inputEditorPos);

		inputEditorSize = new IntegerListInput("InputEditorSize", GUI, def);
		inputEditorSize.setValidCount(2);
		inputEditorSize.setValidRange(1, 8192);
		inputEditorSize.setPromptReqd(false);
		inputEditorSize.setHidden(true);
		this.addInput(inputEditorSize);

		outputViewerPos = new IntegerListInput("OutputViewerPos", GUI, def);
		outputViewerPos.setValidCount(2);
		outputViewerPos.setValidRange(-8192, 8192);
		outputViewerPos.setPromptReqd(false);
		outputViewerPos.setHidden(true);
		this.addInput(outputViewerPos);

		outputViewerSize = new IntegerListInput("OutputViewerSize", GUI, def);
		outputViewerSize.setValidCount(2);
		outputViewerSize.setValidRange(1, 8192);
		outputViewerSize.setPromptReqd(false);
		outputViewerSize.setHidden(true);
		this.addInput(outputViewerSize);

		propertyViewerPos = new IntegerListInput("PropertyViewerPos", GUI, def);
		propertyViewerPos.setValidCount(2);
		propertyViewerPos.setValidRange(-8192, 8192);
		propertyViewerPos.setPromptReqd(false);
		propertyViewerPos.setHidden(true);
		this.addInput(propertyViewerPos);

		propertyViewerSize = new IntegerListInput("PropertyViewerSize", GUI, def);
		propertyViewerSize.setValidCount(2);
		propertyViewerSize.setValidRange(1, 8192);
		propertyViewerSize.setPromptReqd(false);
		propertyViewerSize.setHidden(true);
		this.addInput(propertyViewerSize);

		logViewerPos = new IntegerListInput("LogViewerPos", GUI, def);
		logViewerPos.setValidCount(2);
		logViewerPos.setValidRange(-8192, 8192);
		logViewerPos.setPromptReqd(false);
		logViewerPos.setHidden(true);
		this.addInput(logViewerPos);

		logViewerSize = new IntegerListInput("LogViewerSize", GUI, def);
		logViewerSize.setValidCount(2);
		logViewerSize.setValidRange(1, 8192);
		logViewerSize.setPromptReqd(false);
		logViewerSize.setHidden(true);
		this.addInput(logViewerSize);

		eventViewerPos = new IntegerListInput("EventViewerPos", GUI, def);
		eventViewerPos.setValidCount(2);
		eventViewerPos.setValidRange(-8192, 8192);
		eventViewerPos.setPromptReqd(false);
		eventViewerPos.setHidden(true);
		this.addInput(eventViewerPos);

		eventViewerSize = new IntegerListInput("EventViewerSize", GUI, def);
		eventViewerSize.setValidCount(2);
		eventViewerSize.setValidRange(1, 8192);
		eventViewerSize.setPromptReqd(false);
		eventViewerSize.setHidden(true);
		this.addInput(eventViewerSize);

		controlPanelWidth = new IntegerInput("ControlPanelWidth", GUI, Integer.valueOf(1));
		controlPanelWidth.setValidRange(1, 8192);
		controlPanelWidth.setPromptReqd(false);
		controlPanelWidth.setHidden(true);
		this.addInput(controlPanelWidth);

		// Hidden keywords
		startTimeInput = new ValueInput("StartTime", KEY_INPUTS, 0.0d);
		startTimeInput.setUnitType(TimeUnit.class);
		startTimeInput.setValidRange(0.0d, Double.POSITIVE_INFINITY);
		startTimeInput.setHidden(true);
		this.addInput(startTimeInput);

		printInputReport = new BooleanInput("PrintInputReport", KEY_INPUTS, false);
		printInputReport.setHidden(true);
		this.addInput(printInputReport);
	}

	public Simulation() {
		if (getJaamSimModel().getSimulation() != null)
			throw new ErrorException("Cannot Define a second Simulation object");
		getJaamSimModel().setSimulation(this);
	}

	static final InputCallback calendarCallback = new InputCallback() {
		@Override
		public void callback(Entity ent, Input<?> inp) {
			Simulation sim = (Simulation)ent;

			sim.getJaamSimModel().setCalendar(sim.isGregorianCalendar(), sim.getStartDate());
		}
	};

	static final InputCallback reportDirectoryCallback = new InputCallback() {
		@Override
		public void callback(Entity ent, Input<?> inp) {
			Simulation sim = (Simulation)ent;
			DirInput dirinp = (DirInput)inp;
			sim.getJaamSimModel().setReportDirectory(dirinp.getDir());
		}
	};

	static final InputCallback enableTracingCallback = new InputCallback() {
		@Override
		public void callback(Entity ent, Input<?> inp) {
			boolean bool = ((BooleanInput) inp).getValue();
			for (Entity e : ent.getJaamSimModel().getClonesOfIterator(Entity.class)) {
				e.enableTracing(bool);
			}
		}
	};

	static final InputCallback startingScenarioNumberCallback = new InputCallback() {
		@Override
		public void callback(Entity ent, Input<?> inp) {
			Simulation sim = (Simulation)ent;

			sim.getJaamSimModel().setScenarioNumber(sim.getStartingScenarioNumber());
		}
	};

	static final InputCallback displayedUnitsCallback = new InputCallback() {
		@Override
		public void callback(Entity ent, Input<?> inp) {
			((Simulation)ent).displayUnitsCallback();
		}
	};

	void displayUnitsCallback() {
		getJaamSimModel().setPreferredUnitList(displayedUnits.getValue());
	}

	static final InputCallback scenarioIndexDefinitionListCallback = new InputCallback() {
		@Override
		public void callback(Entity ent, Input<?> inp) {
			((Simulation)ent).scenarioIndexDefinitionListCallback();
		}
	};

	void scenarioIndexDefinitionListCallback() {
		getJaamSimModel().setScenarioIndexList();
		startingScenarioNumber.setRunIndexRangeList(getScenarioIndexDefinitionList());
		endingScenarioNumber.setRunIndexRangeList(getScenarioIndexDefinitionList());
	}

	static final InputCallback lockWindowsCallback = new InputCallback() {
		@Override
		public void callback(Entity ent, Input<?> inp) {
			boolean bool = (boolean) inp.getValue();
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					GUIListener gui = ent.getJaamSimModel().getGUIListener();
					if (gui != null) {
						gui.allowResizing(!bool);
					}
				}
			});
		}
	};

	@Override
	public void validate() {
		super.validate();

		if (getJaamSimModel().isReloadReqd())
			throw new InputErrorException("Inputs to GregorianCalendar or StartDate have changed "
					+ "AFTER the simulation calendar has been used to process another input.%n"
					+ "Re-open the model to process these inputs in the correct order.");

		double maxRunDuration = Long.MAX_VALUE*tickLengthInput.getValue();
		if (getRunDuration() > maxRunDuration) {
			throw new InputErrorException("RunDuration exceeds the maximum value of %g seconds.\n"
					+ "Received: %g seconds.\n"
					+ "The maximum value can be increased by increasing the TickLength input.\n"
					+ "Present value: %g seconds.",
					maxRunDuration, runDuration.getValue(), tickLengthInput.getValue());
		}
	}

	@Override
	public void earlyInit() {
		super.earlyInit();
		unitTypeList.reset();  // Delete an unnecessary input
	}

	/**
	 * Clears the Simulation prior to loading a new model
	 */
	public void clear() {

		// Reset all Simulation inputs to their default values
		for (Input<?> inp : getEditableInputs()) {
			InputAgent.applyArgs(this, inp.getKeyword());
		}
	}

	@Override
	public Entity getChild(String name) {
		return getJaamSimModel().getNamedEntity(name);
	}

	/**
	 * Returns whether a paused simulation can be resumed.
	 * @param simTicks - present simulation time in clock ticks
	 * @return true if the simulation can be resumed
	 */
	public boolean canResume(long simTicks) {
		double totalDur = getRunDuration() + getInitializationTime();
		return simTicks < this.getJaamSimModel().getEventManager().secondsToNearestTick(totalDur);
	}

	public int getSubstreamNumber() {
		if (globalSeedInput.isDefault())
			return getJaamSimModel().getReplicationNumber();
		return (int)globalSeedInput.getNextSample(this, 0.0);
	}

	public boolean getPrintReport() {
		return printReport.getValue();
	}

	public boolean traceEvents() {
		return traceEventsInput.getValue();
	}

	public boolean verifyEvents() {
		return verifyEventsInput.getValue();
	}

	public double getTickLength() {
		return tickLengthInput.getValue();
	}

	public double getPauseTime() {
		return pauseTime.getValue();
	}

	public String getPauseTimeString() {
		return pauseTime.getValueString();
	}

	/**
	 * Returns the start time of the run.
	 * @return - simulation time in seconds for the start of the run.
	 */
	public double getStartTime() {
		return startTimeInput.getValue();
	}

	/**
	 * Returns the end time of the run.
	 * @return - simulation time in seconds when the current run will stop.
	 */
	public double getEndTime() {
		return getStartTime() + getInitializationTime() + getRunDuration();
	}

	/**
	 * Returns the duration of the run (not including intialization)
	 */
	public double getRunDuration() {
		return runDuration.getNextSample(this, 0.0d);
	}

	/**
	 * Returns the duration of the initialization period
	 */
	public double getInitializationTime() {
		return initializationTime.getNextSample(this, 0.0d);
	}

	/**
	 * Returns whether a Gregorian calendar with leap years and leap seconds is used.
	 * @return true if the calendar is Gregorian
	 */
	public boolean isGregorianCalendar() {
		return gregorianCalendar.getValue();
	}

	/**
	 * Returns the calendar date corresponding to zero simulation time.
	 */
	public SimDate getStartDate() {
		return startDate.getValue();
	}

	/**
	 * Returns the decimal fraction of the present run that has been completed for the specified
	 * simulation time.
	 * @param simTime - present simulation time
	 * @return fraction completed
	 */
	public double getProgress(double simTime) {
		return (simTime - getStartTime()) / (getRunDuration() + getInitializationTime());
	}

	public int getRunOutputListSize() {
		return runOutputList.getListSize();
	}

	public ArrayList<String> getRunOutputHeaders() {
		ArrayList<String> ret = new ArrayList<>(runOutputList.getListSize());
		for (int i = 0; i < runOutputList.getListSize(); i++) {
			ret.add(runOutputList.getValue().get(i).toString());
		}
		return ret;
	}

	public ArrayList<String> getRunOutputStrings(double simTime) {
		ArrayList<String> ret = new ArrayList<>(runOutputList.getListSize());
		for (int i = 0; i < runOutputList.getListSize(); i++) {
			ret.add(runOutputList.getNextString(i, this, simTime));
		}
		return ret;
	}

	public ArrayList<Double> getRunOutputValues(double simTime) {
		ArrayList<Double> ret = new ArrayList<>(runOutputList.getListSize());
		for (int i = 0; i < runOutputList.getListSize(); i++) {
			ret.add(runOutputList.getNextValue(i, this, simTime));
		}
		return ret;
	}

	public ArrayList<String> getRunParameterHeaders() {
		ArrayList<String> ret = new ArrayList<>(runParameterList.getListSize());
		for (int i = 0; i < runParameterList.getListSize(); i++) {
			ret.add(runParameterList.getValue().get(i).toString());
		}
		return ret;
	}

	public ArrayList<String> getRunParameterStrings(double simTime) {
		ArrayList<String> ret = new ArrayList<>(runParameterList.getListSize());
		for (int i = 0; i < runParameterList.getListSize(); i++) {
			ret.add(runParameterList.getNextString(i, this, simTime));
		}
		return ret;
	}

	public int getMaxEntitiesToDisplay() {
		return maxEntitiesToDisplay.getValue();
	}

	@Override
	public boolean isEnableTracing() {
		return enableTracing.getValue();
	}

	public boolean isShowLabels() {
		return showLabels.getValue();
	}

	public boolean isShowSubModels() {
		return showSubModels.getValue();
	}

	public boolean isPresentationMode() {
		return presentationMode.getValue();
	}

	public boolean isLockWindows() {
		return lockWindows.getValue();
	}

	public boolean isShowReferences() {
		return showReferences.getValue();
	}

	public boolean isShowEntityFlow() {
		return showEntityFlow.getValue();
	}

	public double getIncrementSize() {
		if (incrementSize.isDefault())
			return snapGridSpacing.getValue();
		return incrementSize.getValue();
	}

	public boolean isSnapToGrid() {
		return snapToGrid.getValue();
	}

	public double getSnapGridSpacing() {
		return snapGridSpacing.getValue();
	}

	public String getSnapGridSpacingString() {
		if (snapGridSpacing.isDefault())
			return snapGridSpacing.getDefaultString(getJaamSimModel());
		return snapGridSpacing.getValueString();
	}

	public boolean getExitAtPauseCondition() {
		return exitAtPauseCondition.getValue();
	}

	public boolean isPauseConditionSet() {
		return !pauseConditionInput.isDefault();
	}

	public boolean isPauseConditionSatisfied(double simTime) {
		return !pauseConditionInput.isDefault() &&
				pauseConditionInput.getNextSample(this, simTime) != 0.0d;
	}

	public Vec3d getSnapGridPosition(Vec3d pos) {
		return getSnapGridPosition(pos, snapGridSpacing.getValue());
	}

	public Vec3d getSnapGridPosition(Vec3d newPos, Vec3d oldPos, boolean shift) {
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

	public boolean getExitAtStop() {
		return exitAtStop.getValue();
	}

	public boolean getPrintInputReport() {
		return printInputReport.getValue();
	}

	public boolean isRealTime() {
		return realTime.getValue();
	}

	public double getRealTimeFactor() {
		return realTimeFactor.getValue();
	}

	public void resetWindowPositionsAndSizes() {
		KeywordIndex[] kws = new KeywordIndex[15];
		kws[0] = InputAgent.formatArgs(modelBuilderPos.getKeyword());
		kws[1] = InputAgent.formatArgs(modelBuilderSize.getKeyword());
		kws[2] = InputAgent.formatArgs(objectSelectorPos.getKeyword());
		kws[3] = InputAgent.formatArgs(objectSelectorSize.getKeyword());
		kws[4] = InputAgent.formatArgs(inputEditorPos.getKeyword());
		kws[5] = InputAgent.formatArgs(inputEditorSize.getKeyword());
		kws[6] = InputAgent.formatArgs(outputViewerPos.getKeyword());
		kws[7] = InputAgent.formatArgs(outputViewerSize.getKeyword());
		kws[8] = InputAgent.formatArgs(propertyViewerPos.getKeyword());
		kws[9] = InputAgent.formatArgs(propertyViewerSize.getKeyword());
		kws[10] = InputAgent.formatArgs(logViewerPos.getKeyword());
		kws[11] = InputAgent.formatArgs(logViewerSize.getKeyword());
		kws[12] = InputAgent.formatArgs(eventViewerPos.getKeyword());
		kws[13] = InputAgent.formatArgs(eventViewerSize.getKeyword());
		kws[14] = InputAgent.formatArgs(controlPanelWidth.getKeyword());
		InputAgent.storeAndExecute(new KeywordCommand(this, kws));
	}

	public IntegerVector getModelBuilderPos() {
		return modelBuilderPos.getValue();
	}

	public IntegerVector getModelBuilderSize() {
		return modelBuilderSize.getValue();
	}

	public IntegerVector getObjectSelectorPos() {
		return objectSelectorPos.getValue();
	}

	public IntegerVector getObjectSelectorSize() {
		return objectSelectorSize.getValue();
	}

	public IntegerVector getInputEditorPos() {
		return inputEditorPos.getValue();
	}

	public IntegerVector getInputEditorSize() {
		return inputEditorSize.getValue();
	}

	public IntegerVector getOutputViewerPos() {
		return outputViewerPos.getValue();
	}

	public IntegerVector getOutputViewerSize() {
		return outputViewerSize.getValue();
	}

	public IntegerVector getPropertyViewerPos() {
		return propertyViewerPos.getValue();
	}

	public IntegerVector getPropertyViewerSize() {
		return propertyViewerSize.getValue();
	}

	public IntegerVector getLogViewerPos() {
		return logViewerPos.getValue();
	}

	public IntegerVector getLogViewerSize() {
		return logViewerSize.getValue();
	}

	public IntegerVector getEventViewerPos() {
		return eventViewerPos.getValue();
	}

	public IntegerVector getEventViewerSize() {
		return eventViewerSize.getValue();
	}

	public void setControlPanelWidth(int width) {
		if (controlPanelWidth.getValue() == width)
			return;
		KeywordIndex kw = InputAgent.formatIntegers(controlPanelWidth.getKeyword(), width);
		InputAgent.storeAndExecute(new KeywordCommand(this, kw));
	}

	public int getControlPanelWidth() {
		return controlPanelWidth.getValue();
	}

	public boolean isModelBuilderVisible() {
		return showModelBuilder.getValue();
	}

	public boolean isObjectSelectorVisible() {
		return showObjectSelector.getValue();
	}

	public boolean isInputEditorVisible() {
		return showInputEditor.getValue();
	}

	public boolean isOutputViewerVisible() {
		return showOutputViewer.getValue();
	}

	public boolean isPropertyViewerVisible() {
		return showPropertyViewer.getValue();
	}

	public boolean isLogViewerVisible() {
		return showLogViewer.getValue();
	}

	public boolean isEventViewerVisible() {
		return showEventViewer.getValue();
	}

	public void setModelBuilderDefaults(int x, int y, int width, int height) {
		modelBuilderPos.setDefaultValue(x, y);
		modelBuilderSize.setDefaultValue(width, height);
	}

	public void setObjectSelectorDefaults(int x, int y, int width, int height) {
		objectSelectorPos.setDefaultValue(x, y);
		objectSelectorSize.setDefaultValue(width, height);
	}

	public void setInputEditorDefaults(int x, int y, int width, int height) {
		inputEditorPos.setDefaultValue(x, y);
		inputEditorSize.setDefaultValue(width, height);
	}

	public void setOutputViewerDefaults(int x, int y, int width, int height) {
		outputViewerPos.setDefaultValue(x, y);
		outputViewerSize.setDefaultValue(width, height);
	}

	public void setPropertyViewerDefaults(int x, int y, int width, int height) {
		propertyViewerPos.setDefaultValue(x, y);
		propertyViewerSize.setDefaultValue(width, height);
	}

	public void setLogViewerDefaults(int x, int y, int width, int height) {
		logViewerPos.setDefaultValue(x, y);
		logViewerSize.setDefaultValue(width, height);
	}

	public void setEventViewerDefaults(int x, int y, int width, int height) {
		eventViewerPos.setDefaultValue(x, y);
		eventViewerSize.setDefaultValue(width, height);
	}

	public void setControlPanelWidthDefault(int width) {
		controlPanelWidth.setDefaultValue(width);
	}

	public int getNumberOfReplications() {
		return (int) numberOfReplications.getNextSample(this, 0.0d);
	}

	public int getNumberOfThreads() {
		if (isRealTime())
			return 1;
		return Math.min(numberOfThreads.getValue(), getNumberOfRuns());
	}

	public boolean getPrintReplications() {
		return printReplications.getValue();
	}

	public boolean getPrintConfidenceIntervals() {
		return printConfidenceIntervals.getValue();
	}

	public boolean getPrintRunLabels() {
		return printRunLabels.getValue();
	}

	public int getStartingScenarioNumber() {
		return (int) startingScenarioNumber.getNextSample(this, 0.0d);
	}

	public int getEndingScenarioNumber() {
		int ret = (int) endingScenarioNumber.getNextSample(this, 0.0d);
		return Math.max(ret, getStartingScenarioNumber());
	}

	public int getNumberOfScenarios() {
		return getEndingScenarioNumber() - getStartingScenarioNumber() + 1;
	}

	public int getNumberOfRuns() {
		return getNumberOfScenarios() * getNumberOfReplications();
	}

	public IntegerVector getScenarioIndexDefinitionList() {
		return scenarioIndexDefinitionList.getValue();
	}

	@Output(name = "SoftwareName",
	 description = "The licensed name for the simulation software.",
	  reportable = true,
	    sequence = 0)
	public String getSoftwareName(double simTime) {
		return AboutBox.softwareName;
	}

	@Output(name = "SoftwareVersion",
	 description = "The release number for the simulation software.",
	  reportable = true,
	    sequence = 1)
	public String getSoftwareVersion(double simTime) {
		return AboutBox.version;
	}

	@Output(name = "ConfigurationFile",
	 description = "The configuration file that has been loaded.",
	  reportable = true,
	    sequence = 2)
	public String getConfigFileName(double simTime) {
		if (getJaamSimModel().getConfigFile() != null)
			return getJaamSimModel().getConfigFile().getPath();

		return "";
	}

	@Output(name = "ScenarioNumber",
	 description = "The counter used to indentify an individual simulation scenario when multiple "
	             + "scenarios are being made.",
	    unitType = DimensionlessUnit.class,
	  reportable = true,
	    sequence = 3)
	public int getScenarioNumber(double simTime) {
		return getJaamSimModel().getScenarioNumber();
	}

	@Output(name = "ScenarioIndex",
	 description = "The list of scenario indices that correspond to the scenario number.",
	    unitType = DimensionlessUnit.class,
	  reportable = true,
	    sequence = 4)
	public IntegerVector getScenarioIndex(double simTime) {
		return getJaamSimModel().getScenarioIndexList();
	}

	@Output(name = "ReplicationNumber",
	 description = "The counter used to indentify an individual replication for the present "
	             + "scenario.",
	    unitType = DimensionlessUnit.class,
	  reportable = true,
	    sequence = 5)
	public int getReplicationNumber(double simTime) {
		return getJaamSimModel().getReplicationNumber();
	}

	@Output(name = "RunNumber",
	 description = "The counter used to indentify an individual simulation run when multiple runs "
	             + "are being made.",
	    unitType = DimensionlessUnit.class,
	  reportable = true,
	    sequence = 6)
	public int getRunNumber(double simTime) {
		return getJaamSimModel().getRunNumber();
	}

	@Output(name = "RunIndex",
	 description = "For backwards compatibility - same as the ScenarioIndex output.",
	    unitType = DimensionlessUnit.class,
	  reportable = true,
	    sequence = 7)
	public IntegerVector getRunIndex(double simTime) {
		return getJaamSimModel().getScenarioIndexList();
	}

	@Output(name = "PresentTimeAndDate",
	 description = "The present local time and date.",
	  reportable = true,
	    sequence = 8)
	public String getPresentTime(double simTime) {
		String timeStamp = new SimpleDateFormat("MMM dd, yyyy HH:mm").format(Calendar.getInstance().getTime());
		return timeStamp;
	}

	@Output(name = "InitializationDuration",
	 description = "The length of time the model was executed prior to the start of statistics "
	             + "collection.",
	    unitType = TimeUnit.class,
	  reportable = true,
	    sequence = 9)
	public double getInitializationDuration(double simTime) {
		return getInitializationTime();
	}

	@Output(name = "RunDuration",
	 description = "The length of time over which statistics were collected.",
	    unitType = TimeUnit.class,
	  reportable = true,
	    sequence = 10)
	public double getRunDuration(double simTime) {
		return getRunDuration();
	}

	@Output(name = "PresentSimulationTime",
	 description = "The value for the simulation clock at the present time.",
	    unitType = TimeUnit.class,
	  reportable = true,
	    sequence = 11)
	public double getPresentSimulationTime(double simTime) {
		return simTime;
	}

	@Output(name = "SimDate",
	 description = "The calendar date and time of day for the present simulation time expressed "
	             + "as an array of integer values in the format "
	             + "(YYYY, MM, DD, hh, mm, ss, milliseconds).",
	    unitType = DimensionlessUnit.class,
	    sequence = 12)
	public int[] getSimDate(double simTime) {
		long millis = getJaamSimModel().simTimeToCalendarMillis(simTime);
		return getJaamSimModel().getSimDate(millis).toArray();
	}

	@Output(name = "SimDayOfWeek",
	 description = "The calendar day of week (Sunday = 1, Monday = 2, ..., Saturday = 7) for the "
	             + "present simulation time.",
	    unitType = DimensionlessUnit.class,
	    sequence = 13)
	public int getSimDayOfWeek(double simTime) {
		long millis = getJaamSimModel().simTimeToCalendarMillis(simTime);
		return getJaamSimModel().getDayOfWeek(millis);
	}

	@Output(name = "PresentDate",
	 description = "The present local calendar date and time of day expressed "
	             + "as an array of integer values in the format "
	             + "(YYYY, MM, DD, hh, mm, ss, milliseconds).",
	    sequence = 14)
	public int[] getPresentDate(double simTime) {
		SimDate simDate = new SimDate(Calendar.getInstance());
		return simDate.toArray();
	}

	@Output(name = "PresentDayOfWeek",
	 description = "The calendar day of week (Sunday = 1, Monday = 2, ..., Saturday = 7) for the "
	             + "present local time.",
	    unitType = DimensionlessUnit.class,
	    sequence = 15)
	public int getPresentDayOfWeek(double simTime) {
		return Calendar.getInstance().get(Calendar.DAY_OF_WEEK);
	}

	@Output(name = "PresentTime",
	 description = "The present elapsed time since the epoch (1970-01-01 00:00:00).",
	    unitType = TimeUnit.class,
	    sequence = 16)
	public double getPresentMilliseconds(double simTime) {
		return Calendar.getInstance().getTimeInMillis()/1000.0d;
	}

}
