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
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DragSource;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JToolTip;
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
import com.jaamsim.ui.FrameBox;
import com.sandwell.JavaSimulation.ObjectType;
import com.sandwell.JavaSimulation.Palette;

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
		setDefaultCloseOperation(FrameBox.HIDE_ON_CLOSE);

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

		for (Palette p : Palette.getAll()) {
			DefaultMutableTreeNode packNode = new DefaultMutableTreeNode(p.getName(), true);
			for( ObjectType type : ObjectType.getAll() ) {
				if( type.getPalette() != p || ! type.isDragAndDrop() )
					continue;

				DefaultMutableTreeNode classNode = new DefaultMutableTreeNode(type, true);
				packNode.add(classNode);
			}
			if(!packNode.isLeaf())
				root.add(packNode);
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
		private final MyToolTip toolTip;

		public MyTree() {
			toolTip = new MyToolTip();
		}

		/*
		 * This JTree has a custom ToolTip
		 */
		@Override
		public JToolTip createToolTip() {
			return toolTip;
		}

		/*
		 * override getToolTipText to control what to display
		 */
		@Override
		public String getToolTipText(MouseEvent e) {
			toolTip.setImage( null ); // Set defaults so we can quick out
			toolTip.setPreferredSize( null );

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
			String text = ((ObjectType)object).getName();
			DisplayModel dm = ((ObjectType)object).getDefaultDisplayModel();
			if (dm == null) {
				return null;
			}
			Future<BufferedImage> fi = RenderManager.inst().getPreviewForDisplayModel(dm, notifier);

			if (!fi.isDone()) {
				return null;
			}
			if (fi.failed()) {
				return null;
			}

			BufferedImage image = RenderUtils.scaleToRes(fi.get(), 180, 180);
			Dimension dim = new Dimension(180, 180); // frame size for image toolTip

			toolTip.setImage( image );
			toolTip.setPreferredSize( dim );
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

	static class MyToolTip extends JToolTip {
		private BufferedImage image;

		public MyToolTip() {
			image = null;
		}

		protected void setImage(BufferedImage image){
			this.image = image;
		}

		@Override
		public void paintComponent(Graphics g) {
			super.paintComponent(g);
			if(image != null){
				g.drawImage(image, 0, 0, this);
			}
		}
	}
}
