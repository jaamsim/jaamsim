/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2013 Ausenco Engineering Canada Inc.
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
package com.jaamsim.collada;

import java.io.File;
import java.io.IOException;

import com.jaamsim.DisplayModels.ColladaModel;
import com.jaamsim.controllers.RenderManager;
import com.jaamsim.input.InputAgent;
import com.jaamsim.math.AABB;
import com.jaamsim.math.Vec3d;
import com.jaamsim.render.MeshProtoKey;
import com.jaamsim.render.RenderUtils;
import com.sandwell.JavaSimulation.Entity;
import com.sandwell.JavaSimulation.FileEntity;
import com.sandwell.JavaSimulation.Input;
import com.sandwell.JavaSimulation.Keyword;
import com.sandwell.JavaSimulation.StringInput;
import com.sandwell.JavaSimulation3D.DisplayEntity;

/**
 * This is currently an experimental class for building up scenes based on a list of
 * collada (or jsm) files
 * @author matt.chudleigh
 *
 */
public class ColladaEntityFactory extends Entity {

	@Keyword(description = "A file to scan for collada or jsm files to use to create display entities. " +
	                       "Note: This is currently an experimental feature!",
	         example = "Factory CreateFromDirectory { './models' }")
	private final StringInput directoryInput;

	{
		directoryInput = new StringInput("CreateFromDirectory", "Entity Creation", null);
		this.addInput(directoryInput, true);
	}

	@Override
	public void updateForInput( Input<?> in ) {
		super.updateForInput(in);

		if (in == directoryInput) {
			updateForDirectoryInput();
		}
	}

	private void updateForDirectoryInput() {

		String dir = FileEntity.getRootDirectory() + System.getProperty( "file.separator" ) + directoryInput.getValue();

		File dirFile = new File(dir);

		File[] files = dirFile.listFiles();

		for (File f : files) {
			try {
				createEntityFromFile(f);
			} catch (IOException ex) {
				System.out.printf("Import IO error: %s\n", ex.getMessage());
			}
		}
	}

	private void createEntityFromFile(File f) throws IOException {
		String fileName = f.getName();
		String filePath = f.getCanonicalPath();

		int i = fileName.lastIndexOf('.');
		String extension = null;
		if (i <= 0) {
			return;
		}
		extension = fileName.substring(i+1);
		extension = extension.toUpperCase();

		if (!extension.equals("DAE") &&
			!extension.equals("ZIP") &&
			!extension.equals("JSM")) {
			// Not a supported file
			return;
		}
		String entityName = fileName.substring(0, i);
		entityName = entityName.replaceAll(" ", ""); // Space is not allowed for Entity Name

		String modelName = entityName + "-model";
		ColladaModel dm = InputAgent.defineEntity(ColladaModel.class, modelName, true);

		InputAgent.processEntity_Keyword_Value(dm, "ColladaFile", "'" + filePath + "'");

		MeshProtoKey meshKey = RenderUtils.FileNameToMeshProtoKey(filePath);
		AABB modelBounds = RenderManager.inst().getMeshBounds(meshKey, true);

		DisplayEntity de = InputAgent.defineEntity(DisplayEntity.class, entityName, true);
		InputAgent.processEntity_Keyword_Value(de, "DisplayModel", dm.getInputName());

		// Now set the size and position
		Vec3d entityPos = modelBounds.center;
		Vec3d modelSize = new Vec3d(modelBounds.radius);
		modelSize.scale3(2);

		InputAgent.processEntity_Keyword_Value(de, "Position", String.format("%.6f %.6f %.6f m", entityPos.x, entityPos.y, entityPos.z));
		InputAgent.processEntity_Keyword_Value(de, "Alignment", "0 0 0");

		InputAgent.processEntity_Keyword_Value(de, "Size", String.format("%.6f %.6f %.6f m", modelSize.x, modelSize.y, modelSize.z));

	}
}

