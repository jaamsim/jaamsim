/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2013 Ausenco Engineering Canada Inc.
 * Copyright (C) 2016 JaamSim Software Inc.
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

import com.jaamsim.DisplayModels.ColladaModel;
import com.jaamsim.DisplayModels.ImageModel;
import com.jaamsim.Graphics.DisplayEntity;
import com.jaamsim.basicsim.Entity;
import com.jaamsim.controllers.RenderManager;
import com.jaamsim.input.InputAgent;
import com.jaamsim.input.KeywordIndex;
import com.jaamsim.math.AABB;
import com.jaamsim.math.Vec2d;
import com.jaamsim.math.Vec3d;
import com.jaamsim.render.MeshProtoKey;
import com.jaamsim.render.RenderUtils;

/**
 * Creates entities from a 3D asset file (.DAE, .OBJ, etc.) or a image file (.png, .jpg, etc.).
 *
 * @author matt.chudleigh
 *
 */
public class DisplayEntityFactory extends Entity {

	private static File lastDir; // Last directory selected by the file chooser

	/**
	 * Opens a FileDialog for selecting images to import.
	 *
	 * @param gui - the Control Panel.
	 */
	public static void importImages(GUIFrame gui) {

		// Create a file chooser
		final JFileChooser chooser = new JFileChooser(lastDir);
		chooser.setMultiSelectionEnabled(true);

		// Set the file extension filters
		chooser.setAcceptAllFileFilterUsed(false);
		FileNameExtensionFilter[] imgFilters = ImageModel.getFileNameExtensionFilters();
		for (int i = 0; i < imgFilters.length; i++) {
			chooser.addChoosableFileFilter(imgFilters[i]);
		}
		chooser.setFileFilter(imgFilters[0]);

		// Show the file chooser and wait for selection
		int returnVal = chooser.showDialog(gui, "Import Images");

		// Create the selected graphics files
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			lastDir = chooser.getCurrentDirectory();
			File[] files = chooser.getSelectedFiles();
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
		final JFileChooser chooser = new JFileChooser(lastDir);
		chooser.setMultiSelectionEnabled(true);

		// Set the file extension filters
		chooser.setAcceptAllFileFilterUsed(false);
		FileNameExtensionFilter[] colFilters = ColladaModel.getFileNameExtensionFilters();
		chooser.addChoosableFileFilter(colFilters[0]);
		for (int i = 1; i < colFilters.length; i++) {
			chooser.addChoosableFileFilter(colFilters[i]);
		}
		chooser.setFileFilter(colFilters[0]);

		// Show the file chooser and wait for selection
		int returnVal = chooser.showDialog(gui, "Import 3D Assets");

		// Create the selected graphics files
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			lastDir = chooser.getCurrentDirectory();
			File[] files = chooser.getSelectedFiles();
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

		// Loop through the graphics files
		for (File f : files) {

			// Determine the file name and extension
			String fileName = f.getName();
			int i = fileName.lastIndexOf('.');
			if (i <= 0 || i >= fileName.length() - 1) {
				GUIFrame.invokeErrorDialog("Input Error",
						"File name: %s is invalid.", f.getName());
				continue;
			}
			String extension = fileName.substring(i+1).toLowerCase();
			if (!ImageModel.isValidExtension(extension)) {
				GUIFrame.invokeErrorDialog("Input Error",
						"The extension for file: %s is invalid for an image.", fileName);
				continue;
			}

			// Set the entity name
			String entityName = fileName.substring(0, i);
			entityName = entityName.replaceAll(" ", "_"); // Space is not allowed for Entity Name

			// Create the ImageModel
			String modelName = entityName + "-model";
			ImageModel dm = InputAgent.defineEntityWithUniqueName(ImageModel.class, modelName, "", true);

			// Load the image to the ImageModel
			InputAgent.applyArgs(dm, "ImageFile", f.getPath());

			// Create the DisplayEntity
			DisplayEntity de = InputAgent.defineEntityWithUniqueName(DisplayEntity.class, entityName, "", true);

			// Assign the ImageModel to the new DisplayEntity
			InputAgent.applyArgs(de, "DisplayModel", dm.getName());

			// Set the x-dimension of the image to maintain its aspect ratio
			Vec3d size = new Vec3d(1.0, 1.0, 0.0);
			Vec2d imageDims = RenderManager.inst().getImageDims(f.toURI());
			if (imageDims != null)
				size.x = imageDims.x / imageDims.y;
			KeywordIndex kw = InputAgent.formatPointInputs("Size", size, "m");
			InputAgent.apply(de, kw);
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

		// Loop through the graphics files
		for (File f : files) {

			// Determine the file name and extension
			String fileName = f.getName();
			int i = fileName.lastIndexOf('.');
			if (i <= 0 || i >= fileName.length() - 1) {
				GUIFrame.invokeErrorDialog("Input Error",
						"File name: %s is invalid.", f.getName());
				continue;
			}
			String extension = fileName.substring(i+1).toLowerCase();
			if (!ColladaModel.isValidExtension(extension)) {
				GUIFrame.invokeErrorDialog("Input Error",
						"The extension for file: %s is invalid for a 3D asset.", fileName);
				continue;
			}

			// Set the entity name
			String entityName = fileName.substring(0, i);
			entityName = entityName.replaceAll(" ", "_"); // Space is not allowed for Entity Name

			// Create the ColladaModel
			String modelName = entityName + "-model";
			ColladaModel dm = InputAgent.defineEntityWithUniqueName(ColladaModel.class, modelName, "", true);

			// Load the 3D content to the ColladaModel
			InputAgent.applyArgs(dm, "ColladaFile", f.getPath());

			// Create the DisplayEntity
			DisplayEntity de = InputAgent.defineEntityWithUniqueName(DisplayEntity.class, entityName, "", true);

			// Assign the ColladaModel to the new DisplayEntity
			InputAgent.applyArgs(de, "DisplayModel", dm.getName());

			// Calculate the DisplayEntity's size and position from the ColladaModel
			MeshProtoKey meshKey = RenderUtils.FileNameToMeshProtoKey(f.toURI());
			AABB modelBounds = RenderManager.inst().getMeshBounds(meshKey, true);
			Vec3d entityPos = modelBounds.center;
			Vec3d modelSize = new Vec3d(modelBounds.radius);
			modelSize.scale3(2);

			// Set the DisplayEntity's position, size, and alignment
			KeywordIndex kw;
			kw = InputAgent.formatPointInputs("Position", entityPos, "m");
			InputAgent.apply(de, kw);
			kw = InputAgent.formatPointInputs("Alignment", new Vec3d(), null);
			InputAgent.apply(de, kw);
			kw = InputAgent.formatPointInputs("Size", modelSize, "m");
			InputAgent.apply(de, kw);
		}
	}

}
