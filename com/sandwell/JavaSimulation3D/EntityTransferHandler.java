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
package com.sandwell.JavaSimulation3D;

import java.awt.Point;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JRootPane;
import javax.swing.TransferHandler;
import javax.vecmath.Vector3d;

import com.sandwell.JavaSimulation.Entity;
import com.sandwell.JavaSimulation.ErrorException;
import com.sandwell.JavaSimulation.ObjectType;
import com.sandwell.JavaSimulation.Simulation;


public class EntityTransferHandler extends TransferHandler {


	public final boolean canImport(JComponent destination, DataFlavor [] flavors) {
		List<DataFlavor> flavorList = Arrays.asList(flavors);
		return flavorList.contains(TransferableObjectType.OBJECT_TYPE_FLAVOR);
	}

	/**
	 * Add the the dropped item to the Window
	 */
	public boolean importData(JComponent destination, Transferable transferable) {

		// Region of the new item
		Region region = ((Sim3DWindow)((JRootPane)destination).getParent()).getRegion();

		// OrbitBehavior of the window
		OrbitBehavior behavior = ((Sim3DWindow)((JRootPane)destination).getParent()).behavior;

		// Obtain the ObjectType
		ObjectType type = null;
		try {
			type = (ObjectType) transferable.getTransferData(TransferableObjectType.OBJECT_TYPE_FLAVOR);

		} catch (UnsupportedFlavorException e) {
			throw new ErrorException(e);
		} catch (IOException e) {
			throw new ErrorException(e);
		}

		// Determine the name of the entity based on its class name and
		// the first available integer number starting from 1
		int i = 1;
		Class<? extends Entity> proto  = type.getJavaClass();
		String name = proto.getSimpleName();
		while (Simulation.getNamedEntity(String.format("%s%d", name, i)) != null) {
			i++;
		}
		name = String.format("%s%d", name, i);

		// Create a new instance
		DisplayEntity dEntity  = (DisplayEntity) InputAgent.defineEntity(proto, name, true);

		// Dropped position on the window
		Point dropLocation = destination.getMousePosition();

		// Dropped position in the model universe
		Vector3d universeLocation = behavior.getUniversePointFromMouseLoc(dropLocation.x, dropLocation.y, OrbitBehavior.Plane.XY_PLANE, 0.0d);

		// Position the DisplayEntity
		dEntity.setPosition(universeLocation);
		dEntity.setAlignment(new Vector3d(0.0, 0.0, -0.5));

		// Enter region
		dEntity.initializeGraphics();
		dEntity.enterRegion(region);

		FrameBox.setSelectedEntity(dEntity);
		dEntity.updateInputPosition();
		dEntity.updateInputSize();
		dEntity.updateInputAlignment();
		return true;
	}
}
