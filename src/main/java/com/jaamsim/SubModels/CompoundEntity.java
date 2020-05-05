/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2019-2020 JaamSim Software Inc.
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

import java.util.ArrayList;
import java.util.HashMap;

import com.jaamsim.Graphics.DisplayEntity;
import com.jaamsim.ProcessFlow.LinkedComponent;
import com.jaamsim.Graphics.Region;
import com.jaamsim.SubModels.SubModelEnd;
import com.jaamsim.SubModels.SubModelStart;
import com.jaamsim.basicsim.Entity;
import com.jaamsim.basicsim.ErrorException;
import com.jaamsim.basicsim.JaamSimModel;
import com.jaamsim.input.Input;
import com.jaamsim.input.BooleanInput;
import com.jaamsim.input.InputAgent;
import com.jaamsim.input.Keyword;

public abstract class CompoundEntity extends LinkedComponent {

	@Keyword(description = "Determines whether to display the sub-model's components.",
	         exampleList = {"FALSE"})
	protected final BooleanInput showComponents;

	private final HashMap<String, Entity> namedChildren = new HashMap<>();
	private SubModelStart smStart;
	private SubModelRegion smRegion;

	{
		namedExpressionInput.setHidden(true); // FIXME CustomOutputList conflicts with the component outputs
		nextComponent.setRequired(false);

		showComponents = new BooleanInput("ShowComponents", FORMAT, false);
		this.addInput(showComponents);
	}

	public CompoundEntity() {}

	@Override
	public void postDefine() {
		super.postDefine();

		// Create the region
		JaamSimModel simModel = getJaamSimModel();
		smRegion = InputAgent.generateEntityWithName(simModel, SubModelRegion.class, "Region", this, true, true);
		smRegion.setSubModel(this);
	}

	@Override
	public void setInputsForDragAndDrop() {
		super.setInputsForDragAndDrop();
		boolean bool = getJaamSimModel().getSimulation().isShowSubModels();
		showTemporaryComponents(bool);
	}

	@Override
	public void updateForInput(Input<?> in) {
		super.updateForInput(in);

		if (in == showComponents) {
			showComponents(showComponents.getValue());
			return;
		}
	}

	@Override
	public void earlyInit() {
		super.earlyInit();

		// Find the first component in the sub-model
		for (Entity comp : getChildren()) {
			if (comp instanceof SubModelStart) {
				smStart = (SubModelStart)comp;
				break;
			}
		}

		// Find the last component in the sub-model
		for (Entity comp : getChildren()) {
			if (comp instanceof SubModelEnd) {
				((SubModelEnd)comp).setSubModel(this);
			}
		}
	}

	@Override
	public Entity getChild(String name) {
		synchronized (namedChildren) {
			return namedChildren.get(name);
		}
	}

	@Override
	public void addChild(Entity ent) {
		synchronized (namedChildren) {
			namedChildren.put(ent.getLocalName(), ent);
		}
	}

	@Override
	public void removeChild(Entity ent) {
		synchronized (namedChildren) {
			if (ent != namedChildren.remove(ent.getLocalName()))
				throw new ErrorException("Named Children Internal Consistency error: %s", ent);
		}
	}

	@Override
	public void renameChild(Entity ent, String oldName, String newName) {
		synchronized (namedChildren) {
			if (namedChildren.get(newName) != null)
				throw new ErrorException("Child name: %s is already in use.", newName);

			if (namedChildren.remove(oldName) != ent)
				throw new ErrorException("Named Children Internal Consistency error");

			namedChildren.put(newName, ent);
		}
	}

	@Override
	public ArrayList<Entity> getChildren() {
		synchronized (namedChildren) {
			return new ArrayList<>(namedChildren.values());
		}
	}

	public Region getSubModelRegion() {
		return smRegion;
	}

	/**
	 * Displays or hides the sub-model's components.
	 * @param bool - if true, the components are displayed; if false, they are hidden.
	 */
	public void showComponents(boolean bool) {
		for (Entity ent : getChildren()) {
			if (!(ent instanceof DisplayEntity))
				continue;
			DisplayEntity comp = (DisplayEntity) ent;
			InputAgent.applyBoolean(comp, "Show", bool);
		}
	}

	public void showTemporaryComponents(boolean bool) {
		bool = bool || getShowComponents();
		for (Entity ent : getChildren()) {
			if (!(ent instanceof DisplayEntity))
				continue;
			DisplayEntity comp = (DisplayEntity) ent;
			comp.setShow(bool);
		}
	}

	public boolean getShowComponents() {
		return showComponents.getValue();
	}

	@Override
	public void addEntity(DisplayEntity ent) {
		super.addEntity(ent);
		smStart.addEntity(ent);
	}

	public void addReturnedEntity(DisplayEntity ent) {
		sendToNextComponent(ent);
	}

}
