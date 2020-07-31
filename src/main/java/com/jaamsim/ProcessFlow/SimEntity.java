/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2014 Ausenco Engineering Canada Inc.
 * Copyright (C) 2016-2020 JaamSim Software Inc.
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

import com.jaamsim.Graphics.DisplayEntity;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.StringInput;
import com.jaamsim.input.StringListInput;
import com.jaamsim.states.StateEntity;

public class SimEntity extends StateEntity {

	@Keyword(description = "A list of states that will always appear in the output report, "
	                     + "even if no time is recorded for this state.",
	         exampleList = "Idle Working")
	protected final StringListInput defaultStateList;

	@Keyword(description = "The state of the SimEntity at the start of the simulation run or when "
	                     + "it is first created during a simulation run.",
	         exampleList = "Idle")
	protected final StringInput initialState;

	{
		attributeDefinitionList.setHidden(false);
		stateGraphics.setHidden(false);
		workingStateListInput.setHidden(true);

		defaultStateList = new StringListInput("DefaultStateList", KEY_INPUTS, new ArrayList<String>());
		this.addInput(defaultStateList);

		initialState = new StringInput("InitialState", KEY_INPUTS, "None");
		this.addInput(initialState);
	}

	public SimEntity() {}

	@Override
	public void earlyInit() {
		super.earlyInit();

		for (String state : defaultStateList.getValue()) {
			this.addState(state);
		}
	}

	@Override
	public String getInitialState() {
		return initialState.getValue();
	}

	@Override
	public void collectInitializationStats() {
		if (isGenerated())
			return;
		super.collectInitializationStats();
	}

	@Override
	public boolean isValidState(String state) {
		return true;
	}

	@Override
	public void linkTo(DisplayEntity nextEnt) {
		if (!(nextEnt instanceof EntityGen))
			return;

		EntityGen gen = (EntityGen) nextEnt;
		gen.setPrototypeEntity(this);
	}

}
