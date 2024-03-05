/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2014 Ausenco Engineering Canada Inc.
 * Copyright (C) 2016-2024 JaamSim Software Inc.
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

import com.jaamsim.Graphics.DisplayEntity;
import com.jaamsim.input.InputAgent;
import com.jaamsim.input.InterfaceEntityInput;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.StringInput;

public class Pack extends AbstractPack {

	@Keyword(description = "The prototype for EntityContainers to be generated. "
	                     + "The generated EntityContainers will be copies of this entity.",
	         exampleList = {"EntityContainer1"})
	protected final InterfaceEntityInput<EntContainer> prototypeEntityContainer;

	@Keyword(description = "The base for the names assigned to the generated EntityContainers. "
	                     + "The generated containers will be named Name1, Name2, etc.",
	         exampleList = {"Container", "Box"})
	private final StringInput baseName;

	private int numberGenerated;  // Number of EntityContainers generated so far

	{
		prototypeEntityContainer = new InterfaceEntityInput<>(EntContainer.class, "PrototypeEntityContainer", KEY_INPUTS, null);
		prototypeEntityContainer.setRequired(true);
		this.addInput(prototypeEntityContainer);

		baseName = new StringInput("BaseName", KEY_INPUTS, null);
		baseName.setDefaultText("Pack Name");
		this.addInput(baseName);
	}

	public Pack() {}

	@Override
	public void earlyInit() {
		super.earlyInit();
		numberGenerated = 0;
	}

	@Override
	protected boolean isContainerAvailable() {
		return true;
	}

	@Override
	protected EntContainer getNextContainer() {
		numberGenerated++;

		// Set the name for the container
		String name = baseName.getValue();
		if (name == null) {
			name = this.getName() + "_";
			name = name.replace(".", "_");
		}
		name = name + numberGenerated;

		DisplayEntity proto = (DisplayEntity)prototypeEntityContainer.getValue();
		DisplayEntity ret = (DisplayEntity) InputAgent.getGeneratedClone(proto, name);
		ret.earlyInit();
		ret.lateInit();
		return (EntContainer)ret;
	}

}
