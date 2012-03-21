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

import java.awt.Dimension;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Enumeration;

import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import com.sandwell.JavaSimulation.Entity;
import com.sandwell.JavaSimulation.ObjectType;
import com.sandwell.JavaSimulation.Palette;
import com.sandwell.JavaSimulation.Util;

public class ObjectSelector extends FrameBox {
	private static ObjectSelector myInstance;

	// Tree view properties
	private DefaultMutableTreeNode top;
	private final DefaultTreeModel treeModel;
	private final JTree tree;
	private final JScrollPane treeView;
	protected static Entity currentEntity;

	private long entSequence;

	public ObjectSelector() {
		super( "Object Selector" );
		setDefaultCloseOperation( FrameBox.HIDE_ON_CLOSE );

		top = new DefaultMutableTreeNode(Util.fileShortName(InputAgent.getConfigFileName()));
		treeModel = new DefaultTreeModel(top);
		tree = new JTree();
		tree.setModel(treeModel);
		tree.getSelectionModel().setSelectionMode( TreeSelectionModel.SINGLE_TREE_SELECTION );
		updateTree();

		treeView = new JScrollPane(tree);
		treeView.setPreferredSize(new Dimension(220, 400));
		getContentPane().add(treeView);

		entSequence = 0;

		setLocation(0, 510);
		setSize(220, 490);
		tree.addTreeSelectionListener( new MyTreeSelectionListener() );
		treeModel.addTreeModelListener( new MyTreeModelListener(tree) );

		tree.addMouseListener(new MyMouseListener());
		tree.addKeyListener(new MyKeyListener());
	}

	public void setEntity(Entity ent) {
		if(ent == currentEntity || ! this.isVisible())
			return;

		currentEntity = ent;
		updateValues();

		if (currentEntity == null) {
			tree.setSelectionPath(null);
			return;
		}

		// if the entity is an added entity, allow renaming.  otherwise, do not.
		if (currentEntity.testFlag(Entity.FLAG_ADDED))
			tree.setEditable(true);
		else
			tree.setEditable(false);

		DefaultMutableTreeNode root = (DefaultMutableTreeNode)tree.getModel().getRoot();
		Enumeration<?> e = root.depthFirstEnumeration();
		while (e.hasMoreElements()) {
			DefaultMutableTreeNode aNode = (DefaultMutableTreeNode)e.nextElement();
			if (aNode.getUserObject() == currentEntity) {
				TreePath path = new TreePath(aNode.getPath());
				tree.scrollPathToVisible(path);
				tree.setSelectionPath(path);
				return;
			}
		}
	}

	public void updateValues() {
		if (!this.isVisible())
			return;

		long curSequence = Entity.getEntitySequence();
		if (entSequence != curSequence) {
			entSequence = curSequence;
			updateTree();
		}
	}

	/**
	 * Returns the only instance of the Object Selector
	 */
	public static synchronized ObjectSelector getInstance() {
		if (myInstance == null)
			myInstance = new ObjectSelector();

		return myInstance;
	}

	public void dispose() {
		myInstance = null;
		currentEntity = null;
		super.dispose();
	}

	private void updateTree() {

		// Make a best-effort attempt to find all used classes...can race with
		// object creation/deletion, but that's ok
		ArrayList<Class<? extends Entity>> used = new ArrayList<Class<? extends Entity>>();
		for (int i = 0; i < Entity.getAll().size(); i++) {
			try {
				Class<? extends Entity> klass = Entity.getAll().get(i).getClass();
				if (!used.contains(klass))
					used.add(klass);
			}
			catch (IndexOutOfBoundsException e) {}
		}

		for (Palette p : Palette.getAll()) {
			DefaultMutableTreeNode packNode = getNodeFor_In(p.getName(), top);
			for( ObjectType type : ObjectType.getAll() ) {
				if( type.getPackage() != p )
					continue;

				Class<? extends Entity> proto = type.getJavaClass();
				// skip unused classes
				if (!used.contains(proto))
					continue;

				DefaultMutableTreeNode classNode = getNodeFor_In(proto.getSimpleName(), packNode);
				for (int i = 0; i < Entity.getAll().size(); i++) {
					try {
						Entity each = Entity.getAll().get(i);

						// Skip all that do not match the current class
						if (each.getClass() != proto)
							continue;

						// skip locked Entities
						if (each.testFlag(Entity.FLAG_LOCKED))
							continue;
						DefaultMutableTreeNode eachNode = getNodeFor_In(each, classNode);
						if(classNode.getIndex(eachNode) < 0)
							classNode.add(eachNode);
					}
					catch (IndexOutOfBoundsException e) {
						continue;
					}
				}

				// Remove the killed entities from the class node
				Enumeration<?> enumeration = classNode.children();
				while (enumeration.hasMoreElements ()) {
					DefaultMutableTreeNode each = (DefaultMutableTreeNode) enumeration.nextElement();
					if (!Entity.getAll().contains(each.getUserObject())) {
						classNode.remove(each);
					}
				}
				if(!classNode.isLeaf()) {

					// Class node does not exist in the package node
					if(packNode.getIndex(classNode) < 0) {
						packNode.add(classNode);
					}
				}
				else if( packNode.getIndex(classNode) >= 0) {
					packNode.remove(classNode);
				}
			}

			// Package node is not empty
			if(!packNode.isLeaf()) {
				if(top.getIndex(packNode) < 0)
					top.add(packNode);
			}
			else if(top.getIndex(packNode) >= 0) {
				top.remove(packNode);
			}
		}

		// Store all the expanded paths
		Enumeration<TreePath> expandedPaths = tree.getExpandedDescendants(new TreePath(top));
		TreePath selectedPath = tree.getSelectionPath();

		treeModel.reload(top); // refresh tree

		// Restore all expanded paths and the selected path
		tree.setSelectionPath(selectedPath);
		while (expandedPaths != null && expandedPaths.hasMoreElements())
		{
			TreePath path = expandedPaths.nextElement();
			tree.expandPath(path);
		}
	}

	/**
	 * Return a node of userObject in parent
	 */
	private static DefaultMutableTreeNode getNodeFor_In(Object userObject, DefaultMutableTreeNode parent) {

		// obtain all the children in parent
		Enumeration<?> enumeration = parent.children();

		while (enumeration.hasMoreElements ()) {
			DefaultMutableTreeNode eachNode = (DefaultMutableTreeNode) enumeration.nextElement();
			if( eachNode.getUserObject() == userObject ||
				userObject instanceof String && ((String) userObject).equals(eachNode.getUserObject()) ) {

				// This child already exists in parent
				return eachNode;
			}
		}

		// Child does not exist in parent; create it
		return new DefaultMutableTreeNode(userObject, true);
	}

	static class MyTreeSelectionListener implements TreeSelectionListener {
		public void valueChanged( TreeSelectionEvent e ) {
			JTree tree = (JTree) e.getSource();
			if(tree.getLastSelectedPathComponent() == null) {
				// This occurs when we set no selected entity (null) and then
				// force the tree to have a null selected node
				return;
			}

			DefaultMutableTreeNode node = (DefaultMutableTreeNode)tree.getLastSelectedPathComponent();

			if (node.getUserObject() instanceof Entity) {
				Entity entity = (Entity)node.getUserObject();
				FrameBox.setSelectedEntity(entity);
			}
			else {
				FrameBox.setSelectedEntity(null);
			}
		}
	}

	static class MyTreeModelListener implements TreeModelListener {
		private final JTree tree;

		public MyTreeModelListener(JTree tree) {
			this.tree = tree;
		}

		public void treeNodesChanged( TreeModelEvent e ) {

			DefaultMutableTreeNode node = (DefaultMutableTreeNode)tree.getLastSelectedPathComponent();
			String newName = (String)node.getUserObject();
			currentEntity.setInputName(newName);
			currentEntity.setName(newName);
			node.setUserObject(currentEntity);
			FrameBox.valueUpdate();
		}

		public void treeNodesInserted(TreeModelEvent e) {}
		public void treeNodesRemoved(TreeModelEvent e) {}
		public void treeStructureChanged(TreeModelEvent e) {}
	}

	static class MyMouseListener implements MouseListener {
		private final JPopupMenu menu= new JPopupMenu();

		public void mouseClicked(MouseEvent e) {

			if(e.getButton() != MouseEvent.BUTTON3)
				return;

			if(currentEntity instanceof DisplayEntity ) {
				DisplayEntity disp = (DisplayEntity)currentEntity;

				if(! disp.isMovable())
					return;

				// Right mouse click on a movable DisplayEntity
				menu.removeAll();
				disp.addMenuItems(menu, e.getX(), e.getY());
				menu.show(e.getComponent(), e.getX(), e.getX());
			}
		}
		public void mouseEntered(MouseEvent e) {}
		public void mouseExited(MouseEvent e) {}
		public void mousePressed(MouseEvent e) {}
		public void mouseReleased(MouseEvent e) {}
	}
	static class MyKeyListener implements KeyListener {
		public void keyReleased(KeyEvent e) {

			if (e.getKeyCode() != KeyEvent.VK_DELETE)
				return;

			if(currentEntity instanceof DisplayEntity ) {
				DisplayEntity disp = (DisplayEntity)currentEntity;

				if(! disp.isMovable())
					return;

				// Delete key was released on a movable DisplayEntity
				disp.kill();
				FrameBox.setSelectedEntity(null);
			}
		}
		public void keyPressed(KeyEvent e) {}
		public void keyTyped(KeyEvent e) {}
	}
}
