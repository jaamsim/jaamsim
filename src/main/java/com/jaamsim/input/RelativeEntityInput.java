/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2015 Ausenco Engineering Canada Inc.
 * Copyright (C) 2019-2026 JaamSim Software Inc.
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

import com.jaamsim.EntityProviders.EntityProvConstant;
import com.jaamsim.EntityProviders.EntityProvInput;
import com.jaamsim.EntityProviders.EntityProvider;
import com.jaamsim.Graphics.DisplayEntity;
import com.jaamsim.Graphics.EntityLabel;
import com.jaamsim.Graphics.OverlayEntity;
import com.jaamsim.Graphics.Region;
import com.jaamsim.basicsim.Entity;
import com.jaamsim.basicsim.JaamSimModel;

public class RelativeEntityInput extends EntityProvInput<DisplayEntity> {
	public RelativeEntityInput(String key, String cat, EntityProvider<DisplayEntity> def) {
		super(DisplayEntity.class, key, cat, def);
	}

	@Override
	public void parse(Entity thisEnt, KeywordIndex kw) throws InputErrorException {
		EntityProvider<DisplayEntity> temp = Input.parseEntityProvider(kw, thisEnt, DisplayEntity.class);
		if (temp instanceof EntityProvConstant) {
			DisplayEntity ent = temp.getNextEntity(thisEnt, 0.0d);
			if (isCircular(thisEnt, ent)) {
				throw new InputErrorException("The assignment of %s to RelativeEntity would create a circular loop.", ent);
			}
		}
		setValid(true);
		value = temp;
	}

	private static boolean isCircular(Entity thisEnt, DisplayEntity e) {
		DisplayEntity ent = e;
		while (ent != null) {
			if (ent == thisEnt)
				return true;
			ent = ent.getRelativeEntity();
		}
		return false;
	}

	@Override
	public ArrayList<String> getValidOptions(Entity ent) {
		ArrayList<String> list = new ArrayList<>();
		JaamSimModel simModel = ent.getJaamSimModel();
		for (DisplayEntity each: simModel.getClonesOfIterator(DisplayEntity.class)) {
			if (each.isGenerated())
				continue;

			if (each instanceof OverlayEntity || each instanceof Region || each instanceof EntityLabel)
				continue;

			if (isCircular(ent, each))
				continue;

			list.add(each.getName());
		}
		Collections.sort(list, Input.uiSortOrder);
		return list;
	}

}
