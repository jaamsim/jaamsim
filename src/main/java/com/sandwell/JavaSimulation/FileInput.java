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

import com.jaamsim.input.InputAgent;

public class FileInput extends Input<URI> {

	public FileInput(String key, String cat, URI def) {
		super(key, cat, def);
	}

	@Override
	public void parse(StringVector input)
	throws InputErrorException {
		Input.assertCount(input, 1);
		value = InputAgent.getFileURI(input.get(0));
	}

	public FileEntity getFileEntity(int io_status, boolean append) {
		return new FileEntity(value, io_status, append);
	}
}
