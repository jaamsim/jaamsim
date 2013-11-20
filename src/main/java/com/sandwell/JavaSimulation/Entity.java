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
import java.util.HashMap;

import com.jaamsim.events.ProcessTarget;
import com.jaamsim.events.ReflectionTarget;
import com.jaamsim.input.InputAgent;
import com.jaamsim.input.Output;
import com.jaamsim.input.OutputHandle;

/**
 * Abstract class that encapsulates the methods and data needed to create a
 * simulation object. Encapsulates the basic system objects to achieve discrete
 * event execution.
 */
public class Entity {
	private static long entityCount = 0;
	private static final ArrayList<Entity> allInstances;
	private static final HashMap<String, Entity> namedEntities;

	private String entityName;
	private String entityInputName; // Name input by user
	private final long entityNumber;

	//public static final int FLAG_TRACE = 0x01; // reserved in case we want to treat tracing like the other flags
	public static final int FLAG_TRACEREQUIRED = 0x02;
	public static final int FLAG_TRACESTATE = 0x04;
	public static final int FLAG_LOCKED = 0x08;
	//public static final int FLAG_TRACKEVENTS = 0x10;
	public static final int FLAG_ADDED = 0x20;
	public static final int FLAG_EDITED = 0x40;
	public static final int FLAG_GENERATED = 0x80;
	public static final int FLAG_DEAD = 0x0100;
	private int flags;
	protected boolean traceFlag = false;

	private final ArrayList<Input<?>> editableInputs = new ArrayList<Input<?>>();
	private final HashMap<String, Input<?>> inputMap = new HashMap<String, Input<?>>();

	private final BooleanInput trace;

	@Keyword(description = "A free form string describing the Entity",
	         example = "Ent Description { 'A very useful entity' }")
	private final StringInput desc;

	static {
		allInstances = new ArrayList<Entity>(100);
		namedEntities = new HashMap<String, Entity>(100);
	}

	{
		trace = new BooleanInput("Trace", "Key Inputs", false);
		trace.setHidden(true);
		this.addInput(trace, true);

		desc = new StringInput("Description", "Key Inputs", "");
		this.addInput(desc, true);
	}

	/**
	 * Constructor for entity initializing members.
	 */
	public Entity() {
		entityNumber = getNextID();
		synchronized(allInstances) {
			allInstances.add(this);
		}

		flags = 0;
	}

	private static synchronized long getNextID() {
		return ++entityCount;
	}

	public static ArrayList<? extends Entity> getAll() {
		synchronized(allInstances) {
			return allInstances;
		}
	}


	public static <T extends Entity> ArrayList<T> getInstancesOf(Class<T> proto) {
		ArrayList<T> instanceList = new ArrayList<T>();

		for (Entity each : allInstances) {
			if (proto == each.getClass()) {
				instanceList.add(proto.cast(each));
			}
		}

		return instanceList;
	}

	public static <T extends Entity> ArrayList<T> getClonesOf(Class<T> proto) {
		ArrayList<T> cloneList = new ArrayList<T>();

		for (Entity each : allInstances) {
			if (proto.isAssignableFrom(each.getClass())) {
				cloneList.add(proto.cast(each));
			}
		}

		return cloneList;
	}

	public static Entity idToEntity(long id) {
		synchronized (allInstances) {
			for (Entity e : allInstances) {
				if (e.getEntityNumber() == id) {
					return e;
				}
			}
			return null;
		}
	}

	// This is defined for handlers only
	public void validate() throws InputErrorException {}

	public void earlyInit() {}

	public void startUp() {}

	public void kill() {
		synchronized (allInstances) {
			allInstances.remove(this);
		}
		if (namedEntities.get(this.getInputName()) == this)
			namedEntities.remove(this.getInputName());

		setFlag(FLAG_DEAD);
	}

	public void doEnd() {}

	public static Entity getNamedEntity(String name) {
		return namedEntities.get(name);
	}

	public static long getEntitySequence() {
		long seq = (long)allInstances.size() << 32;
		seq += entityCount;
		return seq;
	}

	/**
	 * Get the current Simulation ticks value.
	 * @return the current simulation tick
	 */
	public final long getSimTicks() {
		try {
			return Process.currentTick();
		}
		catch (ErrorException e) {
			return EventManager.rootManager.currentTick();
		}
	}

	/**
	 * Get the current Simulation time.
	 * @return the current time in seconds
	 */
	public final double getSimTime() {
		return Process.ticksToSeconds(getSimTicks());
	}

	public final double getCurrentTime() {
		long ticks = getSimTicks();
		return ticks / Process.getSimTimeFactor();
	}

	protected void mapInput(Input<?> in, String key) {
		if (inputMap.put(key.toUpperCase().intern(), in) != null) {
			System.out.format("WARN: keyword handled twice, %s:%s\n", this.getClass().getName(), key);
		}
	}

	protected void addInput(Input<?> in, boolean editable, String... synonyms) {
		this.mapInput(in, in.getKeyword());

		// Editable inputs are sorted by category
		if (editable) {

			int index = editableInputs.size();
			for( int i = editableInputs.size() - 1; i >= 0; i-- ) {
				Input<?> ei = editableInputs.get( i );
				if( ei.getCategory().equals( in.getCategory() ) ) {
					index = i+1;
					break;
				}
			}

			editableInputs.add(index, in);
		}

		for (String each : synonyms)
			this.mapInput(in, each);
	}

	public Input<?> getInput(String key) {
		return inputMap.get(key.toUpperCase());
	}

	/**
	 * Copy the inputs for each keyword to the caller.  Any inputs that have already
	 * been set for the caller are overwritten by those for the entity being copied.
	 * @param ent = entity whose inputs are to be copied
	 */
	public void copyInputs(Entity ent) {
		for(Input<?> sourceInput: ent.getEditableInputs() ){
			Input<?> targetInput = this.getInput(sourceInput.getKeyword());
			String val = sourceInput.getValueString();
			if( val.isEmpty() ) {
				if( ! targetInput.getValueString().isEmpty() )
					targetInput.reset();
			}
			else {
				InputAgent.processEntity_Keyword_Value(this, targetInput, val);
			}
		}
	}

	public void setFlag(int flag) {
		flags |= flag;
	}

	public void clearFlag(int flag) {
		flags &= ~flag;
	}

	public boolean testFlag(int flag) {
		return (flags & flag) != 0;
	}

	public void setTraceFlag() {
		traceFlag = true;
	}

	public void clearTraceFlag() {
		traceFlag = false;
	}

	/**
	 * Static method to get the eventManager for all entities.
	 */
	private EventManager getEventManager() {
		return EventManager.rootManager;
	}

	/**
	 * Method to return the name of the entity.
	 * Note that the name of the entity may not be the unique identifier used in the namedEntityHashMap; see Entity.toString()
	 */
	public String getName() {
		if (entityName == null)
			return "Entity-" + entityNumber;
		else
			return entityName;
	}

	/**
	 * Get the unique number for this entity
	 * @return
	 */
	public long getEntityNumber() {
		return entityNumber;
	}

	/**
	 * Method to set the name of the entity.
	 */
	public void setName(String newName) {
		entityName = newName;
	}

	/**
	 * Method to set the name of the entity to prefix+entityNumber.
	 */
	public void setNamePrefix(String prefix) {
		entityName = prefix + entityNumber;
	}

	/**
	 * Method to return the unique identifier of the entity. Used when building Edit tree labels
	 * @return entityName
	 */
	@Override
	public String toString() {
		return getInputName();
	}

	/**
	 * Method to set the input name of the entity.
	 */
	public void setInputName(String newName) {
		entityInputName = newName;
		namedEntities.put(newName, this);

		String name = newName;
		if (newName.contains("/"))
			name = newName.substring(newName.indexOf("/") + 1);

		this.setName(name);

	}

	/**
	 * Method to get the input name of the entity.
	 */
	public String getInputName() {
		if (entityInputName == null) {
			return this.getName();
		}
		else {
			return entityInputName;
		}
	}

	/**
	 * This method updates the Entity for changes in the given input
	 */
	public void updateForInput( Input<?> in ) {
		if (in == trace) {
			if (trace.getValue())
				this.setTraceFlag();
			else
				this.clearTraceFlag();

			return;
		}
	}

	/**
	 * Interpret the input data in the given buffer of strings corresponding to the given keyword.
	 * Reads keyword from a configuration file:
	 *  @param data - Vector of Strings containing data to be parsed
	 *  @param keyword - the keyword to determine what the data represents
	 */
	public void readData_ForKeyword(StringVector data, String keyword)
	throws InputErrorException {
		throw new InputErrorException( "Invalid keyword " + keyword );
	}

	static long calculateDelayLength(double waitLength) {
		return Math.round(waitLength * Process.getSimTimeFactor());
	}

	public double calculateDiscreteTime(double time) {
		long discTime = calculateDelayLength(time);
		return discTime / Process.getSimTimeFactor();
	}

	public double calculateEventTime(double waitLength) {
		long eventTime = Process.currentTick() + calculateDelayLength(waitLength);
		return eventTime / Process.getSimTimeFactor();
	}

	public double calculateEventTimeBefore(double waitLength) {
		long eventTime = Process.currentTick() + (long)Math.floor(waitLength * Process.getSimTimeFactor());
		return eventTime / Process.getSimTimeFactor();
	}

	public double calculateEventTimeAfter(double waitLength) {
		long eventTime = Process.currentTick() + (long)Math.ceil(waitLength * Process.getSimTimeFactor());
		return eventTime / Process.getSimTimeFactor();
	}

	public final void startProcess(String methodName, Object... args) {
		ProcessTarget t = new ReflectionTarget(this, methodName, args);
		Process.start(t);
	}

	public final void scheduleProcess(ProcessTarget t) {
		getEventManager().scheduleProcess(0, EventManager.PRIO_DEFAULT, t);
	}

	public final void scheduleProcess(double duration, int priority, ProcessTarget t) {
		long waitLength = calculateDelayLength(duration);
		getEventManager().scheduleProcess(waitLength, priority, t);
	}

	public final void scheduleSingleProcess(ProcessTarget t) {
		getEventManager().scheduleSingleProcess(0, EventManager.PRIO_LASTFIFO, t);
	}

	/**
	 * Wait a number of simulated seconds.
	 * @param secs
	 */
	public final void simWait(double secs) {
		simWait(secs, EventManager.PRIO_DEFAULT);
	}

	/**
	 * Wait a number of simulated seconds and a given priority.
	 * @param secs
	 * @param priority
	 */
	public final void simWait(double secs, int priority) {
		long ticks = Process.secondsToTicks(secs);
		this.simWaitTicks(ticks, priority);
	}

	/**
	 * Wait a number of discrete simulation ticks.
	 * @param secs
	 */
	public final void simWaitTicks(long ticks) {
		simWaitTicks(ticks, EventManager.PRIO_DEFAULT);
	}

	/**
	 * Wait a number of discrete simulation ticks and a given priority.
	 * @param secs
	 * @param priority
	 */
	public final void simWaitTicks(long ticks, int priority) {
		getEventManager().waitTicks(ticks, priority);
	}

	/**
	 * Wrapper of eventManager.scheduleWait(). Used as a syntax nicity for
	 * calling the wait method.
	 *
	 * @param duration The duration to wait
	 */
	public final void scheduleWait(double duration) {
		long waitLength = calculateDelayLength(duration);
		getEventManager().scheduleWait(waitLength, EventManager.PRIO_DEFAULT);
	}

	/**
	 * Wrapper of eventManager.scheduleWait(). Used as a syntax nicity for
	 * calling the wait method.
	 *
	 * @param duration The duration to wait
	 * @param priority The relative priority of the event scheduled
	 */
	public final void scheduleWait( double duration, int priority ) {
		long waitLength = calculateDelayLength(duration);
		getEventManager().scheduleWait(waitLength, priority);
	}

	/**
	 * Schedules an event to happen as the last event at the current time.
	 * Additional calls to scheduleLast will place a new event as the last event.
	 */
	public final void scheduleLastFIFO() {
		getEventManager().waitTicks(0, EventManager.PRIO_LASTFIFO);
	}

	/**
	 * Schedules an event to happen as the last event at the current time.
	 * Additional calls to scheduleLast will place a new event as the last event.
	 */
	public final void scheduleLastLIFO() {
		getEventManager().waitTicks(0, EventManager.PRIO_LASTLIFO);
	}

	public final void waitUntil() {
		getEventManager().waitUntil();
	}

	public final void waitUntilEnded() {
		getEventManager().waitUntilEnded();
	}

	// ******************************************************************************************************
	// EDIT TABLE METHODS
	// ******************************************************************************************************

	public ArrayList<Input<?>> getEditableInputs() {
		return editableInputs;
	}

	/**
	 * Make the given keyword editable from the edit box with Synonyms
	 */
	public void addEditableKeyword( String keyword, String unit, String defaultValue, boolean append, String category, String... synonymsArray ) {

		if( this.getInput( keyword ) == null ) {
			// Create a new input object
			CompatInput in = new CompatInput(this, keyword, category, defaultValue);
			in.setAppendable( append );
			this.addInput(in, true, synonymsArray);
		} else {
			System.out.format("Edited keyword added twice %s:%s%n", this.getClass().getName(), keyword);
		}
	}

	// ******************************************************************************************************
	// TRACING METHODS
	// ******************************************************************************************************

	/**
	 * Track the given subroutine.
	 */
	public void trace(String meth) {
		if (traceFlag) InputAgent.trace(0, this, meth);
	}

	/**
	 * Track the given subroutine.
	 */
	public void trace(int level, String meth) {
		if (traceFlag) InputAgent.trace(level, this, meth);
	}

	/**
	 * Track the given subroutine (one line of text).
	 */
	public void trace(String meth, String text1) {
		if (traceFlag) InputAgent.trace(0, this, meth, text1);
	}

	/**
	 * Track the given subroutine (two lines of text).
	 */
	public void trace(String meth, String text1, String text2) {
		if (traceFlag) InputAgent.trace(0, this, meth, text1, text2);
	}

	/**
	 * Print an addition line of tracing.
	 */
	public void traceLine(String text) {
		this.trace( 1, text );
	}

	/**
	 * Print an error message.
	 */
	public void error( String meth, String text1, String text2 ) {
		InputAgent.logError("Time:%.5f Entity:%s%n%s%n%s%n%s%n",
							getCurrentTime(), getName(),
							meth, text1, text2);

		// We don't want the model to keep executing, throw an exception and let
		// the higher layers figure out if we should terminate the run or not.
		throw new ErrorException("ERROR: %s", getName());
	}

	/**
	 * Print a warning message.
	 */
	public void warning( String meth, String text1, String text2 ) {
		InputAgent.logWarning("Time:%.5f Entity:%s%n%s%n%s%n%s%n",
				getCurrentTime(), getName(),
				meth, text1, text2);
	}

	public OutputHandle getOutputHandle(String outputName) {
		return new OutputHandle(this, outputName);
	}

	public boolean hasOutput(String outputName) {
		return OutputHandle.hasOutput(this.getClass(), outputName);
	}

	@Output(name = "Name",
	        description="The unique input name for this entity.")
	public String getNameOutput(double simTime) {
		return entityName;
	}

	@Output(name = "Description",
	        description="A string describing this entity.")
	public String getDescription(double simTime) {
		return desc.getValue();
	}
}
