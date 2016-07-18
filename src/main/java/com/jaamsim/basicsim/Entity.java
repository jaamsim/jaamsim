/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2002-2011 Ausenco Engineering Canada Inc.
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicLong;

import com.jaamsim.datatypes.DoubleVector;
import com.jaamsim.events.Conditional;
import com.jaamsim.events.EventHandle;
import com.jaamsim.events.EventManager;
import com.jaamsim.events.ProcessTarget;
import com.jaamsim.input.AttributeDefinitionListInput;
import com.jaamsim.input.AttributeHandle;
import com.jaamsim.input.BooleanInput;
import com.jaamsim.input.ExpressionHandle;
import com.jaamsim.input.Input;
import com.jaamsim.input.InputAgent;
import com.jaamsim.input.InputErrorException;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.KeywordIndex;
import com.jaamsim.input.NamedExpression;
import com.jaamsim.input.NamedExpressionListInput;
import com.jaamsim.input.Output;
import com.jaamsim.input.OutputHandle;
import com.jaamsim.input.StringInput;
import com.jaamsim.input.SynonymInput;
import com.jaamsim.ui.FrameBox;
import com.jaamsim.units.DimensionlessUnit;
import com.jaamsim.units.TimeUnit;
import com.jaamsim.units.Unit;
import com.jaamsim.units.UserSpecifiedUnit;

/**
 * Abstract class that encapsulates the methods and data needed to create a
 * simulation object. Encapsulates the basic system objects to achieve discrete
 * event execution.
 */
public class Entity {
	private static AtomicLong entityCount = new AtomicLong(0);
	private static final ArrayList<Entity> allInstances;
	private static final HashMap<String, Entity> namedEntities;

	private String entityName;
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

	private final ArrayList<Input<?>> inpList = new ArrayList<>();

	private final HashMap<String, AttributeHandle> attributeMap = new LinkedHashMap<>();
	private final HashMap<String, ExpressionHandle> customOutputMap = new LinkedHashMap<>();

	private final BooleanInput trace;

	@Keyword(description = "A free form string describing the Entity",
	         exampleList = {"'A very useful entity'"})
	protected final StringInput desc;

	@Keyword(description = "The list of user defined attributes for this entity.\n" +
			" The attribute name is followed by its initial value. The unit provided for" +
			"this value will determine the attribute's unit type.",
	         exampleList = {"{ A 20.0 s } { alpha 42 }"})
	public final AttributeDefinitionListInput attributeDefinitionList;


	@Keyword(description = "The list of user defined custom outputs for this entity.\n" +
			" The output name is followed by an expression to be evaluated as the output and the unit type of the expression.",
	         exampleList = {"{ TwiceSimTime '2*this.SimTime' TimeUnit } { CargoVolume 'this.Cargo/this.CargoDensity' VolumeUnit }"})
	public final NamedExpressionListInput namedExpressionInput;

	static {
		allInstances = new ArrayList<>(100);
		namedEntities = new HashMap<>(100);
	}

	{
		trace = new BooleanInput("Trace", "Key Inputs", false);
		trace.setHidden(true);
		this.addInput(trace);

		desc = new StringInput("Description", "Key Inputs", "");
		desc.setHidden(true);
		this.addInput(desc);

		attributeDefinitionList = new AttributeDefinitionListInput(this, "AttributeDefinitionList",
				"Key Inputs", new ArrayList<AttributeHandle>());
		attributeDefinitionList.setHidden(false);
		this.addInput(attributeDefinitionList);

		namedExpressionInput = new NamedExpressionListInput(this, "CustomOutputList",
				"Key Inputs", new ArrayList<NamedExpression>());
		namedExpressionInput.setHidden(false);
		this.addInput(namedExpressionInput);

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

	private static long getNextID() {
		return entityCount.incrementAndGet();
	}

	public static ArrayList<? extends Entity> getAll() {
		synchronized(allInstances) {
			return allInstances;
		}
	}

	public static <T extends Entity> InstanceIterable<T> getInstanceIterator(Class<T> proto){
		return new InstanceIterable<>(proto);
	}

	public static <T extends Entity> ClonesOfIterable<T> getClonesOfIterator(Class<T> proto){
		return new ClonesOfIterable<>(proto);
	}

	/**
	 * Returns an iterator over the given proto class, but also filters only those
	 * objects that implement the given interface class.
	 * @return
	 */
	public static <T extends Entity> ClonesOfIterableInterface<T> getClonesOfIterator(Class<T> proto, Class<?> iface){
		return new ClonesOfIterableInterface<>(proto, iface);
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

	public void validate() throws InputErrorException {
		for (Input<?> in : inpList) {
			in.validate();
		}
	}

	/**
	 * Initialises the entity prior to the start of the model run.
	 * <p>
	 * This method must not depend on any other entities so that it can be
	 * called for each entity in any sequence.
	 */
	public void earlyInit() {

		// Reset the attributes to their initial values
		for (AttributeHandle h : attributeMap.values()) {
			h.setValue(h.getInitialValue());
		}
	}

	/**
	 * Initialises the entity prior to the start of the model run.
	 * <p>
	 * This method assumes other entities have already called earlyInit.
	 */
	public void lateInit() {}

	/**
	 * Starts the execution of the model run for this entity.
	 * <p>
	 * If required, initialisation that depends on another entity can be
	 * performed in this method. It is called after earlyInit().
	 */
	public void startUp() {}

	/**
	 * Resets the statistics collected by the entity.
	 */
	public void clearStatistics() {}

	/**
	 * Assigns input values that are helpful when the entity is dragged and
	 * dropped into a model.
	 */
	public void setInputsForDragAndDrop() {}

	public void kill() {
		synchronized (allInstances) {
			for (int i = 0; i < allInstances.size(); i++) {
				if (allInstances.get(i) == this) {
					allInstances.remove(i);
					break;
				}
			}
		}
		if (!testFlag(FLAG_GENERATED)) {
			synchronized (namedEntities) {
				if (namedEntities.get(entityName) == this)
					namedEntities.remove(entityName);

				entityName = null;
			}
		}

		setFlag(FLAG_DEAD);
	}

	/**
	 * Performs any actions that are required at the end of the simulation run, e.g. to create an output report.
	 */
	public void doEnd() {}

	public static long getEntitySequence() {
		long seq = (long)allInstances.size() << 32;
		seq += entityCount.get();
		return seq;
	}

	/**
	 * Get the current Simulation ticks value.
	 * @return the current simulation tick
	 */
	public final long getSimTicks() {
		return EventManager.simTicks();
	}

	/**
	 * Get the current Simulation time.
	 * @return the current time in seconds
	 */
	public final double getSimTime() {
		return EventManager.simSeconds();
	}

	protected void addInput(Input<?> in) {
		inpList.add(in);
	}

	protected void addSynonym(Input<?> in, String synonym) {
		inpList.add(new SynonymInput(synonym, in));
	}

	public final Input<?> getInput(String key) {
		for (int i = 0; i < inpList.size(); i++) {
			Input<?> in = inpList.get(i);
			if (key.equals(in.getKeyword())) {
				if (in.isSynonym())
					return ((SynonymInput)in).input;
				else
					return in;
			}
		}

		return null;
	}

	/**
	 * Copy the inputs for each keyword to the caller.  Any inputs that have already
	 * been set for the caller are overwritten by those for the entity being copied.
	 * @param ent = entity whose inputs are to be copied
	 */
	public void copyInputs(Entity ent) {
		ArrayList<String> tmp = new ArrayList<>();
		for (Input<?> sourceInput : ent.inpList) {
			if (sourceInput.isDefault() || sourceInput.isSynonym()) {
				continue;
			}
			tmp.clear();
			sourceInput.getValueTokens(tmp);
			KeywordIndex kw = new KeywordIndex(sourceInput.getKeyword(), tmp, null);
			InputAgent.apply(this, kw);
		}
	}

	/**
	 * Creates an exact copy of the specified entity.
	 * <p>
	 * All the entity's inputs are copied to the new entity, but its internal
	 * properties are left uninitialised.
	 * @param ent - entity to be copied.
	 * @param name - name of the copied entity.
	 * @return - copied entity.
	 */
	public static <T extends Entity> T fastCopy(T ent, String name) {
		// Create the new entity
		@SuppressWarnings("unchecked")
		T ret = (T)InputAgent.generateEntityWithName(ent.getClass(), name);
		// Loop through the original entity's inputs
		ArrayList<Input<?>> orig = ent.getEditableInputs();
		for (int i = 0; i < orig.size(); i++) {
			Input<?> sourceInput = orig.get(i);

			// Default values do not need to be copied
			if (sourceInput.isDefault() || sourceInput.isSynonym())
				continue;

			// Assign the value to the copied entity's input
			Input<?> targetInput = ret.getEditableInputs().get(i);
			targetInput.copyFrom(sourceInput);
			ret.updateForInput(targetInput);
		}
		return ret;
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
	 * Method to return the name of the entity.
	 * Note that the name of the entity may not be the unique identifier used in the namedEntityHashMap; see Entity.toString()
	 */
	public final String getName() {
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
	 * Method to return the unique identifier of the entity. Used when building Edit tree labels
	 * @return entityName
	 */
	@Override
	public String toString() {
		return getName();
	}

	public static Entity getNamedEntity(String name) {
		synchronized (namedEntities) {
			return namedEntities.get(name);
		}
	}

	/**
	 * Method to set the input name of the entity.
	 */
	public void setName(String newName) {
		if (testFlag(FLAG_GENERATED)) {
			entityName = newName;
			return;
		}

		synchronized (namedEntities) {
			namedEntities.remove(entityName);
			entityName = newName;
			namedEntities.put(entityName, this);
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

		if (in == attributeDefinitionList) {
			attributeMap.clear();
			for (AttributeHandle h : attributeDefinitionList.getValue()) {
				this.addAttribute(h.getName(), h);
			}

			// Update the OutputBox
			FrameBox.reSelectEntity();
			return;
		}
		if (in == namedExpressionInput) {
			customOutputMap.clear();
			for (NamedExpression ne : namedExpressionInput.getValue()) {
				ExpressionHandle eh = new ExpressionHandle(this, ne.getExpression(), ne.getName());
				eh.setUnitType(ne.getUnitType());
				customOutputMap.put(ne.getName(), eh);
			}

			// Update the OutputBox
			FrameBox.reSelectEntity();
			return;
		}

	}

	public final void startProcess(String methodName, Object... args) {
		EventManager.startProcess(new ReflectionTarget(this, methodName, args));
	}

	public final void startProcess(ProcessTarget t) {
		EventManager.startProcess(t);
	}

	public final void scheduleProcess(double secs, int priority, ProcessTarget t) {
		EventManager.scheduleSeconds(secs, priority, false, t, null);
	}

	public final void scheduleProcess(double secs, int priority, String methodName, Object... args) {
		EventManager.scheduleSeconds(secs, priority, false, new ReflectionTarget(this, methodName, args), null);
	}

	public final void scheduleProcess(double secs, int priority, ProcessTarget t, EventHandle handle) {
		EventManager.scheduleSeconds(secs, priority, false, t, handle);
	}

	public final void scheduleProcess(double secs, int priority, boolean fifo, ProcessTarget t, EventHandle handle) {
		EventManager.scheduleSeconds(secs, priority, fifo, t, handle);
	}

	public final void scheduleProcessTicks(long ticks, int priority, boolean fifo, ProcessTarget t, EventHandle h) {
		EventManager.scheduleTicks(ticks, priority, fifo, t, h);
	}

	public final void scheduleProcessTicks(long ticks, int priority, ProcessTarget t) {
		EventManager.scheduleTicks(ticks, priority, false, t, null);
	}

	public final void scheduleProcessTicks(long ticks, int priority, String methodName, Object... args) {
		EventManager.scheduleTicks(ticks, priority, false, new ReflectionTarget(this, methodName, args), null);
	}

	public final void waitUntil(Conditional cond, EventHandle handle) {
		// Don't actually wait if the condition is already true
		if (cond.evaluate()) return;
		EventManager.waitUntil(cond, handle);
	}

	/**
	 * Wait a number of simulated seconds and a given priority.
	 * @param secs
	 * @param priority
	 */
	public final void simWait(double secs, int priority) {
		EventManager.waitSeconds(secs, priority, false, null);
	}

	/**
	 * Wait a number of simulated seconds and a given priority.
	 * @param secs
	 * @param priority
	 */
	public final void simWait(double secs, int priority, EventHandle handle) {
		EventManager.waitSeconds(secs, priority, false, handle);
	}

	/**
	 * Wait a number of simulated seconds and a given priority.
	 * @param secs
	 * @param priority
	 */
	public final void simWait(double secs, int priority, boolean fifo, EventHandle handle) {
		EventManager.waitSeconds(secs, priority, fifo, handle);
	}

	/**
	 * Wait a number of discrete simulation ticks and a given priority.
	 * @param secs
	 * @param priority
	 */
	public final void simWaitTicks(long ticks, int priority) {
		EventManager.waitTicks(ticks, priority, false, null);
	}

	/**
	 * Wait a number of discrete simulation ticks and a given priority.
	 * @param secs
	 * @param priority
	 * @param fifo
	 * @param handle
	 */
	public final void simWaitTicks(long ticks, int priority, boolean fifo, EventHandle handle) {
		EventManager.waitTicks(ticks, priority, fifo, handle);
	}

	public void handleSelectionLost() {}

	// ******************************************************************************************************
	// EDIT TABLE METHODS
	// ******************************************************************************************************

	public ArrayList<Input<?>> getEditableInputs() {
		return inpList;
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
	 * Throws an ErrorException for this entity with the specified message.
	 * @param fmt - format string for the error message
	 * @param args - objects used by the format string
	 * @throws ErrorException
	 */
	public void error(String fmt, Object... args)
	throws ErrorException {
		final StringBuilder sb = new StringBuilder(this.getName());
		sb.append(": ");
		sb.append(String.format(fmt, args));
		throw new ErrorException(sb.toString());
	}

	/**
	 * Returns a user specific unit type. This is needed for entity types like distributions that may change the unit type
	 * that is returned at runtime.
	 * @return
	 */
	public Class<? extends Unit> getUserUnitType() {
		return DimensionlessUnit.class;
	}


	public final OutputHandle getOutputHandle(String outputName) {
		if (hasAttribute(outputName))
			return attributeMap.get(outputName);

		if (customOutputMap.containsKey(outputName))
			return customOutputMap.get(outputName);

		if (hasOutput(outputName)) {
			OutputHandle ret = new OutputHandle(this, outputName);
			if (ret.getUnitType() == UserSpecifiedUnit.class)
				ret.setUnitType(getUserUnitType());

			return ret;
		}

		return null;
	}

	/**
	 * Optimized version of getOutputHandle() for output names that are known to be interned
	 * @param outputName
	 * @return
	 */
	public final OutputHandle getOutputHandleInterned(String outputName) {
		if (hasAttribute(outputName))
			return attributeMap.get(outputName);

		if (customOutputMap.containsKey(outputName))
			return customOutputMap.get(outputName);

		if (OutputHandle.hasOutputInterned(this.getClass(), outputName)) {
			OutputHandle ret = new OutputHandle(this, outputName);
			if (ret.getUnitType() == UserSpecifiedUnit.class)
				ret.setUnitType(getUserUnitType());

			return ret;
		}

		return null;
	}

	public boolean hasOutput(String outputName) {
		if (OutputHandle.hasOutput(this.getClass(), outputName))
			return true;
		if (attributeMap.containsKey(outputName))
			return true;
		if (customOutputMap.containsKey(outputName))
			return true;

		return false;
	}

	private static final String OUTPUT_FORMAT = "%s\t%s\t%s\t%s%n";
	private static final String LIST_OUTPUT_FORMAT = "%s\t%s[%s]\t%s\t%s%n";

	/**
	 * Writes the entry in the output report for this entity.
	 * @param file - the file in which the outputs are written
	 * @param simTime - simulation time at which the outputs are evaluated
	 */
	public void printReport(FileEntity file, double simTime) {

		// Loop through the outputs
		ArrayList<OutputHandle> handles = OutputHandle.getOutputHandleList(this);
		for (OutputHandle out : handles) {

			// Should this output appear in the report?
			if (!out.isReportable())
				continue;

			// Determine the preferred unit for this output
			Class<? extends Unit> ut = out.getUnitType();
			double factor = Unit.getDisplayedUnitFactor(ut);
			String unitString = Unit.getDisplayedUnit(ut);
			if (ut == Unit.class || ut == DimensionlessUnit.class)
				unitString = "-";

			// Numerical output
			if (out.isNumericValue()) {
				double val = out.getValueAsDouble(simTime, Double.NaN)/factor;
				file.format(OUTPUT_FORMAT,
						this.getName(), out.getName(), val, unitString);
			}

			// DoubleVector output
			else if (out.getReturnType() == DoubleVector.class) {
				DoubleVector vec = out.getValue(simTime, DoubleVector.class);
				for (int i=0; i<vec.size(); i++) {
					double val = vec.get(i);
					file.format(LIST_OUTPUT_FORMAT,
							this.getName(), out.getName(), i, val/factor, unitString);
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
								this.getName(), out.getName(), i, val/factor, unitString);
					}
					else {
						file.format(LIST_OUTPUT_FORMAT,
							this.getName(), out.getName(), i, obj, unitString);
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
								this.getName(), out.getName(), mapEntry.getKey(), val/factor, unitString);
					}
					else {
						file.format(LIST_OUTPUT_FORMAT,
								this.getName(), out.getName(), mapEntry.getKey(), obj, unitString);
					}
				}
			}

			// All other outputs
			else {
				if (ut != Unit.class && ut != DimensionlessUnit.class)
					unitString = Unit.getSIUnit(ut);  // other outputs are not converted to preferred units
				String str = out.getValue(simTime, out.getReturnType()).toString();
				file.format(OUTPUT_FORMAT,
						this.getName(), out.getName(), str, unitString);
			}
		}
	}

	/**
	 * Returns true if there are any outputs that will be printed to the output report.
	 */
	public boolean isReportable() {
		return OutputHandle.isReportable(getClass());
	}

	public String getDescription() {
		return desc.getValue();
	}

	private void addAttribute(String name, AttributeHandle h) {
		attributeMap.put(name, h);
	}

	public boolean hasAttribute(String name) {
		return attributeMap.containsKey(name);
	}

	public Class<? extends Unit> getAttributeUnitType(String name) {
		AttributeHandle h = attributeMap.get(name);
		if (h == null)
			return null;
		return h.getUnitType();
	}

	public void setAttribute(String name, double value, Class<? extends Unit> ut) {
		AttributeHandle h = attributeMap.get(name);
		if (h == null)
			this.error("Invalid attribute name: %s", name);

		if (h.getUnitType() != ut)
			this.error("Invalid unit returned by an expression. Received: %s, expected: %s",
					ut.getSimpleName(), h.getUnitType().getSimpleName(), "");

		h.setValue(value);
	}

	public ArrayList<String> getAttributeNames(){
		ArrayList<String> ret = new ArrayList<>();
		for (String name : attributeMap.keySet()) {
			ret.add(name);
		}
		return ret;
	}

	public ArrayList<String> getCustomOutputNames(){
		ArrayList<String> ret = new ArrayList<>();
		for (String name : customOutputMap.keySet()) {
			ret.add(name);
		}
		return ret;
	}


	public ObjectType getObjectType() {
		return ObjectType.getObjectTypeForClass(this.getClass());
	}

	@Output(name = "Name",
	 description = "The unique input name for this entity.",
	    sequence = 0)
	public String getNameOutput(double simTime) {
		return entityName;
	}

	@Output(name = "ObjectType",
	 description = "The class of objects that this entity belongs to.",
	    sequence = 1)
	public String getObjectTypeName(double simTime) {
		ObjectType ot = this.getObjectType();
		if (ot == null)
			return null;
		return ot.getName();
	}

	@Output(name = "SimTime",
	 description = "The present simulation time.",
	    unitType = TimeUnit.class,
	    sequence = 2)
	public double getSimTime(double simTime) {
		return simTime;
	}

}
