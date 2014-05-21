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

import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DragSource;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.HashMap;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.ToolTipManager;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import com.jaamsim.DisplayModels.DisplayModel;
import com.jaamsim.controllers.RenderManager;
import com.jaamsim.render.Future;
import com.jaamsim.render.RenderUtils;
import com.sandwell.JavaSimulation.ObjectType;
import com.sandwell.JavaSimulation3D.GUIFrame;

public class EntityPallet extends JFrame implements DragGestureListener {

	private static EntityPallet myInstance;  // only one instance allowed to be open

	private final JScrollPane treeView;
	private final JTree tree;

	private final DefaultMutableTreeNode top;
	private final DefaultTreeModel treeModel;

	private EntityPallet() {

		super( "Model Builder" );
		setIconImage(GUIFrame.getWindowIcon());
		// Make the x button do the same as the close button
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		addWindowListener(FrameBox.getCloseListener("ShowModelBuilder"));

		tree = new MyTree();
		tree.setRootVisible(false);
		tree.setShowsRootHandles(true);
		DragSource dragSource = new DragSource();
		dragSource.createDefaultDragGestureRecognizer(tree, DnDConstants.ACTION_COPY, this);

		top = EntityPallet.createTree();
		treeModel = new DefaultTreeModel(top);

		tree.setModel(treeModel);
		tree.getSelectionModel().setSelectionMode( TreeSelectionModel.SINGLE_TREE_SELECTION );

		// Create the tree scroll pane and add the tree to it
		treeView = new JScrollPane( tree );
		getContentPane().add( treeView );

		tree.setRowHeight(25);
		tree.setCellRenderer(new TreeCellRenderer());
		ToolTipManager.sharedInstance().registerComponent(tree);
		ToolTipManager.sharedInstance().setDismissDelay(600000);

		setLocation(GUIFrame.COL1_START, GUIFrame.TOP_START);
		setSize(GUIFrame.COL1_WIDTH, GUIFrame.HALF_TOP);
	}

	@Override
	public void dragGestureRecognized(DragGestureEvent event) {

		TreePath path = tree.getSelectionPath();
		if (path != null) {

			// Dragged node is a DefaultMutableTreeNode
			if(path.getLastPathComponent() instanceof DefaultMutableTreeNode) {
				DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) path.getLastPathComponent();

				// This is an ObjectType node
				if(treeNode.getUserObject() instanceof ObjectType) {
					ObjectType type = (ObjectType) treeNode.getUserObject();
					Cursor cursor = null;

					if (event.getDragAction() == DnDConstants.ACTION_COPY) {
						cursor = DragSource.DefaultCopyDrop;
					}
					if (RenderManager.isGood()) {
						// The new renderer is initialized
						RenderManager.inst().startDragAndDrop(type);
						event.startDrag(cursor,new TransferableObjectType(type), RenderManager.inst());

					} else {
						event.startDrag(cursor,new TransferableObjectType(type));
					}
				}
			}
		}
	}

	private static DefaultMutableTreeNode createTree() {

		// Create a tree that allows one selection at a time
		DefaultMutableTreeNode root = new DefaultMutableTreeNode();
		HashMap<String, DefaultMutableTreeNode> paletteNodes = new HashMap<String, DefaultMutableTreeNode>();
		for (ObjectType type : ObjectType.getAll()) {
			if (!type.isDragAndDrop())
				continue;

			String pName = type.getPaletteName();
			DefaultMutableTreeNode palNode = paletteNodes.get(pName);
			if (palNode == null) {
				palNode = new DefaultMutableTreeNode(pName, true);
				paletteNodes.put(pName, palNode);
				root.add(palNode);
			}

			DefaultMutableTreeNode classNode = new DefaultMutableTreeNode(type, true);
			palNode.add(classNode);
		}

		return root;
	}

	public synchronized static EntityPallet getInstance() {

		if (myInstance == null)
			myInstance = new EntityPallet();

		return myInstance;
	}

	/**
	 * Disposes the only instance of the entity pallet
	 */
	public static void clear() {
		if (myInstance != null) {
			myInstance.dispose();
			myInstance = null;
		}
	}

	private static final Dimension prefSize = new Dimension(220, 24);
	private static final Runnable notifier = new PalletNotifier();
	private static final class PalletNotifier implements Runnable {
		@Override
		public void run() {
			EntityPallet.getInstance().repaint();
		}
	}

	private static class TreeCellRenderer extends DefaultTreeCellRenderer {
		private final ImageIcon icon = new ImageIcon();

		@Override
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
	}

	static class MyTree extends JTree {

		public MyTree() {
		}

		/*
		 * override getToolTipText to control what to display
		 */
		@Override
		public String getToolTipText(MouseEvent e) {

			if(this.getPathForLocation(e.getX(), e.getY()) == null) {
				return null;
			}

			// Obtain the node under the mouse
			DefaultMutableTreeNode node = (DefaultMutableTreeNode)this.getPathForLocation(e.getX(), e.getY()).getLastPathComponent();
			if(node == null) {
				return null;
			}

			Object object = node.getUserObject();

			// It is a leaf node
			if (!(object instanceof ObjectType)) {
				return null;
			}
			ObjectType ot = (ObjectType)object;
			String text = ot.getName();

			String desc = ot.getDescription(0);

			if (desc != null && desc.length() != 0) {
				text = text + ": " + desc;
			}

			return text;
		}
	}

	private final static DataFlavor OBJECT_TYPE_FLAVOR;
	static {
		try {
			// Create OBJECT_TYPE_FLAVOR
			String objectTypeFlavor = DataFlavor.javaJVMLocalObjectMimeType +
			";class=" + TransferableObjectType.class.getName();
			OBJECT_TYPE_FLAVOR = new DataFlavor(objectTypeFlavor);
		} catch (ClassNotFoundException ex) {
			throw new RuntimeException(ex);
		}
	}

	private static class TransferableObjectType implements Transferable {
		private final ObjectType type;

		TransferableObjectType(ObjectType type) {
			this.type = type;
		}

		@Override
		public DataFlavor [] getTransferDataFlavors() {
			return new DataFlavor [] {OBJECT_TYPE_FLAVOR};
		}

		@Override
		public boolean isDataFlavorSupported(DataFlavor flavor) {
			return OBJECT_TYPE_FLAVOR.equals(flavor);
		}

		@Override
		public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
			if (flavor.equals(OBJECT_TYPE_FLAVOR)) {
				return type;
			} else {
				throw new UnsupportedFlavorException(flavor);
			}
		}
	}

}
