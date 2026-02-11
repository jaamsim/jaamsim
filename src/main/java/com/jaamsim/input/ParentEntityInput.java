/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2026 JaamSim Software Inc.
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

import java.util.ArrayList;
import java.util.Collections;

import com.jaamsim.Graphics.DisplayEntity;
import com.jaamsim.Graphics.EntityLabel;
import com.jaamsim.Graphics.OverlayEntity;
import com.jaamsim.Graphics.Region;
import com.jaamsim.basicsim.Entity;

public class ParentEntityInput extends EntityInput<Entity> {

	public ParentEntityInput(String key, String cat, Entity def) {
		super(Entity.class, key, cat, def);
	}

	@Override
	public void parse(Entity thisEnt, KeywordIndex kw) throws InputErrorException {
		Input.assertCount(kw, 1);
		Entity tmp = Input.parseEntity(thisEnt.getJaamSimModel(), kw.getArg(0), Entity.class);
		if (tmp == null)
			throw new InputErrorException(INP_ERR_ENTNAME, tmp);

		String localName = thisEnt.getLocalName();
		if (tmp.getChild(localName) != null)
			throw new InputErrorException("Entity %s already has a child entity named %s.%n"
					+ "Change the new child's name before assigning it to this parent entity.",
					tmp, localName);

		if (isCircular(thisEnt, tmp))
			throw new InputErrorException("The assignment of %s to Parent would create a circular loop.", tmp);
		value = tmp;
	}

	private static boolean isCircular(Entity thisEnt, Entity e) {
		Entity ent = e;
		while (ent != null) {
			if (ent == thisEnt)
				return true;
			ent = ent.getParent();
		}
		return false;
	}

	@Override
	public ArrayList<String> getValidOptions(Entity ent) {
		ArrayList<String> list = new ArrayList<>();
		for (DisplayEntity each: ent.getJaamSimModel().getClonesOfIterator(DisplayEntity.class)) {
			if (!each.isRegistered())
				continue;

			if (each instanceof OverlayEntity || each instanceof Region || each instanceof EntityLabel)
				continue;

			if (isCircular(ent, each))
				continue;

			if (each != ent.getParent() && each.getChild(ent.getLocalName()) != null)
				continue;

			list.add(each.getName());
		}
		Collections.sort(list, Input.uiSortOrder);
		return list;
	}

	@Override
	public boolean isEdited() {
		// Parent name inputs are not saved to the configuration file
		return false;
	}

	public void setInitialValue(Entity newParent) {
		value = newParent;
		String name = "";
		if (newParent != null)
			name = newParent.getName();
		valueTokens = new String[]{name};
		isDef = false;
	}

	@Override
	public void reset(Entity ent) {
		String localName = ent.getLocalName();
		if (ent.getParent() != null && ent.getJaamSimModel().getEntity(localName) != null)
			throw new InputErrorException("Entity named %s already exists.%n"
					+ "Change the entity's name before deleting its 'Parent' input.",
					localName);
		super.reset();
	}

}
