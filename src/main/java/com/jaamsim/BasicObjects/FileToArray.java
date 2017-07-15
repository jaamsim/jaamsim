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
import com.jaamsim.input.EnumListInput;
import com.jaamsim.input.ExpError;
import com.jaamsim.input.ExpEvaluator;
import com.jaamsim.input.ExpParser;
import com.jaamsim.input.ExpResult;
import com.jaamsim.input.FileInput;
import com.jaamsim.input.Input;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.Parser;
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

		dataFile = new FileInput("DataFile", "Key Inputs", null);
		dataFile.setRequired(true);
		this.addInput(dataFile);

		dataFormat = new EnumListInput<>(ValidFormats.class, "DataFormat", "Key Inputs", null);
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
		setValueForURI(dataFile.getValue());
	}

	@Override
	public void addEntity(DisplayEntity ent) {
		super.addEntity(ent);
		setValueForURI(dataFile.getValue());
		sendToNextComponent(ent);
	}

	protected ExpResult getExpResult(int i, String str)
	throws ExpError {

		if (dataFormat.getValue() != null && i < dataFormat.getListSize()) {
			switch (dataFormat.getValue().get(i)) {
			case TIMESTAMP:
				double time = 0.0d;
				try {
					time = Input.parseRFC8601DateTime(str)/1e6;
				}
				catch (Exception e) {
					throw new ExpError(str, 0, e.getMessage());
				}
				return ExpResult.makeNumResult(time, TimeUnit.class);
			case ENTITY:
				str = Parser.addEnclosure("[", str, "]");
				break;
			case STRING:
				str = Parser.addEnclosure("\"", str, "\"");
				break;
			case EXPRESSION:
				break;
			}
		}

		ExpEvaluator.EntityParseContext pc = ExpEvaluator.getParseContext(this, str);
		Expression exp = ExpParser.parseExpression(pc, str);
		return ExpEvaluator.evaluateExpression(exp, 0.0d);
	}

	protected abstract void setValueForURI(URI uri);
	protected abstract void clearValue();

}
