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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;

import com.jaamsim.input.InputAgent;
import com.jaamsim.input.Parser;

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
		if (value != null)
			return InputAgent.getRelativeFilePath(value);
		else
			return "";
	}



	public static ArrayList<ArrayList<String>> getTokensFromURI(URI uri){

		ArrayList<ArrayList<String>> tokens = new ArrayList<ArrayList<String>>();
		ArrayList<String> rec = new ArrayList<String>();

		BufferedReader b = null;
		try {
			InputStream r = uri.toURL().openStream();
			b = new BufferedReader(new InputStreamReader(r));

			while (true) {
				String line = null;
				line = b.readLine();

				if (line == null)
					break;

				Parser.tokenize(rec, line, true);
				if (rec.size() == 0)
					continue;

				tokens.add(rec);
				rec = new ArrayList<String>();
			}
			b.close();
			return tokens;
		}
		catch (MalformedURLException e) {}
		catch (IOException e) {
			try {
				if (b != null) b.close();
			}
			catch (IOException e2) {}
		}

		return null;

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
