/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2019-2021 JaamSim Software Inc.
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
import java.util.LinkedHashMap;

import com.jaamsim.Graphics.DisplayEntity;
import com.jaamsim.Graphics.EntityLabel;
import com.jaamsim.Graphics.Region;
import com.jaamsim.ProcessFlow.LinkedComponent;
import com.jaamsim.basicsim.Entity;
import com.jaamsim.basicsim.ErrorException;
import com.jaamsim.basicsim.JaamSimModel;
import com.jaamsim.input.BooleanInput;
import com.jaamsim.input.Input;
import com.jaamsim.input.InputAgent;
import com.jaamsim.input.Keyword;
import com.jaamsim.math.Vec3d;
import com.jaamsim.units.DimensionlessUnit;
import com.jaamsim.units.DistanceUnit;

public abstract class CompoundEntity extends LinkedComponent {

	@Keyword(description = "Determines whether to display the sub-model's components.",
	         exampleList = {"FALSE"})
	protected final BooleanInput showComponents;

	private final LinkedHashMap<String, Entity> namedChildren = new LinkedHashMap<>();
	private SubModelStart smStart;
	private Region smRegion;

	{
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
		smRegion = InputAgent.generateEntityWithName(simModel, Region.class, "Region", this, true, true);

		// Set the default inputs for the region
		InputAgent.applyArgs( smRegion, "RelativeEntity", this.getName());
		InputAgent.applyArgs( smRegion, "DisplayModel",   "RegionRectangle");
		InputAgent.applyValue(smRegion, "Scale",          0.5d, "");
		InputAgent.applyVec3d(smRegion, "Size",           new Vec3d(2.0d,  1.0d, 0.0d), DistanceUnit.class);
		InputAgent.applyVec3d(smRegion, "Position",       new Vec3d(0.0d, -1.5d, 0.0d), DistanceUnit.class);
		InputAgent.applyVec3d(smRegion, "Alignment",      new Vec3d(), DimensionlessUnit.class);
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

		if (in == regionInput) {
			if (getCurrentRegion() == null)
				return;
			InputAgent.applyArgs(smRegion, "Region", getCurrentRegion().getName());
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
			if (namedChildren.get(ent.getLocalName()) != null)
				throw new ErrorException("Entity name: %s is already in use.", ent.getName());
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
			if (comp instanceof EntityLabel) {
				comp.setShow(bool && getSimulation().isShowLabels());
				continue;
			}
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
