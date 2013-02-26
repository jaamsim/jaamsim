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

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;

import com.sandwell.JavaSimulation.ObjectType;

public class TransferableObjectType implements Transferable {
	public final static DataFlavor OBJECT_TYPE_FLAVOR;
	private final ObjectType type;
	static {
		try {

			// Create OBJECT_TYPE_FLAVOR
			String objectTypeFlavor = DataFlavor.javaJVMLocalObjectMimeType +
			";class=" + TransferableObjectType.class.getName();
			OBJECT_TYPE_FLAVOR = new DataFlavor(objectTypeFlavor);
		} catch (ClassNotFoundException ex) {
			throw new RuntimeException(ex);
		}
	}

	public TransferableObjectType(ObjectType type) {
		this.type = type;
	}

	@Override
	public DataFlavor [] getTransferDataFlavors() {
		return new DataFlavor [] {OBJECT_TYPE_FLAVOR};
	}

	@Override
	public boolean isDataFlavorSupported(DataFlavor flavor) {
		return OBJECT_TYPE_FLAVOR.equals(flavor);
	}

	@Override
	public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
		if (flavor.equals(OBJECT_TYPE_FLAVOR)) {
			return type;
		} else {
			throw new UnsupportedFlavorException(flavor);
		}
	}

}
