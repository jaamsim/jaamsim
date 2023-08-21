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
package com.jaamsim.ProcessFlow;

import com.jaamsim.BooleanProviders.BooleanProvInput;
import com.jaamsim.Graphics.DisplayEntity;
import com.jaamsim.Thresholds.SignalThreshold;
import com.jaamsim.input.EntityInput;
import com.jaamsim.input.Keyword;

public class EntitySignal extends LinkedComponent {

	@Keyword(description = "The Threshold controlled by this Signal.",
	         exampleList = {"SignalThreshold1"})
	private final EntityInput<SignalThreshold> targetSignalThreshold;

	@Keyword(description = "The new state for the target SignalThreshold: TRUE = Open, FALSE = Closed.")
	private final BooleanProvInput newState;

	{
		targetSignalThreshold = new EntityInput<>( SignalThreshold.class, "TargetSignalThreshold", KEY_INPUTS, null);
		targetSignalThreshold.setRequired(true);
		this.addInput( targetSignalThreshold);

		newState = new BooleanProvInput( "NewState", KEY_INPUTS, true);
		this.addInput( newState);
	}

	public boolean getNewState(double simTime) {
		return newState.getNextBoolean(this, simTime);
	}

	@Override
	public void addEntity( DisplayEntity ent ) {
		super.addEntity(ent);

		// Signal the target threshold
		SignalThreshold target = targetSignalThreshold.getValue();
		boolean bool = getNewState(getSimTime());
		target.setOpen(bool);

		// Send the entity to the next component
		this.sendToNextComponent( ent );
	}

}
