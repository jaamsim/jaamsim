/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2011 Ausenco Engineering Canada Inc.
 * Copyright (C) 2019-2022 JaamSim Software Inc.
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


public class StringListInput extends ArrayListInput<String> {
	private ArrayList<String> validOptions;

	 // If true convert all the the items to uppercase
	private boolean caseSensitive;

	public StringListInput(String key, String cat, ArrayList<String> def) {
		super(key, cat, def);
		validOptions = null;
		caseSensitive = true;
	}

	@Override
	public void parse(Entity thisEnt, KeywordIndex kw)
	throws InputErrorException {

		// If adding to the list
		if (kw.getArg( 0 ).equals( "++" )) {
			ArrayList<String> input = new ArrayList<>(kw.numArgs()-1);
			for (int i = 1; i < kw.numArgs(); i++) {
				if (validOptions == null)
					input.add(kw.getArg(i));
				else
					input.add(Input.parseString(kw.getArg(i), validOptions, caseSensitive));
			}

			ArrayList<String> newValue;
			if (value == null)
				newValue = new ArrayList<>();
			else
				newValue = new ArrayList<>( value );

			Input.assertCountRange(input, 0, maxCount - newValue.size());

			newValue.addAll( input );
			value = newValue;
		}
		// If removing from the list
		else if (kw.getArg( 0 ).equals( "--" )) {
			ArrayList<String> input = new ArrayList<>(kw.numArgs()-1);
			for (int i = 1; i < kw.numArgs(); i++) {
				if (validOptions == null)
					input.add(kw.getArg(i));
				else
					input.add(Input.parseString(kw.getArg(i), validOptions, caseSensitive));
			}

			Input.assertCountRange(input, 0, value.size() - minCount );

			ArrayList<String> newValue = new ArrayList<>( value );
			for (String val : input) {
				if (! newValue.contains( val ))
					InputAgent.logWarning(thisEnt.getJaamSimModel(),
							"Could not remove " + val + " from " + this.getKeyword() );
				newValue.remove( val );
			}
			value = newValue;
		}
		// Otherwise, just set the list normally
		else {
			Input.assertCountRange(kw, minCount, maxCount);
			if (validOptions != null) {
				value = Input.parseStrings(kw, validOptions, caseSensitive);
				return;
			}

			ArrayList<String> tmp = new ArrayList<>(kw.numArgs());
			for (int i = 0; i < kw.numArgs(); i++) {
				tmp.add(kw.getArg(i));
			}
			value = tmp;
		}
	}

	@Override
	public void setTokens(KeywordIndex kw) {
		isDef = false;

		String[] args = kw.getArgArray();
		if (args.length > 0) {

			// Consider the following input case:
			// Object1 Keyword1 { ++ String1 ...
			if (args[0].equals( "++" )) {
				this.addTokens(args);
				return;
			}

			// Consider the following input case:
			// Object1 Keyword1 { -- String1 ...
			if (args[0].equals( "--" )) {
				if (this.removeTokens(args))
					return;
			}
		}

		valueTokens = args;
	}

	public void setValidOptions(ArrayList<String> list) {
		validOptions = list;
	}

	public void setCaseSensitive(boolean bool) {
		caseSensitive = bool;
	}

	public boolean getCaseSensitive() {
		return caseSensitive;
	}

	@Override
	public ArrayList<String> getValidOptions(Entity ent) {
		return validOptions;
	}

	@Override
	public String getDefaultString(JaamSimModel simModel) {
		if (defValue == null || defValue.isEmpty())
			return "";

		StringBuilder tmp = new StringBuilder(defValue.get(0));
		for (int i = 1; i < defValue.size(); i++) {
			tmp.append(SEPARATOR);
			tmp.append(defValue.get(i));
		}

		return tmp.toString();
	}
}