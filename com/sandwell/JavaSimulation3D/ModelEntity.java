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
package com.sandwell.JavaSimulation3D;

import com.sandwell.JavaSimulation.DoubleListInput;
import com.sandwell.JavaSimulation.BooleanVector;
import com.sandwell.JavaSimulation.DoubleInput;
import com.sandwell.JavaSimulation.DoubleVector;
import com.sandwell.JavaSimulation.BooleanListInput;
import com.sandwell.JavaSimulation.ErrorException;
import com.sandwell.JavaSimulation.FileEntity;
import com.sandwell.JavaSimulation.Input;
import com.sandwell.JavaSimulation.InputErrorException;
import com.sandwell.JavaSimulation.IntegerVector;
import com.sandwell.JavaSimulation.ProbabilityDistribution;
import com.sandwell.JavaSimulation.StringVector;
import com.sandwell.JavaSimulation.Tester;
import static com.sandwell.JavaSimulation.Util.*;
import com.sandwell.JavaSimulation.Vector;
import com.sandwell.JavaSimulation3D.util.Shape;

import java.util.ArrayList;
import java.util.HashMap;

import javax.media.j3d.ColoringAttributes;

/**
 * Class ModelEntity - JavaSimulation3D
 */
public class ModelEntity extends DisplayEntity {

	// Breakdowns
	protected double availability;           // Avail. factor for entity = (op. time) / (breakdown + op time)
	protected double hoursForNextFailure;    // The number of working hours required before the next breakdown
	protected double iATFailure;             // inter arrival time between failures
	protected boolean breakdownPending;          // true when a breakdown is to occur
	protected boolean brokendown;                // true => entity is presently broken down
	protected boolean maintenance;               // true => entity is presently in maintenance
	protected boolean associatedBreakdown;       // true => entity is presently in Associated Breakdown
	protected boolean associatedMaintenance;     // true => entity is presently in Associated Maintenance
	protected double breakdownStartTime;         // Start time of the most recent breakdown
	protected double breakdownEndTime;           // End time of the most recent breakdown
	protected Vector sharedBreakdownList; // List of all equipment that must share the same breakdowns.
	protected Vector checkBreakdownList;  // List of all equipment that must be checked at the end of a breakdown or maintenance,
										  // to see if the equipment is in breakdown or maintenance.  If it is, then the entity
										  // must go into associated breakdown state

	// Breakdown Probability Distributions
	protected ProbabilityDistribution downtimeDurationDistribution;  // probability distribution governing duration of breakdowns
	protected ProbabilityDistribution downtimeIATDistribution;		// probability distribution governing time when breakdowns occur

	// Maintenance
	protected DoubleListInput firstMaintenanceTimes; // Clock time for start of first maintenance.
	protected DoubleListInput maintenanceIntervals;  // Elapsed clock time between one maintenance and the next.
	protected DoubleListInput maintenanceDurations;  // Duration of maintenance
	protected IntegerVector maintenancePendings;  // Number of maintenance periods that are due
	protected BooleanListInput skipMaintenanceIfOverlap; // Flag set if corresponding maintenance is skipped if it
												      // tries to start during another maintenance
	protected Vector sharedMaintenanceList; // List of all entities that must share the same maintenance.
	protected ModelEntity masterMaintenanceEntity;  // The entity that has maintenance information

	protected boolean performMaintenanceAfterCompletingShip;	// handler should go into maintenance after shipDelay for ShipLoader and ShipUnloader
	protected boolean performMaintenanceAfterShipDelayPending;			// maintenance needs to be done after shipDelay


	protected DoubleVector maintenanceOperatingHoursIntervals;  // Elapsed clock time between one maintenance and the next.
	protected DoubleVector maintenanceOperatingHoursDurations;  // Duration of maintenance
	protected IntegerVector maintenanceOperatingHoursPendings;  // Number of maintenance periods that are due
	protected DoubleVector hoursForNextMaintenanceOperatingHours;
	protected double maintenanceStartTime; // Start time of the most recent maintenance
	protected double maintenanceEndTime; // End time of the most recent maintenance
	protected DoubleVector lastScheduledMaintenanceTimes;
	protected DoubleVector deferMaintenanceLimit;                    // Amount of time after scheduled maintenance start time that deferMaintenanceLookahead is ignored

	protected final DoubleInput downtimeToReleaseEquipment;  // if duration of downtime is longer than this limit, equipment will be released
	protected final BooleanListInput releaseEquipment;       // Flag to indicate if routes/tasks are released before performing the maintenance
	protected final BooleanListInput forceMaintenance;       // Flag to indicate if maintenance schedule is performed no matter what the entity is doing

	// Force Maintenance
	protected DoubleVector firstForcedMaintenanceTimes; // Clock time for start of first maintenance.
	protected DoubleVector forcedMaintenanceIntervals;  // Elapsed clock time between one maintenance and the next.
	protected DoubleVector forcedMaintenanceDurations;  // Duration of maintenance
	protected IntegerVector forcedMaintenancePendings;  // Number of maintenance periods that are due
	protected IntegerVector skipForcedMaintenanceIfOverlap; // Flag set if corresponding maintenance is skipped if it
												      // tries to start during another maintenance
	protected Vector sharedForcedMaintenanceList; // List of all entities that must share the same maintenance.
	protected ModelEntity masterForcedMaintenanceEntity;  // The entity that has maintenance information

	protected DoubleVector forcedMaintenanceOperatingHoursIntervals;  // Elapsed clock time between one maintenance and the next.
	protected DoubleVector forcedMaintenanceOperatingHoursDurations;  // Duration of maintenance
	protected IntegerVector forcedMaintenanceOperatingHoursPendings;  // Number of maintenance periods that are due
	protected DoubleVector hoursForNextForcedMaintenanceOperatingHours;
	protected double forcedMaintenanceStartTime; // Start time of the most recent maintenance
	protected double forcedMaintenanceEndTime; // End time of the most recent maintenance
	protected boolean forcedMaintenancePending;          // true when a breakdown is to occur


	protected boolean forcedMaintenance;               // true => entity is presently in maintenance


	// Statistics
	protected boolean printToReport;

	// States
	protected static Vector stateList = new Vector( 11, 1 ); // List of valid states
	protected HashMap<String, Integer> stateMap; // map of states and state list index
	protected HashMap<String, Integer> workingStateMap; // map of working states and working state list index
	protected double workingHours;                    // Accumulated working time spent in working states
	protected static StringVector workingStateList;   // A List of states that are considered for working hours
	protected DoubleVector hoursPerState;             // Elapsed time to date for each state
	protected double lastHistogramUpdateTime;   // Last time at which a histogram was updated for this entity
	protected double secondToLastHistogramUpdateTime;   // Second to last time at which a histogram was updated for this entity
	protected DoubleVector lastStartTimePerState;     // Last time at which the state changed from some other state to each state
	protected DoubleVector secondToLastStartTimePerState;     // The second to last time at which the state changed from some other state to each state
	protected String presentState;                    // The present state of the entity
	protected int presentStateIndex;				  // The index of present state in the statelist
	protected double timeOfLastStateChange;           // Time at which the state was last changed
	protected FileEntity stateReportFile;        // The file to store the state information
	private String finalLastState = "";        // The final state of the entity (in a sequence of transitional states)
	private double timeOfLastPrintedState = 0; // The time that the last state printed in the trace state file

	// Graphics
	protected static ColoringAttributes breakdownColor; // Color of the entity in breaking down
	protected static ColoringAttributes maintenanceColor; // Color of the entity in maintenance
	protected static ColoringAttributes forcedMaintenanceColor; // Color of the entity in force maintenance

	static {
		stateList.addElement( "Idle" );
		stateList.addElement( "Working" );
		stateList.addElement( "Breakdown" );
		stateList.addElement( "Maintenance" );
	}

	{
		downtimeToReleaseEquipment = new DoubleInput("DowntimeToReleaseEquipment", "Maintenance and Breakdown", 0.0d, 0.0d, Double.POSITIVE_INFINITY);
		this.addInput(downtimeToReleaseEquipment, true);

		releaseEquipment = new BooleanListInput("ReleaseEquipment", "Maintenance and Breakdown", null);
		this.addInput(releaseEquipment, true);

		forceMaintenance = new BooleanListInput("ForceMaintenance", "Maintenance and Breakdown", null);
		this.addInput(forceMaintenance, true);

		addEditableKeyword( "PerformMaintenanceAfterCompletingShip",     "  (h)  "   ,   "  False  ", false, "Maintenance and Breakdown" );

	    addEditableKeyword( "Reliability",                      "  -  ",   "  1.0  ", false, "Maintenance and Breakdown" );
	    addEditableKeyword( "BreakdownDurations",                     "  -  ",   "  -  ", false, "Maintenance and Breakdown" );
	    addEditableKeyword( "BreakdownDuration",                     "  -  ",   "  -  ", false, "Maintenance and Breakdown" );
	    addEditableKeyword( "BreakdownProportions",                     "  -  ",   "  -  ", false, "Maintenance and Breakdown" );
		addEditableKeyword( "BreakdownSeed",            "  -  "     ,   "  0  ", false, "Maintenance and Breakdown" );
		addEditableKeyword( "DowntimeDurationDistribution",      	"  (hours)  "  ,   " - ", false, "Maintenance and Breakdown" );
		addEditableKeyword( "DowntimeIATDistribution",      	"  (hours)  "  ,   " - ", false, "Maintenance and Breakdown" );
		addEditableKeyword( "SharedBreakdowns",             "  -  ",   "  -  ", false, "Maintenance and Breakdown" );

		firstMaintenanceTimes = new DoubleListInput( "FirstMaintenanceTimes", "Maintenance and Breakdown", new DoubleVector() );
		firstMaintenanceTimes.setValidRange( 0.0d, Double.POSITIVE_INFINITY );
		firstMaintenanceTimes.setUnits( "h" );
		this.addInput( firstMaintenanceTimes, true, "FirstMaintenanceTime" );

		maintenanceDurations = new DoubleListInput( "MaintenanceDurations", "Maintenance and Breakdown", new DoubleVector() );
		maintenanceDurations.setValidRange( 0.0d, Double.POSITIVE_INFINITY );
		maintenanceDurations.setUnits( "h" );
		this.addInput( maintenanceDurations, true, "MaintenanceDuration" );

		maintenanceIntervals = new DoubleListInput( "MaintenanceIntervals", "Maintenance and Breakdown", new DoubleVector() );
		maintenanceIntervals.setValidRange( 0.0d, Double.POSITIVE_INFINITY );
		maintenanceIntervals.setUnits( "h" );
		this.addInput( maintenanceIntervals, true, "MaintenanceInterval" );

		skipMaintenanceIfOverlap = new BooleanListInput( "SkipMaintenanceIfOverlap", "Maintenance and Breakdown", new BooleanVector() );
		this.addInput( skipMaintenanceIfOverlap, true );

	    addEditableKeyword( "SharedMaintenance",             "  -  ",   "  -  ", false, "Maintenance and Breakdown" );

	    addEditableKeyword( "FirstForcedMaintenanceTimes",                     "  -  ",   "  -  ", false, "Maintenance and Breakdown", "FirstForcedMaintenanceTime"  );
	    addEditableKeyword( "ForcedMaintenanceDurations",                     "  -  ",   "  -  ", false, "Maintenance and Breakdown",  "ForcedMaintenanceDuration"  );
	    addEditableKeyword( "ForcedMaintenanceIntervals",                     "  -  ",   "  -  ", false, "Maintenance and Breakdown", "ForcedMaintenanceInterval"  );
	    addEditableKeyword( "SkipForcedMaintenanceIfOverlap", "  -  "     ,   "  0  ", false, "Maintenance and Breakdown" );

		addEditableKeyword( "MaintenanceOperatingHoursDurations",	        "", "",           false, "Maintenance and Breakdown", "MaintenanceOperatingHoursDuration" );
		addEditableKeyword( "MaintenanceOperatingHoursIntervals",	        "", "",           false, "Maintenance and Breakdown", "MaintenanceOperatingHoursInterval" );

		addEditableKeyword( "PrintToReport",        "",         "TRUE",  false, "Reporting (Optional)" );
	}

	public ModelEntity() {
		presentState = "Idle";
		presentStateIndex = 0;
		hoursPerState = new DoubleVector();
		lastHistogramUpdateTime = 0.0;
		secondToLastHistogramUpdateTime = 0.0;
		lastStartTimePerState = new DoubleVector();
		secondToLastStartTimePerState = new DoubleVector();
		presentState = "";
		timeOfLastStateChange = 0.0;
		availability = 1.0; // Default availability is 100%
		hoursForNextFailure = 0.0;
		iATFailure = 0.0;

		maintenancePendings = new IntegerVector( 1, 1 );
		sharedMaintenanceList = new Vector( 1, 1 );

		maintenanceOperatingHoursIntervals = new DoubleVector( 1, 1 );
		maintenanceOperatingHoursDurations = new DoubleVector( 1, 1 );
		maintenanceOperatingHoursPendings = new IntegerVector( 1, 1 );
		hoursForNextMaintenanceOperatingHours = new DoubleVector( 1, 1 );

		performMaintenanceAfterCompletingShip = false;
		performMaintenanceAfterShipDelayPending = false;
		lastScheduledMaintenanceTimes = new DoubleVector();
		deferMaintenanceLimit = new DoubleVector();

		// force maintenance
		firstForcedMaintenanceTimes = new DoubleVector( 1, 1 );
		forcedMaintenanceIntervals = new DoubleVector( 1, 1 );
		forcedMaintenanceDurations = new DoubleVector( 1, 1 );
		forcedMaintenancePendings = new IntegerVector( 1, 1 );
		skipForcedMaintenanceIfOverlap = new IntegerVector( 1, 1 );
		sharedForcedMaintenanceList = new Vector( 1, 1 );

		forcedMaintenanceOperatingHoursIntervals = new DoubleVector( 1, 1 );
		forcedMaintenanceOperatingHoursDurations = new DoubleVector( 1, 1 );
		forcedMaintenanceOperatingHoursPendings = new IntegerVector( 1, 1 );
		hoursForNextForcedMaintenanceOperatingHours = new DoubleVector( 1, 1 );
		forcedMaintenanceStartTime = 0.0;
		forcedMaintenanceEndTime = 0.0;
		forcedMaintenance = false;
		forcedMaintenancePending = false;


		breakdownStartTime = 0.0;
		breakdownEndTime = 0.0;
		breakdownPending = false;
		brokendown = false;
		associatedBreakdown = false;
		sharedBreakdownList = new Vector( 1, 1 );
		checkBreakdownList = new Vector( 1, 1 );
		maintenanceStartTime = 0.0;
		maintenanceEndTime = 0.0;
		maintenance = false;
		associatedMaintenance = false;
		printToReport = true;
		//setName( "" );
		downtimeDurationDistribution = null;
		downtimeIATDistribution = null;
		workingHours = 0.0;
		workingStateList = new StringVector( 2, 1 );
		stateMap = new HashMap<String, Integer>();
		workingStateMap = new HashMap<String, Integer>();
		timeOfLastStateChange = 0.0;
		maintenanceColor = Shape.getPresetColor(Shape.COLOR_RED);
		forcedMaintenanceColor = Shape.defineColor(0.6f, 0.0f, 0.0f);
		breakdownColor = Shape.defineColor(0.8f, 0.0f, 0.0f);

		// Populate the hash map for the states and state list indices
		for( int i = 0; i < getStateList().size(); i++ ) {
			stateMap.put( ((String)getStateList().get(i)).toLowerCase(), Integer.valueOf(i));
		}
		for( int i = 0; i < getWorkingStateList().size(); i++ ) {
			workingStateMap.put( getWorkingStateList().get(i).toLowerCase(), Integer.valueOf(i));
		}
	}

	/**
	 * Clear memory variables
	 */
	public void clear() {
		super.clearModel();
		hoursPerState.clear();
		presentState = "";
		timeOfLastStateChange = 0.0;
		availability = 1.0;
		hoursForNextFailure = 0.0;
		hoursForNextMaintenanceOperatingHours.clear();
		iATFailure = 0.0;
		maintenancePendings.clear();
		maintenanceOperatingHoursIntervals.clear();
		maintenanceOperatingHoursDurations.clear();
		maintenanceOperatingHoursPendings.clear();
		breakdownStartTime = 0.0;
		breakdownEndTime = 0.0;
		brokendown = false;
		firstForcedMaintenanceTimes.clear();
		forcedMaintenanceIntervals.clear();
		forcedMaintenanceDurations.clear();
		forcedMaintenancePendings.clear();
		forcedMaintenanceOperatingHoursIntervals.clear();
		forcedMaintenanceOperatingHoursDurations.clear();
		forcedMaintenanceOperatingHoursPendings.clear();
		skipForcedMaintenanceIfOverlap.clear();
		printToReport = true;
	}

	/**
	 * Clear internal properties
	 */
	public void clearInternalProperties() {
		presentStateIndex = 0;
		hoursPerState.clear();
		presentState = "";
		timeOfLastStateChange = 0.0;
		hoursForNextFailure = 0.0;
		performMaintenanceAfterShipDelayPending = false;;
		forcedMaintenance = false;
		forcedMaintenancePending = false;
		breakdownPending = false;
		brokendown = false;
		associatedBreakdown = false;
		maintenance = false;
		associatedMaintenance = false;
		workingHours = 0.0;
	}

	// ******************************************************************************************************
	// INPUT
	// ******************************************************************************************************

	public void validateMaintenance() {
		Input.validateIndexedLists(firstMaintenanceTimes.getValue(), maintenanceIntervals.getValue(), "FirstMaintenanceTimes", "MaintenanceIntervals");
		Input.validateIndexedLists(firstMaintenanceTimes.getValue(), maintenanceDurations.getValue(), "FirstMaintenanceTimes", "MaintenanceDurations");
	}

	public void validate()
	throws InputErrorException {
		super.validate();
		this.validateMaintenance();

		if( skipMaintenanceIfOverlap.getValue().size() > 0 )
			Input.validateIndexedLists(firstMaintenanceTimes.getValue(), skipMaintenanceIfOverlap.getValue(), "FirstMaintenanceTimes", "SkipMaintenanceIfOverlap");

		if( releaseEquipment.getValue() != null )
			Input.validateIndexedLists(firstMaintenanceTimes.getValue(), releaseEquipment.getValue(), "FirstMaintenanceTimes", "ReleaseEquipment");

		if( forceMaintenance.getValue() != null ) {
			Input.validateIndexedLists(firstMaintenanceTimes.getValue(), forceMaintenance.getValue(), "FirstMaintenanceTimes", "ForceMaintenance");
		}

	}

	/**
	 * Processes the input data corresponding to the specified keyword. If syntaxOnly is true,
	 * checks input syntax only; otherwise, checks input syntax and process the input values.
	 */
	public void readData_ForKeyword(StringVector data, String keyword, boolean syntaxOnly, boolean isCfgInput)
	throws InputErrorException {

		if ("MAINTENANCEOPERATINGHOURSINTERVALS".equalsIgnoreCase(keyword) ||
		    "MAINTENANCEOPEARINGHOURSINTERVAL".equalsIgnoreCase(keyword)) {
			maintenanceOperatingHoursIntervals = Input.parseDoubleVector(data, 1e-15, Double.POSITIVE_INFINITY);
			return;
		}
		if ("MAINTENANCEOPERATINGHOURSDURATIONS".equalsIgnoreCase(keyword) ||
		    "MAINTENANCEOPERATINGHOURSDURATION".equalsIgnoreCase(keyword)) {
			maintenanceOperatingHoursDurations = Input.parseDoubleVector(data, 1e-15, Double.POSITIVE_INFINITY);
			return;
		}
		if ("PERFORMMAINTENANCEAFTERCOMPLETINGSHIP".equalsIgnoreCase(keyword)) {
			Input.assertCount(data, 1);
			performMaintenanceAfterCompletingShip = Input.parseBoolean(data.get(0));
			return;
		}
		if ("SHAREDMAINTENANCE".equalsIgnoreCase(keyword)) {
			ArrayList<ModelEntity> list = Input.parseEntityList(data, ModelEntity.class, true);
			sharedMaintenanceList.clear();
			sharedMaintenanceList.addAll(list);

			for (ModelEntity each : list)
				each.setMasterMaintenanceBlock(this);
			return;
		}
		if ("AVAILABILITY".equalsIgnoreCase(keyword) ||
		    "RELIABILITY".equalsIgnoreCase(keyword)) {
			Input.assertCount(data, 1);
			availability = Input.parseDouble(data.get(0), 0.0d, 1.0d);
			return;
		}
		if ("FIRSTFORCEDMAINTENANCETIMES".equalsIgnoreCase(keyword) ||
		    "FIRSTFORCEDMAINTENANCETIME".equalsIgnoreCase(keyword)) {
			firstForcedMaintenanceTimes = Input.parseDoubleVector(data, 0.0d, Double.POSITIVE_INFINITY);
			return;
		}
		if ("FORCEDMAINTENANCEINTERVALS".equalsIgnoreCase(keyword) ||
		    "FORCEDMAINTENANCEINTERVAL".equalsIgnoreCase(keyword)) {
			forcedMaintenanceIntervals = Input.parseDoubleVector(data, 1e-15, Double.POSITIVE_INFINITY);
			return;
		}
		if ("FORCEDMAINTENANCEDURATIONS".equalsIgnoreCase(keyword) ||
		    "FORCEDMAINTENANCEDURATION".equalsIgnoreCase(keyword)) {
			forcedMaintenanceDurations = Input.parseDoubleVector(data, 1e-15, Double.POSITIVE_INFINITY);
			return;
		}
		if ("FORCEDMAINTENANCEOPERATINGHOURSINTERVALS".equalsIgnoreCase(keyword)){
			forcedMaintenanceOperatingHoursIntervals = Input.parseDoubleVector(data, 1e-15, Double.POSITIVE_INFINITY);
			return;
		}
		if ("FORCEDMAINTENANCEOPERATINGHOURSDURATIONS".equalsIgnoreCase(keyword) ||
		    "FORCEDMAINTENANCEOPERATINGHOURSDURATION".equalsIgnoreCase(keyword)) {
			forcedMaintenanceOperatingHoursDurations = Input.parseDoubleVector(data, 1e-15, Double.POSITIVE_INFINITY);
			return;
		}
		if ("SKIPFORCEDMAINTENANCEIFOVERLAP".equalsIgnoreCase(keyword)) {
			skipForcedMaintenanceIfOverlap = Input.parseIntegerVector(data, 0, Integer.MAX_VALUE);
			return;
		}
		if ("BreakdownColour".equalsIgnoreCase(keyword) ||
		    "BreakdownColor".equalsIgnoreCase(keyword)) {
			breakdownColor = Input.parseColour(data);
			return;
		}
		if ("MaintenanceColour".equalsIgnoreCase(keyword) ||
		    "MaintenanceColor".equalsIgnoreCase(keyword)) {
			maintenanceColor = Input.parseColour(data);
			return;
		}
		if ("PRINTTOREPORT".equalsIgnoreCase(keyword)) {
			Input.assertCount(data, 1);
			printToReport = Input.parseBoolean(data.get(0));
			return;
		}
		if ("DOWNTIMEDURATIONDISTRIBUTION".equalsIgnoreCase(keyword)) {
			Input.assertCount(data, 1);
			ProbabilityDistribution dist = Input.parseEntity(data.get(0), ProbabilityDistribution.class);
			if (dist.getMinimumValue() >= 0.0d)
				downtimeDurationDistribution = dist;
			else
				throw new InputErrorException("ProbabilityDistibution cannot allow negative values");
			return;
		}
		if ("DOWNTIMEIATDISTRIBUTION".equalsIgnoreCase(keyword)) {
			Input.assertCount(data, 1);
			ProbabilityDistribution dist = Input.parseEntity(data.get(0), ProbabilityDistribution.class);
			if (dist.getMinimumValue() >= 0.0d)
				downtimeIATDistribution = dist;
			else
				throw new InputErrorException("ProbabilityDistibution cannot allow negative values");
			return;
		}
		if ("STATELABEL".equalsIgnoreCase(keyword)) {
			InputAgent.logWarning("The keyword STATELABEL no longer has any effect");
			return;
		}
		if ("STATELABELBOTTOMLEFT".equalsIgnoreCase(keyword)) {
			InputAgent.logWarning("The keyword STATELABELBOTTOMLEFT no longer has any effect");
			return;
		}
		if ("STATELABELTEXTHEIGHT".equalsIgnoreCase(keyword)) {
			InputAgent.logWarning("The keyword STATELABELTEXTHEIGHT no longer has any effect");
			return;
		}
		if ("STATELABELFONTNAME".equalsIgnoreCase(keyword)) {
			InputAgent.logWarning("The keyword STATELABELFONTNAME no longer has any effect");
			return;
		}
		if ("STATELABELFONTSTYLE".equalsIgnoreCase(keyword)) {
			InputAgent.logWarning("The keyword STATELABELFONTSTYLE no longer has any effect");
			return;
		}
		if ("STATELABELFONTCOLOUR".equalsIgnoreCase(keyword) ||
		    "StateLabelFontColor".equalsIgnoreCase(keyword)) {
			InputAgent.logWarning("The keyword STATELABELFONTCOLOUR no longer has any effect");
			return;
		}
		super.readData_ForKeyword(data, keyword, syntaxOnly, isCfgInput);
	}

	// ******************************************************************************************************
	// INITIALIZATION METHODS
	// ******************************************************************************************************

	public void clearStatistics() {

		for( int i = 0; i < maintenanceOperatingHoursIntervals.size(); i++ ) {
			hoursForNextMaintenanceOperatingHours.set( i, hoursForNextMaintenanceOperatingHours.get( i ) - this.getWorkingHours() );
		}

		if( getStateList().size() != 0 ) {
			hoursPerState.fillWithEntriesOf( getStateList().size(), 0.0 );
		}
		else {
			hoursPerState.clear();
		}
		timeOfLastStateChange = getCurrentTime();

		// Determine the time for the first breakdown event
		/*if ( downtimeIATDistribution == null ) {
			if( breakdownSeed != 0 ) {
				breakdownRandGen.initialiseWith( breakdownSeed );
				hoursForNextFailure = breakdownRandGen.getUniformFrom_To( 0.5*iATFailure, 1.5*iATFailure );
			} else {
				hoursForNextFailure = getNextBreakdownIAT();
			}
		}
		 else {
			hoursForNextFailure = getNextBreakdownIAT();
		}*/
	}

	public void initializeStatistics() {
		hoursPerState.fillWithEntriesOf( getStateList().size(), 0.0 );
		timeOfLastStateChange = getCurrentTime();
	}

	/**
	 * *!*!*!*! OVERLOAD !*!*!*!*
	 * Initialize statistics
	 */
	public void initialize() {

		brokendown = false;
		maintenance = false;
		associatedBreakdown = false;
		associatedMaintenance = false;
		forcedMaintenance = false;

		// Create state trace file if required
		if (testFlag(FLAG_TRACESTATE)) {
			String fileName = InputAgent.getReportDirectory() +  InputAgent.getRunName() + "-" + this.getName() + ".trc";
			stateReportFile = new FileEntity( fileName, FileEntity.FILE_WRITE, false );
		}

		hoursPerState.fillWithEntriesOf( getStateList().size(), 0.0 );
		lastStartTimePerState.fillWithEntriesOf( getStateList().size(), 0.0 );
		secondToLastStartTimePerState.fillWithEntriesOf( getStateList().size(), 0.0 );
		timeOfLastStateChange = getCurrentTime();
		workingHours = 0.0;

		//  Calculate the average downtime duration if distributions are used
		double average = 0.0;
		if( downtimeDurationDistribution != null ) {
			downtimeDurationDistribution.initialize();
			average = downtimeDurationDistribution.getExpectedValue();
		}

		// Initialize downtime IAT distribution
		if( downtimeIATDistribution != null ) {
			downtimeIATDistribution.initialize();
		}

		//  Calculate the average downtime inter-arrival time
		if( (availability == 1.0 || average == 0.0) ) {
			iATFailure = 10.0E10;
		}
		else {
			if( downtimeIATDistribution != null ) {
				iATFailure = downtimeIATDistribution.getExpectedValue();

				// Adjust the downtime inter-arrival time to get the specified availability
				if( ! Tester.equalCheckTolerance( iATFailure, ( (average / (1.0 - availability)) - average ) ) ) {
					downtimeIATDistribution.setValueFactor_For( ( (average / (1.0 - availability)) - average) / iATFailure, this  );
					iATFailure = downtimeIATDistribution.getExpectedValue();
				}
			}
			else {
				iATFailure = ( (average / (1.0 - availability)) - average );
			}
		}

		// Determine the time for the first breakdown event
		if ( downtimeIATDistribution != null ) {
			hoursForNextFailure = getNextBreakdownIAT();
		}
		else {
			hoursForNextFailure = iATFailure;
		}

		int ind = this.indexOfState( "Idle" );
		if( ind != -1 ) {
			this.setPresentState( "Idle" );
		}
		brokendown = false;

		//  Start the maintenance network
		if( firstMaintenanceTimes.getValue().size() != 0 ) {
			maintenancePendings.fillWithEntriesOf( firstMaintenanceTimes.getValue().size(), 0 );
			lastScheduledMaintenanceTimes.fillWithEntriesOf( firstMaintenanceTimes.getValue().size(), Double.POSITIVE_INFINITY );

			if( deferMaintenanceLimit.size() == 0 ) {
				deferMaintenanceLimit.fillWithEntriesOf( firstMaintenanceTimes.getValue().size(), 0.0 );
			}

			this.doMaintenanceNetwork();
		}

		if( deferMaintenanceLimit.size() == 0 ) {
			deferMaintenanceLimit.fillWithEntriesOf( firstMaintenanceTimes.getValue().size(), 0.0 );
		}

		// The 3 force maintenance Double Vectors must be the same size
		if( (firstForcedMaintenanceTimes.size() != forcedMaintenanceDurations.size()) || (firstForcedMaintenanceTimes.size() != forcedMaintenanceIntervals.size()) ) {
			throw new ErrorException( " Force Maintenance variables should be the same dimension" );
		}

		// If skipMaintenanceIfOverlap was not defined, just create it with zero entries
		if( skipForcedMaintenanceIfOverlap.size() == 0 ) {
			for( int i=0; i < firstForcedMaintenanceTimes.size(); i++ ) {
				skipForcedMaintenanceIfOverlap.add(0);
			}
		}

		//  Start the force maintenance network
		if( firstForcedMaintenanceTimes.size() != 0 ) {
			forcedMaintenancePendings.fillWithEntriesOf( firstForcedMaintenanceTimes.size(), 0 );
			for( int i = 0; i < firstForcedMaintenanceTimes.size(); i++ ) {
				this.doForcedMaintenanceNetwork( i );
			}
		}

		// The maintenance Double Vectors must be the same size
		if( (maintenanceOperatingHoursDurations.size() != maintenanceOperatingHoursIntervals.size() ) ) {
			throw new ErrorException( " Maintenance variables should be the same dimension" );
		}

		// calculate hours for first operating hours breakdown
		for ( int i = 0; i < maintenanceOperatingHoursIntervals.size(); i++ ) {
			hoursForNextMaintenanceOperatingHours.add( maintenanceOperatingHoursIntervals.get( i ) );
			maintenanceOperatingHoursPendings.add( 0 );
		}
	}

	// ******************************************************************************************************
	// ACCESSOR METHODS
	// ******************************************************************************************************

	/**
	 * Return the time at which the most recent maintenance is scheduled to end
	 */
	public double getMaintenanceEndTime() {
		return maintenanceEndTime;
	}

	/**
	 * Return the time at which a the most recent breakdown is scheduled to end
	 */
	public double getBreakdownEndTime() {
		return breakdownEndTime;
	}

	public double getTimeOfLastStateChange() {
		return timeOfLastStateChange;
	}

	/**
	 * Returns the availability proportion.
	 */
	public double getAvailability() {
		return availability;
	}

	public DoubleListInput getFirstMaintenanceTimes() {
		return firstMaintenanceTimes;
	}

	public boolean getPrintToReport() {
		return printToReport;
	}

	public boolean isBrokendown() {
		return brokendown;
	}

	public boolean isBreakdownPending() {
		return breakdownPending;
	}

	public boolean isInAssociatedBreakdown() {
		return associatedBreakdown;
	}

	public boolean isInMaintenance() {
		return maintenance;
	}

	public boolean isInAssociatedMaintenance() {
		return associatedMaintenance;
	}

	public boolean isInService() {
		return ( brokendown || maintenance || associatedBreakdown || associatedMaintenance || forcedMaintenance );
	}

	public void setBrokendown( boolean bool ) {
		brokendown = bool;
		this.setPresentState();
		this.updateGraphics();
	}

	public void setAssociatedBreakdown( boolean bool ) {
		associatedBreakdown = bool;
	}

	public void setAssociatedMaintenance( boolean bool ) {
		associatedMaintenance = bool;
	}

	public ProbabilityDistribution getDowntimeDurationDistribution() {
		return downtimeDurationDistribution;
	}

	public double getDowntimeToReleaseEquipment() {
		return downtimeToReleaseEquipment.getValue();
	}

	public void setReliability( double val ) {
		availability = val;
	}

	public void setDowntimeDurationDistribution( ProbabilityDistribution val ) {
		downtimeDurationDistribution = val;
	}

	public void setDowntimeIATDistribution( ProbabilityDistribution val ) {
		downtimeIATDistribution = val;
	}

	public boolean hasMaintenanceDefined() {
		return( maintenanceDurations.getValue().size() > 0 || downtimeDurationDistribution != null );
	}

	// ******************************************************************************************************
	// HOURS AND STATES
	// ******************************************************************************************************

	/**
	 * Updates the statistics for the present status.
	 */
	public void updateHours() {

		if( presentState.length() == 0 ) {
			timeOfLastStateChange = getCurrentTime();
			return;
		}

		int index = this.indexOfState( presentState );
		if( index == -1 ) {
			throw new ErrorException( "Present state not found in StateList." );
		}
		if( presentStateIndex != index ) {
			throw new ErrorException( "presentStateIndex should be: " + index + ", but it is: " + presentStateIndex );
		}

		double dur = getCurrentTime() - timeOfLastStateChange;

		if( dur > 0.0 ) {
			/*int index = getStateList().indexOfString( presentState );
			if( index == -1 ) {
				throw new ErrorException( " Present state not found in StateList." );
			}

			hoursPerState.addAt( dur, index );*/
			if( !getStateList().get( presentStateIndex ).equals( presentState ) ) {
				throw new ErrorException( this + " Present state index ( " + presentStateIndex + " ) does not match present state ( " + presentState + " )." );
			}
			if( hoursPerState.size() <= presentStateIndex  ) {
				throw new ErrorException( this + " Present state index ( " + presentStateIndex + " ) does not exist in hoursPerState." );
			}

			hoursPerState.addAt( dur, presentStateIndex );
			timeOfLastStateChange = getCurrentTime();

			// Update working hours, if required
			//if( getWorkingStateList().contains( presentState ) ) {
			if( this.isWorking() ) {
				workingHours += dur;
			}
		}
	}

	/**
	 * Return true if the entity is working
	 */
	public boolean isWorking() {
			return workingStateMap.containsKey( presentState.toLowerCase() );
	}

	/**
	 * Returns the present status.
	 */
	public String getPresentState() {
		return presentState;
	}

	public void setPresentState() {}

	/**
	 * Updates the statistics, then sets the present status to be the specified value.
	 */
	public void setPresentState( String state ) {
		if( traceFlag ) this.trace("setState( "+state+" )");
		if( traceFlag ) this.traceLine(" Old State = "+presentState );

		if( ! presentState.equals( state ) ) {
			if (testFlag(FLAG_TRACESTATE)) this.printStateTrace(state);

			int ind = this.indexOfState( state );
			if( ind != -1 ) {
				this.updateHours();
				presentState = state;
				presentStateIndex = ind;
				if( lastStartTimePerState.size() > 0 ) {
					if( secondToLastStartTimePerState.size() > 0 ) {
						secondToLastStartTimePerState.set( ind, lastStartTimePerState.get( ind ) );
					}
					lastStartTimePerState.set( ind, getCurrentTime() );
				}
				this.updateGraphics();
			}
			else {
				throw new ErrorException( this + " Specified state: " + state + " was not found in the StateList: " + this.getStateList() );
			}
		}
	}

	/**
	 * Print that state information on the trace state log file
	 */
	public void printStateTrace( String state ) {

		// First state ever
		if( finalLastState == "" ) {
			finalLastState = state;
			stateReportFile.putString(String.format("%.5f  %s.setState( \"%s\" ) dt = %s\n",
									  0.0d, this.getName(), presentState, formatNumber(getCurrentTime())));
			stateReportFile.flush();
			timeOfLastPrintedState = getCurrentTime();
		}
		else {

			// The final state in a sequence from the previous state change (one step behind)
			if ( ! Tester.equalCheckTimeStep( timeOfLastPrintedState, getCurrentTime() ) ) {
				stateReportFile.putString(String.format("%.5f  %s.setState( \"%s\" ) dt = %s\n",
										  timeOfLastPrintedState, this.getName(), finalLastState, formatNumber(getCurrentTime() - timeOfLastPrintedState)));
//				for( int i = 0; i < stateTraceRelatedModelEntities.size(); i++ ) {
//					ModelEntitiy each = (ModelEntitiy) stateTraceRelatedModelEntities.get( i );
//					putString( )
//				}
				stateReportFile.flush();
				timeOfLastPrintedState = getCurrentTime();
			}
			finalLastState = state;
		}
	}

	/**
	 * Returns the amount of time spent in the specified status.
	 */
	public double getHoursForState( String state ) {

		int index = this.indexOfState( state );
		if( index != -1 ) {
			this.updateHours();
			return hoursPerState.get( index );
		}
		else {
			throw new ErrorException( "Specified state: " + state + " was not found in the StateList." );
		}
	}

	/**
	 * Set the last time a histogram was updated for this entity
	 */
	public void setLastHistogramUpdateTime( double time ) {
		secondToLastHistogramUpdateTime = lastHistogramUpdateTime;
		lastHistogramUpdateTime = time;
	}

	/**
	 * Returns the time from the start of the start state to the start of the end state
	 */
	public double getTimeFromStartState_ToEndState( String startState, String endState) {

		// Determine the index of the start state
		int startIndex = this.indexOfState( startState );
		if( startIndex == -1 ) {
			throw new ErrorException( "Specified state: " + startState + " was not found in the StateList." );
		}

		// Determine the index of the end state
		int endIndex = this.indexOfState( endState );
		if( endIndex == -1 ) {
			throw new ErrorException( "Specified state: " + endState + " was not found in the StateList." );
		}

		// Is the start time of the end state greater than the start time of the start state?
		if( lastStartTimePerState.get( endIndex ) > lastStartTimePerState.get( startIndex ) ) {

			// If either time was not in the present cycle, return 0
			if( lastStartTimePerState.get( endIndex ) <= lastHistogramUpdateTime ||
				lastStartTimePerState.get( startIndex ) <= lastHistogramUpdateTime ) {
				return 0.0;
			}
			// Return the time from the last start time of the start state to the last start time of the end state
			return lastStartTimePerState.get( endIndex ) - lastStartTimePerState.get( startIndex );
		}
		else {
			// If either time was not in the present cycle, return 0
			if( lastStartTimePerState.get( endIndex ) <= lastHistogramUpdateTime ||
				secondToLastStartTimePerState.get( startIndex ) <= secondToLastHistogramUpdateTime ) {
				return 0.0;
			}
			// Return the time from the second to last start time of the start date to the last start time of the end state
			return lastStartTimePerState.get( endIndex ) - secondToLastStartTimePerState.get( startIndex );
		}
	}

	/**
	 * Return the commitment
	 */
	public double getCommitment() {
		return 1.0 - this.getFractionOfTimeForState( "Idle" );
	}

	/**
	 * Return the fraction of time for the given status
	 */
	public double getFractionOfTimeForState( String aState ) {
		if( hoursPerState.sum() > 0.0 ) {
			return ((this.getHoursForState( aState ) / hoursPerState.sum()) );
		}
		else {
			return 0.0;
		}
	}

	/**
	 * Return the percentage of time for the given status
	 */
	public double getPercentageOfTimeForState( String aState ) {
		if( hoursPerState.sum() > 0.0 ) {
			return ((this.getHoursForState( aState ) / hoursPerState.sum()) * 100.0);
		}
		else {
			return 0.0;
		}
	}

	/**
	 * Returns the number of hours the entity is in use.
	 * *!*!*!*! OVERLOAD !*!*!*!*
	 */
	public double getWorkingHours() {
		this.updateHours();
		return workingHours;
	}

	public DoubleVector getHours() {
		this.updateHours();
		return hoursPerState;
	}

	public Vector getStateList() {
		return stateList;
	}

	public StringVector getWorkingStateList() {
		return workingStateList;
	}

	public int indexOfState( String state ) {
		Integer i = stateMap.get( state.toLowerCase() );
		if( i != null ) {
			return  i.intValue();
		}
		else {
			return -1;
		}
	}

	public DoubleVector getHoursPerState() {
		return hoursPerState;
	}

	// *******************************************************************************************************
	// MAINTENANCE METHODS
	// *******************************************************************************************************

	/**
	 * Perform tasks required before a maintenance period
	 */
	public void doPreMaintenance() {
	//@debug@ cr 'Entity should be overloaded' print
	}

	/**
	 * Start working again following a breakdown or maintenance period
	 */
	public void restart() {
	//@debug@ cr 'Entity should be overloaded' print
	}

	/**
	 * Disconnect routes, release truck assignments, etc. when performing maintenance or breakdown
	 */
	public void releaseEquipment() {}

	public boolean releaseEquipmentForMaintenanceSchedule( int index ) {

		if( releaseEquipment.getValue() == null )
			return true;

		return releaseEquipment.getValue().get( index );
	}

	public boolean forceMaintenanceSchedule( int index ) {

		if( forceMaintenance.getValue() == null )
			return false;

		return forceMaintenance.getValue().get( index );
	}

	/**
	 * Perform all maintenance schedules that are due
	 */
	public void doMaintenance() {

		// scheduled maintenance
		for( int index = 0; index < maintenancePendings.size(); index++ ) {
			if( this.getMaintenancePendings().get( index ) > 0 ) {
				if( traceFlag ) this.trace( "Starting Maintenance Schedule: " + index );
				this.doMaintenance(index);
			}
		}

		// Operating hours maintenance
		for( int index = 0; index < maintenanceOperatingHoursPendings.size(); index++ ) {
			if( this.getWorkingHours() > hoursForNextMaintenanceOperatingHours.get( index ) ) {
				hoursForNextMaintenanceOperatingHours.set(index, this.getWorkingHours() + maintenanceOperatingHoursIntervals.get( index ));
				maintenanceOperatingHoursPendings.addAt( 1, index );
				this.doMaintenanceOperatingHours(index);
			}
		}
	}

	/**
	 * Perform all the planned maintenance that is due for the given schedule
	 */
	public void doMaintenance( int index ) {

		double wait;
		if( masterMaintenanceEntity != null ) {
			wait = masterMaintenanceEntity.getMaintenanceDurations().getValue().get( index );
		}
		else {
			wait = this.getMaintenanceDurations().getValue().get( index );
		}

		if( wait > 0.0 && maintenancePendings.get( index ) != 0 ) {

			if( traceFlag ) this.trace( "ModelEntity.doMaintenance_Wait() -- start of maintenance" );

			// Keep track of the start and end of maintenance times
			maintenanceStartTime = getCurrentTime();

			if( masterMaintenanceEntity != null ) {
				maintenanceEndTime = maintenanceStartTime + ( maintenancePendings.get( index ) * masterMaintenanceEntity.getMaintenanceDurations().getValue().get( index ) );
			}
			else {
				maintenanceEndTime = maintenanceStartTime + ( maintenancePendings.get( index ) * maintenanceDurations.getValue().get( index ) );
			}

			this.setPresentState( "Maintenance" );
			maintenance = true;
			this.updateGraphics();
			this.doPreMaintenance();

			// Release equipment if necessary
			if( this.releaseEquipmentForMaintenanceSchedule( index ) ) {
				this.releaseEquipment();
			}

			while( maintenancePendings.get( index ) != 0 ) {
				maintenancePendings.subAt( 1, index );
				scheduleWait( wait );

				// If maintenance pending goes negative, something is wrong
				if( maintenancePendings.get( index ) < 0 ) {
					this.error( "ModelEntity.doMaintenance_Wait()", "Maintenace pending should not be negative", "maintenacePending = "+maintenancePendings.get( index ) );
				}
			}
			if( traceFlag ) this.trace( "ModelEntity.doMaintenance_Wait() -- end of maintenance" );

			//  The maintenance is over
			this.setPresentState( "Idle" );
			maintenance = false;
			this.updateGraphics();
			this.restart();
		}
	}

	/**
	 * Perform all the planned maintenance that is due
	 */
	public void doMaintenanceOperatingHours( int index ) {

		if( maintenanceOperatingHoursPendings.get( index ) != 0 ) {

			if( traceFlag ) this.trace( "ModelEntity.doMaintenance_Wait() -- start of maintenance" );

			// Keep track of the start and end of maintenance times
			maintenanceStartTime = getCurrentTime();
			maintenanceEndTime = maintenanceStartTime + ( maintenanceOperatingHoursPendings.get( index ) * maintenanceOperatingHoursDurations.get( index ) );

			this.setPresentState( "Maintenance" );
			maintenance = true;
			this.updateGraphics();
			this.doPreMaintenance();

			while( maintenanceOperatingHoursPendings.get( index ) != 0 ) {
				//scheduleWait( maintenanceDurations.get( index ) );
				scheduleWait( maintenanceEndTime - maintenanceStartTime );
				maintenanceOperatingHoursPendings.subAt( 1, index );

				// If maintenance pending goes negative, something is wrong
				if( maintenanceOperatingHoursPendings.get( index ) < 0 ) {
					this.error( "ModelEntity.doMaintenance_Wait()", "Maintenace pending should not be negative", "maintenacePending = "+maintenanceOperatingHoursPendings.get( index ) );
				}

			}
			if( traceFlag ) this.trace( "ModelEntity.doMaintenance_Wait() -- end of maintenance" );

			//  The maintenance is over
			maintenance = false;
			this.setPresentState( "Idle" );
			this.updateGraphics();
			this.restart();
		}
	}

	/**
	 * Check if a maintenance is due.  if so, try to perform the maintenance
	 */
	public boolean checkMaintenance() {
		if( traceFlag ) this.trace( "checkMaintenance()" );
		if( checkOperatingHoursMaintenance() ) {
			return true;
		}

		// List of all entities going to maintenance
		Vector sharedMaintenanceEntities;

		// This is not a master maintenance entity
		if( masterMaintenanceEntity != null ) {
			sharedMaintenanceEntities = masterMaintenanceEntity.sharedMaintenanceList;
		}

		// This is a master maintenance entity
		else {
			sharedMaintenanceEntities = sharedMaintenanceList;
		}

		// If this entity is in shared maintenance relation with a group of entities
		if( sharedMaintenanceEntities.size() > 0 || masterMaintenanceEntity != null ) {

			// Are all entities in the group ready for maintenance
			if( this.areAllEntitiesAvailable() ) {

				// For every entity in the shared maintenance list plus the master maintenance entity
				for( int i=0; i <= sharedMaintenanceEntities.size(); i++ ) {
					ModelEntity aModel;

					// Locate master maintenance entity( after all entity in shared maintenance list have been taken care of )
					if( i == sharedMaintenanceEntities.size() ) {

						// This entity is manster maintenance entity
						if( masterMaintenanceEntity == null ) {
							aModel = this;
						}

						// This entity is on the shared maintenannce list of the master maintenance entity
						else {
							aModel = masterMaintenanceEntity;
						}
					}

					// Next entity in the shared maintenance list
					else {
						aModel = (ModelEntity) sharedMaintenanceEntities.get( i );
					}

					// Check for aModel maintenances
					for( int index = 0; index < maintenancePendings.size(); index++ ) {
						if( aModel.getMaintenancePendings().get( index ) > 0 ) {
							if( traceFlag ) this.trace( "Starting Maintenance Schedule: " + index );
							aModel.startProcess("doMaintenance", index);
						}
					}
				}
				return true;
			}
			else {
				return false;
			}
		}

		// This block is maintained indipendently
		else {

			//  Check for maintenances
			for( int i = 0; i < maintenancePendings.size(); i++ ) {
				if( maintenancePendings.get( i ) > 0 ) {
					if( this.canStartMaintenance( i ) ) {
						if( traceFlag ) this.trace( "Starting Maintenance Schedule: " + i );
						this.startProcess("doMaintenance", i);
						return true;
					}
				}
			}
		}
		return false;
	}

	/**
	 * Determine how many hours of maintenance is scheduled between startTime and endTime
	 */
	public double getScheduledMaintenanceHoursForPeriod( double startTime, double endTime ) {
		if( traceFlag ) this.trace("Handler.getScheduledMaintenanceHoursForPeriod( "+startTime+", "+endTime+" )" );

		double totalHours = 0.0;
		double firstTime = 0.0;

		// Add on hours for all pending maintenance
		for( int i=0; i < maintenancePendings.size(); i++ ) {
			totalHours += maintenancePendings.get( i ) * maintenanceDurations.getValue().get( i );
		}

		if( traceFlag ) this.traceLine( "Hours of pending maintenances="+totalHours );

		// Add on hours for all maintenance scheduled to occur in the given period from startTime to endTime
		for( int i=0; i < maintenancePendings.size(); i++ ) {

			// Find the first time that maintenance is scheduled after startTime
			firstTime = firstMaintenanceTimes.getValue().get( i );
			while( firstTime < startTime ) {
				firstTime += maintenanceIntervals.getValue().get( i );
			}
			if( traceFlag ) this.traceLine(" first time maintenance "+i+" is scheduled after startTime= "+firstTime );

			// Now have the first maintenance start time after startTime
			// Add all maintenances that lie in the given interval
			while( firstTime < endTime ) {
				if( traceFlag ) this.traceLine(" Checking for maintenances for period:"+firstTime+" to "+endTime );
				// Add the maintenance
				totalHours += maintenanceDurations.getValue().get( i );

				// Update the search period
				endTime += maintenanceDurations.getValue().get( i );

				// Look for next maintenance in new interval
				firstTime += maintenanceIntervals.getValue().get( i );
				if( traceFlag ) this.traceLine(" Adding Maintenance duration = "+maintenanceDurations.getValue().get( i ) );
			}
		}

		// Return the total hours of maintenance scheduled from startTime to endTime
		if( traceFlag ) this.traceLine( "Maintenance hours to add= "+totalHours );
		return totalHours;
	}

	public boolean checkOperatingHoursMaintenance() {
		if( traceFlag ) this.trace("checkOperatingHoursMaintenance()");

		//  Check for maintenances
		for( int i = 0; i < maintenanceOperatingHoursIntervals.size(); i++ ) {

			// If the entity is not available, maintenance cannot start
			if( ! this.canStartMaintenance( i ) )
				continue;

			if( this.getWorkingHours() > hoursForNextMaintenanceOperatingHours.get( i ) ) {
				hoursForNextMaintenanceOperatingHours.set(i, (this.getWorkingHours() + maintenanceOperatingHoursIntervals.get( i )));
				maintenanceOperatingHoursPendings.addAt( 1, i );

				if( traceFlag ) this.trace( "Starting Maintenance Operating Hours Schedule : " + i );
				this.startProcess("doMaintenanceOperatingHours", i);
				return true;
			}
		}
		return false;
	}

	/**
	 * Wrapper method for doMaintenance_Wait.
	 */
	public void doMaintenanceNetwork() {
		this.startProcess("doMaintenanceNetwork_Wait");
	}

	/**
	 * Network for planned maintenance.
	 * This method should be called in the initialize method of the specific entity.
	 */
	public void doMaintenanceNetwork_Wait() {

		// Initialize schedules
		for( int i=0; i < maintenancePendings.size(); i++ ) {
			maintenancePendings.set( i, 0 );
		}
		DoubleVector nextMaintenanceTimes = new DoubleVector(firstMaintenanceTimes.getValue());

		// Make sure that maintenance for entities on the shared list are being called after those entities have been initialize (AT TIME ZERO)
		scheduleLastLIFO();
		while( true ) {

			// Find the next maintenance event
			int index = 0;
			double earliestTime = Double.POSITIVE_INFINITY;
			for( int i=0; i < nextMaintenanceTimes.size(); i++ ) {
				double time = nextMaintenanceTimes.get( i );
				if( Tester.lessCheckTolerance( time, earliestTime ) ) {
					earliestTime = time;
					index = i;
				}
			}

			double dt = earliestTime - getCurrentTime();

			// Wait for the maintenance check time
			if( dt > simulation.getEventTolerance() ) {
				scheduleWait( dt );
			}

			// Increment the number of maintenances due for the entity
			maintenancePendings.addAt( 1, index );

			// If this is a master maintenance entity
			if( sharedMaintenanceList.size() > 0 ) {

				// If all the entities on the shared list are ready for maintenance
				if( this.areAllEntitiesAvailable() ) {

					// Put this entity to maintenance
					if( traceFlag ) this.trace( "Starting Maintenance Schedule: " + index );
					this.startProcess("doMaintenance", index);
				}
			}

			// If this entity is maintained independently
			else {

				// Do maintenance if possible
				if( ! this.isInService() && this.canStartMaintenance( index ) ) {
					// if( traceFlag ) this.trace( "doMaintenanceNetwork_Wait: Starting Maintenance.  PresentState = "+presentState+" IsAvailable? = "+this.isAvailable() );
					if( traceFlag ) this.trace( "Starting Maintenance Schedule: " + index );
					this.startProcess("doMaintenance", index);
				}
				// Keep track of the time the maintenance was attempted
				else {
					lastScheduledMaintenanceTimes.set( index, getCurrentTime() );

					// If skipMaintenance was defined, cancel the maintenance
					if( this.shouldSkipMaintenance( index ) ) {

						// if a different maintenance is due, cancel this maintenance
						boolean cancelMaintenance = false;
						for( int i=0; i < maintenancePendings.size(); i++ ) {
							if( i != index ) {
								if( maintenancePendings.get( i ) > 0 ) {
									cancelMaintenance = true;
									break;
								}
							}
						}

						if( cancelMaintenance || this.isInMaintenance() ) {
							maintenancePendings.subAt( 1, index );
						}
					}

					// Do a check after the limit has expired
					if( deferMaintenanceLimit.get( index ) > 0.0 ) {
						this.startProcess( "scheduleCheckMaintenance", deferMaintenanceLimit.get( index ) );
					}
				}
			}

			// Determine the next maintenance time
			nextMaintenanceTimes.addAt( maintenanceIntervals.getValue().get( index ), index );
		}
	}

	public void scheduleCheckMaintenance( double wait ) {
		scheduleWait( wait );
		this.checkMaintenance();
	}

	public boolean shouldSkipMaintenance( int index ) {

		if( skipMaintenanceIfOverlap.getValue().size() == 0 )
			return false;

		return skipMaintenanceIfOverlap.getValue().get( index );
	}

	/**
	 * Return TRUE if there is a pending maintenance for any schedule
	 */
	public boolean isMaintenancePending() {
		for( int i = 0; i < maintenancePendings.size(); i++ ) {
			if( maintenancePendings.get( i ) > 0 ) {
				return true;
			}
		}

		for( int i = 0; i < hoursForNextMaintenanceOperatingHours.size(); i++ ) {
			if( this.getWorkingHours() > hoursForNextMaintenanceOperatingHours.get( i ) ) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Wrapper method for doForcedMaintenance_Wait.
	 */
	public void doForcedMaintenanceNetwork( int index ) {
		this.startProcess("doForcedMaintenanceNetwork_Wait", index);
	}

	/**
	 * Network for planned maintenance.
	 * This method should be called in the initialize method of the specific entity.
	 */
	public void doForcedMaintenanceNetwork_Wait( int index ) {

		// Check if the network should be executed
		if( ( forcedMaintenanceDurations.get( index ) == 0.0 || forcedMaintenanceIntervals.get( index ) == 0.0) ) {
			return;
		}
		forcedMaintenancePendings.set( index, 0 );

		// If this is a master maintenance entity
		if( sharedForcedMaintenanceList.size() > 0 ) {

			// For every entity in the shared maintenance list
//			for( int i=0; i < sharedMaintenanceList.size(); i++ ) {
//				ModelEntity aModel = (ModelEntity) sharedMaintenanceList.get( i );
//				aModel.getMaintenancePendings().fillWithEntriesOf( firstMaintenanceTimes.size(), 0 );
//				aModel.getMaintenancePendings().setElementAt( 0, index );
//			}
		}

		double dt = firstForcedMaintenanceTimes.get( index );

		// Make sure that maintenance for entities on the shared list are being called after those entities have been initialize (AT TIME ZERO)
		scheduleLastLIFO();
		while( true ) {

			// Wait for the maintenance check time
			if( dt > simulation.getEventTolerance() ) {
				scheduleWait( dt );
			}

			// If this is a master maintenance entity
			if( sharedForcedMaintenanceList.size() > 0 ) {

				// Increment the number of maintenances due for every entity in the list
//				for( int i=0; i < sharedMaintenanceList.size(); i++ ) {
//					ModelEntity aModel = (ModelEntity) sharedMaintenanceList.get( i );
//					aModel.getMaintenancePendings().addAt( 1, index );
//				}

				// Increment the number of maintenances due for the entity
				forcedMaintenancePendings.addAt( 1, index );

				// If all the entities on the shared list are ready for maintenance
				if( this.areAllEntitiesAvailable() ) {

					// Put the entities on shared maintenance list on maintenance
//					for( int i=0; i < sharedMaintenanceList.size(); i++ ) {
//						ModelEntity aModle = (ModelEntity) sharedMaintenanceList.get( i );
//						Object[] args = new Object[1];
//						args[0] = new Integer( index );
//						this.trace( "Starting Maintenance Schedule: " + index );
//						eventManager.startProcess( aModle, "doMaintenance", args );
//					}

					// Put this entity to maintenance
					this.trace( "Starting Force Maintenance Schedule: " + index );
					this.startProcess("doForcedMaintenance", index);

				}
			}

			// If this entity is maintained independently
			else {

				// Increment the number of maintenances due
				forcedMaintenancePendings.addAt( 1, index );

				forcedMaintenancePending = true;

				if(this.canStartForcedMaintenance()) {
					// this.trace( "doForcedMaintenanceNetwork_Wait: Starting Maintenance.  PresentState = "+presentState+" IsAvailable? = "+this.isAvailable() );
					this.trace( "Starting Force Maintenance Schedule: " + index );
					this.startProcess("doForcedMaintenance_Idle", index);
				}
			}

			// Determine the wait for the next force maintenance interval
			dt = forcedMaintenanceIntervals.get( index );
		}
	}



	/**
	 * Perform all the planned force maintenance that is due
	 */
	public void doForcedMaintenance_Idle( int index ) {
		// overwritten
	}

	public Vector getSharedMaintenanceList () {
		return sharedMaintenanceList;
	}

	public IntegerVector getMaintenancePendings () {
		return maintenancePendings;
	}

	public DoubleListInput getMaintenanceDurations() {
		return maintenanceDurations;
	}

	// Shows if an Entity would ever go on service
	public boolean hasServiceScheduled() {
		if( firstMaintenanceTimes.getValue().size() != 0 || masterMaintenanceEntity != null ) {
			return true;
		}
		return false;
	}
	public void setMasterMaintenanceBlock( ModelEntity aModel ) {
		masterMaintenanceEntity =  aModel;
	}

	// *******************************************************************************************************
	// BREAKDOWN METHODS
	// *******************************************************************************************************

	/**
	 * No Comments Given.
	 */
	public void calculateTimeOfNextFailure() {
		hoursForNextFailure = (this.getWorkingHours() + this.getNextBreakdownIAT());
	}

	/**
	 * Activity Network for Breakdowns.
	 */
	public void doBreakdown() {
	}

	/**
	 * Prints the header for the entity's state list.
	 * @return bottomLine contains format for each column of the bottom line of the group report
	 */
	public IntegerVector printUtilizationHeaderOn( FileEntity anOut ) {

		IntegerVector bottomLine = new IntegerVector();

		if( getStateList().size() != 0 ) {
			anOut.putStringTabs( "Name", 1 );
			bottomLine.add( ReportAgent.BLANK );

			int doLoop = getStateList().size();
			for( int x = 0; x < doLoop; x++ ) {
				String state = (String)getStateList().get( x );
				anOut.putStringTabs( state, 1 );
				bottomLine.add( ReportAgent.AVERAGE_PCT_ONE_DEC );
			}
			anOut.newLine();
		}
		return bottomLine;
	}

	/**
	 * Print the entity's name and percentage of hours spent in each state.
	 * @return columnValues are the values for each column in the group report (0 if the value is a String)
	 */
	public DoubleVector printUtilizationOn( FileEntity anOut ) {

		double total;
		DoubleVector columnValues = new DoubleVector();

		this.updateHours();
		if( hoursPerState.size() != 0 ) {
			total = hoursPerState.sum();
			if( !(total == 0.0) ) {
				this.updateHours();
				anOut.putStringTabs( getName(), 1 );
				columnValues.add( 0.0 );

				for( int i = 0; i < hoursPerState.size(); i++ ) {
					double value = hoursPerState.get( i ) / total;
					anOut.putDoublePercentWithDecimals( value, 1 );
					anOut.putTabs( 1 );
					columnValues.add( value );
				}
				anOut.newLine();
			}
		}
		return columnValues;
	}

	/**
	 * This method must be overridden in any subclass of ModelEntity.
	 */
	public boolean isAvailable() {
		throw new ErrorException( "Must override isAvailable in any subclass of ModelEntity." );
	}

	/**
	 * This method must be overridden in any subclass of ModelEntity.
	 */
	public boolean canStartMaintenance( int index ) {
		return isAvailable();
	}

	/**
	 * This method must be overridden in any subclass of ModelEntity.
	 */
	public boolean canStartForcedMaintenance() {
		return isAvailable();
	}

	/**
	 * This method must be overridden in any subclass of ModelEntity.
	 */
	public boolean areAllEntitiesAvailable() {
		throw new ErrorException( "Must override areAllEntitiesAvailable in any subclass of ModelEntity." );
	}

	/**
	 * Return the time of the next breakdown duration
	 */
	public double getBreakdownDuration() {
		// if( traceFlag ) this.trace( "getBreakdownDuration()" );

		//  If a distribution was specified, then select a duration randomly from the distribution
		if ( downtimeDurationDistribution != null ) {
			return downtimeDurationDistribution.nextValue();
		}
		else {
			return 0.0;
		}
	}

	/**
	 * Return the time of the next breakdown IAT
	 */
	public double getNextBreakdownIAT() {
		if( downtimeIATDistribution != null ) {
			return downtimeIATDistribution.nextValue();
		}
		else {
			return iATFailure;
		}
	}

	public double getHoursForNextFailure() {
		return hoursForNextFailure;
	}

	public void setHoursForNextFailure( double hours ) {
		hoursForNextFailure = hours;
	}

	/**
	 Returns a vector of strings describing the ModelEntity.
	 Override to add details
	 @return Vector - tab delimited strings describing the DisplayEntity
	 **/
	public Vector getInfo() {
		Vector info = super.getInfo();
		if( presentState.equals( "" ) || presentState == null )
			info.addElement( "Present State\t<no state>" );
		else
			info.addElement( "Present State" + "\t" + presentState );
		return info;
	}

}