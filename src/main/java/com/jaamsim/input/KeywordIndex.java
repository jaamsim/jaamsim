/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2014 Ausenco Engineering Canada Inc.
 * Copyright (C) 2019-2020 JaamSim Software Inc.
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
import java.util.Arrays;


public class KeywordIndex {
	private final ArrayList<String> input;
	public final String keyword;
	private final int start;
	private final int end;
	public final ParseContext context;

	public KeywordIndex(String word, ArrayList<String> arg, ParseContext ctxt) {
		this(word, arg, 0, arg.size(), ctxt);
	}

	public KeywordIndex(KeywordIndex kw, int s) {
		this(kw.keyword, kw.input, kw.start + s, kw.end, kw.context);
	}

	public KeywordIndex(KeywordIndex kw, int s, int e) {
		this(kw.keyword, kw.input, kw.start + s, kw.start + e, kw.context);
	}

	public KeywordIndex(String word, ArrayList<String> inp, int s, int e, ParseContext ctxt) {
		input = inp;
		keyword = word;
		start = s;
		end = e;
		context = ctxt;
	}

	public int numArgs() {
		return end - start;
	}

	public String argString() {
		ArrayList<String> tokens = new ArrayList<>(Arrays.asList(getArgArray()));
		return Input.getValueString(tokens, false);
	}

	public String[] getArgArray() {
		String[] ret = new String[end - start];
		for (int i = start; i < end; i++) {
			ret[i - start] = this.input.get(i);
		}
		return ret;
	}

	public String getArg(int index) {
		if (index < 0 || index >= numArgs())
			throw new IndexOutOfBoundsException("Index out of range:" + index);
		return input.get(start + index);
	}

	public ArrayList<KeywordIndex> getSubArgs() {
		Input.assertBracesMatch(this);
		ArrayList<KeywordIndex> subArgs = new ArrayList<>();
		for (int i= 0; i < this.numArgs(); i++) {
			//skip over opening brace if present
			if (this.getArg(i).equals("{"))
				i++;

			//iterate until closing brace, or end of entry
			int subArgStart = i;
			int subArgEnd = i;
			for (int j = i; j < this.numArgs(); j++, i++){
				if (this.getArg(j).equals("}")) {
					break;
				}

				subArgEnd++;
			}

			KeywordIndex subArg = new KeywordIndex(keyword, this.input, subArgStart + start, subArgEnd + start, context);
			subArgs.add(subArg);
		}

		return subArgs;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof KeywordIndex))
			return false;

		KeywordIndex kw = (KeywordIndex) obj;
		if (context == null && kw.context != null)
			return false;
		if (context != null && !context.equals(kw.context))
			return false;

		return input.equals(kw.input) && keyword.equals(kw.keyword)
				&& start == kw.start && end == kw.end;
    }

	@Override
	public String toString() {
		return input.subList(start, end).toString();
	}

}
