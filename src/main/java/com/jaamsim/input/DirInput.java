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

public class DirInput extends StringInput {
	private URI dir;

	public DirInput(String key, String cat, String def) {
		super(key, cat, def);
		dir = null;
	}

	@Override
	public void parse(KeywordIndex kw)
	throws InputErrorException {
		Input.assertCount(kw, 1);

		URI temp = Input.parseURI(kw);
		try {
			File f = new File(temp);
			if (f.exists() && !f.isDirectory())
				throw new InputErrorException("File Entity parse error: %s is not a directory", kw.getArg(0));
		}
		catch (IllegalArgumentException e) {
			throw new InputErrorException("Unable to parse the directory:\n%s", kw.getArg(0));
		}

		value = kw.getArg(0);
		dir = temp;
	}

	public File getDir() {
		if (dir == null) {
			return null;
		}

		try {
			File f = new File(dir);
			return f;
		}
		catch (IllegalArgumentException e) {}

		return null;
	}
}
