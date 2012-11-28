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
package com.jaamsim.observers;

import java.net.MalformedURLException;
import java.net.URL;

import com.sandwell.JavaSimulation.Entity;
import com.sandwell.JavaSimulation.StringInput;
import com.sandwell.JavaSimulation.Util;
import com.sandwell.JavaSimulation3D.InputAgent;

public class ImageObserverProto extends ObserverProto {

	private final StringInput imageNameInput;

	public ImageObserverProto() {
		imageNameInput = new StringInput("Image", "Key Inputs", "");
		addInput(imageNameInput, true);
	}

	@Override
	public Observer instantiate() {
		return instantiateImp(observeeInput.getValue());
	}

	@Override
	public Observer instantiate(Entity observee) {
		return instantiateImp(observee);
	}


	private Observer instantiateImp(Entity observee) {
		try {
			URL imageURL = new URL(Util.getAbsoluteFilePath(imageNameInput.getValue()));
			return new ImageObserver(observee, imageURL);
		}
		catch (MalformedURLException e) {
			// Could not instantiate a proper observer, log it
			InputAgent.logError("Could not find image: %s", imageNameInput.getValue());
			return new NullObserver(observee);
		}

	}
}
