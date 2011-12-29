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

import com.sandwell.JavaSimulation.ObjectType;
import com.sandwell.JavaSimulation.Package;
import com.sandwell.JavaSimulation.Util;

import java.awt.Cursor;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DragSource;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.ToolTipManager;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

public class EntityPallet extends JFrame implements DragGestureListener {

	private static EntityPallet myInstance;  // only one instance allowed to be open

	private JScrollPane treeView;
	private static JTree tree;

	private static DefaultMutableTreeNode top;
	private static DefaultTreeModel treeModel;

	private EntityPallet() {

		super( "Model Builder" );
		setIconImage(GUIFrame.getWindowIcon());
		// Make the x button do the same as the close button
		setDefaultCloseOperation(FrameBox.HIDE_ON_CLOSE);

		tree = new JTree();
		DragSource dragSource = new DragSource();
		dragSource.createDefaultDragGestureRecognizer(tree, DnDConstants.ACTION_COPY, this);

		top = EntityPallet.createTree();
		treeModel = new DefaultTreeModel(top);

		tree.setModel(treeModel);
		tree.getSelectionModel().setSelectionMode( TreeSelectionModel.SINGLE_TREE_SELECTION );

		// Create the tree scroll pane and add the tree to it
		treeView = new JScrollPane( tree );
		getContentPane().add( treeView );

		// Set the attributes for the frame
		setLocation(0, 110);
		setVisible( true );

		tree.setRowHeight(25);
		tree.setCellRenderer(new TreeCellRenderer());
		ToolTipManager.sharedInstance().registerComponent(tree);
		pack();
		setSize(220, 400);
	}

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
					event.startDrag(cursor,new TransferableObjectType(type) );
				}
			}
		}
	}

	private static DefaultMutableTreeNode createTree() {

		// Create a tree that allows one selection at a time
		DefaultMutableTreeNode root = new DefaultMutableTreeNode( Util.fileShortName( InputAgent.getConfigFileName() ) );

		for (Package p : Package.getAll()) {
			DefaultMutableTreeNode packNode = new DefaultMutableTreeNode(p.getName(), true);
			for( ObjectType type : ObjectType.getAll() ) {
				if( type.getPackage() != p || ! type.isDragAndDrop() )
					continue;

				DefaultMutableTreeNode classNode = new DefaultMutableTreeNode(type, true);
				packNode.add(classNode);
			}
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
}
