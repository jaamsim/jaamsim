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
import com.jaamsim.basicsim.Entity;
import com.jaamsim.input.EntityListInput;
import com.jaamsim.input.Keyword;

public class Duplicate extends LinkedComponent {

	@Keyword(description = "The list of components that will receive the duplicated entities. " +
			"One duplicated entity will be sent to each entry in the list.",
	         exampleList = {"Assign1 Queue1"})
	protected final EntityListInput<LinkedComponent> targetComponentList;

	{
		targetComponentList = new EntityListInput<>( LinkedComponent.class, "TargetComponentList", "Key Inputs", null);
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
		for (LinkedComponent target : targetComponentList.getValue()) {

			// Create the duplicated entity
			StringBuilder sb = new StringBuilder();
			sb.append(ent.getName()).append("_Dup").append(n);
			DisplayEntity dup = Entity.fastCopy(ent, sb.toString());

			// Set the state for the duplicated entity
			if (dup instanceof SimEntity) {
				String state = ((SimEntity)ent).getPresentState(getSimTime());
				((SimEntity)dup).setPresentState(state);
			}

			// Send the duplicate to the target component
			target.addEntity(dup);
			n++;
		}

		// Send the received entity to the next component
		this.sendToNextComponent(ent);
	}

}
