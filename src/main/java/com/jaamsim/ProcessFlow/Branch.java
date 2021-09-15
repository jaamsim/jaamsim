/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2013 Ausenco Engineering Canada Inc.
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
import com.jaamsim.Samples.SampleInput;
import com.jaamsim.basicsim.Entity;
import com.jaamsim.input.InputAgent;
import com.jaamsim.input.InterfaceEntityListInput;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.KeywordIndex;
import com.jaamsim.ui.Federate;
import com.jaamsim.units.DimensionlessUnit;

public class Branch extends LinkedComponent {

	@Keyword(description = "The list of possible next objects to which the processed DisplayEntity can be passed.",
	         exampleList = {"Queue1 Queue2"})
	protected final InterfaceEntityListInput<Linkable> nextComponentList;

	@Keyword(description = "A number that determines the choice of next component: "
	                     + "1 = first branch, 2 = second branch, etc.",
	         exampleList = {"2", "DiscreteDistribution1", "'indexOfMin([Queue1].QueueLength, [Queue2].QueueLength)'"})
	private final SampleInput choice;

	{
		nextComponent.setHidden(true);

		nextComponentList = new InterfaceEntityListInput<>(Linkable.class, "NextComponentList", KEY_INPUTS, null);
		nextComponentList.setRequired(true);
		this.addInput(nextComponentList);

		choice = new SampleInput("Choice", KEY_INPUTS, null);
		choice.setUnitType(DimensionlessUnit.class );
		choice.setEntity(this);
		choice.setValidRange(1, Double.POSITIVE_INFINITY);
		choice.setRequired(true);
		this.addInput(choice);
	}

	@Override
	public void addEntity( DisplayEntity ent ) {
		super.addEntity(ent);

		// Choose the next component for this entity
		int i = (int) choice.getValue().getNextSample(this.getSimTime());
		if (i<1 || i>nextComponentList.getValue().size())
			error("Chosen index i=%s is out of range for NextComponentList: %s.",
			      i, nextComponentList.getValue());
//Added by Jalal
		
		ArrayList<String> atts = this.getAttributeNames();
		if(atts.size() > 0 && atts.get(0).equals("waitRTIOrder")) Federate.instance.onAddEntity(this);
		
		//Till here
		
		// Set the standard outputs for a LinkedComponent
		this.sendToNextComponent(ent);

		// Pass the entity to the selected next component
		nextComponentList.getValue().get(i-1).addEntity(ent);
		

	}

	@Override
	public ArrayList<Entity> getDestinationEntities() {
		ArrayList<Entity> ret = new ArrayList<>();
		ArrayList<Linkable> ls = nextComponentList.getValue();
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
		nextComponentList.getValueTokens(toks);
		toks.add(nextEnt.getName());
		KeywordIndex kw = new KeywordIndex(nextComponentList.getKeyword(), toks, null);
		InputAgent.storeAndExecute(new KeywordCommand(this, kw));
	}

}
