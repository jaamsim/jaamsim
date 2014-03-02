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

		// Convert the file path to a URI
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
			throw new InputErrorException("Unable to parse the file path:\n%s", input.get(0));

		if (!temp.isOpaque() && temp.getPath() == null)
			 throw new InputErrorException("Unable to parse the file path:\n%s", input.get(0));

		// Confirm that the file exists
		if (!InputAgent.fileExists(temp))
			throw new InputErrorException("The specified file does not exist.\n" +
					"File path = %s", input.get(0));

		if (!isValidExtension(temp))
			throw new InputErrorException("Invalid file extension: %s.\nValid extensions are: %s",
					temp.getPath(), Arrays.toString(validExtensions));

		value = temp;
	}

	@Override
	public String getValueString() {
		return InputAgent.getRelativeFilePath(value);
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

	/**
	 * Returns a String containing the valid file extensions suitable for use
	 * with FileDialog.  For example, "*.png; *.jpg".
	 *
	 * @return - String containing the valid extensions.
	 */
	public String getValidExtensionsString() {
		StringBuilder validString = new StringBuilder(45);
		for( int i=0; i<validExtensions.length; i++) {
			validString.append("*.").append(validExtensions[i]);
			if(i < validExtensions.length - 1)
				validString.append("; ");
		}
		return validString.toString();
	}
}
