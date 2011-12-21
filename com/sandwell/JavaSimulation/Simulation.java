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
package com.sandwell.JavaSimulation;

import java.io.File;
import java.io.FilenameFilter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;

import com.sandwell.JavaSimulation3D.Clock;
import com.sandwell.JavaSimulation3D.ExceptionBox;
import com.sandwell.JavaSimulation3D.InputAgent;
import com.sandwell.JavaSimulation3D.Region;

/**
 * Class Simulation - Sandwell Discrete Event Simulation
 * <p>
 * Class structure defining essential simulation objects.  Eventmanager is
 * instantiated to manage events generated in the simulation.  Function prototypes
 * are defined which any simulation must define in order to run.
 * <p>
 * Global information available to the simulation
 * <ul>
 *  <li>version of the Java Simulation Modeling Language</li>
 *  <li>licensee of the Java Simulation Modeling Language</li>
 *  <li>copyright notice for the simulation</li>
 *  <li>issue copy number for simulation Licensee</li>
 *  <li></li>
 *  <li>name of the simulation model. example: Marine Transportation Model</li>
 *  <li>version of the model (modelName)</li>
 *  <li>Licensed User of the Model</li>
 *  <li>copyright notice for the model</li>
 *	<li>issue copy number for licensed user (or "unlimited")</li>
 *	<li>additional information to be displayed in the about box</li>
 *  <li></li>
 *	<li>name of a specific simulation run</li>
 *	<li>description of a specific simulation run</li>
 * </ul>
 * <p>
 * Simulations are executed following a distinct path of methods and callbacks
 * from program execution to model running.
 * <p>
 * <table width = "100%" border= "1">
 *   <tr bgcolor= "#999999"><th>Event</th><th>Method</th><th>Callback</th><th>Resulting State</th></tr>
 *   <tr><td>~launch~</td><td>~constructor~</td><td>preConfigureModel()</td><td>UNCONFIGURED</td></tr>
 *   <tr><td>Load</td><td>configure()</td><td>configureModel()<br>postConfigureModel()</td><td>CONFIGURED</td></tr>
 *   <tr><td>Play</td><td>start()</td><td>startModel()</td><td>RUNNING</td></tr>
 *   <tr><td>Pause</td><td>pause()</td><td>PAUSED</td></tr>
 *   <tr><td>Play (resume)</td><td>resume()</td><td>RUNNING</td></tr>
 * </table>
 */
public abstract class Simulation extends Entity {

	private ArrayList<EventManager> definedManagers;

	/** description of a specific simulation run */
	protected final StringInput runDescription;
	/** the amount of time to initialize the simulation */
	protected final DoubleInput initializationTime;
	/** starting date in the simulation */
	protected final StringInput startDate;
	/** starting time of day on the start date in the simulation */
	protected final TimeInput startTimeInput;
	/** display time for time 0 in the simulation */
	protected double startTime;
	/** time the simulation run will stop executing */
	protected double endTime;
	/** duration of the simulation run (not including initialization) */
	protected final DoubleInput runDuration;
	private double lastTimeForTrace = -1.0;

	private final DoubleInput simTimeScaleInput;
	private static double simTimeScale; // Simulation timeslice (long <-> double)
	private double realTimeFactor; // Speed-up factor for real-time simulation
	private Process doEndAtThread;

	/** the current simulation state */
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
	/** model has run, but presently is stopped */
	public static final int SIM_STATE_STOPPED = 5;

	private int eventState; // state variable used to communicate with the eventManagers
	static final int EVENTS_STOPPED = 0;
	static final int EVENTS_RUNNING = 1;
	static final int EVENTS_RUNONE = 2;
	static final int EVENTS_TIMESTEP = 3;
	static final int EVENTS_UNTILTIME = 4;

	/** region Entities are placed in unless otherwise specified */
	protected Region defaultRegion;
	private boolean traceEnabled;
	private FileEntity eventTraceFile;
	private FileEntity eventVerifyFile;
	private ArrayList<EventTraceRecord> eventBuffer;
	private long bufferTime; // Internal sim time buffer has been filled to

	private final BooleanInput exitAtStop;
	private final TimeInput portTimeStep;
	private final TimeInput clockTimeStep;
	private final DoubleInput gravity;     // rate of gravity
	private final DoubleInput traceStartTime;
	private final BooleanInput traceEventsInput;
	private final BooleanInput verifyEventsInput;
	private final BooleanInput printInputReport;

	{
		runDuration = new DoubleInput( "Duration", "Key Inputs", 8760.0 );
		runDuration.setValidRange( 1e-15d, Double.POSITIVE_INFINITY );
		runDuration.setUnits( "h" );
		this.addInput( runDuration, true, "RunDuration" );

		initializationTime = new DoubleInput( "Initialization", "Key Inputs", 0.0 );
		initializationTime.setValidRange( 0.0d, Double.POSITIVE_INFINITY );
		initializationTime.setUnits( "h" );
		this.addInput( initializationTime, true, "InitializationDuration" );

		startDate = new StringInput("StartDate", "Optional", null);
		this.addInput(startDate, true);

		startTimeInput = new TimeInput( "StartTime", "Optional", 0.0 );
		startTimeInput.setValidRange( 0.0d, Double.POSITIVE_INFINITY );
		startTimeInput.setUnits( "h" );
		this.addInput( startTimeInput, true );

		exitAtStop = new BooleanInput( "ExitAtStop", "Optional", true );
		this.addInput( exitAtStop, true );

		clockTimeStep = new TimeInput( "TimeStep", "Optional", 1.0 );
		clockTimeStep.setValidRange( 1e-15d, Double.POSITIVE_INFINITY );
		clockTimeStep.setUnits( "h" );
		this.addInput( clockTimeStep, true );

		portTimeStep = new TimeInput( "PortTimeStep", "Optional", 1.0 );
		portTimeStep.setValidRange( 0.001d, Double.POSITIVE_INFINITY );
		portTimeStep.setUnits( "h" );
		this.addInput( portTimeStep, true );

		addEditableKeyword( "CargoUnits",          "",        		"",               false, "Optional" );
		addEditableKeyword( "FuelUnits",           "",       		"",               false, "Optional" );
		addEditableKeyword( "DistanceUnits",       "",        		"",               false, "Optional" );
		addEditableKeyword( "SpeedUnits",          "",        		"",               false, "Optional" );

		runDescription = new StringInput("Description", "Optional", "");
		this.addInput(runDescription, true);

		simTimeScaleInput = new DoubleInput( "SimulationTimeScale", "Optional", 4000.0d );
		simTimeScaleInput.setValidRange( 1e-15d, Double.POSITIVE_INFINITY );
		this.addInput( simTimeScaleInput, true );

		gravity = new DoubleInput( "Gravity", "Optional", 32.2d );
		gravity.setValidRange( 0.0d, Double.POSITIVE_INFINITY );
		gravity.setUnits( "ft/s2" );
		this.addInput( gravity, true );

		traceStartTime = new DoubleInput( "TraceStartTime", "Optional", 0.0d );
		traceStartTime.setValidRange( 0.0d, Double.POSITIVE_INFINITY );
		traceStartTime.setUnits( "h" );
		this.addInput( traceStartTime, false );

		traceEventsInput = new BooleanInput( "TraceEvents", "Optional", false );
		this.addInput( traceEventsInput, false );

		verifyEventsInput = new BooleanInput( "VerifyEvents", "Optional", false );
		this.addInput( verifyEventsInput, false );

		printInputReport = new BooleanInput( "PrintInputReport", "Optional", false );
		this.addInput( printInputReport, true );
	}

	/**
	 *	Constructor for the Simulation
	 *  Protected makes this a 'singleton' class -- only one instance of it exists.  is instantiated through 'getSimulation()' method.
	 */
	protected Simulation() {
		simState = SIM_STATE_UNCONFIGURED;
		eventState = EVENTS_STOPPED;

		// Initialize global Entity references
		eventManager = new EventManager(null, "DefaultEventManager");
		Entity.setSimulation(this);
		EventManager.setSimulation(this);
		eventManager.start();

		definedManagers = new ArrayList<EventManager>();
		definedManagers.add(eventManager);

		// Create clock
		Clock.setStartDate(2000, 1, 1);

		// Initialize basic model information
		startTime = 0.0;
		endTime = 8760.0;

		traceEnabled = false;
		// Real time execution state
		realTimeFactor = 500.0d;
		doEndAtThread = null;

		simState = SIM_STATE_LOADED;
	}

	/**
	 * Processes the input data corresponding to the specified keyword. If syntaxOnly is true,
	 * checks input syntax only; otherwise, checks input syntax and process the input values.
	 */
	public void readData_ForKeyword(StringVector data, String keyword, boolean syntaxOnly, boolean isCfgInput)
	throws InputErrorException {

		if( "CargoUnits".equalsIgnoreCase( keyword ) ) {
			Input.assertCount(data, 1);
			Util.setCargoUnits(data.get(0));
			return;
		}
		if( "FuelUnits".equalsIgnoreCase( keyword ) ) {
			Input.assertCount(data, 1);
			Util.setFuelUnits(data.get(0));
			return;
		}
		if( "DistanceUnits".equalsIgnoreCase( keyword ) ) {
			InputAgent.logWarning( "The keyword DistanceUnits is no longer required." );
			return;
		}
		if( "SpeedUnits".equalsIgnoreCase( keyword ) ) {
			InputAgent.logWarning( "The keyword SpeedUnits is no longer required." );
			return;
		}
		if ("DEFINEEVENTMANAGER".equalsIgnoreCase(keyword)) {
			for (String name : data) {
				this.defineEventManager(name);
			}

			return;
		}
		super.readData_ForKeyword( data, keyword, syntaxOnly, isCfgInput );
	}

	public void validate() {
		if( startDate.getValue() != null && !Tester.isDate( startDate.getValue() ) ) {
			throw new InputErrorException("The value for Start Date must be a valid date.");
		}
	}

	public void defineEventManager(String name) {
		EventManager evt = new EventManager(eventManager, name);
		definedManagers.add(evt);
		evt.start();
	}

	EventManager getDefinedManager(String name) {
		for (EventManager each : definedManagers) {
			if (each.name.equals(name)) {
				return each;
			}
		}

		return null;
	}

	public void clear() {
		eventManager.initialize();

		this.resetInputs();

		// Create clock
		Clock.setStartDate(2000, 1, 1);

		// Initialize basic model information
		startTime = 0.0;
		endTime = 8760.0;

		traceEnabled = false;
		// Real time execution state
		realTimeFactor = 500.0d;
		doEndAtThread = null;

		simState = SIM_STATE_LOADED;
	}

	/**
	 *	Facilitates the configuration of the model.
	 *		1) calls configureModel() to read and configure the initial state of the model
	 *		2) Performs simulation and use interface settings after configuration
	 *		3) calls postConfigureModel() to allow model to initialize objects
	 *
	 *	This method may be called several times as a result of interaction with the user.
	 */
	public void configure(String configFileName) {
		InputAgent.setConfigFileName(configFileName);
		InputAgent.loadConfigFile(InputAgent.getConfigFileName());

		// store the present state
		simState = SIM_STATE_CONFIGURED;

		// Validate each entity in the model
		for (int i = 0; i < Entity.getAll().size(); i++) {
			try {
				Entity.getAll().get(i).validate();
			}
			catch (InputErrorException e) {
				throw new InputErrorException("Validation error for %s: %s", Entity.getAll().get(i).getName(), e.getMessage());
			}
		}

		System.out.println( "Configuration File Loaded" );
	}

	void setEventState(int state) {
		eventState = state;
	}

	int getEventState() {
		return eventState;
	}

	/**
	 *	Initializes and starts the model
	 *		1) Initializes EventManager to accept events.
	 *		2) calls startModel() to allow the model to add its starting events to EventManager
	 *		3) start EventManager processing events
	 */
	public void start() {
		// call startModel from a process so it can handle events
		eventManager.basicInit();
		if (eventVerifyFile != null) {
			eventVerifyFile.toStart();
			eventBuffer.clear();
			bufferTime = 0;
		}

		this.startExternalProcess("startModel");
		resume();
	}

	public void restart() {
		simState = SIM_STATE_CONFIGURED;
		this.start();
	}

	/**
	 *	Requests the EventManager to stop processing events.
	 */
	public void pause() {
		eventManager.pause();

		// store the present state
		simState = SIM_STATE_PAUSED;
	}

	/**
	 *	Requests the EventManager to stop processing events.
	 */
	public void stop() {
		eventManager.pause();

		// store the present state
		simState = SIM_STATE_STOPPED;
	}

	/**
	 *	Requests the EventManager to resume processing events.
	 */
	public void resume() {
		eventManager.resume();
		// store the present state
		simState = SIM_STATE_RUNNING;
	}

	boolean isTraceEnabled() {
		return traceEnabled;
	}

	public void traceAllEvents(boolean enable) {
		if (enable) {
			verifyAllEvents(false);
			eventTraceFile = new FileEntity(InputAgent.getRunName() + ".evt", FileEntity.FILE_WRITE, false);
		} else if (eventTraceFile != null) {
			eventTraceFile.close();
			eventTraceFile = null;
		}

		traceEnabled = enable;
	}

	public void verifyAllEvents(boolean enable) {
		if (enable) {
			traceAllEvents(false);
			eventBuffer = new ArrayList<EventTraceRecord>();
			bufferTime = 0;
			eventVerifyFile = new FileEntity(InputAgent.getRunName() + ".evt", FileEntity.FILE_READ, false);
		} else if (eventVerifyFile != null) {
			eventVerifyFile.close();
			eventVerifyFile = null;
		}

		traceEnabled = enable;
	}

	private void fillBufferUntil(long internalTime) {
		while (bufferTime <= internalTime) {
			EventTraceRecord temp = new EventTraceRecord(eventVerifyFile);

			// reached end of verify file, don't add an empty record
			if (temp.size() == 0) {
				break;
			}
			if (temp.isDefaultEventManager() && temp.getInternalTime() > bufferTime) {
				bufferTime = temp.getInternalTime();
			}

			//System.out.println("Filling buffer:");
			//for (String line : temp) {
			//	System.out.println(line);
			//}
			eventBuffer.add(temp);
		}
	}

	private void findEventInBuffer(EventTraceRecord record) {
		// Try an optimistic approach first looking for exact matches
		for (EventTraceRecord each : eventBuffer) {
			if (!each.basicCompare(record)) {
				continue;
			}

			for (int i = 1; i < record.size(); i++) {
				if (!record.get(i).equals(each.get(i))) {
					System.out.println("Difference in event stream detected");
					System.out.println("Received:");
					for (String line : record) {
						System.out.println(line);
					}

					System.out.println("Expected:");
					for (String line : each) {
						System.out.println(line);
					}

					System.out.println("Lines:");
					System.out.println("R:" + record.get(i));
					System.out.println("E:" + each.get(i));

					pause();
					new Throwable().printStackTrace();
					break;
				}
			}

			// Found the event, it compared OK, remove from the buffer
			eventBuffer.remove(each);
			//System.out.println("Buffersize:" + eventBuffer.size());
			return;
		}

		System.out.println("No matching event found for:");
		for (String line : record) {
			System.out.println(line);
		}
		for (EventTraceRecord rec : eventBuffer) {
			System.out.println("Buffered Record:");
			for (String line : rec) {
				System.out.println(line);
			}
			System.out.println();
		}
		pause();
	}

	void processTraceData(EventTraceRecord traceRecord) {
		if (eventTraceFile != null) {
			synchronized (eventTraceFile) {
				for (String each : traceRecord) {
					eventTraceFile.putString(each);
					eventTraceFile.newLine();
				}
				eventTraceFile.flush();
			}
		}

		if (eventVerifyFile != null) {
			synchronized (eventVerifyFile) {
				this.fillBufferUntil(traceRecord.getInternalTime());
				this.findEventInBuffer(traceRecord);
			}
		}
	}

	public void earlyInit() {
		super.earlyInit();

		simTimeScale = simTimeScaleInput.getValue();

		if( traceEventsInput.getValue() ) {
			this.traceAllEvents(traceEventsInput.getValue());
		}
		else if( verifyEventsInput.getValue() ) {
			this.verifyAllEvents(verifyEventsInput.getValue());
		}

		if( startDate.getValue() != null ) {
			Clock.getStartingDateFromString( startDate.getValue() );
		}
		this.setStartTime( Clock.calcTimeForYear_Month_Day_Hour( 1, Clock.getStartingMonth(), Clock.getStartingDay(), startTimeInput.getValue() ) );
		this.setEndTime( this.getStartTime() + this.getInitializationTime() + this.getRunDuration() );

		doEndAtThread = null;
	}

	public void startUp() {
		super.startUp();
		this.startProcess("doEndAt", this.getEndTime());
	}

	/**
	 *	Called by Simulation to inform the model to begin simulation networks.  Events should not be
	 *	added to the EventManager before startModel();
	 **/
	public void startModel() {
		// Suppress all tracing of old model state during a restart
		boolean tempTracingEnabled = traceEnabled;
		traceEnabled = false;
		eventManager.initialize();
		traceEnabled = tempTracingEnabled;

		if (simState <= SIM_STATE_UNCONFIGURED)
			throw new ErrorException( "Failed to initialize" );

		// Initialize each entity based on inputs only
		for (int i = 0; i < Entity.getAll().size(); i++) {
			Entity.getAll().get(i).earlyInit();
		}

		if( this.getStartTime() > 0.0 ) {
			scheduleWait( this.getStartTime() );
		}

		// Initialize each entity based on early initialization and start networks
		for (int i = 0; i < Entity.getAll().size(); i++) {
			Entity.getAll().get(i).startUp();
		}
	}

	/**
	 * Called at the end of the run
	 */
	public void doEndAt( double end ) {
		if( (end - getCurrentTime()) > 0.0 ) {
			terminateThread(doEndAtThread);
			doEndAtThread = Process.currentProcess();
			scheduleWait( (end - getCurrentTime()) );
			doEndAtThread = null;

			this.pause();

			for (int i = 0; i < Entity.getAll().size(); i++) {
				Entity.getAll().get(i).doEnd();
			}
			System.out.println( "Made it to do end at" );


			// close warning/error trace file
			InputAgent.closeLogFile();

			if( this.getExitAtStop() ) {
				System.exit( 0 );
			}
			this.pause();
		}
	}

	public static double getSimTimeFactor() {
		return simTimeScale;
	}

	public double getDiscreteTime(double time) {
		double factor = getSimTimeFactor();
		return Math.round(time * factor) / factor;
	}

	/**
	 * Return the time as a number of ticks rounded up
	 */
	public double getDiscreteTimeRoundedUp(double time) {
		if (time == Double.POSITIVE_INFINITY) {
			return time;
		} else {
			double factor = getSimTimeFactor();
			return Math.ceil(time * factor) / factor;
		}
	}

	public double getEventTolerance() {
		return (1.0d / getSimTimeFactor());
	}


	/**
	 * Accessor method to retrieve to simulation Name.
	 * @return String - the name of the current simulation run
	 */
	public String getRunDescription() {
		return runDescription.getValue();
	}

	/**
	 * Returns the end time of the run.
	 * @return double - the time the current run will stop
	 */
	public double getEndTime() {
		return endTime;
	}

	/**
	 * Sets the end time of the run.
	 */
	public void setEndTime( double t ) {
		if( t < 0.0 ) {
			throw new ErrorException( " The End Time cannot be a negative number " );
		}
		else {
			if( t > startTime ) {
				endTime = t;
			}
			else {
				throw new ErrorException( " The End time must be after the start time " );
			}
		}
	}

	/**
	 * Return the run duration for the run (not including intialization)
	 */
	public double getRunDuration() {
		return runDuration.getValue();
	}

	/**
	 * Returns the start time of the run.
	 */
	public double getStartTime() {
		return startTime;
	}

	/**
	 * Sets the start time for the run
	 */
	public void setStartTime( double time ) {
		if( time < 0.0 ) {
			throw new ErrorException( " The start time cannot be a negative number " );
		}
		else {
			startTime = time;
		}
	}

	/**
	 * Return the initialization duration in hours
	 */
	public double getInitializationTime() {
		return initializationTime.getValue();
	}

	/** sets the simulation into real time execution mode */
	public void setRealTimeExecution( boolean useRealTime ) {
		eventManager.setExecuteRealTime(useRealTime);
	}

	/** returns whether the simulation is currently executing in real time execution mode */
	public boolean getRealTimeExecution() {
		return eventManager.getExecuteRealtime();
	}

	/** assigns the speedup factor for real time execution mode */
	public void setRealTimeFactor( double newRealTimeFactor ) {
		realTimeFactor = newRealTimeFactor;
		// Called to ensure the time datum is recalculated when the realtimefactor
		// changes
		eventManager.setExecuteRealTime(eventManager.getExecuteRealtime());
	}

	/** retrieves the current value for speedup factor for real time execution mode */
	public double getRealTimeFactor() {
		return realTimeFactor;
	}

	/** returns the current state of the simulation. One of <br>
	 *   SIM_STATE_LOADED - indicates the simulation environment is loaded
	 *   SIM_STATE_UNCONFIGURED - indicates the model is ready to be configured
	 *   SIM_STATE_CONFIGURED - indicates the model is configured, read to execute
	 *   SIM_STATE_RUNNING - indicates the model is currently executing events
	 *   SIM_STATE_PAUSED - indicates the model is currently not executing events
	 *   @return int - current simulation state
	 */
	public int getSimulationState() {
		return simState;
	}

	/**
	 * Accessor to return the default region.
	 * @return Region - the default region for the model
	 */
	public Region getDefaultRegion() {
		return defaultRegion;
	}

	/**
	 * Returns an Entity with the given identifier in the objectMap (region/name for BTM and MTM)
	 * @param key, the name of the object.  Format <region>/<name>
	 * @return the Entity specified
	 */
	public Entity getEntityWithName( String key ) {
		Entity anObject = getNamedEntity(key);

		// if the object is not in the namedEntityHashMap, check the Entity List
		if ( anObject == null ) {
			String regionName = null;
			String entName = null;

			//check if region is part of name
			if( key.indexOf( "/" ) > -1 ) {
				String[] itemArray = key.split( "/" );
				regionName = itemArray[0];
				entName = itemArray[1];
			} else {
				entName = key;
			}

			for (Entity thisEnt : Entity.getAll()) {
				if( thisEnt.getName().equalsIgnoreCase( entName ) ){
					if ((regionName == null) || (thisEnt.getCurrentRegion().getName().equalsIgnoreCase(regionName))) {
						anObject = thisEnt;
					}
				}
			}
		}

		return anObject;
	}

	public void updateTime(double simTime) {}
	public void setProgress(int percentage) {}
	public void setProgressText(String text) {}

	public <T extends Entity> ArrayList<? extends T> getClonesOf(Class<T> proto) {
		ArrayList<T> cloneList = new ArrayList<T>();

		for (Entity each : Entity.getAll()) {
			if (proto.isAssignableFrom(each.getClass())) {
				cloneList.add(proto.cast(each));
			}
		}

		return cloneList;
	}

	public static <T extends Entity> ArrayList<T> getInstancesOf(Class<T> proto) {
		ArrayList<T> instanceList = new ArrayList<T>();

		for (Entity each : Entity.getAll()) {
			if (proto == each.getClass()) {
				instanceList.add(proto.cast(each));
			}
		}

		return instanceList;
	}

	void trace(int indent, Entity ent, String meth, String... text) {
		StringBuilder ind = new StringBuilder("");
		for (int i = 0; i < indent; i++)
			ind.append("   ");

		if( lastTimeForTrace != ent.getCurrentTime() ) {
			System.out.println(String.format(" \nTIME = %.5f", ent.getCurrentTime() ));
			lastTimeForTrace = ent.getCurrentTime();
		}

		System.out.print( String.format( "%s%s %s\n", ind, ent.getName(), meth ) );

		for (String line : text) {
			System.out.print(ind.toString());
			System.out.println(line);
		}

		System.out.flush();
	}

	/**
	 * Returns the first CHM file containing the given string in the file name, or null if not found
	 */
	public static File getCHMFile( String str ) {
		// TODO: need to add a way of picking which chm file to open
		// as-is, picks the first one it finds ie. [0]
		final FilenameFilter helpFilter = new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return name.matches(".+.[cC][hH][mM]");
			}
		};

		File[] chmFiles = null;

		// Test if we are running inside an exe4j packaged executable
		String exe4j = System.getProperty("exe4j.moduleName");
		if (exe4j != null) {
			try {
				chmFiles = new File(exe4j).getParentFile().listFiles(helpFilter);
			}
			catch (Exception e) {}
		}

		// If the exe4j path didn't find anything, try the jar path
		if (chmFiles == null) {
			URI res;
			try {
				res = Simulation.class.getProtectionDomain().getCodeSource().getLocation().toURI();
			}
			catch (URISyntaxException e) {
				return null;
			}
			File temp = new File(res);

			if (!temp.isDirectory())
				temp = temp.getParentFile();

			chmFiles = temp.listFiles(helpFilter);
		}

		if (chmFiles == null || chmFiles.length == 0)
			return null;

		for (File each : chmFiles) {
			if (each.exists() && each.getName().contains(str)) {
				return each;
			}
		}
		return null;
	}

	/**
	 * Spawn the help menu for the CHM file containing the given string in the file name
	 */
	public static void spawnHelp(String str, String suffix) {
		File chmFile = Simulation.getCHMFile( str );
		if (chmFile != null && chmFile.exists()) {
			try {
				Runtime.getRuntime().exec(new String[]{"hh", chmFile.getCanonicalPath() + suffix});
			} catch (Throwable e) {}
		}
	}

	/**
	 * Create an error message box
	 */
	static void makeExceptionBox(Throwable e) {
		// pause the simulation on a fatal exception
		simulation.pause();

		System.err.println("EXCEPTION AT TIME: " + simulation.getCurrentTime());
		new ExceptionBox(e, true);
	}

	public boolean getExitAtStop() {
		return exitAtStop.getValue();
	}

	public double getTimeStep() {
		return clockTimeStep.getValue();
	}

	public double getPortTimeStep() {
		return portTimeStep.getValue();
	}

	public double getGravity() {
		return gravity.getValue();
	}
	public double getTraceStartTime() {
		return traceStartTime.getValue();
	}

	/**
	 * Wait until the trace start time and then turn on the traceflag for all required entities
	 */
	public void doTracing() {

		// Wait until trace start time
		if( this.getTraceStartTime() > 0 ) {
			scheduleWait( this.getTraceStartTime() );
		}

		// Set all required entities trace flags to TRUE
		for (Entity ent : Entity.getAll()) {
			if (ent.testFlag(FLAG_TRACEREQUIRED)) {
				ent.setTraceFlag();
			}
		}
	}

	public boolean getPrintInputReport() {
		return printInputReport.getValue();
	}
}
