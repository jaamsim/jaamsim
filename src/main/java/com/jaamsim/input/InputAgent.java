/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2009-2011 Ausenco Engineering Canada Inc.
 * Copyright (C) 2018-2026 JaamSim Software Inc.
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
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map.Entry;

import com.jaamsim.Graphics.AbstractDirectedEntity;
import com.jaamsim.Graphics.EntityLabel;
import com.jaamsim.Statistics.SampleStatistics;
import com.jaamsim.basicsim.Entity;
import com.jaamsim.basicsim.ErrorException;
import com.jaamsim.basicsim.FileEntity;
import com.jaamsim.basicsim.GUIListener;
import com.jaamsim.basicsim.Group;
import com.jaamsim.basicsim.JaamSimModel;
import com.jaamsim.basicsim.ObjectType;
import com.jaamsim.basicsim.Scenario;
import com.jaamsim.basicsim.SimRun;
import com.jaamsim.basicsim.Simulation;
import com.jaamsim.datatypes.DoubleVector;
import com.jaamsim.math.Vec3d;
import com.jaamsim.units.DimensionlessUnit;
import com.jaamsim.units.DistanceUnit;
import com.jaamsim.units.Unit;

public class InputAgent {
	private static final String recordEditsMarker = "RecordEdits";

	public static final String INP_ERR_DEFINEUSED = "The name: %s has already been used and is a %s";
	public static final String INP_ERR_BADNAME = "An entity name cannot be blank or contain "
	                                            + "spaces, tabs, braces, single or double quotes,\n"
	                                            + "square brackets, hash characters, or periods.\n"
	                                            + "Name: %s";
	private static final String INP_ERR_BADPARENT = "The parent entity [%s] has not been defined.";
	static final char[] INVALID_ENTITY_CHARS = new char[]{' ', '\t', '\n', '{', '}', '\'', '"', '[', ']', '#','.'};

	public static final String[] EARLY_KEYWORDS = { "Prototype",
	                                                "TickLength",
	                                                "ConversionFactorToSI",
	                                                "UnitType", "UnitTypeList", "OutputUnitType",
	                                                "SecondaryUnitType", "XAxisUnitType",
	                                                "GregorianCalendar", "StartDate", "DataFile",
	                                                "AttributeDefinitionList", "CustomOutputList",
	                                                "KeywordList"};
	private static final String[] GRAPHICS_PALETTES = {"Graphics Objects", "View", "Display Models"};
	private static final String[] GRAPHICS_CATEGORIES = {Entity.GRAPHICS, Entity.FONT, Entity.FORMAT, Entity.GUI};

	private static final String COMMA_SEPARATOR = ", ";

	private static final int MAX_BRACE_DEPTH = 3;

	private static int getBraceDepth(JaamSimModel simModel, ArrayList<String> tokens, int startingBraceDepth, int startingIndex) {
		int braceDepth = startingBraceDepth;
		for (int i = startingIndex; i < tokens.size(); i++) {
			String token = tokens.get(i);

			if (token.equals("{"))
				braceDepth++;

			if (token.equals("}"))
				braceDepth--;
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

	/**
	 * Reads model inputs from the specified source.
	 * <p>
	 * The specified file path string can be absolute or relative to a reference folder.
	 * It can also contain the keyword '&LTres&GT' for the case of a resource file.
	 * In the case of a relative file path, a 'context' folder must be specified.
	 * A 'context' of null indicates an absolute file path.
	 * A 'jailPrefix' of null indicates no restriction.
	 * <p>
	 * @param simModel - simulation model to be populated
	 * @param jailPrefix - file path to a base folder from which a relative path cannot escape
	 * @param context - URI for the folder that is the reference for relative file paths
	 * @param filePath - file path string for the model inputs
	 * @return true if the inputs were read
	 * @throws URISyntaxException
	 */
	public static final boolean readStream(JaamSimModel simModel, String jailPrefix, URI context,
			String filePath) throws URISyntaxException {
		URI resolved = getFileURI(simModel, context, filePath, jailPrefix);

		URL url = null;
		try {
			url = resolved.normalize().toURL();
		}
		catch (MalformedURLException e) {
			rethrowWrapped(e);
		}

		if (url == null) {
			simModel.logError("Unable to resolve path %s%s - %s", jailPrefix, context.toString(), filePath);
			return false;
		}

		BufferedReader buf = null;
		try {
			InputStream in = url.openStream();
			buf = new BufferedReader(new InputStreamReader(in));
		} catch (IOException e) {
			simModel.logError("Could not read from url: '%s'%n%s", url.toString(), e.getMessage());
			return false;
		}

		InputAgent.readBufferedStream(simModel, buf, resolved, jailPrefix);
		return true;
	}

	public static final void readBufferedStream(JaamSimModel simModel, BufferedReader buf, URI resolved, String root) {

		try {
			ArrayList<String> record = new ArrayList<>();
			int braceDepth = 0;
			boolean quoted = false;
			String firstLine = "";

			ParseContext pc = new ParseContext(resolved, root);

			String line = "";
			while (true) {
				String str = buf.readLine();
				// end of file, stop reading
				if (str == null)
					break;
				line = str;

				if (record.isEmpty() && !line.isEmpty())
					firstLine = line;

				int previousRecordSize = record.size();
				quoted = Parser.tokenize(record, line, quoted, true);

				// Print the inputs to the .log file
				simModel.logFileMessage(line);

				// Keep reading the input file until the opening and closing braces are matched
				braceDepth = InputAgent.getBraceDepth(simModel, record, braceDepth, previousRecordSize);

				if (braceDepth < 0 || braceDepth > MAX_BRACE_DEPTH) {
					simModel.logError("Invalid brace depth: %s", braceDepth);
					record.clear();
					braceDepth = 0;
					quoted = false;
				}

				if( braceDepth > 0 || quoted || record.isEmpty())
					continue;

				// Process the input lines

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
			if (record.size() > 0) {
				StringBuilder sb = new StringBuilder();
				sb.append("File ended before the last model input was completed:%n");
				if (braceDepth > 0)
					sb.append("- missing a closing brace ('}')%n");
				if (quoted)
					sb.append("- missing a closing single quote (')%n");
				sb.append("The bad input began with the following line:%n");
				sb.append(firstLine);
				simModel.logError(sb.toString());
			}

			buf.close();
		}
		catch (IOException e) {
			// Make best effort to ensure it closes
			try { buf.close(); } catch (IOException e2) {}
		}
	}

	private static void processIncludeRecord(JaamSimModel simModel, ParseContext pc, ArrayList<String> record) throws URISyntaxException {
		if (record.size() != 2) {
			simModel.logError("Bad Include record, should be: Include <File>");
			return;
		}
		InputAgent.readStream(simModel, pc.jail, pc.context, record.get(1).replaceAll("\\\\", "/"));
	}

	private static void processDefineRecord(JaamSimModel simModel, ArrayList<String> record) {
		if (record.size() < 5 ||
		    !record.get(2).equals("{") ||
		    !record.get(record.size() - 1).equals("}")) {
			simModel.logError("Bad Define record, should be: Define <Type> { <names>... }");
			return;
		}

		Class<? extends Entity> klass = null;
		Entity proto = null;
		try {
			if( record.get( 1 ).equalsIgnoreCase( "ObjectType" ) ) {
				klass = ObjectType.class;
			}
			else {
				Entity type = simModel.getNamedEntity(record.get(1));
				if (type == null)
					throw new InputErrorException("Entity type not found: %s", record.get(1));

				if (type instanceof ObjectType) {
					klass = Input.parseEntityType(simModel, record.get(1));
				}
				else {
					proto = type;
					klass = proto.getClass();
				}
			}
		}
		catch (InputErrorException e) {
			simModel.logError("%s", e.getMessage());
			return;
		}

		// Loop over all the new Entity names
		for (int i = 3; i < record.size() - 1; i++) {
			InputAgent.defineEntity(simModel, klass, proto, record.get(i), simModel.isRecordEdits());
		}
	}

	public static <T extends Entity> T generateEntityWithName(JaamSimModel simModel, Class<T> klass, Entity proto, String key, Entity parent,
			boolean reg, boolean retain) {
		if (key == null)
			throw new ErrorException("Must provide a name for generated Entities");

		if (!isValidName(key))
			throw new ErrorException(INP_ERR_BADNAME, key);

		T ent = simModel.createInstance(klass, proto, key, parent, false, true, reg, retain);
		if (ent == null)
			throw new ErrorException("Could not create new Entity: %s", key);

		return ent;
	}

	public static Entity getGeneratedClone(Entity proto, String name) {
		Entity ret = proto.getCloneFromPool();
		if (ret == null)
			return InputAgent.generateEntityWithName(proto.getJaamSimModel(),
					proto.getClass(), proto, name, null, false, false);

		ret.setNameInput(name);
		return ret;
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
	 * @param klass - class for the entity to be created
	 * @param proto - prototype for the newly defined Entity
	 * @param key - base absolute name for the entity to be created
	 * @param sep - string to append to the name if it is already in use
	 * @param addedEntity - true if the entity is new to the model
	 * @return new entity
	 */
	public static <T extends Entity> T defineEntityWithUniqueName(JaamSimModel simModel, Class<T> klass, Entity proto, String key, String sep, boolean addedEntity) {
		String name = getUniqueName(simModel, key, sep);
		return defineEntity(simModel, klass, proto, name, addedEntity);
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
	 * @param klass - class for the entity to be created
	 * @param proto - prototype for the entity
	 * @param key - absolute name for the entity to be created
	 * @param addedEntity - true if the entity is new to the model
	 * @return new entity
	 */
	private static <T extends Entity> T defineEntity(JaamSimModel simModel, Class<T> klass, Entity proto, String key, boolean addedEntity) {
		Entity existingEnt = Input.tryParseEntity(simModel, key, Entity.class);
		if (existingEnt != null) {
			simModel.logError(INP_ERR_DEFINEUSED, key, existingEnt.getClass().getSimpleName());
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
				simModel.logError(INP_ERR_BADPARENT, parentName);
				return null;
			}
		}

		if (!isValidName(localName)) {
			simModel.logError(INP_ERR_BADNAME, localName);
			return null;
		}

		T ent = simModel.createInstance(klass, proto, localName, parent, addedEntity, false, true, true);

		if (ent == null) {
			simModel.logError("Could not create new child Entity: %s for parent: %s", localName, parent);
			return null;
		}

		return ent;
	}

	public static void processKeywordRecord(JaamSimModel simModel, ArrayList<String> record, ParseContext context) {
		Entity ent = Input.tryParseEntity(simModel, record.get(0), Entity.class);
		if (ent == null) {
			simModel.logError("Could not find Entity: %s", record.get(0));
			return;
		}

		// Validate the tokens have the Entity Keyword { Args... } Keyword { Args... }
		ArrayList<KeywordIndex> words = InputAgent.getKeywords(record, context);
		for (KeywordIndex keyword : words) {
			try {
				InputAgent.processKeyword(ent, keyword);
			}
			catch (Throwable e) {
				simModel.logInpError("Entity: %s, Keyword: %s - %s", ent.getName(), keyword.keyword, e.getMessage());
				if (e.getMessage() == null) {
					simModel.logStackTrace(e);
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

	/**
	 * Prepares the keyword and input value for processing.
	 *
	 * @param ent - the entity whose keyword and value has been entered.
	 * @param keyword - the keyword.
	 * @param args - the input value String for the keyword.
	 */
	public static void applyArgs(Entity ent, String keyword, String... args){
		KeywordIndex kw = formatArgs(keyword, args);
		InputAgent.apply(ent, kw);
	}

	public static void applyVec3d(Entity ent, String keyword, Vec3d point, Class<? extends Unit> ut) {
		KeywordIndex kw = formatVec3dInput(ent, keyword, point, ut);
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

	private static final DecimalFormat coordFormat = (DecimalFormat)NumberFormat.getNumberInstance(Locale.US);
	static {
		coordFormat.applyPattern("0.0#####");
	}

	public static KeywordIndex formatVec3dInput(Entity ent, String keyword, Vec3d point, Class<? extends Unit> ut) {
		double factor = 1.0d;
		String unitStr = Unit.getSIUnit(ut);
		Unit u = ent.getJaamSimModel().getPreferredUnit(ut);
		if (u != null) {
			factor = u.getConversionFactorToSI();
			unitStr = u.getName();
		}
		ArrayList<String> tokens = new ArrayList<>(4);
		tokens.add(coordFormat.format(point.x/factor));
		tokens.add(coordFormat.format(point.y/factor));
		tokens.add(coordFormat.format(point.z/factor));
		if (!unitStr.isEmpty()) {
			tokens.add(unitStr);
		}
		return new KeywordIndex(keyword, tokens, null);
	}


	public static KeywordIndex formatPointsInputs(Entity ent, String keyword, ArrayList<Vec3d> points, Vec3d offset) {
		double factor = 1.0d;
		String unitStr = Unit.getSIUnit(DistanceUnit.class);
		Unit u = ent.getJaamSimModel().getPreferredUnit(DistanceUnit.class);
		if (u != null) {
			factor = u.getConversionFactorToSI();
			unitStr = u.getName();
		}
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

	public static final void apply(Entity ent, KeywordIndex kw) {
		Input<?> in = ent.getInput(kw.keyword);
		if (in == null) {
			String msg = String.format("Keyword '%s' could not be found", kw.keyword);
			throw new ErrorException(ent, msg);
		}
		InputAgent.apply(ent, in, kw);
	}

	public static final void apply(Entity ent, Input<?> in, KeywordIndex kw) {

		//if (ent.getName().startsWith("Fred") && kw.keyword.equals("NextComponent")) {
		//	System.out.format("apply - ent=%s, key=%s, args=%s%n",
		//			ent, kw.keyword, Arrays.toString(kw.getArgArray()));
		//}

		if (in.isLocked() && !ent.isGenerated()) {
			throw new InputErrorException("Input value is locked");
		}

		ArrayList<String> defaultInheritedTokens = null;
		ArrayList<String> inheritedTokens = null;
		if (ent.isClone()) {
			defaultInheritedTokens = new ArrayList<>(Arrays.asList(in.getInheritedValueArray()));
			inheritedTokens = ent.getInheritedValueTokens(in);
			//System.out.format("%ndefaultInheritedTokens=%s%n", defaultInheritedTokens);
			//System.out.format("inheritedTokens=%s%n", inheritedTokens);
		}

		// Restore the default if the input value is blank or is equal to its inherited value
		boolean changed = true;
		if (kw.numArgs() == 0 || (ent.isClone()
				&& Arrays.equals(in.getInheritedValueArray(), kw.getArgArray()))) {
			if (in.isDef())
				changed = false;
			in.reset(ent);

			// Set the inherited value if the input value is blank
			if (ent.isClone() && kw.numArgs() == 0
					&& !inheritedTokens.equals(defaultInheritedTokens)) {
				kw = new KeywordIndex(kw.keyword, inheritedTokens, kw.context);
				in.parse(ent, kw);
				in.setTokens(kw);
			}
		}

		// Ignore the input if it is the same as the present value
		else if (Arrays.equals(in.valueTokens, kw.getArgArray())) {
			changed = false;
		}

		// Otherwise, set the new input value
		else {
			in.parse(ent, kw);
			in.setTokens(kw);
		}

		// Mark the input explicitly as 'inherited' if it had to be changed from its inherited
		// value because of a reference to its parent SubModel
		in.setInherited(ent.isClone() && !in.isDef()
				&& in.getValueTokens().equals(inheritedTokens));

		// Only mark the keyword edited if we have finished initial configuration
		JaamSimModel simModel = ent.getJaamSimModel();
		if (changed && simModel.isRecordEdits() && !in.isInherited()) {
			in.setEdited(true);
			ent.setEdited();
		}

		// Execute the input callback for the entity
		in.doCallback(ent);

		// Copy the input value to any clones
		for (Entity clone : ent.getAllClones()) {
			clone.copyInput(ent, in.getKeyword(), kw.context);
		}

		// Refresh the graphics
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
		grp.saveGroupKeyword(key);

		// Store the keyword data for use in the edit table
		for (Entity ent : grp.getList()) {
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
		String fileName = simModel.getReportFileName(".inp");
		if (fileName == null)
			throw new ErrorException("Cannot create the input report file");
		File f = new File(fileName);
		if (f.exists() && !f.delete())
			throw new ErrorException("Cannot delete the existing input report file %s", f);
		FileEntity inputReportFile = new FileEntity(simModel, f);

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
				return a.getLibraryName().compareTo(b.getLibraryName());
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

	/**
	 * Prints the present state of the model to a new configuration file.
	 *
	 * @param f - the full path and file name for the new configuration file.
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
		FileEntity file = new FileEntity(simModel, f);
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
			if (ent instanceof ObjectType || ent.getObjectType() == null)
				continue;
			if (ent instanceof EntityLabel) {
				EntityLabel label = (EntityLabel) ent;
				if (!label.getShowInput() && label.isDefault())
					continue;
				if (label.getParent() != null && label.getPrototype() != null
					&& label.getParent().getPrototype() == label.getPrototype().getParent())
					continue;
			}
			newEntities.add(ent);
		}
		Collections.sort(newEntities, uiEntitySortOrder);

		// Add a blank line before the first object definition
		if (!newEntities.isEmpty())
			file.format("%n");

		// Print the Define statements for the entities
		saveDefinitions(newEntities, file);

		// 3) WRITE THE INPUTS

		// Prepare a sorted list of all the entities that were edited
		ArrayList<Entity> entityList = new ArrayList<>();
		for (Entity ent : simModel.getClonesOfIterator(Entity.class)) {
			if (!ent.isEdited() || !ent.isRegistered())
				continue;
			if (ent instanceof ObjectType || ent.getObjectType() == null)
				continue;
			if (ent instanceof EntityLabel && !((EntityLabel) ent).getShowInput()
					&& ((EntityLabel) ent).isDefault())
				continue;
			entityList.add(ent);
		}
		Collections.sort(entityList, uiEntitySortOrder);

		// Save the inputs for each entity
		saveInputs(entityList, file);

		// Close the new configuration file
		file.flush();
		file.close();

		simModel.setSessionEdited(false);
	}

	/**
	 * Prints the Define statements for the specified lists of entities.
	 * @param newEntities - entities to be defined
	 * @param file - file to which the definitions are to be printed
	 */
	public static void saveDefinitions(ArrayList<Entity> newEntities, FileEntity file) {

		// Loop through the entities
		Class<? extends Entity> entClass = null;
		int level = 0;
		Entity proto = null;
		for (Entity ent : newEntities) {
			if (ent.isGenerated())
				continue;

			// Is the class different from the last one
			if (ent.getClass() != entClass || ent.getPrototype() != proto) {

				// Close the previous Define statement
				if (entClass != null) {
					file.format("}%n");
				}

				// Add a blank line between dependency levels
				if (ent.getDependenceLevel() != level) {
					level = ent.getDependenceLevel();
					file.format("%n");
				}

				// Start the new Define statement
				entClass = ent.getClass();
				proto = ent.getPrototype();
				String objName = ent.getObjectType().getName();
				if (proto != null)
					objName = proto.getName();
				file.format("Define %s {", objName);
			}

			// Print the entity name to the Define statement
			file.format(" %s ", ent);
		}

		// Close the define statement
		if (!newEntities.isEmpty())
			file.format("}%n");
	}

	/**
	 * Prints the input statements for the specified lists of entities.
	 * @param entityList - entities whose inputs are to be printed
	 * @param file - file to which the inputs are to be printed
	 */
	public static void saveInputs(ArrayList<Entity> entityList, FileEntity file) {

		// Write a stub definition for the Attributes and Custom Outputs for each entity
		file.format("%n");
		for (Entity ent : entityList) {
			if (!ent.isRegistered())
				continue;
			writeStubOutputDefs(file, ent);
		}

		// Loop through the early keywords
		for (int i = 0; i < EARLY_KEYWORDS.length; i++) {

			// Loop through the entities
			boolean blankLinePrinted = false;
			for (Entity ent : entityList) {
				if (!ent.isRegistered())
					continue;

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

		// Non-graphics inputs for non-graphic entities
		Class<? extends Entity> entClass = null;
		Entity lastEnt = null;
		for (Entity ent : entityList) {
			if (!ent.isRegistered() || isGraphicsEntity(ent))
				continue;

			for (Input<?> in : ent.getEditableInputs()) {
				if (in.isSynonym() || !in.isEdited() || isEarlyInput(in) || isGraphicsInput(in))
					continue;

				// Print a header if the entity class is new
				if (ent.getClass() != entClass) {
					entClass = ent.getClass();
					if (entClass != Simulation.class) {
						file.format("%n");
						file.format("# *** %s ***%n", ent.getObjectType());
					}
				}
				if (ent != lastEnt) {
					lastEnt = ent;
					file.format("%n");
				}

				writeInputOnFile_ForEntity(file, ent, in);
			}
		}

		// Graphics inputs for non-graphic entities
		lastEnt = null;
		for (Entity ent : entityList) {
			if (!ent.isRegistered() || isGraphicsEntity(ent))
				continue;

			for (Input<?> in : ent.getEditableInputs()) {
				if (in.isSynonym() || !in.isEdited() || isEarlyInput(in) || !isGraphicsInput(in))
					continue;

				// Print a header
				if (lastEnt == null) {
					file.format("%n");
					file.format("# *** GRAPHICS INPUTS ***%n");
				}
				if (ent != lastEnt) {
					lastEnt = ent;
					file.format("%n");
				}

				writeInputOnFile_ForEntity(file, ent, in);
			}
		}

		// All inputs for graphic entities
		entClass = null;
		lastEnt = null;
		for (Entity ent : entityList) {
			if (!ent.isRegistered() || !isGraphicsEntity(ent))
				continue;

			for (Input<?> in : ent.getEditableInputs()) {
				if (in.isSynonym() || !in.isEdited() || isEarlyInput(in))
					continue;

				// Print a header if the entity class is new
				if (ent.getClass() != entClass) {
					entClass = ent.getClass();
					if (entClass != Simulation.class) {
						file.format("%n");
						file.format("# *** %s ***%n", ent.getObjectType());
					}
				}
				if (ent != lastEnt) {
					lastEnt = ent;
					file.format("%n");
				}

				writeInputOnFile_ForEntity(file, ent, in);
			}
		}
	}

	public static void saveEntity(Entity entity, File f) {

		// List the entities to be saved
		ArrayList<Entity> entityList = new ArrayList<>();
		entityList.add(entity);
		for (Entity ent : entity.getDescendants()) {
			if (ent instanceof EntityLabel && !((EntityLabel) ent).getShowInput()
					&& ((EntityLabel) ent).isDefault())
				continue;
			entityList.add(ent);
		}
		Collections.sort(entityList, uiEntitySortOrder);

		// Save the definitions and inputs
		FileEntity file = new FileEntity(entity.getJaamSimModel(), f);
		saveDefinitions(entityList, file);
		saveInputs(entityList, file);

		file.flush();
		file.close();
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
		String pal = ent.getObjectType().getLibraryName();
		return Arrays.asList(GRAPHICS_PALETTES).contains(pal);
	}

	static void writeInputOnFile_ForEntity(FileEntity file, Entity ent, Input<?> in) {
		try {
			file.format("%s %s { %s }%n", ent.getName(), in.getKeyword(), in.getInputString());
		}
		catch (Exception e) {
			ent.getJaamSimModel().logMessage("Error writing Entity:%s Keyword:%s", ent.getName(), in.getKeyword());
		}
	}

	static void writeStubOutputDefs(FileEntity file, Entity ent) {
		for (Input<?> in : ent.getEditableInputs()) {
			if (!in.isEdited())
				continue;
			String stub = in.getStubDefinition();
			if (stub == null)
				continue;
			file.format("%s %s { %s }%n", ent.getName(), in.getKeyword(), stub);
		}
	}

	/**
	 * Prints the column headings for the custom output report.
	 * @param simModel - model whose headings are to be printed
	 * @param labels - true if scenario and replication labels are to be printed for each run
	 * @param reps - true if the results for each replication are to be printed
	 * @param bool - true if confidence intervals are to be printed
	 * @param outStream - PrintStream to which the report will be printed
	 */
	public static void printRunOutputHeaders(JaamSimModel simModel, boolean labels, boolean reps,
			boolean bool, PrintStream outStream) {
		Simulation simulation = simModel.getSimulation();
		StringBuilder sb = new StringBuilder();

		// Scenario and replication columns
		if (labels) {
			sb.append("Scenario").append("\t");
			if (reps)
				sb.append("Replication").append("\t");
		}

		// Run parameter columns
		for (String str : simulation.getRunParameterHeaders()) {
			sb.append(str).append("\t");
		}

		// Write the header line for the expressions
		boolean first = true;
		for (String str : simulation.getRunOutputHeaders()) {
			if (first)
				first = false;
			else
				sb.append("\t");
			sb.append(str);
			if (bool)
				sb.append("\t");
		}
		outStream.println(sb.toString());
	}

	/**
	 * Prints the custom output report for the specified scenario.
	 * @param scene - scenario to the reported
	 * @param labels - true if scenario and replication labels are to be printed for each run
	 * @param reps - true if the results for each replication are to be printed
	 * @param bool - true if confidence intervals are to be printed
	 * @param outStream - PrintStream to which the results will be printed
	 */
	public static void printScenarioOutputs(Scenario scene, boolean labels, boolean reps,
			boolean bool, PrintStream outStream) {
		int replications = scene.getRunsCompleted().size();

		// Sort the completed runs by replication number
		ArrayList<SimRun> runList = scene.getRunsCompleted();
		Collections.sort(runList, new Comparator<SimRun>() {
			@Override
			public int compare(SimRun run1, SimRun run2) {
				return Integer.compare(run1.getReplicationNumber(), run2.getReplicationNumber());
			}
		});

		// Print the outputs for each replication
		if (reps) {
			for (SimRun run : runList) {
				StringBuilder sb = new StringBuilder();

				// Scenario and replication numbers
				if (labels) {
					sb.append(scene.getScenarioNumber()).append("\t");
					sb.append(run.getReplicationNumber()).append("\t");
				}

				// Run parameters
				for (String str : run.getRunParameterStrings()) {
					sb.append(str).append("\t");
				}

				// Run outputs
				boolean first = true;
				for (String str : run.getRunOutputStrings()) {
					if (!first) {
						sb.append("\t");
					}
					first = false;
					sb.append(str);
					if (bool) {
						sb.append("\t");
					}
				}
				outStream.println(sb.toString());
			}
		}

		// No need to print the aggregate averages if there is just one replication
		if (replications <= 1 && reps)
			return;

		// Print an error message for any runs that failed
		if (!reps) {
			for (SimRun run : runList) {
				if (!run.isError())
					continue;
				String msg = run.getErrorMessage();
				outStream.format("%s\tError in replication %s - %s%n",
						run.getScenario().getScenarioNumber(), run.getReplicationNumber(), msg);
			}
		}

		// Scenario and replication columns
		StringBuilder sb = new StringBuilder();
		if (labels) {
			sb.append(scene.getScenarioNumber()).append("\t");
			if (reps)
				sb.append("\t");
		}

		// Run parameters
		for (String str : scene.getParameters()) {
			sb.append(str).append("\t");
		}

		// Mean value and confidence interval for each output
		ArrayList<SampleStatistics> stats = scene.getRunStatistics();
		for (int i = 0; i < stats.size(); i++) {
			if (i > 0)
				sb.append("\t");

			double mean = stats.get(i).getMean();
			// Mean value
			if (!Double.isNaN(mean))
				sb.append(mean);

			// Confidence interval
			double interval95 = stats.get(i).getConfidenceInterval95();
			if (bool) {
				sb.append("\t");
				if (!Double.isNaN(interval95))
					sb.append(interval95);
			}
		}

		outStream.println(sb.toString());
	}


	private static final String OUTPUT_FORMAT = "%s\t%s\t%s\t%s%n";
	private static final String LIST_OUTPUT_FORMAT = "%s\t%s[%s]\t%s\t%s%n";

	/**
	 * Writes the entry in the output report for this entity.
	 * @param file - the file in which the outputs are written
	 * @param simTime - simulation time at which the outputs are evaluated
	 */
	public static void printReport(Entity ent, FileEntity file, double simTime) {
		JaamSimModel simModel = ent.getJaamSimModel();

		// Loop through the outputs
		for (ValueHandle out : ent.getAllOutputs()) {

			// Should this output appear in the report?
			if (!out.isReportable())
				continue;

			// Determine the preferred unit for this output
			Class<? extends Unit> ut = out.getUnitType();
			double factor = ent.getJaamSimModel().getDisplayedUnitFactor(ut);
			String unitString = ent.getJaamSimModel().getDisplayedUnit(ut);
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
				String val = InputAgent.getValueAsString(simModel, out, simTime, "%s", factor, "");
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
	 * @param simModel - simulation model whose report is to be printed
	 * @param simTime - simulation time at which the report is printed
	 * @param reportFile - file in which to print the report
	 */
	public static void printReport(JaamSimModel simModel, double simTime, FileEntity reportFile) {

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
	}

	private static class SubModelComparator implements Comparator<Entity> {
		@Override
		public int compare(Entity ent0, Entity ent1) {
			return Integer.compare(ent0.getSubModelLevel(), ent1.getSubModelLevel());
		}
	}
	public static final Comparator<Entity> subModelSortOrder = new SubModelComparator();

	private static class EntityComparator implements Comparator<Entity> {
		@Override
		public int compare(Entity ent0, Entity ent1) {

			// Place the Simulation entity in the first position
			boolean isSim0 = (ent0.getClass() == Simulation.class);
			boolean isSim1 = (ent1.getClass() == Simulation.class);
			int ret = Boolean.compare(isSim1, isSim0);  // Simulation goes first
			if (ret != 0)
				return ret;

			// First sort by dependence level
			ret = Integer.compare(ent0.getDependenceLevel(), ent1.getDependenceLevel());
			if (ret != 0)
				return ret;

			Class<? extends Entity> class0 = ent0.getClass();
			Class<? extends Entity> class1 = ent1.getClass();
			ObjectType ot0 = ent0.getJaamSimModel().getObjectTypeForClass(class0);
			ObjectType ot1 = ent1.getJaamSimModel().getObjectTypeForClass(class1);
			String pal0 = ot0.getLibraryName();
			String pal1 = ot1.getLibraryName();

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

			// If the classes are the same, then sort alphabetically by prototype name
			if (ent0.isClone() && ent1.isClone()) {
				ret = Input.uiSortOrder.compare(ent0.getPrototype(), ent1.getPrototype());
				if (ret != 0)
					return ret;
			}

			// If the prototypes are the same, then sort alphabetically by entity name
			return Input.uiSortOrder.compare(ent0, ent1);
		}
	}
	public static final Comparator<Entity> uiEntitySortOrder = new EntityComparator();

	/**
	 * Returns a formated string for the specified output.
	 * @param simModel - simulation model
	 * @param out - output
	 * @param simTime - present simulation time
	 * @param floatFmt - format string for numerical values
	 * @param factor - divisor to be applied to numerical values
	 * @param unitString - unit to be appended to numerical values
	 * @return formated string for the output
	 */
	public static String getValueAsString(JaamSimModel simModel, ValueHandle out, double simTime, String floatFmt, double factor, String unitString) {

		if (!unitString.isEmpty())
			unitString = "[" + unitString +"]";

		// Numeric outputs
		if (out.isNumericValue()) {
			double val = out.getValueAsDouble(simTime, Double.NaN);
			return String.format(floatFmt, val/factor) + unitString;
		}

		Class<?> retType = out.getReturnType();
		Object ret = out.getValue(simTime, retType);
		return getOutputString(simModel, ret, floatFmt, factor, unitString);
	}

	public static String getOutputString(JaamSimModel simModel, Object ret, String floatFmt, double factor, String unitString) {
		StringBuilder sb = new StringBuilder();

		if (ret == null)
			return "null";

		// String outputs
		if (ret instanceof String) {
			sb.append("\"").append(ret).append("\"");
			return sb.toString();
		}

		// Entity outputs
		if (ret instanceof Entity || ret instanceof AbstractDirectedEntity) {
			sb.append("[").append(ret).append("]");
			return sb.toString();
		}

		// Floating point number
		if (ret instanceof Double || ret instanceof Float) {
			double val = (double) ret;
			return String.format(floatFmt, val/factor) + unitString;
		}

		// double[] outputs
		if (ret instanceof double[]) {
			double[] val = (double[]) ret;
			sb.append("{");
			for (int i=0; i<val.length; i++) {
				if (i > 0)
					sb.append(COMMA_SEPARATOR);
				String str = String.format(floatFmt, val[i]/factor);
				sb.append(str).append(unitString);
			}
			sb.append("}");
			return sb.toString();
		}

		// double[][] outputs
		if (ret instanceof double[][]) {
			double[][] val = (double[][]) ret;
			sb.append("{");
			for (int i=0; i<val.length; i++) {
				if (i > 0)
					sb.append(COMMA_SEPARATOR);
				sb.append("{");
				for (int j=0; j<val[i].length; j++) {
					if (j > 0)
						sb.append(COMMA_SEPARATOR);
					String str = String.format(floatFmt, val[i][j]/factor);
					sb.append(str).append(unitString);
				}
				sb.append("}");
			}
			sb.append("}");
			return sb.toString();
		}

		// int[] outputs
		if (ret instanceof int[]) {
			int[] val = (int[]) ret;
			sb.append("{");
			for (int i=0; i<val.length; i++) {
				if (i > 0)
					sb.append(COMMA_SEPARATOR);
				String str = String.format("%s", val[i]);
				sb.append(str);
			}
			sb.append("}");
			return sb.toString();
		}

		// String[] outputs
		if (ret instanceof String[]) {
			String[] val = (String[]) ret;
			sb.append("{");
			for (int i = 0; i < val.length; i++) {
				if (i > 0)
					sb.append(COMMA_SEPARATOR);
				sb.append("\"").append(val[i]).append("\"");
			}
			sb.append("}");
			return sb.toString();
		}

		// Vec3d outputs
		if (ret instanceof Vec3d) {
			Vec3d vec = (Vec3d) ret;
			sb.append("{");
			sb.append(vec.x/factor).append(unitString).append(COMMA_SEPARATOR);
			sb.append(vec.y/factor).append(unitString).append(COMMA_SEPARATOR);
			sb.append(vec.z/factor).append(unitString);
			sb.append("}");
			return sb.toString();
		}

		// DoubleVector output
		if (ret instanceof DoubleVector) {
			sb.append("{");
			DoubleVector vec = (DoubleVector) ret;
			for (int i=0; i<vec.size(); i++) {
				String str = String.format(floatFmt, vec.get(i)/factor);
				sb.append(str).append(unitString);
				if (i < vec.size()-1) {
					sb.append(COMMA_SEPARATOR);
				}
			}
			sb.append("}");
			return sb.toString();
		}

		// ArrayList output
		if (ret instanceof ArrayList) {
			sb.append("{");
			ArrayList<?> array = (ArrayList<?>) ret;
			for (int i=0; i<array.size(); i++) {
				if (i > 0)
					sb.append(COMMA_SEPARATOR);
				Object obj = array.get(i);
				sb.append(getOutputString(simModel, obj, floatFmt, factor, unitString));
			}
			sb.append("}");
			return sb.toString();
		}

		// Keyed outputs
		if (ret instanceof LinkedHashMap) {
			sb.append("{");
			LinkedHashMap<?, ?> map = (LinkedHashMap<?, ?>) ret;
			boolean first = true;
			for (Entry<?, ?> mapEntry : map.entrySet()) {
				Object obj = mapEntry.getValue();
				if (obj instanceof Double && ((Double) obj).doubleValue() == 0.0d)
					continue;

				if (first)
					first = false;
				else
					sb.append(COMMA_SEPARATOR);

				Object key = mapEntry.getKey();
				sb.append(getOutputString(simModel, key, floatFmt, factor, unitString));
				sb.append("=");
				sb.append(getOutputString(simModel, obj, floatFmt, factor, unitString));
			}
			sb.append("}");
			return sb.toString();
		}

		// Expression result
		if (ret instanceof ExpResult) {
			ExpResult result = (ExpResult) ret;
			if (result.type == ExpResType.NUMBER) {
				sb.append(String.format(floatFmt, result.value/factor)).append(unitString);
				return sb.toString();
			}
			return result.getOutputString(simModel);
		}

		// All other outputs
		return ret.toString();
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
			inputString = inputString.substring(resString.length());
			try {
				inputString = URLDecoder.decode(inputString, StandardCharsets.UTF_8.name());
			}
			catch (UnsupportedEncodingException e) {}
			return String.format("<res>/%s", inputString);
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

	public static String getResourceFolderName(URI uri) {
		String resString = resRoot.toString();
		String inputString = uri.toString();
		if (!inputString.startsWith(resString))
			return null;
		return inputString.substring(resString.length());
	}

	public static ArrayList<FileInput> getExamplesFileInputs(Entity ent) {
		ArrayList<FileInput> ret = new ArrayList<>();
		for (Input<?> in : ent.getEditableInputs()) {

			// Is the input a FileInput whose value has been set?
			if (!(in instanceof FileInput))
				continue;
			FileInput fileIn = (FileInput) in;
			URI uri = fileIn.getValue();
			if (uri == null)
				continue;

			// Is the file located in the 'examples' folder?
			String folder = getResourceFolderName(uri);
			if (folder != null && folder.startsWith("examples")) {
				ret.add(fileIn);
			}
		}
		return ret;
	}

	/**
	 * Loads the default configuration file.
	 */
	public static void loadDefault(JaamSimModel simModel) {
		simModel.setRecordEdits(true);
		// Read the default configuration file
		InputAgent.readResource(simModel, "<res>/inputs/default.cfg");

		// A RecordEdits marker in the default configuration must be ignored
		simModel.setRecordEditsFound(false);

		// Set the model state to unedited
		simModel.setSessionEdited(false);
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
		return formatInput(keyword, str, null);
	}

	public static KeywordIndex formatInput(String keyword, String str, ParseContext pc) {
		ArrayList<String> tokens = new ArrayList<>();
		Parser.tokenize(tokens, str, true);
		return new KeywordIndex(keyword, tokens, pc);
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
	public static URI getFileURI(JaamSimModel sm, URI context, String filePath, String jailPrefix) throws URISyntaxException {

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
			sm.logMessage("Failed jail test: %s%njail: %s%ncontext: %s%n",
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
