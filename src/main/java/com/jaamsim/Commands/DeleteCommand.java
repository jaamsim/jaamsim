/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2017 JaamSim Software Inc.
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
package com.jaamsim.Commands;

import com.jaamsim.basicsim.Entity;
import com.jaamsim.input.Input;
import com.jaamsim.input.InputAgent;

public class DeleteCommand implements Command {

	private final Entity entity;
	private final String entityName;

	public DeleteCommand(Entity ent) {
		entity = ent;
		entityName = entity.getName();
	}

	@Override
	public void execute() {
		entity.kill();
		if (!entity.testFlag(Entity.FLAG_GENERATED)) {
			InputAgent.setSessionEdited(true);
		}

		// Remove any references to the deleted entity from the inputs to other entities
		for (Entity ent : Entity.getClonesOfIterator(Entity.class)) {
			for (Input<?> in : ent.getEditableInputs()) {
				in.removeReferences(entity);
			}
		}
	}

	@Override
	public void undo() {
		entity.restore(entityName);

		// FIXME need to restore references to the deleted entity in the inputs to other entities
	}

	@Override
	public Command tryMerge(Command cmd) {
		return null;
	}

	@Override
	public String toString() {
		return String.format("Delete: '%s'", entityName);
	}

}
