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
package com.sandwell.JavaSimulation;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;

import com.jaamsim.input.Input;
import com.jaamsim.input.InputAgent;
import com.jaamsim.input.KeywordIndex;

public class DirInput extends Input<URI> {
	public DirInput(String key, String cat, URI def) {
		super(key, cat, def);
	}

	@Override
	public void parse(KeywordIndex kw)
	throws InputErrorException {
		Input.assertCount(kw, 1);

		String arg = kw.getArg(0);
		// Convert the file path to a URI
		URI temp = null;
		try {
			if (kw.context != null)
				temp = InputAgent.getFileURI(kw.context.context, arg, kw.context.jail);
			else
				temp = InputAgent.getFileURI(null, arg, null);
		}
		catch (URISyntaxException ex) {
			throw new InputErrorException("File Entity parse error: %s", ex.getMessage());
		}

		if (temp == null)
			throw new InputErrorException("Unable to parse the file path:\n%s", arg);

		try {
			File f = new File(temp);
			if (f.exists() && !f.isDirectory())
				throw new InputErrorException("File Entity parse error: %s is not a directory", arg);
		}
		catch (IllegalArgumentException e) {
			throw new InputErrorException("Unable to parse the directory:\n%s", arg);
		}

		value = temp;
	}

	@Override
	public String getValueString() {
		if (value != null)
			return InputAgent.getRelativeFilePath(value);
		else
			return "";
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
