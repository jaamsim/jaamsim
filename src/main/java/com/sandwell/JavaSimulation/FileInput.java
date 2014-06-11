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

import javax.swing.filechooser.FileNameExtensionFilter;

import com.jaamsim.input.Input;
import com.jaamsim.input.InputAgent;
import com.jaamsim.input.KeywordIndex;
import com.jaamsim.input.Parser;

public class FileInput extends Input<URI> {
	private String fileType;  // the type of file, e.g. "Image" or "3D"
	private String[] validFileExtensions;  // supported file extensions
	private String[] validFileDescriptions;  // description of each supported file extension

	public FileInput(String key, String cat, URI def) {
		super(key, cat, def);
		fileType = null;
		validFileExtensions = null;
		validFileDescriptions = null;
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

		if (!temp.isOpaque() && temp.getPath() == null)
			 throw new InputErrorException("Unable to parse the file path:\n%s", arg);

		// Confirm that the file exists
		if (!InputAgent.fileExists(temp))
			throw new InputErrorException("The specified file does not exist.\n" +
					"File path = %s", arg);

		if (!isValidExtension(temp))
			throw new InputErrorException("Invalid file extension: %s.\nValid extensions are: %s",
					temp.getPath(), Arrays.toString(validFileExtensions));

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

	/**
	 * Set the file type description for this file input.
	 *
	 * @param type - description of the file type, for example "Image" or "3D".
	 */
	public void setFileType(String type) {
		fileType = type;
	}

	/**
	 * Sets the list of supported file extensions for this file input.
	 *
	 * @param ext - array of supported file extensions.
	 */
	public void setValidFileExtensions(String... ext) {
		validFileExtensions = ext;
	}

	/**
	 * Sets the list of descriptions for the supported file extensions.
	 *
	 * @param desc - array of descriptions for the supported file extensions.
	 */
	public void setValidFileDescriptions(String... desc) {
		validFileDescriptions = desc;
	}

	private String getFileExtention(URI u) {
		String name = u.toString();
		int idx = name.lastIndexOf(".");
		if (idx < 0)
			return "";

		return name.substring(idx + 1).trim();
	}

	private boolean isValidExtension(URI u) {
		if (validFileExtensions == null)
			return true;

		String ext = getFileExtention(u);
		for (String val : validFileExtensions) {
			if (val.equalsIgnoreCase(ext))
				return true;
		}

		return false;
	}

	/**
	 * Returns an array of file name extension filters, one for each of the
	 * supported file types.
	 *
	 * @return an array of file extension filters.
	 */
	public FileNameExtensionFilter[] getFileNameExtensionFilters() {
		return getFileNameExtensionFilters(fileType, validFileExtensions, validFileDescriptions);
	}

	/**
	 * Returns an array of file name extension filters, one for each of the
	 * supported file types.
	 *
	 * @param fileExt - the valid file extension for each type of file.
	 * @param fileDesc - the description field for each type of file.
	 * @return an array of file extension filters.
	 */
	public static FileNameExtensionFilter[] getFileNameExtensionFilters(String type, String[] fileExt, String[] fileDesc) {
		FileNameExtensionFilter typeFilter = null;
		if (type != null && fileExt != null) {
			StringBuilder desc = new StringBuilder(45);
			desc.append("All Supported ").append(type).append(" Files (");

			for (int i = 0; i < fileExt.length; i++) {
				if (i > 0)
					desc.append("; ");
				desc.append("*.").append(fileExt[i].toLowerCase());
			}
			desc.append(")");

			typeFilter = new FileNameExtensionFilter(desc.toString(), fileExt);
		}

		FileNameExtensionFilter[] temp = null;
		if (fileExt != null && fileDesc != null) {
			temp = new FileNameExtensionFilter[fileExt.length];
			for (int i = 0; i < fileExt.length; i++) {
				temp[i] = new FileNameExtensionFilter(fileDesc[i], fileExt[i]);
			}
		}

		int len = 0;
		if (typeFilter != null)
			len += 1;

		if (temp != null)
			len += temp.length;

		FileNameExtensionFilter[] ret = new FileNameExtensionFilter[len];

		int idx = 0;
		if (typeFilter != null) {
			ret[idx] = typeFilter;
			idx++;
		}

		if (temp != null) {
			for (int i = 0; i < temp.length; i++) {
				ret[idx] = temp[i];
				idx++;
			}
		}

		return ret;
	}
}
