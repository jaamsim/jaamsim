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
package com.jaamsim.BasicObjects;

import java.net.URI;

import com.jaamsim.Graphics.DisplayEntity;
import com.jaamsim.ProcessFlow.LinkedComponent;
import com.jaamsim.input.FileInput;
import com.jaamsim.input.Input;
import com.jaamsim.input.Keyword;

public abstract class FileToArray extends LinkedComponent {

	@Keyword(description = "A file containing numerical data, delimited by spaces or tabs.",
	         exampleList = {"'c:/test/data.txt'"})
	private final FileInput dataFile;

	{
		nextComponent.setRequired(false);

		dataFile = new FileInput("DataFile", "Key Inputs", null);
		dataFile.setRequired(true);
		this.addInput(dataFile);
	}

	public FileToArray() {}

	@Override
	public void updateForInput(Input<?> in) {
		super.updateForInput(in);

		if (in == dataFile) {
			if (dataFile.getValue() == null) {
				clearValue();
				return;
			}
			setValueForURI(dataFile.getValue());
			return;
		}
	}

	@Override
	public void addEntity(DisplayEntity ent) {
		super.addEntity(ent);
		setValueForURI(dataFile.getValue());
		sendToNextComponent(ent);
	}

	protected abstract void setValueForURI(URI uri);
	protected abstract void clearValue();

}
