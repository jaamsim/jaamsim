/*
 * JaamSim Discrete Event Simulation
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

import com.jaamsim.Graphics.Region;
import com.jaamsim.basicsim.Entity;
import com.jaamsim.basicsim.JaamSimModel;

public class RegionInput extends EntityInput<Region> {

	public RegionInput(String key, String cat, Region def) {
		super(Region.class, key, cat, def);
	}

	@Override
	public void parse(Entity thisEnt, KeywordIndex kw) throws InputErrorException {
		Input.assertCount(kw, 1);
		Region reg = Input.parseEntity(thisEnt.getJaamSimModel(), kw.getArg(0), Region.class);
		if (thisEnt instanceof Region && isCircular((Region) thisEnt, reg))
			throw new InputErrorException("The assignment of %s to Region would create a circular loop.", reg);
		value = reg;
	}

	private boolean isCircular(Region thisReg, Region r) {
		Region reg = r;
		while (reg != null) {
			if (reg == thisReg)
				return true;
			reg = reg.getCurrentRegion();
		}
		return false;
	}

	@Override
	public ArrayList<String> getValidOptions(Entity ent) {
		ArrayList<String> list = new ArrayList<>();
		JaamSimModel simModel = ent.getJaamSimModel();
		for (Region each: simModel.getClonesOfIterator(Region.class)) {
			if (each.isGenerated())
				continue;

			if (ent instanceof Region && isCircular((Region) ent, each))
				continue;

			list.add(each.getName());
		}
		Collections.sort(list, Input.uiSortOrder);
		return list;
	}

}
