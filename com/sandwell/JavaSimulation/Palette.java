/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2010-2011 Ausenco Engineering Canada Inc.
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

import java.util.ArrayList;

public class Palette extends Entity {
	private static final ArrayList<Palette> allInstances;

	private final ArrayList<Class<? extends Entity>> classList;

	// Part of the name of the chm help file for the palette
	private final StringInput helpFilePrefix;

	// The name of the htm file containing keywords for the palette.
	// To find the htm file, open the chm file in 7zip
	private final StringInput helpSection;

	static {
		allInstances = new ArrayList<Palette>();
	}

	{
		helpFilePrefix = new StringInput("HelpFile", "Help", "Java Simulation Environment");
		this.addInput(helpFilePrefix, true);

		helpSection = new StringInput("HelpSection", "Help", null);
		this.addInput(helpSection, true);
	}

	public Palette() {
		allInstances.add(this);
		classList = new ArrayList<Class<? extends Entity>>();
	}

	public static ArrayList<Palette> getAll() {
		return allInstances;
	}

	public void kill() {
		super.kill();
		allInstances.remove(this);
	}

	public String getFilePrefix() {
		return helpFilePrefix.getValue();
	}

	public String getHelpSection() {
		if( helpSection == null ) {
			return null;
		}
		else {
			return helpSection.getValue();
		}
	}

	public void addClass(Class<? extends Entity> proto) {
		classList.add(proto);
	}

	public ArrayList<Class<? extends Entity>> getClasses() {
		return classList;
	}
}
