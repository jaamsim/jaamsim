/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2024 JaamSim Software Inc.
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
package com.jaamsim.input;

import com.jaamsim.basicsim.Entity;

public class EntityNameInput extends Input<String> {

	public EntityNameInput(String key, String cat, String def) {
		super(key, cat, def);
	}

	@Override
	public void parse(Entity thisEnt, KeywordIndex kw) throws InputErrorException {
		Input.assertCount(kw, 1);

		String localName = kw.getArg(0);
		if (!isDef) {

			// Check that the entity was defined AFTER the RecordEdits command
			if (!thisEnt.isAdded())
				throw new InputErrorException("Cannot rename an entity that was defined before the "
						+ "RecordEdits command.");

			// Check that the new name is valid
			if (!InputAgent.isValidName(localName))
				throw new InputErrorException(InputAgent.INP_ERR_BADNAME, localName);

			// Get the new absolute name
			String name = localName;
			if (thisEnt.getParent() != thisEnt.getSimulation())
				name = thisEnt.getParent().getName() + "." + localName;

			// Check that the new absolute name does not conflict with another entity
			Entity ent = thisEnt.getJaamSimModel().getNamedEntity(name);
			if (ent != null && ent != thisEnt)
				throw new InputErrorException(InputAgent.INP_ERR_DEFINEUSED, name,
						ent.getClass().getSimpleName());
		}

		value = localName;
	}

	@Override
	public String getValidInputDesc() {
		return Input.VALID_ENTITY_NAME;
	}

	@Override
	public boolean isEdited() {
		// Name inputs are not saved to the configuration file
		return false;
	}

	public void setInitialValue(String name) {
		value = name;
		valueTokens = new String[]{name};
		isDef = false;
	}

}
