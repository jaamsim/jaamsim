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
package com.jaamsim.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Locale;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileNameExtensionFilter;

import com.jaamsim.DisplayModels.ColladaModel;
import com.jaamsim.DisplayModels.DisplayModel;
import com.jaamsim.controllers.RenderManager;
import com.jaamsim.input.InputAgent;
import com.jaamsim.input.KeywordIndex;
import com.jaamsim.math.AABB;
import com.jaamsim.math.Vec3d;
import com.jaamsim.render.Future;
import com.jaamsim.render.MeshProtoKey;
import com.jaamsim.render.RenderUtils;
import com.sandwell.JavaSimulation.Entity;
import com.sandwell.JavaSimulation3D.DisplayEntity;
import com.sandwell.JavaSimulation3D.GUIFrame;

public class GraphicBox extends JDialog {
	private static GraphicBox myInstance;  // only one instance allowed to be open
	private final  JLabel previewLabel; // preview DisplayModel as a picture
	final ImageIcon previewIcon = new ImageIcon();
	private static DisplayEntity currentEntity;
	private final static JList displayModelList; // All defined DisplayModels
	private File lastDir;  // last directory accessed by the file chooser

	private final JCheckBox useModelSize;
	private final JCheckBox useModelPosition;
	static {
		displayModelList = new JList();
	}

	private GraphicBox() {

		setTitle( "Select DisplayModel" );
		setIconImage(GUIFrame.getWindowIcon());

		this.setModal(true);

		// Upon closing do the close method
		setDefaultCloseOperation( FrameBox.DO_NOTHING_ON_CLOSE );
		WindowListener windowListener = new WindowAdapter() {
			@Override
			public void windowClosing( WindowEvent e ) {
				myInstance.close();
			}
		};
		this.addWindowListener( windowListener );

		previewLabel = new JLabel("", JLabel.CENTER);
		getContentPane().add(previewLabel, "West");

		JScrollPane scrollPane = new JScrollPane(displayModelList);
		scrollPane.setBorder(new EmptyBorder(5, 0, 44, 10));
		getContentPane().add(scrollPane, "East");

		// Update the previewLabel according to the selected DisplayModel
		displayModelList.addListSelectionListener(   new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {

				// do this unconditionally to force a repaint and allow for quick outs
				previewLabel.setIcon(null);

				// Avoid null pointer exception when the list is being re-populated
				if(displayModelList.getSelectedIndex() == -1)
					return;

				// Selected DisplayModel
				DisplayModel dm = (DisplayModel) ((JList)e.getSource()).getSelectedValue();

				if (!RenderManager.isGood()) { return; }

				Future<BufferedImage> fi = RenderManager.inst().getPreviewForDisplayModel(dm, null);
				fi.blockUntilDone();

				if (fi.failed()) {
					return; // Something went wrong...
				}

				BufferedImage image = RenderUtils.scaleToRes(fi.get(), 180, 180);

				if(image == null) {
					return;
				}

				previewIcon.setImage(image);
				previewLabel.setIcon(previewIcon);
			}
		} );

		// Import and Accept buttons
		JButton importButton = new JButton("Import");
		importButton.addActionListener( new ActionListener() {
			@Override
			public void actionPerformed( ActionEvent e ) {

				if (!RenderManager.isGood()) {
					return;
				}

				// Create a file chooser
				final JFileChooser chooser = new JFileChooser(lastDir);

				// Set the file extension filters
				chooser.setAcceptAllFileFilterUsed(false);
				chooser.addChoosableFileFilter(ColladaModel.getFileNameExtensionFilter());
				for (FileNameExtensionFilter filter : ColladaModel.getFileNameExtensionFilters()) {
					chooser.addChoosableFileFilter(filter);
				}

				// Show the file chooser and wait for selection
				int returnVal = chooser.showDialog(null, "Import");

				// Create the selected graphics files
				if (returnVal == JFileChooser.APPROVE_OPTION) {
		            File f = chooser.getSelectedFile();
		            lastDir = chooser.getCurrentDirectory();

					// Determine the file name and extension
					String fileName = f.getName();
					int i = fileName.lastIndexOf('.');
					if (i <= 0 || i >= fileName.length() - 1) {
						LogBox.format("File name: %s is invalid.", f.getName());
						LogBox.getInstance().setVisible(true);
						return;
					}
					String extension = fileName.substring(i+1).toLowerCase();

					// Set the entity name
					String entityName = fileName.substring(0, i);
					entityName = entityName.replaceAll(" ", ""); // Space is not allowed for Entity Name

					// Check for a valid extension
					if (!ColladaModel.isValidExtension(extension)) {
						LogBox.format("File name: %s is invalid.", f.getName());
						LogBox.getInstance().setVisible(true);
						return;
					}

					// Create the ColladaModel
					String modelName = entityName + "-model";
					ColladaModel dm = InputAgent.defineEntityWithUniqueName(ColladaModel.class, modelName, "", true);

					// Load the 3D content to the ColladaModel
					InputAgent.processEntity_Keyword_Value(dm, "ColladaFile", "'" + f.getPath() + "'");

					 // Add the new DisplayModel to the List
					myInstance.refresh();
					FrameBox.valueUpdate();

					// Scroll to selection and ensure it is visible
					int index = displayModelList.getModel().getSize() - 1;
					displayModelList.setSelectedIndex(index);
					displayModelList.ensureIndexIsVisible(index);
				}
			}
		} );

		JButton acceptButton = new JButton("Accept");
		acceptButton.addActionListener( new ActionListener() {
			@Override
			public void actionPerformed( ActionEvent e ) {
				setEnabled(false); // Don't accept any interaction
				DisplayModel dm = (DisplayModel) displayModelList.getSelectedValue();

				ArrayList<String> tokens = new ArrayList<String>(1);
				tokens.add(dm.getInputName());

				KeywordIndex kw = new KeywordIndex(tokens, "DisplayModel", 0, tokens.size(), null);
				InputAgent.apply(currentEntity, kw);

				if (!RenderManager.isGood()) {
					myInstance.close();
				}

				AABB modelBounds = new AABB();
				if (dm instanceof ColladaModel) {
					ColladaModel dmc = (ColladaModel)dm;
					MeshProtoKey key = RenderUtils.FileNameToMeshProtoKey(dmc.getColladaFile());
					modelBounds = RenderManager.inst().getMeshBounds(key, true);
				}

				Vec3d modelSize = new Vec3d(modelBounds.radius);
				modelSize.scale3(2);

				Vec3d entitySize = currentEntity.getSize();
				double longestSide = modelSize.x;
				//double ratio = dm.getConversionFactorToMeters();
				double ratio = 1;
				if(! useModelSize.isSelected()) {
					ratio = entitySize.x/modelSize.x;
					if(modelSize.y > longestSide) {
						ratio = entitySize.y/modelSize.y;
						longestSide = modelSize.y;
					}
					if(modelSize.z > longestSide) {
						ratio = entitySize.z/modelSize.z;
					}
				}
				Locale loc = null;
				if (useModelPosition.isSelected()) {

					Vec3d entityPos = modelBounds.center;

					InputAgent.processEntity_Keyword_Value(currentEntity, "Position", String.format(loc, "%.6f %.6f %.6f m", entityPos.x, entityPos.y, entityPos.z));
					InputAgent.processEntity_Keyword_Value(currentEntity, "Alignment", "0 0 0");
				}
				if (dm instanceof ColladaModel) {
					entitySize = new Vec3d(modelSize);
					entitySize.scale3(ratio);
					InputAgent.processEntity_Keyword_Value(currentEntity, "Size", String.format(loc, "%.6f %.6f %.6f m", entitySize.x, entitySize.y, entitySize.z));
				}
				FrameBox.valueUpdate();
				myInstance.close();
			}
		} );
		useModelSize = new JCheckBox("Use Display Model Size");
		useModelSize.setSelected(true);
		useModelPosition = new JCheckBox("Keep Model Position");
		useModelPosition.setSelected(false);
		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout( new FlowLayout(FlowLayout.RIGHT) );
		buttonPanel.add(useModelSize);
		buttonPanel.add(useModelPosition);
		buttonPanel.add(importButton);
		buttonPanel.add(acceptButton);
		getContentPane().add(buttonPanel, BorderLayout.SOUTH);
		this.pack();
	}

	public static GraphicBox getInstance( DisplayEntity ent, int x, int y ) {
		currentEntity = ent;

		Point pos = new Point(x, y);

		// Has the Graphic Box been created?
		if (myInstance == null) {
			myInstance = new GraphicBox();
			myInstance.setMinimumSize(new Dimension(400, 300));
		}

		// Position of the GraphicBox
		pos.setLocation(pos.x + myInstance.getSize().width /2 , pos.y + myInstance.getSize().height /2);
		myInstance.setLocation(pos.x, pos.y);
		myInstance.refresh();
		myInstance.setEnabled(true);
		return myInstance;
	}

	private void close() {
		currentEntity = null;
		setVisible(false);
	}

	@Override
	public void dispose() {
		super.dispose();
		myInstance = null;
	}

	private void refresh() {
		DisplayModel entDisplayModel = currentEntity.getDisplayModelList().get(0);

		ArrayList<DisplayModel> models = Entity.getClonesOf(DisplayModel.class);
		// Populate JList with all the DisplayModels
		DisplayModel[] displayModels = new DisplayModel[models.size()];
		int index = 0;
		int i = 0;
		for (DisplayModel each : models) {
			if(entDisplayModel == each) {
				index = i;
			}
			displayModels[i++] = each;
		}
		displayModelList.setListData(displayModels);
		displayModelList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		displayModelList.setSelectedIndex(index);
		displayModelList.ensureIndexIsVisible(index);
	}
}
