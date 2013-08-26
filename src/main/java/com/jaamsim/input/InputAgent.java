/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2009-2011 Ausenco Engineering Canada Inc.
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
package com.jaamsim.input;

import java.awt.FileDialog;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.swing.JOptionPane;

import com.jaamsim.ui.ExceptionBox;
import com.jaamsim.ui.FrameBox;
import com.sandwell.JavaSimulation.Entity;
import com.sandwell.JavaSimulation.ErrorException;
import com.sandwell.JavaSimulation.FileEntity;
import com.sandwell.JavaSimulation.Group;
import com.sandwell.JavaSimulation.Input;
import com.sandwell.JavaSimulation.InputErrorException;
import com.sandwell.JavaSimulation.ObjectType;
import com.sandwell.JavaSimulation.Palette;
import com.sandwell.JavaSimulation.Simulation;
import com.sandwell.JavaSimulation.StringVector;
import com.sandwell.JavaSimulation.Util;
import com.sandwell.JavaSimulation.Vector;
import com.sandwell.JavaSimulation3D.DisplayEntity;
import com.sandwell.JavaSimulation3D.GUIFrame;

public class InputAgent {
	private static final String addedRecordMarker = "\" *** Added Records ***";

	private static int numErrors = 0;
	private static int numWarnings = 0;
	private static FileEntity logFile;

	private static double lastTimeForTrace;

	private static String configFileName;
	private static boolean batchRun;
	private static boolean sessionEdited;
	private static boolean addedRecordFound;
	private static boolean endOfFileReached;		// notes end of cfg files
	// ConfigurationFile load and save variables
	final protected static int SAVE_ONLY = 2;

	private static final String INP_ERR_DEFINEUSED = "The name: %s has already been used and is a %s";

	private static boolean printInputReport;
	private static String reportDirectory;

	static {
		addedRecordFound = false;
		sessionEdited = false;
		endOfFileReached = false;
		batchRun = false;
		configFileName = null;
		reportDirectory = "";
		lastTimeForTrace = -1.0d;
		printInputReport = false;
	}

	public static void clear() {
		logFile = null;
		numErrors = 0;
		numWarnings = 0;
		addedRecordFound = false;
		sessionEdited = false;
		configFileName = null;
		reportDirectory = "";
		lastTimeForTrace = -1.0d;
		printInputReport = false;
	}


	public static void setPrintInputs(boolean print) {
		printInputReport = print;
	}

	public static String getReportDirectory() {
		return reportDirectory;
	}

	public static void setReportDirectory(String dir) {
		reportDirectory = Util.getAbsoluteFilePath(dir);
		if (!reportDirectory.substring(reportDirectory.length() - 1).equals("\\"))
			reportDirectory = reportDirectory + "\\";

		// Create the report directory if it does not already exist
		// This code should probably be added to FileEntity someday to
		// create necessary folders on demand.
		File f = new File(reportDirectory);
		f.mkdirs();
	}

	public static void setConfigFileName(String name) {
		configFileName = name;
	}

	public static String getConfigFileName() {
		return configFileName;
	}

	public static String getRunName() {
		String runName;
		if( InputAgent.getConfigFileName() == null ) {
			runName = "";
		}
		else {
			int index = Util.fileShortName( InputAgent.getConfigFileName() ).indexOf( "." );
			if( index > -1 ) {
				runName = Util.fileShortName( InputAgent.getConfigFileName() ).substring( 0, index );
			}
			else {
				runName = Util.fileShortName( InputAgent.getConfigFileName() );
			}
		}
		return runName;
	}

	public static boolean hasAddedRecords() {
		return addedRecordFound;
	}

	public static boolean isSessionEdited() {
		return sessionEdited;
	}

	public static void setBatch(boolean batch) {
		batchRun = batch;
	}

	public static boolean getBatch() {
		return batchRun;
	}

	/**
	 * returns true if the first and last tokens are matched braces
	 **/
	public static boolean enclosedByBraces(ArrayList<String> tokens) {
		if(tokens.size() < 2 || tokens.indexOf("{") < 0) // no braces
			return false;

		int level =1;
		int i = 1;
		for(String each: tokens.subList(1, tokens.size())) {
			if(each.equals("{")) {
				level++;
			}
			if(each.equals("}")) {
				level--;

				// Matching close brace found
				if(level == 0)
					break;
			}
			i++;
		}

		if(level == 0 && i == tokens.size()-1) {
			return true;
		}

		return false;
	}

	private static int getBraceDepth(ArrayList<String> tokens, int startingBraceDepth, int startingIndex) {
		int braceDepth = startingBraceDepth;
		for (int i = startingIndex; i < tokens.size(); i++) {
			String token = tokens.get(i);

			if (token.equals("{"))
				braceDepth++;

			if (token.equals("}"))
				braceDepth--;

			if (braceDepth < 0) {
				InputAgent.logBadInput(tokens, "Extra closing braces found");
				tokens.clear();
			}

			if (braceDepth > 2) {
				InputAgent.logBadInput(tokens, "Maximum brace depth (2) exceeded");
				tokens.clear();
			}
		}

		return braceDepth;
	}

	private static URI pwdPath;
	private static URI pwdRoot;
	private static URI resRoot;
	private static URI resPath;
	private static final String res = "/resources/inputs/";

	static {
		// Walk up the parent list until we find a parentless entry, call that
		// the 'root'
		File f = new File(System.getProperty("user.dir"));
		File par = f;
		while (true) {
			File t = par.getParentFile();
			if (t == null) {
				pwdRoot = par.toURI();
				break;
			}
			par = t;
		}

		pwdPath = pwdRoot.relativize(f.toURI());

		try {
			// locate the resource folder, and create
			resRoot = InputAgent.class.getResource(res).toURI();
		}
		catch (URISyntaxException e) {}

		resPath = URI.create("");
	}

	public static final void readResource(String res) {
		if (res == null)
			return;

		readStream(resRoot, resPath, res);
	}

	public static final boolean readStream(URI root, URI path, String file) {
		URI resolved = path.resolve(file);
		resolved.normalize();

		if (resolved.getRawPath().contains("../")) {
			InputAgent.logWarning("Unable to resolve path %s%s - %s", root.toString(), path.toString(), file);
			return false;
		}

		URL t = null;
		try {
			t = new URI(root.toString() + resolved.toString()).toURL();
		}
		catch (MalformedURLException e) {}
		catch (URISyntaxException e) {}

		if (t == null) {
			InputAgent.logWarning("Unable to resolve path %s%s - %s", root.toString(), path.toString(), file);
			return false;
		}
		readURL(t);

		return true;
	}

	public static void readURL(URL url) {
		if (url == null)
			return;

		BufferedReader buf = null;
		try {
			InputStream in = url.openStream();
			buf = new BufferedReader(new InputStreamReader(in));
		} catch (IOException e) {
			InputAgent.logWarning("Could not read from %s", url.toString());
			return;
		}

		try {
			ArrayList<String> record = new ArrayList<String>();
			int braceDepth = 0;

			while (true) {
				String line = buf.readLine();
				// end of file, stop reading
				if (line == null)
					break;

				// Set flag if found " *** Added Records ***
				if ( line.trim().equalsIgnoreCase( addedRecordMarker ) ) {
					addedRecordFound = true;
				}

				int previousRecordSize = record.size();
				Parser.tokenize(record, line);
				braceDepth = InputAgent.getBraceDepth(record, braceDepth, previousRecordSize);
				if( braceDepth != 0 )
					continue;

				Parser.removeComments(record);
				InputAgent.processRecord(url, record);
				record.clear();
			}

			// Leftover Input at end of file
			if (record.size() > 0)
				InputAgent.logBadInput(record, "Leftover input at end of file");
			buf.close();
		}
		catch (IOException e) {
			// Make best effort to ensure it closes
			try { buf.close(); } catch (IOException e2) {}
		}

	}

	private static void processRecord(URL url, ArrayList<String> record) {
		//InputAgent.echoInputRecord(record);

		if (record.size() == 0)
			return;

		if (record.get(0).equalsIgnoreCase("INCLUDE")) {
			InputAgent.processIncludeRecord(url, record);
			return;
		}

		if (record.get(0).equalsIgnoreCase("DEFINE")) {
			InputAgent.processDefineRecord(record);
			return;
		}

		// Otherwise assume it is a Keyword record
		InputAgent.processKeywordRecord(record);
	}

	private static void processIncludeRecord(URL baseURL, ArrayList<String> record) {
		if (record.size() != 2) {
			InputAgent.logError("Bad Include record, should be: Include <File>");
			return;
		}

		// Ensure the include filename is well formed
		URL finalURL = null;
		try {
			URI incFile = new URI(record.get(1).replaceAll("\\\\", "/"));

			// Construct a base file in case this URI is relative and split at ! to
			// account for a jar:file:<jarfilename>!<internalfilename> URL
			int bangIndex = baseURL.toString().lastIndexOf("!") + 1;
			String prefix = baseURL.toString().substring(0, bangIndex);
			String folder = baseURL.toString().substring(bangIndex);

			URI folderURI = new URI(folder).resolve(incFile);
			// Remove all remaining relative path directives ../
			String noRelative = folderURI.toString().replaceAll("\\.\\./", "");
			finalURL = new URL(prefix + noRelative);
		}
		catch (NullPointerException e) {}
		catch (URISyntaxException e) {}
		catch (MalformedURLException e) {}
		finally {
			if (finalURL == null) {
				InputAgent.logError("Unable to parse filename: %s", record.get(1));
				return;
			}
		}

		InputAgent.readURL(finalURL);
	}

	private static void processDefineRecord(ArrayList<String> record) {
		if (record.size() < 5 ||
		    !record.get(2).equals("{") ||
		    !record.get(record.size() - 1).equals("}")) {
			InputAgent.logError("Bad Define record, should be: Define <Type> { <names>... }");
			return;
		}

		Class<? extends Entity> proto = null;
		try {
			if( record.get( 1 ).equalsIgnoreCase( "Palette" ) ) {
				proto = Palette.class;
			}
			else if( record.get( 1 ).equalsIgnoreCase( "ObjectType" ) ) {
				proto = ObjectType.class;
			}
			else {
				proto = Input.parseEntityType(record.get(1));
			}
		}
		catch (InputErrorException e) {
			InputAgent.logError("%s", e.getMessage());
			return;
		}

		// Loop over all the new Entity names
		for (int i = 3; i < record.size() - 1; i++) {
			InputAgent.defineEntity(proto, record.get(i), addedRecordFound);
		}
	}

	/**
	 * Like defineEntity(), but will generate a unique name if a name collision exists
	 * @param proto
	 * @param key
	 * @param addedEntity
	 * @return
	 */
	public static <T extends Entity> T defineEntityWithUniqueName(Class<T> proto, String key, boolean addedEntity) {

		// Has the provided name been used already?
		if (Entity.getNamedEntity(key) == null) {
			return defineEntity(proto, key, addedEntity);
		}

		// Try the provided name plus "-1", "-2", etc. until an unused name is found
		int entityNum = 1;
		while(true) {
			String name = String.format("%s-%d", key, entityNum);
			if (Entity.getNamedEntity(name) == null) {
				return defineEntity(proto, name, addedEntity);
			}

			entityNum++;
		}
	}

	/**
	 * if addedEntity is true then this is an entity defined
	 * by user interaction or after added record flag is found;
	 * otherwise, it is from an input file define statement
	 * before the model is configured
	 * @param proto
	 * @param key
	 * @param addedEntity
	 */
	public static <T extends Entity> T defineEntity(Class<T> proto, String key, boolean addedEntity) {
		Entity existingEnt = Input.tryParseEntity(key, Entity.class);
		if (existingEnt != null) {
			InputAgent.logError(INP_ERR_DEFINEUSED, key, existingEnt.getClass().getSimpleName());
			return null;
		}

		T ent = null;
		try {
			ent = proto.newInstance();
			if (addedEntity) {
				ent.setFlag(Entity.FLAG_ADDED);
				sessionEdited = true;
			}
		}
		catch (InstantiationException e) {}
		catch (IllegalAccessException e) {}
		finally {
			if (ent == null) {
				InputAgent.logError("Could not create new Entity: %s", key);
				return null;
			}
		}

		ent.setInputName(key);
		return ent;
	}

	private static void processKeywordRecord(ArrayList<String> record) {
		Entity ent = Input.tryParseEntity(record.get(0), Entity.class);
		if (ent == null) {
			InputAgent.logError("Could not find Entity: %s", record.get(0));
			return;
		}

		ArrayList<ArrayList<String>> keywords = InputAgent.splitKeywords(record);
		for (ArrayList<String> keyword : keywords) {
			if (keyword.size() < 3 ||
			    !keyword.get(1).equals("{") ||
			    !keyword.get(keyword.size() - 1).equals("}")) {
				InputAgent.logError("Keyword not valid, should be <keyword> { <args> }");
				continue;
			}

			String key = keyword.get(0);
			StringVector args = new StringVector(keyword.size() - 3);
			for (int i = 2; i < keyword.size() - 1; i++) {
				args.add(keyword.get(i));
			}
			try {
				InputAgent.processKeyword(ent, args, key);
			}
			catch (Throwable e) {
				InputAgent.logError("Exception thrown from Entity: %s for keyword:%s - %s", ent.getInputName(), key, e.getMessage());
			}
		}
	}

	private static ArrayList<ArrayList<String>> splitKeywords(ArrayList<String> input) {
		ArrayList<ArrayList<String>> inputs = new ArrayList<ArrayList<String>>();

		int braceDepth = 0;
		ArrayList<String> currentLine = null;
		for (int i = 1; i < input.size(); i++) {
			if (currentLine == null)
				currentLine = new ArrayList<String>( input.size() );

			currentLine.add(input.get(i));
			if (input.get(i).equals("{")) {
				braceDepth++;
				continue;
			}

			if (input.get(i).equals("}")) {
				braceDepth--;
				if (braceDepth == 0) {
					inputs.add(currentLine);
					currentLine = null;
					continue;
				}
			}
		}

		return inputs;
	}

	public static void doError(Throwable e) {
		if (!batchRun)
			return;

		System.out.println("An error occurred in the simulation environment.  Please check inputs for an error:");
		System.out.println(e);
		GUIFrame.shutdown(1);
	}

	// Load the run file
	public static void loadConfigurationFile( String fileName) {

		String inputTraceFileName = InputAgent.getRunName() + ".log";
		// Initializing the tracing for the model
		try {
			System.out.println( "Creating trace file" );

			// Set and open the input trace file name
			logFile = new FileEntity( inputTraceFileName, FileEntity.FILE_WRITE, false );
		}
		catch( Exception e ) {
			throw new ErrorException( "Could not create trace file" );
		}

		InputAgent.loadConfigurationFile(fileName, true);

		// At this point configuration file is loaded

		// The session is not considered to be edited after loading a configuration file
		sessionEdited = false;

		// Save and close the input trace file
		if (logFile != null) {
			if (InputAgent.numWarnings == 0 && InputAgent.numErrors == 0) {
				logFile.close();
				logFile.delete();
				logFile = new FileEntity( inputTraceFileName, FileEntity.FILE_WRITE, false );
			}
		}

		//  Check for found errors
		if( InputAgent.numErrors > 0 )
			throw new InputErrorException("%d input errors found, check log file", InputAgent.numErrors);

		if (printInputReport)
			InputAgent.printInputFileKeywords();
	}

	/**
	 *
	 * @param fileName
	 * @param firstTime ( true => this is the main config file (run file);  false => this is an included file within  main config file or another included file )
	 */
	public static void loadConfigurationFile( String fileName, boolean firstTime ) {
		//System.out.println( "load configuration file " + fileName );
		FileEntity file;
		Vector record;

		// If the file does not exist, write an error and exit
		if( !FileEntity.fileExists( fileName ) ) {
			System.out.println( (("Error  --  The input file " + fileName) + " was not found ") );
			GUIFrame.shutdown(0);
		}

		// Open the file
		file = new FileEntity( fileName, FileEntity.FILE_READ, false );

		String mainRootDirectory = null;
		String originalJarFileRootDirectory = null;

		if( firstTime ) {

			// Store the directory of the first input file
			file.setRootDirectory();
		}
		else {

			// Save the directory of the first file
			mainRootDirectory = FileEntity.getRootDirectory();

			// Switch to the directory of the current input file
			file.setRootDirectory();

			// Save the directory of the first file within the jar file
			originalJarFileRootDirectory = FileEntity.getJarFileRootDirectory();
		}

		// Initialize the input file
		file.toStart();

		GUIFrame.instance().setProgressText(file.getFileName());

		// For each line in the file
		while( true ) {

			// Read the next line to record
			record = getNextParsedRecord( file );
//			System.out.println( record.toString() );

			// Determine the amount of file read and update the progress gauge
			int per = (int)(((double)file.getNumRead()) / ((double)file.getLength()) * 100.0);
			GUIFrame.instance().setProgress( per );

			// When end-of-file is reached, record.size() == 0
			if( endOfFileReached ) {
				break;
			}

			// Process this line if it is not empty
			if ( record.size() > 0 ) {

				// If there is an included file, LoadConfigurationFile is run for that (This is recursive)
				InputAgent.readRecord(record, file);
			}
		}

		// Reset the progress bar to zero and remove its label
		GUIFrame.instance().setProgressText(null);
		GUIFrame.instance().setProgress(0);

		// Close the file
		file.close();

		// Restore to the directory of the first input file
		if ( ! firstTime ) {
			FileEntity.setRootDirectory( mainRootDirectory );
			FileEntity.setJarFileRootDirectory( originalJarFileRootDirectory );
		}
	}

	/**
	 * Reads record, either as a default, define statement, include, or keyword
	 * @param record
	 * @param file
	 */
	private static void readRecord(Vector record, FileEntity file) {
		if(record.size() < 2){
			InputAgent.logError("Invalid input line - missing keyword or parameter");
			return;
		}

		try {
			if( "DEFINE".equalsIgnoreCase( (String)record.get( 0 ) )  ) {
				ArrayList<String> tempCopy = new ArrayList<String>(record.size());
				for (int i = 0; i < record.size(); i++)
					tempCopy.add((String)record.get(i));
				InputAgent.processDefineRecord(tempCopy);
			}
			// Process other files
			else if( "INCLUDE".equalsIgnoreCase( (String)record.get( 0 ) )  ) {

				if( record.size() == 2 ) {
					if( FileEntity.fileExists( (String)record.get( 1 ) ) ) {

						// Load the included file and process its records first
						InputAgent.loadConfigurationFile( (String)record.get( 1 ), false );
						GUIFrame.instance().setProgressText(file.getFileName());
					}
					else {
						InputAgent.logError("File not found: %s", (String)record.get(1));
					}
				}
				else {
					InputAgent.logError("There must be exactly two entries in an Include record");
				}
			}

			// is a keyword
			else {
				InputAgent.processData(record);
			}
		}
		catch( InputErrorException iee ) {
			InputAgent.logError( iee.getMessage() );
		}
	}

	// Read the next line of the file
	protected static Vector getNextParsedRecord(FileEntity file) {

		Vector record = new Vector();

		int noOfUnclosedBraces = 0;

		do {
			Vector nextLine = file.readAndParseRecord();
			InputAgent.echoInput(nextLine);
			if (nextLine.size() == 0) {
				endOfFileReached = true;
			}
			else {
				endOfFileReached = false;
			}

			// Set flag if input records added through the EditBox interface are found
			if ( !(endOfFileReached) && ( ((String) nextLine.get( 0 )).equalsIgnoreCase( addedRecordMarker ) ) ) {
				addedRecordFound = true;
			}

			Util.discardComment( nextLine );

			// Count braces and allow input with a missing space following an opening brace and/or a missing space preceding a closing brace
			for (int i = 0; i < nextLine.size(); i++) {
				String checkRecord = (String)nextLine.get( i );
				Vector parsedString = new Vector( nextLine.size() );

				// Check for braces
				for  (int j=0; j<checkRecord.length(); j++) {

					// '(' & ')' are not allowed in the input file
					if( checkRecord.charAt(j) == '(' || checkRecord.charAt(j) == ')' ) {
						throw new ErrorException( "\n\"" + checkRecord.charAt(j) + "\""  + " is not allowed in the input file: \n" + nextLine + "\n" + FileEntity.getRootDirectory() + file.getFileName() );
					}
					if	(checkRecord.charAt(j) == '{') {
						noOfUnclosedBraces++;
						parsedString.add("{");
					}
					else if (checkRecord.charAt(j) == '}') {
						noOfUnclosedBraces--;
						parsedString.add("}");
					} else {
						// no brace is found, assume it is a whole word until the next brace
						StringBuffer stringDump = new StringBuffer( checkRecord.length() );
						// iterate through
						for ( int k = j; k<checkRecord.length(); k++ ) {
							// if a brace is found, end checking this word
							if ( checkRecord.charAt(k) == '{' || checkRecord.charAt(k) == '}' ) {
								k = checkRecord.length();
							}
							// otherwise, make the word
							else {
								stringDump.append( checkRecord.charAt(k) );
							}
						}
						j += stringDump.length() - 1;
						parsedString.add(stringDump.toString());
					}

				}

				// Add brackets as separate entries
				if (parsedString.size() > 1 ) {
					nextLine.remove( i );
					nextLine.addAll( i , parsedString );
					i = i + parsedString.size() - 1;
				}

			}
			record.addAll(nextLine);

		} while ( ( noOfUnclosedBraces != 0 ) && ( !endOfFileReached ) );

		if( noOfUnclosedBraces != 0 ) {
			InputAgent.logError("Missing closing brace");
		}
		return record;

	}

	public static final void apply(Entity ent, Input<?> in, StringVector data) {
		in.parse(data);
		ent.updateForInput(in);
	}

	public static final void apply(Entity ent, StringVector data, String keyword)
	throws InputErrorException {
		Input<?> in = ent.getInput(keyword);
		if (in != null) {
			InputAgent.apply(ent, in, data);
			FrameBox.valueUpdate();
		} else {
			ent.readData_ForKeyword(data, keyword);
			FrameBox.valueUpdate();
		}
	}

	private static void processKeyword( Entity entity, StringVector recordCmd, String keyword) {
		if (keyword == null)
			throw new InputErrorException("The keyword is null.");

		if (entity.testFlag(Entity.FLAG_LOCKED))
			throw new InputErrorException("Entity: %s is locked and cannot be modified", entity.getName());

		try {
			Input<?> input = entity.getInput( keyword );
			if( input != null && input.isAppendable() ) {
				ArrayList<StringVector> splitData = Util.splitStringVectorByBraces(recordCmd);
				for ( int i = 0; i < splitData.size(); i++ ) {
					InputAgent.apply(entity, input, splitData.get(i));
				}
			}
			else {
				InputAgent.apply(entity, recordCmd, keyword);
			}

			// Create a list of entities to update in the edit table
			ArrayList<Entity> updateList = null;
			if (entity instanceof Group && input == null) {
				updateList = ((Group)entity).getList();
			}
			else {
				updateList = new ArrayList<Entity>(1);
				updateList.add(entity);
			}

			// Store the keyword data for use in the edit table
			for( int i = 0; i < updateList.size(); i++ ) {
				Entity ent = updateList.get( i );
				Input<?> in = ent.getInput(keyword);

				if (in != null) {
					InputAgent.updateInput(ent, in, recordCmd);
				}

				// The keyword is not on the editable keyword list
				else {
					InputAgent.logWarning("Keyword %s is obsolete. Please replace the Keyword. Refer to the manual for more detail.", keyword);
				}
			}
		}
		catch ( InputErrorException e ) {
			InputAgent.logError("Entity: %s Keyword: %s - %s", entity.getName(), keyword, e.getMessage());
			throw e;
		}
	}

	public static void processData(Entity ent, Vector rec) {
		if( rec.get( 1 ).toString().trim().equals( "{" ) ) {
			InputAgent.logError("A keyword expected after: %s", ent.getName());
		}
		ArrayList<StringVector> multiCmds = InputAgent.splitMultipleCommands(rec);

		// Process each command
		for( int i = 0; i < multiCmds.size(); i++ ) {
			StringVector cmd = multiCmds.get(i);
			String keyword = cmd.remove(0);

			// Process the record
			InputAgent.processKeyword(ent, cmd, keyword);
		}
		return;
	}

	/**
	 * process's input data from record for use as a keyword.
	 * format of record: <obj-name> <keyword> <data> <keyword> <data>
	 * braces are included
	 */
	public static void processData( Vector record ) {
		String item1 = ((String)record.get( 0 )).trim();

		//  Checks on Entity:
		Entity obj = Input.tryParseEntity(item1, Entity.class);
		if (obj == null) {
			InputAgent.logError("Object not found: %s", item1);
			return;
		}

		// Entity exists with name <entityName> or name <region>/<entityName>
		InputAgent.processData(obj, record);
	}

	/**
	 * returns a vector of vectors
	 * each vector will be of form <obj-name> <kwd> <data> <data>
	 * no braces are returned
	 */
	private static ArrayList<StringVector> splitMultipleCommands( Vector record ) {

		// SUPPORTED SYNTAX:
		//
		//   <obj-name> <kwd> { <par> }
		//   <obj-name> <kwd> { <par> <par> ... }
		//   <obj-name> <kwd> { <par> <par> ... } <kwd> { <par> <par> ... } ...
		//   <obj-name> <kwd> <par> <kwd> { <par> <par> ... } ...
		ArrayList<StringVector> multiCmds = new ArrayList<StringVector>();
		int noOfUnclosedBraces = 0;

		// Loop through the keywords and assemble new commands
		for( int i = 1; i < record.size(); ) {

			// Enter the class, object, and keyword in the new command
			StringVector cmd = new StringVector( record.size() );

			// Keyword changes as loop proceeds
			cmd.add((String)record.get(i));
			i++;

			// For a command of the new form "<obj-name> <file-name>", record
			// will be empty here.
			if( i < record.size() ) {

				// If there is an opening brace, then the keyword has a list of
				// parameters
				String openingBrace = (String)record.get( i );
				if( openingBrace.equals("{") ) {
					noOfUnclosedBraces ++ ;
					i++; // move past the opening brace {

					// Iterate through record
					while( (i < record.size()) && ( noOfUnclosedBraces > 0 ) ) {
						if ( record.get(i).equals("{") )
							noOfUnclosedBraces ++ ;
						else if (record.get(i).equals("}"))
							noOfUnclosedBraces -- ;
						cmd.add((String)record.get(i));
						i++;
					}

					if( ( record.size() == i ) && ( noOfUnclosedBraces != 0) ) { // corresponding "}" is missing
						InputAgent.logError("Closing brace } is missing.");
						return multiCmds;
					}

					// Last item added was the corresponding closing brace
					else {
						cmd.remove(cmd.size()-1); // throw out the closing brace }
						multiCmds.add( cmd );
					}
				}

				// If there is no brace, then the keyword must have a single
				// parameter.
				else {
					cmd.add((String)record.get(i));
					i++;
					multiCmds.add( cmd );
				}
			}

			// Record contains no other items
			else {
				multiCmds.add( cmd );
			}
		}
		return multiCmds;
	}

	private static class ConfigFileFilter implements FilenameFilter {
		@Override
		public boolean accept(File inFile, String fileName) {
			return fileName.endsWith("[cC][fF][gG]");
		}
	}

	public static void load(GUIFrame gui) {
		System.out.println("Loading...");

		FileDialog chooser = new FileDialog(gui, "Load Configuration File", FileDialog.LOAD);
		chooser.setFilenameFilter(new ConfigFileFilter());
		String chosenFileName = chooseFile(chooser, FileDialog.LOAD);
		if (chosenFileName != null) {
			//dispose();
			setLoadFile(gui, chosenFileName);
		} else {
			//dispose();
		}
	}

	public static void save(GUIFrame gui) {
		System.out.println("Saving...");
		if( InputAgent.getConfigFileName() != null ) {
			setSaveFile(gui, FileEntity.getRootDirectory() + System.getProperty( "file.separator" ) + InputAgent.getConfigFileName(), SAVE_ONLY );
		}
		else {
			saveAs( gui );
		}
	}

	public static void saveAs(GUIFrame gui) {
		System.out.println("Save As...");
		FileDialog chooser = new FileDialog(gui, "Save Configuration File As", FileDialog.SAVE);
		chooser.setFilenameFilter(new ConfigFileFilter());
		String chosenFileName = chooseFile(chooser, FileDialog.SAVE);
		if ( chosenFileName != null ) {
			//dispose();
			setSaveFile(gui, chosenFileName, FileDialog.SAVE );
		} else {
			//dispose();
		}
	}

	/**
	 *  Opens browser to choose file.  returns a boolean if a file was picked, false if canceled or closed.
	 */
	private static String chooseFile(FileDialog chooser, int saveOrLoadType) {

		// filter
		if (saveOrLoadType == FileDialog.SAVE) {
			chooser.setFile( InputAgent.getConfigFileName() );
		} else {
			chooser.setFile( "*.cfg" );
		}

		// display browser
		//this.show();

		chooser.setVisible( true );

		// if a file was picked, set entryarea to be this file
		if( chooser.getFile() != null ) {

			//chooser should not set root directory
			//FileEntity.setRootDirectory( chooser.getDirectory() );
			String chosenFileName = chooser.getDirectory() + chooser.getFile();
			return chosenFileName.trim();
		} else {
			return null;
		}
	}

	public static void configure(GUIFrame gui, String configFileName) {
		try {
			gui.clear();
			Simulation.setSimState(Simulation.SIM_STATE_UNCONFIGURED);

			InputAgent.setConfigFileName(configFileName);
			gui.updateForSimulationState();

			try {
				InputAgent.loadConfigurationFile(configFileName);
			}
			catch( InputErrorException iee ) {
				if (!batchRun)
					ExceptionBox.instance().setError(iee);
				else
					System.out.println( iee.getMessage() );
			}

			// store the present state
			Simulation.setSimState(Simulation.SIM_STATE_CONFIGURED);

			System.out.println("Configuration File Loaded");

			// show the present state in the user interface
			gui.setTitle( Simulation.getModelName() + " - " + InputAgent.getRunName() );
			gui.updateForSimulationState();
		}
		catch( Throwable t ) {
			ExceptionBox.instance().setError(t);
		}
	}

	/**
	 *  Loads configuration file , calls GraphicSimulation.configure() method
	 */
	private static void setLoadFile(final GUIFrame gui, String fileName) {
		final String chosenFileName = fileName;
		new Thread(new Runnable() {
			@Override
			public void run() {
				File temp = new File(chosenFileName);

				if( temp.isAbsolute() ) {
					FileEntity.setRootDirectory( temp.getParentFile() );
					InputAgent.configure(gui, temp.getName());
				}
				else {
					InputAgent.configure(gui, chosenFileName);
				}
				GUIFrame.displayWindows(true);
				FrameBox.valueUpdate();
			}
		}).start();
	}

	/**
	 *  saves the cfg/pos file.  checks for 'save' and 'save as', recursively goes to 'save as' if 'save' is not possible.
	 *  updates runname and filename of file.
	 *  if editbox is open and unaccepted, accepts changes.
	 */
	private static void setSaveFile(GUIFrame gui, String fileName, int saveOrLoadType) {
		String configFilePath = FileEntity.getRootDirectory() + System.getProperty( "file.separator" ) + InputAgent.getConfigFileName();

		// check ending string of filename, force cfg onto end if needed
		if (!(fileName.endsWith(".cfg"))) {
			fileName = fileName.concat(".cfg");
		}

		File temp = new File(fileName);

		//System.out.println("fileName is " + fileName);

		// If the original configuration file is the same as the file to save, and there were no added records,
		// then do not save the file because it would be recursive, i.e. contain "include <fileName>"

		if( configFilePath.equalsIgnoreCase( fileName ) ) {
			if( !InputAgent.hasAddedRecords() )  {
				if( saveOrLoadType == FileDialog.SAVE) {
					// recursive -- if can't overwrite base file, 'save as'

					// Ask if appending to base configuration is ok
					int appendOption = JOptionPane.showConfirmDialog( null,
							"Cannot overwrite base configuration file.  Do you wish to append changes?",
							"Confirm Append",
							JOptionPane.YES_OPTION,
							JOptionPane.WARNING_MESSAGE );

					// Perform append only if yes
					if (appendOption == JOptionPane.YES_OPTION) {
						FileEntity configFile = new FileEntity( fileName, FileEntity.FILE_WRITE, true );
						configFile.write( "\n" + addedRecordMarker );
						addedRecordFound = true;
					}
					else {
						InputAgent.saveAs(gui);
						return;
					}
				}
				else {
					InputAgent.saveAs(gui);
					return;
				}

			} else if ( saveOrLoadType == SAVE_ONLY) {
				System.out.println("Saving...");
			}
		}

		// set root directory
		FileEntity.setRootDirectory( temp.getParentFile() );

		//saveFile = new FileEntity( fileName, FileEntity.FILE_WRITE, false );
		//simulation.printNewConfigurationFileOn( saveFile );
		InputAgent.printNewConfigurationFileWithName( fileName );
		sessionEdited = false;

		//TODOalan set directory of model.. ?
		InputAgent.setConfigFileName(Util.fileShortName(fileName));

		// Set the title bar to match the new run name
		gui.setTitle( Simulation.getModelName() + " - " + InputAgent.getRunName() );

		// close the window
		//dispose();
	}

	/*
	 * write input file keywords and values
	 *
	 * input file format:
	 *  Define Group { <Group names> }
	 *  Define <Object> { <Object names> }
	 *
	 *  <Object name> <Keyword> { < values > }
	 *
	 */
	public static void printInputFileKeywords() {
		Entity ent;

		// Create report file for the inputs
		FileEntity	inputReportFile;
		String inputReportFileName = InputAgent.getReportDirectory() + InputAgent.getRunName() + ".inp";

		if( FileEntity.fileExists( inputReportFileName ) ) {
			inputReportFile = new FileEntity( inputReportFileName, FileEntity.FILE_WRITE, false );
			inputReportFile.flush();
		}
		else
		{
			inputReportFile = new FileEntity( inputReportFileName, FileEntity.FILE_WRITE, false );
		}

		// Loop through the entity classes
		boolean hasinput = false;		// for formating output
		int count = 0;					// for formating output
		String entityName = null;		// to take out Region name

		// print Define statements
		for( ObjectType type : ObjectType.getAll() ) {
			Class<? extends Entity> each = type.getJavaClass();

			 // Loop through the instances for this entity class
			ArrayList<? extends Entity> cloneList = Entity.getInstancesOf(each);
			count = 0;
	        for( int j=0; j < cloneList.size(); j++ ) {
	        	hasinput = false;

				ent = cloneList.get(j);
			for( Input<?> in : ent.getEditableInputs() ){
        			// If the keyword has been used, then add a record to the report
				if ( in.getValueString().length() != 0 ){
        					hasinput = true;
        					count++;
        					break;
        			}
        		}

        		if ( each.getSimpleName().equalsIgnoreCase("Region") && ! hasinput )
        		{
        			count++;
        			hasinput = true;
        		}

	        	if( hasinput ){
					entityName = cloneList.get(j).getInputName();
	        		if ( (count-1)%5 == 0) {
			        	inputReportFile.putString( "Define" );
		        		inputReportFile.putTab();
						inputReportFile.putString(type.getInputName());
		        		inputReportFile.putTab();
		        		inputReportFile.putString( "{ " + entityName );
		        		inputReportFile.putTab();
		        	}
		        	else if ( (count-1)%5 == 4 ){
						inputReportFile.putString( entityName + " }" );
						inputReportFile.newLine();
		        	}
		        	else {
		        		inputReportFile.putString( entityName );
		        		inputReportFile.putTab();
		        	}
	        	}
	        }

	        if ( cloneList.size() > 0 ){
		        if ( count%5 != 0 ){
		        	inputReportFile.putString(  " }" );
		        	inputReportFile.newLine();
		        }
		        inputReportFile.newLine();
	        }
		}

		for( ObjectType type : ObjectType.getAll() ) {
			Class<? extends Entity> each = type.getJavaClass();

			// Get the list of instances for this entity class
			// sort the list alphabetically
			ArrayList<? extends Entity> cloneList = Entity.getInstancesOf(each);

			// Print the entity class name to the report (in the form of a comment)
			if( cloneList.size() > 0 ) {
				inputReportFile.putString( "\" " + each.getSimpleName() + " \"");
				inputReportFile.newLine();
				inputReportFile.newLine();  // blank line below the class name heading
			}

			Collections.sort(cloneList, new Comparator<Entity>() {
				@Override
				public int compare(Entity a, Entity b) {
					return a.getInputName().compareTo(b.getInputName());
				}
			});

	        // Loop through the instances for this entity class
	        for( int j=0; j < cloneList.size(); j++ ) {

	        	// Make sure the clone is an instance of the class (and not an instance of a subclass)
				if (cloneList.get(j).getClass() == each) {
					ent = cloneList.get(j);
					entityName = cloneList.get(j).getInputName();
	        		hasinput = false;

	        		// Loop through the editable keywords for this instance
				for( Input<?> in : ent.getEditableInputs() ) {
	        			// If the keyword has been used, then add a record to the report
					if ( in.getValueString().length() != 0 ) {

						if ( ! in.getCategory().contains("Graphics") ) {
		        				hasinput = true;
				        		inputReportFile.putTab();
				        		inputReportFile.putString( entityName );
				        		inputReportFile.putTab();
							inputReportFile.putString( in.getKeyword() );
				        		inputReportFile.putTab();
							if( in.getValueString().lastIndexOf( "{" ) > 10 ) {
		        					String[] item1Array;
								item1Array = in.getValueString().trim().split( " }" );

		        					inputReportFile.putString( "{ " + item1Array[0] + " }" );
		        					for (int l = 1; l < (item1Array.length); l++ ){
		        						inputReportFile.newLine();
		        						inputReportFile.putTabs( 5 );
		        						inputReportFile.putString( item1Array[l] + " } " );
		        					}
		        					inputReportFile.putString(  "	}" );
		        				}
		        				else {
								inputReportFile.putString( "{ " + in.getValueString() + " }" );
		        				}
				        		inputReportFile.newLine();
	        				}
	        			}
	        		}
	        		// Put a blank line after each instance
					if ( hasinput ) {
						inputReportFile.newLine();
					}
	        	}
	        }
		}

		// Close out the report
		inputReportFile.flush();
		inputReportFile.close();

	}

	public static void closeLogFile() {
		if (logFile == null)
			return;

		logFile.flush();
		logFile.close();

		if (numErrors ==0 && numWarnings == 0) {
			logFile.delete();
		}
		logFile = null;
	}

	private static final String errPrefix = "*** ERROR *** %s%n";
	private static final String wrnPrefix = "***WARNING*** %s%n";

	public static int numErrors() {
		return numErrors;
	}

	public static int numWarnings() {
		return numWarnings;
	}

	private static void echoInputRecord(ArrayList<String> tokens) {
		StringBuilder line = new StringBuilder();
		for (int i = 0; i < tokens.size(); i++) {
			line.append("  ").append(tokens.get(i));
			if (tokens.get(i).startsWith("\"")) {
				InputAgent.logMessage("%s", line.toString());
				line.setLength(0);
			}
		}

		// Leftover input
		if (line.length() > 0)
			InputAgent.logMessage("%s", line.toString());
	}

	private static void logBadInput(ArrayList<String> tokens, String msg) {
		InputAgent.echoInputRecord(tokens);
		InputAgent.logError("%s", msg);
	}

	public static void logMessage(String fmt, Object... args) {
		String msg = String.format(fmt, args);
		System.out.println(msg);

		if (logFile == null)
			return;

		logFile.write(msg);
		logFile.newLine();
		logFile.flush();
	}

	public static void trace(int indent, Entity ent, String meth, String... text) {
		// Create an indent string to space the lines
		StringBuilder ind = new StringBuilder("");
		for (int i = 0; i < indent; i++)
			ind.append("   ");
		String spacer = ind.toString();

		// Print a TIME header every time time has advanced
		double traceTime = ent.getCurrentTime();
		if (lastTimeForTrace != traceTime) {
			System.out.format(" \nTIME = %.5f\n", traceTime);
			lastTimeForTrace = traceTime;
		}

		// Output the traces line(s)
		System.out.format("%s%s %s\n", spacer, ent.getName(), meth);
		for (String line : text) {
			System.out.format("%s%s\n", spacer, line);
		}

		System.out.flush();
	}

	/*
	 * Log the input to a file, but don't echo it out as well.
	 */
	public static void echoInput(Vector line) {
		// if there is no log file currently, output nothing
		if (logFile == null)
			return;

		StringBuilder msg = new StringBuilder();
		for (Object each : line) {
			msg.append("  ");
			msg.append(each);
		}
		logFile.write(msg.toString());
		logFile.newLine();
		logFile.flush();
	}

	public static void logWarning(String fmt, Object... args) {
		numWarnings++;
		String msg = String.format(fmt, args);
		InputAgent.logMessage(wrnPrefix, msg);
	}

	public static void logError(String fmt, Object... args) {
		numErrors++;
		String msg = String.format(fmt, args);
		InputAgent.logMessage(errPrefix, msg);
	}

	public static void processEntity_Keyword_Value(Entity ent, Input<?> in, String value){
		ArrayList<String> tokens = new ArrayList<String>();
		Parser.tokenize(tokens, value);
		if(! InputAgent.enclosedByBraces(tokens) ) {
			tokens.add(0, "{");
			tokens.add("}");
		}
		Parser.removeComments(tokens);
		tokens.add(0, ent.getInputName());
		tokens.add(1, in.getKeyword());

		Vector data = new Vector(tokens.size());
		data.addAll(tokens);
		InputAgent.processData(ent, data);
	}


	public static void processEntity_Keyword_Value(Entity ent, String keyword, String value){
		Input<?> in = ent.getInput( keyword );
		processEntity_Keyword_Value(ent, in, value);
	}

	public static void updateInput(Entity ent, Input<?> in, StringVector data) {
		if(ent.testFlag(Entity.FLAG_GENERATED))
			return;

		String str = data.toString();
		// reformat input string to be added to keyword
		// strip out "{}" from data to find value
		if( data.size() > 0 ) {
			if (!(data.get(0).equals("{"))) {
				str = str.replaceAll("[{}]", "");
			} else {
				int strLength = str.length();
				str = String.format("{%s}", str.substring(3,strLength-3));
			}
			str = str.replaceAll( "[,]", " " );
			str = str.trim();
		}

		// Takes care of old format, displaying as new format -- appending onto end of record.
		if( in.isAppendable() && ! data.get(0).equals("{") ) {
			str = String.format("%s { %s }",  in.getValueString(), str );
		}

		if(in.isEdited()) {
			ent.setFlag(Entity.FLAG_EDITED);
			sessionEdited = true;
		}
		in.setValueString(str);

	}

	/**
	 * Print out a configuration file with all the edited changes attached
	 */
	public static void printNewConfigurationFileWithName( String fileName ) {

		ArrayList<String> preAddedRecordLines = new ArrayList<String>();
		String configFilePath = FileEntity.getRootDirectory() + System.getProperty( "file.separator" ) + InputAgent.getConfigFileName();
		if( InputAgent.hasAddedRecords() && FileEntity.fileExists( configFilePath ) ) {
			// Store the original configuration file lines up to added records
			try {
				BufferedReader in = new BufferedReader( new FileReader( configFilePath ) );

				String line;
				while ( ( line = in.readLine() ) != null ) {
					if ( line.startsWith( addedRecordMarker ) ) {
						break;
					}
					else {
						preAddedRecordLines.add( line );
					}
				}
				in.close();
			}
			catch ( Exception e ) {
				throw new ErrorException( e );
			}
		}

		FileEntity file = new FileEntity( fileName, FileEntity.FILE_WRITE, false );

		// include the original configuration file
		if (!InputAgent.hasAddedRecords()) {
			file.format( "\" File: %s\n\n", file.getFileName() );
			file.format( "include %s\n\n", InputAgent.getConfigFileName() );
		}
		else {
			for( int i=0; i < preAddedRecordLines.size(); i++ ) {
				String line = preAddedRecordLines.get( i );
				if( line.startsWith( "\" File: " ) ) {
					file.format( "\" File: %s\n", file.getFileName() );
				}
				else {
					file.format("%s\n", line);
				}
			}
		}

		file.format("%s\n", addedRecordMarker);
		addedRecordFound = true;

		// Print changes to simulation
		writeInputsOnFile_ForEntity( file, DisplayEntity.simulation );
		file.format("\n");

		// Determine all the new classes that were created
		ArrayList<Class<? extends Entity>> newClasses = new ArrayList<Class<? extends Entity>>();
		for (int i = 0; i < Entity.getAll().size(); i++) {
			Entity ent = Entity.getAll().get(i);
			if (!ent.testFlag(Entity.FLAG_ADDED))
				continue;

			if (!newClasses.contains(ent.getClass()))
				newClasses.add(ent.getClass());
		}

		// Print the define statements for each new class
		for( Class<? extends Entity> newClass : newClasses ) {
			for (ObjectType o : ObjectType.getAll()) {
				if (o.getJavaClass() == newClass) {
					file.putString( "Define " + o.getInputName()+" {" );
					break;
				}
			}

			for (int i = 0; i < Entity.getAll().size(); i++) {
				Entity ent = Entity.getAll().get(i);
				if (!ent.testFlag(Entity.FLAG_ADDED))
					continue;

				if (ent.getClass() == newClass)
					file.format(" %s ", ent.getInputName());

			}
			file.format("}\n");
		}

		// List all the changes that were saved for each edited entity
		for (int i = 0; i < Entity.getAll().size(); i++) {
			Entity ent = Entity.getAll().get(i);
			if( ent == DisplayEntity.simulation )
				continue;

			if (!ent.testFlag(Entity.FLAG_EDITED))
				continue;

			writeInputsOnFile_ForEntity( file, ent );
		}

		file.flush();
		file.close();
	}

	static void writeInputsOnFile_ForEntity( FileEntity file, Entity ent ) {
		// Write new configuration file for non-appendable keywords
		file.format("\n");
		for( int j=0; j < ent.getEditableInputs().size(); j++ ) {
			Input<?> in = ent.getEditableInputs().get( j );

			if( in.isEdited() ) {

				// Each line starts with the entity name followed by changed keyword
				file.format("%s %s ", ent.getInputName(), in.getKeyword());

				String value = in.getValueString();
				ArrayList<String> tokens = new ArrayList<String>();
				Parser.tokenize(tokens, value);
				if(! InputAgent.enclosedByBraces(tokens) ) {
					value = String.format("{ %s }", value);
				}
				file.format("%s\n", value);
			}
		}
	}

	public static void loadDefault() {
		// Read the default configuration file
		InputAgent.readResource("default.cfg");
		sessionEdited = false;
	}

	/**
	 * Split an input (list of strings) down to a single level of nested braces, this may then be called again for
	 * further nesting.
	 * @param input
	 * @return
	 */
	public static ArrayList<ArrayList<String>> splitForNestedBraces(List<String> input) {
		ArrayList<ArrayList<String>> inputs = new ArrayList<ArrayList<String>>();

		int braceDepth = 0;
		ArrayList<String> currentLine = null;
		for (int i = 0; i < input.size(); i++) {
			if (currentLine == null)
				currentLine = new ArrayList<String>();

			currentLine.add(input.get(i));
			if (input.get(i).equals("{")) {
				braceDepth++;
				continue;
			}

			if (input.get(i).equals("}")) {
				braceDepth--;
				if (braceDepth == 0) {
					inputs.add(currentLine);
					currentLine = null;
					continue;
				}
			}
		}

		return inputs;
	}


}
