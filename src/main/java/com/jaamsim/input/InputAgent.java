/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2009-2011 Ausenco Engineering Canada Inc.
 * Copyright (C) 2018-2020 JaamSim Software Inc.
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
package com.jaamsim.input;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;

import com.jaamsim.Commands.Command;
import com.jaamsim.Graphics.EntityLabel;
import com.jaamsim.StringProviders.StringProvider;
import com.jaamsim.basicsim.Entity;
import com.jaamsim.basicsim.ErrorException;
import com.jaamsim.basicsim.FileEntity;
import com.jaamsim.basicsim.GUIListener;
import com.jaamsim.basicsim.Group;
import com.jaamsim.basicsim.JaamSimModel;
import com.jaamsim.basicsim.ObjectType;
import com.jaamsim.basicsim.Simulation;
import com.jaamsim.datatypes.DoubleVector;
import com.jaamsim.events.EventManager;
import com.jaamsim.math.Vec3d;
import com.jaamsim.ui.LogBox;
import com.jaamsim.units.DimensionlessUnit;
import com.jaamsim.units.DistanceUnit;
import com.jaamsim.units.TimeUnit;
import com.jaamsim.units.Unit;

public class InputAgent {
	private static final String recordEditsMarker = "RecordEdits";

	private static final String INP_ERR_DEFINEUSED = "The name: %s has already been used and is a %s";
	public static final String INP_ERR_BADNAME = "An entity name cannot be blank or contain "
	                                            + "spaces, tabs, braces, single or double quotes, "
	                                            + "square brackets, the hash character, or a "
	                                            + "period.\n "
	                                            + "Name: %s";
	private static final String INP_ERR_BADPARENT = "The parent entity [%s] has not been defined.";
	static final char[] INVALID_ENTITY_CHARS = new char[]{' ', '\t', '\n', '{', '}', '\'', '"', '[', ']', '#','.'};

	private static final String[] EARLY_KEYWORDS = {"UnitType", "UnitTypeList", "OutputUnitType", "SecondaryUnitType", "DataFile", "AttributeDefinitionList", "CustomOutputList"};
	private static final String[] GRAPHICS_PALETTES = {"Graphics Objects", "View", "Display Models"};
	private static final String[] GRAPHICS_CATEGORIES = {Entity.GRAPHICS, Entity.FONT, Entity.FORMAT, Entity.GUI};

	public static void storeAndExecute(Command cmd) {
		GUIListener gui = cmd.getJaamSimModel().getGUIListener();
		if (gui == null) {
			cmd.execute();
			return;
		}
		gui.storeAndExecute(cmd);
	}

	private static int getBraceDepth(JaamSimModel simModel, ArrayList<String> tokens, int startingBraceDepth, int startingIndex) {
		int braceDepth = startingBraceDepth;
		for (int i = startingIndex; i < tokens.size(); i++) {
			String token = tokens.get(i);

			if (token.equals("{"))
				braceDepth++;

			if (token.equals("}"))
				braceDepth--;

			if (braceDepth < 0) {
				InputAgent.logBadInput(simModel, tokens, "Extra closing braces found");
				tokens.clear();
				braceDepth = 0;
			}

			if (braceDepth > 3) {
				InputAgent.logBadInput(simModel, tokens, "Maximum brace depth (3) exceeded");
				tokens.clear();
				braceDepth = 0;
			}
		}

		return braceDepth;
	}

	private static URI resRoot;
	private static final String res = "/resources/";

	static {

		try {
			// locate the resource folder, and create
			URI temp = InputAgent.class.getResource(res).toURI();
			resRoot = new URI(temp.getScheme(), temp.getSchemeSpecificPart(), null).normalize();
		}
		catch (URISyntaxException e) {}
	}

	private static void rethrowWrapped(Exception ex) {
		StringBuilder causedStack = new StringBuilder();
		for (StackTraceElement elm : ex.getStackTrace())
			causedStack.append(elm.toString()).append("\n");
		throw new InputErrorException("Caught exception: %s", ex.getMessage() + "\n" + causedStack.toString());
	}

	public static final void readResource(JaamSimModel simModel, String res) {
		if (res == null)
			return;

		try {
			readStream(simModel, null, null, res);
		}
		catch (URISyntaxException ex) {
			rethrowWrapped(ex);
		}

	}

	public static final boolean readStream(JaamSimModel simModel, String root, URI path, String file) throws URISyntaxException {
		URI resolved = getFileURI(path, file, root);

		URL url = null;
		try {
			url = resolved.normalize().toURL();
		}
		catch (MalformedURLException e) {
			rethrowWrapped(e);
		}

		if (url == null) {
			InputAgent.logError(simModel,
					"Unable to resolve path %s%s - %s", root, path.toString(), file);
			return false;
		}

		BufferedReader buf = null;
		try {
			InputStream in = url.openStream();
			buf = new BufferedReader(new InputStreamReader(in));
		} catch (IOException e) {
			InputAgent.logError(simModel,
					"Could not read from url: '%s'%n%s", url.toString(), e.getMessage());
			return false;
		}

		InputAgent.readBufferedStream(simModel, buf, resolved, root);
		return true;
	}

	public static final void readBufferedStream(JaamSimModel simModel, BufferedReader buf, URI resolved, String root) {

		try {
			ArrayList<String> record = new ArrayList<>();
			int braceDepth = 0;

			ParseContext pc = new ParseContext(resolved, root);

			while (true) {
				String line = buf.readLine();
				// end of file, stop reading
				if (line == null)
					break;

				int previousRecordSize = record.size();
				boolean quoted = Parser.tokenize(record, line, true);

				// Keep reading lines if the end of line was hit while in quoted context
				while (quoted) {

					// Append the next line to the line
					String nextLine = buf.readLine();
					if (nextLine == null)  // end of file
						break;
					StringBuilder sb = new StringBuilder(line);
					sb.append(nextLine);
					line = sb.toString();

					// Clear the record and tokenize the now longer line
					record.clear();
					quoted = Parser.tokenize(record, line, true);
				}

				braceDepth = InputAgent.getBraceDepth(simModel, record, braceDepth, previousRecordSize);
				if( braceDepth != 0 )
					continue;

				if (record.size() == 0)
					continue;

				InputAgent.echoInputRecord(simModel, record);

				if ("DEFINE".equalsIgnoreCase(record.get(0))) {
					InputAgent.processDefineRecord(simModel, record);
					record.clear();
					continue;
				}

				if ("INCLUDE".equalsIgnoreCase(record.get(0))) {
					try {
						InputAgent.processIncludeRecord(simModel, pc, record);
					}
					catch (URISyntaxException ex) {
						rethrowWrapped(ex);
					}
					record.clear();
					continue;
				}

				if ("RECORDEDITS".equalsIgnoreCase(record.get(0))) {
					simModel.setRecordEditsFound(true);
					simModel.setRecordEdits(true);
					record.clear();
					continue;
				}

				// Otherwise assume it is a Keyword record
				InputAgent.processKeywordRecord(simModel, record, pc);
				record.clear();
			}

			// Leftover Input at end of file
			if (record.size() > 0)
				InputAgent.logBadInput(simModel, record, "Leftover input at end of file");
			buf.close();
		}
		catch (IOException e) {
			// Make best effort to ensure it closes
			try { buf.close(); } catch (IOException e2) {}
		}
	}

	private static void processIncludeRecord(JaamSimModel simModel, ParseContext pc, ArrayList<String> record) throws URISyntaxException {
		if (record.size() != 2) {
			InputAgent.logError(simModel,
					"Bad Include record, should be: Include <File>");
			return;
		}
		InputAgent.readStream(simModel, pc.jail, pc.context, record.get(1).replaceAll("\\\\", "/"));
	}

	private static void processDefineRecord(JaamSimModel simModel, ArrayList<String> record) {
		if (record.size() < 5 ||
		    !record.get(2).equals("{") ||
		    !record.get(record.size() - 1).equals("}")) {
			InputAgent.logError(simModel,
					"Bad Define record, should be: Define <Type> { <names>... }");
			return;
		}

		Class<? extends Entity> proto = null;
		try {
			if( record.get( 1 ).equalsIgnoreCase( "ObjectType" ) ) {
				proto = ObjectType.class;
			}
			else {
				proto = Input.parseEntityType(simModel, record.get(1));
			}
		}
		catch (InputErrorException e) {
			InputAgent.logError(simModel,
					"%s", e.getMessage());
			return;
		}

		// Loop over all the new Entity names
		for (int i = 3; i < record.size() - 1; i++) {
			InputAgent.defineEntity(simModel, proto, record.get(i), simModel.isRecordEdits());
		}
	}

	public static <T extends Entity> T generateEntityWithName(JaamSimModel simModel, Class<T> proto, String key) {
		return generateEntityWithName(simModel, proto, key, null, false, false);
	}

	public static <T extends Entity> T generateEntityWithName(JaamSimModel simModel, Class<T> proto, String key,
			boolean reg) {
		return generateEntityWithName(simModel, proto, key, null, reg, false);
	}

	public static <T extends Entity> T generateEntityWithName(JaamSimModel simModel, Class<T> proto, String key,
			boolean reg, boolean retain) {
		return generateEntityWithName(simModel, proto, key, null, reg, retain);
	}

	public static <T extends Entity> T generateEntityWithName(JaamSimModel simModel, Class<T> proto, String key, Entity parent,
			boolean reg, boolean retain) {
		if (key == null)
			throw new ErrorException("Must provide a name for generated Entities");

		if (!isValidName(key))
			throw new ErrorException(INP_ERR_BADNAME, key);

		T ent = simModel.createInstance(proto, key, parent, false, true, reg, retain);
		if (ent == null)
			throw new ErrorException("Could not create new Entity: %s", key);

		return ent;
	}

	public static String getUniqueName(JaamSimModel sim, String name, String sep) {

		// Is the provided name unused?
		if (sim.getNamedEntity(name) == null)
			return name;

		// Try the provided name plus "1", "2", etc. until an unused name is found
		int entityNum = 1;
		while(true) {
			String ret = String.format("%s%s%d", name, sep, entityNum);
			if (sim.getNamedEntity(ret) == null) {
				return ret;
			}
			entityNum++;
		}
	}

	/**
	 * Creates a new entity with a unique name. If an entity already exists with the specified
	 * base name, a separator will be appended followed by the smallest integer required to make
	 * the name unique. If addedEntity is true then this is an entity defined by user interaction
	 * or after the 'AddedRecord' flag is found in the configuration file.
	 * @param simModel - JaamSimModel in which to create the entity
	 * @param proto - class for the entity to be created
	 * @param key - base absolute name for the entity to be created
	 * @param sep - string to append to the name if it is already in use
	 * @param addedEntity - true if the entity is new to the model
	 * @return new entity
	 */
	public static <T extends Entity> T defineEntityWithUniqueName(JaamSimModel simModel, Class<T> proto, String key, String sep, boolean addedEntity) {
		String name = getUniqueName(simModel, key, sep);
		return defineEntity(simModel, proto, name, addedEntity);
	}

	public static boolean isValidName(String key) {
		if (key.isEmpty())
			return false;
		for (int i = 0; i < key.length(); ++i) {
			final char c = key.charAt(i);
			for (char invChar : INVALID_ENTITY_CHARS) {
				if (c == invChar)
					return false;
			}
		}
		return true;
	}

	/**
	 * Creates a new entity with the specified name. If addedEntity is true then this is an entity
	 * defined by user interaction or after the 'AddedRecord' flag is found in the configuration
	 * file.
	 * @param simModel - JaamSimModel in which to create the entity
	 * @param proto - class for the entity to be created
	 * @param key - absolute name for the entity to be created
	 * @param addedEntity - true if the entity is new to the model
	 * @return new entity
	 */
	private static <T extends Entity> T defineEntity(JaamSimModel simModel, Class<T> proto, String key, boolean addedEntity) {
		Entity existingEnt = Input.tryParseEntity(simModel, key, Entity.class);
		if (existingEnt != null) {
			InputAgent.logError(simModel,
					INP_ERR_DEFINEUSED, key, existingEnt.getClass().getSimpleName());
			return null;
		}

		Entity parent = null;
		String localName = key;

		if (key.contains(".")) {
			String[] names = key.split("\\.");
			localName = names[names.length - 1];
			names = Arrays.copyOf(names, names.length - 1);
			parent = simModel.getEntityFromNames(names);
			if (parent == null) {
				String parentName = key.substring(0, key.length() - localName.length() - 1);
				InputAgent.logError(simModel, INP_ERR_BADPARENT, parentName);
				return null;
			}
		}

		return defineEntity(simModel, proto, localName, parent, addedEntity);
	}

	private static <T extends Entity> T defineEntity(JaamSimModel simModel, Class<T> proto, String localName, Entity parent, boolean addedEntity) {

		if (!isValidName(localName)) {
			InputAgent.logError(simModel, INP_ERR_BADNAME, localName);
			return null;
		}

		T ent = simModel.createInstance(proto, localName, parent, addedEntity, false, true, true);

		if (ent == null) {
			InputAgent.logError(simModel,
					"Could not create new child Entity: %s for parent: %s", localName, parent);
			return null;
		}

		return ent;
	}

	public static void processKeywordRecord(JaamSimModel simModel, ArrayList<String> record, ParseContext context) {
		Entity ent = Input.tryParseEntity(simModel, record.get(0), Entity.class);
		if (ent == null) {
			InputAgent.logError(simModel,
					"Could not find Entity: %s", record.get(0));
			return;
		}

		// Validate the tokens have the Entity Keyword { Args... } Keyword { Args... }
		ArrayList<KeywordIndex> words = InputAgent.getKeywords(record, context);
		for (KeywordIndex keyword : words) {
			try {
				InputAgent.processKeyword(ent, keyword);
			}
			catch (Throwable e) {
				InputAgent.logInpError(simModel,
						"Entity: %s, Keyword: %s - %s", ent.getName(), keyword.keyword, e.getMessage());
				if (e.getMessage() == null) {
					for (StackTraceElement each : e.getStackTrace())
						InputAgent.logMessage(simModel, each.toString());
				}
			}
		}
	}

	private static ArrayList<KeywordIndex> getKeywords(ArrayList<String> input, ParseContext context) {
		ArrayList<KeywordIndex> ret = new ArrayList<>();

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

					ret.add(new KeywordIndex(keyword, input, keyWordIdx + 2, i, context));
					keyWordIdx = i + 1;
					continue;
				}
			}
		}

		if (keyWordIdx != input.size())
			throw new InputErrorException("The input for a keyword must be enclosed by braces. Should be <keyword> { <args> }");

		return ret;
	}

	// Load the run file
	public static void loadConfigurationFile(JaamSimModel simModel, File file) throws URISyntaxException {

		// Load the input file
		URI dirURI = file.getParentFile().toURI();
		InputAgent.readStream(simModel, "", dirURI, file.getName());

		// Perform any actions that are required after loading the input file
		simModel.postLoad();

		// Validate the inputs
		for (Entity each : simModel.getClonesOfIterator(Entity.class)) {
			try {
				each.validate();
			}
			catch (Throwable e) {
				simModel.recordError();
				InputAgent.logMessage(simModel,
						"Validation Error - %s: %s", each, e.getMessage());
			}
		}

		//  Check for found errors
		if (simModel.getNumErrors() > 0 )
			throw new InputErrorException("%d input errors and %d warnings found",
					simModel.getNumErrors(), simModel.getNumWarnings());

		if (simModel.getSimulation().getPrintInputReport())
			InputAgent.printInputFileKeywords(simModel);
	}

	/**
	 * Prepares the keyword and input value for processing.
	 *
	 * @param ent - the entity whose keyword and value has been entered.
	 * @param keyword - the keyword.
	 * @param value - the input value String for the keyword.
	 */
	public static void applyArgs(Entity ent, String keyword, String... args){
		KeywordIndex kw = formatArgs(keyword, args);
		InputAgent.apply(ent, kw);
	}

	public static void applyVec3d(Entity ent, String keyword, Vec3d point, Class<? extends Unit> ut) {
		KeywordIndex kw = formatVec3dInput(keyword, point, ut);
		InputAgent.apply(ent, kw);
	}

	public static void applyBoolean(Entity ent, String keyword, boolean bool) {
		KeywordIndex kw = formatBoolean(keyword, bool);
		InputAgent.apply(ent, kw);
	}

	public static void applyIntegers(Entity ent, String keyword, int... args){
		KeywordIndex kw = formatIntegers(keyword, args);
		InputAgent.apply(ent, kw);
	}

	public static void applyValue(Entity ent, String keyword, double val, String unit){
		KeywordIndex kw = formatDoubleInput(keyword, val, unit);
		InputAgent.apply(ent, kw);
	}

	public static final void apply(Entity ent, KeywordIndex kw) {
		Input<?> in = ent.getInput(kw.keyword);
		if (in == null) {
			InputAgent.logError(ent.getJaamSimModel(),
					"Keyword %s could not be found for Entity %s.", kw.keyword, ent.getName());
			return;
		}

		InputAgent.apply(ent, in, kw);
	}

	public static final void apply(Entity ent, Input<?> in, KeywordIndex kw) {
		// If the input value is blank, restore the default
		if (kw.numArgs() == 0) {
			if (in.isDefault())
				return;
			in.reset();
		}
		else {
			in.parse(ent, kw);
			in.setTokens(kw);
		}

		// Only mark the keyword edited if we have finished initial configuration
		JaamSimModel simModel = ent.getJaamSimModel();
		if (simModel.isRecordEdits()) {
			in.setEdited(true);
			ent.setEdited();
		}

		ent.updateForInput(in);
		GUIListener gui = ent.getJaamSimModel().getGUIListener();
		if (gui != null)
			gui.updateAll();
	}

	public static void processKeyword(Entity entity, KeywordIndex key) {
		Input<?> input = entity.getInput( key.keyword );
		if (input != null) {
			InputAgent.apply(entity, input, key);
			return;
		}

		if (!(entity instanceof Group))
			throw new InputErrorException("Not a valid keyword");

		Group grp = (Group)entity;
		grp.saveGroupKeyword(entity.getJaamSimModel(), key);

		// Store the keyword data for use in the edit table
		for( int i = 0; i < grp.getList().size(); i++ ) {
			Entity ent = grp.getList().get( i );
			InputAgent.apply(ent, key);
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
	public static void printInputFileKeywords(JaamSimModel simModel) {
		// Create report file for the inputs
		String inputReportFileName = simModel.getReportFileName(simModel.getRunName() + ".inp");

		FileEntity inputReportFile = new FileEntity( inputReportFileName);
		inputReportFile.flush();

		ArrayList<ObjectType> objectTypes = new ArrayList<>();
		for (ObjectType type : simModel.getObjectTypes())
			objectTypes.add( type );

		// Sort ObjectTypes by Units, Simulation, and then alphabetically by palette name
		Collections.sort(objectTypes, new Comparator<ObjectType>() {
			@Override
			public int compare(ObjectType a, ObjectType b) {

				// Put Unit classes first
				if (Unit.class.isAssignableFrom(a.getJavaClass())) {
					if (Unit.class.isAssignableFrom(b.getJavaClass()))
						return 0;
					else
						return -1;
				}
				if (Unit.class.isAssignableFrom(b.getJavaClass())) {
						return 1;
				}

				// Put Simulation classes second
				if (Simulation.class.isAssignableFrom(a.getJavaClass())) {
					if (Simulation.class.isAssignableFrom(b.getJavaClass()))
						return 0;
					else
						return -1;
				}
				if (Simulation.class.isAssignableFrom(b.getJavaClass())) {
						return 1;
				}

				// Sort the rest alphabetically by palette name
				return a.getPaletteName().compareTo(b.getPaletteName());
			}
		});

		// Loop through the entity classes printing Define statements
		for (ObjectType type : objectTypes) {
			Class<? extends Entity> each = type.getJavaClass();

			// Loop through the instances for this entity class
			int count = 0;
			for (Entity ent : simModel.getInstanceIterator(each)) {
				if (simModel.isPreDefinedEntity(ent))
					continue;

				count++;

				String entityName = ent.getName();
				if ((count - 1) % 5 == 0) {
					inputReportFile.write("Define");
					inputReportFile.write("\t");
					inputReportFile.write(type.getName());
					inputReportFile.write("\t");
					inputReportFile.write("{ " + entityName);
					inputReportFile.write("\t");
				}
				else if ((count - 1) % 5 == 4) {
					inputReportFile.write(entityName + " }");
					inputReportFile.newLine();
				}
				else {
					inputReportFile.write(entityName);
					inputReportFile.write("\t");
				}
			}

			if (count % 5 != 0) {
				inputReportFile.write(" }");
				inputReportFile.newLine();
			}
			if (count > 0)
				inputReportFile.newLine();
		}

		for (ObjectType type : objectTypes) {
			Class<? extends Entity> each = type.getJavaClass();

			// Get the list of instances for this entity class
			// sort the list alphabetically
			ArrayList<Entity> cloneList = new ArrayList<>();
			for (Entity ent : simModel.getInstanceIterator(each)) {
				if (simModel.isPreDefinedEntity(ent)) {
					if (! (ent instanceof Simulation) ) {
						continue;
					}
				}

				cloneList.add(ent);
			}

			// Print the entity class name to the report (in the form of a comment)
			if (cloneList.size() > 0) {
				inputReportFile.write("\" " + each.getSimpleName() + " \"");
				inputReportFile.newLine();
				inputReportFile.newLine(); // blank line below the class name heading
			}

			Collections.sort(cloneList, new Comparator<Entity>() {
				@Override
				public int compare(Entity a, Entity b) {
					return a.getName().compareTo(b.getName());
				}
			});

			// Loop through the instances for this entity class
			for (int j = 0; j < cloneList.size(); j++) {

				// Make sure the clone is an instance of the class (and not an instance of a subclass)
				if (cloneList.get(j).getClass() != each)
					continue;

				Entity ent = cloneList.get(j);
				String entityName = ent.getName();
				boolean hasinput = false;

				// Loop through the editable Key Inputs for this instance
				for (Input<?> in : ent.getEditableInputs()) {
					if (in.isSynonym())
						continue;

					// If the keyword has been used, then add a record to the report
					String valueString = in.getValueString();
					if (valueString.length() == 0)
						continue;

					if (! in.getCategory().contains(Entity.KEY_INPUTS))
						continue;

					hasinput = true;
					inputReportFile.write("\t");
					inputReportFile.write(entityName);
					inputReportFile.write("\t");
					inputReportFile.write(in.getKeyword());
					inputReportFile.write("\t");
					if (valueString.lastIndexOf('{') > 10) {
						String[] item1Array;
						item1Array = valueString.trim().split(" }");

						inputReportFile.write("{ " + item1Array[0] + " }");
						for (int l = 1; l < (item1Array.length); l++) {
							inputReportFile.newLine();
							inputReportFile.write("\t\t\t\t\t");
							inputReportFile.write(item1Array[l] + " } ");
						}
						inputReportFile.write("	}");
					}
					else {
						inputReportFile.write("{ " + valueString + " }");
					}
					inputReportFile.newLine();
				}

				// Loop through the editable keywords
				// (except for Key Inputs) for this instance
				for (Input<?> in : ent.getEditableInputs()) {
					if (in.isSynonym())
						continue;

					// If the keyword has been used, then add a record to the report
					String valueString = in.getValueString();
					if (valueString.length() == 0)
						continue;

					if (in.getCategory().contains(Entity.KEY_INPUTS))
						continue;

					hasinput = true;
					inputReportFile.write("\t");
					inputReportFile.write(entityName);
					inputReportFile.write("\t");
					inputReportFile.write(in.getKeyword());
					inputReportFile.write("\t");
					if (valueString.lastIndexOf('{') > 10) {
						String[] item1Array;
						item1Array = valueString.trim().split(" }");

						inputReportFile.write("{ " + item1Array[0] + " }");
						for (int l = 1; l < (item1Array.length); l++) {
							inputReportFile.newLine();
							inputReportFile.write("\t\t\t\t\t");
							inputReportFile.write(item1Array[l] + " } ");
						}
						inputReportFile.write("	}");
					}
					else {
						inputReportFile.write("{ " + valueString + " }");
					}
					inputReportFile.newLine();
				}

				// Put a blank line after each instance
				if (hasinput) {
					inputReportFile.newLine();
				}
			}
		}

		// Close out the report
		inputReportFile.flush();
		inputReportFile.close();

	}

	private static final String errPrefix = "*** ERROR *** %s%n";
	private static final String inpErrPrefix = "*** INPUT ERROR *** %s%n";
	private static final String wrnPrefix = "***WARNING*** %s%n";

	private static void echoInputRecord(JaamSimModel simModel, ArrayList<String> tokens) {
		FileEntity logFile = simModel.getLogFile();
		if (logFile == null)
			return;

		boolean beginLine = true;
		for (int i = 0; i < tokens.size(); i++) {
			if (!beginLine)
				logFile.write(Input.SEPARATOR);
			String tok = tokens.get(i);
			logFile.write(tok);
			beginLine = false;
		}
		// If there were any leftover string written out, make sure the line gets terminated
		if (!beginLine)
			logFile.newLine();

		logFile.flush();
	}

	private static void logBadInput(JaamSimModel simModel, ArrayList<String> tokens, String msg) {
		InputAgent.echoInputRecord(simModel, tokens);
		InputAgent.logError(simModel, "%s", msg);
	}

	public static void logMessage(String fmt, Object... args) {  //FIXME delete when possible
		logMessage(null, fmt, args);
	}

	/**
	 * Writes an error or warning message to standard error, the Log Viewer, and the Log File.
	 * @param fmt - format for the message
	 * @param args - objects to be printed in the message
	 */
	public static void logMessage(JaamSimModel simModel, String fmt, Object... args) {
		String msg = String.format(fmt, args);
		LogBox.logLine(msg);
		System.err.println(msg);
		if (simModel == null)  //FIXME delete when possible
			return;
		simModel.logMessage(msg);
	}

	public static void logStackTrace(Throwable t) {  //FIXME delete when possible
		logStackTrace(null, t);
	}

	/**
	 * Writes a stack trace to standard error, the Log Viewer, and the Log File.
	 * @param e - exception to be traced
	 */
	public static void logStackTrace(JaamSimModel simModel, Throwable t) {
		for (StackTraceElement each : t.getStackTrace()) {
			InputAgent.logMessage(simModel, each.toString());
		}
	}

	public static final void trace(JaamSimModel simModel, int indent, Entity ent, String fmt, Object... args) {
		// Print a TIME header every time time has advanced
		long traceTick = EventManager.simTicks();
		if (simModel.getLastTickForTrace() != traceTick) {
			double unitFactor = Unit.getDisplayedUnitFactor(TimeUnit.class);
			String unitString = Unit.getDisplayedUnit(TimeUnit.class);
			System.out.format(" \nTIME = %.6f %s,  TICKS = %d\n",
					EventManager.current().ticksToSeconds(traceTick) / unitFactor, unitString,
					traceTick);
			simModel.setLastTickForTrace(traceTick);
		}

		// Create an indent string to space the lines
		StringBuilder str = new StringBuilder("");
		for (int i = 0; i < indent; i++)
			str.append("   ");

		// Append the Entity name if provided
		if (ent != null)
			str.append(ent.toString()).append(".");

		str.append(String.format(fmt, args));
		System.out.println(str.toString());
		System.out.flush();
	}

	/**
	 * Writes a warning message to standard error, the Log Viewer, and the Log File.
	 * @param fmt - format string for the warning message
	 * @param args - objects used by the format string
	 */
	public static void logWarning(JaamSimModel simModel, String fmt, Object... args) {
		simModel.recordWarning();
		String msg = String.format(fmt, args);
		InputAgent.logMessage(simModel, wrnPrefix, msg);
	}

	/**
	 * Writes an error message to standard error, the Log Viewer, and the Log File.
	 * @param fmt - format string for the error message
	 * @param args - objects used by the format string
	 */
	public static void logError(JaamSimModel simModel, String fmt, Object... args) {
		simModel.recordError();
		String msg = String.format(fmt, args);
		InputAgent.logMessage(simModel, errPrefix, msg);
	}

	/**
	 * Writes a input error message to standard error, the Log Viewer, and the Log File.
	 * @param fmt - format string for the error message
	 * @param args - objects used by the format string
	 */
	public static void logInpError(JaamSimModel simModel, String fmt, Object... args) {
		simModel.recordError();
		String msg = String.format(fmt, args);
		InputAgent.logMessage(simModel, inpErrPrefix, msg);
	}

	/**
	 * Prints the present state of the model to a new configuration file.
	 *
	 * @param fileName - the full path and file name for the new configuration file.
	 */
	public static void printNewConfigurationFileWithName(JaamSimModel simModel, File f) {

		// 1) WRITE LINES FROM THE ORIGINAL CONFIGURATION FILE

		// Copy the original configuration file up to the "RecordEdits" marker (if present)
		// Temporary storage for the copied lines is needed in case the original file is to be overwritten
		ArrayList<String> preAddedRecordLines = new ArrayList<>();
		if( simModel.getConfigFile() != null ) {
			try {
				BufferedReader in = new BufferedReader( new FileReader(simModel.getConfigFile()) );
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
		FileEntity file = new FileEntity(f);
		for( int i=0; i < preAddedRecordLines.size(); i++ ) {
			file.format("%s%n", preAddedRecordLines.get( i ));
		}

		// If not already present, insert the "RecordEdits" marker at the end of the original configuration file
		if (!simModel.isRecordEditsFound()) {
			file.format("%n%s%n", recordEditsMarker);
			simModel.setRecordEditsFound(true);
		}

		// 2) WRITE THE DEFINITION STATEMENTS FOR NEW OBJECTS

		// Prepare a sorted list of all the entities that were added to the model
		ArrayList<Entity> newEntities = new ArrayList<>();
		for (Entity ent : simModel.getClonesOfIterator(Entity.class)) {
			if (!ent.isAdded() || ent.isGenerated())
				continue;
			if (ent instanceof EntityLabel && !((EntityLabel) ent).getShow()
					&& ((EntityLabel) ent).isDefault())
				continue;
			newEntities.add(ent);
		}
		Collections.sort(newEntities, uiEntitySortOrder);

		// Add a blank line before the first object definition
		if (!newEntities.isEmpty())
			file.format("%n");

		// Print the first part of the "Define" statement for this object type
		Class<? extends Entity> entClass = null;
		int level = 0;
		for (Entity ent : newEntities) {

			// Is the class different from the last one
			if (ent.getClass() != entClass) {

				// Close the previous Define statement
				if (entClass != null) {
					file.format("}%n");
				}

				// Add a blank line between sub-model levels
				if (ent.getSubModelLevel() != level) {
					level = ent.getSubModelLevel();
					file.format("%n");
				}

				// Start the new Define statement
				entClass = ent.getClass();
				ObjectType ot = simModel.getObjectTypeForClass(entClass);
				file.format("Define %s {", ot.getName());
			}

			// Print the entity name to the Define statement
			file.format(" %s ", ent.getName());
		}

		// Close the define statement
		if (!newEntities.isEmpty())
			file.format("}%n");

		// 3) WRITE THE INPUTS FOR SPECIAL KEYWORDS THAT MUST COME BEFORE THE OTHERS

		// Prepare a sorted list of all the entities that were edited
		ArrayList<Entity> entityList = new ArrayList<>();
		for (Entity ent : simModel.getClonesOfIterator(Entity.class)) {
			if (!ent.isEdited() || !ent.isRegistered())
				continue;
			if (ent instanceof EntityLabel && !((EntityLabel) ent).getShow()
					&& ((EntityLabel) ent).isDefault())
				continue;
			entityList.add(ent);
		}
		Collections.sort(entityList, uiEntitySortOrder);

		// Write a stub definition for the Custom Outputs for each entity
		boolean blankLinePrinted = false;
		for (Entity ent : entityList) {
			Input<?> in = ent.getInput("CustomOutputList");
			if (in == null || !in.isEdited())
				continue;
			if (!blankLinePrinted) {
				file.format("%n");
				blankLinePrinted = true;
			}
			writeStubOutputDefs(file, ent);
		}

		// Loop through the early keywords
		for (int i = 0; i < EARLY_KEYWORDS.length; i++) {

			// Loop through the entities
			blankLinePrinted = false;
			for (Entity ent : entityList) {

				// Print an entry for each entity that used this keyword
				final Input<?> in = ent.getInput(EARLY_KEYWORDS[i]);
				if (in != null && in.isEdited()) {
					if (!blankLinePrinted) {
						file.format("%n");
						blankLinePrinted = true;
					}
					writeInputOnFile_ForEntity(file, ent, in);
				}
			}
		}

		// 4) WRITE THE INPUTS FOR THE REMAINING KEYWORDS

		// 4.1) Non-graphics inputs for non-graphic entities
		entClass = null;
		for (Entity ent : entityList) {
			if (isGraphicsEntity(ent))
				continue;

			// Print a header if the entity class is new
			if (ent.getClass() != entClass) {
				entClass = ent.getClass();
				if (entClass != Simulation.class) {
					ObjectType ot = simModel.getObjectTypeForClass(entClass);
					file.format("%n");
					file.format("# *** %s ***%n", ot);
				}
			}
			file.format("%n");

			for (Input<?> in : ent.getEditableInputs()) {
				if (in.isSynonym() || !in.isEdited() || isEarlyInput(in) || isGraphicsInput(in))
					continue;
				writeInputOnFile_ForEntity(file, ent, in);
			}
		}

		// 4.2) Graphics inputs for non-graphic entities
		file.format("%n");
		file.format("# *** GRAPHICS INPUTS ***%n");
		for (Entity ent : entityList) {
			if (isGraphicsEntity(ent))
				continue;
			file.format("%n");

			for (Input<?> in : ent.getEditableInputs()) {
				if (in.isSynonym() || !in.isEdited() || isEarlyInput(in) || !isGraphicsInput(in))
					continue;
				writeInputOnFile_ForEntity(file, ent, in);
			}
		}

		// 4.3) All inputs for graphic entities
		entClass = null;
		for (Entity ent : entityList) {
			if (!isGraphicsEntity(ent))
				continue;

			// Print a header if the entity class is new
			if (ent.getClass() != entClass) {
				entClass = ent.getClass();
				if (entClass != Simulation.class) {
					ObjectType ot = simModel.getObjectTypeForClass(entClass);
					file.format("%n");
					file.format("# *** %s ***%n", ot);
				}
			}
			file.format("%n");

			for (Input<?> in : ent.getEditableInputs()) {
				if (in.isSynonym() || !in.isEdited() || isEarlyInput(in))
					continue;
				writeInputOnFile_ForEntity(file, ent, in);
			}
		}

		// Close the new configuration file
		file.flush();
		file.close();

		simModel.setSessionEdited(false);
	}

	public static boolean isEarlyInput(Input<?> in) {
		String key = in.getKeyword();
		return Arrays.asList(EARLY_KEYWORDS).contains(key);
	}

	public static boolean isGraphicsInput(Input<?> in) {
		String cat = in.getCategory();
		return Arrays.asList(GRAPHICS_CATEGORIES).contains(cat);
	}

	public static boolean isGraphicsEntity(Entity ent) {
		String pal = ent.getObjectType().getPaletteName();
		return Arrays.asList(GRAPHICS_PALETTES).contains(pal);
	}

	static void writeInputOnFile_ForEntity(FileEntity file, Entity ent, Input<?> in) {
		file.format("%s %s { %s }%n",
		            ent.getName(), in.getKeyword(), in.getValueString());
	}

	static void writeStubOutputDefs(FileEntity file, Entity ent) {
		NamedExpressionListInput in = (NamedExpressionListInput) ent.getInput("CustomOutputList");
		if (in == null || in.isDefault()) {
			return;
		}
		file.format("%s %s { %s }%n", ent.getName(), in.getKeyword(), in.getStubDefinition());
	}

	public static void printRunOutputHeaders(JaamSimModel simModel, PrintStream outStream) {
		Simulation simulation = simModel.getSimulation();

		// Write the header line for the expressions
		StringBuilder sb = new StringBuilder();
		ArrayList<String> toks = new ArrayList<>();
		simulation.getRunOutputList().getValueTokens(toks);
		boolean first = true;
		for (String str : toks) {
			if (str.equals("{") || str.equals("}"))
				continue;
			if (first)
				first = false;
			else
				sb.append("\t");
			sb.append(str);
		}
		outStream.println(sb.toString());
	}

	/**
	 * Prints selected outputs for the simulation run to stdout or a file.
	 * @param simTime - simulation time at which the outputs are printed.
	 */
	public static void printRunOutputs(JaamSimModel simModel, PrintStream outStream, double simTime) {
		Simulation simulation = simModel.getSimulation();

		// Write the selected outputs
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < simulation.getRunOutputList().getListSize(); i++) {
			StringProvider samp = simulation.getRunOutputList().getValue().get(i);
			String str;
			try {
				str = samp.getNextString(simTime);
			} catch (Exception e) {
				str = e.getMessage();
			}
			if (i > 0)
				sb.append("\t");
			sb.append(str);
		}
		outStream.println(sb.toString());

		// Terminate the outputs
		if (simModel.isLastRun()) {
			outStream.close();
			outStream = null;
		}
	}


	private static final String OUTPUT_FORMAT = "%s\t%s\t%s\t%s%n";
	private static final String LIST_OUTPUT_FORMAT = "%s\t%s[%s]\t%s\t%s%n";

	/**
	 * Writes the entry in the output report for this entity.
	 * @param file - the file in which the outputs are written
	 * @param simTime - simulation time at which the outputs are evaluated
	 */
	public static void printReport(Entity ent, FileEntity file, double simTime) {

		// Loop through the outputs
		ArrayList<OutputHandle> handles = OutputHandle.getOutputHandleList(ent);
		for (OutputHandle out : handles) {

			// Should this output appear in the report?
			if (!out.isReportable())
				continue;

			// Determine the preferred unit for this output
			Class<? extends Unit> ut = out.getUnitType();
			double factor = Unit.getDisplayedUnitFactor(ut);
			String unitString = Unit.getDisplayedUnit(ut);
			if (ut == Unit.class || ut == DimensionlessUnit.class)
				unitString = "-";

			// Numerical output
			if (out.isNumericValue()) {
				try {
					double val = out.getValueAsDouble(simTime, Double.NaN)/factor;
					file.format(OUTPUT_FORMAT,
							ent.getName(), out.getName(), val, unitString);
				}
				catch (Exception e) {
					file.format(OUTPUT_FORMAT,
							ent.getName(), out.getName(), Double.NaN, unitString);
				}
			}

			// double[] output
			else if (out.getReturnType() == double[].class) {
				double[] vec = out.getValue(simTime, double[].class);
				for (int i = 0; i < vec.length; i++) {
					file.format(LIST_OUTPUT_FORMAT,
							ent.getName(), out.getName(), i, vec[i]/factor, unitString);
				}
			}

			// DoubleVector output
			else if (out.getReturnType() == DoubleVector.class) {
				DoubleVector vec = out.getValue(simTime, DoubleVector.class);
				for (int i=0; i<vec.size(); i++) {
					double val = vec.get(i);
					file.format(LIST_OUTPUT_FORMAT,
							ent.getName(), out.getName(), i, val/factor, unitString);
				}
			}

			// ArrayList output
			else if (out.getReturnType() == ArrayList.class) {
				ArrayList<?> array = out.getValue(simTime, ArrayList.class);
				for (int i=0; i<array.size(); i++) {
					Object obj = array.get(i);
					if (obj instanceof Double) {
						double val = (Double)obj;
						file.format(LIST_OUTPUT_FORMAT,
								ent.getName(), out.getName(), i, val/factor, unitString);
					}
					else {
						file.format(LIST_OUTPUT_FORMAT,
							ent.getName(), out.getName(), i, obj, unitString);
					}
				}
			}

			// Keyed output
			else if (out.getReturnType() == LinkedHashMap.class) {
				LinkedHashMap<?, ?> map = out.getValue(simTime, LinkedHashMap.class);
				for (Entry<?, ?> mapEntry : map.entrySet()) {
					Object obj = mapEntry.getValue();
					if (obj instanceof Double) {
						double val = (Double)obj;
						file.format(LIST_OUTPUT_FORMAT,
								ent.getName(), out.getName(), mapEntry.getKey(), val/factor, unitString);
					}
					else {
						file.format(LIST_OUTPUT_FORMAT,
								ent.getName(), out.getName(), mapEntry.getKey(), obj, unitString);
					}
				}
			}
			// Expression based custom outputs
			else if (out.getReturnType() == ExpResult.class) {
				String val = InputAgent.getValueAsString(out, simTime, "%s", factor);
				file.format(OUTPUT_FORMAT,
						ent.getName(), out.getName(), val, unitString);
			}

			// All other outputs
			else {
				if (ut != Unit.class && ut != DimensionlessUnit.class)
					unitString = Unit.getSIUnit(ut);  // other outputs are not converted to preferred units
				String str = out.getValue(simTime, out.getReturnType()).toString();
				file.format(OUTPUT_FORMAT,
						ent.getName(), out.getName(), str, unitString);
			}
		}
	}

	/**
	 * Prints the output report for the simulation run.
	 * @param simTime - simulation time at which the report is printed.
	 */
	public static void printReport(JaamSimModel simModel, double simTime) {

		// Create the report file
		FileEntity reportFile = simModel.getReportFile();

		// Print run number header when multiple runs are to be performed
		if (simModel.isMultipleRuns())
			reportFile.format("%s%n%n", simModel.getRunHeader());

		// Prepare a sorted list of entities
		ArrayList<Entity> entList = new ArrayList<>();
		for (Entity ent : simModel.getClonesOfIterator(Entity.class)) {

			if (!ent.isRegistered())
				continue;

			if (!ent.isReportable())
				continue;

			entList.add(ent);
		}
		Collections.sort(entList, uiEntitySortOrder);

		// Loop through the entities
		Class<? extends Entity> entClass = null;
		for (Entity ent : entList) {

			// Print a header if the entity class is new
			if (ent.getClass() != entClass) {
				entClass = ent.getClass();
				if (entClass != Simulation.class) {
					ObjectType ot = simModel.getObjectTypeForClass(entClass);
					reportFile.format("*** %s ***%n%n", ot);
				}
			}

			// Print the report for the entity
			InputAgent.printReport(ent, reportFile, simTime);
			reportFile.format("%n");
		}

		// Close the report file
		if (simModel.isLastRun()) {
			reportFile.close();
			reportFile = null;
		}
	}

	private static class EntityComparator implements Comparator<Entity> {
		@Override
		public int compare(Entity ent0, Entity ent1) {

			// Place the Simulation entity in the first position
			Class<? extends Entity> class0 = ent0.getClass();
			Class<? extends Entity> class1 = ent1.getClass();
			boolean isSim0 = (class0 == Simulation.class);
			boolean isSim1 = (class1 == Simulation.class);
			int ret = Boolean.compare(isSim1, isSim0);  // Simulation goes first
			if (ret != 0)
				return ret;

			// First sort by sub-model level
			int sub0 = ent0.getSubModelLevel();
			int sub1 = ent1.getSubModelLevel();
			ret = Integer.compare(sub0, sub1);
			if (ret != 0)
				return ret;

			ObjectType ot0 = ent0.getJaamSimModel().getObjectTypeForClass(class0);
			ObjectType ot1 = ent1.getJaamSimModel().getObjectTypeForClass(class1);
			String pal0 = ot0.getPaletteName();
			String pal1 = ot1.getPaletteName();

			// If the levels are the same, then sort by graphics vs non-graphics palettes
			boolean isGraf0 = Arrays.asList(GRAPHICS_PALETTES).contains(pal0);
			boolean isGraf1 = Arrays.asList(GRAPHICS_PALETTES).contains(pal1);
			ret = Boolean.compare(isGraf0, isGraf1);  // Non-graphics goes first
			if (ret != 0)
				return ret;

			// If the graphics types are the same, then sort alphabetically by palette name
			ret = Input.uiSortOrder.compare(pal0, pal1);
			if (ret != 0)
				return ret;

			// If the palettes are the same, then sort alphabetically by class name
			ret = Input.uiSortOrder.compare(ot0, ot1);
			if (ret != 0)
				return ret;

			// If the classes are the same, then sort alphabetically by entity name
			return Input.uiSortOrder.compare(ent0, ent1);
		}
	}
	public static final Comparator<Entity> uiEntitySortOrder = new EntityComparator();

	/**
	 * Returns a formated string for the specified output.
	 * @param out - output
	 * @param simTime - present simulation time
	 * @param floatFmt - format string for numerical values
	 * @param factor - divisor to be applied to numerical values
	 * @return formated string for the output
	 */
	public static String getValueAsString(OutputHandle out, double simTime, String floatFmt, double factor) {
		StringBuilder sb = new StringBuilder();
		String str;
		String COMMA_SEPARATOR = ", ";

		Class<?> retType = out.getReturnType();

		// Numeric outputs
		if (out.isNumericValue()) {
			double val = out.getValueAsDouble(simTime, Double.NaN);
			return String.format(floatFmt, val/factor);
		}

		// double[] outputs
		if (retType == double[].class) {
			double[] val = out.getValue(simTime, double[].class);
			sb.append("{");
			for (int i=0; i<val.length; i++) {
				if (i > 0)
					sb.append(COMMA_SEPARATOR);
				str = String.format(floatFmt, val[i]/factor);
				sb.append(str);
			}
			sb.append("}");
			return sb.toString();
		}

		// double[][] outputs
		if (retType == double[][].class) {
			double[][] val = out.getValue(simTime, double[][].class);
			sb.append("{");
			for (int i=0; i<val.length; i++) {
				if (i > 0)
					sb.append(COMMA_SEPARATOR);
				sb.append("{");
				for (int j=0; j<val[i].length; j++) {
					if (j > 0)
						sb.append(COMMA_SEPARATOR);
					str = String.format(floatFmt, val[i][j]/factor);
					sb.append(str);
				}
				sb.append("}");
			}
			sb.append("}");
			return sb.toString();
		}

		// int[] outputs
		if (retType == int[].class) {
			int[] val = out.getValue(simTime, int[].class);
			sb.append("{");
			for (int i=0; i<val.length; i++) {
				if (i > 0)
					sb.append(COMMA_SEPARATOR);
				str = String.format("%s", val[i]);
				sb.append(str);
			}
			sb.append("}");
			return sb.toString();
		}

		// Vec3d outputs
		if (retType == Vec3d.class) {
			Vec3d vec = out.getValue(simTime, Vec3d.class);
			sb.append(vec.x/factor);
			sb.append(Input.SEPARATOR).append(vec.y/factor);
			sb.append(Input.SEPARATOR).append(vec.z/factor);
			return sb.toString();
		}

		// DoubleVector output
		if (retType == DoubleVector.class) {
			sb.append("{");
			DoubleVector vec = out.getValue(simTime, DoubleVector.class);
			for (int i=0; i<vec.size(); i++) {
				str = String.format(floatFmt, vec.get(i)/factor);
				sb.append(str);
				if (i < vec.size()-1) {
					sb.append(COMMA_SEPARATOR);
				}
			}
			sb.append("}");
			return sb.toString();
		}

		// ArrayList output
		if (retType == ArrayList.class) {
			sb.append("{");
			ArrayList<?> array = out.getValue(simTime, ArrayList.class);
			for (int i=0; i<array.size(); i++) {
				if (i > 0)
					sb.append(COMMA_SEPARATOR);
				Object obj = array.get(i);
				if (obj instanceof Double) {
					double val = (Double)obj;
					sb.append(String.format(floatFmt, val/factor));
				}
				else if (obj instanceof ArrayList) {
					ArrayList<?> list = (ArrayList<?>) obj;
					sb.append("{");
					for (int j=0; j<list.size(); j++) {
						if (j > 0)
							sb.append(COMMA_SEPARATOR);
						sb.append(list.get(j).toString());
					}
					sb.append("}");
				}
				else {
					sb.append(String.format("%s", obj));
				}
			}
			sb.append("}");
			return sb.toString();
		}

		// Keyed outputs
		if (retType == LinkedHashMap.class) {
			sb.append("{");
			LinkedHashMap<?, ?> map = out.getValue(simTime, LinkedHashMap.class);
			boolean first = true;
			for (Entry<?, ?> mapEntry : map.entrySet()) {
				if (first)
					first = false;
				else
					sb.append(COMMA_SEPARATOR);

				sb.append(String.format("%s=", mapEntry.getKey()));
				Object obj = mapEntry.getValue();
				if (obj instanceof Double) {
					double val = (Double)obj;
					sb.append(String.format(floatFmt, val/factor));
				}
				else if (obj instanceof LinkedHashMap) {
					sb.append("{");
					LinkedHashMap<?, ?> innerMap = (LinkedHashMap<?, ?>)obj;
					boolean innerFirst = true;
					for (Entry<?, ?> innerMapEntry : innerMap.entrySet()) {
						if (innerFirst)
							innerFirst = false;
						else
							sb.append(COMMA_SEPARATOR);

						sb.append(String.format("%s=", innerMapEntry.getKey()));
						Object innerMapObj = innerMapEntry.getValue();
						if (innerMapObj instanceof Double) {
							double val = (Double)innerMapObj;
							sb.append(String.format(floatFmt, val/factor));
						}
						else {
							sb.append(String.format("%s", obj));
						}
					}
					sb.append("}");
				}
				else {
					sb.append(String.format("%s", obj));
				}
			}
			sb.append("}");
			return sb.toString();
		}

		if (retType == ExpResult.class) {
			ExpResult result = out.getValue(simTime, ExpResult.class);
			switch (result.type) {
			case STRING:
				sb.append(result.stringVal);
				break;
			case ENTITY:
				if (result.entVal == null)
					sb.append("null");
				else
					sb.append("[").append(result.entVal.getName()).append("]");
				break;
			case NUMBER:
				sb.append(String.format(floatFmt, result.value/factor));
				break;
			case COLLECTION:
				sb.append(result.colVal.getOutputString());
				break;
			default:
				assert(false);
				sb.append("???");
				break;
			}
			return sb.toString();
		}
		// All other outputs
		final Object ret = out.getValue(simTime, retType);
		if (ret != null)
			return ret.toString();
		else
			return "null";
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
	static public String getRelativeFilePath(JaamSimModel simModel, URI uri) {

		// Relativize the file path against the resources folder
		String resString = resRoot.toString();
		String inputString = uri.toString();
		if (inputString.startsWith(resString)) {
			return String.format("<res>/%s", inputString.substring(resString.length()));
		}

		// Relativize the file path against the configuration file
		try {
			URI configDirURI = simModel.getConfigFile().getParentFile().toURI();
			return String.format("%s", configDirURI.relativize(uri).getPath());
		}
		catch (Exception ex) {
			return String.format("%s", uri.getPath());
		}
	}

	/**
	 * Loads the default configuration file.
	 */
	public static void loadDefault(JaamSimModel simModel) {

		// Read the default configuration file
		InputAgent.readResource(simModel, "<res>/inputs/default.cfg");

		// A RecordEdits marker in the default configuration must be ignored
		simModel.setRecordEditsFound(false);

		// Set the model state to unedited
		simModel.setSessionEdited(false);
	}

	private static final DecimalFormat coordFormat = (DecimalFormat)NumberFormat.getNumberInstance(Locale.US);
	static {
		coordFormat.applyPattern("0.0#####");
	}

	public static KeywordIndex formatPointsInputs(String keyword, ArrayList<Vec3d> points, Vec3d offset) {
		String unitStr = Unit.getDisplayedUnit(DistanceUnit.class);
		double factor = Unit.getDisplayedUnitFactor(DistanceUnit.class);
		ArrayList<String> tokens = new ArrayList<>(points.size() * 6);
		for (Vec3d v : points) {
			tokens.add("{");
			tokens.add(coordFormat.format((v.x + offset.x)/factor));
			tokens.add(coordFormat.format((v.y + offset.y)/factor));
			tokens.add(coordFormat.format((v.z + offset.z)/factor));
			tokens.add(unitStr);
			tokens.add("}");
		}
		return new KeywordIndex(keyword, tokens, null);
	}

	public static KeywordIndex formatVec3dInput(String keyword, Vec3d point, Class<? extends Unit> ut) {
		String unitStr = Unit.getDisplayedUnit(ut);
		double factor = Unit.getDisplayedUnitFactor(ut);
		ArrayList<String> tokens = new ArrayList<>(4);
		tokens.add(coordFormat.format(point.x/factor));
		tokens.add(coordFormat.format(point.y/factor));
		tokens.add(coordFormat.format(point.z/factor));
		if (!unitStr.isEmpty()) {
			tokens.add(unitStr);
		}
		return new KeywordIndex(keyword, tokens, null);
	}

	public static KeywordIndex formatArgs(String keyword, String... args) {
		ArrayList<String> tokens = new ArrayList<>(args.length);
		for (String each : args) {
			tokens.add(each);
		}
		return new KeywordIndex(keyword, tokens, null);
	}

	public static KeywordIndex formatBoolean(String keyword, boolean bool) {
		String str = "FALSE";
		if (bool)
			str = "TRUE";
		return formatArgs(keyword, str);
	}

	public static KeywordIndex formatIntegers(String keyword, int... args) {
		ArrayList<String> tokens = new ArrayList<>(args.length);
		for (int each : args) {
			tokens.add(String.format((Locale)null, "%d", each));
		}
		return new KeywordIndex(keyword, tokens, null);
	}

	public static KeywordIndex formatDoubleInput(String keyword, double val, String unit) {
		ArrayList<String> tokens = new ArrayList<>(2);
		tokens.add(String.format((Locale)null, "%s", val));
		if (unit != null && !unit.isEmpty())
			tokens.add(unit);
		return new KeywordIndex(keyword, tokens, null);
	}

	public static KeywordIndex formatInput(String keyword, String str) {
		ArrayList<String> tokens = new ArrayList<>();
		Parser.tokenize(tokens, str, true);
		return new KeywordIndex(keyword, tokens, null);
	}

	/**
	 * Split an input (list of strings) down to a single level of nested braces, this may then be called again for
	 * further nesting.
	 * @param input
	 * @return
	 */
	public static ArrayList<ArrayList<String>> splitForNestedBraces(List<String> input) {
		ArrayList<ArrayList<String>> inputs = new ArrayList<>();

		int braceDepth = 0;
		ArrayList<String> currentLine = null;
		for (int i = 0; i < input.size(); i++) {
			if (currentLine == null)
				currentLine = new ArrayList<>();

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
			InputAgent.logMessage("Failed jail test: %s\n"
					+ "jail: %s\n"
					+ "context: %s\n",
					ret.toString(), jailPrefix, context.toString());
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
