/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2002-2011 Ausenco Engineering Canada Inc.
 * Copyright (C) 2016-2019 JaamSim Software Inc.
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
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;

import com.jaamsim.events.Conditional;
import com.jaamsim.events.EventHandle;
import com.jaamsim.events.EventManager;
import com.jaamsim.events.ProcessTarget;
import com.jaamsim.input.AttributeDefinitionListInput;
import com.jaamsim.input.AttributeHandle;
import com.jaamsim.input.BooleanInput;
import com.jaamsim.input.ExpError;
import com.jaamsim.input.ExpParser.Expression;
import com.jaamsim.input.ExpResType;
import com.jaamsim.input.ExpResult;
import com.jaamsim.input.ExpValResult;
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
	private final JaamSimModel simModel;

	String entityName;
	private final long entityNumber;

	// Package private so it can be accessed by JaamSimModel and EntityListNode
	EntityListNode listNode;

	private static final int FLAG_TRACE = 0x01;
	//public static final int FLAG_TRACEREQUIRED = 0x02;
	//public static final int FLAG_TRACESTATE = 0x04;
	//public static final int FLAG_LOCKED = 0x08;
	//public static final int FLAG_TRACKEVENTS = 0x10;
	public static final int FLAG_ADDED = 0x20;  // entity was defined after the 'RecordEdits' flag
	public static final int FLAG_EDITED = 0x40;  // one or more inputs were modified after the 'RecordEdits' flag
	public static final int FLAG_GENERATED = 0x80;  // entity was created during the execution of the simulation
	public static final int FLAG_DEAD = 0x0100;  // entity has been deleted
	public static final int FLAG_REGISTERED = 0x0200;  // entity is included in the namedEntities HashMap
	public static final int FLAG_RETAINED = 0x0400;  // entity is retained when the model is reset between runs
	private int flags;

	Entity parent;

	private final ArrayList<Input<?>> inpList = new ArrayList<>();

	private final HashMap<String, AttributeHandle> attributeMap = new LinkedHashMap<>();
	private final HashMap<String, ExpressionHandle> customOutputMap = new LinkedHashMap<>();

	public static final String KEY_INPUTS = "Key Inputs";
	public static final String OPTIONS = "Options";
	public static final String GRAPHICS = "Graphics";
	public static final String THRESHOLDS = "Thresholds";
	public static final String MAINTENANCE = "Maintenance";
	public static final String FONT = "Font";
	public static final String FORMAT = "Format";
	public static final String GUI = "GUI";
	public static final String MULTIPLE_RUNS = "Multiple Runs";

	@Keyword(description = "A free form string describing the Entity",
	         exampleList = {"'A very useful entity'"})
	protected final StringInput desc;

	@Keyword(description = "Provides the programmer with a detailed trace of the logic executed "
	                     + "by the entity. Trace information is sent to standard out.",
	         exampleList = {"TRUE"})
	protected final BooleanInput trace;

	@Keyword(description = "If TRUE, the object is used in the simulation run.",
	         exampleList = {"FALSE"})
	protected final BooleanInput active;

	@Keyword(description = "Defines one or more attributes for this entity. "
	                     + "An attribute's value can be a number with or without units, "
	                     + "an entity, a string, an array, a map, or a lambda function. "
	                     + "The initial value set by the definition can only be changed by an "
	                     + "Assign object.",
	         exampleList = {"{ AAA 1 }  { bbb 2[s] }  { c '\"abc\"' }  { d [Queue1] }",
	                        "{ e '{1,2,3}' }  { f '|x|(2*x)' }"})
	public final AttributeDefinitionListInput attributeDefinitionList;

	@Keyword(description = "Defines one or more custom outputs for this entity. "
	                     + "A custom output can return a number with or without units, "
	                     + "an entity, a string, an array, a map, or a lambda function. "
	                     + "The present value of a custom output is calculated on demand by the "
	                     + "model.",
	         exampleList = {"{ TwiceSimTime '2*this.SimTime' TimeUnit }  { SimTimeInDays 'this.SimTime/1[d]' }",
	                        "{ FirstEnt 'size([Queue1].QueueList)>0 ? [Queue1].QueueList(1) : [SimEntity1]' }"})
	public final NamedExpressionListInput namedExpressionInput;

	{
		desc = new StringInput("Description", KEY_INPUTS, "");
		this.addInput(desc);

		trace = new BooleanInput("Trace", OPTIONS, false);
		trace.setHidden(true);
		this.addInput(trace);

		active = new BooleanInput("Active", OPTIONS, true);
		active.setHidden(true);
		this.addInput(active);

		attributeDefinitionList = new AttributeDefinitionListInput("AttributeDefinitionList",
				OPTIONS, new ArrayList<AttributeHandle>());
		attributeDefinitionList.setHidden(false);
		this.addInput(attributeDefinitionList);

		namedExpressionInput = new NamedExpressionListInput("CustomOutputList",
				OPTIONS, new ArrayList<NamedExpression>());
		namedExpressionInput.setHidden(false);
		this.addInput(namedExpressionInput);

	}

	/**
	 * Constructor for entity initializing members.
	 */
	public Entity() {
		simModel = JaamSimModel.getCreateModel();
		entityNumber = simModel.getNextEntityID();
		flags = 0;
	}

	/**
	 * Performs any initialization that must occur after the constructor has finished.
	 */
	public void postDefine() {}

	public JaamSimModel getJaamSimModel() {
		return simModel;
	}

	public Simulation getSimulation() {
		return simModel.getSimulation();
	}

	/**
	 * Performs any additional actions that are required after a new configuration file has been
	 * loaded. Performed prior to validation.
	 */
	public void postLoad() {}

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
		if (testFlag(Entity.FLAG_DEAD))
			return;
		simModel.removeInstance(this);
	}

	/**
	 * Reverses the actions taken by the kill method.
	 * @param name - entity's name before it was deleted
	 */
	public void restore(String name) {
		simModel.restoreInstance(this);
		this.setName(name);
		this.clearFlag(Entity.FLAG_DEAD);
	}

	/**
	 * Returns whether the entity can participate in the simulation.
	 * @return true if the entity can be used
	 */
	public boolean isActive() {
		return active.getValue();
	}

	/**
	 * Performs any actions that are required at the end of the simulation run, e.g. to create an output report.
	 */
	public void doEnd() {}

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

	protected void removeInput(Input<?> in) {
		inpList.remove(in);
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
			String key = sourceInput.getKeyword();
			Input<?> targetInput = this.getInput(key);
			if (sourceInput.isDefault() || sourceInput.isSynonym() || targetInput == null) {
				continue;
			}
			tmp.clear();
			sourceInput.getValueTokens(tmp);
			KeywordIndex kw = new KeywordIndex(key, tmp, null);
			InputAgent.apply(this, targetInput, kw);
		}
	}

	/**
	 * Copies the input values from one entity to another. This method is significantly faster
	 * than copying and re-parsing the input data.
	 * @param ent - entity whose inputs are to be copied.
	 * @param target - entity whose inputs are to be assigned.
	 */
	public static void fastCopyInputs(Entity ent, Entity target) {
		// Loop through the original entity's inputs
		ArrayList<Input<?>> orig = ent.getEditableInputs();
		for (int i = 0; i < orig.size(); i++) {
			Input<?> sourceInput = orig.get(i);

			// Default values do not need to be copied
			if (sourceInput.isDefault() || sourceInput.isSynonym())
				continue;

			// Get the matching input for the new entity
			Input<?> targetInput = target.getEditableInputs().get(i);

			// Assign the value to the copied entity's input
			targetInput.copyFrom(target, sourceInput);

			// Further processing related to this input
			target.updateForInput(targetInput);
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

	public final void setTraceFlag() {
		this.setFlag(FLAG_TRACE);
	}

	public final void clearTraceFlag() {
		this.clearFlag(FLAG_TRACE);
	}

	public final boolean isTraceFlag() {
		return this.testFlag(FLAG_TRACE);
	}

	/**
	 * Method to return the name of the entity.
	 * This returns the "absolute" name for entities that are the child of other entities.
	 * Use getLocalName() for the name relative to this entity's parent
	 */
	public final String getName() {
		if (!this.testFlag(FLAG_REGISTERED) || parent == null) {
			return entityName;
		}

		// Build up the name based on the chain of parents
		ArrayList<String> revNames = new ArrayList<>();
		revNames.add(entityName);
		Entity curEnt = this.getParent();
		JaamSimModel model = getJaamSimModel();
		while(curEnt != model.getSimulation()) {
			revNames.add(curEnt.entityName);
			curEnt = curEnt.getParent();
		}

		// Build up the name back to front
		StringBuilder sb = new StringBuilder();
		Collections.reverse(revNames);
		for (int i = 0; i < revNames.size(); ++i) {
			sb.append(revNames.get(i));
			if (i < revNames.size()-1) {
				sb.append('.');
			}
		}
		return sb.toString();
	}

	public final String getLocalName() {
		return entityName;
	}

	/**
	 * Add a child to this entity, should only be called from JaamSimModel
	 * @param child
	 */
	public void addChild(Entity child) {
		error("Entity [%s] may not have children", getName());
	}

	public void removeChild(Entity child) {
		error("Entity [%s] may not have children", getName());
	}

	/**
	 * Called when an entity which is a child of this entity has been renamed
	 * Note: Should only be called from JaamSimModel.renameEntity()
	 * @param e
	 * @param oldName
	 * @param newName
	 */
	public void renameChild(Entity e, String oldName, String newName) {
		error("Entity [%s] may not have children", getName());
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

	/**
	 * Method to set the local name of the entity.
	 */
	public void setName(String newName) {
		simModel.renameEntity(this, newName);
	}

	/**
	 * Returns the parent entity for this entity
	 */
	public Entity getParent() {
		if (parent != null)
			return parent;
		return simModel.getSimulation();
	}

	/**
	 * Gets a named child from this entity.
	 * Default behaviour always returns null, only specific entities may have children
	 * @param name - the local name of the child, implementers must split the name on '.' characters and recursively call getChild()
	 * @return the descendant named or null if no such entity exists
	 */
	public Entity getChild(String name) {
		return null;
	}

	/**
	 * Returns the named child entities for this entity.
	 * @return array of child entities
	 */
	public ArrayList<Entity> getChildren() {
		return new ArrayList<>();
	}

	public int getSubModelLevel() {
		int ret = 0;
		Entity ent = parent;
		while (ent != null) {
			ret++;
			ent = ent.parent;
		}
		return ret;
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
			return;
		}
		if (in == namedExpressionInput) {
			customOutputMap.clear();
			for (NamedExpression ne : namedExpressionInput.getValue()) {
				addCustomOutput(ne.getName(), ne.getExpression(), ne.getUnitType());
			}
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
	 * Prints a trace statement for the given subroutine.
	 * The entity name is included in the output.
	 * @param indent - number of tabs with which to indent the text
	 * @param fmt - format string for the trace data (include the method name)
	 * @param args - trace data
	 */
	public void trace(int indent, String fmt, Object... args) {
		InputAgent.trace(simModel, indent, this, fmt, args);
	}

	/**
	 * Prints an additional line of trace info.
	 * The entity name is NOT included in the output
	 * @param indent - number of tabs with which to indent the text
	 * @param fmt - format string for the trace data
	 * @param args - trace data
	 */
	public void traceLine(int indent, String fmt, Object... args) {
		InputAgent.trace(simModel, indent, null, fmt, args);
	}

	/**
	 * Throws an ErrorException for this entity with the specified message.
	 * @param fmt - format string for the error message
	 * @param args - objects used by the format string
	 * @throws ErrorException
	 */
	public void error(String fmt, Object... args)
	throws ErrorException {
		if (fmt == null)
			throw new ErrorException(this, "null");

		throw new ErrorException(this, String.format(fmt, args));
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

	public void addCustomOutput(String name, Expression exp, Class<? extends Unit> unitType) {
		ExpressionHandle eh = new ExpressionHandle(this, exp, name);
		eh.setUnitType(unitType);
		customOutputMap.put(name, eh);
	}

	public void removeCustomOutput(String name) {
		customOutputMap.remove(name);
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

	// Utility function to help set attribute values for nested indices
	private ExpResult setAttribIndices(ExpResult.Collection coll, ExpResult[] indices, int indNum, ExpResult value) throws ExpError {
		assert(indNum < indices.length);
		ExpResType indType = indices[indNum].type;
		if (indType != ExpResType.NUMBER && indType != ExpResType.STRING) {
			this.error("Assigning to attributes must have numeric or string indices. Index #%d is %s",
			           indNum, ExpValResult.typeString(indices[indNum].type));
		}
		if (indNum == indices.length-1) {
			// Last index, assign the value
			ExpResult.Collection newCol = coll.assign(indices[indNum], value.getCopy());
			return ExpResult.makeCollectionResult(newCol);
		}
		// Otherwise, recurse one level deeper
		ExpResult nestedColl = coll.index(indices[indNum]);
		if (nestedColl.type != ExpResType.COLLECTION)
		{
			this.error("Assigning to value that is not a collection. Value is a %s", ExpValResult.typeString(nestedColl.type));
		}
		ExpResult recurseRes = setAttribIndices(nestedColl.colVal, indices, indNum+1, value);
		ExpResult.Collection newCol = coll.assign(indices[indNum], recurseRes);
		return ExpResult.makeCollectionResult(newCol);
	}

	public void setAttribute(String name, ExpResult[] indices, ExpResult value) throws ExpError {
		AttributeHandle h = attributeMap.get(name);
		if (h == null)
			throw new ExpError(null, -1, "Invalid attribute name for %s: %s", this, name);

		ExpResult assignValue = null;
		if (indices != null) {
			ExpResult attribValue = h.getValue(getSimTime(), ExpResult.class);
			if (attribValue.type != ExpResType.COLLECTION) {
				throw new ExpError(null, -1, "Trying to set %s attribute: %s with an index, "
						+ "but it is not a collection", this, name);
			}

			try {
				assignValue = setAttribIndices(attribValue.colVal, indices, 0, value);

			} catch (ExpError err) {
				throw new ExpError(err.source, err.pos, "Error during assignment to %s: %s",
						this, err.getMessage());
			}
		} else {
			assignValue = value.getCopy();
		}

		if (value.type == ExpResType.NUMBER && h.getUnitType() != value.unitType)
			throw new ExpError(null, -1, "Invalid unit returned by an expression. Received: %s, expected: %s",
					value.unitType.getSimpleName(), h.getUnitType().getSimpleName());

		h.setValue(assignValue);
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
		return simModel.getObjectTypeForClass(this.getClass());
	}

	@Output(name = "Name",
	 description = "The unique input name for this entity.",
	    sequence = 0)
	public final String getNameOutput(double simTime) {
		return getName();
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

	@Output(name = "Parent",
	 description = "The parent entity of this entity.",
	    sequence = 3)
	public Entity getParentOutput(double simTime) {
		return getParent();
	}

}
