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
import java.awt.Dimension;
import java.awt.image.BufferedImage;

import javax.swing.ImageIcon;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;

import com.jaamsim.DisplayModels.DisplayModel;
import com.jaamsim.controllers.RenderManager;
import com.jaamsim.render.Future;
import com.jaamsim.render.RenderUtils;
import com.sandwell.JavaSimulation.ObjectType;

class TreeCellRenderer extends DefaultTreeCellRenderer {
	private static final Dimension prefSize = new Dimension(220, 24);
	private final ImageIcon icon = new ImageIcon();

	public Component getTreeCellRendererComponent(JTree tree,
			Object value, boolean selected, boolean expanded,
			boolean leaf, int row, boolean hasFocus) {

		super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);

		// If not a leaf, just return
		if (!leaf)
			return this;

		// If we don't find an ObjectType (likely we will) just return
		Object userObj = ((DefaultMutableTreeNode)value).getUserObject();
		if (!(userObj instanceof ObjectType))
			return this;

		ObjectType type = (ObjectType)userObj;
		this.setText(type.getInputName());
		this.setPreferredSize(prefSize);

		if (!RenderManager.isGood())
			return this;

		DisplayModel dm = type.getDefaultDisplayModel();
		if (dm == null)
			return this;

		Future<BufferedImage> fi = RenderManager.inst().getPreviewForDisplayModel(dm, notifier);
		if (fi.failed() || !fi.isDone())
			return this;

		icon.setImage(RenderUtils.scaleToRes(fi.get(), 24, 24));
		this.setIcon(icon);
		return this;
	}

private static final Runnable notifier = new PalletNotifier();
private static final class PalletNotifier implements Runnable {
	@Override
	public void run() {
		EntityPallet.getInstance().repaint();
	}
}
}
