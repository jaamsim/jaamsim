/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2018-2020 JaamSim Software Inc.
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

import com.jaamsim.EntityProviders.EntityProvInput;
import com.jaamsim.Graphics.DisplayEntity;
import com.jaamsim.Graphics.OverlayEntity;
import com.jaamsim.Graphics.TextBasics;
import com.jaamsim.ProcessFlow.Linkable;
import com.jaamsim.input.Keyword;

public class SubModelEnd extends DisplayEntity implements Linkable {

	@Keyword(description = "The next object, external to the sub-model, to which the processed "
	                     + "entity is passed. "
	                     + "If left blank, the entity is returned to the parent sub-model which "
	                     + "directs it to the object specified by its NextComponent input.",
	         exampleList = {"Statistics1"})
	protected final EntityProvInput<DisplayEntity> nextComponent;

	private CompoundEntity submodel;

	{
		nextComponent = new EntityProvInput<>(DisplayEntity.class, "NextComponent", KEY_INPUTS, null);
		nextComponent.addInvalidClass(OverlayEntity.class);
		nextComponent.addInvalidClass(TextBasics.class);
		this.addInput(nextComponent);
	}

	public SubModelEnd() {}

	public void setSubModel(CompoundEntity sub) {
		submodel = sub;
	}

	@Override
	public void addEntity(DisplayEntity ent) {

		// If NextComponent is not specified, return the entity to the sub-model object
		if (nextComponent.isDefault()) {
			submodel.addReturnedEntity(ent);
			return;
		}

		// If NextComponent is specified, send the entity to that object
		DisplayEntity nextComp = nextComponent.getValue().getNextEntity(getSimTime());
		if (!(nextComp instanceof Linkable)) {
			error("Object '%s' returned by NextComponent does not accept an entity.", nextComp);
		}
		((Linkable)nextComp).addEntity(ent);
	}

}
