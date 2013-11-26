/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2011 Ausenco Engineering Canada Inc.
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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;

import com.jaamsim.input.InputAgent;

public class FileInput extends Input<URI> {
	private String[] validExtensions;

	public FileInput(String key, String cat, URI def) {
		super(key, cat, def);
		validExtensions = null;
	}

	@Override
	public void parse(StringVector input, Input.ParseContext context)
	throws InputErrorException {
		Input.assertCount(input, 1);
		URI temp = null;
		try {
			if (context != null)
				temp = InputAgent.getFileURI(context.context, input.get(0), context.jail);
			else
				temp = InputAgent.getFileURI(null, input.get(0), null);

		}
		catch (URISyntaxException ex) {
			throw new InputErrorException("File Entity parse error: %s", ex.getMessage());
		}

		if (temp == null)
			throw new InputErrorException("Unable to parse a valid file");

		if (!isValidExtension(temp))
			throw new InputErrorException("Invalid file extension, valid extensions are: " + Arrays.toString(validExtensions));
		value = temp;
	}

	public FileEntity getFileEntity(int io_status, boolean append) {
		return new FileEntity(value, io_status, append);
	}

	public void setValidExtensions(String... ext) {
		validExtensions = ext;
	}


	private String getFileExtention(URI u) {
		String name = u.toString();
		int idx = name.lastIndexOf(".");
		if (idx < 0)
			return "";

		return name.substring(idx + 1).trim();
	}

	private boolean isValidExtension(URI u) {
		if (validExtensions == null)
			return true;

		String ext = getFileExtention(u);
		for (String val : validExtensions) {
			if (val.equalsIgnoreCase(ext))
				return true;
		}

		return false;
	}

	@Override
	public void parse(StringVector input) throws InputErrorException {
		throw new InputErrorException("FileInput.parse() deprecated method called.");
	}
}
