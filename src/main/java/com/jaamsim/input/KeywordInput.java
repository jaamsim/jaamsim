/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2015 Ausenco Engineering Canada Inc.
 * Copyright (C) 2021 JaamSim Software Inc.
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

import com.jaamsim.Graphics.DisplayEntity;
import com.jaamsim.basicsim.Entity;

public class KeywordInput extends Input<String> {
	private DisplayEntity targetEntity;
	private Input<?> targetInput;

	public KeywordInput(String key, String cat, String def) {
		super(key, cat, def);
		targetEntity = null;
		targetInput = null;
	}

	@Override
	public void parse(Entity thisEnt, KeywordIndex kw) throws InputErrorException {
		Input.assertCount(kw, 2);
		try {
			DisplayEntity ent = Input.parseEntity(thisEnt.getJaamSimModel(), kw.getArg(0), DisplayEntity.class);

			String key = kw.getArg(1);
			Input<?> in = ent.getInput(key);
			if (in == null)
				throw new InputErrorException("'%s' is not a valid keyword for entity '%s'", key, ent);

			value = key;
			targetEntity = ent;
			targetInput = in;
		}
		catch (InputErrorException e) {
			throw new InputErrorException(e.getMessage());
		}
	}

	public Input<?> getTargetInput() {
		return targetInput;
	}

	public DisplayEntity getTargetEntity() {
		return targetEntity;
	}

	@Override
	public void getValueTokens(ArrayList<String> toks) {
		if (value == null || isDef)
			return;
		toks.add(targetEntity.getName());
		toks.add(value);
	}

}
