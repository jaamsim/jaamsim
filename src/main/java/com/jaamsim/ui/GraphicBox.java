/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2011 Ausenco Engineering Canada Inc.
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

import java.awt.BorderLayout;
import java.awt.Component;
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
import java.util.Collections;
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

import com.jaamsim.Commands.DefineCommand;
import com.jaamsim.Commands.KeywordCommand;
import com.jaamsim.DisplayModels.ColladaModel;
import com.jaamsim.DisplayModels.DisplayModel;
import com.jaamsim.DisplayModels.ImageModel;
import com.jaamsim.DisplayModels.ShapeModel;
import com.jaamsim.Graphics.DisplayEntity;
import com.jaamsim.basicsim.Entity;
import com.jaamsim.controllers.RenderManager;
import com.jaamsim.input.Input;
import com.jaamsim.input.InputAgent;
import com.jaamsim.input.KeywordIndex;
import com.jaamsim.math.AABB;
import com.jaamsim.math.Vec2d;
import com.jaamsim.math.Vec3d;
import com.jaamsim.render.Future;
import com.jaamsim.render.MeshProtoKey;
import com.jaamsim.render.RenderUtils;

public class GraphicBox extends JDialog {
	private static GraphicBox myInstance;  // only one instance allowed to be open
	private final  JLabel previewLabel; // preview DisplayModel as a picture
	final ImageIcon previewIcon = new ImageIcon();
	private static DisplayEntity currentEntity;
	private final static JList<DisplayModel> displayModelList; // All defined DisplayModels

	private final JCheckBox useModelSize;
	private final JCheckBox useModelPosition;
	static {
		displayModelList = new JList<>();
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

		// Preview Area
		previewLabel = new JLabel("", JLabel.CENTER);
		previewLabel.setBorder(new EmptyBorder(10, 10, 20, 10));
		getContentPane().add(previewLabel, "West");

		// DisplayModel List
		JScrollPane scrollPane = new JScrollPane(displayModelList);
		scrollPane.setBorder(new EmptyBorder(10, 10, 20, 10));
		getContentPane().add(scrollPane, "East");

		// Update the preview area according to the selected DisplayModel
		displayModelList.addListSelectionListener(   new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {

				// do this unconditionally to force a repaint and allow for quick outs
				previewLabel.setIcon(null);

				// Avoid null pointer exception when the list is being re-populated
				if(displayModelList.getSelectedIndex() == -1)
					return;

				// Selected DisplayModel
				DisplayModel dm = (DisplayModel)((JList<?>)e.getSource()).getSelectedValue();

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

		// Import button
		JButton importButton = new JButton("Import");
		importButton.addActionListener( new ActionListener() {
			@Override
			public void actionPerformed( ActionEvent e ) {

				if (!RenderManager.isGood()) {
					return;
				}

				// Create a file chooser
				final JFileChooser chooser = new JFileChooser(GUIFrame.get3DFolder());

				// Set the file extension filters
				chooser.setAcceptAllFileFilterUsed(false);
				FileNameExtensionFilter[] colFilters = ColladaModel.getFileNameExtensionFilters();
				FileNameExtensionFilter[] imgFilters = ImageModel.getFileNameExtensionFilters();
				chooser.addChoosableFileFilter(colFilters[0]);
				chooser.addChoosableFileFilter(imgFilters[0]);
				for (int i = 1; i < colFilters.length; i++) {
					chooser.addChoosableFileFilter(colFilters[i]);
				}
				for (int i = 1; i < imgFilters.length; i++) {
					chooser.addChoosableFileFilter(imgFilters[i]);
				}
				chooser.setFileFilter(colFilters[0]);

				// Show the file chooser and wait for selection
				int returnVal = chooser.showDialog(null, "Import");

				// Create the selected graphics files
				if (returnVal == JFileChooser.APPROVE_OPTION) {
					File f = chooser.getSelectedFile();
					GUIFrame.set3DFolder(f.getParent());

					// Determine the file name and extension
					String fileName = f.getName();
					int i = fileName.lastIndexOf('.');
					if (i <= 0 || i >= fileName.length() - 1) {
						GUIFrame.invokeErrorDialog("Input Error",
								"File name: %s is invalid.", f.getName());
						return;
					}
					String extension = fileName.substring(i+1).toLowerCase();

					// Set the entity name
					String entityName = fileName.substring(0, i);
					entityName = entityName.replaceAll(" ", "_"); // Space is not allowed for Entity Name
					entityName = InputAgent.getUniqueName(entityName, "");
					String modelName = InputAgent.getUniqueName(entityName + "-model", "");

					// Create the DisplayModel
					DisplayModel dm = null;
					if (ColladaModel.isValidExtension(extension)) {
						InputAgent.storeAndExecute(new DefineCommand(ColladaModel.class, modelName));
						dm = (DisplayModel) Entity.getNamedEntity(modelName);
						InputAgent.applyArgs(dm, "ColladaFile", f.getPath());
					}
					else if (ImageModel.isValidExtension(extension)) {
						InputAgent.storeAndExecute(new DefineCommand(ImageModel.class, modelName));
						dm = (DisplayModel) Entity.getNamedEntity(modelName);
						InputAgent.applyArgs(dm, "ImageFile", f.getPath());
					}
					else {
						GUIFrame.invokeErrorDialog("Input Error",
								"The extension for file: %s is invalid for both an image and "
								+ "a 3D asset.", fileName);
					}

					 // Add the new DisplayModel to the List
					myInstance.refresh();
					GUIFrame.updateUI();

					// Scroll to the new DisplayModel and ensure it is visible
					int index = GraphicBox.this.getListIndex(dm);
					displayModelList.setSelectedIndex(index);
					displayModelList.ensureIndexIsVisible(index);
				}
			}
		} );

		// Accept button
		JButton acceptButton = new JButton("Accept");
		acceptButton.addActionListener( new ActionListener() {
			@Override
			public void actionPerformed( ActionEvent e ) {
				setEnabled(false); // Don't accept any interaction
				DisplayModel dm = displayModelList.getSelectedValue();
				ArrayList<KeywordIndex> kwList = new ArrayList<>(3);
				KeywordIndex dmKw = InputAgent.formatArgs("DisplayModel", dm.getName());
				kwList.add(dmKw);

				if (!RenderManager.isGood()) {
					myInstance.close();
				}

				Locale loc = null;

				AABB modelBounds = new AABB();
				if (dm instanceof ColladaModel) {
					ColladaModel dmc = (ColladaModel)dm;
					MeshProtoKey key = RenderUtils.FileNameToMeshProtoKey(dmc.getColladaFile());
					modelBounds = RenderManager.inst().getMeshBounds(key, true);

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

					entitySize = new Vec3d(modelSize);
					entitySize.scale3(ratio);
					KeywordIndex sizeKw = InputAgent.formatArgs("Size",
		                                 String.format(loc, "%.6f", entitySize.x),
		                                 String.format(loc, "%.6f", entitySize.y),
		                                 String.format(loc, "%.6f", entitySize.z), "m");
					kwList.add(sizeKw);
				}

				if (dm instanceof ImageModel) {
					ImageModel im = (ImageModel)dm;
					Vec2d imageDims = RenderManager.inst().getImageDims(im.getImageFile());
					if (imageDims != null && useModelSize.isSelected()) {
						// Keep the y size the same, but use the image's proportions. We can't really use the model size, as it is in pixels
						double scale = currentEntity.getSize().y / imageDims.y;
						KeywordIndex sizeKw = InputAgent.formatArgs("Size",
						                     String.format(loc, "%.6f", imageDims.x * scale),
						                     String.format(loc, "%.6f", imageDims.y * scale),
						                     "1.0", "m");
						kwList.add(sizeKw);
					}
				}

				if (useModelPosition.isSelected()) {

					Vec3d entityPos = modelBounds.center;

					KeywordIndex posKw = InputAgent.formatArgs("Position",
					                     String.format(loc, "%.6f", entityPos.x),
					                     String.format(loc, "%.6f", entityPos.y),
					                     String.format(loc, "%.6f", entityPos.z), "m");
					KeywordIndex alignKw = InputAgent.formatArgs("Alignment", "0", "0", "0");
					kwList.add(posKw);
					kwList.add(alignKw);
				}

				KeywordIndex[] kws = new KeywordIndex[kwList.size()];
				kwList.toArray(kws);
				InputAgent.storeAndExecute(new KeywordCommand(currentEntity, kws));

				GUIFrame.updateUI();
				myInstance.close();
			}
		} );

		// Cancel button
		JButton cancelButton = new JButton("Cancel");
		cancelButton.addActionListener( new ActionListener() {
			@Override
			public void actionPerformed( ActionEvent e ) {
				myInstance.close();
			}
		} );

		// CheckBoxes
		JPanel checkBoxPanel = new JPanel(new BorderLayout());
		useModelSize = new JCheckBox("Use Display Model Size");
		useModelSize.setSelected(true);
		useModelPosition = new JCheckBox("Keep Model Position");
		useModelPosition.setSelected(false);
		checkBoxPanel.add(useModelSize, BorderLayout.PAGE_START);
		checkBoxPanel.add(useModelPosition, BorderLayout.PAGE_END);

		// Button Panel
		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout( new FlowLayout(FlowLayout.CENTER) );
		buttonPanel.add(importButton);
		buttonPanel.add(checkBoxPanel);
		buttonPanel.add(acceptButton);
		buttonPanel.add(cancelButton);
		getContentPane().add(buttonPanel, BorderLayout.SOUTH);
		this.pack();
	}

	public static GraphicBox getInstance(DisplayEntity ent, Component c, int x, int y) {
		currentEntity = ent;

		// Has the Graphic Box been created?
		if (myInstance == null) {
			myInstance = new GraphicBox();
			myInstance.setMinimumSize(new Dimension(400, 300));
		}

		// Position of the GraphicBox
		Point pos = c.getLocationOnScreen();
		myInstance.setLocation(pos.x + x, pos.y + y);
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
		// Prepare a sorted array of all the DisplayModels
		ArrayList<DisplayModel> models = new ArrayList<>();
		for (DisplayModel each : Entity.getClonesOfIterator(DisplayModel.class)) {
			if (each instanceof ImageModel ||
			    each instanceof ColladaModel ||
			    each instanceof ShapeModel)
				models.add(each);
		}
		Collections.sort(models, Input.uiSortOrder);

		DisplayModel[] displayModels = new DisplayModel[models.size()];
		for (int i = 0; i < models.size(); i++) {
			displayModels[i] = models.get(i);
		}
		displayModelList.setListData(displayModels);
		displayModelList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		// Select the present DisplayModel
		int index = this.getListIndex(currentEntity.getDisplayModelList().get(0));
		displayModelList.setSelectedIndex(index);
		displayModelList.ensureIndexIsVisible(index);
	}

	private int getListIndex(DisplayModel dm) {
		for (int i=0; i<displayModelList.getModel().getSize(); i++) {
			if (displayModelList.getModel().getElementAt(i) == dm) {
				return i;
			}
		}
		return -1;
	}
}
