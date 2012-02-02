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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FileDialog;
import java.awt.FlowLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.image.BufferedImage;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import com.sandwell.JavaSimulation.ObjectType;
import com.sandwell.JavaSimulation.Simulation;
import com.sandwell.JavaSimulation.Vector;

public class GraphicBox extends JDialog {
	private static GraphicBox myInstance;  // only one instance allowed to be open
	private final  JLabel previewLabel; // preview DisplayModel as a picture
	final ImageIcon previewIcon = new ImageIcon();
	private static DisplayEntity currentEntity;
	private final static JList displayModelList; // All defined DisplayModels


	private final JCheckBox useModelSize;
	private final JCheckBox enabledCulling;
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
			public void valueChanged(ListSelectionEvent e) {

				// Avoid null pointer exception when the list is being re-populated
				if(displayModelList.getSelectedIndex() == -1)
					return;

				// Selected DisplayModel
				DisplayModel dm = (DisplayModel) ((JList)e.getSource()).getSelectedValue();
				BufferedImage image = dm.getHighResImage();
				// do this unconditionally to force a repaint
				previewLabel.setIcon(null);
				if(image != null) {
					previewIcon.setImage(image);
					previewLabel.setIcon(previewIcon);
				}
			}
		} );

		// Import and Accept buttons
		JButton importButton = new JButton("Import");
		importButton.addActionListener( new ActionListener() {
			public void actionPerformed( ActionEvent e ) {

				FileDialog chooser = new FileDialog(myInstance, "New DisplayModel", FileDialog.LOAD );
				String validTypes ="";
				for(String each: DisplayModel.getValidExtentions()) {
					validTypes += "*." +each.toLowerCase()+";";
				}
				validTypes = validTypes.substring(0, validTypes.length()-1);
				chooser.setFile(validTypes);
				chooser.setVisible(true);

				// A file has been selected
				if( chooser.getFile() != null ) {
					String fileName = chooser.getFile();
					String chosenFileName = chooser.getDirectory() + fileName;
					chosenFileName.trim();

					int to = fileName.contains(".") ? fileName.indexOf(".") : fileName.length()-1;
					String entityName = fileName.substring(0, to); // File name without the extension
					entityName = entityName.replaceAll(" ", ""); // Space is not allowed for Entity Name

					// If entityName already exists:
					// Determine the name of the new entity based on entityName
					// and the first available integer number starting from 1
					// inside the braces
					if( Simulation.getNamedEntity(String.format(entityName)) != null ) {
						int i = 1;
						while (Simulation.getNamedEntity(String.format("%s(%d)", entityName, i)) != null) {
							i++;
						}
						entityName = String.format("%s(%d)", entityName, i);;
					}

					// Create the new DisplayModel
					ObjectType type = null;
					for(ObjectType each: ObjectType.getAll()) {
						if(each.getJavaClass() == DisplayModel.class) {
							type = each;
						}
					}

					if(type == null)
						return;

					DisplayModel newModel = (DisplayModel)type.getNewInstance();
					newModel.setName(entityName);
					newModel.setInputName(entityName);
					Vector data = new Vector(1);
					data.addElement(entityName);
					data.addElement("Shape");
					data.addElement(chosenFileName);
					data.addElement("EnableCulling");
					if(!enabledCulling.isSelected()) {
						data.addElement("False");
					}
					else {
						data.addElement("True");
					}
					InputAgent.processData(newModel, data);
					myInstance.refresh(); // Add the new DisplayModel to the List
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
			public void actionPerformed( ActionEvent e ) {
				setEnabled(false); // Don't accept any interaction
				DisplayModel dm = (DisplayModel) displayModelList.getSelectedValue();

				Vector data = new Vector(3);
				data.addElement(currentEntity.getInputName());
				data.addElement(currentEntity.getDisplayModelList().getKeyword());
				data.addElement(dm.getName());
				InputAgent.processData(currentEntity, data);

				currentEntity.setupGraphics();

				// Make the DisplayEntity look fine proportionally
				Point3d modelSize = dm.getModelSize();
				Vector3d entitySize = currentEntity.getSize();
				double longestSide = modelSize.x;
				double ratio = dm.getConversionFactorToMeters();
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
				entitySize = new Vector3d(modelSize.x*ratio, modelSize.y*ratio, modelSize.z*ratio);
				currentEntity.setSize(entitySize);
				currentEntity.updateInputSize();

				FrameBox.valueUpdate();
				myInstance.close();
			}
		} );
		useModelSize = new JCheckBox("Use Display Model Size");
		useModelSize.setSelected(true);
		enabledCulling = new JCheckBox("Enable Culling");
		enabledCulling.setSelected(true);
		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout( new FlowLayout(FlowLayout.RIGHT) );
		buttonPanel.add(useModelSize);
		buttonPanel.add(enabledCulling);
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

	public void dispose() {
		super.dispose();
		myInstance = null;
	}

	private void refresh() {
		DisplayModel entDisplayModel = currentEntity.getDisplayModelList().getValue().get(0);

		// Populate JList with all the DisplayModels
		DisplayModel[ ] displayModels = new DisplayModel[DisplayModel.getAll().size()];
		int index = 0;
		int i = 0;
		for(DisplayModel each: DisplayModel.getAll()){
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
