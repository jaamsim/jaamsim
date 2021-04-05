/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2014 Ausenco Engineering Canada Inc.
 * Copyright (C) 2016-2021 JaamSim Software Inc.
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
import com.jaamsim.input.StringInput;
import com.jaamsim.states.StateEntity;

public class Duplicate extends LinkedComponent {

	@Keyword(description = "The list of components that will receive the duplicated entities. " +
			"One duplicated entity will be sent to each entry in the list.",
	         exampleList = {"Assign1 Queue1"})
	protected final InterfaceEntityListInput<Linkable> targetComponentList;

	@Keyword(description = "The base for the names assigned to the duplicated entities. "
	                     + "The duplicated entities will be named Name1, Name2, etc.",
	         exampleList = {"Customer", "Package"})
	private final StringInput baseName;

	{
		targetComponentList = new InterfaceEntityListInput<>( Linkable.class, "TargetComponentList", KEY_INPUTS, null);
		targetComponentList.setUnique(false);
		targetComponentList.setRequired(true);
		this.addInput( targetComponentList);

		baseName = new StringInput("BaseName", KEY_INPUTS, null);
		baseName.setDefaultText("EntityName_Dup");
		this.addInput(baseName);
	}

	public Duplicate() {}

	@Override
	public void addEntity(DisplayEntity ent) {
		super.addEntity(ent);
		double simTime = getSimTime();

		// Set the base name for the duplicates
		String name = baseName.getValue();
		if (name == null) {
			name = ent.getName() + "_Dup";
			name = name.replace(".", "_");
		}

		// Make the duplicates and send them to the targets
		int n = 1;
		for (Linkable target : targetComponentList.getValue()) {

			// Create the duplicated entity
			DisplayEntity dup = InputAgent.generateEntityWithName(getJaamSimModel(), ent.getClass(), name + n);
			Entity.fastCopyInputs(ent, dup);

			// Set the state for the duplicated entity
			if (dup instanceof StateEntity) {
				String state = ((StateEntity) ent).getPresentState(simTime);
				((StateEntity) dup).setPresentState(state);
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
	public ArrayList<DisplayEntity> getDestinationEntities() {
		ArrayList<DisplayEntity> ret = super.getDestinationEntities();

		ArrayList<Linkable> ls = targetComponentList.getValue();
		if (ls == null)
			return ret;

		for (Linkable l : ls) {
			if (l != null && (l instanceof DisplayEntity)) {
				ret.add((DisplayEntity)l);
			}
		}
		return ret;
	}

	@Override
	public void linkTo(DisplayEntity nextEnt, boolean dir) {
		if (nextComponent.isDefault()) {
			super.linkTo(nextEnt);
			return;
		}
		if (!(nextEnt instanceof Linkable) || nextEnt instanceof EntityGen)
			return;

		ArrayList<String> toks = new ArrayList<>();
		targetComponentList.getValueTokens(toks);
		toks.add(nextEnt.getName());
		KeywordIndex kw = new KeywordIndex(targetComponentList.getKeyword(), toks, null);
		InputAgent.storeAndExecute(new KeywordCommand(this, kw));
	}

}
