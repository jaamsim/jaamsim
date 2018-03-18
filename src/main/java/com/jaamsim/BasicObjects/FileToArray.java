/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2017-2018 JaamSim Software Inc.
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
import com.jaamsim.basicsim.Entity;
import com.jaamsim.input.EnumListInput;
import com.jaamsim.input.ExpError;
import com.jaamsim.input.ExpEvaluator;
import com.jaamsim.input.ExpParser;
import com.jaamsim.input.ExpResult;
import com.jaamsim.input.FileInput;
import com.jaamsim.input.Input;
import com.jaamsim.input.Keyword;
import com.jaamsim.units.TimeUnit;
import com.jaamsim.input.ExpParser.Expression;

public abstract class FileToArray extends LinkedComponent {

	enum ValidFormats {
		TIMESTAMP,
		ENTITY,
		STRING,
		EXPRESSION,
	}

	@Keyword(description = "A file containing entries that are delimited by spaces and/or tabs.",
	         exampleList = {"'c:/test/data.txt'"})
	private final FileInput dataFile;

	@Keyword(description = "An optional list of data types that describe the entries in each "
	                     + "column of the data file. The data type inputs cause the data file "
	                     + "entries to be processed as follows:\n"
	                     + "- TIMESTAMP: parse the data entry as a time stamp in "
	                     + "'YYYY-MM-DD HH:MM:SS.SSS' or YYYY-MM-DDTHH:MM:SS.SSS format.\n"
	                     + "- ENTITY: add square bracket around the data entry and parse it "
	                     + "as an entity name.\n"
	                     + "- STRING: add double quotes around the data entry and parse it "
	                     + "as an string.\n"
	                     + "- EXPRESSION: parse the data entry as an expression.",
	         exampleList = { "TIMESTAMP ENTITY STRING STRING" })
	private final EnumListInput<ValidFormats> dataFormat;

	{
		nextComponent.setRequired(false);

		dataFile = new FileInput("DataFile", KEY_INPUTS, null);
		dataFile.setRequired(true);
		this.addInput(dataFile);

		dataFormat = new EnumListInput<>(ValidFormats.class, "DataFormat", KEY_INPUTS, null);
		dataFormat.setDefaultText("EXPRESSION");
		this.addInput(dataFormat);
	}

	public FileToArray() {}

	@Override
	public void updateForInput(Input<?> in) {
		super.updateForInput(in);

		if (in == dataFile) {
			if (dataFile.getValue() == null) {
				clearValue();
			}
			return;
		}
	}

	@Override
	public void earlyInit() {
		super.earlyInit();
		setValueForURI(dataFile.getValue(), 0.0d);
	}

	@Override
	public void addEntity(DisplayEntity ent) {
		super.addEntity(ent);
		setValueForURI(dataFile.getValue(), getSimTime());
		sendToNextComponent(ent);
	}

	protected ExpResult getExpResult(int i, String str, double simTime) {

		// Is the entry a time stamp?
		if (Input.isRFC8601DateTime(str)) {
			try {
				double time = Input.parseRFC8601DateTime(str)/1e6;
				return ExpResult.makeNumResult(time, TimeUnit.class);
			}
			catch (Exception e) {}
		}

		// Is the entry an entity?
		Entity ent = Entity.getNamedEntity(str);
		if (ent != null) {
			return ExpResult.makeEntityResult(ent);
		}

		// Is the entry a valid expression?
		try {
			ExpEvaluator.EntityParseContext pc = ExpEvaluator.getParseContext(this, str);
			Expression exp = ExpParser.parseExpression(pc, str);
			return ExpEvaluator.evaluateExpression(exp, simTime);
		}
		catch (ExpError e) {}

		// If all else fails, return a string
		return ExpResult.makeStringResult(str);
	}

	protected abstract void setValueForURI(URI uri, double simTime);
	protected abstract void clearValue();

}
