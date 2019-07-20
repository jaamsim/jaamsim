/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2017-2019 JaamSim Software Inc.
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
import java.util.ArrayList;

import com.jaamsim.Graphics.DisplayEntity;
import com.jaamsim.ProcessFlow.LinkedComponent;
import com.jaamsim.basicsim.Entity;
import com.jaamsim.input.ExpError;
import com.jaamsim.input.ExpEvaluator;
import com.jaamsim.input.ExpParser;
import com.jaamsim.input.ExpResult;
import com.jaamsim.input.FileInput;
import com.jaamsim.input.Input;
import com.jaamsim.input.Keyword;
import com.jaamsim.units.DimensionlessUnit;
import com.jaamsim.units.TimeUnit;
import com.jaamsim.input.ExpParser.Expression;

public abstract class FileToArray extends LinkedComponent {

	@Keyword(description = "A text file containing one or more records whose entries are "
	                     + "delimited by tabs and/or spaces.\n\n"
	                     + "The following types of entries can be used. "
	                     + "If an entry includes spaces, double quotation marks, or curly braces, "
	                     + "it must be enclosed by single quotation marks.\n"
	                     + "- Comments. Records that begin with a # symbol are ignored (e.g. # abc)\n"
	                     + "- Numbers with or without units, specified in expression format (e.g. 5.2[m])\n"
	                     + "- Strings (e.g. 'quick red fox' or quick_red_fox)\n"
	                     + "- Entity names (e.g. DisplayEntity1)\n"
	                     + "- Time stamps in YYYY-MM-DD HH:MM:SS.SSS or YYYY-MM-DDTHH:MM:SS.SSS format "
	                     + "(e.g. '2018-06-31 13:00:00.000' or 2018-06-31T13:00:00.000)\n"
	                     + "- Arrays of numbers, entities, strings, or arrays, specified in expression format "
	                     + "(e.g. '{ 5[m], \"abc\", [DisplayEntity1] }'\n"
	                     + "- Expressions. A valid expression is executed and saved when the file is read "
	                     + "(e.g. 1[m]/2[s] is saved as 1.0[m/s]). An invalid expression is saved as a string.",
	         exampleList = {"'c:/test/data.txt'"})
	private final FileInput dataFile;

	{
		nextComponent.setRequired(false);

		dataFile = new FileInput("DataFile", KEY_INPUTS, null);
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
			setValueForURI(dataFile.getValue(), 0.0d);
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

	protected ExpResult getExpResult(String str, double simTime) {

		// Is the entry a time stamp?
		if (Input.isRFC8601DateTime(str)) {
			try {
				double time = Input.parseRFC8601DateTime(str)/1e6;
				return ExpResult.makeNumResult(time, TimeUnit.class);
			}
			catch (Exception e) {}
		}

		// Is the entry an entity?
		Entity ent = getJaamSimModel().getNamedEntity(str);
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

	protected ArrayList<ExpResult> getExpResultList(ArrayList<String> list, double simTime) {
		ArrayList<ExpResult> ret = new ArrayList<>(list.size());
		for (String str : list) {
			ret.add(getExpResult(str, simTime));
		}
		return ret;
	}

	protected ArrayList<ExpResult> getExpResultList(ArrayList<Object> list) throws ExpError {
		ArrayList<ExpResult> ret = new ArrayList<>(list.size());
		for (Object obj : list) {
			ExpResult res = ExpEvaluator.getResultFromObject(obj, DimensionlessUnit.class);
			ret.add(res);
		}
		return ret;
	}

	protected abstract void setValueForURI(URI uri, double simTime);
	protected abstract void clearValue();

}
