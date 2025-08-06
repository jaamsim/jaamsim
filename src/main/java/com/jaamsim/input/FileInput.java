/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2011 Ausenco Engineering Canada Inc.
 * Copyright (C) 2018-2021 JaamSim Software Inc.
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;

import javax.swing.filechooser.FileNameExtensionFilter;

import com.jaamsim.basicsim.Entity;


public class FileInput extends Input<URI> {
	private String fileType;  // the type of file, e.g. "Image" or "3D"
	private String[] validFileExtensions;  // supported file extensions
	private String[] validFileDescriptions;  // description of each supported file extension
	private Entity ent;

	public FileInput(String key, String cat, URI def) {
		super(key, cat, def);
		fileType = null;
		validFileExtensions = null;
		validFileDescriptions = null;
	}

	@Override
	public String applyConditioning(String str) {
		return Parser.addQuotesIfNeeded(str);
	}

	@Override
	public void parse(Entity thisEnt, KeywordIndex kw)
	throws InputErrorException {
		URI temp = Input.parseURI(thisEnt.getJaamSimModel(), kw);

		// Confirm that the file exists
		if (!InputAgent.fileExists(temp))
			throw new InputErrorException("The specified file does not exist.\n"
					+ "File path = %s\n"
					+ "URI=%s", kw.getArg(0), temp);

		if (!isValidExtension(temp))
			throw new InputErrorException("Invalid file extension: %s.\nValid extensions are: %s",
					temp.getPath(), Arrays.toString(validFileExtensions));

		ent = thisEnt;
		value = temp;
	}

	@Override
	public String getValidInputDesc() {
		return Input.VALID_FILE;
	}

	@Override
	public void getValueTokens(ArrayList<String> toks) {
		if (value == null || ent == null || isDef)
			return;

		toks.add(InputAgent.getRelativeFilePath(ent.getJaamSimModel(), value));
	}

	public static ArrayList<ArrayList<String>> getTokensFromURI(URI uri){

		ArrayList<ArrayList<String>> tokens = new ArrayList<>();
		ArrayList<String> rec = new ArrayList<>();

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
				rec = new ArrayList<>();
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
		int idx = name.lastIndexOf('.');
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
