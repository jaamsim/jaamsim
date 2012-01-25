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
package com.sandwell.JavaSimulation3D;

import java.awt.FileDialog;
import java.io.BufferedReader;
import java.io.File;
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

import javax.swing.JOptionPane;

import com.sandwell.JavaSimulation.*;
import com.sandwell.JavaSimulation.Package;

public class InputAgent {
	private static final String addedRecordMarker = "\" *** Added Records ***";

	private static int numErrors = 0;
	private static int numWarnings = 0;
	private static FileEntity logFile;

	private static String configFileName;
	private static boolean batchRun;
	private static boolean sessionEdited;
	private static boolean addedRecordFound;
	private static boolean endOfFileReached;		// notes end of cfg files
	// ConfigurationFile load and save variables
	final protected static int SAVE_ONLY = 2;

	// Defaults
	private static final ArrayList<Vector> defaultRecords;		// stores the default strings
	private static boolean defaultsCanBeSet;		// can defaults be changed?

	private static final String INP_ERR_DEFINEUSED = "The name: %s has already been used and is a %s";

	private static String reportDirectory;

	static {
		defaultRecords = new ArrayList<Vector>();
		defaultsCanBeSet = true;
		addedRecordFound = false;
		sessionEdited = false;
		endOfFileReached = false;
		batchRun = false;
		configFileName = "Simulation1.cfg";
		reportDirectory = "";
	}

	public static void clear() {
		logFile = null;
		numErrors = 0;
		numWarnings = 0;
		addedRecordFound = false;
		sessionEdited = false;
		configFileName = "Simulation1.cfg";
		reportDirectory = "";
	}


	public static String getReportDirectory() {
		return reportDirectory;
	}

	public static void setReportDirectory(String dir) {
		reportDirectory = Util.getAbsoluteFilePath(dir);
		if (reportDirectory.substring(reportDirectory.length() - 1) != "\\")
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
		int index = Util.fileShortName( InputAgent.getConfigFileName() ).indexOf( "." );
		String runName;
		if( index > -1 ) {
			runName = Util.fileShortName( InputAgent.getConfigFileName() ).substring( 0, index );
		}
		else {
			runName = Util.fileShortName( InputAgent.getConfigFileName() );
		}
		return runName;
	}

	public static boolean hasAddedRecords() {
		return addedRecordFound;
	}

	public static boolean isSessionEdited() {
		return sessionEdited;
	}

	public static void addEditedEntity(Entity ent) {
		ent.setFlag(Entity.FLAG_EDITED);
		sessionEdited = true;
	}

	public static void setBatch(boolean batch) {
		batchRun = batch;
	}

	/**
	 * Break the record into individual tokens and append it to the token list
	 */
	static void tokenizeString(ArrayList<String> tokens, String record) {
		// Split the record into two pieces, the contents portion and possibly
		// a commented portion
		String[] contents = record.split("\"", 2);

		// Split the contents along single-quoted substring boundaries to allow
		// us to parse quoted and unquoted sections separately
		String[] substring = contents[0].split("'", -1);
		for (int i = 0; i < substring.length; i++) {
			// Odd indices were single-quoted strings in the original record
			// restore the quotes and append the whole string as a single token
			// even if there was nothing between the quotes (an empty string)
			if (i % 2 != 0) {
				tokens.add(String.format("'%s'", substring[i]));
				continue;
			}

			// The even-index strings need tokenizing, we allow spaces, tabs and
			// commas to delimit token boundaries, we also want all braces {} to
			// appear as a single token
			// Ensure { or } has tabs separating if from adjacent characters
			String temp = substring[i].replaceAll("([\\{\\}])", "\t$1\t");
			// Split along space, comma and tab characters, treat consecutive
			// characters as one delimiter
			String[] delimTokens = temp.split("[ ,\t]+", 0);
			// Append the new tokens that have greater than zero length
			for (String each : delimTokens) {
				if (each.length() > 0)
					tokens.add(each);
			}
		}

		// add any comments if they exist with a leading " prepended to denote it
		// as commented
		if (contents.length == 2)
			tokens.add(String.format("\"%s", contents[1]));
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

	private static boolean isRecordComplete(ArrayList<String> tokens) {
		int braceDepth = 0;
		for (int i = 0; i < tokens.size(); i++) {
			String token = tokens.get(i);

			if (token.equals("{"))
				braceDepth++;

			if (token.equals("}"))
				braceDepth--;

			if (braceDepth < 0) {
				InputAgent.logBadInput(tokens, "Extra closing braces found");
				tokens.clear();
				return false;
			}

			if (braceDepth > 2) {
				InputAgent.logBadInput(tokens, "Maximum brace depth (2) exceeded");
				tokens.clear();
				return false;
			}
		}

		if (braceDepth == 0)
			return true;
		else
			return false;
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

			while (true) {
				String line = buf.readLine();
				// end of file, stop reading
				if (line == null)
					break;

				// Set flag if found " *** Added Records ***
				if ( line.trim().equalsIgnoreCase( addedRecordMarker ) ) {
					addedRecordFound = true;
				}

				InputAgent.tokenizeString(record, line);
				if (!InputAgent.isRecordComplete(record))
					continue;

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

	private static void removeComments(ArrayList<String> record) {
		for (int i = record.size() - 1; i >= 0; i--) {
			// remove parts of the input that were commented out
			if (record.get(i).startsWith("\"")) {
				record.remove(i);
				continue;
			}

			// strip single-quoted strings when passing through to the parsers
			if (record.get(i).startsWith("'")) {
				String noQuotes = record.get(i).substring(1, record.get(i).length() - 1);
				record.set(i, noQuotes);
			}
		}
	}

	private static void processRecord(URL url, ArrayList<String> record) {
		//InputAgent.echoInputRecord(record);
		InputAgent.removeComments(record);

		if (record.size() == 0)
			return;

		if (record.get(0).equalsIgnoreCase("INCLUDE")) {
			InputAgent.processIncludeRecord(url, record);
			return;
		}

		if (record.get(0).equalsIgnoreCase("DEFINE")) {
			InputAgent.processDefineRecord(record);
			if (hasAddedRecords()) {
				Vector vec = new Vector();
				vec.addAll( record );

				// remove braces
				Vector braces = new Vector( 2,1 );
				braces.add( "{" );
				braces.add( "}" );
				vec.removeAll( braces );

				InputAgent.processAddedRecordsFromCfgFile(vec);
			}
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
			if( record.get( 1 ).equalsIgnoreCase( "Package" ) ) {
				proto = Package.class;
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
			InputAgent.defineEntity(proto, record.get(i));
		}
	}

	private static void defineEntity(Class<? extends Entity> proto, String key) {
		Region region = null;
		String name = key;

		Entity ent = Input.tryParseEntity(key, Entity.class);
		if (ent != null) {
			InputAgent.logError(INP_ERR_DEFINEUSED, name, ent.getClass().getSimpleName());
			return;
		}

		if (key.contains("/")) {
			String regionName = key.substring(0, key.indexOf("/"));
			name = key.substring(key.indexOf("/") + 1);
			Entity check = Input.tryParseEntity(regionName, Entity.class);
			if (check == null) {
				InputAgent.logError("%s not found, could not define: %s", regionName, name);
				return;
			}
			if (check instanceof Region)
				region = (Region)check;
			else
				name = key;
		}

		ent = null;
		try {
			ent = proto.newInstance();
		}
		catch (InstantiationException e) {}
		catch (IllegalAccessException e) {}
		finally {
			if (ent == null) {
				InputAgent.logError("Could not create new Entity: %s", key);
				return;
			}
		}

		ent.setName(name);
		ent.setInputName(key);
		if (region != null)
			ent.setRegion( region );
		ent.defineNewEntity();
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

	public static void doError(Throwable e) {
		if (!batchRun)
			return;

		System.out.println("An error occurred in the simulation environment.  Please check inputs for an error:");
		System.out.println(e);
		System.exit(1);
	}

	public static void loadConfigFile(String fileName) {
		try {
			InputAgent.loadConfigurationFile(fileName);
			GraphicsUpdateBehavior.forceUpdate = true;
		}
		catch( InputErrorException iee ) {
			if (!batchRun) {
				javax.swing.JOptionPane.showMessageDialog( null, iee.getMessage(), "Input Error", javax.swing.JOptionPane.ERROR_MESSAGE );
			}
			else {
				System.out.println( iee.getMessage() );
			}
			return;
		}
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

		// print inputKeywordfile
		if( DisplayEntity.simulation.getPrintInputReport() ) {
			InputAgent.printInputFileKeywords();
		}
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
			System.exit( 0 );
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

			try {
				// Determine the url of the file from the jar file com/sandwell/JavaSimulation directory
				String relativeURL = FileEntity.getRelativeURL( fileName );

				// Is the file is inside the jar file?
				if (Simulation.class.getResource(relativeURL) != null) {

					// Set the directory of the file inside the jar file relative to com/sandwell/JavaSimulation directory
					int lastIndex = relativeURL.lastIndexOf( '/' );
					String jarFileRootDirectory = relativeURL.substring( 0, lastIndex ).replace( "/", "\\" );
					FileEntity.setJarFileRootDirectory( jarFileRootDirectory );
				}
			}
			catch( Exception e ) {
				throw new ErrorException( "Could not open " + fileName );
			}
		}

		// Initialize the input file
		file.toStart();

		DisplayEntity.simulation.setProgressText( file.getFileName() );

		// For each line in the file
		while( true ) {

			// Read the next line to record
			record = getNextParsedRecord( file );
//			System.out.println( record.toString() );

			// Determine the amount of file read and update the progress gauge
			int per = (int)(((double)file.getNumRead()) / ((double)file.getLength()) * 100.0);
			DisplayEntity.simulation.setProgress( per );

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
		DisplayEntity.simulation.setProgressText( null );
		DisplayEntity.simulation.setProgress( 0 );

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

		// Adjust input and warn about deprecated feature use
		InputAgent.backwardsCompatibility(record);

		try {



			// Check for default
			if( "DEFAULT".equalsIgnoreCase( (String)record.get( 0 ))) {
				if ( defaultsCanBeSet ) {
					record.remove( 0 );
					defaultRecords.add(record);
				} else {
					throw new InputErrorException( "Defaults can only be set before any Define statements" );
				}
			} else if( "DEFINE".equalsIgnoreCase( (String)record.get( 0 ) )  ) {

				// Defaults can only be set prior to any define statements
				defaultsCanBeSet = false;
				InputAgent.processDefine( record );
				if (hasAddedRecords()) {
					InputAgent.processAddedRecordsFromCfgFile( record );
				}
			}
			else if( "STOP".equalsIgnoreCase( (String)record.get( 0 ) )  ) {

			}

			// Process other files
			else if( "INCLUDE".equalsIgnoreCase( (String)record.get( 0 ) )  ) {

				if( record.size() == 2 ) {
					if( FileEntity.fileExists( (String)record.get( 1 ) ) ) {

						// Load the included file and process its records first
						InputAgent.loadConfigurationFile( (String)record.get( 1 ), false );
						DisplayEntity.simulation.setProgressText( file.getFileName() );
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
		}
	}

	/**
	 * Process a 'Define' record from the input file.  record should already be parsed.  Will add defaults.
	 * TODO:add to description -- parsed how?
	 */
	private static void processDefine( Vector record ) {

		Entity newObject;

		if( record.size() < 2 ) {
			InputAgent.logError("Define record does not specify an object");
		}

		Class<? extends Entity> proto = Input.parseEntityType((String)record.get(1));

		// remove braces
		Vector braces = new Vector( 2,1 );
		braces.add( "{" );
		braces.add( "}" );
		record.removeAll( braces );

		// loop through object names to be created
		for ( int i = 2; i < record.size(); i++ ) {

			String item = (String)record.get( i );
			String objectName = "";
			String regionName = "";
			Region region = null;

			if( item.indexOf( "/" ) > -1 ) {
				String[] itemArray = item.split( "/" );
				regionName = itemArray[0];
				objectName = itemArray[1];

				region = Input.tryParseEntity(regionName, Region.class);
				if ( region == null ) {
					InputAgent.logError("Could not find region: %s", regionName);
				}
			}
			else {
				objectName = item;
			}

			// Determine the object key - include the region if it is not ModelStage
			String objectKey;
			if (region != null) {
				objectKey = regionName + "/" + objectName;
			}
			else {
				objectName = item;
				objectKey = item;
			}

			Entity ent = Input.tryParseEntity(objectKey, Entity.class);
			if( ent != null ) {
				InputAgent.logError(INP_ERR_DEFINEUSED, objectKey, ent.getClass().getSimpleName());
				continue;
			}

			// Create the object
			try {
				newObject = proto.newInstance();
			}
			catch (Throwable e) {
				throw new ErrorException("Error instantiating a new object of type: " + proto.getName());
			}
			newObject.setName(objectName);
			newObject.setInputName(item);
			if (region != null) {
				newObject.setRegion(region);
			}
			newObject.defineNewEntity();

			// Set defaults
			for ( int j = 0; j < defaultRecords.size(); j++ ) {
				if (((String)defaultRecords.get(j).get(0)).equalsIgnoreCase( (String)record.get( 1 ) ) ) {
					Vector thisDefaultRecord = new Vector( 1,1 );

					// Add item name
					thisDefaultRecord.add( item );

					// Add defaults
					thisDefaultRecord.addAll(1, defaultRecords.get(j));

					// Remove class
					thisDefaultRecord.remove( 1 );

					// Process data input for objects
					InputAgent.processData(newObject, thisDefaultRecord);
				}
			}

		}
	}

	// Read the next line of the file
	protected static Vector getNextParsedRecord(FileEntity file) {

		Vector record = new Vector( 1 , 1 );

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
				Vector parsedString = new Vector( 1 , 1 );

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
						String stringDump = "";
						// iterate through
						for ( int k = j; k<checkRecord.length(); k++ ) {
							// if a brace is found, end checking this word
							if ( checkRecord.charAt(k) == '{' || checkRecord.charAt(k) == '}' ) {
								k = checkRecord.length();
							}
							// otherwise, make the word
							else {
								stringDump += checkRecord.charAt(k);
							}
						}
						j += stringDump.length() - 1;
						parsedString.add(stringDump);
					}

				}

				// Add brackets as separate entries
				if (parsedString.size() > 0 ) {
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

	private static void processKeyword( Entity entity, StringVector recordCmd, String keyword) {
		if (keyword == null)
			throw new InputErrorException("The keyword is null.");

		if (entity.testFlag(Entity.FLAG_LOCKED))
			throw new InputErrorException("Entity: %s is locked and cannot be modified", entity.getName());

		// Extra processing: records added in the previous session as if they were
		// edited and accepted in the current session
		if (hasAddedRecords()) {
			Vector vec = new Vector();
			vec.add( entity.getInputName() );
			vec.add( keyword );
			vec.addAll( recordCmd );

			InputAgent.processAddedRecordsFromCfgFile(vec);
		}

		try {
			Input<?> input = entity.getInput( keyword );
			if( input != null && input.isAppendable() ) {
				ArrayList<StringVector> splitData = Util.splitStringVectorByBraces(recordCmd);
				for ( int i = 0; i < splitData.size(); i++ ) {
					entity.readInput(splitData.get(i), keyword, false, false);
				}
			}
			else {
				entity.readInput(recordCmd, keyword, false, false);
			}

			entity.updateKeywordValuesForEditBox(keyword, recordCmd);
		}
		catch ( InputErrorException e ) {
			InputAgent.logError("Entity: %s Keyword: %s - %s", entity.getName(), keyword, e.getMessage());
			throw e;
		}
	}

	private static void processAddedRecordsFromCfgFile( Vector record ) {
		if (record.size() < 3)
			return;

		// Expecting record format: Define <entity-class-name> <entity-name>...
		if ("Define".equalsIgnoreCase((String)record.get(0))) {
			for (int i = 2; i < record.size(); i++) {
				String entityName = (String)record.get(i);
				Entity ent = Input.tryParseEntity(entityName, Entity.class);
				ent.setFlag(Entity.FLAG_ADDED);
			}
			return;
		}

		// Process all edited appendable and non-appendable keyword values
		// Expecting record format:
		// <entity-class-name> <entity-name> <keyword> <par> <par> ...
		// NEW format: <entity-name> <keyword> <par> <par> ...
		String entityName = (String)record.get(0);
		Entity ent = Input.tryParseEntity(entityName, Entity.class);
		String keyword = (String)record.get(1);

		StringBuilder keyString = new StringBuilder();
		if (record.size() > 2) {
			keyString.append((String)record.get(2));
		}
		for (int i = 3; i < record.size(); i++) {
			keyString.append(" ").append((String)record.get(i));
		}

		Input<?> in = ent.getInput(keyword);
		in.setEdited(true);
		in.setEditedValueString(keyString.toString());
		ent.setFlag(Entity.FLAG_EDITED);
	}

	public static void processData(Entity ent, Vector rec) {
		if( rec.get( 1 ).toString().trim().equals( "{" ) ) {
			InputAgent.logError("A keyword expected after: %s", ent.getName());
		}
		Vector multiCmds = InputAgent.splitMultipleCommands(rec);

		// Process each command
		for( int i = 0; i < multiCmds.size(); i++ ) {
			Vector cmd = (Vector)multiCmds.get(i);
			StringVector recordCmd = new StringVector(cmd.size() - 1);

			// Omit the object name (first entry) as it is already determined
			for (int j = 1; j < cmd.size(); j++) {
				recordCmd.add((String)cmd.get(j));
			}
			// Allow old-style input to prepend the keyword if necessary
			String keyword = recordCmd.remove(0);

			// Process the record
			InputAgent.processKeyword(ent, recordCmd, keyword);

			// Extra processing: records added in the previous session as if they were
			// edited and accepted in the current session
			if (hasAddedRecords()) {
				InputAgent.processAddedRecordsFromCfgFile( (Vector) multiCmds.get( i ) );
			}
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
	 *  Reformat old style input data into new style input format
	 */
	private static void backwardsCompatibility(Vector record) {

		// If "Define" is in the second position of the record, move it to the beginning
		if ( "DEFINE".equalsIgnoreCase( (String)record.get( 1 ) ) ) {
			InputAgent.logWarning("Syntax is DEFINE <Type> not <Type> DEFINE");
			record.insertElementAt( record.remove( 1 ), 0 );
		}

		// allow compatibility for multiple define names
		if ( "DEFINE".equalsIgnoreCase( (String)record.get( 0 ) ) ) {

			if ( ( (String)record.get( 1 ) ).equalsIgnoreCase("ProductTypes") ) {
				InputAgent.logWarning("Type name is ProductType, not ProductTypes");
				record.setElementAt("ProductType", 1);
			}

			if ( ( (String)record.get( 1 ) ).equalsIgnoreCase("Segment") ) {
				InputAgent.logWarning("Type name is RouteSegment, not Segment");
				record.setElementAt("RouteSegment", 1);
			}

			if ( ( (String)record.get( 1 ) ).equalsIgnoreCase("CharterShips") ) {
				InputAgent.logWarning("Type name is CharteredShipGenerator, not CharterShips");
				record.setElementAt("CharteredShipGenerator", 1);
			}

		} // end define names

		// RunControl records
		if ( ( (String)record.get( 0 ) ).equalsIgnoreCase("RunControl") ) {
			InputAgent.logWarning("Replace RunControl with Simulation");
			// Convert "RunControl ..." to "Simulation ... "
			record.set( 0, "Simulation" );
		}

		// Route records
		if ( ( (String)record.get( 0 ) ).equalsIgnoreCase("Route") ) {
			InputAgent.logWarning("Use RouteManager keyword Route and Heel");
			// Convert "Route <origin> <desination> <seg1> <seg2> ..."
			// to      "RouteManager Route { <origin> <destination> <seg1> <seg2> ... }"
			record.insertElementAt("RouteManager", 0);

			// Convert "Route <origin> <desination> Heel <heel>"
			// to      "RouteManager Heel { <origin> <destination> <heel> }"
			if( record.size() == 6 ) {
				if( record.get( 4 ).equals( "Heel" ) ) {
					record.remove( 4 );
					record.setElementAt( "Heel" , 1 );
				}
			}

			record.insertElementAt( "{", 2 );
			record.insertElementAt( "}", record.size() );
		}

		// Convert "Segment <SegmentName> <keyword> <data>"
		// to      "<SegmentName> <keyword> { <data> }"
		if ( ( (String)record.get( 0 ) ).equalsIgnoreCase("Segment") ) {
			InputAgent.logWarning("No longer required to specify 'Segment', reference the named segment directly");
			record.remove( 0 );
			record.insertElementAt( "{", 2 );
			record.insertElementAt( "}", record.size() );
		}

		// Convert "Fleet <FleetName> <keyword> <data>"
		// to      "<FleetName> <keyword> { <data> }"
		if ( ( (String)record.get( 0 ) ).equalsIgnoreCase("Fleet") ) {
			InputAgent.logWarning("No longer required to specify 'Fleet', reference the named fleet directly");
			record.remove( 0 );
			record.insertElementAt( "{", 2 );
			record.insertElementAt( "}", record.size() );
		}

		// Zone records
		if ( ( (String)record.get( 0 ) ).equalsIgnoreCase("Zone") ) {
			InputAgent.logWarning("No longer required to specify 'Zone', reference the named zone directly");
			// Convert "Zone <ZoneName> <filename>"
			// to      "Zone <ZoneName> FileName <filename>"
			if( FileEntity.fileExists( (String)record.get( 2 ) ) ) {
				InputAgent.logWarning("Use the Zone keyword MetOceanFile");
				record.insertElementAt( "MetoceanFile", 2 );
			}

			// Convert "Zone <ZoneName> <keyword> <data>"
			// to      "<ZoneName> <keyword> { <data> }"
			record.remove( 0 );
			record.insertElementAt( "{", 2 );
			record.insertElementAt( "}", record.size() );
		}

		// Convert "Company <CompanyName> <keyword> <data>"
		// to      "<CompanyName> <keyword> { <data> }"
		if ( ( (String)record.get( 0 ) ).equalsIgnoreCase("Company") ) {
			InputAgent.logWarning("No longer required to specify 'Company', reference the named company directly");
			record.remove( 0 );
			record.insertElementAt( "{", 2 );
			record.insertElementAt( "}", record.size() );
		}

		// Convert "CharterShips <CharteredShipGeneratorName> <keyword> <data>"
		// to      "<CharteredShipGeneratorName> <keyword> { <data> }"
		if ( ( (String)record.get( 0 ) ).equalsIgnoreCase("CharterShips") ) {
			InputAgent.logWarning("No longer required to specify 'CharterShips', reference the named CharteredShipGenerator directly");
			record.remove( 0 );
			record.insertElementAt( "{", 2 );
			record.insertElementAt( "}", record.size() );
		}

		//IceBreaker records
		// needs to be dealt with by IceBreaker Manager
		if ( ( (String)record.get( 0 ) ).equalsIgnoreCase("IceBreaker") ) {
			InputAgent.logWarning("Use the IceBreakerManager keyword IceBreaker");
			record.insertElementAt( "IceBreakerManager", 0 );
			record.insertElementAt( "{", 2 );
			record.insertElementAt( "}", record.size() );
		}

		// allow compatibility for multiple define names
		if ( "DEFINE".equalsIgnoreCase( (String)record.get( 0 ) ) ) {

			// views are regions
			if ( ( (String)record.get( 1 ) ).equalsIgnoreCase("View") ) {
				record.setElementAt("Region", 1);
			}
		}

		// Still common to go View <NameOfView> <Keyword>
		if ( ( (String)record.get( 0 ) ).equalsIgnoreCase("View") ) {
			InputAgent.logWarning("No longer required to specify 'View', reference the named view directly");
			record.remove( 0 );
		}
	}

	/**
	 * returns a vector of vectors
	 * each vector will be of form <obj-name> <kwd> <data> <data>
	 * no braces are returned
	 */
	private static Vector splitMultipleCommands( Vector record ) {

		// SUPPORTED SYNTAX:
		//
		//   <obj-name> <kwd> { <par> }
		//   <obj-name> <kwd> { <par> <par> ... }
		//   <obj-name> <kwd> { <par> <par> ... } <kwd> { <par> <par> ... } ...
		//   <obj-name> <kwd> <par> <kwd> { <par> <par> ... } ...
		Vector multiCmds = new Vector();
		int noOfUnclosedBraces = 0;
		String itemObject = (String)record.remove(0);

		// Loop through the keywords and assemble new commands
		while( record.size() > 0 ) {

			// Enter the class, object, and keyword in the new command
			Vector cmd = new Vector();

			// Class and object are constant
			//cmd.add( itemClass );
			cmd.add( itemObject );

			// Keyword changes as loop proceeds
			cmd.add( record.remove( 0 ) );


			// For a command of the new form "<obj-name> <file-name>", record
			// will be empty here.
			if( !record.isEmpty() ) {

				// If there is an opening brace, then the keyword has a list of
				// parameters
				String openingBrace = (String)record.get( 0 );
				if( openingBrace.equals("{") ) {
					noOfUnclosedBraces ++ ;
					record.remove( 0 ); // throw out opening brace {

					// Iterate through record
					while( (record.size() > 0) && ( noOfUnclosedBraces > 0 ) ) {
						if ( record.get(0).equals("{") )
							noOfUnclosedBraces ++ ;
						else if (record.get(0).equals("}"))
							noOfUnclosedBraces -- ;
						cmd.add( record.remove( 0 ) );
					}

					if( ( record.size() == 0 ) && ( noOfUnclosedBraces != 0) ) { // corresponding "}" is missing
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
					cmd.add( record.remove( 0 ) );
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
		public boolean accept(File inFile, String fileName) {
			return fileName.endsWith("[cC][fF][gG]");
		}
	}

	public static void load() {
		System.out.println("Loading...");

		FileDialog chooser = new FileDialog( DisplayEntity.simulation.getGUIFrame() , "Load Configuration File", FileDialog.LOAD );
		chooser.setFilenameFilter(new ConfigFileFilter());
		String chosenFileName = chooseFile(chooser, FileDialog.LOAD);
		if (chosenFileName != null) {
			//dispose();
			setLoadFile(chosenFileName);
		} else {
			//dispose();
		}
	}

	public static void save() {
		System.out.println("Saving...");
		setSaveFile( FileEntity.getRootDirectory() + System.getProperty( "file.separator" ) + InputAgent.getConfigFileName(), SAVE_ONLY );
	}

	public static void saveAs() {
		System.out.println("Save As...");
		FileDialog chooser = new FileDialog( DisplayEntity.simulation.getGUIFrame(), "Save Configuration File As", FileDialog.SAVE );
		chooser.setFilenameFilter(new ConfigFileFilter());
		String chosenFileName = chooseFile(chooser, FileDialog.SAVE);
		if ( chosenFileName != null ) {
			//dispose();
			setSaveFile( chosenFileName, FileDialog.SAVE );
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

	/**
	 *  Loads configuration file , calls GraphicSimulation.configure() method
	 */
	private static void setLoadFile(String fileName) {
		final String chosenFileName = fileName;
		new Thread(new Runnable() {
			public void run() {
				File temp = new File(chosenFileName);

				if( temp.isAbsolute() ) {
					FileEntity.setRootDirectory( temp.getParentFile() );
					DisplayEntity.simulation.configure(temp.getName());
				}
				else {
					DisplayEntity.simulation.configure(chosenFileName);
				}
				FrameBox.valueUpdate();
			}
		}).start();
	}

	/**
	 *  saves the cfg/pos file.  checks for 'save' and 'save as', recursively goes to 'save as' if 'save' is not possible.
	 *  updates runname and filename of file.
	 *  if editbox is open and unaccepted, accepts changes.
	 */
	private static void setSaveFile(String fileName, int saveOrLoadType) {
		String configFilePath = FileEntity.getRootDirectory() + System.getProperty( "file.separator" ) + InputAgent.getConfigFileName();

		//		userOption = 0 for YES save file, userOption = 1 for NO do not save file
		int userOption = 0;

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
						InputAgent.saveAs();
						return;
					}
				}
				else {
					InputAgent.saveAs();
					return;
				}

			} else if ( saveOrLoadType == SAVE_ONLY) {
				System.out.println("Saving...");
			}
		}

		// set root directory
		FileEntity.setRootDirectory( temp.getParentFile() );

		if ( userOption == 0 ) {

			//saveFile = new FileEntity( fileName, FileEntity.FILE_WRITE, false );
			//simulation.printNewConfigurationFileOn( saveFile );
			DisplayEntity.simulation.printNewConfigurationFileWithName( fileName );
			addedRecordFound = true;
			sessionEdited = false;

			//TODOalan set directory of model.. ?
			InputAgent.setConfigFileName(Util.fileShortName(fileName));

			// Set the title bar to match the new run name
			DisplayEntity.simulation.getGUIFrame().setTitle( DisplayEntity.simulation.getModelName() + " - " + InputAgent.getRunName() );

			// close the window
			//dispose();
		}
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
			ArrayList<? extends Entity> cloneList = Simulation.getInstancesOf(each);
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

        		if ( each.getSimpleName().equalsIgnoreCase("Region") && hasinput == false )
        		{
        			count++;
        			hasinput = true;
        		}

	        	if( hasinput ){
					entityName = cloneList.get(j).getInputName();
	        		if ( (count-1)%5 == 0) {
			        	inputReportFile.putString( "Define" );
		        		inputReportFile.putTab();
						inputReportFile.putString(each.getSimpleName());
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
			ArrayList<? extends Entity> cloneList = Simulation.getInstancesOf(each);

			// Print the entity class name to the report (in the form of a comment)
			if( cloneList.size() > 0 ) {
				inputReportFile.putString( "\" " + each.getSimpleName() + " \"");
				inputReportFile.newLine();
				inputReportFile.newLine();  // blank line below the class name heading
			}

			Collections.sort(cloneList, new Comparator<Entity>() {
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
			logFile = null;
		}
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
		InputAgent.tokenizeString(tokens, value);
		if(! InputAgent.enclosedByBraces(tokens) ) {
			tokens.add(0, "{");
			tokens.add("}");
		}
		tokens.add(0, ent.getInputName());
		tokens.add(1, in.getKeyword());

		Vector data = new Vector(tokens.size());
		data.addAll(tokens);
		InputAgent.processData(ent, data);
		InputAgent.addEditedEntity(ent);
		in.setEdited(true);
	}


	public static void processEntity_Keyword_Value(Entity ent, String keyword, String value){
		Input<?> in = ent.getInput( keyword );
		processEntity_Keyword_Value(ent, in, value);
	}
}
