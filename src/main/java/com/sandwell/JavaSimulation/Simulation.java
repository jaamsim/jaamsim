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


import com.jaamsim.controllers.RenderManager;
import com.jaamsim.events.ProcessTarget;
import com.jaamsim.input.InputAgent;
import com.jaamsim.input.ValueInput;
import com.jaamsim.ui.EntityPallet;
import com.jaamsim.ui.ExceptionBox;
import com.jaamsim.ui.FrameBox;
import com.jaamsim.units.TimeUnit;
import com.sandwell.JavaSimulation3D.Clock;
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
	@Keyword(description = "The initialization period for the simulation run. The model will " +
	                "run for the initialization period and then clear the statistics " +
	                "and execute for the specified run duration. The total length of the " +
	                "simulation run will be the sum of Initialization and Duration.",
	         example = "Simulation Initialization { 720 h }")
	protected final DoubleInput initializationTime;

	@Keyword(description = "Date at which the simulation run is started (yyyy-mm-dd). This " +
	                "input has no effect on the simulation results unless the seasonality " +
	                "factors vary from month to month.",
	         example = "Simulation StartDate { 2011-01-01 }")
	protected final StringInput startDate;

	@Keyword(description = "Time at which the simulation run is started (hh:mm).",
	         example = "Simulation StartTime { 2160 h }")
	private final ValueInput startTimeInput;

	@Keyword(description = "The duration of the simulation run in which all statistics will be recorded.",
	         example = "Simulation Duration { 8760 h }")
	protected final DoubleInput runDuration;

	@Keyword(description = "The number of discrete time units in one hour.",
	         example = "Simulation SimulationTimeScale { 4500 }")
	private final DoubleInput simTimeScaleInput;

	@Keyword(description = "If the value is TRUE, then the input report file will be printed after loading the " +
	                "configuration file.  The input report can always be generated when needed by selecting " +
	                "\"Print Input Report\" under the File menu.",
	         example = "Simulation PrintInputReport { TRUE }")
	private final BooleanInput printInputReport;

	@Keyword(description = "This is placeholder description text",
	         example = "This is placeholder example text")
	private final BooleanInput traceEventsInput;

	@Keyword(description = "This is placeholder description text",
	         example = "This is placeholder example text")
	private final BooleanInput verifyEventsInput;

	@Keyword(description = "This is placeholder description text",
	         example = "This is placeholder example text")
	private final BooleanInput exitAtStop;

	@Keyword(description = "The real time speed up factor",
	         example = "RunControl RealTimeFactor { 1200 }")
	private final IntegerInput realTimeFactor;
	public static final int DEFAULT_REAL_TIME_FACTOR = 10000;
	public static final int MIN_REAL_TIME_FACTOR = 1;
	public static final int MAX_REAL_TIME_FACTOR= 1000000;
	@Keyword(description = "A Boolean to turn on or off real time in the simulation run",
	         example = "RunControl RealTime { TRUE }")
	private final BooleanInput realTime;

	private Process doEndAtThread;
	protected double startTime;
	protected double endTime;

	private static String modelName = "JaamSim";

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

		startTimeInput = new ValueInput("StartTime", "Key Inputs", 0.0d);
		startTimeInput.setUnitType(TimeUnit.class);
		startTimeInput.setValidRange(0.0d, Double.POSITIVE_INFINITY);
		this.addInput(startTimeInput, true);

		exitAtStop = new BooleanInput( "ExitAtStop", "Key Inputs", false );
		this.addInput( exitAtStop, true );

		simTimeScaleInput = new DoubleInput( "SimulationTimeScale", "Key Inputs", 4000.0d );
		simTimeScaleInput.setValidRange( 1e-15d, Double.POSITIVE_INFINITY );
		this.addInput( simTimeScaleInput, true );

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
		// Create clock
		Clock.setStartDate(2000, 1, 1);

		// Initialize basic model information
		startTime = 0.0;
		endTime = 8760.0;

		// Real time execution state
		doEndAtThread = null;
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
		double startTimeHours = startTimeInput.getValue() / 3600.0d;
		startTime = Clock.calcTimeForYear_Month_Day_Hour(1, Clock.getStartingMonth(), Clock.getStartingDay(), startTimeHours);
		endTime = this.getStartTime() + this.getInitializationTime() + this.getRunDuration();

		doEndAtThread = null;
	}

	private static class EndAtTarget extends ProcessTarget {
		final Simulation sim;

		EndAtTarget(Simulation sim) {
			this.sim = sim;
		}

		@Override
		public String getDescription() {
			return sim.getInputName() + ".doEndAt";
		}

		@Override
		public void process() {
			sim.doEndAt(sim.getEndTime());
		}
	}

	@Override
	public void startUp() {
		super.startUp();
		Process.start(new EndAtTarget(this));
	}

	@Override
	public void updateForInput( Input<?> in ) {
		super.updateForInput( in );

		if(in == realTimeFactor || in == realTime) {
			EventManager.rootManager.setExecuteRealTime(realTime.getValue(), realTimeFactor.getValue());
			GUIFrame.instance().updateForRealTime(this.getRealTimeExecution(), this.getRealTimeFactor());
			return;
		}

		if (in == printInputReport) {
			InputAgent.setPrintInputs(printInputReport.getValue());
			return;
		}
	}

	public void clear() {
		EventManager.rootManager.basicInit();

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

		FrameBox.clear();
		EntityPallet.clear();

		if (RenderManager.isGood()) {
			RenderManager.inst().closeAllWindows();
		}

		// Kill all entities except simulation
		while(Entity.getAll().size() > 1) {
			Entity ent = Entity.getAll().get(Entity.getAll().size()-1);
			ent.kill();
		}

		GUIFrame.instance().updateForSimulationState(SIM_STATE_LOADED);
	}

	/**
	 *	Initializes and starts the model
	 *		1) Initializes EventManager to accept events.
	 *		2) calls startModel() to allow the model to add its starting events to EventManager
	 *		3) start EventManager processing events
	 */
	public void start() {
		EventManager.rootManager.basicInit();

		if( traceEventsInput.getValue() ) {
			EventTracer.traceAllEvents(traceEventsInput.getValue());
		}
		else if( verifyEventsInput.getValue() ) {
			EventTracer.verifyAllEvents(verifyEventsInput.getValue());
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

		EventManager.rootManager.scheduleProcess(0, EventManager.PRIO_DEFAULT, this, new StartModelTarget(this));
		Simulation.resume();
	}


	public static final void resume() {
		EventManager.rootManager.resume();
		GUIFrame.instance().updateForSimulationState(SIM_STATE_RUNNING);
	}

	/**
	 *	Requests the EventManager to stop processing events.
	 */
	public static final void pause() {
		EventManager.rootManager.pause();
		GUIFrame.instance().updateForSimulationState(SIM_STATE_PAUSED);
	}

	/**
	 *	Requests the EventManager to stop processing events.
	 */
	public static final void stop() {
		EventManager.rootManager.pause();
		GUIFrame.instance().updateForSimulationState(SIM_STATE_STOPPED);

		// kill all generated objects
		for (int i = 0; i < Entity.getAll().size();) {
			Entity ent = Entity.getAll().get(i);
			if (ent.testFlag(Entity.FLAG_GENERATED))
				ent.kill();
			else
				i++;
		}
	}

	private static class StartUpTarget extends ProcessTarget {
		final Entity ent;

		StartUpTarget(Entity ent) {
			this.ent = ent;
		}

		@Override
		public String getDescription() {
			return ent.getInputName() + ".startUp";
		}

		@Override
		public void process() {
			ent.startUp();
		}
	}

	private static class StartModelTarget extends ProcessTarget {
		final Simulation ent;

		StartModelTarget(Simulation sim) {
			this.ent = sim;
		}

		@Override
		public String getDescription() {
			return ent.getInputName() + ".startModel";
		}

		@Override
		public void process() {
			ent.startModel();
		}
	}

	/**
	 *	Called by Simulation to inform the model to begin simulation networks.  Events should not be
	 *	added to the EventManager before startModel();
	 **/
	public void startModel() {
		for (int i = 0; i < Entity.getAll().size(); i++) {
			Entity.getAll().get(i).earlyInit();
		}

		if( this.getStartTime() > 0.0 ) {
			scheduleWait( this.getStartTime() );
		}

		// Initialize each entity based on early initialization and start networks
		for (int i = 0; i < Entity.getAll().size(); i++) {
			Process.start(new StartUpTarget(Entity.getAll().get(i)));
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

	/** returns whether the simulation is currently executing in real time execution mode */
	public boolean getRealTimeExecution() {
		return realTime.getValue();
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

	public boolean getExitAtStop() {
		if (InputAgent.getBatch())
			return true;

		return exitAtStop.getValue();
	}
}
