/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2017-2020 JaamSim Software Inc.
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
import com.jaamsim.basicsim.ErrorException;
import com.jaamsim.basicsim.JaamSimModel;
import com.jaamsim.input.InputAgent;

public class DefineCommand implements Command {

	private final JaamSimModel simModel;
	private final Class<? extends Entity> klass;
	private Entity entity;
	private final String entityName;

	public DefineCommand(JaamSimModel sim, Class<? extends Entity> cls, String name) {
		simModel = sim;
		klass = cls;
		entity = null;
		entityName = name;
	}

	@Override
	public void execute() {

		// If the entity has been killed by the undo method, then simply restore it to life
		if (entity != null) {
			entity.restore(entityName);
			entity.getJaamSimModel().setSessionEdited(true);
			return;
		}

		// Create the entity
		if (simModel.getNamedEntity(entityName) != null) {
			throw new ErrorException("Name is already in use. Should never happen.");
		}
		entity = InputAgent.defineEntityWithUniqueName(simModel, klass, entityName, "", true);
		entity.getJaamSimModel().setSessionEdited(true);
	}

	@Override
	public void undo() {
		entity.kill();
		entity.getJaamSimModel().setSessionEdited(true);
	}

	@Override
	public Command tryMerge(Command cmd) {
		return null;
	}

	@Override
	public boolean isChange() {
		return true;
	}

	@Override
	public JaamSimModel getJaamSimModel() {
		return simModel;
	}

	@Override
	public String toString() {
		return String.format("Create: '%s'", entityName);
	}

}
