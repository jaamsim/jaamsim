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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import com.jaamsim.input.InputAgent;
import com.jaamsim.input.InputGroup;
import com.jaamsim.input.Output;
import com.jaamsim.math.Vec3d;
import com.jaamsim.ui.FrameBox;

/**
 * Abstract class that encapsulates the methods and data needed to create a
 * simulation object. Encapsulates the basic system objects to achieve discrete
 * event execution.
 */
public class Entity {
	private static long entityCount = 0;
	private static final ArrayList<Entity> allInstances;
	private static final HashMap<String, Entity> namedEntities;

	private EventManager eventManager;

	private String entityName;
	private String entityInputName; // Name input by user
	private final long entityNumber;

	//public static final int FLAG_TRACE = 0x01; // reserved in case we want to treat tracing like the other flags
	public static final int FLAG_TRACEREQUIRED = 0x02;
	public static final int FLAG_TRACESTATE = 0x04;
	public static final int FLAG_LOCKED = 0x08;
	public static final int FLAG_TRACKEVENTS = 0x10;
	public static final int FLAG_ADDED = 0x20;
	public static final int FLAG_EDITED = 0x40;
	public static final int FLAG_GENERATED = 0x80;
	public static final int FLAG_DEAD = 0x0100;
	private int flags;
	protected boolean traceFlag = false;

	private final ArrayList<Input<?>> editableInputs;
	private final HashMap<String, Input<?>> inputMap;

	private final ArrayList<InputGroup> inputGroups;

	static {
		allInstances = new ArrayList<Entity>(100);
		namedEntities = new HashMap<String, Entity>(100);
	}

	/**
	 * Constructor for entity initializing members.
	 */
	public Entity() {
		entityNumber = ++entityCount;
		synchronized(allInstances) {
			allInstances.add(this);
		}

		flags = 0;

		editableInputs = new ArrayList<Input<?>>();
		inputMap = new HashMap<String, Input<?>>();
		inputGroups = new ArrayList<InputGroup>();
	}

	public static ArrayList<? extends Entity> getAll() {
		synchronized(allInstances) {
			return allInstances;
		}
	}

	public static ArrayList<? extends Entity> getAllCopy() {
		synchronized(allInstances) {
			return new ArrayList<Entity>(allInstances);
		}
	}

	public static String idToName(long id) {
		synchronized (allInstances) {
			for (Entity e : allInstances) {
				if (e.getEntityNumber() == id) {
					return e.getInputName();
				}
			}
			return "";
		}
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
	public void validate() throws InputErrorException {
		for (InputGroup grp : inputGroups) {
			grp.validate();
		}
	}

	public void earlyInit() {
		for (InputGroup grp : inputGroups) {
			grp.earlyInit();
		}
	}

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
		//synchronized (allInstances) {
			long seq = (long)allInstances.size() << 32;
			seq += entityCount;
			return seq;
		//}
	}

	public final double getCurrentTime() {
		long internalTime = 0;
		try {
			internalTime = Process.currentTime();
		}
		catch (ErrorException e) {
			internalTime = EventManager.rootManager.currentTime();
		}
		return internalTime / Process.getSimTimeFactor();
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

	protected void addInputGroup(InputGroup grp) {

		inputGroups.add(grp);

		for (Input<?> in : grp.getInputs()) {
			this.addInput(in, true);
		}
	}

	public Input<?> getInput(String key) {
		return inputMap.get(key.toUpperCase());
	}

	public ArrayList<InputGroup> getInputGroups() {
		return inputGroups;
	}

	public void resetInputs() {
        Iterator<Entry<String,Input<?>>> iterator = inputMap.entrySet().iterator();
        while(iterator. hasNext()){
            iterator.next().getValue().reset();
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
	public EventManager getEventManager() {
		if (eventManager != null)
			return eventManager;

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
	 * Method to set an alias of the entity.
	 */
	public void setAlias(String newName) {
		namedEntities.put(newName, this);
	}

	/**
	 * Method to remove an alias of the entity.
	 */
	public void removeAlias(String newName) {
		namedEntities.remove(newName);
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
	public void updateForInput( Input<?> in ) {}

	public final void readInput(StringVector data, String keyword, boolean syntaxOnly, boolean isCfgInput)
	throws InputErrorException {
		Input<?> in = this.getInput(keyword);
		if (in != null) {
//			System.out.format("Parsing using input object:%s\n", in.getKeyword());
			in.parse(data);
			this.updateForInput( in );
		} else {
			this.readData_ForKeyword(data, keyword, syntaxOnly, isCfgInput);
		}
		FrameBox.valueUpdate();
	}

	/**
	 * Interpret the input data in the given buffer of strings corresponding to the given keyword.
	 * Reads keyword from a configuration file:
	 *   TRACE		 - trace flag (0 = off, 1 = on)
	 *  @param data - Vector of Strings containing data to be parsed
	 *  @param keyword - the keyword to determine what the data represents
	 */
	public void readData_ForKeyword(StringVector data, String keyword, boolean syntaxOnly, boolean isCfgInput)
	throws InputErrorException {
		if ("EVENTMANAGER".equalsIgnoreCase(keyword)) {
			EventManager evt = EventManager.getDefinedManager(data.get(0));

			if (evt == null) {
				throw new InputErrorException("EventManager %s not defined", data.get(0));
			}
			eventManager = evt;
			return;
		}

		if( "TRACE".equalsIgnoreCase( keyword ) ) {
			Input.assertCount(data, 1);
			boolean trace = Input.parseBoolean(data.get(0));
			if (trace)
				this.setFlag(FLAG_TRACEREQUIRED);
			else
				this.clearFlag(FLAG_TRACEREQUIRED);
			return;
		}
		if( "TRACESTATE".equalsIgnoreCase( keyword ) ) {
			Input.assertCount(data, 1);
			boolean value = Input.parseBoolean(data.get(0));
			if (value)
				this.setFlag(FLAG_TRACESTATE);
			else
				this.clearFlag(FLAG_TRACESTATE);
			return;
		}

		// --------------- LOCK ---------------
		if( "LOCK".equalsIgnoreCase( keyword ) ) {
			Input.assertCount(data, 1);
			boolean value = Input.parseBoolean(data.get(0));

			if (!value)
				throw new InputErrorException("Object cannot be unlocked");

			if (value)
				this.setFlag(FLAG_LOCKED);
			else
				this.clearFlag(FLAG_LOCKED);
			return;
		}

		// --------------- LOCKKEYWORDS ---------------
		if( "LOCKKEYWORDS".equalsIgnoreCase( keyword ) ) {

			// Check that the individual keywords for locking are valid
			for( int i = 0; i < data.size(); i++ ) {
				if( this.getInput( data.get(i) ) == null ) {
					throw new InputErrorException( "LockKeyword " + data.get( i ) + " is not a valid keyword" );
				}
			}
			// Lock the individual keywords for this entity
			for( int i = 0; i < data.size(); i++ ) {
				Input<?> input = this.getInput( data.get(i) );
				input.setLocked( true );
			}
			return;
		}

		throw new InputErrorException( "Invalid keyword " + keyword );
	}

	/**
	 * Accessor to return information about the entity.
	 */
	public Vector getInfo() {
		Vector info = new Vector( 1, 1 );
		info.addElement( "Name" + "\t" + getName() );
		String[] temp = getClass().getName().split( "[.]" );
		info.addElement( "Type" + "\t" + temp[temp.length - 1] );
		return info;
	}

	private long calculateDelayLength(double waitLength) {
		return Math.round(waitLength * Process.getSimTimeFactor());
	}

	public double calculateDiscreteTime(double time) {
		long discTime = calculateDelayLength(time);
		return discTime / Process.getSimTimeFactor();
	}

	public double calculateEventTime(double waitLength) {
		long eventTime = Process.currentTime() + this.calculateDelayLength(waitLength);
		return eventTime / Process.getSimTimeFactor();
	}

	public double calculateEventTimeBefore(double waitLength) {
		long eventTime = Process.currentTime() + (long)Math.floor(waitLength * Process.getSimTimeFactor());
		return eventTime / Process.getSimTimeFactor();
	}

	public double calculateEventTimeAfter(double waitLength) {
		long eventTime = Process.currentTime() + (long)Math.ceil(waitLength * Process.getSimTimeFactor());
		return eventTime / Process.getSimTimeFactor();
	}

	public final void startProcess(String methodName, Object... args) {
		Process.start(this, methodName, args);
	}

	public final void startExternalProcess(String methodName, Object... args) {
		getEventManager().startExternalProcess(this, methodName, args);
	}

	public final void scheduleSingleProcess(String methodName, Object... args) {
		getEventManager().scheduleSingleProcess(0, EventManager.PRIO_LASTFIFO, this, methodName, args);
	}

	/**
	 * Wrapper of eventManager.scheduleWait(). Used as a syntax nicity for
	 * calling the wait method.
	 *
	 * @param duration The duration to wait
	 */
	public final void scheduleWait(double duration) {
		long waitLength = calculateDelayLength(duration);
		getEventManager().scheduleWait(waitLength, EventManager.PRIO_DEFAULT, this);
	}

	public final void scheduleWaitBefore(double duration) {
		long waitLength = (long)Math.floor(duration * Process.getSimTimeFactor());
		getEventManager().scheduleWait(waitLength, EventManager.PRIO_DEFAULT, this);
	}

	public final void scheduleWaitBefore(double duration, int priority) {
		long waitLength = (long)Math.floor(duration * Process.getSimTimeFactor());
		getEventManager().scheduleWait(waitLength, priority, this);
	}

	public final void scheduleWaitAfter(double duration) {
		long waitLength = (long)Math.ceil(duration * Process.getSimTimeFactor());
		getEventManager().scheduleWait(waitLength, EventManager.PRIO_DEFAULT, this);
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
		getEventManager().scheduleWait(waitLength, priority, this);
	}

	/**
	 * Schedules an event to happen as the last event at the current time.
	 * Additional calls to scheduleLast will place a new event as the last event.
	 */
	public final void scheduleLastFIFO() {
		getEventManager().scheduleLastFIFO( this );
	}

	/**
	 * Schedules an event to happen as the last event at the current time.
	 * Additional calls to scheduleLast will place a new event as the last event.
	 */
	public final void scheduleLastLIFO() {
		getEventManager().scheduleLastLIFO( this );
	}

	public final void waitUntil() {
		getEventManager().waitUntil(this);
	}

	public final void waitUntilEnded() {
		getEventManager().waitUntilEnded(this);
	}

	/**
	 * Increment clock by the minimum time step
	 */
	public final void scheduleWaitOneTick() {
		getEventManager().scheduleWait(1, EventManager.PRIO_DEFAULT, this);
	}

	public final void scheduleWaitOneTick( int priority ) {
		getEventManager().scheduleWait(1, priority, this);
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
			CompatInput in = new CompatInput(this, keyword, category, unit, defaultValue);
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

	@SuppressWarnings("unchecked") // This is to suppress the cast to T warning, which really is checked
	public <T> T getOutputValue(String outputName, double simTime, Class<T> klass) {
		for (Method m : this.getClass().getMethods()) {
			Output o = m.getAnnotation(Output.class);
			if (o == null) {
				continue;
			}
			// Check the name
			if (!o.name().equals(outputName)) {
				continue;
			}

			// check the types
			if (m.getReturnType() != klass) {
				continue;
			}

			Class<?>[] paramTypes = m.getParameterTypes();
			if (paramTypes.length != 1 ||
				paramTypes[0] != double.class) {
				continue;
			}

			// Okay, this is definitely the method we are looking for
			T ret = null;
			try {
				ret = (T)(m.invoke(this, simTime));
			} catch (InvocationTargetException ex) {
				assert false;
			} catch (IllegalAccessException ex) {
				assert false;
			}
			return ret;
		}
		// No output found
		return null;
	}

	public Double getDoubleOutput(String outputName, double simTime) {
		return getOutputValue(outputName, simTime, Double.class);
	}
	public Vec3d getVec3dOutput(String outputName, double simTime) {
		return getOutputValue(outputName, simTime, Vec3d.class);
	}
}
