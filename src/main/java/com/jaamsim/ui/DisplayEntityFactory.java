/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2013 Ausenco Engineering Canada Inc.
 * Copyright (C) 2016-2026 JaamSim Software Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jaamsim.ui;

import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

import com.jaamsim.Commands.DefineCommand;
import com.jaamsim.DisplayModels.ColladaModel;
import com.jaamsim.DisplayModels.ImageModel;
import com.jaamsim.Graphics.DisplayEntity;
import com.jaamsim.Graphics.Image;
import com.jaamsim.basicsim.Entity;
import com.jaamsim.basicsim.JaamSimModel;
import com.jaamsim.controllers.RenderManager;
import com.jaamsim.input.FileInput;
import com.jaamsim.input.InputAgent;
import com.jaamsim.math.AABB;
import com.jaamsim.math.Vec2d;
import com.jaamsim.math.Vec3d;
import com.jaamsim.render.MeshProtoKey;
import com.jaamsim.render.RenderUtils;
import com.jaamsim.units.DimensionlessUnit;
import com.jaamsim.units.DistanceUnit;

/**
 * Creates entities from a 3D asset file (.DAE, .OBJ, etc.) or a image file (.png, .jpg, etc.).
 *
 * @author matt.chudleigh
 *
 */
public class DisplayEntityFactory extends Entity {

	public static final String LAST_USED_3D_FOLDER = "3D_FOLDER";
	public static final String LAST_USED_IMAGE_FOLDER = "IMAGE_FOLDER";

	/**
	 * Opens a FileDialog for selecting images to import.
	 *
	 * @param gui - the Control Panel.
	 */
	public static void importImages(GUIFrame gui) {

		// Create a file chooser
		final JFileChooser chooser = new JFileChooser(JaamSimModel.getPreferenceFolder(DisplayEntityFactory.LAST_USED_IMAGE_FOLDER));
		chooser.setMultiSelectionEnabled(true);

		// Set the file extension filters
		chooser.setAcceptAllFileFilterUsed(false);
		FileNameExtensionFilter[] imgFilters = FileInput.getFileNameExtensionFilters("Image",
				ImageModel.VALID_FILE_EXTENSIONS, ImageModel.VALID_FILE_DESCRIPTIONS);
		for (int i = 0; i < imgFilters.length; i++) {
			chooser.addChoosableFileFilter(imgFilters[i]);
		}
		chooser.setFileFilter(imgFilters[0]);

		// Show the file chooser and wait for selection
		int returnVal = chooser.showDialog(gui, "Import Images");

		// Create the selected graphics files
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			File[] files = chooser.getSelectedFiles();
			JaamSimModel.setPreferenceFolder(DisplayEntityFactory.LAST_USED_IMAGE_FOLDER, files[0].getParent());
			DisplayEntityFactory.importImageFiles(files);
		}
	}

	/**
	 * Opens a FileDialog for selecting 3D assets to import.
	 *
	 * @param gui - the Control Panel.
	 */
	public static void import3D(GUIFrame gui) {

		// Create a file chooser
		final JFileChooser chooser = new JFileChooser(JaamSimModel.getPreferenceFolder(DisplayEntityFactory.LAST_USED_3D_FOLDER));
		chooser.setMultiSelectionEnabled(true);

		// Set the file extension filters
		chooser.setAcceptAllFileFilterUsed(false);
		FileNameExtensionFilter[] colFilters = FileInput.getFileNameExtensionFilters("3D",
				ColladaModel.VALID_FILE_EXTENSIONS, ColladaModel.VALID_FILE_DESCRIPTIONS);
		chooser.addChoosableFileFilter(colFilters[0]);
		for (int i = 1; i < colFilters.length; i++) {
			chooser.addChoosableFileFilter(colFilters[i]);
		}
		chooser.setFileFilter(colFilters[0]);

		// Show the file chooser and wait for selection
		int returnVal = chooser.showDialog(gui, "Import 3D Assets");

		// Create the selected graphics files
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			File[] files = chooser.getSelectedFiles();
			JaamSimModel.setPreferenceFolder(DisplayEntityFactory.LAST_USED_3D_FOLDER, files[0].getParent());
			DisplayEntityFactory.import3DFiles(files);
		}
	}

	/**
	 * Creates a new thread for creating the imported images.
	 *
	 * @param files - the image files to import
	 */
	private static void importImageFiles(File[] files) {

		final File[] chosenfiles = files;
		new Thread(new Runnable() {
			@Override
			public void run() {
				DisplayEntityFactory.createImages(chosenfiles);
			}
		}).start();
	}

	/**
	 * Creates a new thread for creating the 3D assets.
	 *
	 * @param files - the 3D asset files to import
	 */
	private static void import3DFiles(File[] files) {

		final File[] chosenfiles = files;
		new Thread(new Runnable() {
			@Override
			public void run() {
				DisplayEntityFactory.create3DAssets(chosenfiles);
			}
		}).start();
	}

	/**
	 * Loops through the specified files and creates a new DisplayEntity for each one.
	 *
	 * @param files - the image files
	 */
	private static void createImages(File[] files) {

		if (!RenderManager.isGood()) {
			GUIFrame.invokeErrorDialog("Runtime Error",	"The renderer is not ready.");
			return;
		}

		JaamSimModel simModel = GUIFrame.getJaamSimModel();

		// Loop through the graphics files
		for (File f : files) {

			// Determine the file name and extension
			String fileName = f.getName();
			int i = fileName.lastIndexOf('.');
			if (i <= 0 || i >= fileName.length() - 1) {
				GUIFrame.invokeErrorDialog("Input Error",
						String.format("File name: %s is invalid.", fileName));
				continue;
			}
			String extension = fileName.substring(i+1).toLowerCase();
			if (!ImageModel.isValidExtension(extension)) {
				GUIFrame.invokeErrorDialog("Input Error",
						String.format("The extension for file: %s is invalid for an image.",
								fileName));
				continue;
			}

			// Set the entity name
			i = fileName.indexOf('.');  // first period in the file name
			String entityName = fileName.substring(0, i);
			entityName = entityName.replaceAll(" ", "_"); // Space is not allowed for Entity Name
			entityName = InputAgent.getUniqueName(simModel, entityName, "");

			// Create the ImageModel
			String modelName = InputAgent.getUniqueName(simModel,entityName + "-model", "");
			simModel.storeAndExecute(new DefineCommand(simModel, ImageModel.class, modelName));
			ImageModel dm = (ImageModel) simModel.getNamedEntity(modelName);

			// Load the image to the ImageModel
			InputAgent.applyArgs(dm, "ImageFile", f.getPath());

			// Create the DisplayEntity
			simModel.storeAndExecute(new DefineCommand(simModel, Image.class, entityName));
			Image de = (Image) simModel.getNamedEntity(entityName);

			// Assign the ImageModel to the new DisplayEntity
			InputAgent.applyArgs(de, "DisplayModel", dm.getName());

			// Set the x-dimension of the image to maintain its aspect ratio
			Vec3d size = new Vec3d(1.0, 1.0, 1.0);
			Vec2d imageDims = RenderManager.inst().getImageDims(f.toURI());
			if (imageDims != null)
				size.x = imageDims.x / imageDims.y;
			InputAgent.applyVec3d(de, "Size", size, DistanceUnit.class);
		}
	}

	/**
	 * Loops through the specified files and creates a new DisplayEntity for each one.
	 *
	 * @param files - the 3D asset and/or image files.
	 */
	private static void create3DAssets(File[] files) {

		if (!RenderManager.isGood()) {
			GUIFrame.invokeErrorDialog("Runtime Error",	"The renderer is not ready.");
			return;
		}

		JaamSimModel simModel = GUIFrame.getJaamSimModel();

		// Loop through the graphics files
		for (File f : files) {

			// Determine the file name and extension
			String fileName = f.getName();
			int i = fileName.lastIndexOf('.');
			if (i <= 0 || i >= fileName.length() - 1) {
				GUIFrame.invokeErrorDialog("Input Error",
						String.format("File name: %s is invalid.", fileName));
				continue;
			}
			String extension = fileName.substring(i+1).toLowerCase();
			if (!ColladaModel.isValidExtension(extension)) {
				GUIFrame.invokeErrorDialog("Input Error",
						String.format("The extension for file: %s is invalid for a 3D asset.",
								fileName));
				continue;
			}

			// Set the entity name
			i = fileName.indexOf('.');  // first period in the file name
			String entityName = fileName.substring(0, i);
			entityName = entityName.replaceAll(" ", "_"); // Space is not allowed for Entity Name
			entityName = InputAgent.getUniqueName(simModel, entityName, "");

			// Create the ColladaModel
			String modelName = InputAgent.getUniqueName(simModel, entityName + "-model", "");
			simModel.storeAndExecute(new DefineCommand(simModel, ColladaModel.class, modelName));
			ColladaModel dm = (ColladaModel) simModel.getNamedEntity(modelName);

			// Load the 3D content to the ColladaModel
			InputAgent.applyArgs(dm, "ColladaFile", f.getPath());

			// Create the DisplayEntity
			simModel.storeAndExecute(new DefineCommand(simModel, DisplayEntity.class, entityName));
			DisplayEntity de = (DisplayEntity) simModel.getNamedEntity(entityName);

			// Assign the ColladaModel to the new DisplayEntity
			InputAgent.applyArgs(de, "DisplayModel", dm.getName());

			// Calculate the DisplayEntity's size and position from the ColladaModel
			MeshProtoKey meshKey = RenderUtils.FileNameToMeshProtoKey(f.toURI());
			AABB modelBounds = RenderManager.inst().getMeshBounds(meshKey, true);
			Vec3d entityPos = modelBounds.center;
			Vec3d modelSize = new Vec3d(modelBounds.radius);
			modelSize.scale3(2);

			// Set the DisplayEntity's position, size, and alignment
			InputAgent.applyVec3d(de, "Position", entityPos, DistanceUnit.class);
			InputAgent.applyVec3d(de, "Alignment", new Vec3d(), DimensionlessUnit.class);
			InputAgent.applyVec3d(de, "Size", modelSize, DistanceUnit.class);
		}
	}

}
