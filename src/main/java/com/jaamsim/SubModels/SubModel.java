/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2018-2023 JaamSim Software Inc.
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
package com.jaamsim.SubModels;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map.Entry;

import com.jaamsim.Commands.KeywordCommand;
import com.jaamsim.Graphics.DisplayEntity;
import com.jaamsim.Graphics.Region;
import com.jaamsim.ProbabilityDistributions.RandomStreamUser;
import com.jaamsim.basicsim.Entity;
import com.jaamsim.basicsim.GUIListener;
import com.jaamsim.basicsim.JaamSimModel;
import com.jaamsim.input.EntityInput;
import com.jaamsim.input.ExpressionHandle;
import com.jaamsim.input.ExpressionInput;
import com.jaamsim.input.Input;
import com.jaamsim.input.InputAgent;
import com.jaamsim.input.InputCallback;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.KeywordIndex;
import com.jaamsim.input.Output;
import com.jaamsim.input.ValueHandle;
import com.jaamsim.input.ExpParser.Expression;
import com.jaamsim.ui.DragAndDropable;
import com.jaamsim.units.Unit;

public class SubModel extends CompoundEntity implements DragAndDropable {

	@Keyword(description = "The prototype sub-model from which this sub-model is cloned.",
	         exampleList = {"SubModel1"})
	protected final EntityInput<SubModel> prototypeSubModel;

	@Keyword(description = "Defines new keywords for the sub-model and creates new outputs with "
	                     + "the same names. "
	                     + "This allows the components of a sub-model to receive all their inputs "
	                     + "from either the parent sub-model or from other components.",
	         exampleList = {"{ ServiceTime TimeUnit } { NumberOfUnits }"})
	protected final PassThroughListInput keywordListInput;

	protected ArrayList<PassThroughData> keywordList;
	private ArrayList<ExpressionInput> newInputList;
	private final LinkedHashMap<String, ValueHandle> inputOutputMap = new LinkedHashMap<>();

	public static final String PALETTE_NAME = "Pre-built SubModels";

	{
		prototypeSubModel = new EntityInput<>(SubModel.class, "Prototype", KEY_INPUTS, null);
		prototypeSubModel.setHidden(true);
		prototypeSubModel.setCallback(prototypeKeywordCallback);
		this.addInput(prototypeSubModel);

		keywordListInput = new PassThroughListInput("KeywordList", OPTIONS, new ArrayList<PassThroughData>());
		keywordListInput.setCallback(keywordListKeywordCallback);
		this.addInput(keywordListInput);
	}

	public SubModel() {
		keywordList = new ArrayList<>();
		newInputList = new ArrayList<>();

		GUIListener gui = getJaamSimModel().getGUIListener();
		if (gui != null)
			gui.updateModelBuilder();
	}

	static final InputCallback prototypeKeywordCallback = new InputCallback() {
		@Override
		public void callback(Entity ent, Input<?> inp) {
			JaamSimModel sim = ent.getJaamSimModel();
			SubModelClone smc = (SubModelClone)ent;
			boolean bool = sim.isRecordEdits();
			sim.setRecordEdits(false);
			smc.createComponents();
			sim.setRecordEdits(bool);
		}
	};

	static final InputCallback keywordListKeywordCallback = new InputCallback() {
		@Override
		public void callback(Entity ent, Input<?> inp) {
			SubModel sm = (SubModel)ent;
			PassThroughListInput ptl = (PassThroughListInput)inp;

			sm.updateKeywords(ptl.getValue());
			for (SubModelClone clone : sm.getClones()) {
				clone.updateKeywords(ptl.getValue());
			}
			GUIListener gui = sm.getJaamSimModel().getGUIListener();
			if (gui != null && gui.isSelected(sm))
				gui.updateInputEditor();
		}
	};

	static final InputCallback subModelKeywordCallback = new InputCallback() {
		@Override
		public void callback(Entity ent, Input<?> inp) {
			ExpressionInput expIn = (ExpressionInput)inp;
			SubModel subModel = (SubModel) ent;
			subModel.addInputAsOutput(expIn.getKeyword(), expIn.getValue(), expIn.getUnitType());
		}
	};

	@Override
	public void setInputsForDragAndDrop() {
		super.setInputsForDragAndDrop();
		SubModel proto = getPrototypeSubModel();
		if (proto == null)
			return;

		// Set the new keywords for the clone
		updateKeywords(proto.getKeywordList());

		// Set the inputs for the clone to those for the prototype SubModel
		copyInputs(proto, true, false);

		// Create the clone's components
		update();
	}

	@Override
	public void postLoad() {
		super.postLoad();
		update();
	}

	@Override
	public void kill() {
		super.kill();
		GUIListener gui = getJaamSimModel().getGUIListener();
		if (gui != null)
			gui.updateModelBuilder();
	}

	@Override
	public void restore(String name) {
		super.restore(name);
		GUIListener gui = getJaamSimModel().getGUIListener();
		if (gui != null)
			gui.updateModelBuilder();
	}

	@Override
	public void validate() {
		// If there are clones, only the clones need to be validated
		if (hasClone())
			return;
		super.validate();
	}

	public void setKeywordList(ArrayList<PassThroughData> list) {
		keywordList = new ArrayList<>(list);
	}

	public ArrayList<PassThroughData> getKeywordList() {
		return keywordList;
	}

	/**
	 * Updates the added keywords to match the specified list.
	 * @param newDataList - data for the new list of added keywords
	 */
	void updateKeywords(ArrayList<PassThroughData> newDataList) {

		// Do nothing if the keywords are unchanged
		if (newDataList.equals(keywordList))
			return;

		// Remove the old inputs and outputs
		for (ExpressionInput in : newInputList) {
			removeInput(in);
			removeInputAsOutput(in.getKeyword());
		}

		// Add the new keywords, using the old ones whenever possible to save their input values
		ArrayList<ExpressionInput> list = new ArrayList<>(newDataList.size());
		for (PassThroughData data : newDataList) {
			int index = keywordList.indexOf(data);
			ExpressionInput in = null;
			if (index == -1) {
				in = new ExpressionInput(data.getName(), KEY_INPUTS, null);
				in.setUnitType(data.getUnitType());
				in.setCallback(subModelKeywordCallback);
				in.setRequired(true);
			}
			else {
				in = newInputList.get(index);
			}
			addInput(in);
			addInputAsOutput(in.getKeyword(), in.getValue(), in.getUnitType());
			list.add(in);
		}
		newInputList = list;
		setKeywordList(newDataList);
	}

	private void addInputAsOutput(String name, Expression exp, Class<? extends Unit> unitType) {
		ExpressionHandle eh = new ExpressionHandle(this, exp, name, unitType);
		inputOutputMap.put(name, eh);
	}

	private void removeInputAsOutput(String name) {
		inputOutputMap.remove(name);
	}

	@Override
	public ValueHandle getOutputHandle(String outputName) {
		ValueHandle ret = inputOutputMap.get(outputName);
		if (ret != null)
			return ret;

		return super.getOutputHandle(outputName);
	}

	@Override
	public ArrayList<ValueHandle> getAllOutputs() {
		ArrayList<ValueHandle> ret = super.getAllOutputs();

		// Add the Inputs as Outputs
		for (Entry<String, ValueHandle> e : inputOutputMap.entrySet()) {
			ret.add(e.getValue());
		}

		return ret;
	}


	public void updateClones() {
		for (SubModelClone clone : getClones()) {
			clone.update();
		}
	}

	/**
	 * Returns the clones that were made from this prototype sub-model.
	 * @return list of clones of this prototype.
	 */
	public ArrayList<SubModelClone> getClones() {
		ArrayList<SubModelClone> ret = new ArrayList<>();
		JaamSimModel simModel = getJaamSimModel();
		for (SubModelClone clone : simModel.getClonesOfIterator(SubModelClone.class)) {
			if (clone.isClone(this)) {
				ret.add(clone);
			}
		}
		return ret;
	}

	public boolean hasClone() {
		JaamSimModel simModel = getJaamSimModel();
		for (SubModelClone clone : simModel.getClonesOfIterator(SubModelClone.class)) {
			if (clone.isClone(this)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Returns whether this sub-model is a clone of the specified prototype.
	 * @param proto - prototype sub-model.
	 * @return true is this sub-model is a clone of the specified prototype.
	 */
	public boolean isClone(SubModel proto) {
		return getPrototypeSubModel() == proto;
	}

	/**
	 * Adjusts the clone to match the present setting for its prototype sub-model.
	 */
	public void update() {
		SubModel proto = getPrototypeSubModel();
		if (proto == null)
			return;

		// Do not record the components and their inputs to be 'edited'
		boolean bool = getJaamSimModel().isRecordEdits();
		getJaamSimModel().setRecordEdits(false);

		// Update the AttributeDefinitionList input
		String oldStr = attributeDefinitionList.getValueString();
		String key = attributeDefinitionList.getKeyword();
		String newStr = proto.getInput(key).getValueString();
		if (!newStr.equals(oldStr)) {
			KeywordIndex kw = InputAgent.formatInput(key, newStr);
			InputAgent.storeAndExecute(new KeywordCommand(this, kw));
		}

		// Update the components
		createComponents();
		setComponentInputs();

		// Reset the record edits state
		getJaamSimModel().setRecordEdits(bool);
	}

	/**
	 * Creates this sub-model's components and sets their inputs to match its prototype's components.
	 * @param protoCompList - components for the prototype
	 */
	protected void createComponents() {
		SubModel proto = getPrototypeSubModel();
		//System.out.format("%s.createComponents - protoComp=%s%n", this, protoCompList);

		// Delete any components that are not in the prototype
		for (Entity comp : getChildren()) {
			if (proto == null || proto.getChild(comp.getLocalName()) == null)
				comp.kill();
		}

		if (proto == null)
			return;

		// Create the new components
		for (Entity protoComp : proto.getChildren()) {
			String name = protoComp.getLocalName();
			if (getChild(name) != null)
				continue;
			Entity comp = InputAgent.generateEntityWithName(getJaamSimModel(),
					protoComp.getClass(), protoComp, name, this, true, true);
			//System.out.format("protoComp=%s, comp=%s%n", protoComp, comp);
			if (comp instanceof DisplayEntity && !(comp instanceof Region)) {
				InputAgent.applyArgs(comp, "Region", proto.getSubModelRegion().getName());
				comp.getInput("Region").setLocked(true);
			}
		}
	}

	protected void setComponentInputs() {
		SubModel proto = getPrototypeSubModel();
		if (proto == null)
			return;

		// Save the seeds for the components that use a random distribution
		LinkedHashMap<Entity, Integer> seedMap = new LinkedHashMap<>();
		for (Entity comp : getChildren()) {
			if (!(comp instanceof RandomStreamUser))
				continue;
			seedMap.put(comp, ((RandomStreamUser) comp).getStreamNumber());
		}
		//System.out.println(seedMap);

		// Set the early and normal keywords for each component
		for (int seq = 0; seq < 2; seq++) {
			for (Entity protoComp : proto.getChildren()) {
				String localName = protoComp.getLocalName();
				Entity comp = getChild(localName);
				comp.copyInputs(protoComp, seq, true, true);
			}
		}

		// Reset the stream number inputs for the random distributions to the saved values
		for (Entry<Entity, Integer> entry : seedMap.entrySet()) {
			Entity comp = entry.getKey();
			Entity protoComp = proto.getChild(comp.getLocalName());

			// Assign a new seed if it is the same as the seed for the parent component
			int seed = entry.getValue().intValue();
			if (seed == -1 || seed == ((RandomStreamUser) protoComp).getStreamNumber())
				seed = getJaamSimModel().getSmallestAvailableStreamNumber();

			//System.out.format("comp=%s, seed=%s%n", comp, seed);
			String key = ((RandomStreamUser) comp).getStreamNumberKeyword();
			InputAgent.applyIntegers(comp, key, seed);
			comp.getInput(key).setLocked(true);

			// Mark the seed input and clone as edited so that the seed value is saved in the
			// configuration file
			comp.getInput(key).setEdited(true);
			comp.setEdited();
		}

		// Set the "Show" input for each component
		boolean bool = getJaamSimModel().getSimulation().isShowSubModels();
		showTemporaryComponents(bool);
	}

	public SubModel getPrototypeSubModel() {
		return prototypeSubModel.getValue();
	}

	@Override
	public Class<? extends Entity> getJavaClass() {
		return SubModelClone.class;
	}

	@Override
	public Entity getPrototypeForDragAndDrop() {
		return this;
	}

	@Override
	public boolean isDragAndDrop() {
		return true;
	}

	@Override
	public String getPaletteName() {
		return PALETTE_NAME;
	}

	@Override
	public BufferedImage getIconImage() {
		return getObjectType().getIconImage();
	}

	@Output(name = "PrototypeSubModel",
	 description = "The prototype SubModel from which this sub-model was cloned.",
	    sequence = 0)
	public SubModel getPrototypeSubModel(double simTime) {
		return getPrototypeSubModel();
	}

}
