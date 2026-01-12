/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2016-2025 JaamSim Software Inc.
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
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.prefs.Preferences;

import com.jaamsim.Graphics.DisplayEntity;
import com.jaamsim.Graphics.EntityLabel;
import com.jaamsim.ProbabilityDistributions.RandomStreamUser;
import com.jaamsim.Samples.SampleExpression;
import com.jaamsim.StringProviders.StringProvExpression;
import com.jaamsim.SubModels.SubModel;
import com.jaamsim.Thresholds.ThresholdUser;
import com.jaamsim.datatypes.IntegerVector;
import com.jaamsim.events.EventHandle;
import com.jaamsim.events.EventManager;
import com.jaamsim.events.EventTimeListener;
import com.jaamsim.events.ProcessTarget;
import com.jaamsim.input.ExpError;
import com.jaamsim.input.Input;
import com.jaamsim.input.InputAgent;
import com.jaamsim.input.InputErrorException;
import com.jaamsim.input.KeywordIndex;
import com.jaamsim.input.ParseContext;
import com.jaamsim.rng.MRG1999a;
import com.jaamsim.ui.EventViewer;
import com.jaamsim.units.DimensionlessUnit;
import com.jaamsim.units.TimeUnit;
import com.jaamsim.units.Unit;

public class JaamSimModel implements EventTimeListener {
	// Perform debug only entity list validation logic
	private static final boolean VALIDATE_ENT_LIST = false;

	private final EventManager eventManager;
	private Simulation simulation;
	private String name;
	private int scenarioNumber;    // labels each scenario when multiple scenarios are being made
	private IntegerVector scenarioIndexList;
	private int replicationNumber;
	private RunListener runListener;  // notifies the SimRun that the run has ended
	private GUIListener gui;
	private final AtomicLong entityCount = new AtomicLong(0);

	private final HashMap<String, Entity> namedEntities = new HashMap<>(100);
	private final HashMap<Class<? extends Unit>, Unit> preferredUnit = new HashMap<>();

	// Note, entityList is an empty list node used to identify the end of the list
	// The first real entity is at entityList.next.ent
	private final EntityListNode entityList = new EntityListNode();
	private int numLiveEnts;

	private File configFile;           // present configuration file
	private File reportDir;         // directory for the output reports

	private boolean batchRun;       // true if the run is to be terminated automatically
	private boolean scriptMode;     // TRUE if script mode (command line) is specified
	private boolean sessionEdited;  // TRUE if any inputs have been changed after loading a configuration file
	private boolean recordEditsFound;  // TRUE if the "RecordEdits" marker is found in the configuration file
	private boolean recordEdits;       // TRUE if input changes are to be marked as edited

	private FileEntity logFile;
	private int numErrors = 0;
	private int numWarnings = 0;

	private long lastTickForTrace = -1L;
	private long preDefinedEntityCount = 0L;  // Number of entities after loading autoload.cfg

	private final ArrayList<ObjectType> objectTypes = new ArrayList<>();
	private final HashMap<Class<? extends Entity>, ObjectType> objectTypeMap = new HashMap<>();

	private final SimCalendar calendar = new SimCalendar();
	private long startMillis;  // start time in milliseonds from the epoch
	private boolean calendarUsed;  // records whether the calendar has been used
	private boolean reloadReqd;  // indicates that the simulation must be saved and reloaded

	private final HashMap<String, MRG1999a[]> rngMap = new HashMap<>();

	final AtomicBoolean hasStarted = new AtomicBoolean();
	final AtomicBoolean hasEnded = new AtomicBoolean();
	final AtomicBoolean isConfiguring = new AtomicBoolean();

	public JaamSimModel() {
		this("");
	}

	public JaamSimModel(String name) {
		eventManager = new EventManager("DefaultEventManager");
		eventManager.setTimeListener(this);
		simulation = null;
		this.name = name;
		scenarioNumber = 1;
		replicationNumber = 1;
		scenarioIndexList = new IntegerVector();
		scenarioIndexList.add(1);
	}

	public JaamSimModel(JaamSimModel sm, String name) {
		this(name);
		//System.out.format("%nJaamSimModel constructor%n");
		autoLoad();
		setRecordEdits(true);

		configFile = sm.configFile;
		reportDir = sm.reportDir;

		// Ensure that 'getReportDirectory' works correctly for an Example Model
		if (reportDir == null && configFile == null)
			reportDir = new File(sm.getReportDirectory());

		// Create the new entities in the same order as the original model
		for (Entity ent : sm.getClonesOfIterator(Entity.class)) {
			if (!ent.isRegistered())
				break;
			if (ent.isPreDefined() || getNamedEntity(ent.getName()) != null)
				continue;

			// Generate all the sub-model components when the first one is found
			if (ent.isGenerated() && ent.getParent() instanceof SubModel) {
				SubModel clone = (SubModel) getNamedEntity(ent.getParent().getName());
				if (clone == null)
					continue;
				clone.createComponents();
				continue;
			}

			// Define the new object
			Entity proto = ent.getPrototype();
			if (proto != null)
				proto = getNamedEntity(proto.getName());
			//System.out.format("defineEntity - ent=%s, proto=%s%n", ent, proto);
			InputAgent.defineEntityWithUniqueName(this, ent.getClass(), proto, ent.getName(), "_", true);
		}

		// Prepare a sorted list of registered entities on which to set inputs
		ArrayList<Entity> entityList = new ArrayList<>();
		for (Entity ent : sm.getClonesOfIterator(Entity.class)) {
			if (!ent.isRegistered())
				break;
			if (ent instanceof ObjectType)
				continue;
			entityList.add(ent);
		}
		Collections.sort(entityList, InputAgent.subModelSortOrder);

		// Stub definitions
		for (Entity ent : entityList) {
			if (ent.isGenerated())
				continue;
			for (Input<?> in : ent.getEditableInputs()) {
				String stub = in.getStubDefinition();
				if (stub == null || in.isDef())
					continue;
				Entity newEnt = getNamedEntity(ent.getName());
				if (newEnt == null)
					throw new ErrorException("New entity not found: %s", ent.getName());
				KeywordIndex kw = InputAgent.formatInput(in.getKeyword(), stub);
				InputAgent.apply(newEnt, kw);
			}
		}

		ParseContext context = null;
		if (sm.getConfigFile() != null) {
			URI uri = sm.getConfigFile().getParentFile().toURI();
			context = new ParseContext(uri, null);
		}

		// Copy the early inputs to the new entities in the specified sequence of inputs
		for (String key : InputAgent.EARLY_KEYWORDS) {
			for (Entity ent : entityList) {
				Entity newEnt = getNamedEntity(ent.getName());
				if (newEnt == null)
					throw new ErrorException("New entity not found: %s", ent.getName());
				//System.out.format("Early Keyword - ent=%s, key=%s%n", ent, key);
				newEnt.copyInput(ent, key, context);
			}
		}

		// Copy the normal inputs to the new entities
		for (Entity ent : entityList) {
			Entity newEnt = getNamedEntity(ent.getName());
			if (newEnt == null)
				throw new ErrorException("New entity not found: %s", ent.getName());
			for (Input<?> in : ent.getEditableInputs()) {
				if (in.isSynonym() || InputAgent.isEarlyInput(in))
					continue;
				String key = in.getKeyword();
				//System.out.format("Normal Keyword - ent=%s, key=%s%n", ent, key);
				newEnt.copyInput(ent, key, context);
			}
		}

		// Complete the preparation of the sub-model clones
		postLoad();

		// Verify that the new JaamSimModel is an exact copy
		if (!this.isCopyOf(sm))
			throw new ErrorException("Copied JaamSimModel does not match the original");
	}

	/**
	 * Returns whether this JaamSimModel is a copy of the specified model.
	 * Avoids the complexities of overriding the equals method.
	 * @param sm - JaamSimModel to be compared
	 * @return true if the model is a copy
	 */
	public boolean isCopyOf(JaamSimModel sm) {

		// Loop through the two sets of registered entities in parallel
		// (any non-registered entities appear after all the registered entities)
		ClonesOfIterable<Entity> itr0 = sm.getClonesOfIterator(Entity.class);
		ClonesOfIterable<Entity> itr1 = this.getClonesOfIterator(Entity.class);
		while (itr0.hasNext() || itr1.hasNext()) {
			Entity ent0 = null;
			Entity ent1 = null;
			try {
				ent0 = itr0.hasNext() ? itr0.next() : null;
				ent1 = itr1.hasNext() ? itr1.next() : null;
			}
			catch (Exception e) {}
			if ((ent0 == null || !ent0.isRegistered()) && (ent1 == null || !ent1.isRegistered()))
				break;

			// Verify that the entity list contains the same sequence of objects
			if (ent0 == null || ent1 == null || !ent1.isCopyOf(ent0)) {
				System.out.format("Entity lists do not match: ent0=%s, ent1=%s%n", ent0, ent1);
				return false;
			}
		}
		return true;
	}

	public void setGUIListener(GUIListener l) {
		gui = l;
	}

	public GUIListener getGUIListener() {
		return gui;
	}

	@Override
	public void tickUpdate(long tick) {
		if (gui == null)
			return;
		gui.gui_tickUpdate(tick);
	}

	@Override
	public void timeRunning() {
		if (gui != null)
			gui.gui_timeRunning();
	}

	@Override
	public void handleError(Throwable t) {
		this.recordError();
		this.logMessage("Runtime error in replication %s of scenario %s at time %f s:",
				this.getReplicationNumber(), this.getScenarioNumber(), this.getSimTime());
		this.logMessage("%s", t.getLocalizedMessage());

		// Stack trace for the root cause
		Throwable rootCause = t;
		while (rootCause.getCause() != null && rootCause.getCause() != rootCause) {
			rootCause = rootCause.getCause();
		}
		this.logMessage("Stack trace:");
		this.logStackTrace(rootCause);
		this.logMessage("");
		if (isMultipleRuns()) {
			runListener.handleRuntimeError(this, t);
			return;
		}
		if (gui != null)
			gui.gui_handleError(t);
	}

	public boolean isStarted() {
		return hasStarted.get();
	}

	public boolean isEnded() {
		return hasEnded.get();
	}

	public void setConfiguring(boolean config) {
		isConfiguring.set(config);
	}

	public boolean isConfiguring() {
		return isConfiguring.get();
	}

	public boolean isRealTime() {
		return simulation.isRealTime();
	}

	public String getName() {
		return name;
	}

	/**
	 * Deletes all the objects in the present model and prepares the JaamSimModel to load a new
	 * input file using the autoLoad() and configure() methods.
	 */
	public void clear() {
		eventManager.clear();
		hasStarted.set(false);
		hasEnded.set(false);

		// Reset the GUI inputs maintained by the Simulation entity
		if (getSimulation() != null) {
			getSimulation().clear();
		}

		EntityListNode listNode = entityList.next;
		while(listNode != entityList) {
			Entity curEnt = listNode.ent;
			if (curEnt != null && !curEnt.isDead()) {
				curEnt.kill();
			}
			listNode = listNode.next;
		}

		// Reset calendar
		calendar.setGregorian(false);
		startMillis = 0L;
		calendarUsed = false;
		reloadReqd = false;

		// Clear the 'simulation' property
		simulation = null;

		// close warning/error trace file
		closeLogFile();

		// Reset the run number and run indices
		scenarioNumber = 1;
		replicationNumber = 1;

		configFile = null;
		reportDir = null;

		setSessionEdited(false);
		recordEditsFound = false;
		numErrors = 0;
		numWarnings = 0;
		lastTickForTrace = -1L;
	}

	/**
	 * Pre-loads the simulation model with built-in objects such as Simulation and Units.
	 */
	public void autoLoad() {

		// Load the autoload.cfg file
		setRecordEdits(false);
		InputAgent.readResource(this, "<res>/inputs/autoload.cfg");

		// Save the number of entities created by the autoload.cfg file
		preDefinedEntityCount = getTailEntity().getEntityNumber();
	}

	/**
	 * Loads the specified configuration file to create the objects in the model.
	 * The autoLoad() method must be executed first.
	 * @param file - configuration file
	 * @throws URISyntaxException
	 */
	public void configure(File file) throws URISyntaxException {
		configFile = file;
		name = file.getName();
		openLogFile();

		// Load the input file
		loadFile(file);

		// Perform any actions that are required after loading the input file
		postLoad();

		// Validate the inputs
		for (Entity each : getClonesOfIterator(Entity.class)) {
			if (each.hasClone())
				continue;

			try {
				each.validate();
			}
			catch (Throwable e) {
				recordError();
				this.logMessage("Validation Error - %s: %s%n", each, e.getMessage());
			}
		}

		//  Check for found errors
		if (getNumErrors() > 0)
			throw new InputErrorException("%d input errors and %d warnings found",
					getNumErrors(), getNumWarnings());

		if (getSimulation().getPrintInputReport())
			InputAgent.printInputFileKeywords(this);

		// The session is not considered to be edited after loading a configuration file
		setSessionEdited(false);

		// Save and close the input trace file
		if (numWarnings == 0 && numErrors == 0) {
			closeLogFile();

			// Open a fresh log file for the simulation run
			openLogFile();
		}
	}

	/**
	 * Parses configuration file records from the specified file.
	 * @param file - file containing the input records
	 * @throws URISyntaxException
	 */
	public void loadFile(File file) throws URISyntaxException {
		URI dirURI = file.getParentFile().toURI();
		InputAgent.readStream(this, "", dirURI, file.getName());
	}

	/**
	 * Performs any additional actions that are required for each entity after a new configuration
	 * file has been loaded. Performed prior to validation.
	 */
	public void postLoad() {
		for (Entity each : getClonesOfIterator(Entity.class)) {
			each.postLoad();
		}
	}

	public final boolean start(RunListener l) {
		if (l == null)
			throw new NullPointerException("A runlistener must be provided to start a run");

		runListener = l;

		for (Entity each : getClonesOfIterator(Entity.class)) {
			if (each.hasClone())
				continue;

			try {
				each.validate();
			}
			catch (Throwable t) {
				String msg = String.format("Validation Error - %s: %s%n", each, t.getMessage());
				if (t instanceof ErrorException)
					msg = String.format("Validation Error - %s%n", t.getMessage());
				this.logMessage(msg);
				if (t.getMessage() == null || t.getMessage().equals("null"))
					this.logStackTrace(t);
				if (gui != null) {
					gui.handleInputError(t, each);
				}
				return false;
			}
		}

		prepareReportDirectory();
		killGeneratedEntities();
		eventManager.clear();
		hasStarted.set(false);
		hasEnded.set(false);

		// Set up any tracing to be performed
		try {
			if (getSimulation().traceEvents()) {
				String evtName = configFile.getParentFile() + File.separator + getRunName() + ".evt";
				EventRecorder rec = new EventRecorder(evtName);
				eventManager.setTraceListener(rec);
			}
			else if (getSimulation().verifyEvents()) {
				String evtName = configFile.getParentFile() + File.separator + getRunName() + ".evt";
				EventTracer trc = new EventTracer(evtName);
				eventManager.setTraceListener(trc);
			}
			else if (getSimulation().isEventViewerVisible() && gui != null) {
				eventManager.setTraceListener(EventViewer.getInstance());
			}
		}
		catch (Exception e) {
			if (gui != null) {
				gui.handleInputError(e, getSimulation());
			}
			return false;
		}

		eventManager.setTickLength(getSimulation().getTickLength());
		eventManager.scheduleProcessExternal(0, Entity.PRI_HIGHEST, false, new InitModelTarget(this), null);
		resume();
		return true;
	}

	/**
	 * Performs the shut down procedure for each entity.
	 */
	public void close() {
		for (Entity each : getClonesOfIterator(Entity.class)) {
			each.close();
		}
	}

	private final PauseModelTarget pauseModelTarget = new PauseModelTarget(this);

	/**
	 * Suspends model execution at the present simulation time.
	 */
	public void pause() {
		//System.out.format("%s.pause%n", this);
		eventManager.pause();
	}

	/**
	 * Resumes a paused simulation model.
	 * The model will continue execution until the specified simulation time at which the model
	 * will be paused.
	 * Events scheduled at the next pause time will not be executed until the model is resumed.
	 * @param simTime - next pause time
	 */
	public void resume() {
		eventManager.resumeSeconds(getSimulation().getPauseTime());
	}

	/**
	 * Sets the simulation time to zero and re-initializes the model.
	 * The start() method can be used to begin a new simulation run.
	 */
	public void reset() {
		eventManager.pause();
		eventManager.clear();
		hasStarted.set(false);
		hasEnded.set(false);
		killGeneratedEntities();
		rngMap.clear();

		// Keep the labels and sub-models consistent with the gui
		if (getSimulation().isShowLabels())
			showTemporaryLabels();

		// Perform earlyInit
		for (Entity each : getClonesOfIterator(Entity.class)) {
			// Try/catch is required because some earlyInit methods use simTime which is only
			// available from a process thread, which is not the case when called from endRun
			try {
				each.earlyInit();
			} catch (Exception e) {}
		}

		// Perform lateInit
		for (Entity each : getClonesOfIterator(Entity.class)) {
			// Try/catch is required because some lateInit methods use simTime which is only
			// available from a process thread, which is not the case when called from endRun
			try {
				each.lateInit();
			} catch (Exception e) {}
		}
	}


	void event_init() {
		hasStarted.set(true);
		//System.out.format("%ninit%n");
		Simulation simulation = this.getSimulation();

		// Early Initialization
		this.thresholdChangedTarget.users.clear();
		for (Entity each : this.getClonesOfIterator(Entity.class)) {
			each.earlyInit();
		}

		// Late Initialization
		for (Entity each : this.getClonesOfIterator(Entity.class)) {
			each.lateInit();
		}

		// Schedule a starting event for each active Entity at the startup time
		long startTicks = eventManager.secondsToNearestTick(simulation.getStartTime());
		long initTicks = eventManager.secondsToNearestTick(simulation.getInitializationTime());
		long durationTicks = eventManager.secondsToNearestTick(simulation.getRunDuration());

		for (Entity each : this.getClonesOfIterator(Entity.class)) {
			if (!each.isActive())
				continue;
			EventManager.scheduleTicks(startTicks, Entity.PRI_HIGHEST, true, new StartUpTarget(each), null);
		}

		// Schedule the statistics initialization if one has been set
		if (initTicks > 0)
			EventManager.scheduleTicks(startTicks + initTicks, Entity.PRI_NORMAL, false, new ClearStatisticsTarget(this), null);

		// Schedule the end of the simulation run
		EventManager.scheduleTicks(startTicks + initTicks + durationTicks, Entity.PRI_NORMAL, false, new EndModelTarget(this), null);

		// Start checking the pause condition
		if (simulation.isPauseConditionSet())
			EventManager.scheduleUntil(pauseModelTarget, pauseModelTarget.condition, null);
	}

	void event_pause() {
		Simulation simulation = this.getSimulation();

		// If specified, terminate the simulation run
		if (simulation.getExitAtPauseCondition()) {
			this.event_end();
			return;
		}

		// Pause the simulation run
		pause();

		// When the run is resumed, continue to check the pause condition
		if (simulation.isPauseConditionSet())
			EventManager.scheduleUntil(pauseModelTarget, pauseModelTarget.condition, null);
	}

	/**
	 * Reset the statistics for each entity.
	 */
	void event_clearStatistics() {
		for (Entity ent : getClonesOfIterator(Entity.class)) {
			if (!ent.isActive())
				continue;
			ent.clearStatistics();
		}
	}

	/**
	 * Prepares the model for the next simulation run number.
	 */
	void event_end() {
		hasEnded.set(true);
		pause();

		// Execute the end of run method for each entity
		for (Entity each : getClonesOfIterator(Entity.class)) {
			if (!each.isActive())
				continue;
			each.doEnd();
		}

		runListener.runEnded();
	}

	/**
	 * Destroys the entities that were generated during the present simulation run.
	 */
	private void killGeneratedEntities() {
		EntityListNode listNode = entityList.next;
		while(listNode != entityList) {
			Entity curEnt = listNode.ent;
			if (curEnt != null && !curEnt.isDead() && !curEnt.isRetained()) {
				curEnt.kill();
			}
			listNode = listNode.next;
		}
	}

	/**
	 * Returns whether events are being executed.
	 * @return true if the events are being executed
	 */
	public boolean isRunning() {
		return eventManager.isRunning();
	}

	/**
	 * Returns the present simulation time in seconds.
	 * @return simulation time
	 */
	public double getSimTime() {
		return eventManager.getSeconds();
	}

	public long getSimTicks() {
		return eventManager.getTicks();
	}

	/**
	 * Evaluates the specified expression and returns its value as a string.
	 * Any type of result can be returned by the expression, including an entity or an array.
	 * If it returns a number, it must be dimensionless.
	 * @param expString - expression to be evaluated
	 * @return expression value as a string
	 */
	public String getStringValue(String expString) {
		double simTime = getSimTime();
		try {
			Class<? extends Unit> unitType = DimensionlessUnit.class;
			Entity thisEnt = getSimulation();
			StringProvExpression strProv = new StringProvExpression(expString, thisEnt, unitType);
			return strProv.getNextString(thisEnt, simTime);
		}
		catch (ExpError e) {
			return "Cannot evaluate";
		}
	}

	/**
	 * Evaluates the specified expression and returns its value.
	 * The expression must return a dimensionless number.
	 * All other types of expressions return NaN.
	 * @param expString - expression to be evaluated
	 * @return expression value
	 */
	public double getDoubleValue(String expString) {
		double simTime = getSimTime();
		try {
			Class<? extends Unit> unitType = DimensionlessUnit.class;
			Entity thisEnt = getSimulation();
			SampleExpression sampleExp = new SampleExpression(expString, thisEnt, unitType);
			return sampleExp.getNextSample(thisEnt, simTime);
		}
		catch (ExpError e) {
			return Double.NaN;
		}
	}

	/**
	 * Creates a new entity for the specified type and name.
	 * If the name already used, "_1", "_2", etc. will be appended to the name until an unused
	 * name is found.
	 * @param type - type of entity to be created
	 * @param name - absolute name for the created entity
	 */
	public void defineEntity(String type, String name) {
		try {
			Class<? extends Entity> klass = Input.parseEntityType(this, type);
			InputAgent.defineEntityWithUniqueName(this, klass, null, name, "_", true);
		}
		catch (InputErrorException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Sets the input for the specified entity and keyword to the specified string.
	 * @param entName - name of the entity whose input is to be set
	 * @param keyword - input keyword whose value is to be set
	 * @param arg - input string as it would appear in the Input Editor
	 */
	public void setInput(String entName, String keyword, String arg) {
		setRecordEdits(true);
		Entity ent = getNamedEntity(entName);
		if (ent == null)
			throw new ErrorException("Entity '%s' not found", entName);
		KeywordIndex kw = InputAgent.formatInput(keyword, arg);
		InputAgent.apply(ent, kw);
	}

	/**
	 * Writes the inputs for the simulation model to the specified file.
	 * @param file - file to which the model inputs are to be saved
	 */
	public void save(File file) {
		InputAgent.printNewConfigurationFileWithName(this, file);
		configFile = file;
		name = file.getName();
	}

	public EventManager getEventManager() {
		return eventManager;
	}

	void setSimulation(Simulation sim) {
		simulation = sim;
	}

	public Simulation getSimulation() {
		return simulation;
	}

	public boolean isMultipleRuns() {
		return getSimulation().getNumberOfRuns() > 1;
	}

	public boolean isFirstRun() {
		return isFirstScenario() && replicationNumber == 1;
	}

	public boolean isLastRun() {
		return isLastScenario() && replicationNumber == getSimulation().getNumberOfReplications();
	}

	public boolean isFirstScenario() {
		return scenarioNumber == getSimulation().getStartingScenarioNumber();
	}

	public boolean isLastScenario() {
		return scenarioNumber >= getSimulation().getEndingScenarioNumber();
	}

	/**
	 * Returns the run indices that correspond to a given run number.
	 * @param n - run number.
	 * @param rangeList - maximum value for each index.
	 * @return run indices.
	 */
	public static IntegerVector getRunIndexList(int n, IntegerVector rangeList) {
		if (rangeList.size() == 0) {
			IntegerVector indexList = new IntegerVector(1);
			indexList.add(n);
			return indexList;
		}
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
	 * Returns the input format used to specify a set of scenario indices.
	 * @param indexList - scenario indices.
	 * @return scenario code.
	 */
	public static String getScenarioCode(IntegerVector indexList) {
		StringBuilder sb = new StringBuilder();
		sb.append(indexList.get(0));
		for (int i=1; i<indexList.size(); i++) {
			sb.append("-").append(indexList.get(i));
		}
		return sb.toString();
	}

	public void setScenarioNumber(int n) {
		scenarioNumber = n;
		setScenarioIndexList();
	}

	public void setScenarioIndexList() {
		scenarioIndexList = getRunIndexList(scenarioNumber, getSimulation().getScenarioIndexDefinitionList());
	}

	public int getScenarioNumber() {
		return scenarioNumber;
	}

	public IntegerVector getScenarioIndexList() {
		return scenarioIndexList;
	}

	public String getScenarioCode() {
		return getScenarioCode(scenarioIndexList);
	}

	public void setReplicationNumber(int n) {
		replicationNumber = n;
	}

	public int getReplicationNumber() {
		return replicationNumber;
	}

	public int getRunNumber() {
		int numberOfReplications = getSimulation().getNumberOfReplications();
		return (scenarioNumber - 1) * numberOfReplications + replicationNumber;
	}

	public String getRunHeader() {
		return String.format("##### SCENARIO %s - REPLICATION %s #####",
				getScenarioCode(), replicationNumber);
	}

	final long getNextEntityID() {
		return entityCount.incrementAndGet();
	}

	public final Entity getNamedEntity(String name) {
		if (name.contains(".")) {
			String[] names = name.split("\\.");
			return getEntityFromNames(names);
		}

		synchronized (namedEntities) {
			return namedEntities.get(name);
		}
	}

	public final Entity getEntity(String name) {
		Entity ret = getNamedEntity(name);
		if (ret != null)
			return ret;
		for (Entity ent: getClonesOfIterator(Entity.class)) {
			if (ent.getName().equals(name)) {
				return ent;
			}
		}
		return null;
	}

	// Get an entity from a chain of names, descending into the tree of children
	public final Entity getEntityFromNames(String[] names) {
		Entity currEnt = getSimulation();
		for (String name: names) {
			if (currEnt == null) {
				return null;
			}
			currEnt = currEnt.getChild(name);
		}
		return currEnt;
	}

	public final long getEntitySequence() {
		long seq = (long)numLiveEnts << 32;
		seq += entityCount.get();
		return seq;
	}

	public final Entity idToEntity(long id) {
		synchronized (namedEntities) {

			EntityListNode curNode = entityList.next;
			while(true) {
				if (curNode == entityList) {
					return null;
				}
				Entity curEnt = curNode.ent;
				if (curEnt != null && curEnt.getEntityNumber() == id) {
					return curEnt;
				}
				curNode = curNode.next;
			}
		}
	}

	/**
	 * Creates a new entity.
	 * @param klass - class for the entity
	 * @param proto - prototype for the entity
	 * @param name - entity local name
	 * @param parent - entity's parent
	 * @param added - true if the entity was defined after the 'RecordEdits' flag
	 * @param gen - true if the entity was created during the execution of the simulation
	 * @param reg - true if the entity is included in the namedEntities HashMap
	 * @param retain - true if the entity is retained when the model is reset between runs
	 * @return new entity
	 */
	public final <T extends Entity> T createInstance(Class<T> klass, Entity proto, String name, Entity parent,
			boolean added, boolean gen, boolean reg, boolean retain) {
		T ent = createInstance(klass);
		if (ent == null)
			return null;

		// Set the entity type
		if (added)
			ent.setFlag(Entity.FLAG_ADDED);
		if (gen)
			ent.setFlag(Entity.FLAG_GENERATED);
		if (reg)
			ent.setFlag(Entity.FLAG_REGISTERED);
		if (retain)
			ent.setFlag(Entity.FLAG_RETAINED);

		ent.parent = parent;
		ent.setNameInput(name);
		ent.setPrototype(proto);

		// Create any objects associated with this entity and set their inputs
		// (These objects and their inputs are not be marked as 'edited' to avoid having them saved
		// to the input file)
		boolean bool = isRecordEdits();
		setRecordEdits(false);
		ent.postDefine();
		setRecordEdits(bool);

		return ent;
	}


	private static AtomicReference<JaamSimModel> createModel = new AtomicReference<>();

	static JaamSimModel getCreateModel() {
		return createModel.getAndSet(null);
	}

	private static final Class<?>[] defArgClasses = new Class[0];
	private static final Object[] defArgs = new Object[0];
	public final <T extends Entity> T createInstance(Class<T> klass) {
		T ent = null;
		try {
			while (!createModel.compareAndSet(null, this)) {}
			ent = klass.getConstructor(defArgClasses).newInstance(defArgs);
			addInstance(ent);
		}
		catch (Throwable e) {}

		return ent;
	}

	public final void addNamedEntity(Entity ent) {
		if (ent.parent != null) {
			ent.parent.addChild(ent);
			return;
		}

		synchronized (namedEntities) {
			if (namedEntities.get(ent.entityName) != null)
				throw new ErrorException("Entity name: %s is already in use.", ent.entityName);
			namedEntities.put(ent.entityName, ent);
		}
	}

	public final void removeNamedEntity(Entity ent) {
		if (ent.parent != null) {
			ent.parent.removeChild(ent);
			return;
		}

		synchronized (namedEntities) {
			if (namedEntities.remove(ent.entityName) != ent)
				throw new ErrorException("Named Entities Internal Consistency error");
		}
	}

	/**
	 * Changes the specified entity's name.
	 * @param ent - entity to be renamed
	 * @param newName - new local name for the entity
	 */
	final void renameEntity(Entity ent, String newName) {
		if (!ent.isRegistered()) {
			ent.entityName = newName;
		}
		else {
			if (ent.entityName != null)
				removeNamedEntity(ent);
			ent.entityName = newName;
			addNamedEntity(ent);
		}

		if (gui != null) {
			gui.updateObjectSelector(ent);
		}
	}

	private void validateEntList() {
		if (!VALIDATE_ENT_LIST) {
			return;
		}
		synchronized(namedEntities) {
			// Count the number of live entities and make sure all entity numbers are increasing
			// Also, check that the lastEnt reference is correct
			int numEntities = 0;
			long lastEntNum = -1;
			int numDeadEntities = 0;
			if (entityList.ent != null) {
				assert(false);
				throw new ErrorException("Entity List Validation Error!");
			}

			EntityListNode curNode = entityList.next;
			EntityListNode lastNode = entityList;
			while (curNode != null && curNode != entityList) {
				Entity curEnt = curNode.ent;
				if (!curEnt.isDead()) {
					numEntities++;
				} else {
					numDeadEntities++;
				}
				if (curEnt.getEntityNumber() <= lastEntNum) {
					assert(false);
					throw new ErrorException("Entity List Validation Error!");
				}
				if (curNode.prev != lastNode) {
					assert(false);
					throw new ErrorException("Entity List Validation Error!");
				}
				lastEntNum = curEnt.getEntityNumber();
				lastNode = curNode;
				curNode = curNode.next;
			}
			if (numEntities != numLiveEnts) {
				assert(false);
				throw new ErrorException("Entity List Validation Error!");
			}
			if (this.entityList.prev != lastNode) {
				assert(false);
				throw new ErrorException("Entity List Validation Error!");
			}
			if (numDeadEntities > 0) {
				assert(false);
				throw new ErrorException("Entity List Validation Error!");
			}
			if (curNode == null) {
				assert(false);
				throw new ErrorException("Entity List Validation Error!");
			}

			// Scan the list backwards
			curNode = entityList.prev;
			lastNode = entityList;
			numEntities = 0;
			lastEntNum = Long.MAX_VALUE;
			while (curNode != null && curNode != entityList) {
				Entity curEnt = curNode.ent;
				if (!curEnt.isDead()) {
					numEntities++;
				} else {
					numDeadEntities++;
				}
				if (curEnt.getEntityNumber() >= lastEntNum) {
					assert(false);
					throw new ErrorException("Entity List Validation Error!");
				}
				if (curNode.next != lastNode) {
					assert(false);
					throw new ErrorException("Entity List Validation Error!");
				}
				lastEntNum = curEnt.getEntityNumber();
				lastNode = curNode;
				curNode = curNode.prev;
			}
			if (numEntities != numLiveEnts) {
				assert(false);
				throw new ErrorException("Entity List Validation Error!");
			}
			if (this.entityList.next != lastNode) {
				assert(false);
				throw new ErrorException("Entity List Validation Error!");
			}
			if (numDeadEntities > 0) {
				assert(false);
				throw new ErrorException("Entity List Validation Error!");
			}
			if (curNode == null) {
				assert(false);
				throw new ErrorException("Entity List Validation Error!");
			}
		} // synchronized(namedEntities)
	}

	final void addInstance(Entity e) {
		synchronized(namedEntities) {
			validateEntList();

			numLiveEnts++;

			EntityListNode newNode = new EntityListNode(e);

			EntityListNode oldLast = entityList.prev;
			newNode.prev = oldLast;
			newNode.next = entityList;
			oldLast.next = newNode;
			entityList.prev = newNode;
			validateEntList();
		}
	}

	final void restoreInstance(Entity e) {
		synchronized (namedEntities) {
			validateEntList();
			numLiveEnts++;
			if (e.isRegistered())
				addNamedEntity(e);

			// Scan through the linked list to find the place to insert this entity
			// This is slow, but should only happen due to user actions
			long entNum = e.getEntityNumber();
			EntityListNode curNode = entityList.next;

			while(true) {
				Entity nextEnt = curNode.next.ent;
				if (nextEnt == null || nextEnt.getEntityNumber() > entNum) {
					// End of the list or at the correct location

					// Insert a new node after curNode
					EntityListNode newNode = new EntityListNode(e);

					newNode.next = curNode.next;
					newNode.prev = curNode;
					curNode.next = newNode;
					newNode.next.prev = newNode;
					validateEntList();
					return;
				}

				curNode = curNode.next;
			}
		}
	}

	final void removeInstance(Entity e) {
		synchronized (namedEntities) {
			validateEntList();
			numLiveEnts--;
			if (e.isRegistered())
				removeNamedEntity(e);

			EntityListNode listNode = e.listNode;

			// Break the link to the list
			e.listNode = null;
			listNode.ent = null;

			listNode.next.prev = listNode.prev;
			listNode.prev.next = listNode.next;

			// Note, leaving the nodes next and prev pointers intact so that any outstanding iterators
			// can finish traversing the list
			validateEntList();
		}
	}

	public int getEntityCount() {
		return numLiveEnts;
	}

	/**
	 * Returns an Iterator that loops over the instances of the specified class. It does not
	 * include instances of any sub-classes of the class.
	 * The specified class must be a sub-class of Entity.
	 * @param proto - specified class
	 * @return Iterator for instances of the class
	 */
	public <T extends Entity> InstanceIterable<T> getInstanceIterator(Class<T> proto){
		return new InstanceIterable<>(this, proto);
	}

	/**
	 * Returns an Iterator that loops over the instances of the specified class and its
	 * sub-classes.
	 * The specified class must be a sub-class of Entity.
	 * @param proto - specified class
	 * @return Iterator for instances of the class and its sub-classes
	 */
	public <T extends Entity> ClonesOfIterable<T> getClonesOfIterator(Class<T> proto){
		return new ClonesOfIterable<>(this, proto);
	}

	/**
	 * Returns an iterator that loops over the instances of the specified class and its
	 * sub-classes, but of only those classes that implement the specified interface.
	 * The specified class must be a sub-class of Entity.
	 * @param proto - specified class
	 * @param iface - specified interface
	 * @return Iterator for instances of the class and its sub-classes that implement the specified interface
	 */
	public <T extends Entity> ClonesOfIterableInterface<T> getClonesOfIterator(Class<T> proto, Class<?> iface){
		return new ClonesOfIterableInterface<>(this, proto, iface);
	}

	// Note, these methods should only be called by EntityIterator and some unit tests
	public final Entity getHeadEntity() {
		return entityList.next.ent;
	}
	public final Entity getTailEntity() {
		return entityList.prev.ent;
	}
	public final EntityListNode getEntityList() {
		return entityList;
	}


	public void addObjectType(ObjectType ot) {
		synchronized (objectTypes) {
			objectTypes.add(ot);
			objectTypeMap.put(ot.getJavaClass(), ot);
		}
	}

	public void removeObjectType(ObjectType ot) {
		synchronized (objectTypes) {
			objectTypes.remove(ot);
			objectTypeMap.remove(ot.getJavaClass());
		}
	}

	public ArrayList<ObjectType> getObjectTypes() {
		synchronized (objectTypes) {
			return objectTypes;
		}
	}

	public ObjectType getObjectTypeForClass(Class<? extends Entity> klass) {
		synchronized (objectTypes) {
			return objectTypeMap.get(klass);
		}
	}

	private final EventHandle thresholdChangedHandle = new EventHandle();
	private final ThresholdChangedTarget thresholdChangedTarget = new ThresholdChangedTarget();

	private static class ThresholdChangedTarget extends ProcessTarget {
		public final ArrayList<ThresholdUser> users = new ArrayList<>();

		public ThresholdChangedTarget() {}

		@Override
		public void process() {
			for (int i = 0; i < users.size(); i++) {
				users.get(i).thresholdChanged();
			}
			users.clear();
		}

		@Override
		public String getDescription() {
			return "UpdateAllThresholdUsers";
		}
	}

	public void updateThresholdUsers(ArrayList<ThresholdUser> userList) {
		for (ThresholdUser user : userList) {
			if (!thresholdChangedTarget.users.contains(user))
				thresholdChangedTarget.users.add(user);
		}
		if (!thresholdChangedTarget.users.isEmpty() && !thresholdChangedHandle.isScheduled())
			EventManager.scheduleTicks(0, Entity.PRI_HIGH, false, thresholdChangedTarget, thresholdChangedHandle);
	}

	/**
	 * Returns the present configuration file.
	 * Null is returned if no configuration file has been loaded or saved yet.
	 * @return present configuration file
	 */
	public File getConfigFile() {
		return configFile;
	}

	/**
	 * Returns the name of the simulation run.
	 * For example, if the model name is "case1.cfg", then the run name is "case1".
	 * @return name of the simulation run
	 */
	public String getRunName() {
		String simName = getName();
		int index = simName.lastIndexOf('.');
		if (index == -1)
			return simName;
		else
			return simName.substring(0, index);
	}

	private String getReportDirectory() {
		if (reportDir != null)
			return reportDir.getPath();

		if (configFile != null)
			return configFile.getParentFile().getPath();

		return JaamSimModel.getPreferenceFolder("");
	}

	public static String getPreferenceFolder(String key) {
		Preferences prefs = Preferences.userRoot().node("com.jaamsim.ui.GUIFrame");
		String folder = prefs.get(key, null);
		if (folder != null)
			return folder;

		folder = prefs.get("", null);
		if (folder != null)
			return folder;

		return new File(".").getAbsolutePath();
	}

	public static void setPreferenceFolder(String key, String path) {
		Preferences prefs = Preferences.userRoot().node("com.jaamsim.ui.GUIFrame");
		prefs.put(key, path);
	}

	/**
	 * Returns the path to the report file with the specified extension for this model.
	 * Returns null if the file path cannot be constructed.
	 * @param ext - file extension, e.g. ".dat"
	 * @return file path
	 */
	public String getReportFileName(String ext) {
		StringBuilder sb = new StringBuilder();
		sb.append(getReportDirectory());
		sb.append(File.separator);
		sb.append(getRunName());
		sb.append(ext);
		return sb.toString();
	}

	public void setReportDirectory(File dir) {
		reportDir = dir;
		if (reportDir == null)
			return;
		if (!reportDir.exists() && !reportDir.mkdirs())
			throw new InputErrorException("Was unable to create the Report Directory: %s", reportDir.toString());
	}

	public void prepareReportDirectory() {
		if (reportDir != null) reportDir.mkdirs();
	}

	public void setBatchRun(boolean bool) {
		batchRun = bool;
	}

	public boolean isBatchRun() {
		return batchRun;
	}

	public void setScriptMode(boolean bool) {
		scriptMode = bool;
	}

	public boolean isScriptMode() {
		return scriptMode;
	}

	public void setSessionEdited(boolean bool) {
		sessionEdited = bool;
	}

	public boolean isSessionEdited() {
		return sessionEdited;
	}

	/**
	 * Specifies whether a RecordEdits marker was found in the present configuration file.
	 * @param bool - TRUE if a RecordEdits marker was found.
	 */
	public void setRecordEditsFound(boolean bool) {
		recordEditsFound = bool;
	}

	/**
	 * Indicates whether a RecordEdits marker was found in the present configuration file.
	 * @return - TRUE if a RecordEdits marker was found.
	 */
	public boolean isRecordEditsFound() {
		return recordEditsFound;
	}

	/**
	 * Sets the "RecordEdits" mode for the InputAgent.
	 * @param bool - boolean value for the RecordEdits mode
	 */
	public void setRecordEdits(boolean bool) {
		recordEdits = bool;
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
	public boolean isRecordEdits() {
		return recordEdits;
	}

	public FileEntity getLogFile() {
		return logFile;
	}

	public void openLogFile() {
		String logFileName = getRunName() + ".log";
		URI logURI = null;
		logFile = null;
		try {
			URI confURI = configFile.toURI();
			logURI = confURI.resolve(new URI(null, logFileName, null)); // The new URI here effectively escapes the file name
			File f = new File(logURI.getPath());
			if (f.exists() && !f.delete())
				throw new Exception("Cannot delete an existing log file.");
			logFile = new FileEntity(this, f);
		}
		catch( Exception e ) {
			this.logWarning("Could not create log file.%n%s", e.getMessage());
		}
	}

	public void closeLogFile() {
		if (logFile == null)
			return;

		logFile.close();

		// Delete the log file if no errors or warnings were recorded
		if (numErrors == 0 && numWarnings == 0) {
			logFile.delete();
		}

		logFile = null;
	}

	public void logFileMessage(String msg) {
		if (logFile == null)
			return;

		logFile.write(msg);
		logFile.newLine();
		logFile.flush();
	}

	private void recordError() {
		numErrors++;
	}

	public int getNumErrors() {
		return numErrors;
	}

	private void recordWarning() {
		numWarnings++;
	}

	public int getNumWarnings() {
		return numWarnings;
	}

	/**
	 * Writes an error or warning message to standard error, the Log Viewer, and the Log File.
	 * @param fmt - format for the message
	 * @param args - objects to be printed in the message
	 */
	public final void logMessage(String fmt, Object... args) {
		String msg = String.format(fmt, args);
		Log.logLine(msg);
		this.logFileMessage(msg);
	}

	/**
	 * Writes a warning message to standard error, the Log Viewer, and the Log File.
	 * @param fmt - format string for the warning message
	 * @param args - objects used by the format string
	 */
	public final void logWarning(String fmt, Object... args) {
		this.recordWarning();
		String msg = String.format(fmt, args);
		this.logMessage("***WARNING*** %s%n", msg);
	}

	/**
	 * Writes an error message to standard error, the Log Viewer, and the Log File.
	 * @param fmt - format string for the error message
	 * @param args - objects used by the format string
	 */
	public final void logError(String fmt, Object... args) {
		this.recordError();
		String msg = String.format(fmt, args);
		this.logMessage("*** ERROR *** %s%n", msg);
	}

	/**
	 * Writes a input error message to standard error, the Log Viewer, and the Log File.
	 * @param fmt - format string for the error message
	 * @param args - objects used by the format string
	 */
	public final void logInpError(String fmt, Object... args) {
		this.recordError();
		String msg = String.format(fmt, args);
		this.logMessage("*** INPUT ERROR *** %s%n", msg);
	}

	/**
	 * Writes a stack trace to standard error, the Log Viewer, and the Log File.
	 * @param t - exception to be traced
	 */
	public final void logStackTrace(Throwable t) {
		for (StackTraceElement each : t.getStackTrace()) {
			this.logMessage(each.toString());
		}
	}

	public final void trace(int indent, Entity ent, String fmt, Object... args) {
		if (!EventManager.hasCurrent())
			return;

		// Print a TIME header every time time has advanced
		EventManager evt = EventManager.current();
		long traceTick = evt.getTicks();
		if (lastTickForTrace != traceTick) {
			double unitFactor = this.getDisplayedUnitFactor(TimeUnit.class);
			String unitString = this.getDisplayedUnit(TimeUnit.class);
			System.out.format(" \nTIME = %.6f %s,  TICKS = %d\n",
					evt.ticksToSeconds(traceTick) / unitFactor, unitString,
					traceTick);
			lastTickForTrace = traceTick;
		}

		// Create an indent string to space the lines
		StringBuilder str = new StringBuilder("");
		for (int i = 0; i < indent; i++)
			str.append("   ");

		// Append the Entity name if provided
		if (ent != null)
			str.append(ent.getName()).append(":");

		str.append(String.format(fmt, args));
		System.out.println(str.toString());
		System.out.flush();
	}

	public boolean isPreDefinedEntity(Entity ent) {
		return ent.getEntityNumber() <= preDefinedEntityCount;
	}

	/**
	 * Sets the inputs for the calendar. The simulation can use the usual Gregorian calendar with
	 * leap years or it can use a simplified calendar with a fixed 365 days per year.
	 * @param bool - true for the Gregorian calendar, false for the simple calendar
	 * @param date - calendar date corresponding to zero simulation time
	 */
	public void setCalendar(boolean bool, SimDate date) {
		if (calendarUsed)
			reloadReqd = true;

		calendar.setGregorian(bool);
		startMillis = calendar.getTimeInMillis(date.year, date.month - 1, date.dayOfMonth,
				date.hourOfDay, date.minute, date.second, date.millisecond);
	}

	/**
	 * Returns whether the model must be saved and reloaded before it can be executed.
	 * This can occur when the calendar type (simple vs. Gregorian) or start date has
	 * been changed AFTER one or more calendar date inputs has been converted to simulation
	 * time using the previous calendar inputs.
	 * @return true if the model must be saved and reloaded
	 */
	public boolean isReloadReqd() {
		return reloadReqd;
	}

	/**
	 * Returns the simulation time corresponding to the specified date.
	 * @param year - year
	 * @param month - month (0 - 11)
	 * @param dayOfMonth - day of the month (1 - 31)
	 * @param hourOfDay - hour of the day (0 - 23)
	 * @param minute - minutes (0 - 59)
	 * @param second - seconds (0 - 59)
	 * @param ms - millisecond (0 - 999)
	 * @return time in milliseconds from the epoch
	 */
	public long getCalendarMillis(int year, int month, int dayOfMonth, int hourOfDay, int minute, int second, int ms) {
		calendarUsed = true;
		return calendar.getTimeInMillis(year, month, dayOfMonth, hourOfDay, minute, second, ms);
	}

	/**
	 * Returns the simulation time in seconds that corresponds to the specified time in
	 * milliseconds from the epoch.
	 * @param millis - milliseconds from the epoch
	 * @return simulation time in seconds
	 */
	public double calendarMillisToSimTime(long millis) {
		return (millis - startMillis) / 1000.0d;
	}

	/**
	 * Returns the time in milliseconds from the epoch that corresponds to the specified
	 * simulation time.
	 * @param simTime - simulation time in seconds
	 * @return milliseconds from the epoch
	 */
	public long simTimeToCalendarMillis(double simTime) {
		return Math.round(simTime * 1000.0d) + startMillis;
	}

	/**
	 * Returns the date corresponding to the specified time in milliseconds from the epoch.
	 * @param millis - time in milliseconds from the epoch
	 * @return date for the specified time
	 */
	public synchronized Date getCalendarDate(long millis) {
		synchronized (calendar) {
			calendar.setTimeInMillis(millis);
			return calendar.getTime();
		}
	}

	/**
	 * Returns the calendar date and time for the specified time in milliseconds from the epoch.
	 * @param millis - time in milliseconds from the epoch
	 * @return SimDate for the specified time
	 */
	public SimDate getSimDate(long millis) {
		synchronized (calendar) {
			calendar.setTimeInMillis(millis);
			return calendar.getSimDate();
		}
	}

	/**
	 * Returns the day of week for the specified time in milliseconds from the epoch.
	 * @param millis - time in milliseconds from the epoch
	 * @return day of week (Sunday = 1, Monday = 2, ..., Saturday = 7)
	 */
	public int getDayOfWeek(long millis) {
		synchronized (calendar) {
			if (calendar.isGregorian()) {
				calendar.setTimeInMillis(millis);
				return calendar.get(Calendar.DAY_OF_WEEK);
			}
			calendar.setTimeInMillis(startMillis);
			long simDay = (millis - startMillis)/(1000*60*60*24);
			return (int) ((calendar.get(Calendar.DAY_OF_WEEK) - 1 + simDay) % 7L) + 1;
		}
	}

	public final void setPreferredUnitList(ArrayList<? extends Unit> list) {
		ArrayList<String> utList = Unit.getUnitTypeList(this);

		// Set the preferred units in the list
		for (Unit u : list) {
			Class<? extends Unit> ut = u.getClass();
			this.setPreferredUnit(ut, u);
			utList.remove(ut.getSimpleName());
		}

		// Clear the entries for unit types that were not in the list
		for (String utName : utList) {
			Class<? extends Unit> ut = Input.parseUnitType(this, utName);
			preferredUnit.remove(ut);
		}
	}

	public final void setPreferredUnit(Class<? extends Unit> type, Unit u) {
		if (u.getName().equals(Unit.getSIUnit(type))) {
			preferredUnit.remove(type);
			return;
		}
		preferredUnit.put(type, u);
	}

	public final ArrayList<Unit> getPreferredUnitList() {
		return new ArrayList<>(preferredUnit.values());
	}

	public final <T extends Unit> Unit getPreferredUnit(Class<T> type) {
		return preferredUnit.get(type);
	}

	public final <T extends Unit> String getDisplayedUnit(Class<T> ut) {
		Unit u = this.getPreferredUnit(ut);
		if (u == null)
			return Unit.getSIUnit(ut);
		return u.getName();
	}

	public final <T extends Unit> double getDisplayedUnitFactor(Class<T> ut) {
		Unit u = this.getPreferredUnit(ut);
		if (u == null)
			return 1.0;
		return u.getConversionFactorToSI();
	}

	/**
	 * Returns an array of random number generators to be used by the random distribution functions
	 * provided by the expression system. If the seed input is set to -1, the next available random
	 * stream number is selected.
	 * @param key - identifies the random distribution function caller
	 * @param seed - stream number for the first random generator
	 * @param num - number of random generators required for the distribution function
	 * @return array of random generators
	 */
	public MRG1999a[] getRandomGenerators(String key, int seed, int num) {
		MRG1999a[] ret = rngMap.get(key);
		if (ret == null) {
			ret = new MRG1999a[num];
			int streamNumber = seed;
			if (seed == -1)
				streamNumber = getSmallestAvailableStreamNumber();
			int substreamNumber = getSimulation().getSubstreamNumber();
			for (int i = 0; i < num; i++) {
				ret[i] = new MRG1999a(streamNumber + i, substreamNumber);
			}
			rngMap.put(key, ret);
		}
		if (ret.length != num)
			throw new ErrorException("Incorrect number of random generators");
		return ret;
	}

	/**
	 * Returns the smallest random stream number that has not been used.
	 * @return smallest unused stream number
	 */
	public int getSmallestAvailableStreamNumber() {

		// Construct a list of stream numbers that have been used
		ArrayList<RandomStreamUser> userList = new ArrayList<>();
		for (Entity each : getClonesOfIterator(Entity.class, RandomStreamUser.class)) {
			userList.add((RandomStreamUser) each);
		}
		int[] streamNumbers = new int[userList.size() + rngMap.size()];

		for (int i = 0; i < userList.size(); i++) {
			streamNumbers[i] = userList.get(i).getStreamNumber();
		}

		int k = userList.size();
		for (MRG1999a[] rngArray : rngMap.values()) {
			streamNumbers[k] = rngArray[0].getStreamNumber();
			k++;
		}

		// Sort the stream number list and return the first unused integer
		Arrays.sort(streamNumbers);
		int ret = 1;
		for (int i = 0; i < streamNumbers.length; i++) {
			if (streamNumbers[i] > ret)
				return ret;
			if (streamNumbers[i] == ret)
				ret++;
		}
		return ret;
	}

	/**
	 * Returns a list of objects that use the specified random stream.
	 * @param seed - random stream number
	 * @return users of the random stream
	 */
	public ArrayList<RandomStreamUser> getRandomStreamUsers(int seed) {
		ArrayList<RandomStreamUser> ret = new ArrayList<>();
		for (Entity each : getClonesOfIterator(Entity.class, RandomStreamUser.class)) {
			RandomStreamUser user = (RandomStreamUser) each;
			if (user.getStreamNumber() == seed) {
				ret.add(user);
			}
		}
		return ret;
	}

	public void showTemporaryLabels() {
		for (DisplayEntity ent : getClonesOfIterator(DisplayEntity.class)) {
			if (!EntityLabel.canLabel(ent))
				continue;
			EntityLabel.showTemporaryLabel(ent);
		}
	}

	@Override
	public String toString() {
		return name;
	}

}
