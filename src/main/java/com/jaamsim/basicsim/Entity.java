/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2002-2011 Ausenco Engineering Canada Inc.
 * Copyright (C) 2016-2026 JaamSim Software Inc.
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

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;

import com.jaamsim.Graphics.EntityLabel;
import com.jaamsim.events.EventManager;
import com.jaamsim.input.AttributeDefinitionListInput;
import com.jaamsim.input.AttributeHandle;
import com.jaamsim.input.BooleanInput;
import com.jaamsim.input.EntityNameInput;
import com.jaamsim.input.ParentEntityInput;
import com.jaamsim.input.ExpError;
import com.jaamsim.input.ExpEvaluator;
import com.jaamsim.input.ExpResType;
import com.jaamsim.input.ExpResult;
import com.jaamsim.input.ExpValResult;
import com.jaamsim.input.ExpressionHandle;
import com.jaamsim.input.InOutHandle;
import com.jaamsim.input.Input;
import com.jaamsim.input.InputAgent;
import com.jaamsim.input.InputCallback;
import com.jaamsim.input.InputErrorException;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.KeywordIndex;
import com.jaamsim.input.NamedExpression;
import com.jaamsim.input.NamedExpressionListInput;
import com.jaamsim.input.Output;
import com.jaamsim.input.OutputHandle;
import com.jaamsim.input.ParseContext;
import com.jaamsim.input.StringInput;
import com.jaamsim.input.SynonymInput;
import com.jaamsim.input.ValueHandle;
import com.jaamsim.units.DimensionlessUnit;
import com.jaamsim.units.TimeUnit;
import com.jaamsim.units.Unit;

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
	static final int FLAG_ADDED = 0x20;  // entity was defined after the 'RecordEdits' flag
	static final int FLAG_EDITED = 0x40;  // one or more inputs were modified after the 'RecordEdits' flag
	static final int FLAG_GENERATED = 0x80;  // entity was created during the execution of the simulation
	static final int FLAG_DEAD = 0x0100;  // entity has been deleted
	static final int FLAG_REGISTERED = 0x0200;  // entity is included in the namedEntities HashMap
	static final int FLAG_RETAINED = 0x0400;  // entity is retained when the model is reset between runs
	static final int FLAG_POOLED = 0x0800;  // entity is held in its prototype's clone pool
	private int flags;

	Entity parent;

	Entity prototype;
	ArrayList<Entity> cloneList;  // registered clones
	ArrayList<Entity> clonePool;  // generated clones available for re-use
	private static final int MAX_POOL = 100;

	private final ArrayList<Input<?>> inpList = new ArrayList<>();

	private HashMap<String, ValueHandle> userOutputMap;

	// Input categories
	public static final String KEY_INPUTS = "Key Inputs";
	public static final String OPTIONS = "Options";
	public static final String GRAPHICS = "Graphics";
	public static final String THRESHOLDS = "Thresholds";
	public static final String MAINTENANCE = "Maintenance";
	public static final String FONT = "Font";
	public static final String FORMAT = "Format";
	public static final String GUI = "GUI";
	public static final String MULTIPLE_RUNS = "Multiple Runs";

	// Future event priorities
	public static final int PRI_HIGHEST = 0;
	public static final int PRI_HIGHER = 1;
	public static final int PRI_HIGH = 2;
	public static final int PRI_NORMAL = 5;
	public static final int PRI_MED_LOW = 7;
	public static final int PRI_LOW = 10;
	public static final int PRI_LOWER = 11;
	public static final int PRI_LOWEST = 99;

	// Future event insertion rules
	public static final boolean EVT_FIFO = true;
	public static final boolean EVT_LIFO = false;

	@Keyword(description = "Local name for the entity.",
	         exampleList = {"Conveyor1"})
	protected final EntityNameInput nameInput;

	@Keyword(description = "Parent entity for this entity.",
	         exampleList = {"SimEntity1"})
	protected final ParentEntityInput parentInput;

	@Keyword(description = "A free-form string describing the object.",
	         exampleList = {"'A very useful entity'"})
	protected final StringInput desc;

	@Keyword(description = "Provides the programmer with a detailed trace of the logic executed "
	                     + "by the entity. Trace information is sent to standard out.")
	protected final BooleanInput trace;

	@Keyword(description = "If TRUE, the object is used in the simulation run.")
	protected final BooleanInput active;

	@Keyword(description = "Defines one or more attributes for this entity. "
	                     + "An attribute's value can be a number with or without units, "
	                     + "an entity, a string, an array, a hashmap, or a lambda function. "
	                     + "The initial value set by the definition can only be changed by an "
	                     + "Assign object.")
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
		nameInput = new EntityNameInput("Name", KEY_INPUTS, "");
		nameInput.setCallback(nameInputCallback);
		nameInput.setOutput(false);
		this.addInput(nameInput);

		parentInput = new ParentEntityInput("Parent", KEY_INPUTS, null);
		parentInput.setCallback(parentInputCallback);
		parentInput.setOutput(false);
		this.addInput(parentInput);

		desc = new StringInput("Description", KEY_INPUTS, "");
		this.addInput(desc);

		trace = new BooleanInput("Trace", OPTIONS, false);
		trace.setCallback(traceInputCallback);
		trace.setHidden(true);
		this.addInput(trace);

		active = new BooleanInput("Active", OPTIONS, true);
		active.setHidden(true);
		this.addInput(active);

		attributeDefinitionList = new AttributeDefinitionListInput("AttributeDefinitionList",
				OPTIONS, new ArrayList<NamedExpression>());
		attributeDefinitionList.setCallback(userOutputCallback);
		attributeDefinitionList.setHidden(false);
		this.addInput(attributeDefinitionList);

		namedExpressionInput = new NamedExpressionListInput("CustomOutputList",
				OPTIONS, new ArrayList<NamedExpression>());
		namedExpressionInput.setCallback(userOutputCallback);
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

	static final InputCallback nameInputCallback = new InputCallback() {
		@Override
		public void callback(Entity ent, Input<?> inp) {
			String newName = (String) inp.getValue();
			if (newName.isEmpty() || newName.equals(ent.entityName))
				return;
			EntityLabel label = EntityLabel.getLabel(ent);
			ent.setLocalName(newName);

			// Update the entity's label
			if (label != null) {
				if (label.getParent() != ent)
					label.setLocalName(newName + "_Label");
				label.updateForTargetNameChange();
			}
		}
	};

	public void setNameInput(String localName) throws InputErrorException {
		if (nameInput.isDef()) {
			nameInput.setInitialValue(localName);
			setLocalName(localName);
			nameInput.setLocked(isGenerated());
			return;
		}
		InputAgent.applyArgs(this, nameInput.getKeyword(), localName);
	}

	public void resetNameInput() {
		nameInput.reset();
		setLocalName(nameInput.getValue());
	}

	static final InputCallback parentInputCallback = new InputCallback() {
		@Override
		public void callback(Entity ent, Input<?> inp) {
			Entity newParent = (Entity) inp.getValue();
			if (newParent == ent.parent)
				return;
			JaamSimModel simModel = ent.getJaamSimModel();
			simModel.removeNamedEntity(ent);
			ent.parent = newParent;
			simModel.addNamedEntity(ent);
		}
	};

	public void setParentInput(Entity newParent) throws InputErrorException {
		if (parentInput.isDef()) {
			parentInput.setInitialValue(newParent);
			this.parent = newParent;
			parentInput.setLocked(isGenerated());
			return;
		}
		InputAgent.applyArgs(this, parentInput.getKeyword(), newParent.getName());
	}

	public boolean isCopyOf(Entity ent) {

		// Names and classes must match
		if (ent.getClass() != getClass() || !ent.getName().equals(getName())) {
			System.out.format("Names or classes do not match: this=%s, ent=%s%n", this, ent);
			return false;
		}

		// Input strings must match
		boolean ret = true;
		for (int i = 0; i < inpList.size(); i++) {
			Input<?> in = inpList.get(i);
			if (in.isSynonym())
				continue;
			if (InputAgent.isGraphicsInput(in))  //FIXME resetGraphics clears the Position/Points inputs
				continue;
			Input<?> in1 = ent.inpList.get(i);
			if (!in.getValueTokens().equals(in1.getValueTokens())) {
				System.out.format("Inputs do not match: entity=%s, keyword=%s, in0=%s, in1=%s%n",
						ent, in.getKeyword(), in.getValueString(), in1.getValueString());
				ret = false;
			}
		}
		return ret;
	}

	/**
	 * Performs any initialization that must occur after the constructor has finished.
	 */
	public void postDefine() {

		// Add any specified inputs as outputs
		updateUserOutputMap();

		// Create any children for the new entity
		if (prototype != null) {
			boolean reg = isRegistered();
			boolean retain = isRetained();
			for (Entity child : prototype.getChildren()) {
				String name = child.getLocalName();
				Class<? extends Entity> klass = child.getClass();
				InputAgent.generateEntityWithName(simModel, klass, child, name, this, reg, retain);
			}

			// Copy the inputs for the new components
			for (int seq = 0; seq < 2; seq++) {
				for (Entity child : getChildren()) {
					child.copyInputs(child.prototype, seq);
				}
			}
		}
	}

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
			try {
				in.validate();
			}
			catch (ErrorException e) {
				e.entName = getName();
				e.keyword = in.getKeyword();
				throw e;
			}
		}

		if (!isActive() && active.getHidden() && !active.isDef())
			throw new ErrorException(
					"Setting the Active keyword to FALSE has no effect on this object");
	}

	/**
	 * Initialises the entity prior to the start of the model run.
	 * <p>
	 * This method must not depend on any other entities so that it can be
	 * called for each entity in any sequence.
	 */
	public void earlyInit() {

		// Reset the attributes to their initial values
		for (ValueHandle vh : getAllUserOutputHandles()) {
			if (!(vh instanceof AttributeHandle))
				continue;
			AttributeHandle h = (AttributeHandle) vh;
			try {
				ExpResult res = ExpEvaluator.evaluateExpression(h.getExpression(), this, 0.0d);
				h.setValue(res);
			}
			catch (ExpError e) {
				throw new ErrorException(this, attributeDefinitionList.getKeyword(), e);
			}
		}

		// Clear the clone pool
		clonePool = null;
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
		//System.out.format("%s.kill%n", this);

		// Remove the entity from the model
		if (!isDead()) {
			simModel.removeInstance(this);
			setFlag(Entity.FLAG_DEAD);
		}

		// Kill the entity's clones
		for (Entity clone : getCloneList()) {
			clone.kill();
		}

		// Clear the pool of generated clones
		clonePool = null;

		// If the entity is a clone, remove it from its prototype's list
		if (prototype != null && isRegistered())
			prototype.removeClone(this);

		// Kill the entity's children
		for (Entity ent : getChildren()) {
			ent.kill();
		}
	}

	/**
	 * Reverses the actions taken by the kill method.
	 */
	public void restore() {
		//System.out.format("%s.restore%n", this);

		// Restore the children before the parent entity
		for (Entity ent : getChildren()) {
			ent.restore();
		}

		// Restore the clones before the parent entity
		for (Entity clone : getCloneList()) {
			clone.restore();
		}

		// Restore the entity to the model
		simModel.restoreInstance(this);
		this.clearFlag(Entity.FLAG_DEAD);

		if (prototype != null && isRegistered())
			prototype.addClone(this);
	}

	/**
	 * Returns whether the entity was defined after the 'RecordEdits' flag was set.
	 * @return true if defined after RecordEdits
	 */
	public final boolean isAdded() {
		return this.testFlag(Entity.FLAG_ADDED);
	}

	/**
	 * Returns whether the entity was created during the execution of the simulation run.
	 * @return true if created during the simulation run
	 */
	public final boolean isGenerated() {
		return this.testFlag(Entity.FLAG_GENERATED);
	}

	/**
	 * Returns whether the entity is included in the 'namedEntities' HashMap and therefore can be
	 * referenced by the inputs to other entities.
	 * @return true if its name is recorded
	 */
	public final boolean isRegistered() {
		return this.testFlag(Entity.FLAG_REGISTERED);
	}

	/**
	 * Returns whether the generated entity is retained at the end of a simulation run for re-use
	 * in the next run.
	 * @return true if the entity is to be retained at the end of the simulation run
	 */
	public final boolean isRetained() {
		return this.testFlag(Entity.FLAG_RETAINED);
	}

	/**
	 * Returns whether all references to the entity have been removed by the 'kill' method.
	 * @return true if the entity has been killed
	 */
	public final boolean isDead() {
		return this.testFlag(Entity.FLAG_DEAD);
	}

	/**
	 * Returns whether one or more inputs were modified after the 'RecordEdits' flag was set.
	 * @return true if edited after RecordEdits
	 */
	public final boolean isEdited() {
		return this.testFlag(Entity.FLAG_EDITED);
	}

	public final void setEdited() {
		this.setFlag(Entity.FLAG_EDITED);
	}

	/**
	 * Returns whether the entity is being held its prototype's clone pool, ready for re-use
	 * @return true if the entity is pooled for re-use
	 */
	public final boolean isPooled() {
		return this.testFlag(Entity.FLAG_POOLED) || (parent != null && parent.isPooled());
	}

	/**
	 * Returns whether the entity was created by the 'autoload' file.
	 * @return true if created by autoload
	 */
	public final boolean isPreDefined() {
		return simModel.isPreDefinedEntity(this);
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
	 * Performs any actions that are required when a model is closed prior to its scheduled end
	 * time. For example, an entity may need to close a file that it opened.
	 */
	public void close() {}

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

	protected void clearInputs() {
		inpList.clear();
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
	 * Copy the inputs for each keyword to the caller.
	 * @param ent = entity whose inputs are to be copied
	 */
	public void copyInputs(Entity ent) {
		ParseContext context = null;
		if (simModel.getConfigFile() != null) {
			URI uri = simModel.getConfigFile().getParentFile().toURI();
			context = new ParseContext(uri, null);
		}
		for (int seq = 0; seq < 2; seq++) {
			copyInputs(ent, seq, context);
		}
	}

	public void copyInputs(Entity ent, int seq) {
		ParseContext context = null;
		if (simModel.getConfigFile() != null) {
			URI uri = simModel.getConfigFile().getParentFile().toURI();
			context = new ParseContext(uri, null);
		}
		copyInputs(ent, seq, context);
	}

	/**
	 * Copy the inputs for the keywords with the specified sequence number to the caller.
	 * @param ent = entity whose inputs are to be copied
	 * @param seq = sequence number for the keyword (0 = early keyword, 1 = normal keyword)
	 */
	public void copyInputs(Entity ent, int seq, ParseContext context) {

		// Provide stub definitions for the custom outputs
		if (seq == 0) {
			for (Input<?> in : ent.getEditableInputs()) {
				String stub = in.getStubDefinition();
				if (stub == null || in.isDef())
					continue;
				KeywordIndex kw = InputAgent.formatInput(in.getKeyword(), stub);
				InputAgent.apply(this, kw);
			}
		}

		// Apply the inputs based on the source entity
		for (Input<?> sourceInput : ent.getEditableInputs()) {
			if (sourceInput.isSynonym() || sourceInput.getSequenceNumber() != seq
					|| sourceInput instanceof EntityNameInput)
				continue;
			String key = sourceInput.getKeyword();
			try {
				copyInput(ent, key, context);
			}
			catch (Throwable t) {
				Log.logException(t);
			}
		}
	}

	/**
	 * Copy the input with the specified keyword from the specified entity to the caller.
	 * @param ent - entity whose input is to be copied
	 * @param key - keyword for the input to be copied
	 * @param context - specifies the file path to the folder containing the configuration file
	 * @param ignoreDef - true if a default input is not copied
	 */
	public void copyInput(Entity ent, String key, ParseContext context) {
		//boolean trace = ent.getLocalName().equals("Label") && key.equals("Name");
		//if (trace) System.out.format("%n%s.copyInput - ent=%s, key=%s%n", this, ent, key);

		Input<?> sourceInput = ent.getInput(key);
		Input<?> targetInput = this.getInput(key);
		if (sourceInput == null || targetInput == null)
			return;

		// Replace references to the parent entity
		ArrayList<String> tmp = ent.getValueTokens(sourceInput, parent);

		// An overwritten input for a clone cannot be changed by the prototype, except in the
		// following circumstances:
		// - the input was inherited from its prototype but contained an explicit reference to its
		//   parent entity
		// - the new input value from the prototype is equal to the present value (handled in the
		//   'assign' method)
		// - the input is for the CustomOutputList keyword which had been assigned a stub value
		if (getPrototype() == ent && !targetInput.isDef() && !targetInput.isInherited()
				&& !targetInput.getValueTokens().equals(tmp)
				&& targetInput.getStubDefinition() == null)
			return;

		// Set the new input value
		try {
			KeywordIndex kw = new KeywordIndex(key, tmp, context);
			InputAgent.apply(this, targetInput, kw);
		}
		catch (Exception e) {
			throw new ErrorException(this, key, e);
		}
	}

	/**
	 * Returns the array of input tokens for the specified input with any explicit references to
	 * this entity's parent entity replaced with the specified parent entity.
	 * @param in - input object
	 * @param newParent - specified parent entity
	 * @return input tokens
	 */
	public ArrayList<String> getValueTokens(Input<?> in, Entity newParent) {

		// For a blank input, check the input inherited from its prototype and replace references
		// to the prototype's parent
		if (in.isDef() && prototype != null && in.getProtoInput() != null)
			return prototype.getValueTokens(in.getProtoInput(), newParent);

		ArrayList<String> ret = in.getValueTokens();
		if (ret.isEmpty() || parent == null || newParent == null || parent == newParent)
			return ret;

		// Find the first parent that has a different name
		Entity oldP = parent;
		Entity newP = newParent;
		while (oldP.getLocalName().equals(newP.getLocalName())
				&& oldP.parent != null && newP.parent != null) {
			oldP = oldP.parent;
			newP = newP.parent;
		}

		// Replace any explicit references to the parent entity with the specified new parent
		String oldName = parent.getName();
		String oldName1 = "[" + oldName + "]";
		String oldName2 = oldP.getName() + ".";

		String newName = newParent.getName();
		String newName1 = "[" + newName + "]";
		String newName2 = newP.getName() + ".";

		for (int i = 0; i < ret.size(); i++) {
			String str = ret.get(i);
			if (str.equals(oldName))
				str = newName;
			else {
				str = str.replace(oldName1, newName1);
				str = str.replace(oldName2, newName2);
			}
			ret.set(i, str);
		}
		return ret;
	}

	public ArrayList<String> getInheritedValueTokens(Input<?> in) {
		if (prototype == null || in.getProtoInput() == null)
			return new ArrayList<>();
		return prototype.getValueTokens(in.getProtoInput(), parent);
	}

	/**
	 * Copies the present attribute values from one entity to another.
	 * @param ent - entity whose attribute values are to be copied
	 * @param target - entity whose attribute values are to be assigned
	 */
	public static void copyAttributeValues(Entity ent, Entity target) {
		for (ValueHandle sourceVHandle : ent.getAllUserOutputHandles()) {
			ValueHandle targetVHandle = target.getUserOutputHandle(sourceVHandle.getName());
			if (!(sourceVHandle instanceof AttributeHandle)
					|| !(targetVHandle instanceof AttributeHandle))
				continue;
			AttributeHandle sourceHandle = (AttributeHandle) sourceVHandle;
			AttributeHandle targetHandle = (AttributeHandle) targetVHandle;
			targetHandle.setValue(sourceHandle.copyValue());
		}
	}

	public ArrayList<Entity> getEntityReferences() {
		ArrayList<Entity> ret = new ArrayList<>();
		for (Input<?> inp : inpList) {
			inp.appendEntityReferences(ret);
		}
		return ret;
	}

	/**
	 * Returns a list of entities that are appear in the inputs to this entity and its children,
	 * grand-children, etc., but are not the entity or one of its children, grand-children, etc.
	 * or one of the entities that is defined automatically when JaamSim is launched.
	 * @return list of entities that are external references
	 */
	public ArrayList<Entity> getExternalReferences() {
		ArrayList<Entity> ret = new ArrayList<>();
		ArrayList<Entity> entityList = new ArrayList<>(getDescendants());
		entityList.add(0, this);
		for (Entity ent : entityList) {
			for (Entity reference : ent.getEntityReferences()) {
				if (reference.isPreDefined() || entityList.contains(reference)
						|| ret.contains(reference))
					continue;
				ret.add(reference);
			}
		}
		return ret;
	}

	final void setFlag(int flag) {
		flags |= flag;
	}

	final void clearFlag(int flag) {
		flags &= ~flag;
	}

	final boolean testFlag(int flag) {
		return (flags & flag) != 0;
	}

	public final void setTraceFlag() {
		this.setFlag(FLAG_TRACE);
	}

	public final void clearTraceFlag() {
		this.clearFlag(FLAG_TRACE);
	}

	public void setTraceFlag(boolean bool) {
		if (bool)
			setFlag(FLAG_TRACE);
		else
			clearFlag(FLAG_TRACE);
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
		if (parent == null) {
			return entityName;
		}
		return parent.getName() + "." + entityName;
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
	 * Get the unique number for this entity
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
	 * Sets the local name of the entity.
	 * @param newName - new local name
	 */
	public void setLocalName(String newName) {
		simModel.renameEntity(this, newName);
	}

	/**
	 * Returns the parent entity for this entity
	 */
	public Entity getParent() {
		return parent;
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

	/**
	 * Returns a list of all the children, grand-children, etc. of this entity.
	 * @return array of descendant entities
	 */
	public ArrayList<Entity> getDescendants() {
		ArrayList<Entity> ret = new ArrayList<>();
		for (Entity ent : getChildren()) {
			ret.add(ent);
			ret.addAll(ent.getDescendants());
		}
		return ret;
	}

	public int getSubModelLevel() {
		if (parent == null)
			return 0;
		return parent.getSubModelLevel() + 1;
	}

	/**
	 * Returns the number of entities that must be defined before this entity can be defined.
	 * @return number of entities that must be defined previously
	 */
	public int getDependenceLevel() {
		if (parent == null && prototype == null && !hasClone())
			return 0;
		return getDependencies().size();
	}

	/**
	 * Returns a list of entities that must be defined before this entity can be defined.
	 * @return list of entities to be defined previously
	 */
	private ArrayList<Entity> getDependencies() {
		ArrayList<Entity> ret = new ArrayList<>();
		addDependencies(ret);
		return ret;
	}

	private void addDependencies(ArrayList<Entity> list) {

		// Add the parent entity and its dependencies
		if (parent != null && !list.contains(parent)) {
			list.add(parent);
			parent.addDependencies(list);
		}

		// Add the prototype entity and its dependencies
		if (prototype != null && !list.contains(prototype)) {
			list.add(prototype);
			prototype.addDependencies(list);

			// Any children of the prototype must also be defined previously
			for (Entity ent : prototype.getChildren()) {
				if (!ent.isGenerated() && !list.contains(ent)) {
					list.add(ent);
					ent.addDependencies(list);
				}
			}
		}
	}

	static final InputCallback traceInputCallback = new InputCallback() {
		@Override
		public void callback(Entity ent, Input<?> inp) {
			BooleanInput trc = (BooleanInput)inp;
			ent.setTraceFlag(trc.getValue() && ent.isEnableTracing());
		}
	};

	public boolean isEnableTracing() {
		return getSimulation() != null && getSimulation().isEnableTracing();
	}

	public void enableTracing(boolean bool) {
		trace.setHidden(!bool);
		setTraceFlag(bool && trace.getValue());
	}

	static final InputCallback userOutputCallback = new InputCallback() {
		@Override
		public void callback(Entity ent, Input<?> inp) {
			ent.updateUserOutputMap();
		}
	};

	public void updateUserOutputMap() {
		clearUserOutputs();
		for (NamedExpression ne : attributeDefinitionList.getValue()) {
			AttributeHandle ah = new AttributeHandle(this, ne.getName(), ne.getExpression(), null, ne.getUnitType());
			addUserOutputHandle(ne.getName(), ah);
		}
		for (NamedExpression ne : namedExpressionInput.getValue()) {
			ExpressionHandle eh = new ExpressionHandle(this, ne.getExpression(), ne.getName(), ne.getUnitType());
			addUserOutputHandle(eh.getName(), eh);
		}
		for (Input<?> in : inpList) {
			if (!in.isOutput() || in.getHidden())
				continue;
			InOutHandle ioh = new InOutHandle(this, in, in.getKeyword(), in.getReturnType(), in.getUnitType());
			addUserOutputHandle(ioh.getName(), ioh);
		}
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
		simModel.trace(indent, this, fmt, args);
	}

	/**
	 * Prints an additional line of trace info.
	 * The entity name is NOT included in the output
	 * @param indent - number of tabs with which to indent the text
	 * @param fmt - format string for the trace data
	 * @param args - trace data
	 */
	public void traceLine(int indent, String fmt, Object... args) {
		simModel.trace(indent, null, fmt, args);
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
	 */
	public Class<? extends Unit> getUserUnitType() {
		return DimensionlessUnit.class;
	}

	public ValueHandle getOutputHandle(String outputName) {
		ValueHandle ret = getUserOutputHandle(outputName);
		if (ret != null)
			return ret;

		return OutputHandle.getOutputHandle(this, outputName);
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

	private void addUserOutputHandle(String name, ValueHandle vh) {
		if (userOutputMap == null)
			userOutputMap = new LinkedHashMap<>();
		userOutputMap.put(name, vh);
	}

	private ValueHandle getUserOutputHandle(String name) {
		if (userOutputMap == null)
			return null;
		return userOutputMap.get(name);
	}

	private Collection<ValueHandle> getAllUserOutputHandles() {
		if (userOutputMap == null)
			return new ArrayList<>(0);
		return userOutputMap.values();
	}

	private void clearUserOutputs() {
		userOutputMap = null;
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
		ValueHandle vh = getUserOutputHandle(name);
		if (!(vh instanceof AttributeHandle))
			throw new ExpError(null, -1, "Invalid attribute name for %s: %s", this, name);
		AttributeHandle h = (AttributeHandle) vh;

		ExpResult assignValue = null;

		// Collection Attribute
		if (indices != null) {
			ExpResult attribValue = h.getValue(ExpResult.class);
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
		}

		// Single-Valued Attribute
		else {
			if (value.type == ExpResType.NUMBER && h.getUnitType() != value.unitType) {
				throw new ExpError(null, -1, "Unit returned by the expression does not match the "
						+ "attribute. Received: %s, expected: %s",
						value.unitType.getSimpleName(), h.getUnitType().getSimpleName());
			}
			assignValue = value.getCopy();
		}

		h.setValue(assignValue);
	}

	public ArrayList<ValueHandle> getAllOutputs() {
		ArrayList<ValueHandle> ret = OutputHandle.getAllOutputHandles(this);

		// Add the attributes and custom outputs
		ret.addAll( getAllUserOutputHandles() );

		Collections.sort(ret, new ValueHandleComparator());
		return ret;
	}

	private static class ValueHandleComparator implements Comparator<ValueHandle> {

		@Override
		public int compare(ValueHandle hand0, ValueHandle hand1) {
			Class<?> class0 = hand0.getDeclaringClass();
			Class<?> class1 = hand1.getDeclaringClass();

			if (class0 == class1) {
				if (hand0.getSequence() == hand1.getSequence())
					return 0;
				else if (hand0.getSequence() < hand1.getSequence())
					return -1;
				else
					return 1;
			}

			if (class0.isAssignableFrom(class1))
				return -1;
			else
				return 1;
		}
	}

	public void setPrototype(Entity proto) {
		if (proto == prototype)
			return;
		if (prototype != null)
			throw new ErrorException("Cannot re-assign the prototype for an entity: "
					+ "old=%s, new=%s", prototype, proto);
		if (proto.getClass() != getClass())
			throw new ErrorException("An entity and its prototype must be instances of the same "
					+ "class");

		// Record the clone with its prototype
		prototype = proto;
		if (isRegistered())
			prototype.addClone(this);

		// Loop through the inputs for this entity
		for (int i = 0; i < inpList.size(); i++) {
			Input<?> in = inpList.get(i);
			if (in.getKeyword().equals("Prototype"))
				continue;

			// Set the prototype input
			in.setProtoInput(prototype.inpList.get(i));

			// If the inherited value is used, then perform its callback
			if (!in.isDef() || in.isDefault())
				continue;
			in.doCallback(this);
		}
	}

	public Entity getPrototype() {
		return prototype;
	}

	public synchronized boolean hasClone() {
		return cloneList != null && !cloneList.isEmpty();
	}

	public boolean isClone() {
		return prototype != null;
	}

	public int getCloneLevel() {
		if (prototype == null)
			return 0;
		return prototype.getCloneLevel() + 1;
	}

	private synchronized void addClone(Entity ent) {
		// If the entity is dead, then it already has a hashmap of its clones
		if (isDead())
			return;

		if (cloneList == null)
			cloneList = new ArrayList<>();
		cloneList.add(ent);
	}

	private synchronized boolean removeClone(Entity ent) {
		//System.out.format("%s.removeClone(%s) - isDead=%s, cloneList=%s%n",
		//		this, ent, isDead(), cloneList);
		// If the entity is dead, then retain its hashmap of clones
		if (isDead())
			return false;

		if (cloneList == null)
			return false;
		return cloneList.remove(ent);
	}

	private synchronized ArrayList<Entity> getCloneList() {
		if (cloneList == null)
			return new ArrayList<>();
		return new ArrayList<>(cloneList);
	}

	public ArrayList<Entity> getAllClones() {
		ArrayList<Entity> ret = getCloneList();

		// Include the generated entities that have not been registered
		if (simModel.isStarted()) {
			for (Entity ent : simModel.getClonesOfIterator(Entity.class)) {
				if (ent.getPrototype() != this || ent.isRegistered() || ent.isPooled())
					continue;
				ret.add(ent);
			}
		}
		return ret;
	}

	private void addCloneToPool(Entity clone) {
		if (clonePool == null)
			clonePool = new ArrayList<>();
		clone.setFlag(Entity.FLAG_POOLED);
		clonePool.add(clone);
	}

	public int getClonePoolSize() {
		if (clonePool == null)
			return 0;
		return clonePool.size();
	}

	public Entity getCloneFromPool() {
		if (clonePool == null || clonePool.isEmpty())
			return null;
		Entity ret = clonePool.remove(clonePool.size() - 1);
		ret.clearFlag(Entity.FLAG_POOLED);
		ret.resetNameInput();

		// Reset any inputs that were changed
		if (ret.isEdited()) {
			for (Input<?> in : ret.inpList) {
				if (in.isDef())
					continue;
				in.reset();
				in.doCallback(ret);
			}
			ret.clearFlag(Entity.FLAG_EDITED);
		}

		return ret;
	}

	/**
	 * Removes a generated entity from the model by either pooling or killing it.
	 */
	public void dispose() {
		if (!isGenerated())
			return;
		if (isClone() && prototype.getClonePoolSize() < MAX_POOL) {
			prototype.addCloneToPool(this);
			return;
		}
		kill();
	}

	/**
	 * Returns the object type for this entity.
	 * Null is returned if the entity itself is an instance of ObjectType.
	 * <p>
	 * For example, if Server1 is an instance of Server, then
	 * Server1.getObjectType() returns Server, and
	 * Server.getObjectType() returns null.
	 * @return object type for the entity
	 */
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
	public ObjectType getObjectTypeName(double simTime) {
		 return getObjectType();
	}

	@Output(name = "SimTime",
	 description = "The present simulation time.",
	    unitType = TimeUnit.class,
	    sequence = 2)
	public double getSimTime(double simTime) {
		return simTime;
	}

	@Output(name = "Parent",
	 description = "The parent entity for this entity.",
	    sequence = 3)
	public Entity getParentOutput(double simTime) {
		return getParent();
	}

	@Output(name = "Children",
	 description = "List of entities whose parent is this entity.",
	    sequence = 4)
	public ArrayList<Entity> getChildren(double simTime) {
		return getChildren();
	}

	@Output(name = "Prototype",
	 description = "The entity that provides the default inputs for this entity.",
	    sequence = 5)
	public Entity getPrototype(double simTime) {
		return getPrototype();
	}

	@Output(name = "CloneList",
	 description = "List of entities whose prototype is this entity.",
	    sequence = 6)
	public ArrayList<Entity> getCloneList(double simTime) {
		ArrayList<Entity> ret = getAllClones();
		Collections.sort(ret, InputAgent.uiEntitySortOrder);
		return ret;
	}

}
