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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
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

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;

import com.jaamsim.basicsim.ErrorException;
import com.jaamsim.input.Input.ParseContext;
import com.jaamsim.ui.ExceptionBox;
import com.jaamsim.ui.FrameBox;
import com.jaamsim.ui.LogBox;
import com.sandwell.JavaSimulation.Entity;
import com.sandwell.JavaSimulation.FileEntity;
import com.sandwell.JavaSimulation.Group;
import com.sandwell.JavaSimulation.ObjectType;
import com.sandwell.JavaSimulation.Simulation;
import com.sandwell.JavaSimulation3D.GUIFrame;

public class InputAgent {
	private static final String recordEditsMarker = "RecordEdits";

	private static int numErrors = 0;
	private static int numWarnings = 0;
	private static FileEntity logFile;

	private static double lastTimeForTrace;

	private static File configFile;           // present configuration file
	private static boolean batchRun;
	private static boolean sessionEdited;     // TRUE if any inputs have been changed after loading a configuration file
	private static boolean recordEditsFound;  // TRUE if the "RecordEdits" marker is found in the configuration file
	private static boolean recordEdits;       // TRUE if input changes are to be marked as edited.

	private static final String INP_ERR_DEFINEUSED = "The name: %s has already been used and is a %s";

	private static File reportDir;

	static {
		recordEditsFound = false;
		sessionEdited = false;
		batchRun = false;
		configFile = null;
		reportDir = null;
		lastTimeForTrace = -1.0d;
	}

	public static void clear() {
		logFile = null;
		numErrors = 0;
		numWarnings = 0;
		recordEditsFound = false;
		sessionEdited = false;
		configFile = null;
		reportDir = null;
		lastTimeForTrace = -1.0d;
		setReportDirectory(null);
	}

	private static String getReportDirectory() {
		if (reportDir != null)
			return reportDir.getPath() + File.separator;

		if (configFile != null)
			return configFile.getParentFile().getPath() + File.separator;

		return null;
	}

	public static String getReportFileName(String name) {
		return getReportDirectory() + name;
	}

	public static void setReportDirectory(File dir) {
		reportDir = dir;
	}

	public static void prepareReportDirectory() {
		if (reportDir != null) reportDir.mkdirs();
	}

	/**
	 * Sets the present configuration file.
	 *
	 * @param file - the present configuration file.
	 */
	public static void setConfigFile(File file) {
		configFile = file;
	}

	/**
	 * Returns the present configuration file.
	 * <p>
	 * Null is returned if no configuration file has been loaded or saved yet.
	 * <p>
	 * @return the present configuration file.
	 */
	public static File getConfigFile() {
		return configFile;
	}

	/**
	 * Returns the name of the simulation run.
	 * <p>
	 * For example, if the configuration file name is "case1.cfg", then the
	 * run name is "case1".
	 * <p>
	 * @return the name of simulation run.
	 */
	public static String getRunName() {

		if( InputAgent.getConfigFile() == null )
			return "";

		String name = InputAgent.getConfigFile().getName();
		int index = name.lastIndexOf( "." );
		if( index == -1 )
			return name;

		return name.substring( 0, index );
	}

	/**
	 * Specifies whether a RecordEdits marker was found in the present configuration file.
	 *
	 * @param bool - TRUE if a RecordEdits marker was found.
	 */
	public static void setRecordEditsFound(boolean bool) {
		recordEditsFound = bool;
	}

	/**
	 * Indicates whether a RecordEdits marker was found in the present configuration file.
	 *
	 * @return - TRUE if a RecordEdits marker was found.
	 */
	public static boolean getRecordEditsFound() {
		return recordEditsFound;
	}

	/**
	 * Returns the "RecordEdits" mode for the InputAgent.
	 * <p>
	 * When RecordEdits is TRUE, any model inputs that are changed and any objects that
	 * are defined are marked as "edited". When FALSE, model inputs and object
	 * definitions are marked as "unedited".
	 * <p>
	 * RecordEdits mode is used to determine the way JaamSim saves a configuration file
	 * through the graphical user interface. Object definitions and model inputs
	 * that are marked as unedited will be copied exactly as they appear in the original
	 * configuration file that was first loaded.  Object definitions and model inputs
	 * that are marked as edited will be generated automatically by the program.
	 *
	 * @return the RecordEdits mode for the InputAgent.
	 */
	public static boolean recordEdits() {
		return recordEdits;
	}

	/**
	 * Sets the "RecordEdits" mode for the InputAgent.
	 * <p>
	 * When RecordEdits is TRUE, any model inputs that are changed and any objects that
	 * are defined are marked as "edited". When FALSE, model inputs and object
	 * definitions are marked as "unedited".
	 * <p>
	 * RecordEdits mode is used to determine the way JaamSim saves a configuration file
	 * through the graphical user interface. Object definitions and model inputs
	 * that are marked as unedited will be copied exactly as they appear in the original
	 * configuration file that was first loaded.  Object definitions and model inputs
	 * that are marked as edited will be generated automatically by the program.
	 *
	 * @param b - boolean value for the RecordEdits mode
	 */
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

			if (braceDepth > 3) {
				InputAgent.logBadInput(tokens, "Maximum brace depth (3) exceeded");
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

		URL url = null;
		try {
			url = resolved.normalize().toURL();
		}
		catch (MalformedURLException e) {
			rethrowWrapped(e);
		}

		if (url == null) {
			InputAgent.logError("Unable to resolve path %s%s - %s", root, path.toString(), file);
			return false;
		}

		BufferedReader buf = null;
		try {
			InputStream in = url.openStream();
			buf = new BufferedReader(new InputStreamReader(in));
		} catch (IOException e) {
			InputAgent.logError("Could not read from %s", url.toString());
			return false;
		}

		try {
			ArrayList<String> record = new ArrayList<String>();
			int braceDepth = 0;

			Input.ParseContext pc = new Input.ParseContext();
			pc.jail = root;
			pc.context = resolved;

			while (true) {
				String line = buf.readLine();
				// end of file, stop reading
				if (line == null)
					break;

				int previousRecordSize = record.size();
				Parser.tokenize(record, line, true);
				braceDepth = InputAgent.getBraceDepth(record, braceDepth, previousRecordSize);
				if( braceDepth != 0 )
					continue;

				if (record.size() == 0)
					continue;

				InputAgent.echoInputRecord(record);

				if ("DEFINE".equalsIgnoreCase(record.get(0))) {
					InputAgent.processDefineRecord(record);
					record.clear();
					continue;
				}

				if ("INCLUDE".equalsIgnoreCase(record.get(0))) {
					try {
						InputAgent.processIncludeRecord(pc, record);
					}
					catch (URISyntaxException ex) {
						rethrowWrapped(ex);
					}
					record.clear();
					continue;
				}

				if ("RECORDEDITS".equalsIgnoreCase(record.get(0))) {
					InputAgent.setRecordEditsFound(true);
					InputAgent.setRecordEdits(true);
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

		return true;
	}

	private static void processIncludeRecord(ParseContext pc, ArrayList<String> record) throws URISyntaxException {
		if (record.size() != 2) {
			InputAgent.logError("Bad Include record, should be: Include <File>");
			return;
		}
		InputAgent.readStream(pc.jail, pc.context, record.get(1).replaceAll("\\\\", "/"));
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
			if( record.get( 1 ).equalsIgnoreCase( "ObjectType" ) ) {
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
			InputAgent.defineEntity(proto, record.get(i), InputAgent.recordEdits());
		}
	}

	public static <T extends Entity> T defineEntityWithUniqueName(Class<T> proto, String key, boolean addedEntity) {
		return InputAgent.defineEntityWithUniqueName(proto, key, "-", addedEntity);
	}

	/**
	 * Like defineEntity(), but will generate a unique name if a name collision exists
	 * @param proto
	 * @param key
	 * @param sep
	 * @param addedEntity
	 * @return
	 */
	public static <T extends Entity> T defineEntityWithUniqueName(Class<T> proto, String key, String sep, boolean addedEntity) {

		// Has the provided name been used already?
		if (Entity.getNamedEntity(key) == null) {
			return defineEntity(proto, key, addedEntity);
		}

		// Try the provided name plus "1", "2", etc. until an unused name is found
		int entityNum = 1;
		while(true) {
			String name = String.format("%s%s%d", key, sep, entityNum);
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

		if (key.contains(" ") || key.contains("\t") || key.contains("{") || key.contains("}")) {
			InputAgent.logError("Entity names cannot contain spaces, tabs, { or }: %s", key);
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

		// Validate the tokens have the Entity Keyword { Args... } Keyword { Args... }
		ArrayList<KeywordIndex> words = InputAgent.getKeywords(record, context);
		for (KeywordIndex keyword : words) {
			try {
				InputAgent.processKeyword(ent, keyword);
			}
			catch (Throwable e) {
				InputAgent.logInpError("Entity: %s, Keyword: %s - %s", ent.getInputName(), keyword.keyword, e.getMessage());
			}
		}
	}

	private static ArrayList<KeywordIndex> getKeywords(ArrayList<String> input, ParseContext context) {
		ArrayList<KeywordIndex> ret = new ArrayList<KeywordIndex>();

		int braceDepth = 0;
		int keyWordIdx = 1;
		for (int i = 1; i < input.size(); i++) {
			String tok = input.get(i);
			if ("{".equals(tok)) {
				braceDepth++;
				continue;
			}

			if ("}".equals(tok)) {
				braceDepth--;
				if (braceDepth == 0) {
					// validate keyword form
					String keyword = input.get(keyWordIdx);
					if (keyword.equals("{") || keyword.equals("}") || !input.get(keyWordIdx + 1).equals("{"))
						throw new InputErrorException("The input for a keyword must be enclosed by braces. Should be <keyword> { <args> }");

					ret.add(new KeywordIndex(input, keyword, keyWordIdx + 2, i, context));
					keyWordIdx = i + 1;
					continue;
				}
			}
		}

		if (keyWordIdx != input.size())
			throw new InputErrorException("The input for a keyword must be enclosed by braces. Should be <keyword> { <args> }");

		return ret;
	}

	public static void doError(Throwable e) {
		if (!batchRun)
			return;

		LogBox.logLine("An error occurred in the simulation environment.  Please check inputs for an error:");
		LogBox.logLine(e.toString());
		GUIFrame.shutdown(1);
	}

	// Load the run file
	public static void loadConfigurationFile( File file) throws URISyntaxException {

		String inputTraceFileName = InputAgent.getRunName() + ".log";
		// Initializing the tracing for the model
		try {
			System.out.println( "Creating trace file" );

			URI confURI = file.toURI();
			URI logURI = confURI.resolve(new URI(null, inputTraceFileName, null)); // The new URI here effectively escapes the file name

			// Set and open the input trace file name
			logFile = new FileEntity( logURI.getPath());
		}
		catch( Exception e ) {
			InputAgent.logWarning("Could not create trace file");
		}

		URI dirURI = file.getParentFile().toURI();
		InputAgent.readStream("", dirURI, file.getName());

		GUIFrame.instance().setProgressText(null);
		GUIFrame.instance().setProgress(0);

		// At this point configuration file is loaded

		// The session is not considered to be edited after loading a configuration file
		sessionEdited = false;

		// Save and close the input trace file
		if (logFile != null) {
			if (InputAgent.numWarnings == 0 && InputAgent.numErrors == 0) {
				logFile.close();
				logFile.delete();
				logFile = new FileEntity( inputTraceFileName);
			}
		}

		//  Check for found errors
		if( InputAgent.numErrors > 0 )
			throw new InputErrorException("%d input errors and %d warnings found, check %s", InputAgent.numErrors, InputAgent.numWarnings, inputTraceFileName);

		if (Simulation.getPrintInputReport())
			InputAgent.printInputFileKeywords();
	}

	public static final void apply(Entity ent, KeywordIndex kw) {
		Input<?> in = ent.getInput(kw.keyword);
		if (in == null) {
			InputAgent.logWarning("Keyword %s could not be found for Entity %s.", kw.keyword, ent.getInputName());
			return;
		}

		InputAgent.apply(ent, in, kw);
		FrameBox.valueUpdate();
	}

	public static final void apply(Entity ent, Input<?> in, KeywordIndex kw) {
		// If the input value is blank, restore the default
		if (kw.numArgs() == 0) {
			in.reset();
		}
		else {
			in.parse(kw);
			if (kw.numArgs() < 1000)
				in.setValueString(kw.argString());
			else
				in.setValueString("");
		}

		// Only mark the keyword edited if we have finished initial configuration
		if (InputAgent.recordEdits()) {
			in.setEdited(true);
			ent.setFlag(Entity.FLAG_EDITED);
			if (!ent.testFlag(Entity.FLAG_GENERATED))
				sessionEdited = true;
		}

		ent.updateForInput(in);
	}

	public static void processKeyword(Entity entity, KeywordIndex key) {
		if (entity.testFlag(Entity.FLAG_LOCKED))
			throw new InputErrorException("Entity: %s is locked and cannot be modified", entity.getName());

		Input<?> input = entity.getInput( key.keyword );
		if (input != null) {
			InputAgent.apply(entity, input, key);
			FrameBox.valueUpdate();
			return;
		}

		if (!(entity instanceof Group))
			throw new InputErrorException("Not a valid keyword");

		Group grp = (Group)entity;
		grp.saveGroupKeyword(key);

		// Store the keyword data for use in the edit table
		for( int i = 0; i < grp.getList().size(); i++ ) {
			Entity ent = grp.getList().get( i );
			InputAgent.apply(ent, key);
		}
	}

	public static void load(GUIFrame gui) {
		LogBox.logLine("Loading...");

		// Create a file chooser
		final JFileChooser chooser = new JFileChooser(InputAgent.getConfigFile());

		// Set the file extension filters
		chooser.setAcceptAllFileFilterUsed(true);
		FileNameExtensionFilter cfgFilter =
				new FileNameExtensionFilter("JaamSim Configuration File (*.cfg)", "CFG");
		chooser.addChoosableFileFilter(cfgFilter);
		chooser.setFileFilter(cfgFilter);

		// Show the file chooser and wait for selection
		int returnVal = chooser.showOpenDialog(gui);

		// Load the selected file
		if (returnVal == JFileChooser.APPROVE_OPTION) {
            File temp = chooser.getSelectedFile();
    		InputAgent.setLoadFile(gui, temp);
        }
	}

	public static void save(GUIFrame gui) {
		LogBox.logLine("Saving...");
		if( InputAgent.getConfigFile() != null ) {
			setSaveFile(gui, InputAgent.getConfigFile().getPath() );
		}
		else {
			saveAs( gui );
		}
	}

	public static void saveAs(GUIFrame gui) {
		LogBox.logLine("Save As...");

		// Create a file chooser
		final JFileChooser chooser = new JFileChooser(InputAgent.getConfigFile());

		// Set the file extension filters
		chooser.setAcceptAllFileFilterUsed(true);
		FileNameExtensionFilter cfgFilter =
				new FileNameExtensionFilter("JaamSim Configuration File (*.cfg)", "CFG");
		chooser.addChoosableFileFilter(cfgFilter);
		chooser.setFileFilter(cfgFilter);
		chooser.setSelectedFile(InputAgent.getConfigFile());

		// Show the file chooser and wait for selection
		int returnVal = chooser.showSaveDialog(gui);

		// Load the selected file
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			File file = chooser.getSelectedFile();
			String filePath = file.getPath();

			// Add the file extension ".cfg" if needed
			filePath = filePath.trim();
			if (filePath.indexOf(".") == -1)
				filePath = filePath.concat(".cfg");

			// Confirm overwrite if file already exists
			File temp = new File(filePath);
			if (temp.exists()) {

				int userOption = JOptionPane.showConfirmDialog( null,
						file.getName() + " already exists.\n" +
						"Do you wish to replace it?", "Confirm Save As",
						JOptionPane.YES_NO_OPTION,
						JOptionPane.WARNING_MESSAGE );

				if (userOption == JOptionPane.NO_OPTION) {
					return;
				}
			}

			// Save the configuration file
			InputAgent.setSaveFile(gui, filePath);
		}
	}

	public static void configure(GUIFrame gui, File file) {
		try {
			gui.clear();
			InputAgent.setConfigFile(file);
			gui.updateForSimulationState(GUIFrame.SIM_STATE_UNCONFIGURED);

			try {
				InputAgent.loadConfigurationFile(file);
			}
			catch( InputErrorException iee ) {
				if (!batchRun)
					ExceptionBox.instance().setErrorBox(iee.getMessage());
				else
					LogBox.logLine( iee.getMessage() );
			}

			LogBox.logLine("Configuration File Loaded");

			// show the present state in the user interface
			gui.setTitle( Simulation.getModelName() + " - " + InputAgent.getRunName() );
			gui.updateForSimulationState(GUIFrame.SIM_STATE_CONFIGURED);
			gui.enableSave(InputAgent.getRecordEditsFound());
		}
		catch( Throwable t ) {
			ExceptionBox.instance().setError(t);
		}
	}

	/**
	 * Loads the configuration file.
	 * <p>
	 * @param gui - the Control Panel.
	 * @param file - the configuration file to be loaded.
	 */
	private static void setLoadFile(final GUIFrame gui, File file) {

		final File chosenfile = file;
		new Thread(new Runnable() {
			@Override
			public void run() {
				InputAgent.setRecordEdits(false);
				InputAgent.configure(gui, chosenfile);
				InputAgent.setRecordEdits(true);

				GUIFrame.displayWindows();
				FrameBox.valueUpdate();
			}
		}).start();
	}

	/**
	 * Saves the configuration file.
	 * @param gui = Control Panel window for JaamSim
	 * @param fileName = absolute file path and file name for the file to be saved
	 */
	private static void setSaveFile(GUIFrame gui, String fileName) {

		// Set root directory
		File temp = new File(fileName);

		// Save the configuration file
		InputAgent.printNewConfigurationFileWithName( fileName );
		sessionEdited = false;
		InputAgent.setConfigFile(temp);

		// Set the title bar to match the new run name
		gui.setTitle( Simulation.getModelName() + " - " + InputAgent.getRunName() );
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
		String inputReportFileName = InputAgent.getReportFileName(InputAgent.getRunName() + ".inp");

		FileEntity inputReportFile = new FileEntity( inputReportFileName);
		inputReportFile.flush();

		// Loop through the entity classes printing Define statements
		for (ObjectType type : ObjectType.getAll()) {
			Class<? extends Entity> each = type.getJavaClass();

			// Loop through the instances for this entity class
			int count = 0;
			for (Entity ent : Entity.getInstanceIterator(each)) {
				boolean hasinput = false;

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

			if (!Entity.getInstanceIterator(each).hasNext()) {
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
	private static final String inpErrPrefix = "*** INPUT ERROR *** %s%n";
	private static final String wrnPrefix = "***WARNING*** %s%n";

	public static int numErrors() {
		return numErrors;
	}

	public static int numWarnings() {
		return numWarnings;
	}

	private static void echoInputRecord(ArrayList<String> tokens) {
		if (logFile == null)
			return;

		boolean beginLine = true;
		for (int i = 0; i < tokens.size(); i++) {
			if (!beginLine)
				logFile.write("  ");
			String tok = tokens.get(i);
			logFile.write(tok);
			beginLine = false;
			if (tok.startsWith("\"")) {
				logFile.newLine();
				beginLine = true;
			}
		}
		// If there were any leftover string written out, make sure the line gets terminated
		if (!beginLine)
			logFile.newLine();

		logFile.flush();
	}

	private static void logBadInput(ArrayList<String> tokens, String msg) {
		InputAgent.echoInputRecord(tokens);
		InputAgent.logError("%s", msg);
	}

	public static void logMessage(String fmt, Object... args) {
		String msg = String.format(fmt, args);
		System.out.println(msg);
		LogBox.logLine(msg);

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

	public static void logInpError(String fmt, Object... args) {
		numErrors++;
		String msg = String.format(fmt, args);
		InputAgent.logMessage(inpErrPrefix, msg);
	}

	/**
	 * Prepares the keyword and input value for processing.
	 *
	 * @param ent - the entity whose keyword and value have been entered.
	 * @param in - the input object for the keyword.
	 * @param value - the input value String for the keyword.
	 */
	public static void processEntity_Keyword_Value(Entity ent, Input<?> in, String value){
		processEntity_Keyword_Value(ent, in.getKeyword(), value);
	}

	/**
	 * Prepares the keyword and input value for processing.
	 *
	 * @param ent - the entity whose keyword and value has been entered.
	 * @param keyword - the keyword.
	 * @param value - the input value String for the keyword.
	 */
	public static void processEntity_Keyword_Value(Entity ent, String keyword, String value){

		// Keyword
		ArrayList<String> tokens = new ArrayList<String>();

		// Value
		if (!value.equals(Input.getNoValue()))
			Parser.tokenize(tokens, value, true);

		// Parse the keyword inputs
		KeywordIndex kw = new KeywordIndex(tokens, keyword, 0, tokens.size(), null);
		InputAgent.processKeyword(ent, kw);
	}

	/**
	 * Prints the present state of the model to a new configuration file.
	 *
	 * @param fileName - the full path and file name for the new configuration file.
	 */
	public static void printNewConfigurationFileWithName( String fileName ) {

		// 1) WRITE LINES FROM THE ORIGINAL CONFIGURATION FILE

		// Copy the original configuration file up to the "RecordEdits" marker (if present)
		// Temporary storage for the copied lines is needed in case the original file is to be overwritten
		ArrayList<String> preAddedRecordLines = new ArrayList<String>();
		if( InputAgent.getConfigFile() != null ) {
			try {
				BufferedReader in = new BufferedReader( new FileReader(InputAgent.getConfigFile()) );
				String line;
				while ( ( line = in.readLine() ) != null ) {
					preAddedRecordLines.add( line );
					if ( line.startsWith( recordEditsMarker ) ) {
						break;
					}
				}
				in.close();
			}
			catch ( Exception e ) {
				throw new ErrorException( e );
			}
		}

		// Create the new configuration file and copy the saved lines
		FileEntity file = new FileEntity( fileName);
		for( int i=0; i < preAddedRecordLines.size(); i++ ) {
			file.format("%s%n", preAddedRecordLines.get( i ));
		}

		// If not already present, insert the "RecordEdits" marker at the end of the original configuration file
		if( ! InputAgent.getRecordEditsFound() ) {
			file.format("%n%s%n", recordEditsMarker);
			InputAgent.setRecordEditsFound(true);
		}

		// 2) WRITE THE DEFINITION STATEMENTS FOR NEW OBJECTS

		// Determine all the new classes that were created
		ArrayList<Class<? extends Entity>> newClasses = new ArrayList<Class<? extends Entity>>();
		for (Entity ent : Entity.getAll()) {
			if (!ent.testFlag(Entity.FLAG_ADDED) || ent.testFlag(Entity.FLAG_GENERATED))
				continue;

			if (!newClasses.contains(ent.getClass()))
				newClasses.add(ent.getClass());
		}

		// Add a blank line before the first object definition
		if( !newClasses.isEmpty() )
			file.format("%n");

		// Identify the object types for which new instances were defined
		for( Class<? extends Entity> newClass : newClasses ) {
			for (ObjectType o : ObjectType.getAll()) {
				if (o.getJavaClass() == newClass) {

					// Print the first part of the "Define" statement for this object type
					file.format("Define %s {", o.getInputName());
					break;
				}
			}

			// Print the new instances that were defined
			for (Entity ent : Entity.getAll()) {
				if (!ent.testFlag(Entity.FLAG_ADDED) || ent.testFlag(Entity.FLAG_GENERATED))
					continue;

				if (ent.getClass() == newClass)
					file.format(" %s ", ent.getInputName());

			}
			// Close the define statement
			file.format("}%n");
		}

		// 3) WRITE THE ATTRIBUTE DEFINITIONS
		boolean blankLinePrinted = false;
		for (Entity ent : Entity.getAll()) {
			if (!ent.testFlag(Entity.FLAG_EDITED))
				continue;
			if (ent.testFlag(Entity.FLAG_GENERATED))
				continue;

			final Input<?> in = ent.getInput("AttributeDefinitionList");
			if (in == null || !in.isEdited())
				continue;

			if (!blankLinePrinted) {
				file.format("%n");
				blankLinePrinted = true;
			}
			writeInputOnFile_ForEntity(file, ent, in);
		}

		// 4) WRITE THE INPUTS FOR KEYWORDS THAT WERE EDITED

		// Identify the entities whose inputs were edited
		for (Entity ent : Entity.getAll()) {
			if (!ent.testFlag(Entity.FLAG_EDITED))
				continue;
			if (ent.testFlag(Entity.FLAG_GENERATED))
				continue;

			file.format("%n");

			ArrayList<Input<?>> deferredInputs = new ArrayList<Input<?>>();
			// Print the key inputs first
			for (Input<?> in : ent.getEditableInputs()) {
				if (!in.isEdited())
					continue;
				if ("AttributeDefinitionList".equals(in.getKeyword()))
					continue;

				// defer all inputs outside the Key Inputs category
				if (!"Key Inputs".equals(in.getCategory())) {
					deferredInputs.add(in);
					continue;
				}

				writeInputOnFile_ForEntity(file, ent, in);
			}

			for (Input<?> in : deferredInputs) {
				writeInputOnFile_ForEntity(file, ent, in);
			}
		}

		// Close the new configuration file
		file.flush();
		file.close();
	}

	static void writeInputOnFile_ForEntity(FileEntity file, Entity ent, Input<?> in) {
		file.format("%s %s { %s }%n",
		            ent.getInputName(), in.getKeyword(), in.getValueString());
	}

	/**
	 * Returns the relative file path for the specified URI.
	 * <p>
	 * The path can start from either the folder containing the present
	 * configuration file or from the resources folder.
	 * <p>
	 * @param uri - the URI to be relativized.
	 * @return the relative file path.
	 */
	static public String getRelativeFilePath(URI uri) {

		// Relativize the file path against the resources folder
		String resString = resRoot.toString();
		String inputString = uri.toString();
		if (inputString.startsWith(resString)) {
			return String.format("'<res>/%s'", inputString.substring(resString.length()));
		}

		// Relativize the file path against the configuration file
		try {
			URI configDirURI = InputAgent.getConfigFile().getParentFile().toURI();
			return String.format("'%s'", configDirURI.relativize(uri).getPath());
		}
		catch (Exception ex) {
			return String.format("'%s'", uri.getPath());
		}
	}

	/**
	 * Loads the default configuration file.
	 */
	public static void loadDefault() {

		// Read the default configuration file
		InputAgent.readResource("inputs/default.cfg");

		// A RecordEdits marker in the default configuration must be ignored
		InputAgent.setRecordEditsFound(false);

		// Set the model state to unedited
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
	 * Converts a file path String to a URI.
	 * <p>
	 * The specified file path can be either relative or absolute. In the case
	 * of a relative file path, a 'context' folder must be specified. A context
	 * of null indicates an absolute file path.
	 * <p>
	 * To avoid bad input accessing an inappropriate file, a 'jail' folder can
	 * be specified. The URI to be returned must include the jail folder for it
	 * to be valid.
	 * <p>
	 * @param context - full file path for the folder that is the reference for relative file paths.
	 * @param filePath - string to be resolved to a URI.
	 * @param jailPrefix - file path to a base folder from which a relative cannot escape.
	 * @return the URI corresponding to the context and filePath.
	 */
	public static URI getFileURI(URI context, String filePath, String jailPrefix) throws URISyntaxException {

		// Replace all backslashes with slashes
		String path = filePath.replaceAll("\\\\", "/");

		int colon = path.indexOf(':');
		int openBrace = path.indexOf('<');
		int closeBrace = path.indexOf('>');
		int firstSlash = path.indexOf('/');

		// Add a leading slash if needed to convert from Windows format (e.g. from "C:" to "/C:")
		if (colon == 1)
			path = String.format("/%s", path);

		// 1) File path starts with a tagged folder, using the syntax "<tagName>/"
		URI ret = null;
		if (openBrace == 0 && closeBrace != -1 && firstSlash == closeBrace + 1) {
			String specPath = path.substring(openBrace + 1, closeBrace);

			// Resources folder in the Jar file
			if (specPath.equals("res")) {
				ret = new URI(resRoot.getScheme(), resRoot.getSchemeSpecificPart() + path.substring(closeBrace+2), null).normalize();
			}
		}
		// 2) Normal file path
		else {
			URI pathURI = new URI(null, path, null).normalize();

			if (context != null) {
				if (context.isOpaque()) {
					// Things are going to get messy in here
					URI schemeless = new URI(null, context.getSchemeSpecificPart(), null);
					URI resolved = schemeless.resolve(pathURI).normalize();

					// Note: we are using the one argument constructor here because the 'resolved' URI is already encoded
					// and we do not want to double-encode (and schemes should never need encoding, I hope)
					ret = new URI(context.getScheme() + ":" + resolved.toString());
				} else {
					ret = context.resolve(pathURI).normalize();
				}
			} else {
				// We have no context, so append a 'file' scheme if necessary
				if (pathURI.getScheme() == null) {
					ret = new URI("file", pathURI.getPath(), null);
				} else {
					ret = pathURI;
				}
			}
		}

		// Check that the file path includes the jail folder
		if (jailPrefix != null && ret.toString().indexOf(jailPrefix) != 0) {
			LogBox.format("Failed jail test: %s in jail: %s context: %s\n", ret.toString(), jailPrefix, context.toString());
			LogBox.getInstance().setVisible(true);
			return null; // This resolved URI is not in our jail
		}

		return ret;
	}

	/**
	 * Determines whether or not a file exists.
	 * <p>
	 * @param filePath - URI for the file to be tested.
	 * @return true if the file exists, false if it does not.
	 */
	public static boolean fileExists(URI filePath) {

		try {
			InputStream in = filePath.toURL().openStream();
			in.close();
			return true;
		}
		catch (MalformedURLException ex) {
			return false;
		}
		catch (IOException ex) {
			return false;
		}
	}
}
