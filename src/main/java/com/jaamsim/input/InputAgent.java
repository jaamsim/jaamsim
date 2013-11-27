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
	private static boolean recordEdits;
	// ConfigurationFile load and save variables
	final protected static int SAVE_ONLY = 2;

	private static final String INP_ERR_DEFINEUSED = "The name: %s has already been used and is a %s";

	private static String reportDirectory;

	static {
		addedRecordFound = false;
		sessionEdited = false;
		batchRun = false;
		configFileName = null;
		reportDirectory = "";
		lastTimeForTrace = -1.0d;
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

	public static boolean recordEdits() {
		return recordEdits;
	}

	public static void setRecordEdits(boolean b) {
		recordEdits = b;
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

	private static URI resRoot;
	private static URI resPath;
	private static final String res = "/resources/";

	static {

		try {
			// locate the resource folder, and create
			resRoot = InputAgent.class.getResource(res).toURI();
		}
		catch (URISyntaxException e) {}

		resPath = URI.create(resRoot.toString());
	}

	private static void rethrowWrapped(Exception ex) {
		StringBuilder causedStack = new StringBuilder();
		for (StackTraceElement elm : ex.getStackTrace())
			causedStack.append(elm.toString()).append("\n");
		throw new InputErrorException("Caught exception: %s", ex.getMessage() + "\n" + causedStack.toString());
	}

	public static final void readResource(String res) {
		if (res == null)
			return;

		try {
			readStream(resRoot.toString(), resPath, res);
			GUIFrame.instance().setProgressText(null);
		}
		catch (URISyntaxException ex) {
			rethrowWrapped(ex);
		}

	}

	public static final boolean readStream(String root, URI path, String file) throws URISyntaxException {
		String shortName = file.substring(file.lastIndexOf('/') + 1, file.length());
		GUIFrame.instance().setProgressText(shortName);
		URI resolved = getFileURI(path, file, root);

		String resolvedPath = resolved.getSchemeSpecificPart();
		String currentDir = resolvedPath.substring(0, resolvedPath.lastIndexOf('/') + 1);

		String oldRoot = FileEntity.getRootDirectory();
		FileEntity.setRootDirectory(currentDir);

		URL url = null;
		try {
			url = resolved.normalize().toURL();
		}
		catch (MalformedURLException e) {
			rethrowWrapped(e);
		}

		if (url == null) {
			InputAgent.logWarning("Unable to resolve path %s%s - %s", root, path.toString(), file);
			return false;
		}

		BufferedReader buf = null;
		try {
			InputStream in = url.openStream();
			buf = new BufferedReader(new InputStreamReader(in));
		} catch (IOException e) {
			InputAgent.logWarning("Could not read from %s", url.toString());
			return false;
		}

		try {
			ArrayList<String> record = new ArrayList<String>();
			int braceDepth = 0;

			Input.ParseContext pc = new Input.ParseContext();
			pc.jail = root;
			pc.context = path;

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
				if (record.size() == 0)
					continue;

				if ("DEFINE".equalsIgnoreCase(record.get(0))) {
					InputAgent.processDefineRecord(record);
					record.clear();
					continue;
				}

				if ("INCLUDE".equalsIgnoreCase(record.get(0))) {
					try {
						InputAgent.processIncludeRecord(pc.jail, resolved, record);
					}
					catch (URISyntaxException ex) {
						rethrowWrapped(ex);
					}
					record.clear();
					continue;
				}

				// Otherwise assume it is a Keyword record
				InputAgent.processKeywordRecord(record, pc);
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

		FileEntity.setRootDirectory(oldRoot);

		return true;
	}

	private static void processIncludeRecord(String root, URI path, ArrayList<String> record) throws URISyntaxException {
		if (record.size() != 2) {
			InputAgent.logError("Bad Include record, should be: Include <File>");
			return;
		}
		InputAgent.readStream(root, path, record.get(1).replaceAll("\\\\", "/"));
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

	public static void processKeywordRecord(ArrayList<String> record, Input.ParseContext context) {
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
				InputAgent.processKeyword(ent, args, key, context);
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

			if (currentLine.size() == 1 && !input.get(i).equals("{")) {
				// Old style brace-free input
				InputAgent.logWarning("Input detected without braces: %s - %s", currentLine.get(0), input.get(i));
				currentLine.add("{");
				currentLine.add(input.get(i));
				currentLine.add("}");
				inputs.add(currentLine);
				currentLine = null;
				continue;
			}

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
	public static void loadConfigurationFile( String fileName) throws URISyntaxException {

		String inputTraceFileName = InputAgent.getRunName() + ".log";
		// Initializing the tracing for the model
		try {
			System.out.println( "Creating trace file" );

			URI confURI = new File(fileName).toURI();
			URI logURI = confURI.resolve(new URI(null, inputTraceFileName, null)); // The new URI here effectively escapes the file name

			// Set and open the input trace file name
			logFile = new FileEntity( logURI.getPath(), FileEntity.FILE_WRITE, false );
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

		if (Simulation.getPrintInputReport())
			InputAgent.printInputFileKeywords();
	}

	/**
	 *
	 * @param fileName
	 * @param firstTime ( true => this is the main config file (run file);  false => this is an included file within  main config file or another included file )
	 */
	public static void loadConfigurationFile( String rawFileName, boolean firstTime ) throws URISyntaxException {

		URI fileURI = new File(rawFileName).toURI();

		String path = fileURI.getPath();

		String dir = path.substring(0, path.lastIndexOf('/')+1);
		URI dirURI = new URI("file", dir, null);
		String fileName = path.substring(path.lastIndexOf('/') + 1, path.length());

		readStream("", dirURI, fileName);

		FileEntity.setRootDirectory(dir);

		GUIFrame.instance().setProgressText(null);
		GUIFrame.instance().setProgress(0);

	}

	public static final void apply(Entity ent, Input<?> in, StringVector data, Input.ParseContext context) {
		in.parse(data, context);

		// Only mark the keyword edited if we have finished initial configuration
		if (InputAgent.hasAddedRecords() || InputAgent.recordEdits())
			in.setEdited(true);

		ent.updateForInput(in);
	}

	public static final void apply(Entity ent, StringVector data, String keyword, Input.ParseContext context)
	throws InputErrorException {
		Input<?> in = ent.getInput(keyword);
		if (in != null) {
			InputAgent.apply(ent, in, data, context);
			FrameBox.valueUpdate();
		} else {
			ent.readData_ForKeyword(data, keyword);
			FrameBox.valueUpdate();
		}
	}

	private static void processKeyword( Entity entity, StringVector recordCmd, String keyword, Input.ParseContext context) {
		if (keyword == null)
			throw new InputErrorException("The keyword is null.");

		if (entity.testFlag(Entity.FLAG_LOCKED))
			throw new InputErrorException("Entity: %s is locked and cannot be modified", entity.getName());

		try {
			Input<?> input = entity.getInput( keyword );
			if( input != null && input.isAppendable() ) {
				ArrayList<StringVector> splitData = Util.splitStringVectorByBraces(recordCmd);
				for ( int i = 0; i < splitData.size(); i++ ) {
					InputAgent.apply(entity, input, splitData.get(i), context);
				}
			}
			else {
				InputAgent.apply(entity, recordCmd, keyword, context);
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
		chooser.setFile("*.cfg");

		chooser.setVisible(true); // display the dialog, waits for selection

		String file = chooser.getFile();
		if (file == null)
			return;

		String absFile = chooser.getDirectory() + file;
		absFile = absFile.trim();
		setLoadFile(gui, absFile);
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
		chooser.setFile(InputAgent.getConfigFileName());

		chooser.setVisible(true); // display the dialog, waits for selection

		String file = chooser.getFile();
		if (file == null)
			return;

		String absFile = chooser.getDirectory() + file;
		absFile = absFile.trim();
		setSaveFile(gui, absFile, FileDialog.SAVE);
	}

	public static void configure(GUIFrame gui, String configFileName) {
		try {
			gui.clear();
			InputAgent.setConfigFileName(configFileName);
			gui.updateForSimulationState(GUIFrame.SIM_STATE_UNCONFIGURED);

			try {
				InputAgent.loadConfigurationFile(configFileName);
			}
			catch( InputErrorException iee ) {
				if (!batchRun)
					ExceptionBox.instance().setError(iee);
				else
					System.out.println( iee.getMessage() );
			}

			System.out.println("Configuration File Loaded");

			// show the present state in the user interface
			gui.setTitle( Simulation.getModelName() + " - " + InputAgent.getRunName() );
			gui.updateForSimulationState(GUIFrame.SIM_STATE_CONFIGURED);
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
					InputAgent.configure(gui, chosenFileName);
				}
				else {
					System.out.printf("Error: loading a relative file: %s\n", chosenFileName);
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

		// Loop through the entity classes printing Define statements
		for (ObjectType type : ObjectType.getAll()) {
			Class<? extends Entity> each = type.getJavaClass();

			// Loop through the instances for this entity class
			ArrayList<? extends Entity> cloneList = Entity.getInstancesOf(each);
			int count = 0;
			for (int j = 0; j < cloneList.size(); j++) {
				boolean hasinput = false;

				Entity ent = cloneList.get(j);
				for (Input<?> in : ent.getEditableInputs()) {
					// If the keyword has been used, then add a record to the report
					if (in.getValueString().length() != 0) {
						hasinput = true;
						count++;
						break;
					}
				}

				if (hasinput) {
					String entityName = ent.getInputName();
					if ((count - 1) % 5 == 0) {
						inputReportFile.putString("Define");
						inputReportFile.putTab();
						inputReportFile.putString(type.getInputName());
						inputReportFile.putTab();
						inputReportFile.putString("{ " + entityName);
						inputReportFile.putTab();
					}
					else if ((count - 1) % 5 == 4) {
						inputReportFile.putString(entityName + " }");
						inputReportFile.newLine();
					}
					else {
						inputReportFile.putString(entityName);
						inputReportFile.putTab();
					}
				}
			}

			if (cloneList.size() > 0) {
				if (count % 5 != 0) {
					inputReportFile.putString(" }");
					inputReportFile.newLine();
				}
				inputReportFile.newLine();
			}
		}

		for (ObjectType type : ObjectType.getAll()) {
			Class<? extends Entity> each = type.getJavaClass();

			// Get the list of instances for this entity class
			// sort the list alphabetically
			ArrayList<? extends Entity> cloneList = Entity.getInstancesOf(each);

			// Print the entity class name to the report (in the form of a comment)
			if (cloneList.size() > 0) {
				inputReportFile.putString("\" " + each.getSimpleName() + " \"");
				inputReportFile.newLine();
				inputReportFile.newLine(); // blank line below the class name heading
			}

			Collections.sort(cloneList, new Comparator<Entity>() {
				@Override
				public int compare(Entity a, Entity b) {
					return a.getInputName().compareTo(b.getInputName());
				}
			});

			// Loop through the instances for this entity class
			for (int j = 0; j < cloneList.size(); j++) {

				// Make sure the clone is an instance of the class (and not an instance of a subclass)
				if (cloneList.get(j).getClass() == each) {
					Entity ent = cloneList.get(j);
					String entityName = ent.getInputName();
					boolean hasinput = false;

					// Loop through the editable keywords for this instance
					for (Input<?> in : ent.getEditableInputs()) {
						// If the keyword has been used, then add a record to the report
						if (in.getValueString().length() != 0) {

							if (!in.getCategory().contains("Graphics")) {
								hasinput = true;
								inputReportFile.putTab();
								inputReportFile.putString(entityName);
								inputReportFile.putTab();
								inputReportFile.putString(in.getKeyword());
								inputReportFile.putTab();
								if (in.getValueString().lastIndexOf("{") > 10) {
									String[] item1Array;
									item1Array = in.getValueString().trim().split(" }");

									inputReportFile.putString("{ " + item1Array[0] + " }");
									for (int l = 1; l < (item1Array.length); l++) {
										inputReportFile.newLine();
										inputReportFile.putTabs(5);
										inputReportFile.putString(item1Array[l] + " } ");
									}
									inputReportFile.putString("	}");
								}
								else {
									inputReportFile.putString("{ " + in.getValueString() + " }");
								}
								inputReportFile.newLine();
							}
						}
					}
					// Put a blank line after each instance
					if (hasinput) {
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

		InputAgent.processKeywordRecord(tokens, null);
	}


	public static void processEntity_Keyword_Value(Entity ent, String keyword, String value){
		Input<?> in = ent.getInput( keyword );
		processEntity_Keyword_Value(ent, in, value);
	}

	public static void updateInput(Entity ent, Input<?> in, StringVector data) {
		if(ent.testFlag(Entity.FLAG_GENERATED))
			return;

		StringBuilder out = new StringBuilder(data.size() * 6);
		for (int i = 0; i < data.size(); i++) {
			String dat = data.get(i);
			if (Parser.needsQuoting(dat) && !dat.equals("{") && !dat.equals("}"))
				out.append("'").append(dat).append("'");
			else
				out.append(dat);

			if( i < data.size() - 1 )
				out.append("  ");
		}
		String str = out.toString();

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
			file.format( "\" File: %s%n%n", file.getFileName() );
			file.format( "include %s%n%n", InputAgent.getConfigFileName() );
		}
		else {
			for( int i=0; i < preAddedRecordLines.size(); i++ ) {
				String line = preAddedRecordLines.get( i );
				if( line.startsWith( "\" File: " ) ) {
					file.format( "\" File: %s%n", file.getFileName() );
				}
				else {
					file.format("%s%n", line);
				}
			}
		}

		file.format("%s%n", addedRecordMarker);
		addedRecordFound = true;

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
					file.format("Define %s {", o.getInputName());
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
			file.format("}%n");
		}

		// List all the changes that were saved for each edited entity
		for (int i = 0; i < Entity.getAll().size(); i++) {
			Entity ent = Entity.getAll().get(i);
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
			if (!in.isEdited())
				continue;

			String value = in.getValueString();
			ArrayList<String> tokens = new ArrayList<String>();
			Parser.tokenize(tokens, value);
			if (!InputAgent.enclosedByBraces(tokens))
				file.format("%s %s { %s }%n", ent.getInputName(), in.getKeyword(), value);
			else
				file.format("%s %s %s%n", ent.getInputName(), in.getKeyword(), value);
		}
	}

	public static void loadDefault() {
		// Read the default configuration file
		InputAgent.readResource("inputs/default.cfg");
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

	/**
	 * This is the heart of path handling, find a file relative to a root 'context' and then check that
	 * the normalized URI matches the jail prefix, otherwise reject it
	 * @param context
	 * @param path
	 * @param jailPrefix
	 * @return
	 */
	public static URI getFileURI(URI context, String path, String jailPrefix) throws URISyntaxException {
		int openBrace = path.indexOf('<');
		int closeBrace = path.indexOf('>');
		int firstSlash = path.indexOf('/');
		URI ret = null;
		if (openBrace == 0 && closeBrace != -1 && firstSlash == closeBrace + 1) {
			// Special path format, expand the resource
			String specPath = path.substring(openBrace + 1, closeBrace);
			if (specPath.equals("res")) {
				ret = new URI(null, resRoot.toString() + path.substring(closeBrace+2), null).normalize();
			}
		} else {
			URI pathURI = new URI(null, path, null).normalize();

			if (context != null) {
				if (context.isOpaque()) {
					// Things are going to get messy in here
					URI schemeless = new URI(null, context.getSchemeSpecificPart(), null);
					URI resolved = schemeless.resolve(pathURI).normalize();
					ret = new URI(context.getScheme(), resolved.toString(), null);
				} else {
					ret = context.resolve(pathURI).normalize();
				}
			} else {
				// We have no context, so hope the URI is absolute or otherwise openable
				ret = pathURI;
			}
		}

		if (jailPrefix != null && ret.toString().indexOf(jailPrefix) != 0) {
			System.out.printf("Failed jail test: %s in jail: %s context: %s\n", ret.toString(), jailPrefix, context.toString());
			return null; // This resolved URI is not in our jail
		}

		return ret;
	}
}
