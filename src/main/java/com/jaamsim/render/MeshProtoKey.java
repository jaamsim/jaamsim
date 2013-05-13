/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2012 Ausenco Engineering Canada Inc.
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
package com.jaamsim.render;

import java.net.URL;

/**
 * Key for a mesh prototype
 * @author Matt.Chudleigh
 *
 */
public class MeshProtoKey {
	private URL _fileURL;
	private String _urlString; // Use this string for comparison purposes

	public MeshProtoKey(URL url) {
		_fileURL = url;
		_urlString = url.toString();
	}

	public URL getURL() {
		return _fileURL;
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof MeshProtoKey)) {
			return false;
		}
		return ((MeshProtoKey)o)._urlString.equals(_urlString);
	}

	@Override
	public int hashCode() {
		return _urlString.hashCode();
	}
}
