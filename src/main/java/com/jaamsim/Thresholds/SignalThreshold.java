/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2014 Ausenco Engineering Canada Inc.
 * Copyright (C) 2023 JaamSim Software Inc.
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
package com.jaamsim.Thresholds;

import com.jaamsim.BooleanProviders.BooleanProvInput;
import com.jaamsim.input.Keyword;

/**
 * SignalThreshold is a type of Threshold that is controlled directly by
 * another object. At present, it is required only for EntitySignal.
 * @author Harry
 *
 */
public class SignalThreshold extends Threshold {

	@Keyword(description = "The state for the SignalThreshold at the start of "
			+ "the simulation run: TRUE = Open, FALSE = Closed.",
	         exampleList = {"FALSE"})
	private final BooleanProvInput initState;

	{
		initState = new BooleanProvInput("InitialState", KEY_INPUTS, false);
		this.addInput(initState);
	}

	public SignalThreshold() {}

	@Override
	public void startUp() {
		super.startUp();
		boolean bool = initState.getNextBoolean(this, 0.0d);
		this.setOpen(bool);
	}

	@Override
	public String getInitialState() {
		if (initState.getNextBoolean(this, 0.0d))
			return "Open";
		else
			return "Closed";
	}

}
