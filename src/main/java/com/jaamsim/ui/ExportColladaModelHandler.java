/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2013 Ausenco Engineering Canada Inc.
 * Copyright (C) 2018-2019 JaamSim Software Inc.
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
import com.jaamsim.basicsim.Entity;

public class ExportColladaModelHandler implements ContextMenuItem {
	@Override
	public String getMenuText() {
		return "Export 3D Binary File (*.jsb)";
	}

	@Override
	public boolean supportsEntity(Entity ent) {
		if (ent instanceof ColladaModel)
			return true;
		return false;
	}

	@Override
	public void performAction(Entity ent, int x, int y) {
		ColladaModel model = (ColladaModel)ent;
		// Create a file chooser
		File colFile = new File(model.getColladaFile());
		final JFileChooser chooser = new JFileChooser(colFile);

		// Set the file extension filters
		chooser.setAcceptAllFileFilterUsed(true);
		FileNameExtensionFilter jsbFilter = new FileNameExtensionFilter("JaamSim 3D Binary Files (*.jsb)", "JSB");
		chooser.addChoosableFileFilter(jsbFilter);
		chooser.setFileFilter(jsbFilter);

		// Set the default name for the binary file
		String defName = colFile.getName().concat(".jsb");
		chooser.setSelectedFile(new File(defName));

		// Show the file chooser and wait for selection
		int returnVal = chooser.showDialog(null, "Export");

		// Create the selected graphics files
		if (returnVal == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
			String filePath = file.getPath();

			// Add the file extension ".jsb" if needed
			filePath = filePath.trim();
			if (filePath.indexOf('.') == -1)
				filePath = filePath.concat(".jsb");

			// Confirm overwrite if file already exists
			File temp = new File(filePath);
			if (temp.exists()) {
				boolean confirmed = GUIFrame.showSaveAsDialog(file.getName());
				if (!confirmed) {
					return;
				}
			}

			// Export the JSB file
            model.exportBinaryMesh(temp.getPath());
		}
	}

}
