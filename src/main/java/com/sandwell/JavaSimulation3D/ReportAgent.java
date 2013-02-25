/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2008-2011 Ausenco Engineering Canada Inc.
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

import java.util.ArrayList;

import com.jaamsim.input.InputAgent;
import com.sandwell.JavaSimulation.DoubleVector;
import com.sandwell.JavaSimulation.Group;
import com.sandwell.JavaSimulation.Input;
import com.sandwell.JavaSimulation.InputErrorException;
import com.sandwell.JavaSimulation.IntegerVector;
import com.sandwell.JavaSimulation.Simulation;
import com.sandwell.JavaSimulation.StringVector;
import com.sandwell.JavaSimulation.FileEntity;

public class ReportAgent extends DisplayEntity {
	private final ArrayList<Group> groupList;  // groups for reporting in the .grp file
	protected DoubleVector reportIntervals; // intervals for printing the .grp file
	protected double lastReportIntervalTime; // time of the last report printing
	protected String groupReportFileName;
	protected FileEntity groupReportFile;
	protected static boolean collectedInitializationStats; // flag to avoid re-entering collectInitializationStats() method

	// Constants for the bottom line information of the group report columns
	public static final int TOTAL_NO_DEC = 0;
	public static final int TOTAL_ONE_DEC = 1;
	public static final int TOTAL_TWO_DEC = 2;
	public static final int TOTAL_THREE_DEC = 3;
	public static final int TOTAL_FOUR_DEC = 4;
	public static final int TOTAL_FIVE_DEC = 5;
	public static final int AVERAGE_PCT_NO_DEC = 6;
	public static final int AVERAGE_PCT_ONE_DEC = 7;
	public static final int AVERAGE_PCT_TWO_DEC = 8;
	public static final int AVERAGE_PCT_THREE_DEC = 9;
	public static final int AVERAGE_PCT_FOUR_DEC = 10;
	public static final int AVERAGE_PCT_FIVE_DEC = 11;
	public static final int MINIMUM_NO_DEC = 12;
	public static final int MINIMUM_ONE_DEC = 13;
	public static final int MINIMUM_TWO_DEC = 14;
	public static final int MINIMUM_THREE_DEC = 15;
	public static final int MINIMUM_FOUR_DEC = 16;
	public static final int MINIMUM_FIVE_DEC = 17;
	public static final int MAXIMUM_NO_DEC = 18;
	public static final int MAXIMUM_ONE_DEC = 19;
	public static final int MAXIMUM_TWO_DEC = 20;
	public static final int MAXIMUM_THREE_DEC = 21;
	public static final int MAXIMUM_FOUR_DEC = 22;
	public static final int MAXIMUM_FIVE_DEC = 23;
	public static final int AVERAGE_NO_DEC = 24;
	public static final int AVERAGE_ONE_DEC = 25;
	public static final int AVERAGE_TWO_DEC = 26;
	public static final int AVERAGE_THREE_DEC = 27;
	public static final int AVERAGE_FOUR_DEC = 28;
	public static final int AVERAGE_FIVE_DEC = 29;
	public static final int BLANK = 30;

	{
		addEditableKeyword( "ReportDirectory",  "  -  ", "  -  ", false, "Key Inputs" );
		addEditableKeyword( "GroupList",        "  -  ", "  -  ", false, "Key Inputs" );
		addEditableKeyword( "ReportIntervals",  "hours", "  -  ", false, "Key Inputs" );
	}

	public ReportAgent() {
		groupList = new ArrayList<Group>();
		reportIntervals = new DoubleVector();
		lastReportIntervalTime = simulation.getInitializationTime();
		groupReportFileName = "";
		groupReportFile = null;
	}

	@Override
	public void earlyInit() {
		super.earlyInit();

		collectedInitializationStats = false;
	}

	/**
	 * Collect stats for each model entity at the end of initialization period
	 */
	protected void collectInitializationStats() {
		if (collectedInitializationStats)
			return;

		collectedInitializationStats = true;

		for ( ModelEntity each : Simulation.getClonesOf(ModelEntity.class) ) {
			each.collectInitializationStats();
		}
	}

	/**
	 * Runs before printing each report interval
	 */
	public void updateStats() {
		for ( ModelEntity each : Simulation.getClonesOf(ModelEntity.class) ) {
			each.updateStateRecordHours();
		}
	}
	/**
	 * Runs at the end of each report interval
	 */
	protected void clearReportStats() {
		for ( ModelEntity each : Simulation.getClonesOf(ModelEntity.class) ) {
			each.clearReportStats();
		}
	}


	// ******************************************************************************************************
	// INPUT METHODS
	// ******************************************************************************************************

	/**
	 * Interpret the input data in the given buffer of strings corresponding to the given keyword.
	 */
	public void readData_ForKeyword(StringVector data, String keyword, boolean syntaxOnly, boolean isCfgInput)
	throws InputErrorException {

		// --------------- ReportDirectory ---------------
		if( "ReportDirectory".equalsIgnoreCase( keyword ) ) {
			Input.assertCountRange(data, 0, 1);
			if (data.size() == 0)
				InputAgent.setReportDirectory("");
			else
				InputAgent.setReportDirectory(data.get(0));

			return;
		}
		if ("GroupList".equalsIgnoreCase(keyword)) {
			ArrayList<Group> temp = new ArrayList<Group>(data.size());

			for (int i = 0; i < data.size(); i++) {
				temp.add(Input.parseEntity(data.get(i), Group.class));
			}
			groupList.clear();
			groupList.addAll(temp);
			return;
		}

		// --------------- ReportIntervals ---------------
		if( "ReportIntervals".equalsIgnoreCase( keyword ) ) {
			reportIntervals = Input.parseDoubleVector(data, 1e-15, Double.POSITIVE_INFINITY);
			return;
		}

		super.readData_ForKeyword( data, keyword, syntaxOnly, isCfgInput );
	}

	public void setReportIntervals(DoubleVector vec) {
		reportIntervals = vec;
	}

	public DoubleVector getReportIntervals() {
		return reportIntervals;
	}

	// ******************************************************************************************************
	// WORKING METHODS
	// ******************************************************************************************************

	/**
	 * Close the reports
	 */
	public void closeReports() {

		if( groupReportFile != null ) {
			groupReportFile.flush();
			groupReportFile.close();
		}
	}

	/**
	 * Check for open files and give an error if there are open files
	 */
	public void checkReports() {
		if (groupList.size() == 0)
			return;

		this.clearFile( groupReportFileName );
		groupReportFile = new FileEntity( groupReportFileName, FileEntity.FILE_WRITE, false );
	}

	/**
	 * Set the file names of the reports
	 */
	public void initializeFileNames() {
		String name = InputAgent.getReportDirectory() +  InputAgent.getRunName();
		groupReportFileName = name + ".grp";
	}

	/**
	 * Print the report header on the given file
	 */
	public void printReportHeaderOn( FileEntity anOut ) {
		String executableName = System.getProperty( "TLS.name" );
		if (executableName == null)
			executableName = System.getProperty( "exe4j.moduleName" );

		// If an executable name exists, print it
		if( executableName != null ) {
			anOut.format( "Executable File:  %s\n\n", executableName );
		}

		anOut.format( "Simulation Run Label:  %s\n\n", InputAgent.getRunName() );
		anOut.format( "Run Duration:\t%s\n", simulation.getRunDuration() );
	}

	/**
 	 * Clear the file with the given name
 	 */
 	public void clearFile( String fileName ) {

		if( FileEntity.fileExists( fileName ) ) {
			FileEntity file = new FileEntity( fileName, FileEntity.FILE_WRITE, false );
			file.flush();
			file.close();
		}
 	}

	/**
	 * Prepare the group report file for writing by opening it
	 */
	public void doGroupReport() {
		if (groupList.size() == 0)
			return;

		if (reportIntervals.size() != 0)
			groupReportFile = new FileEntity(groupReportFileName, FileEntity.FILE_WRITE, false);
	}

	/**
	 * Print the reports at the end of the run
	 */
	public void doReports() {
		if (groupList.size() == 0)
			return;

		if (reportIntervals.size() == 0) {
			this.clearFile(groupReportFileName);
			this.printReportHeaderOn(groupReportFile);
			this.printGroupReportOn(groupReportFile);

			groupReportFile.flush();
			groupReportFile.close();
		} else {
			this.printGroupReport();
		}
	}

	/**
	 * Print group report, when multiple group report intervals are requested
	 */
	public void printGroupReport() {
		if(groupList.size() == 0)
			return;

		this.printReportHeaderOn( groupReportFile );
		this.printGroupReportOn( groupReportFile );
		groupReportFile.putString( "\f" );
		groupReportFile.flush();
	}

	/**
	 * Print group report to the given file
	 */
	public void printGroupReportOn( FileEntity anOut ) {

		anOut.format( "Report Start:\t%f\nReport End:\t%f\n",
				   lastReportIntervalTime, getCurrentTime());
		anOut.format( "==================\n\n" );

		// Print the utilization for each member of the group
		for (Group each : groupList) {
			ArrayList<ModelEntity> temp = new ArrayList<ModelEntity>();
			for (int i = 0; i < each.getList().size(); i++)
				temp.add((ModelEntity)each.getList().get(i));
			this.printGroupUtilizationOn(each.getName(), temp, anOut);
			anOut.newLine();
		}
 	}

	/**
	 * Print the utilization statistics for individual group members and as a total to the given file
	 */
	public void printGroupUtilizationOn(String title, ArrayList<? extends ModelEntity> grp, FileEntity anOut) {
		// If there are no elements in the group, do nothing
		if (grp.size() == 0)
			return;

		// Print the name of the group
		anOut.putString(title);
		anOut.newLine();
		for (int i = 0; i < title.length(); i++) {
			anOut.putString("-");
		}
		anOut.newLine();

		// Print the header and determine if the values on the bottom line are TOTAL, AVERAGE, or BLANK
		IntegerVector bottomLine = grp.get(0).printUtilizationHeaderOn( anOut );

		// Set up the totals, minimum, and maximum for the columns
		DoubleVector columnTotals = new DoubleVector( bottomLine.size() );
		DoubleVector columnMinimums = new DoubleVector( bottomLine.size() );
		DoubleVector columnMaximums = new DoubleVector( bottomLine.size() );
		columnTotals.fillWithEntriesOf( bottomLine.size(), 0.0 );
		columnMinimums.fillWithEntriesOf( bottomLine.size(), Double.POSITIVE_INFINITY );
		columnMaximums.fillWithEntriesOf( bottomLine.size(), Double.NEGATIVE_INFINITY );

		// Loop through each member of the group
		int count = 0;
		for (int i = 0; i < grp.size(); i++) {
			ModelEntity ent = grp.get(i);
			if (!ent.isActive())
				continue;

			// Print and store the values for this member
			DoubleVector columnValues = ent.printUtilizationOn( anOut );

			// Update the total, minimum, and maximum column values
			for( int j = 0; j < columnValues.size(); j++ ) {
				columnTotals.addAt( columnValues.get( j ), j );
				if( columnMinimums.get( j ) > columnValues.get( j ) ) {
					columnMinimums.set( j, columnValues.get( j ) );
				}
				if( columnMaximums.get( j ) < columnValues.get( j ) ) {
					columnMaximums.set( j, columnValues.get( j ) );
				}
			}
			count++;
		}

		// Print the bottom line for the group
		anOut.putStringTabs( "Total", 1 );
		for( int i = 1; i < bottomLine.size(); i++ ) {
			switch (bottomLine.get(i)) {
			case TOTAL_NO_DEC:
				anOut.putDoubleWithDecimals(columnTotals.get(i), 0);
				break;
			case TOTAL_ONE_DEC:
				anOut.putDoubleWithDecimals(columnTotals.get(i), 1);
				break;
			case TOTAL_TWO_DEC:
				anOut.putDoubleWithDecimals(columnTotals.get(i), 2);
				break;
			case TOTAL_THREE_DEC:
				anOut.putDoubleWithDecimals(columnTotals.get(i), 3);
				break;
			case TOTAL_FOUR_DEC:
				anOut.putDoubleWithDecimals(columnTotals.get(i), 4);
				break;
			case TOTAL_FIVE_DEC:
				anOut.putDoubleWithDecimals(columnTotals.get(i), 5);
				break;
			case AVERAGE_PCT_NO_DEC:
				anOut.putDoublePercentWithDecimals(columnTotals.get(i) / count, 0);
				break;
			case AVERAGE_PCT_ONE_DEC:
				anOut.putDoublePercentWithDecimals(columnTotals.get(i) / count, 1);
				break;
			case AVERAGE_PCT_TWO_DEC:
				anOut.putDoublePercentWithDecimals(columnTotals.get(i) / count, 2);
				break;
			case AVERAGE_PCT_THREE_DEC:
				anOut.putDoublePercentWithDecimals(columnTotals.get(i) / count, 3);
				break;
			case AVERAGE_PCT_FOUR_DEC:
				anOut.putDoublePercentWithDecimals(columnTotals.get(i) / count, 4);
				break;
			case AVERAGE_PCT_FIVE_DEC:
				anOut.putDoublePercentWithDecimals(columnTotals.get(i) / count, 5);
				break;
			case MINIMUM_NO_DEC:
				anOut.putDoubleWithDecimals(columnMinimums.get(i), 0);
				break;
			case MINIMUM_ONE_DEC:
				anOut.putDoubleWithDecimals(columnMinimums.get(i), 1);
				break;
			case MINIMUM_TWO_DEC:
				anOut.putDoubleWithDecimals(columnMinimums.get(i), 2);
				break;
			case MINIMUM_THREE_DEC:
				anOut.putDoubleWithDecimals(columnMinimums.get(i), 3);
				break;
			case MINIMUM_FOUR_DEC:
				anOut.putDoubleWithDecimals(columnMinimums.get(i), 4);
				break;
			case MINIMUM_FIVE_DEC:
				anOut.putDoubleWithDecimals(columnMinimums.get(i), 5);
				break;
			case MAXIMUM_NO_DEC:
				anOut.putDoubleWithDecimals(columnMaximums.get(i), 0);
				break;
			case MAXIMUM_ONE_DEC:
				anOut.putDoubleWithDecimals(columnMaximums.get(i), 1);
				break;
			case MAXIMUM_TWO_DEC:
				anOut.putDoubleWithDecimals(columnMaximums.get(i), 2);
				break;
			case MAXIMUM_THREE_DEC:
				anOut.putDoubleWithDecimals(columnMaximums.get(i), 3);
				break;
			case MAXIMUM_FOUR_DEC:
				anOut.putDoubleWithDecimals(columnMaximums.get(i), 4);
				break;
			case MAXIMUM_FIVE_DEC:
				anOut.putDoubleWithDecimals(columnMaximums.get(i), 5);
				break;
			case AVERAGE_NO_DEC:
				anOut.putDoubleWithDecimals(columnTotals.get(i) / count, 0);
				break;
			case AVERAGE_ONE_DEC:
				anOut.putDoubleWithDecimals(columnTotals.get(i) / count, 1);
				break;
			case AVERAGE_TWO_DEC:
				anOut.putDoubleWithDecimals(columnTotals.get(i) / count, 2);
				break;
			case AVERAGE_THREE_DEC:
				anOut.putDoubleWithDecimals(columnTotals.get(i) / count, 3);
				break;
			case AVERAGE_FOUR_DEC:
				anOut.putDoubleWithDecimals(columnTotals.get(i) / count, 4);
				break;
			case AVERAGE_FIVE_DEC:
				anOut.putDoubleWithDecimals(columnTotals.get(i) / count, 5);
				break;
			case BLANK:
				break;
			}
			anOut.putTab();
		}
		anOut.newLine();
	}
}
