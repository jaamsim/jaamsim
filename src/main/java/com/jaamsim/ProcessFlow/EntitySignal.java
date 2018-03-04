/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2014 Ausenco Engineering Canada Inc.
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

import com.jaamsim.Graphics.DisplayEntity;
import com.jaamsim.Thresholds.SignalThreshold;
import com.jaamsim.input.BooleanInput;
import com.jaamsim.input.EntityInput;
import com.jaamsim.input.Keyword;

public class EntitySignal extends LinkedComponent {

	@Keyword(description = "The Threshold controlled by this Signal.",
	         exampleList = {"SignalThreshold1"})
	private final EntityInput<SignalThreshold> targetSignalThreshold;

	@Keyword(description = "The new state for the target SignalThreshold: TRUE = Open, FALSE = Closed.",
	         exampleList = {"FALSE"})
	private final BooleanInput newState;

	{
		targetSignalThreshold = new EntityInput<>( SignalThreshold.class, "TargetSignalThreshold", KEY_INPUTS, null);
		targetSignalThreshold.setRequired(true);
		this.addInput( targetSignalThreshold);

		newState = new BooleanInput( "NewState", KEY_INPUTS, true);
		this.addInput( newState);
	}

	@Override
	public void addEntity( DisplayEntity ent ) {
		super.addEntity(ent);

		// Signal the target threshold
		targetSignalThreshold.getValue().setOpen(newState.getValue());

		// Send the entity to the next component
		this.sendToNextComponent( ent );
	}

}
