/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2010-2011 Ausenco Engineering Canada Inc.
 * Copyright (C) 2021-2022 JaamSim Software Inc.
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

import com.jaamsim.basicsim.Entity;
import com.jaamsim.basicsim.JaamSimModel;


public class StringChoiceInput extends IntegerInput {
	private ArrayList<String> choices;

	{
		choices = new ArrayList<>();
	}

	public StringChoiceInput(String key, String cat, Integer def) {
		super(key, cat, def);
	}

	@Override
	public void parse(Entity thisEnt, KeywordIndex kw)
	throws InputErrorException {
		Input.assertCount(kw, 1);
		String temp = Input.parseString(kw.getArg(0), choices);
		value = choices.indexOf( temp );
	}

	public void addChoice(String choice) {
		if (!choices.contains(choice))
			choices.add(choice);
	}

	public String getChoice() {
		return choices.get(getValue());
	}

	public String getDefaultChoice() {
		if (defValue.intValue() == -1)
			return "";
		return choices.get(defValue);
	}

	public void setChoices(ArrayList<String> list) {
		choices = list;
	}

	@Override
	public ArrayList<String> getValidOptions(Entity ent) {
		return choices;
	}

	@Override
	public String getDefaultString(JaamSimModel simModel) {
		if (getDefaultChoice().isEmpty())
			return "";

		return String.format("%s", getDefaultChoice());
	}
}
