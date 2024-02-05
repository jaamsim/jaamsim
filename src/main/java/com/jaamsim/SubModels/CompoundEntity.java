/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2019-2024 JaamSim Software Inc.
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

import com.jaamsim.BooleanProviders.BooleanProvInput;
import com.jaamsim.Graphics.DisplayEntity;
import com.jaamsim.Graphics.Region;
import com.jaamsim.ProcessFlow.LinkedComponent;
import com.jaamsim.basicsim.Entity;
import com.jaamsim.basicsim.ErrorException;
import com.jaamsim.basicsim.JaamSimModel;
import com.jaamsim.input.Input;
import com.jaamsim.input.InputAgent;
import com.jaamsim.input.InputCallback;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.Vec3dInput;
import com.jaamsim.math.Vec3d;
import com.jaamsim.units.DimensionlessUnit;
import com.jaamsim.units.DistanceUnit;

public abstract class CompoundEntity extends LinkedComponent {

	@Keyword(description = "Determines whether to display the sub-model's components.")
	protected final BooleanProvInput showComponents;

	@Keyword(description = "The position of the entities being processed relative to the "
	                     + "sub-model. "
	                     + "This position is used when the sub-model's components are not shown.",
	         exampleList = {"1.0 0.0 0.01 m"})
	protected final Vec3dInput processPosition;

	private final LinkedHashMap<String, Entity> namedChildren = new LinkedHashMap<>();
	private SubModelStart smStart;

	private static final String smRegionName = "Region";

	{
		nextComponent.setRequired(false);

		regionInput.setCallback(regionCallback);

		showComponents = new BooleanProvInput("ShowComponents", FORMAT, false);
		this.addInput(showComponents);

		processPosition = new Vec3dInput("ProcessPosition", FORMAT, new Vec3d(0.0d, 0.0d, 0.01d));
		processPosition.setUnitType(DistanceUnit.class);
		this.addInput(processPosition);
	}

	public CompoundEntity() {}

	@Override
	public void postDefine() {
		super.postDefine();

		// If a clone, the region and its inputs are copied from the prototype
		Region smRegion = getSubModelRegion();
		if (smRegion != null)
			return;

		// Create the region
		JaamSimModel simModel = getJaamSimModel();
		Region proto = null;
		if (getPrototype() != null)
			proto = ((CompoundEntity) getPrototype()).getSubModelRegion();
		smRegion = InputAgent.generateEntityWithName(simModel, Region.class, proto, smRegionName, this, true, true);

		// Set the region's default inputs if it has no prototype from which to inherit its inputs
		if (proto == null) {
			InputAgent.applyArgs( smRegion, "RelativeEntity", this.getName());
			InputAgent.applyArgs( smRegion, "DisplayModel",   "RegionRectangle");
			InputAgent.applyValue(smRegion, "Scale",          0.5d, "");
			InputAgent.applyVec3d(smRegion, "Size",           new Vec3d(2.0d,  1.0d, 0.0d), DistanceUnit.class);
			InputAgent.applyVec3d(smRegion, "Position",       new Vec3d(0.0d, -1.5d, 0.0d), DistanceUnit.class);
			InputAgent.applyVec3d(smRegion, "Alignment",      new Vec3d(), DimensionlessUnit.class);
		}
	}

	static final InputCallback regionCallback = new InputCallback() {
		@Override
		public void callback(Entity ent, Input<?> inp) {
			CompoundEntity sm = (CompoundEntity) ent;
			Region region = (Region) inp.getValue();

			// Set the region for the sub-model
			sm.setRegion(region);

			// Set the region input for the sub-model's region
			Region subModelRegion = sm.getSubModelRegion();
			if (subModelRegion != null && region != null) {
				InputAgent.applyArgs(subModelRegion, inp.getKeyword(), region.getName());
			}
		}
	};

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
		return (Region) getChild(smRegionName);
	}

	public boolean isShowComponents(double simTime) {
		return showComponents.getNextBoolean(this, 0.0d);
	}

	@Override
	public void addEntity(DisplayEntity ent) {
		super.addEntity(ent);
		smStart.addEntity(ent);
	}

	public void addReturnedEntity(DisplayEntity ent) {
		sendToNextComponent(ent);
	}

	public Vec3d getProcessPosition() {
		return processPosition.getValue();
	}

}
