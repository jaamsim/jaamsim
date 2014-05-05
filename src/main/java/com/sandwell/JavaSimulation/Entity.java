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

import com.jaamsim.basicsim.ClonesOfIterable;
import com.jaamsim.basicsim.InstanceIterable;
import com.jaamsim.basicsim.ReflectionTarget;
import com.jaamsim.events.EventHandle;
import com.jaamsim.events.EventManager;
import com.jaamsim.events.Process;
import com.jaamsim.events.ProcessTarget;
import com.jaamsim.input.AttributeHandle;
import com.jaamsim.input.InputAgent;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.Output;
import com.jaamsim.input.OutputHandle;
import com.jaamsim.ui.FrameBox;

/**
 * Abstract class that encapsulates the methods and data needed to create a
 * simulation object. Encapsulates the basic system objects to achieve discrete
 * event execution.
 */
public class Entity {
	static EventManager root;

	private static long entityCount = 0;
	private static final ArrayList<Entity> allInstances;
	private static final HashMap<String, Entity> namedEntities;

	private String entityName;
	private String entityInputName; // Name input by user
	private final long entityNumber;

	//public static final int FLAG_TRACE = 0x01; // reserved in case we want to treat tracing like the other flags
	//public static final int FLAG_TRACEREQUIRED = 0x02;
	//public static final int FLAG_TRACESTATE = 0x04;
	public static final int FLAG_LOCKED = 0x08;
	//public static final int FLAG_TRACKEVENTS = 0x10;
	public static final int FLAG_ADDED = 0x20;
	public static final int FLAG_EDITED = 0x40;
	public static final int FLAG_GENERATED = 0x80;
	public static final int FLAG_DEAD = 0x0100;
	private int flags;
	protected boolean traceFlag = false;

	private final ArrayList<Input<?>> editableInputs = new ArrayList<Input<?>>();
	private final ArrayList<SynRecord> synonyms = new ArrayList<SynRecord>();

	private final HashMap<String, Double> attributeMap = new HashMap<String, Double>();

	private final BooleanInput trace;

	@Keyword(description = "A free form string describing the Entity",
	         example = "Ent Description { 'A very useful entity' }")
	private final StringInput desc;

	@Keyword(description = "The list of user defined attributes for this entity and default values."
			+ " Input must be a name followed by an initial value.",
	         example = "Ent Attributes { Attrib-1 20.0 Attrib-2 42 }")
	private final StringListInput attributesInput;

	// constants used when scheduling events using the Entity wrappers
	public static final int PRIO_DEFAULT = 5;
	public static final int PRIO_LOWEST = 11;

	static {
		allInstances = new ArrayList<Entity>(100);
		namedEntities = new HashMap<String, Entity>(100);
	}

	{
		trace = new BooleanInput("Trace", "Key Inputs", false);
		trace.setHidden(true);
		this.addInput(trace);

		desc = new StringInput("Description", "Key Inputs", "");
		this.addInput(desc);

		attributesInput = new StringListInput("Attributes", "Key Inputs", new StringVector());
		this.addInput(attributesInput);
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

	public static synchronized final EventManager initEVT() {
		if (root != null) return root;
		root = EventManager.initEventManager("DefaultEventManager");
		return root;
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

	public static <T extends Entity> InstanceIterable<T> getInstanceIterator(Class<T> proto){
		return new InstanceIterable<T>(proto);
	}

	public static <T extends Entity> ClonesOfIterable<T> getClonesOfIterator(Class<T> proto){
		return new ClonesOfIterable<T>(proto);
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
		removeInputName();

		setFlag(FLAG_DEAD);
	}

	public void doEnd() {}

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
		return Process.currentTick();
	}

	/**
	 * Get the current Simulation time.
	 * @return the current time in seconds
	 */
	public final double getSimTime() {
		return root.ticksToSeconds(getSimTicks());
	}

	public final double getCurrentTime() {
		long ticks = getSimTicks();
		return ticks / Simulation.getSimTimeFactor();
	}

	protected void addInput(Input<?> in) {
		for (int i = 0; i < editableInputs.size(); i++) {
			Input<?> ein = editableInputs.get(i);
			if (ein.getKeyword().equalsIgnoreCase(in.getKeyword())) {
				System.out.format("WARN: keyword handled twice, %s:%s\n", this.getClass().getName(), in.getKeyword());
				return;
			}
		}

		editableInputs.add(in);
	}

	private static class SynRecord {
		final String syn;
		final Input<?> in;

		SynRecord(String s, Input<?> i) {
			syn = s;
			in = i;
		}
	}

	protected void addSynonym(Input<?> in, String synonym) {
		for (int i = 0; i < editableInputs.size(); i++) {
			Input<?> ein = editableInputs.get(i);
			if (ein.getKeyword().equalsIgnoreCase(synonym)) {
				System.out.format("WARN: keyword handled twice, %s:%s\n", this.getClass().getName(), synonym);
				return;
			}
		}

		for (int i = 0; i < synonyms.size(); i++) {
			SynRecord rec = synonyms.get(i);
			if (rec.syn.equalsIgnoreCase(synonym)) {
				System.out.format("WARN: keyword handled twice, %s:%s\n", this.getClass().getName(), synonym);
				return;
			}
		}
		synonyms.add(new SynRecord(synonym, in));
	}

	public Input<?> getInput(String key) {
		for (int i = 0; i < editableInputs.size(); i++) {
			Input<?> in = editableInputs.get(i);
			if (in.getKeyword().equalsIgnoreCase(key))
				return in;
		}

		for (int i = 0; i < synonyms.size(); i++) {
			SynRecord rec = synonyms.get(i);
			if (rec.syn.equalsIgnoreCase(key))
				return rec.in;
		}

		return null;
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
		return root;
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
	 * Method to return the unique identifier of the entity. Used when building Edit tree labels
	 * @return entityName
	 */
	@Override
	public String toString() {
		return getInputName();
	}

	public static Entity getNamedEntity(String name) {
		synchronized (namedEntities) {
			return namedEntities.get(name);
		}
	}

	private void removeInputName() {
		synchronized (namedEntities) {
			if (namedEntities.get(entityInputName) == this)
				namedEntities.remove(entityInputName);

			entityInputName = null;
		}
	}

	/**
	 * Method to set the input name of the entity.
	 */
	public void setInputName(String newName) {
		synchronized (namedEntities) {
			namedEntities.remove(entityInputName);
			entityInputName = newName;
			namedEntities.put(entityInputName, this);
		}
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
		if (in == attributesInput) {
			StringVector vals = attributesInput.getValue();
			if (vals.size() % 2 == 1) {
				throw new InputErrorException("Attributes must be a list of pairs of attribute names and values");
			}
			// Try to parse first to check for formatting errors
			for (int i = 0; i < vals.size(); i+=2) {
				String name = vals.get(i);
				String valueString = vals.get(i+1);
				try {
					Double.valueOf(valueString);
				} catch (NumberFormatException e) {
					throw new InputErrorException("Could not parse value from attribute string: %s", valueString);
				}
				if (OutputHandle.hasOutput(this.getClass(), name)) {
					throw new InputErrorException("Attribute name is the same as existing output name: %s", name);
				}
			}
			// Everything parsed, now there's no going back
			attributeMap.clear();

			for (int i = 0; i < vals.size(); i+=2) {
				String name = vals.get(i);
				double value = Double.valueOf(vals.get(i+1));

				addAttribute(name, value);
			}

			// Reselect the current entity (this is needed to update the OutputBox)
			FrameBox.reSelectEntity();
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
		return Math.round(waitLength * Simulation.getSimTimeFactor());
	}

	public double calculateDiscreteTime(double time) {
		long discTime = calculateDelayLength(time);
		return discTime / Simulation.getSimTimeFactor();
	}

	public double calculateEventTime(double waitLength) {
		long eventTime = Process.currentTick() + calculateDelayLength(waitLength);
		return eventTime / Simulation.getSimTimeFactor();
	}

	public double calculateEventTimeBefore(double waitLength) {
		long eventTime = Process.currentTick() + (long)Math.floor(waitLength * Simulation.getSimTimeFactor());
		return eventTime / Simulation.getSimTimeFactor();
	}

	public double calculateEventTimeAfter(double waitLength) {
		long eventTime = Process.currentTick() + (long)Math.ceil(waitLength * Simulation.getSimTimeFactor());
		return eventTime / Simulation.getSimTimeFactor();
	}

	public final void startProcess(String methodName, Object... args) {
		ProcessTarget t = new ReflectionTarget(this, methodName, args);
		startProcess(t);
	}

	public final void startProcess(ProcessTarget t) {
		getEventManager().start(t);
	}

	public final void scheduleProcess(ProcessTarget t) {
		getEventManager().scheduleProcess(0, Entity.PRIO_DEFAULT, false, t);
	}

	public final void scheduleProcess(double secs, int priority, ProcessTarget t) {
		long ticks = root.secondsToNearestTick(secs);
		getEventManager().scheduleProcess(ticks, priority, false, t);
	}

	public final void scheduleProcess(double secs, int priority, ProcessTarget t, EventHandle handle) {
		long ticks = root.secondsToNearestTick(secs);
		getEventManager().scheduleProcess(ticks, priority, false, t, handle);
	}

	public final void scheduleProcessTicks(long ticks, int priority, ProcessTarget t) {
		getEventManager().scheduleProcess(ticks, priority, false, t);
	}

	public final void scheduleSingleProcess(ProcessTarget t) {
		getEventManager().scheduleSingleProcess(0, Entity.PRIO_LOWEST, true, t);
	}

	public final void scheduleSingleProcess(ProcessTarget t, int priority) {
		getEventManager().scheduleSingleProcess(0, priority, false, t);
	}

	public final void scheduleSingleProcess(ProcessTarget t, int priority, boolean fifo) {
		getEventManager().scheduleSingleProcess(0, priority, fifo, t);
	}

	/**
	 * Wait a number of simulated seconds.
	 * @param secs
	 */
	public final void simWait(double secs) {
		simWait(secs, Entity.PRIO_DEFAULT);
	}

	/**
	 * Wait a number of simulated seconds and a given priority.
	 * @param secs
	 * @param priority
	 */
	public final void simWait(double secs, int priority) {
		long ticks = root.secondsToNearestTick(secs);
		this.simWaitTicks(ticks, priority);
	}

	/**
	 * Wait a number of simulated seconds and a given priority.
	 * @param secs
	 * @param priority
	 */
	public final void simWait(double secs, int priority, EventHandle handle) {
		long ticks = root.secondsToNearestTick(secs);
		this.simWaitTicks(ticks, priority, false, handle);
	}

	/**
	 * Wait a number of discrete simulation ticks.
	 * @param secs
	 */
	public final void simWaitTicks(long ticks) {
		simWaitTicks(ticks, Entity.PRIO_DEFAULT);
	}

	/**
	 * Wait a number of discrete simulation ticks and a given priority.
	 * @param secs
	 * @param priority
	 */
	public final void simWaitTicks(long ticks, int priority) {
		getEventManager().waitTicks(ticks, priority, false, null);
	}

	/**
	 * Wait a number of discrete simulation ticks and a given priority.
	 * @param secs
	 * @param priority
	 * @param fifo
	 * @param handle
	 */
	public final void simWaitTicks(long ticks, int priority, boolean fifo, EventHandle handle) {
		getEventManager().waitTicks(ticks, priority, fifo, handle);
	}

	/**
	 * Wrapper of eventManager.scheduleWait(). Used as a syntax nicity for
	 * calling the wait method.
	 *
	 * @param duration The duration to wait
	 */
	public final void scheduleWait(double duration) {
		long waitLength = calculateDelayLength(duration);
		if (waitLength == 0)
			return;
		getEventManager().waitTicks(waitLength, Entity.PRIO_DEFAULT, false, null);
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
		if (waitLength == 0)
			return;
		getEventManager().waitTicks(waitLength, priority, false, null);
	}

	/**
	 * Wrapper of eventManager.scheduleWait(). Used as a syntax nicity for
	 * calling the wait method.
	 *
	 * @param duration The duration to wait
	 * @param priority The relative priority of the event scheduled
	 */
	public final void scheduleWait( double duration, int priority, EventHandle handle ) {
		long waitLength = calculateDelayLength(duration);
		if (waitLength == 0)
			return;
		getEventManager().waitTicks(waitLength, priority, false, handle);
	}

	/**
	 * Schedules an event to happen as the last event at the current time.
	 * Additional calls to scheduleLast will place a new event as the last event.
	 */
	public final void scheduleLastFIFO() {
		getEventManager().waitTicks(0, Entity.PRIO_LOWEST, true, null);
	}

	/**
	 * Schedules an event to happen as the last event at the current time.
	 * Additional calls to scheduleLast will place a new event as the last event.
	 */
	public final void scheduleLastLIFO() {
		getEventManager().waitTicks(0, Entity.PRIO_LOWEST, false, null);
	}

	public final void waitUntil() {
		getEventManager().waitUntil();
	}

	public final void waitUntilEnded() {
		getEventManager().waitUntilEnded();
	}

	public final void killEvent(EventHandle handle) {
		getEventManager().killEvent(handle);
	}

	public final void interruptEvent(EventHandle handle) {
		getEventManager().interruptEvent(handle);
	}

	public final void killEvent(Process proc) {
		// Just return if given a null Process
		if (proc == null)
			return;

		getEventManager().terminateThread(proc);
	}

	public final long secondsToNearestTick(double seconds) {
		return root.secondsToNearestTick(seconds);
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
	public void addEditableKeyword( String keyword, String unit, String defaultValue, boolean append, String category) {

		if( this.getInput( keyword ) == null ) {
			// Create a new input object
			CompatInput in = new CompatInput(this, keyword, category, defaultValue);
			in.setAppendable( append );
			this.addInput(in);
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
		if (hasAttribute(outputName))
			return new AttributeHandle(this, outputName);

		if (hasOutput(outputName))
			return new OutputHandle(this, outputName);

		return null;
	}

	public boolean hasOutput(String outputName) {
		if (OutputHandle.hasOutput(this.getClass(), outputName))
			return true;
		if (attributeMap.containsKey(outputName))
			return true;

		return false;
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

	public double getAttribute(String name) {
		Double val = attributeMap.get(name);
		if (val == null) return 0; // TODO: how should this be handled?
		return val.doubleValue();
	}

	private void addAttribute(String name, double initialValue) {
		attributeMap.put(name, initialValue);
	}

	public boolean hasAttribute(String name) {
		return attributeMap.containsKey(name);
	}

	public void setAttribute(String name, double value) {
		if (!attributeMap.containsKey(name)) {
			// TODO: report this as an error?
			return;
		}

		attributeMap.put(name, value);
	}

	public ArrayList<String> getAttributeNames(){
		ArrayList<String> ret = new ArrayList<String>();
		for (String name : attributeMap.keySet()) {
			ret.add(name);
		}
		return ret;
	}
}
