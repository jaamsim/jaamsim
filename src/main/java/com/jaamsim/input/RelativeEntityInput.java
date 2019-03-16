/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2015 Ausenco Engineering Canada Inc.
 * Copyright (C) 2019 JaamSim Software Inc.

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
import com.jaamsim.basicsim.JaamSimModel;

public class RelativeEntityInput extends EntityInput<DisplayEntity> {
	public RelativeEntityInput(String key, String cat, DisplayEntity def) {
		super(DisplayEntity.class, key, cat, def);
	}

	@Override
	public void parse(Entity thisEnt, KeywordIndex kw) throws InputErrorException {
		Input.assertCount(kw, 1);
		DisplayEntity ent = Input.parseEntity(thisEnt.getJaamSimModel(), kw.getArg(0), DisplayEntity.class);
		if (isCircular(thisEnt, ent))
			throw new InputErrorException("The assignment of %s to RelativeEntity would create a circular loop.", ent);
		value = ent;
	}

	private boolean isCircular(Entity thisEnt, DisplayEntity ent) {
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
			if (each.testFlag(Entity.FLAG_GENERATED))
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
