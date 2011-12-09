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
import java.net.MalformedURLException;
import java.net.URL;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;

import com.sandwell.JavaSimulation.ErrorException;
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
				String offscreen = System.getProperty("JaamSim.offscreen");
				if(!"FALSE".equals(offscreen) && type.getDefaultDisplayModel() != null) {

					// Print the image files if they are not already printed
					if(! FileEntity.fileExists(BranchGroupPrinter.imageFolder  + label.getText() + "LowRes.png") ||
					   ! FileEntity.fileExists(BranchGroupPrinter.imageFolder  + label.getText() + "HighRes.png") ) {
						type.printImage();
					}
					try {

						// Set the icon image for the label
						Icon labelIcon = new ImageIcon( new URL("file:/" + BranchGroupPrinter.imageFolder  + label.getText() + "LowRes.png") );
						label.setIcon(labelIcon);

						// Set the html tooltip with the icon image
						Icon tooltipIcon = new ImageIcon( new URL("file:/" + BranchGroupPrinter.imageFolder  + label.getText() + "HighRes.png") );
						label.setToolTipText("<html><center>" + label.getText()+ "<br><br>" +
								"<img border=\"0\" src=\"" + tooltipIcon + "\" width=\"180\" height=\"180\" />"
								+ "</center><br></html>");
					} catch (MalformedURLException e) {
						throw new ErrorException(e);
					}
				}

				// ObjectType with No DisplayModel
				else {
					label.setToolTipText(label.getText()); // simple text
				}
			}
		}
		return label;
	}
}
