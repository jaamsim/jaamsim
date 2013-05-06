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

import java.util.ArrayList;

import com.jaamsim.controllers.RenderManager;
import com.jaamsim.input.InputAgent;
import com.jaamsim.ui.ExceptionBox;
import com.jaamsim.ui.FrameBox;
import com.sandwell.JavaSimulation3D.Clock;
import com.sandwell.JavaSimulation3D.EntityPallet;
import com.sandwell.JavaSimulation3D.GUIFrame;

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
public class Simulation extends Entity {
	@Keyword(desc = "The initialization period for the simulation run. The model will " +
	                "run for the initialization period and then clear the statistics " +
	                "and execute for the specified run duration. The total length of the " +
	                "simulation run will be the sum of Initialization and Duration.",
	         example = "Simulation Initialization { 720 h }")
	protected final DoubleInput initializationTime;

	@Keyword(desc = "Date at which the simulation run is started (yyyy-mm-dd). This " +
	                "input has no effect on the simulation results unless the seasonality " +
	                "factors vary from month to month.",
	         example = "Simulation StartDate { 2011-01-01 }")
	protected final StringInput startDate;

	@Keyword(desc = "Time at which the simulation run is started (hh:mm).",
	         example = "Simulation StartTime { 2160 h }")
	protected final TimeInput startTimeInput;

	@Keyword(desc = "The duration of the simulation run in which all statistics will be recorded.",
	         example = "Simulation Duration { 8760 h }")
	protected final DoubleInput runDuration;

	@Keyword(desc = "The number of discrete time units in one hour.",
	         example = "Simulation SimulationTimeScale { 4500 }")
	private final DoubleInput simTimeScaleInput;

	@Keyword(desc = "If the value is TRUE, then the input report file will be printed after loading the " +
	                "configuration file.  The input report can always be generated when needed by selecting " +
	                "\"Print Input Report\" under the File menu.",
	         example = "Simulation PrintInputReport { TRUE }")
	private final BooleanInput printInputReport;

	@Keyword(desc = "Timestep for updating port operations, in the form hh:mm or in decimal hours.",
	         example = "Simulation PortTimeStep { 0.25 h }")
	private final TimeInput portTimeStep;

	@Keyword(desc = "The time interval to increment each step by. The model calculates all parameters " +
	                "at every time step, so a higher time step will provide coarser resolution in time " +
	                "for results, but will take less time to complete a simulation run. A time step of " +
	                "15 minutes is recommended for most models.",
	         example = "Simulation TimeStep { 0.25 h }")
	private final TimeInput clockTimeStep;


	@Keyword(desc = "The rate of gravity",
	         example = "This is placeholder example text")
	private final DoubleInput gravity;

	@Keyword(desc = "This is placeholder description text",
	         example = "This is placeholder example text")
	private final DoubleInput traceStartTime;

	@Keyword(desc = "This is placeholder description text",
	         example = "This is placeholder example text")
	private final BooleanInput traceEventsInput;

	@Keyword(desc = "This is placeholder description text",
	         example = "This is placeholder example text")
	private final BooleanInput verifyEventsInput;

	@Keyword(desc = "This is placeholder description text",
	         example = "This is placeholder example text")
	private final BooleanInput exitAtStop;

	@Keyword(desc = "The real time speed up factor",
	         example = "RunControl RealTimeFactor { 1200 }")
	private final IntegerInput realTimeFactor;
	public static final int DEFAULT_REAL_TIME_FACTOR = 10000;
	public static final int MIN_REAL_TIME_FACTOR = 1;
	public static final int MAX_REAL_TIME_FACTOR= 1000000;
	@Keyword(desc = "A Boolean to turn on or off real time in the simulation run",
	         example = "RunControl RealTime { TRUE }")
	private final BooleanInput realTime;

	private Process doEndAtThread;
	protected double startTime;
	protected double endTime;
	private FileEntity eventTraceFile;
	private FileEntity eventVerifyFile;
	private ArrayList<EventTraceRecord> eventBuffer;
	private long bufferTime; // Internal sim time buffer has been filled to

	private static String modelName = "JaamSim";

	/** the current simulation state */
	protected static int simState;
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

	{
		runDuration = new DoubleInput( "Duration", "Key Inputs", 8760.0 );
		runDuration.setValidRange( 1e-15d, Double.POSITIVE_INFINITY );
		runDuration.setUnits( "h" );
		this.addInput( runDuration, true, "RunDuration" );

		initializationTime = new DoubleInput( "Initialization", "Key Inputs", 0.0 );
		initializationTime.setValidRange( 0.0d, Double.POSITIVE_INFINITY );
		initializationTime.setUnits( "h" );
		this.addInput( initializationTime, true, "InitializationDuration" );

		startDate = new StringInput("StartDate", "Key Inputs", null);
		this.addInput(startDate, true);

		startTimeInput = new TimeInput( "StartTime", "Key Inputs", 0.0 );
		startTimeInput.setValidRange( 0.0d, Double.POSITIVE_INFINITY );
		startTimeInput.setUnits( "h" );
		this.addInput( startTimeInput, true );

		exitAtStop = new BooleanInput( "ExitAtStop", "Key Inputs", false );
		this.addInput( exitAtStop, true );

		clockTimeStep = new TimeInput( "TimeStep", "Key Inputs", 1.0 );
		clockTimeStep.setValidRange( 1e-15d, Double.POSITIVE_INFINITY );
		clockTimeStep.setUnits( "h" );
		clockTimeStep.setHidden(true);
		this.addInput( clockTimeStep, true );

		portTimeStep = new TimeInput( "PortTimeStep", "Key Inputs", 1.0 );
		portTimeStep.setValidRange( 0.001d, Double.POSITIVE_INFINITY );
		portTimeStep.setHidden(true);
		portTimeStep.setUnits( "h" );
		this.addInput( portTimeStep, true );

		addEditableKeyword( "CargoUnits",          "kt",        		"",               false, "Key Inputs" );
		addEditableKeyword( "FuelUnits",           "",       		"",               false, "Key Inputs" );
		this.getInput( "FuelUnits" ).setHidden( true );

		simTimeScaleInput = new DoubleInput( "SimulationTimeScale", "Key Inputs", 4000.0d );
		simTimeScaleInput.setValidRange( 1e-15d, Double.POSITIVE_INFINITY );
		this.addInput( simTimeScaleInput, true );

		gravity = new DoubleInput( "Gravity", "Key Inputs", 32.2d );
		gravity.setValidRange( 0.0d, Double.POSITIVE_INFINITY );
		gravity.setUnits( "ft/s2" );
		gravity.setHidden(true);
		this.addInput( gravity, true );

		traceStartTime = new DoubleInput( "TraceStartTime", "Key Inputs", 0.0d );
		traceStartTime.setValidRange( 0.0d, Double.POSITIVE_INFINITY );
		traceStartTime.setUnits( "h" );
		this.addInput( traceStartTime, false );

		traceEventsInput = new BooleanInput( "TraceEvents", "Key Inputs", false );
		this.addInput( traceEventsInput, false );

		verifyEventsInput = new BooleanInput( "VerifyEvents", "Key Inputs", false );
		this.addInput( verifyEventsInput, false );

		printInputReport = new BooleanInput( "PrintInputReport", "Key Inputs", false );
		this.addInput( printInputReport, true );

		realTimeFactor = new IntegerInput("RealTimeFactor", "Key Inputs", DEFAULT_REAL_TIME_FACTOR);
		realTimeFactor.setValidRange(MIN_REAL_TIME_FACTOR, MAX_REAL_TIME_FACTOR);
		this.addInput(realTimeFactor, true);

		realTime = new BooleanInput("RealTime", "Key Inputs", false);
		this.addInput(realTime, true);
	}

	/**
	 *	Constructor for the Simulation
	 *  Protected makes this a 'singleton' class -- only one instance of it exists.  is instantiated through 'getSimulation()' method.
	 */
	public Simulation() {
		Simulation.simState = SIM_STATE_UNCONFIGURED;
		EventManager.setSimulation(this);

		// Create clock
		Clock.setStartDate(2000, 1, 1);

		// Initialize basic model information
		startTime = 0.0;
		endTime = 8760.0;

		// Real time execution state
		doEndAtThread = null;

		Simulation.simState = SIM_STATE_LOADED;
	}

	@Override
	public void validate() {
		super.validate();

		if( startDate.getValue() != null && !Tester.isDate( startDate.getValue() ) ) {
			throw new InputErrorException("The value for Start Date must be a valid date.");
		}
	}

	@Override
	public void earlyInit() {
		super.earlyInit();

		Process.setSimTimeScale(simTimeScaleInput.getValue());

		if( startDate.getValue() != null ) {
			Clock.getStartingDateFromString( startDate.getValue() );
		}
		startTime = Clock.calcTimeForYear_Month_Day_Hour(1, Clock.getStartingMonth(), Clock.getStartingDay(), startTimeInput.getValue());
		endTime = this.getStartTime() + this.getInitializationTime() + this.getRunDuration();

		doEndAtThread = null;
	}

	@Override
	public void startUp() {
		super.startUp();
		this.startProcess("doEndAt", this.getEndTime());
	}

	@Override
	public void updateForInput( Input<?> in ) {
		super.updateForInput( in );

		if(in == realTimeFactor) {

			// Called to ensure the time datum is recalculated when the realtimefactor
			// changes
			setRealTimeExecution(EventManager.rootManager.getExecuteRealtime());

			GUIFrame.instance().updateForRealTime();
		}

		if(in == realTime) {
			setRealTimeExecution(realTime.getValue());
			GUIFrame.instance().updateForRealTime();
		}
	}

	/**
	 * Processes the input data corresponding to the specified keyword. If syntaxOnly is true,
	 * checks input syntax only; otherwise, checks input syntax and process the input values.
	 */
	@Override
	public void readData_ForKeyword(StringVector data, String keyword)
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
		if ("DEFINEEVENTMANAGER".equalsIgnoreCase(keyword)) {
			for (String name : data) {
				EventManager.defineEventManager(name);
			}

			return;
		}
		super.readData_ForKeyword( data, keyword );
	}

	public void clear() {
		EventManager.rootManager.initialize();

		this.resetInputs();

		// Create clock
		Clock.setStartDate(2000, 1, 1);

		// Initialize basic model information
		startTime = 0.0;
		endTime = 8760.0;

		// Real time execution state
		doEndAtThread = null;

		// close warning/error trace file
		InputAgent.closeLogFile();

		ArrayList<FrameBox> boxes = new ArrayList<FrameBox>(FrameBox.getAllFB());
		for (FrameBox each : boxes) {
			each.dispose();
		}

		EntityPallet.clear();

		if (RenderManager.isGood()) {
			RenderManager.inst().closeAllWindows();
		}

		// Kill all entities except simulation
		while(Entity.getAll().size() > 1) {
			Entity ent = Entity.getAll().get(Entity.getAll().size()-1);
			ent.kill();
		}

		Simulation.simState = SIM_STATE_LOADED;
		GUIFrame.instance().updateForSimulationState();
	}

	/**
	 *	Initializes and starts the model
	 *		1) Initializes EventManager to accept events.
	 *		2) calls startModel() to allow the model to add its starting events to EventManager
	 *		3) start EventManager processing events
	 */
	public void start() {
		// call startModel from a process so it can handle events
		EventManager.rootManager.basicInit();
		if (eventVerifyFile != null) {
			eventVerifyFile.toStart();
			eventBuffer.clear();
			bufferTime = 0;
		}

		// Suppress all tracing of old model state during a restart
		EventManager.rootManager.initialize();

		if( traceEventsInput.getValue() ) {
			this.traceAllEvents(traceEventsInput.getValue());
		}
		else if( verifyEventsInput.getValue() ) {
			this.verifyAllEvents(verifyEventsInput.getValue());
		}

		// Validate each entity based on inputs only
		for (int i = 0; i < Entity.getAll().size(); i++) {
			try {
				Entity.getAll().get(i).validate();
			}
			catch (Throwable e) {
				InputAgent.doError(e);
				ExceptionBox.instance().setInputError(Entity.getAll().get(i), e);
				return;
			}
		}

		// Initialize each entity based on inputs only
		for (int i = 0; i < Entity.getAll().size(); i++) {
			Entity.getAll().get(i).earlyInit();
		}

		this.startExternalProcess("startModel");
		Simulation.resume();
	}

	public static final void resume() {
		EventManager.rootManager.resume();
		Simulation.simState = SIM_STATE_RUNNING;
		GUIFrame.instance().updateForSimulationState();
	}

	public void restart() {
		// kill all generated objects
		for (int i = 0; i < Entity.getAll().size();) {
			Entity ent = Entity.getAll().get(i);
			if (ent.testFlag(Entity.FLAG_GENERATED))
				ent.kill();
			else
				i++;
		}

		Simulation.simState = SIM_STATE_CONFIGURED;
		this.start();
	}

	/**
	 *	Requests the EventManager to stop processing events.
	 */
	public static final void pause() {
		EventManager.rootManager.pause();
		Simulation.simState = SIM_STATE_PAUSED;
		GUIFrame.instance().updateForSimulationState();
	}

	/**
	 *	Requests the EventManager to stop processing events.
	 */
	public static final void stop() {
		EventManager.rootManager.pause();
		Simulation.simState = SIM_STATE_STOPPED;
		GUIFrame.instance().updateForSimulationState();
	}

	public void traceAllEvents(boolean enable) {
		if (enable) {
			verifyAllEvents(false);
			eventTraceFile = new FileEntity(InputAgent.getRunName() + ".evt", FileEntity.FILE_WRITE, false);
		} else if (eventTraceFile != null) {
			eventTraceFile.close();
			eventTraceFile = null;
		}

		EventManager.rootManager.traceEvents = enable;
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

		EventManager.rootManager.traceEvents = enable;
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

					Simulation.pause();
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
		Simulation.pause();
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

	/**
	 *	Called by Simulation to inform the model to begin simulation networks.  Events should not be
	 *	added to the EventManager before startModel();
	 **/
	public void startModel() {

		if (Simulation.simState <= SIM_STATE_UNCONFIGURED)
			throw new ErrorException( "Failed to initialize" );

		if( this.getStartTime() > 0.0 ) {
			scheduleWait( this.getStartTime() );
		}

		// Initialize each entity based on early initialization and start networks
		for (int i = 0; i < Entity.getAll().size(); i++) {
			Entity.getAll().get(i).startProcess("startUp");
		}
	}

	/**
	 * Called at the end of the run
	 */
	public void doEndAt( double end ) {
		if( (end - getCurrentTime()) > 0.0 ) {
			Process.terminate(doEndAtThread);
			doEndAtThread = Process.current();
			scheduleWait( (end - getCurrentTime()) );
			doEndAtThread = null;

			Simulation.pause();

			for (int i = 0; i < Entity.getAll().size(); i++) {
				Entity.getAll().get(i).doEnd();
			}

			System.out.println( "Made it to do end at" );

			// close warning/error trace file
			InputAgent.closeLogFile();

			if( this.getExitAtStop() ) {
				GUIFrame.shutdown(0);
			}
			Simulation.pause();
		}
	}

	/**
	 * Return the time as a number of ticks rounded up
	 */
	public double getDiscreteTimeRoundedUp(double time) {
		if (time == Double.POSITIVE_INFINITY) {
			return time;
		} else {
			double factor = Process.getSimTimeFactor();
			return Math.ceil(time * factor) / factor;
		}
	}

	/**
	 * Returns the end time of the run.
	 * @return double - the time the current run will stop
	 */
	public double getEndTime() {
		return endTime;
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
	 * Return the initialization duration in hours
	 */
	public double getInitializationTime() {
		return initializationTime.getValue();
	}

	/** sets the simulation into real time execution mode */
	public void setRealTimeExecution( boolean useRealTime ) {
		EventManager.rootManager.setExecuteRealTime(useRealTime);
	}

	/** returns whether the simulation is currently executing in real time execution mode */
	public boolean getRealTimeExecution() {
		return EventManager.rootManager.getExecuteRealtime();
	}

	/** retrieves the current value for speedup factor for real time execution mode */
	public int getRealTimeFactor() {
		return realTimeFactor.getValue();
	}

	public static void setModelName(String newModelName) {
		modelName = newModelName;
	}

	public static String getModelName() {
		return modelName;
	}

	/** returns the current state of the simulation. One of <br>
	 *   SIM_STATE_LOADED - indicates the simulation environment is loaded
	 *   SIM_STATE_UNCONFIGURED - indicates the model is ready to be configured
	 *   SIM_STATE_CONFIGURED - indicates the model is configured, read to execute
	 *   SIM_STATE_RUNNING - indicates the model is currently executing events
	 *   SIM_STATE_PAUSED - indicates the model is currently not executing events
	 *   @return int - current simulation state
	 */
	public static final int getSimulationState() {
		return Simulation.simState;
	}

	public static final void setSimulationState(int state) {
		Simulation.simState = state;
	}

	public static <T extends Entity> ArrayList<T> getClonesOf(Class<T> proto) {
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

	public boolean getExitAtStop() {
		if (InputAgent.getBatch())
			return true;

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

	public void setSimState(int state) {
		simState = state;
	}
}
