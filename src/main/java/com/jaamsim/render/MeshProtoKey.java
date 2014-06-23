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

import java.net.URI;

/**
 * Key for a mesh prototype
 * @author Matt.Chudleigh
 *
 */
public class MeshProtoKey {
	private URI _fileURI;
	private String _uriString; // Use this string for comparison purposes

	public MeshProtoKey(URI uri) {
		_fileURI = uri;
		_uriString = uri.toString();
	}

	public URI getURI() {
		return _fileURI;
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof MeshProtoKey)) {
			return false;
		}
		return ((MeshProtoKey)o)._uriString.equals(_uriString);
	}

	@Override
	public int hashCode() {
		return _uriString.hashCode();
	}
}
