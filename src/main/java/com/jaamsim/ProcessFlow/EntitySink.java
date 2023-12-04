/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2013 Ausenco Engineering Canada Inc.
 * Copyright (C) 2020-2023 JaamSim Software Inc.
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
import com.jaamsim.events.EventManager;

/**
 * EntitySink kills the DisplayEntities sent to it.
 */
public class EntitySink extends LinkedComponent {

	{
		nextComponent.setHidden(true);
		defaultEntity.setHidden(true);
		stateAssignment.setHidden(true);
	}

	@Override
	public void addEntity( DisplayEntity ent ) {
		super.addEntity(ent);
		double simTime = EventManager.simSeconds();

		// Increment the number processed
		releaseEntity(simTime);

		// Clear the 'obj' output
		setReceivedEntity(null);

		// Kill the added entity
		ent.dispose();
	}

}
