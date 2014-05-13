/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2014 Ausenco Engineering Canada Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 */
package com.jaamsim.input;

import java.util.ArrayList;

import com.jaamsim.input.Input.ParseContext;

public class KeywordIndex {
	public final ArrayList<String> input;
	public final String keyword;
	public final int start;
	public final int end;
	public final ParseContext context;

	public KeywordIndex(ArrayList<String> inp, int s, int e, ParseContext ctxt) {
		input = inp;
		keyword = input.get(s);
		start = s;
		end = e;
		context = ctxt;
	}

	public int numArgs() {
		return end - start - 2;
	}

	public String argString() {
		StringBuilder sb = new StringBuilder();
		for (int i = start + 2; i < end; i++) {
			String dat = this.input.get(i);
			if (i > start + 2)
				sb.append("  ");

			if (Parser.needsQuoting(dat) && !dat.equals("{") && !dat.equals("}"))
				sb.append("'").append(dat).append("'");
			else
				sb.append(dat);
		}
		return sb.toString();
	}

	public String getArg(int index) {
		if (index < 0 || index >= numArgs())
			throw new IndexOutOfBoundsException("Index out of range:" + index);
		return input.get(start + index + 2);
	}
}
