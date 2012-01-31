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

import com.sandwell.JavaSimulation3D.DisplayEntity;
import com.sandwell.JavaSimulation3D.DisplayModel;
import com.sandwell.JavaSimulation3D.InputAgent;

public class ObjectType extends Entity {
	private static final ArrayList<ObjectType> allInstances;

	private final ClassInput javaClass; // the java class of the object type
	private final EntityInput<Package> javaPackage; // the package to which the object type belongs
	private final EntityInput<DisplayModel> defaultDisplayModel; // Only for DisplayEntity
	private final BooleanInput dragAndDrop;

	static {
		allInstances = new ArrayList<ObjectType>();
	}

	{
		javaClass = new ClassInput( "JavaClass", "Key Inputs", null );
		this.addInput( javaClass, true );

		javaPackage = new EntityInput<Package>( Package.class, "Package", "Key Inputs", null );
		this.addInput( javaPackage, true );

		defaultDisplayModel = new EntityInput<DisplayModel>(DisplayModel.class, "DefaultDisplayModel", "Key Inputs", null);
		this.addInput(defaultDisplayModel, true);

		dragAndDrop = new BooleanInput("DragAndDrop", "Key inputs", true);
		this.addInput(dragAndDrop, true);
	}

	public ObjectType() {
		allInstances.add(this);
	}

	public static ArrayList<ObjectType> getAll() {
		return allInstances;
	}

	public static ObjectType getFor(Class<? extends Entity> jClass) {
		ObjectType type = null;
		for(ObjectType each: allInstances) {
			if(each.getJavaClass() == jClass) {
				type = each;
				break;
			}
		}
		return type;
	}

	public void kill() {
		super.kill();
		allInstances.remove(this);
	}

	public Class<? extends Entity> getJavaClass() {
		return javaClass.getValue();
	}

	/**
	 * Returns an instance of this javaClass with a meaningful name
	 * @return
	 */
	public Entity getNewInstance() {
		Entity newEntity;
		try {
			newEntity = getJavaClass().newInstance();
		} catch (InstantiationException e) {
			throw new ErrorException(e);
		} catch (IllegalAccessException e) {
			throw new ErrorException(e);
		}

		// Determine the name of the entity based on its class name and
		// the first available integer number starting from 1
		int i = 1;
		String name = getJavaClass().getSimpleName();
		while (Simulation.getNamedEntity(String.format("%s%d", name, i)) != null) {
			i++;
		}
		name = String.format("%s%d", name, i);
		newEntity.setName(name);
		newEntity.setInputName(name);

		if(newEntity instanceof DisplayEntity && this.getDefaultDisplayModel() != null) {
			ArrayList<DisplayModel> def = new ArrayList<DisplayModel>();
			def.add(this.getDefaultDisplayModel());
			((DisplayEntity)newEntity).getDisplayModelList().setDefaultValue(def);
		}
		newEntity.setFlag(Entity.FLAG_ADDED);
		InputAgent.setSessionEdited(true);

		return newEntity;
	}

	public Package getPackage() {
		return javaPackage.getValue();
	}

	public DisplayModel getDefaultDisplayModel(){
		return defaultDisplayModel.getValue();
	}

	public boolean isDragAndDrop() {
		return dragAndDrop.getValue();
	}
}
