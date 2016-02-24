/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2016 KMA Technologies
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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.filechooser.FileNameExtensionFilter;

import com.jaamsim.DisplayModels.ColladaModel;
import com.jaamsim.Graphics.DisplayEntity;
import com.jaamsim.Graphics.EntityLabel;
import com.jaamsim.basicsim.Entity;
import com.jaamsim.basicsim.Simulation;
import com.jaamsim.controllers.RenderManager;
import com.jaamsim.input.InputAgent;
import com.jaamsim.input.KeywordIndex;
import com.jaamsim.math.Vec3d;

public class ContextMenu {

	private ContextMenu() {}

	/**
	 * Adds menu items to the right click (context) menu for the specified entity.
	 * @param ent - entity whose context menu is to be generated
	 * @param menu - context menu to be populated with menu items
	 * @param x - screen coordinate for the menu
	 * @param y - screen coordinate for the menu
	 */
	public static void populateMenu(JPopupMenu menu, final Entity ent, final int x, final int y) {

		// 1) Input Editor
		JMenuItem inputEditorMenuItem = new JMenuItem( "Input Editor" );
		inputEditorMenuItem.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed( ActionEvent event ) {
				InputAgent.applyArgs(Simulation.getInstance(), "ShowInputEditor", "TRUE");
				FrameBox.setSelectedEntity(ent);
			}
		} );
		menu.add( inputEditorMenuItem );

		// 2) Property Viewer
		JMenuItem propertyViewerMenuItem = new JMenuItem( "Property Viewer" );
		propertyViewerMenuItem.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed( ActionEvent event ) {
				InputAgent.applyArgs(Simulation.getInstance(), "ShowPropertyViewer", "TRUE");
				FrameBox.setSelectedEntity(ent);
			}
		} );
		menu.add( propertyViewerMenuItem );

		// 3) Output Viewer
		JMenuItem outputViewerMenuItem = new JMenuItem( "Output Viewer" );
		outputViewerMenuItem.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed( ActionEvent event ) {
				InputAgent.applyArgs(Simulation.getInstance(), "ShowOutputViewer", "TRUE");
				FrameBox.setSelectedEntity(ent);
			}
		} );
		menu.add( outputViewerMenuItem );

		// 4) Duplicate
		JMenuItem duplicateMenuItem = new JMenuItem( "Duplicate" );
		duplicateMenuItem.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed( ActionEvent event ) {
				Entity copiedEntity = InputAgent.defineEntityWithUniqueName(ent.getClass(),
						ent.getName(), "_Copy", true);

				// Match all the inputs
				copiedEntity.copyInputs(ent);

				// Position the duplicated entity next to the original
				if (copiedEntity instanceof DisplayEntity) {
					DisplayEntity dEnt = (DisplayEntity)copiedEntity;

					Vec3d pos = dEnt.getPosition();
					pos.x += 0.5d * dEnt.getSize().x;
					pos.y -= 0.5d * dEnt.getSize().y;

					dEnt.setPosition(pos);

					// Set the input for the "Position" keyword to the new value
					KeywordIndex kw = InputAgent.formatPointInputs("Position", pos, "m");
					InputAgent.apply(dEnt, kw);
				}

				// Show the duplicated entity in the editors and viewers
				FrameBox.setSelectedEntity(copiedEntity);
			}
		} );
		if (ent.testFlag(Entity.FLAG_GENERATED)) {
			duplicateMenuItem.setEnabled(false);
		}
		menu.add( duplicateMenuItem );

		// 5) Delete
		JMenuItem deleteMenuItem = new JMenuItem( "Delete" );
		deleteMenuItem.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed( ActionEvent event ) {
				ent.kill();
				FrameBox.setSelectedEntity(null);
			}
		} );
		menu.add( deleteMenuItem );

		// DisplayEntity menu items
		if (ent instanceof DisplayEntity) {
			ContextMenu.populateDisplayEntityMenu(menu, (DisplayEntity)ent, x, y);
		}

		// ColladaModel menu items
		if (ent instanceof ColladaModel) {
			ContextMenu.populateColladaModelMenu(menu, (ColladaModel)ent, x, y);
		}
	}

	public static void populateDisplayEntityMenu(JPopupMenu menu, final DisplayEntity ent, final int x, final int y) {

		if (!RenderManager.isGood())
			return;

		// 1) Change Graphics
		JMenuItem changeGraphicsMenuItem = new JMenuItem( "Change Graphics" );
		changeGraphicsMenuItem.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed( ActionEvent event ) {
				GraphicBox graphicBox = GraphicBox.getInstance(ent, x, y);
				graphicBox.setVisible( true );
			}
		} );
		if (ent.getDisplayModelList() == null) {
			changeGraphicsMenuItem.setEnabled(false);
		}
		menu.add( changeGraphicsMenuItem );

		// 2) Add Label
		JMenuItem addLabelMenuItem = new JMenuItem( "Add Label" );
		addLabelMenuItem.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed( ActionEvent event ) {
				EntityLabel label = InputAgent.defineEntityWithUniqueName(EntityLabel.class, ent.getName() + "_Label", "", true);
				InputAgent.applyArgs(label, "TargetEntity", ent.getName());

				InputAgent.applyArgs(label, "RelativeEntity", ent.getName());
				if (ent.getCurrentRegion() != null)
					InputAgent.applyArgs(label, "Region", ent.getCurrentRegion().getName());

				double ypos = -0.15 - 0.5*ent.getSize().y;
				InputAgent.apply(label, InputAgent.formatPointInputs("Position", new Vec3d(0.0, ypos, 0.0), "m"));
				InputAgent.applyArgs(label, "TextHeight", "0.15", "m");
				label.resizeForText();
			}
		} );
		if (ent instanceof EntityLabel || EntityLabel.getLabel(ent) != null
				|| ent.testFlag(Entity.FLAG_GENERATED)) {
			addLabelMenuItem.setEnabled(false);
		}
		menu.add( addLabelMenuItem );

		// 3) Centre in View
		JMenuItem centerInViewMenuItem = new JMenuItem( "Center in View" );
		final View v = RenderManager.inst().getActiveView();
		centerInViewMenuItem.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed( ActionEvent event ) {
				// Move the camera position so that the entity is in the centre of the screen
				Vec3d viewPos = new Vec3d(v.getGlobalPosition());
				viewPos.sub3(v.getGlobalCenter());
				viewPos.add3(ent.getPosition());
				v.setCenter(ent.getPosition());
				v.setPosition(viewPos);
			}
		} );
		if (v == null) {
			centerInViewMenuItem.setEnabled(false);
		}
		menu.add( centerInViewMenuItem );
	}

	public static void populateColladaModelMenu(JPopupMenu menu, final ColladaModel model, final int x, final int y) {

		//1) Export Binary
		JMenuItem exportBinaryMenuItem = new JMenuItem( "Export 3D Binary File (*.jsb)" );
		exportBinaryMenuItem.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed( ActionEvent event ) {

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
					if (filePath.indexOf(".") == -1)
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
		});
		menu.add( exportBinaryMenuItem );
	}

}
