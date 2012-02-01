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

import java.awt.Component;
import java.awt.image.BufferedImage;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;

import com.sandwell.JavaSimulation.FileEntity;
import com.sandwell.JavaSimulation.ObjectType;

class TreeCellRenderer extends DefaultTreeCellRenderer {

	public Component getTreeCellRendererComponent(JTree tree,
			Object value, boolean selected, boolean expanded,
			boolean leaf, int row, boolean hasFocus) {

		JLabel label = (JLabel)super.getTreeCellRendererComponent(
				tree, value, selected, expanded, leaf, row, hasFocus);

		// Only leaf and if it is not already expanded
		if(leaf && ! expanded) {

			// Make sure the leaf is ObjectType
			if(((DefaultMutableTreeNode)value).getUserObject() instanceof ObjectType) {
				ObjectType type = (ObjectType) ((DefaultMutableTreeNode)value).getUserObject();

				// ObjectType has a DisplayModel
				if(type.getDefaultDisplayModel() != null) {

					// Set the icon image for the label
					DisplayModel dm = type.getDefaultDisplayModel();
					BufferedImage image = dm.getLowResImage();
					ImageIcon imageIcon = null;
					if(image != null) {
						imageIcon = new ImageIcon(image);
					}
					this.setIcon(imageIcon);

					// Print the image files if they are not already printed
					if(! FileEntity.fileExists(BranchGroupPrinter.imageFolder  + label.getText() + "LowRes.png") ||
					   ! FileEntity.fileExists(BranchGroupPrinter.imageFolder  + label.getText() + "HighRes.png") ) {
						type.printImage();
					}
				}
			}
		}
		return label;
	}
}
