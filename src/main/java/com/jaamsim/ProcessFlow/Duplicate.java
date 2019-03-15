/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2014 Ausenco Engineering Canada Inc.
 * Copyright (C) 2016 JaamSim Software Inc.
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
import com.jaamsim.Graphics.DisplayEntity;
import com.jaamsim.basicsim.Entity;
import com.jaamsim.input.InputAgent;
import com.jaamsim.input.InterfaceEntityListInput;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.KeywordIndex;

public class Duplicate extends LinkedComponent {

	@Keyword(description = "The list of components that will receive the duplicated entities. " +
			"One duplicated entity will be sent to each entry in the list.",
	         exampleList = {"Assign1 Queue1"})
	protected final InterfaceEntityListInput<Linkable> targetComponentList;

	{
		targetComponentList = new InterfaceEntityListInput<>( Linkable.class, "TargetComponentList", KEY_INPUTS, null);
		targetComponentList.setUnique(false);
		targetComponentList.setRequired(true);
		this.addInput( targetComponentList);
	}

	public Duplicate() {}

	@Override
	public void addEntity(DisplayEntity ent) {
		super.addEntity(ent);

		// Make the duplicates and send them to the targets
		int n = 1;
		for (Linkable target : targetComponentList.getValue()) {

			// Create the duplicated entity
			StringBuilder sb = new StringBuilder();
			sb.append(ent.getName()).append("_Dup").append(n);
			DisplayEntity dup = InputAgent.generateEntityWithName(getJaamSimModel(), ent.getClass(), sb.toString());
			Entity.fastCopyInputs(ent, dup);

			// Set the state for the duplicated entity
			if (dup instanceof SimEntity) {
				String state = ((SimEntity)ent).getPresentState(getSimTime());
				((SimEntity)dup).setPresentState(state);
			}

			// Set the graphics for the duplicated entity
			dup.setRegion(ent.getCurrentRegion());
			dup.setPosition(ent.getPosition());
			dup.setDisplayModelList(ent.getDisplayModelList());
			dup.setSize(ent.getSize());
			dup.setOrientation(ent.getOrientation());
			dup.setAlignment(ent.getAlignment());

			// Send the duplicate to the target component
			target.addEntity(dup);
			n++;
		}

		// Send the received entity to the next component
		this.sendToNextComponent(ent);
	}

	// LinkDisplayable
	@Override
	public ArrayList<Entity> getDestinationEntities() {
		ArrayList<Entity> ret = new ArrayList<>();
		ArrayList<Linkable> ls = targetComponentList.getValue();
		if (ls == null)
			return ret;

		for (Linkable l : ls) {
			if (l != null && (l instanceof Entity)) {
				ret.add((Entity)l);
			}
		}
		return ret;
	}

	@Override
	public void linkTo(DisplayEntity nextEnt) {
		if (!(nextEnt instanceof Linkable) || nextEnt instanceof EntityGenerator) {
			return;
		}

		ArrayList<String> toks = new ArrayList<>();
		targetComponentList.getValueTokens(toks);
		toks.add(nextEnt.getName());
		KeywordIndex kw = new KeywordIndex(targetComponentList.getKeyword(), toks, null);
		InputAgent.storeAndExecute(new KeywordCommand(this, kw));
	}

}
