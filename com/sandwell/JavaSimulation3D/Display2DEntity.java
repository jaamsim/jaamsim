/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2003-2011 Ausenco Engineering Canada Inc.
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

import java.awt.Container;
import java.util.ArrayList;

import com.sandwell.JavaSimulation.Entity;
import com.sandwell.JavaSimulation.Vector;

import javax.swing.JComponent;

/**
 * Abstract class that encapsulates the methods and data needed to display a
 * simulation object in the 2D environment. Extends the basic functionality
 * entity in order to have access to the basic system components like the
 * eventManager. These objects are intended to be displayed in Swing graphics
 * panels and windows. Display2DEntity objects can be displayed in many
 * containers.
 * <p>
 * For 3-Dimensional objects see DisplayEntity.
 */
public class Display2DEntity extends Entity {
	private static final ArrayList<Display2DEntity> allInstances;

	protected Region currentRegion;
	protected JComponent displayModel; // Swing graphics model for the entity

	static {
		allInstances = new ArrayList<Display2DEntity>();
	}
	public Display2DEntity() {
		allInstances.add(this);
		// Build the branchGraph
		displayModel = null;
		currentRegion = DisplayEntity.simulation.getDefaultRegion();
	}

	public static ArrayList<Display2DEntity> getAll() {
		return allInstances;
	}

	public void kill() {
		super.kill();
		allInstances.remove(this);
		exitRegion();
	}

	public Region getCurrentRegion() {
		return currentRegion;
	}

	public void setRegion( Region newRegion ) {
		exitRegion();
		currentRegion = newRegion;
	}

	public void enterRegion() {
		if (currentRegion.getStatusBar() == null)
			return;

		if (displayModel != null)
			currentRegion.getStatusBar().add(displayModel);
	}

	public void exitRegion() {
		if (displayModel == null)
			return;

		Container container = displayModel.getParent();
		if (container == null)
			return;

		container.remove(displayModel);
	}

	public void render(double time) {}

	public Vector getInfo() {
		Vector info = super.getInfo();
		info.addElement( "Region" + "\t" + getCurrentRegion().getName() );
		return info;
	}

	/**
	 *  Accessor method to obtain the display model for the Display2DEntity
	 */
	public JComponent getModel() {
		return displayModel;
	}
}
