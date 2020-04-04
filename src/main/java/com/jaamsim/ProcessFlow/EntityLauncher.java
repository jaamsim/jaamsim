/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2017-2020 JaamSim Software Inc.
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
package com.jaamsim.ProcessFlow;

import java.util.ArrayList;

import com.jaamsim.Commands.KeywordCommand;
import com.jaamsim.EntityProviders.EntityProvInput;
import com.jaamsim.GameObjects.GameEntity;
import com.jaamsim.Graphics.DisplayEntity;
import com.jaamsim.Graphics.OverlayEntity;
import com.jaamsim.Graphics.TextBasics;
import com.jaamsim.basicsim.Entity;
import com.jaamsim.input.InputAgent;
import com.jaamsim.input.InterfaceEntityInput;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.KeywordIndex;
import com.jaamsim.input.StringInput;

public class EntityLauncher extends GameEntity implements EntityGen {

	@Keyword(description = "The prototype for entities to be generated. "
	                     + "The generated entities will be copies of this entity.",
	         exampleList = {"Proto", "'choose( this.NumberGenerated%2+1, [Proto1], [Proto2])'"})
	private final EntityProvInput<DisplayEntity> prototypeEntity;

	@Keyword(description = "The object to which the generated DisplayEntity is passed.",
	         exampleList = {"Queue1"})
	protected final InterfaceEntityInput<Linkable> nextComponent;

	@Keyword(description = "The base for the names assigned to the generated entities. "
	                     + "The generated entities will be named Name1, Name2, etc.",
	         exampleList = {"Customer", "Package"})
	private final StringInput baseName;

	private int numberGenerated = 0;  // Number of entities generated so far

	{
		prototypeEntity = new EntityProvInput<>(DisplayEntity.class, "PrototypeEntity", KEY_INPUTS, null);
		prototypeEntity.setRequired(true);
		prototypeEntity.addInvalidClass(TextBasics.class);
		prototypeEntity.addInvalidClass(OverlayEntity.class);
		this.addInput(prototypeEntity);

		nextComponent = new InterfaceEntityInput<>(Linkable.class, "NextComponent", KEY_INPUTS, null);
		nextComponent.setRequired(true);
		this.addInput(nextComponent);

		baseName = new StringInput("BaseName", KEY_INPUTS, null);
		baseName.setDefaultText("Generator Name");
		this.addInput(baseName);
	}

	public EntityLauncher() {}

	@Override
	public void earlyInit() {
		super.earlyInit();
		numberGenerated = 0;
	}

	@Override
	public void doAction() {

		// Set the name for the entities
		String name = baseName.getValue();
		if (name == null)
			name = this.getName() + "_";

		// Create a new entity
		numberGenerated++;
		DisplayEntity proto = prototypeEntity.getValue().getNextEntity(0.0d);
		name = name + numberGenerated;
		DisplayEntity ent = InputAgent.generateEntityWithName(getJaamSimModel(), proto.getClass(), name);
		Entity.fastCopyInputs(proto, ent);
		ent.earlyInit();

		// Send the entity to the next element in the chain
		nextComponent.getValue().addEntity(ent);
	}

	@Override
	public void setPrototypeEntity(DisplayEntity proto) {
		KeywordIndex kw = InputAgent.formatArgs(prototypeEntity.getKeyword(), proto.getName());
		InputAgent.storeAndExecute(new KeywordCommand(this, kw));
	}

	@Override
	public void linkTo(DisplayEntity nextEnt) {
		if (nextComponent.getHidden() || !(nextEnt instanceof Linkable)
				|| nextEnt instanceof EntityGenerator) {
			return;
		}

		ArrayList<String> toks = new ArrayList<>();
		toks.add(nextEnt.getName());
		KeywordIndex kw = new KeywordIndex(nextComponent.getKeyword(), toks, null);
		InputAgent.storeAndExecute(new KeywordCommand(this, kw));
	}

	@Override
	public ArrayList<DisplayEntity> getDestinationEntities() {
		ArrayList<DisplayEntity> ret = new ArrayList<>();
		Linkable l = nextComponent.getValue();
		if (l != null && (l instanceof DisplayEntity)) {
			ret.add((DisplayEntity)l);
		}
		return ret;
	}

	@Override
	public ArrayList<DisplayEntity> getSourceEntities() {
		ArrayList<DisplayEntity> ret = new ArrayList<>();
		if (prototypeEntity.getValue() == null)
			return ret;
		DisplayEntity ent = prototypeEntity.getValue().getNextEntity(0.0d);
		if (ent != null) {
			ret.add(ent);
		}
		return ret;
	}

}
