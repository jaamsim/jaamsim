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

import java.io.File;
import java.net.URI;
import java.util.ArrayList;


public class DirInput extends Input<URI> {
	public DirInput(String key, String cat, URI def) {
		super(key, cat, def);
	}

	@Override
	public void parse(KeywordIndex kw)
	throws InputErrorException {
		URI temp = Input.parseURI(kw);
		try {
			File f = new File(temp);
			if (f.exists() && !f.isDirectory())
				throw new InputErrorException("File Entity parse error: %s is not a directory", kw.getArg(0));
		}
		catch (IllegalArgumentException e) {
			throw new InputErrorException("Unable to parse the directory:\n%s", kw.getArg(0));
		}

		value = temp;
	}

	@Override
	public void getValueTokens(ArrayList<String> toks) {
		if (value == null) return;

		toks.add(InputAgent.getRelativeFilePath(value));
	}

	public File getDir() {
		if (value == null) {
			return null;
		}

		try {
			File f = new File(value);
			return f;
		}
		catch (IllegalArgumentException e) {}

		return null;
	}
}
