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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import com.jaamsim.events.ProcessTarget;
import com.jaamsim.events.ReflectionTarget;
import com.jaamsim.input.InputAgent;
import com.jaamsim.input.InputGroup;
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

	private HashMap<String, OutputHandle> outputCache = null;

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

	private final ArrayList<Input<?>> editableInputs;
	private final HashMap<String, Input<?>> inputMap;

	private final ArrayList<InputGroup> inputGroups;

	private final BooleanInput trace;

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

		trace = new BooleanInput("Trace", "Key Inputs", false);
		trace.setHidden(true);
		this.addInput(trace, true);
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
		long eventTime = Process.currentTick() + this.calculateDelayLength(waitLength);
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

	public final void startExternalProcess(String methodName, Object... args) {
		getEventManager().startExternalProcess(this, methodName, args);
	}

	public final void scheduleSingleProcess(String methodName, Object... args) {
		getEventManager().scheduleSingleProcess(0, EventManager.PRIO_LASTFIFO, this, methodName, args);
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
		getEventManager().waitTicks(ticks, priority, this);
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
			in.setUnits(unit);
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

	private static class OutputComparator implements Comparator<OutputHandle> {

		@Override
		public int compare(OutputHandle oh0, OutputHandle oh1) {
			Class<?> class0 = oh0.method.getDeclaringClass();
			Class<?> class1 = oh1.method.getDeclaringClass();

			if (class0 == class1) {
				return oh0.annotation.name().compareTo(oh1.annotation.name());
			}

			if (class0.isAssignableFrom(class1))
				return -1;
			else
				return 1;
		}
	}

	public final ArrayList<OutputHandle> getOutputs() {
		ArrayList<OutputHandle> handles = new ArrayList<OutputHandle>();
		for (Method m : this.getClass().getMethods()) {
			Output o = m.getAnnotation(Output.class);
			if (o == null)
				continue;

			// Check that this method only takes a single double (simTime) parameter
			Class<?>[] paramTypes = m.getParameterTypes();
			if (paramTypes.length != 1 ||
			    paramTypes[0] != double.class) {
				continue;
			}

			OutputHandle handle = new OutputHandle(o, m);
			handles.add(handle);
		}

		Collections.sort(handles, new OutputComparator());
		return handles;
	}

	private void buildOutputCache() {
		outputCache = new HashMap<String, OutputHandle>();
		ArrayList<OutputHandle> handles = new ArrayList<OutputHandle>();
		for (Method m : this.getClass().getMethods()) {
			Output o = m.getAnnotation(Output.class);
			if (o == null) {
				continue;
			}

			// Check that this method only takes a single double (simTime) parameter
			Class<?>[] paramTypes = m.getParameterTypes();
			if (paramTypes.length != 1 ||
				paramTypes[0] != double.class) {
				continue;
			}

			OutputHandle handle = new OutputHandle(o, m);
			handles.add(handle);

			outputCache.put(o.name(), handle);
		}

		Collections.sort(handles, new OutputComparator());
	}

	public OutputHandle getOutputHandle(String outputName) {
		// lazily initialize the output cache
		if (outputCache == null) {
			buildOutputCache();
		}

		return outputCache.get(outputName);
	}

	@SuppressWarnings("unchecked") // This suppresses the warning on the cast, which is effectively checked
	public <T> T getOutputValue(OutputHandle out, double simTime, Class<T> klass) {
		if (out.method == null) {
			return null;
		}

		T ret = null;
		try {
			if (!klass.isAssignableFrom(out.getReturnType()))
				return null;

			ret = (T)out.method.invoke(this, simTime);
		}
		catch (InvocationTargetException ex) {}
		catch (IllegalAccessException ex) {}
		catch (ClassCastException ex) {}
		return ret;
	}

	/**
	 * A generic method to return any declared outputs for this Entity
	 * @param outputName - The name of the output
	 * @param simTime - the simulation time to get the value for
	 * @param klass - the class of the return type expected
	 * @return
	 */
	private <T> T getOutputValueImp(String outputName, double simTime, Class<T> klass) {
		OutputHandle out = this.getOutputHandle(outputName);
		return this.getOutputValue(out, simTime, klass);
	}

	private <T> T getInputValueImp(String inputName, double simTime, Class<T> klass) {
		Input<?> input = inputMap.get(inputName.toUpperCase().intern());

		if (input == null) {
			return null;
		}

		T ret = null;
		try {
			ret = klass.cast(input.getValue());
		}
		catch(ClassCastException ex) {}

		return ret;
	}

	/**
	 * Returns the value of the Output for 'outputName' at 'simTime', if if can be cast to 'klass'
	 * This also checks the entities inputs, effectively mapping inputs to outputs
	 * @param outputName
	 * @param simTime
	 * @param klass
	 * @return
	 */
	public <T> T getOutputValue(String outputName, double simTime, Class<T> klass) {
		T ret = getOutputValueImp(outputName, simTime, klass);
		if (ret != null) {
			return ret;
		}
		// Instead try the inputs
		return getInputValueImp(outputName, simTime, klass);
	}

	/**
	 * Return the value of the output at 'simTime' with toString() called on it
	 * @param outputName
	 * @param simTime
	 * @return
	 */
	public String getOutputAsString(String outputName, double simTime) {
		OutputHandle out = this.getOutputHandle(outputName);
		if (out.method == null) {
			// Instead try the inputs
			return getInputAsString(outputName);
		}
		String ret = null;
		try {
			Object o = out.method.invoke(this, simTime);
			if (o == null)
				return null;
			return o.toString();
		} catch (InvocationTargetException ex) {
			assert false;
		} catch (IllegalAccessException ex) {
			assert false;
		}
		return ret;
	}

	private String getInputAsString(String inputName) {
		Input<?> input = inputMap.get(inputName.toUpperCase().intern());
		if (input == null) {
			return null;
		}
		Object val = input.getValue();
		if (val == null) {
			return "null";
		}
		return val.toString();
	}

	public boolean hasOutput(String name, boolean includeInputs) {
		if (outputCache == null)
			buildOutputCache();

		if (outputCache.containsKey(name))
			return true;

		if (includeInputs && this.getInput(name) != null)
			return true;

		return false;
	}

	@Output(name = "Name",
	        description="The unique input name for this entity.")
	public String getNameOutput(double simTime) {
		return entityName;
	}
}
