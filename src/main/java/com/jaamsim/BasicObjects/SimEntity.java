/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2014 Ausenco Engineering Canada Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 */
package com.jaamsim.BasicObjects;

import java.util.ArrayList;

import com.jaamsim.input.Keyword;
import com.jaamsim.input.StringListInput;
import com.jaamsim.states.StateEntity;

public class SimEntity extends StateEntity {

	@Keyword(description = "A list of states that will always appear in the output report, "
			+ "even if no time is recorded for this state.",
	         exampleList = "Idle Working")
	protected final StringListInput defaultStateList;

	{
		attributeDefinitionList.setHidden(false);

		defaultStateList = new StringListInput("DefaultStateList", "Key Inputs", new ArrayList<String>());
		this.addInput(defaultStateList);
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
		return "None";
	}

	@Override
	public boolean isValidState(String state) {
		return true;
	}

}
