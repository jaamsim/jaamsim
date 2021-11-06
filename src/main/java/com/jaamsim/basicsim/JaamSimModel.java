/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2016-2021 JaamSim Software Inc.
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
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicLong;

import com.jaamsim.Graphics.DisplayEntity;
import com.jaamsim.Graphics.EntityLabel;
import com.jaamsim.Samples.SampleExpression;
import com.jaamsim.StringProviders.StringProvExpression;
import com.jaamsim.SubModels.CompoundEntity;
import com.jaamsim.Thresholds.ThresholdUser;
import com.jaamsim.datatypes.IntegerVector;
import com.jaamsim.events.Conditional;
import com.jaamsim.events.EventHandle;
import com.jaamsim.events.EventManager;
import com.jaamsim.events.EventTimeListener;
import com.jaamsim.events.ProcessTarget;
import com.jaamsim.input.ExpError;
import com.jaamsim.input.Input;
import com.jaamsim.input.InputAgent;
import com.jaamsim.input.InputErrorException;
import com.jaamsim.input.KeywordIndex;
import com.jaamsim.input.NamedExpressionListInput;
import com.jaamsim.input.ParseContext;
import com.jaamsim.math.Vec3d;
import com.jaamsim.states.StateEntity;
import com.jaamsim.ui.EventViewer;
import com.jaamsim.ui.LogBox;
import com.jaamsim.units.DimensionlessUnit;
import com.jaamsim.units.DistanceUnit;
import com.jaamsim.units.TimeUnit;
import com.jaamsim.units.Unit;

public class JaamSimModel {
	// Perform debug only entity list validation logic
	private static final boolean VALIDATE_ENT_LIST = false;

	private static final Object createLock = new Object();
	private static JaamSimModel createModel = null;

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
	private FileEntity reportFile;  // file to which the output report will be written

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

	private final HashMap<String, String> stringCache = new HashMap<>();

	private final ArrayList<ObjectType> objectTypes = new ArrayList<>();
	private final HashMap<Class<? extends Entity>, ObjectType> objectTypeMap = new HashMap<>();

	private final SimCalendar calendar = new SimCalendar();
	private long startMillis;  // start time in milliseonds from the epoch
	private boolean calendarUsed;  // records whether the calendar has been used
	private boolean reloadReqd;  // indicates that the simulation must be saved and reloaded

	private int simState;

	/** model was executed, but no configuration performed */
	public static final int SIM_STATE_LOADED = 0;
	/** essential model elements created, no configuration performed */
	public static final int SIM_STATE_UNCONFIGURED = 1;
	/** model has been configured, not started */
	public static final int SIM_STATE_CONFIGURED = 2;
	/** model is presently executing events */
	public static final int SIM_STATE_RUNNING = 3;
	/** model has run, but presently is paused */
	public static final int SIM_STATE_PAUSED = 4;
	/** model is paused but cannot be resumed */
	public static final int SIM_STATE_ENDED = 5;

	public JaamSimModel() {
		this("");
	}

	public JaamSimModel(String name) {
		eventManager = new EventManager("DefaultEventManager");
		simulation = null;
		this.name = name;
		scenarioNumber = 1;
		replicationNumber = 1;
		scenarioIndexList = new IntegerVector();
		scenarioIndexList.add(1);
	}

	public JaamSimModel(JaamSimModel sm) {
		this(sm.name);
		autoLoad();
		simulation = getSimulation();
		setRecordEdits(true);

		configFile = sm.configFile;
		reportDir = sm.reportDir;
		reportFile = sm.reportFile;

		boolean verify = false;  // verify the copied model

		// Create the new entities in the same order as the original model
		for (Entity ent : sm.getClonesOfIterator(Entity.class)) {
			if (ent.isGenerated() || ent.isPreDefined())
				continue;
			if (ent instanceof EntityLabel && !((EntityLabel) ent).getShowInput()
					&& ((EntityLabel) ent).isDefault())
				continue;
			defineEntity(ent.getObjectType().getName(), ent.getName());
		}

		// Prepare a sorted list of registered entities on which to set inputs
		ArrayList<Entity> entityList = new ArrayList<>();
		for (Entity ent : sm.getClonesOfIterator(Entity.class)) {
			if (!ent.isRegistered() || ent instanceof ObjectType)
				continue;
			if (ent instanceof EntityLabel && !((EntityLabel) ent).getShowInput()
					&& ((EntityLabel) ent).isDefault())
				continue;
			entityList.add(ent);
		}
		Collections.sort(entityList, InputAgent.subModelSortOrder);

		// Stub definitions
		for (Entity ent : entityList) {
			if (ent.isGenerated())
				continue;
			NamedExpressionListInput in = (NamedExpressionListInput) ent.getInput("CustomOutputList");
			if (in == null || in.isDefault())
				continue;
			Entity newEnt = getEntity(ent.getName());
			if (newEnt == null)
				throw new ErrorException("New entity not found: %s", ent.getName());
			KeywordIndex kw = InputAgent.formatInput(in.getKeyword(), in.getStubDefinition());
			InputAgent.apply(newEnt, kw);
		}

		ParseContext context = null;
		if (sm.getConfigFile() != null) {
			URI uri = sm.getConfigFile().getParentFile().toURI();
			context = new ParseContext(uri, null);
		}

		// Copy the early inputs to the new entities in the specified sequence of inputs
		for (String key : InputAgent.EARLY_KEYWORDS) {
			for (Entity ent : entityList) {
				Entity newEnt = getEntity(ent.getName());
				if (newEnt == null)
					throw new ErrorException("New entity not found: %s", ent.getName());
				newEnt.copyInput(ent, key, context, false);
			}
		}

		// Copy the normal inputs to the new entities
		for (Entity ent : entityList) {
			Entity newEnt = getEntity(ent.getName());
			if (newEnt == null)
				throw new ErrorException("New entity not found: %s", ent.getName());
			for (Input<?> in : ent.getEditableInputs()) {
				if (in.isSynonym() || InputAgent.isEarlyInput(in))
					continue;
				String key = in.getKeyword();
				newEnt.copyInput(ent, key, context, false);
			}
		}

		// Complete the preparation of the sub-model clones
		postLoad();

		// Verify the copied model by saving its configuration file
		if (verify) {
			try {
				File file = File.createTempFile("JaamSim-", ".cfg");
				InputAgent.printNewConfigurationFileWithName(this, file);
				System.out.println(file.getPath());
			}
			catch (Exception e) {
				System.out.println(e.getMessage());
			}
		}
	}

	public void setName(String str) {
		name = str;
	}

	public final void setTimeListener(EventTimeListener l) {
		eventManager.setTimeListener(l);
	}

	public final void setRunListener(RunListener l) {
		runListener = l;
	}

	public final RunListener getRunListener() {
		return runListener;
	}

	public void setGUIListener(GUIListener l) {
		gui = l;
	}

	public GUIListener getGUIListener() {
		return gui;
	}

	public int getSimState() {
		return simState;
	}

	public void setSimState(int state) {
		simState = state;
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
		eventManager.setTraceListener(null);

		// Reset the GUI inputs maintained by the Simulation entity
		if (getSimulation() != null) {
			getSimulation().clear();
		}

		EntityListNode listNode = entityList.next;
		while(listNode != entityList) {
			Entity curEnt = listNode.ent;
			if (!curEnt.isDead()) {
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

		stringCache.clear();

		// Reset the run number and run indices
		scenarioNumber = 1;
		replicationNumber = 1;

		configFile = null;
		reportDir = null;
		if (reportFile != null) {
			reportFile.close();
			reportFile = null;
		}
		setSessionEdited(false);
		recordEditsFound = false;
		numErrors = 0;
		numWarnings = 0;
		lastTickForTrace = -1L;
	}

	public final String internString(String str) {
		synchronized (stringCache) {
			String ret = stringCache.get(str);
			if (ret == null) {
				stringCache.put(str, str);
				ret = str;
			}
			return ret;
		}
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
		configure(file, true);
	}

	public void configure(File file, boolean bool) throws URISyntaxException {
		configFile = file;
		if (bool)
			openLogFile();
		InputAgent.loadConfigurationFile(this, file);
		name = file.getName();

		// The session is not considered to be edited after loading a configuration file
		setSessionEdited(false);

		// Save and close the input trace file
		if (bool && numWarnings == 0 && numErrors == 0) {
			closeLogFile();

			// Open a fresh log file for the simulation run
			openLogFile();
		}
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

	/**
	 * Performs consistency checks on the model inputs.
	 * @return true if no validation errors were found
	 */
	public boolean validate() {
		for (Entity each : getClonesOfIterator(Entity.class)) {
			try {
				each.validate();
			}
			catch (Throwable t) {
				if (gui != null) {
					gui.handleInputError(t, each);
				}
				else {
					System.out.format("Validation Error - %s: %s%n", each.getName(), t.getMessage());
				}
				return false;
			}
		}
		return true;
	}

	/**
	 * Starts the simulation model on a new thread.
	 */
	public void start() {
		//System.out.format("%s.start%n", this);
		double pauseTime = getSimulation().getPauseTime();
		start(pauseTime);
	}

	public void start(double pauseTime) {
		boolean bool = validate();
		if (!bool)
			return;
		prepareReportDirectory();
		eventManager.clear();

		// Set up any tracing to be performed
		eventManager.setTraceListener(null);
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
		else if (getSimulation().isEventViewerVisible()) {
			eventManager.setTraceListener(EventViewer.getInstance());
		}

		eventManager.setTickLength(getSimulation().getTickLength());
		startRun(pauseTime);
	}

	void initRun() {
		eventManager.scheduleProcessExternal(0, 0, false, new InitModelTarget(this), null);
	}

	/**
	 * Starts a single simulation run.
	 */
	public void startRun(double pauseTime) {
		initRun();
		resume(pauseTime);
	}

	/**
	 * Performs the first stage of initialization for each entity.
	 */
	public void earlyInit() {
		thresholdChangedTarget.users.clear();
		for (Entity each : getClonesOfIterator(Entity.class)) {
			each.earlyInit();
		}
	}

	/**
	 * Performs the second stage of initialization for each entity.
	 */
	public void lateInit() {
		for (Entity each : getClonesOfIterator(Entity.class)) {
			each.lateInit();
		}
	}

	/**
	 * Performs the start-up procedure for each entity.
	 */
	public void startUp() {
		double startTime = getSimulation().getStartTime();
		long startTicks = eventManager.secondsToNearestTick(startTime);
		for (Entity each : getClonesOfIterator(Entity.class)) {
			if (!each.isActive())
				continue;
			EventManager.scheduleTicks(startTicks, 0, true, new StartUpTarget(each), null);
		}
	}

	/**
	 * Performs the shut down procedure for each entity.
	 */
	public void close() {
		for (Entity each : getClonesOfIterator(Entity.class)) {
			each.close();
		}
	}

	public void doPauseCondition() {
		if (getSimulation().isPauseConditionSet())
			EventManager.scheduleUntil(pauseModelTarget, pauseCondition, null);
	}

	private final PauseModelTarget pauseModelTarget = new PauseModelTarget(this);

	private final Conditional pauseCondition = new Conditional() {
		@Override
		public boolean evaluate() {
			double simTime = EventManager.simSeconds();
			return getSimulation().isPauseConditionSatisfied(simTime);
		}
	};

	/**
	 * Reset the statistics for each entity.
	 */
	public void clearStatistics() {
		for (Entity ent : getClonesOfIterator(Entity.class)) {
			if (!ent.isActive())
				continue;
			ent.clearStatistics();
		}

		// Reset state statistics
		for (StateEntity each : getClonesOfIterator(StateEntity.class)) {
			if (!each.isActive())
				continue;
			each.collectInitializationStats();
		}
	}

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
	public void resume(double simTime) {
		eventManager.resume(eventManager.secondsToNearestTick(simTime));
	}

	/**
	 * Executes the end of run method for each entity.
	 */
	public void doEnd() {
		for (Entity each : getClonesOfIterator(Entity.class)) {
			if (!each.isActive())
				continue;
			each.doEnd();
		}
	}

	/**
	 * Sets the simulation time to zero and re-initializes the model.
	 * The start() method can be used to begin a new simulation run.
	 */
	public void reset() {
		eventManager.pause();
		eventManager.clear();
		killGeneratedEntities();

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

		// Close the output reports
		if (reportFile != null) {
			reportFile.close();
			reportFile = null;
		}
	}

	/**
	 * Prepares the model for the next simulation run number.
	 */
	public void endRun() {

		// Execute the end of run method for each entity
		doEnd();

		// Stop the model
		pause();

		// Notify the run manager
		if (runListener != null)
			runListener.runEnded(null);
	}

	/**
	 * Destroys the entities that were generated during the present simulation run.
	 */
	public void killGeneratedEntities() {
		EntityListNode curNode = entityList.next;
		while(curNode != entityList) {
			Entity curEnt = curNode.ent;
			if (!curEnt.isDead() && !curEnt.testFlag(Entity.FLAG_RETAINED)) {
				curEnt.kill();
			}
			curNode = curNode.next;
		}

	}

	/**
	 * Ends a set of simulation runs.
	 */
	public void end() {

		// Close warning/error trace file
		LogBox.logLine("Made it to do end at");
		closeLogFile();

		// Always terminate the run when in batch mode
		if (isBatchRun() || getSimulation().getExitAtStop()) {
			if (gui != null)
				gui.exit(0);
			System.exit(0);
		}

		pause();
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
		return eventManager.ticksToSeconds(eventManager.getTicks());
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
			return strProv.getNextString(simTime);
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
			return sampleExp.getNextSample(simTime);
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
			InputAgent.defineEntityWithUniqueName(this, klass, name, "_", true);
		}
		catch (InputErrorException e) {
			return;
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

	static JaamSimModel getCreateModel() {
		synchronized (createLock) {
			JaamSimModel mod = createModel;
			createModel = null;
			return mod;
		}
	}

	/**
	 * Creates a new entity.
	 * @param proto - class for the entity
	 * @param name - entity local name
	 * @param parent - entity's parent
	 * @param added - true if the entity was defined after the 'RecordEdits' flag
	 * @param gen - true if the entity was created during the execution of the simulation
	 * @param reg - true if the entity is included in the namedEntities HashMap
	 * @param retain - true if the entity is retained when the model is reset between runs
	 * @return new entity
	 */
	public final <T extends Entity> T createInstance(Class<T> proto, String name, Entity parent,
			boolean added, boolean gen, boolean reg, boolean retain) {
		T ent = createInstance(proto);
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
		ent.entityName = name;
		if (reg)
			addNamedEntity(ent);

		// Create any objects associated with this entity and set their inputs
		// (These objects and their inputs are not be marked as 'edited' to avoid having them saved
		// to the input file)
		boolean bool = isRecordEdits();
		setRecordEdits(false);
		ent.postDefine();
		setRecordEdits(bool);

		return ent;
	}

	public final <T extends Entity> T createInstance(Class<T> proto) {
		T ent = null;
		try {
			synchronized (createLock) {
				createModel = this;
				ent = proto.newInstance();
			}
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
			return;
		}

		if (ent.entityName != null) {
			removeNamedEntity(ent);
		}
		ent.entityName = newName;
		addNamedEntity(ent);
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

			e.entityName = null;
			e.setFlag(Entity.FLAG_DEAD);
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
			EventManager.scheduleTicks(0, 2, false, thresholdChangedTarget, thresholdChangedHandle);
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
		int index = name.lastIndexOf('.');
		if (index == -1)
			return name;

		return name.substring(0, index);
	}

	private String getReportDirectory() {
		if (reportDir != null)
			return reportDir.getPath();

		if (configFile != null)
			return configFile.getParentFile().getPath();

		if (gui != null && gui.getDefaultFolder() != null)
			return gui.getDefaultFolder();

		return null;
	}

	/**
	 * Returns the path to the report file with the specified extension for this model.
	 * Returns null if the file path cannot be constructed.
	 * @param ext - file extension, e.g. ".dat"
	 * @return file path
	 */
	public String getReportFileName(String ext) {
		String dir = getReportDirectory();
		if (dir == null)
			return null;
		StringBuilder sb = new StringBuilder();
		sb.append(dir);
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

	public FileEntity getReportFile() {
		if (reportFile == null) {
			String fileName = getReportFileName(".rep");
			if (fileName == null)
				throw new ErrorException("Cannot create the report file");
			File f = new File(fileName);
			if (f.exists() && !f.delete())
				throw new ErrorException("Cannot delete the existing report file %s", f);
			reportFile = new FileEntity(f);
		}
		return reportFile;
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
			logFile = new FileEntity(f);
		}
		catch( Exception e ) {
			InputAgent.logWarning(this, "Could not create log file.%n%s", e.getMessage());
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

	public void logMessage(String msg) {
		if (logFile == null)
			return;

		logFile.write(msg);
		logFile.newLine();
		logFile.flush();
	}

	public void recordError() {
		numErrors++;
	}

	public int getNumErrors() {
		return numErrors;
	}

	public void recordWarning() {
		numWarnings++;
	}

	public int getNumWarnings() {
		return numWarnings;
	}

	public final void trace(int indent, Entity ent, String fmt, Object... args) {
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

	public KeywordIndex formatVec3dInput(String keyword, Vec3d point, Class<? extends Unit> ut) {
		double factor = getDisplayedUnitFactor(ut);
		String unitStr = getDisplayedUnit(ut);
		return InputAgent.formatVec3dInput(keyword, point, factor, unitStr);
	}

	public KeywordIndex formatPointsInputs(String keyword, ArrayList<Vec3d> points, Vec3d offset) {
		double factor = getDisplayedUnitFactor(DistanceUnit.class);
		String unitStr = getDisplayedUnit(DistanceUnit.class);
		return InputAgent.formatPointsInputs(keyword, points, offset, factor, unitStr);
	}

	public void showTemporaryLabels(boolean bool) {
		for (DisplayEntity ent : getClonesOfIterator(DisplayEntity.class)) {
			if (!EntityLabel.canLabel(ent))
				continue;
			EntityLabel.showTemporaryLabel(ent, bool && ent.getShow() && ent.isMovable(), false);
		}
	}

	public void showSubModels(boolean bool) {
		for (CompoundEntity submodel : getClonesOfIterator(CompoundEntity.class)) {
			submodel.showTemporaryComponents(bool);
		}
	}

	@Override
	public String toString() {
		return name;
	}

}
