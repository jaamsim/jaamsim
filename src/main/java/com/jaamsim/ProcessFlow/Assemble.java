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

import com.jaamsim.Commands.KeywordCommand;
import com.jaamsim.Graphics.DisplayEntity;
import com.jaamsim.Graphics.OverlayEntity;
import com.jaamsim.Graphics.TextBasics;
import com.jaamsim.basicsim.Entity;
import com.jaamsim.input.EntityInput;
import com.jaamsim.input.InputAgent;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.KeywordIndex;
import com.jaamsim.input.StringInput;
import com.jaamsim.states.StateEntity;

public class Assemble extends AbstractCombine implements EntityGen {

	@Keyword(description = "The prototype for entities representing the assembled part.",
	         exampleList = {"Proto"})
	private final EntityInput<DisplayEntity> prototypeEntity;

	@Keyword(description = "The base for the names assigned to the generated entities. "
	                     + "The generated entities will be named Name1, Name2, etc.",
	         exampleList = {"Customer", "Package"})
	private final StringInput baseName;

	private DisplayEntity assembledEntity;	// the generated entity representing the assembled part
	private int numberGenerated = 0;  // Number of entities generated so far

	{
		prototypeEntity = new EntityInput<>(DisplayEntity.class, "PrototypeEntity", KEY_INPUTS, null);
		prototypeEntity.setRequired(true);
		prototypeEntity.addInvalidClass(TextBasics.class);
		prototypeEntity.addInvalidClass(OverlayEntity.class);
		this.addInput(prototypeEntity);

		baseName = new StringInput("BaseName", KEY_INPUTS, null);
		baseName.setDefaultText("Assemble Name");
		this.addInput(baseName);
	}

	public Assemble() {}

	@Override
	public void earlyInit() {
		super.earlyInit();

		assembledEntity = null;
		numberGenerated = 0;
	}

	@Override
	protected boolean startProcessing(double simTime) {

		// Do the queues have enough entities?
		ArrayList<Queue> queueList = getQueues();
		int[] numList = getNumberRequired(simTime);
		if (isMatchRequired()) {
			String m = selectMatchValue(queueList, numList);
			if (m == null) {
				return false;
			}
			this.setMatchValue(m);
		}
		else {
			if (!sufficientEntities(queueList, numList, null)) {
				return false;
			}
		}

		// Remove the appropriate entities from each queue
		for (int i = 0; i < queueList.size(); i++) {
			for (int n = 0; n < numList[i]; n++) {
				DisplayEntity ent = queueList.get(i).removeFirstForMatch(getMatchValue());
				if (ent == null)
					error("An entity with the specified match value %s was not found in %s.",
							getMatchValue(), queueList.get(i));
				ent.kill();
			}
		}

		// Create the entity representing the assembled part
		numberGenerated++;
		DisplayEntity proto = prototypeEntity.getValue();
		String name = baseName.getValue();
		if (name == null) {
			name = this.getName() + "_";
			name = name.replace(".", "_");
		}
		name = name + numberGenerated;

		// Create the new entity
		assembledEntity = InputAgent.generateEntityWithName(getJaamSimModel(), proto.getClass(), name);
		Entity.fastCopyInputs(proto, assembledEntity);
		assembledEntity.earlyInit();

		// Set the obj output to the assembled part
		receiveEntity(assembledEntity);
		setEntityState(assembledEntity);

		// Set the state for the assembled part
		if (!stateAssignment.isDefault() && assembledEntity instanceof StateEntity) {
			String state = stateAssignment.getValue().getNextString(simTime);
			((StateEntity)assembledEntity).setPresentState(state);
		}
		return true;
	}

	@Override
	protected void processStep(double simTime) {

		// Send the assembled part to the next element in the chain
		this.sendToNextComponent(assembledEntity);
		assembledEntity = null;
	}

	@Override
	public boolean isFinished() {
		return assembledEntity == null;
	}

	@Override
	public void setPrototypeEntity(DisplayEntity proto) {
		KeywordIndex kw = InputAgent.formatArgs(prototypeEntity.getKeyword(), proto.getName());
		InputAgent.storeAndExecute(new KeywordCommand(this, kw));
	}

	@Override
	public ArrayList<DisplayEntity> getSourceEntities() {
		ArrayList<DisplayEntity> ret = super.getSourceEntities();
		if (prototypeEntity.getValue() != null)
			ret.add(prototypeEntity.getValue());
		return ret;
	}

	@Override
	public void updateGraphics(double simTime) {
		if (assembledEntity == null)
			return;
		moveToProcessPosition(assembledEntity);
	}

}
