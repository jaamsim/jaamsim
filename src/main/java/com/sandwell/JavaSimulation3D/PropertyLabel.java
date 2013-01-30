/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2009-2011 Ausenco Engineering Canada Inc.
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
package com.sandwell.JavaSimulation3D;


public class PropertyLabel extends TextLabel  {
	private PropertyReader propReader;

	{
		propReader = new PropertyReader();

		addInputGroup(propReader);
	}

	public PropertyLabel() {
	}

	@Override
	public String getRenderText(double time) {
		String val = propReader.getPropertyValueString(time);
		if (val.equals("")) {
			return super.getRenderText(time);
		}

		return val;
	}
}
