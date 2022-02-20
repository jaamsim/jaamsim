/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2018-2022 JaamSim Software Inc.
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
package com.jaamsim.SubModels;

import java.util.ArrayList;

import com.jaamsim.Commands.KeywordCommand;
import com.jaamsim.Graphics.DisplayEntity;
import com.jaamsim.ProcessFlow.EntityGen;
import com.jaamsim.ProcessFlow.Linkable;
import com.jaamsim.input.InputAgent;
import com.jaamsim.input.InterfaceEntityInput;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.KeywordIndex;

public class SubModelStart extends DisplayEntity implements Linkable {

	@Keyword(description = "The next component in the sub-model.",
			exampleList = {"Queue1"})
	protected final InterfaceEntityInput<Linkable> nextComponent;
	{
		nextComponent = new InterfaceEntityInput<>(Linkable.class, "NextComponent", KEY_INPUTS, null);
		nextComponent.setRequired(true);
		this.addInput(nextComponent);
	}

	public SubModelStart() {}

	@Override
	public void addEntity(DisplayEntity ent) {
		nextComponent.getValue().addEntity(ent);
	}

	@Override
	public boolean canLink(boolean dir) {
		return dir;
	}

	@Override
	public void linkTo(DisplayEntity nextEnt, boolean dir) {
		if (!(nextEnt instanceof Linkable) || nextEnt instanceof EntityGen)
			return;

		ArrayList<String> toks = new ArrayList<>();
		toks.add(nextEnt.getName());
		KeywordIndex kw = new KeywordIndex(nextComponent.getKeyword(), toks, null);
		InputAgent.storeAndExecute(new KeywordCommand(this, kw));
	}

	@Override
	public ArrayList<DisplayEntity> getDestinationEntities() {
		ArrayList<DisplayEntity> ret = new ArrayList<>();
		Linkable l = nextComponent.getValue();
		if (l != null && (l instanceof DisplayEntity)) {
			ret.add((DisplayEntity)l);
		}
		return ret;
	}

}
