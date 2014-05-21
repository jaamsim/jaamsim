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

import java.util.ArrayList;

import com.jaamsim.DisplayModels.DisplayModel;
import com.jaamsim.input.Keyword;

public class ObjectType extends Entity {
	private static final ArrayList<ObjectType> allInstances;

	@Keyword(description = "The java class of the object type",
	         example = "This is placeholder example text")
	private final ClassInput javaClass;

	@Keyword(description = "The package to which the object type belongs",
	         example = "This is placeholder example text")
	private final StringInput palette;

	@Keyword(description = "Only for DisplayEntity",
	         example = "This is placeholder example text")
	private final EntityInput<DisplayModel> defaultDisplayModel;

	@Keyword(description = "This is placeholder description text",
	         example = "This is placeholder example text")
	private final BooleanInput dragAndDrop;

	static {
		allInstances = new ArrayList<ObjectType>();
	}

	{
		javaClass = new ClassInput( "JavaClass", "Key Inputs", null );
		this.addInput( javaClass );

		palette = new StringInput("Palette", "Key Inputs", null);
		this.addInput( palette );

		defaultDisplayModel = new EntityInput<DisplayModel>(DisplayModel.class, "DefaultDisplayModel", "Key Inputs", null);
		this.addInput(defaultDisplayModel);

		dragAndDrop = new BooleanInput("DragAndDrop", "Key inputs", true);
		this.addInput(dragAndDrop);
	}

	public ObjectType() {
		synchronized (allInstances) {
			allInstances.add(this);
		}
	}

	public static ArrayList<ObjectType> getAll() {
		synchronized (allInstances) {
			return allInstances;
		}
	}

	@Override
	public void kill() {
		super.kill();
		allInstances.remove(this);
	}

	public static ObjectType getFor(Class<? extends Entity> jClass) {
		synchronized (allInstances) {
			ObjectType type = null;
			for(ObjectType each: allInstances) {
				if(each.getJavaClass() == jClass) {
					type = each;
					break;
				}
			}
			return type;
		}
	}

	public Class<? extends Entity> getJavaClass() {
		return javaClass.getValue();
	}

	public String getPaletteName() {
		String s = palette.getValue();
		if (s != null)
			return s;

		return "Default";
	}

	public DisplayModel getDefaultDisplayModel(){
		return defaultDisplayModel.getValue();
	}

	public boolean isDragAndDrop() {
		return dragAndDrop.getValue();
	}
}
