/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2018-2021 JaamSim Software Inc.
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

import java.util.LinkedHashMap;
import java.util.Map.Entry;

import com.jaamsim.Commands.KeywordCommand;
import com.jaamsim.Graphics.DisplayEntity;
import com.jaamsim.Graphics.Region;
import com.jaamsim.ProbabilityDistributions.RandomStreamUser;
import com.jaamsim.basicsim.Entity;
import com.jaamsim.input.EntityInput;
import com.jaamsim.input.Input;
import com.jaamsim.input.InputAgent;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.KeywordIndex;
import com.jaamsim.input.Output;

public class SubModelClone extends AbstractSubModel {

	@Keyword(description = "The prototype sub-model from which this sub-model is cloned.",
	         exampleList = {"SubModel1"})
	protected final EntityInput<SubModel> prototype;

	{
		prototype = new EntityInput<>(SubModel.class, "Prototype", KEY_INPUTS, null);
		prototype.setHidden(true);
		this.addInput(prototype);
	}

	public SubModelClone() {}

	@Override
	public void setInputsForDragAndDrop() {
		super.setInputsForDragAndDrop();
		SubModel proto = prototype.getValue();

		// Set the new keywords for the clone
		updateKeywords(proto.getKeywordList());

		// Set the inputs for the clone to those for the prototype SubModel
		copyInputs(proto);

		// Create the clone's components
		update();
	}

	@Override
	public void postLoad() {
		super.postLoad();
		update();
	}

	@Override
	public void updateForInput(Input<?> in) {
		super.updateForInput(in);

		if (in == prototype) {
			boolean bool = getJaamSimModel().isRecordEdits();
			getJaamSimModel().setRecordEdits(false);
			createComponents();
			getJaamSimModel().setRecordEdits(bool);
			return;
		}
	}

	/**
	 * Returns whether this sub-model is a clone of the specified prototype.
	 * @param proto - prototype sub-model.
	 * @return true is this sub-model is a clone of the specified prototype.
	 */
	public boolean isClone(SubModel proto) {
		return prototype.getValue() == proto;
	}

	/**
	 * Adjusts the clone to match the present setting for its prototype sub-model.
	 */
	public void update() {
		SubModel proto = prototype.getValue();

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
		AbstractSubModel proto = prototype.getValue();
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
			Entity comp = InputAgent.generateEntityWithName(getJaamSimModel(), protoComp.getClass(), name, this, true, true);
			//System.out.format("protoComp=%s, comp=%s%n", protoComp, comp);
			if (comp instanceof DisplayEntity && !(comp instanceof Region)) {
				InputAgent.applyArgs(comp, "Region", proto.getSubModelRegion().getName());
				comp.getInput("Region").setLocked(true);
			}
		}
	}

	protected void setComponentInputs() {
		AbstractSubModel proto = prototype.getValue();

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
				comp.copyInputs(protoComp, seq, true);
			}
		}

		// Reset the stream number inputs for the random distributions to the saved values
		for (Entry<Entity, Integer> entry : seedMap.entrySet()) {
			Entity comp = entry.getKey();
			Entity protoComp = proto.getChild(comp.getLocalName());

			// Assign a new seed if it is the same as the seed for the parent component
			int seed = entry.getValue().intValue();
			if (seed == -1 || seed == ((RandomStreamUser) protoComp).getStreamNumber())
				seed = getSimulation().getLargestStreamNumber() + 1;

			//System.out.format("comp=%s, seed=%s%n", comp, seed);
			String key = ((RandomStreamUser) comp).getStreamNumberKeyword();
			InputAgent.applyIntegers(comp, key, seed);
			comp.getInput(key).setLocked(true);
		}

		// Set the "Show" input for each component
		boolean bool = getJaamSimModel().getSimulation().isShowSubModels();
		showTemporaryComponents(bool);
	}

	@Output(name = "Prototype",
	 description = "The prototype SubModel from which this sub-model was cloned.",
	    sequence = 0)
	public SubModel getPrototype(double simTime) {
		return prototype.getValue();
	}

}
