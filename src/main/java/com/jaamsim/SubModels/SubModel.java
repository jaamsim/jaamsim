/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2018-2024 JaamSim Software Inc.
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

import com.jaamsim.basicsim.Entity;
import com.jaamsim.basicsim.GUIListener;
import com.jaamsim.input.EntityInput;
import com.jaamsim.input.ExpressionHandle;
import com.jaamsim.input.ExpressionInput;
import com.jaamsim.input.Input;
import com.jaamsim.input.InputAgent;
import com.jaamsim.input.InputCallback;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.ValueHandle;
import com.jaamsim.input.ExpParser.Expression;
import com.jaamsim.ui.DragAndDropable;
import com.jaamsim.units.Unit;

public class SubModel extends CompoundEntity implements DragAndDropable {

	@Keyword(description = "The prototype sub-model from which this sub-model is cloned.")
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
			// Set the prototype property and clear the Prototype input so that it is not saved
			SubModel proto = (SubModel) inp.getValue();
			if (proto != null)
				ent.setPrototype(proto);
			inp.reset();
		}
	};

	static final InputCallback keywordListKeywordCallback = new InputCallback() {
		@Override
		public void callback(Entity ent, Input<?> inp) {
			SubModel sm = (SubModel)ent;
			sm.updateKeywords();
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
	public void setPrototype(Entity proto) {
		super.setPrototype(proto);
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
	public void restore() {
		super.restore();
		GUIListener gui = getJaamSimModel().getGUIListener();
		if (gui != null)
			gui.updateModelBuilder();
	}

	/**
	 * Updates the added keywords to match the specified list.
	 * @param newDataList - data for the new list of added keywords
	 */
	void updateKeywords() {
		ArrayList<PassThroughData> newDataList = keywordListInput.getValue();

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
				in.setValid(true);
				in.setCallback(subModelKeywordCallback);
				in.setRequired(true);
				if (isClone()) {
					String key = data.getName();
					in.setProtoInput(getPrototype().getInput(key));
					if (!in.isDefault())
						in.doCallback(this);
				}
			}
			else {
				in = newInputList.get(index);
			}
			addInput(in);
			addInputAsOutput(in.getKeyword(), in.getValue(), in.getUnitType());
			list.add(in);
		}
		newInputList = list;
		keywordList = new ArrayList<>(newDataList);
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
		//System.out.format("%n%s.updateClones()%n", this);
		for (Entity clone : getAllClones()) {
			((SubModel) clone).update();
		}
	}

	/**
	 * Adjusts the clone to match the present setting for its prototype sub-model.
	 */
	public void update() {
		//System.out.format("%n%s.update()%n", this);

		// Both the prototype and region must be set
		if (getPrototype() == null || getSubModelRegion() == null)
			return;

		// Do not record the components and their inputs to be 'edited'
		boolean bool = getJaamSimModel().isRecordEdits();
		getJaamSimModel().setRecordEdits(false);

		// Update the components
		createComponents();

		// Set the inputs for each component
		for (int seq = 0; seq < 2; seq++) {
			//System.out.format("%n%s - copyInputs - seq=%s%n", this, seq);
			for (Entity protoComp : getPrototype().getChildren()) {
				String localName = protoComp.getLocalName();
				Entity comp = getChild(localName);
				comp.copyInputs(protoComp, seq);
			}
		}

		// Reset the record edits state
		getJaamSimModel().setRecordEdits(bool);
	}

	/**
	 * Creates this sub-model's components and sets their inputs to match its prototype's components.
	 * @param protoCompList - components for the prototype
	 */
	public void createComponents() {
		//System.out.format("%n%s.createComponents%n", this);
		SubModel proto = (SubModel) getPrototype();

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
			InputAgent.generateEntityWithName(getJaamSimModel(),
					protoComp.getClass(), protoComp, name, this, true, true);
			//System.out.format("protoComp=%s, comp=%s%n", protoComp, comp);
		}
	}

	@Override
	public Class<? extends Entity> getJavaClass() {
		return SubModel.class;
	}

	@Override
	public boolean isDragAndDrop() {
		return !isClone();
	}

	@Override
	public String getPaletteName() {
		return PALETTE_NAME;
	}

	@Override
	public BufferedImage getIconImage() {
		return getObjectType().getIconImage();
	}

}
