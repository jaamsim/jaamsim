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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.util.ArrayList;
import java.util.Enumeration;

import javax.swing.JFrame;
import javax.swing.JMenuItem;
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

import com.jaamsim.Graphics.DisplayEntity;
import com.jaamsim.Graphics.EntityLabel;
import com.jaamsim.basicsim.Entity;
import com.jaamsim.basicsim.ErrorException;
import com.jaamsim.basicsim.ObjectType;
import com.jaamsim.basicsim.Simulation;
import com.jaamsim.controllers.RenderManager;
import com.jaamsim.input.InputAgent;
import com.jaamsim.input.KeywordIndex;
import com.jaamsim.math.Vec3d;
import com.jaamsim.units.Unit;

public class ObjectSelector extends FrameBox {
	private static ObjectSelector myInstance;

	// Tree view properties
	private DefaultMutableTreeNode top;
	private final DefaultTreeModel treeModel;
	private final JTree tree;
	private final JScrollPane treeView;
	public static Entity currentEntity;

	private long entSequence;

	public ObjectSelector() {
		super( "Object Selector" );
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		addWindowListener(FrameBox.getCloseListener("ShowObjectSelector"));
		addWindowFocusListener(new MyFocusListener());

		top = new DefaultMutableTreeNode();
		treeModel = new DefaultTreeModel(top);
		tree = new JTree();
		tree.setModel(treeModel);
		tree.getSelectionModel().setSelectionMode( TreeSelectionModel.SINGLE_TREE_SELECTION );
		tree.setRootVisible(false);
		tree.setShowsRootHandles(true);
		tree.setInvokesStopCellEditing(true);

		treeView = new JScrollPane(tree);
		getContentPane().add(treeView);

		entSequence = 0;

		setLocation(GUIFrame.COL1_START, GUIFrame.BOTTOM_START);
		setSize(GUIFrame.COL1_WIDTH, GUIFrame.HALF_BOTTOM);

		tree.addTreeSelectionListener( new MyTreeSelectionListener() );
		treeModel.addTreeModelListener( new MyTreeModelListener(tree) );

		tree.addMouseListener(new MyMouseListener());
		tree.addKeyListener(new MyKeyListener());
	}

	@Override
	public void setEntity(Entity ent) {

		if (ent == currentEntity)
			return;
		currentEntity = ent;

		if (tree == null)
			return;

		long curSequence = Entity.getEntitySequence();
		if (entSequence != curSequence) {
			entSequence = curSequence;
			updateTree();
		}

		if (currentEntity == null) {
			tree.setSelectionPath(null);
			tree.setEditable(false);
			return;
		}

		tree.setEditable(true);

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

	@Override
	public void updateValues(double simTime) {
		if (!this.isVisible())
			return;

		long curSequence = Entity.getEntitySequence();
		if (entSequence != curSequence) {
			entSequence = curSequence;
			updateTree();
		}
	}

	public static void allowUpdate() {
		myInstance.entSequence = 0;
	}

	/**
	 * Returns the only instance of the Object Selector
	 */
	public static synchronized ObjectSelector getInstance() {
		if (myInstance == null)
			myInstance = new ObjectSelector();

		myInstance.treeView.getHorizontalScrollBar().getModel().setValue(0);

		return myInstance;
	}

	private synchronized static void killInstance() {
		myInstance = null;
	}

	@Override
	public void dispose() {
		killInstance();
		currentEntity = null;
		super.dispose();
	}

	private void updateTree() {

		if (tree == null || top == null)
			return;

		// Store all the expanded paths
		Enumeration<TreePath> expandedPaths = tree.getExpandedDescendants(new TreePath(top));

		// Identify the selected entity (cannot use currentEntity -- would race with setEntity)
		Entity selectedEnt = null;
		TreePath selectedPath = tree.getSelectionPath();
		if (selectedPath != null) {
			Object selectedObj = ((DefaultMutableTreeNode)selectedPath.getLastPathComponent()).getUserObject();
			if (selectedObj instanceof Entity)
				selectedEnt = (Entity)selectedObj;
		}

		// Clear the present tree
		top.removeAllChildren();

		// Add the instance for Simulation to the top of the tree as a single leaf node
		top.add(new DefaultMutableTreeNode(Simulation.getInstance(), false));

		// Add the instance for TLS if present
		Entity tls = Entity.getNamedEntity("TLS");
		if (tls != null)
			top.add(new DefaultMutableTreeNode(tls, false));

		// Create the tree structure for palettes and object types in the correct order
		for (int i = 0; i < ObjectType.getAll().size(); i++) {
			try {
				final ObjectType type = ObjectType.getAll().get(i);
				if (type == null)
					continue;

				// Find or create the node for the palette
				DefaultMutableTreeNode paletteNode = getNodeFor_In(type.getPaletteName(), top);
				if (paletteNode == null) {
					paletteNode = new DefaultMutableTreeNode(type.getPaletteName());
					top.add(paletteNode);
				}

				// Add the node for the Object Type to the palette
				DefaultMutableTreeNode typeNode = new DefaultMutableTreeNode(type.getName(), true);
				paletteNode.add(typeNode);
			}
			catch (IndexOutOfBoundsException e) {}
		}

		// Loop through the entities in the model
		for (int i = 0; i < Entity.getAll().size(); i++) {
			try {
				final Entity ent = Entity.getAll().get(i);

				// The instance for Simulation has already been added
				if (ent == Simulation.getInstance())
					continue;

				// The instance for TLS has already been added
				if (ent == tls)
					continue;

				// Do not include the units or views
				if (ent instanceof Unit || ent instanceof View)
					continue;

				// Skip an entity that is locked
				if (ent.testFlag(Entity.FLAG_LOCKED))
					continue;

				// Determine the object type for this entity
				final ObjectType type = ent.getObjectType();
				if (type == null)
					continue;

				// Find the pallete node for this entity
				DefaultMutableTreeNode paletteNode = getNodeFor_In(type.getPaletteName(), top);
				if (paletteNode == null)
					continue;

				// Find the object type node for this entity
				DefaultMutableTreeNode typeNode = getNodeFor_In(type.getName(), paletteNode);
				if (typeNode == null)
					continue;

				// Add the entity to the object type node
				DefaultMutableTreeNode entityNode = new DefaultMutableTreeNode(ent, false);
				typeNode.add(entityNode);
			}
			catch (IndexOutOfBoundsException e) {}
		}

		// Remove any object type tree nodes that have no entities
		ArrayList<DefaultMutableTreeNode> nodesToRemove = new ArrayList<>();
		Enumeration<DefaultMutableTreeNode> paletteEnum = top.children();
		while (paletteEnum.hasMoreElements()) {
			DefaultMutableTreeNode paletteNode = paletteEnum.nextElement();
			Enumeration<DefaultMutableTreeNode> typeEnum = paletteNode.children();
			while (typeEnum.hasMoreElements()) {
				DefaultMutableTreeNode typeNode = typeEnum.nextElement();
				if (typeNode.isLeaf())
					nodesToRemove.add(typeNode);
			}
			for (DefaultMutableTreeNode typeNode : nodesToRemove) {
				paletteNode.remove(typeNode);
			}
			nodesToRemove.clear();
		}

		// Remove any palettes that have no object types left
		paletteEnum = top.children();
		while (paletteEnum.hasMoreElements()) {
			DefaultMutableTreeNode paletteNode = paletteEnum.nextElement();

			// Do not remove any of the special nodes such as the instance for Simulation
			if (!paletteNode.getAllowsChildren())
				continue;

			if (paletteNode.isLeaf())
				nodesToRemove.add(paletteNode);
		}
		for (DefaultMutableTreeNode paletteNode : nodesToRemove) {
			top.remove(paletteNode);
		}

		// Refresh the tree
		treeModel.reload(top);

		// Restore the path to the selected entity
		if (selectedEnt != null) {
			TreePath path = ObjectSelector.getPathToEntity(selectedEnt, top);
			if (path != null)
				tree.setSelectionPath(path);
		}

		// Restore all the expanded paths
		while (expandedPaths != null && expandedPaths.hasMoreElements()) {
			TreePath oldPath = expandedPaths.nextElement();
			if (oldPath.getPathCount() < 2)
				continue;

			// Path to a palette
			DefaultMutableTreeNode oldPaletteNode = (DefaultMutableTreeNode) (oldPath.getPath())[1];
			String paletteName = (String) (oldPaletteNode.getUserObject());
			DefaultMutableTreeNode paletteNode = getNodeFor_In(paletteName, top);
			if (paletteNode == null)
				continue;
			if (oldPath.getPathCount() == 2) {
				Object[] nodeList = { top, paletteNode };
				tree.expandPath(new TreePath(nodeList));
				continue;
			}

			// Path to an object type
			DefaultMutableTreeNode oldTypeNode = (DefaultMutableTreeNode) (oldPath.getPath())[2];
			String typeName = (String) (oldTypeNode.getUserObject());
			DefaultMutableTreeNode typeNode = getNodeFor_In(typeName, paletteNode);
			if (typeNode == null)
				continue;
			Object[] nodeList = { top, paletteNode, typeNode };
			tree.expandPath(new TreePath(nodeList));
		}
	}

	/**
	 * Returns a tree node for the specified userObject in the specified parent.
	 * If a node, already exists for this parent, it is returned. If it does
	 * not exist, then null is returned.
	 * @param userObject - object for the tree node.
	 * @param parent - object's parent
	 * @return tree node for the object.
	 */
	private static DefaultMutableTreeNode getNodeFor_In(Object userObject, DefaultMutableTreeNode parent) {

		// Loop through the parent's children
		Enumeration<DefaultMutableTreeNode> enumeration = parent.children();
		while (enumeration.hasMoreElements()) {
			DefaultMutableTreeNode eachNode = enumeration.nextElement();
			if (eachNode.getUserObject() == userObject ||
					userObject instanceof String && ((String) userObject).equals(eachNode.getUserObject()) )
				return eachNode;
		}

		return null;
	}

	private static TreePath getPathToEntity(Entity ent, DefaultMutableTreeNode root) {
		final ObjectType type = ent.getObjectType();
		if (type == null)
			return null;
		DefaultMutableTreeNode paletteNode = getNodeFor_In(type.getPaletteName(), root);
		if (paletteNode == null)
			return null;
		DefaultMutableTreeNode typeNode = getNodeFor_In(type.getName(), paletteNode);
		if (typeNode == null)
			return null;
		DefaultMutableTreeNode entityNode = getNodeFor_In(ent, typeNode);
		if (entityNode == null)
			return null;
		Object[] nodeList = { root, paletteNode, typeNode, entityNode };
		return new TreePath(nodeList);
	}

	static class MyTreeSelectionListener implements TreeSelectionListener {
		@Override
		public void valueChanged( TreeSelectionEvent e ) {
			JTree tree = (JTree) e.getSource();
			DefaultMutableTreeNode node = (DefaultMutableTreeNode)tree.getLastSelectedPathComponent();
			if(node == null) {
				// This occurs when we set no selected entity (null) and then
				// force the tree to have a null selected node
				return;
			}

			Object userObj = node.getUserObject();
			if (userObj instanceof Entity) {
				FrameBox.setSelectedEntity((Entity)userObj);
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

		@Override
		public void treeNodesChanged( TreeModelEvent e ) {
			DefaultMutableTreeNode node = (DefaultMutableTreeNode)tree.getLastSelectedPathComponent();
			String newName = ((String)node.getUserObject()).trim();
			try {
				InputAgent.renameEntity(currentEntity, newName);
				if (currentEntity instanceof DisplayEntity) {
					DisplayEntity dEnt = (DisplayEntity) currentEntity;
					EntityLabel label = EntityLabel.getLabel(dEnt);
					if (label != null)
						label.updateForTargetNameChange();
				}
			}
			catch (ErrorException err) {
				GUIFrame.showErrorDialog("Input Error", err.getMessage());
			}
			finally {
				node.setUserObject(currentEntity);
				FrameBox.reSelectEntity();
			}
		}

		@Override
		public void treeNodesInserted(TreeModelEvent e) {}
		@Override
		public void treeNodesRemoved(TreeModelEvent e) {}
		@Override
		public void treeStructureChanged(TreeModelEvent e) {}
	}


static class InputMenuItem extends MenuItem {
	private final Entity ent;
	public InputMenuItem(Entity ent) {
		super("Input Editor");
		this.ent = ent;
	}

	@Override
	public void action() {
		InputAgent.applyArgs(Simulation.getInstance(), "ShowInputEditor", "TRUE");
		FrameBox.setSelectedEntity(ent);
	}
}

static class PropertyMenuItem extends MenuItem {
	private final Entity ent;
	public PropertyMenuItem(Entity ent) {
		super("Property Viewer");
		this.ent = ent;
	}

	@Override
	public void action() {
		InputAgent.applyArgs(Simulation.getInstance(), "ShowPropertyViewer", "TRUE");
		FrameBox.setSelectedEntity(ent);
	}
}

static class OutputMenuItem extends MenuItem {
	private final Entity ent;
	public OutputMenuItem(Entity ent) {
		super("Output Viewer");
		this.ent = ent;
	}

	@Override
	public void action() {
		InputAgent.applyArgs(Simulation.getInstance(), "ShowOutputViewer", "TRUE");
		FrameBox.setSelectedEntity(ent);
	}
}

static class DuplicateMenuItem extends MenuItem {
	private final Entity ent;
	public DuplicateMenuItem(Entity ent) {
		super("Duplicate");
		this.ent = ent;
	}

	@Override
	public void action() {
		Entity copiedEntity = InputAgent.defineEntityWithUniqueName(ent.getClass(),
				ent.getName(), "_Copy", true);

		// Match all the inputs
		copiedEntity.copyInputs(ent);

		// Position the duplicated entity next to the original
		if (copiedEntity instanceof DisplayEntity) {
			DisplayEntity dEnt = (DisplayEntity)copiedEntity;

			Vec3d pos = dEnt.getPosition();
			pos.x += 0.5d * dEnt.getSize().x;
			pos.y -= 0.5d * dEnt.getSize().y;

			dEnt.setPosition(pos);

			// Set the input for the "Position" keyword to the new value
			KeywordIndex kw = InputAgent.formatPointInputs("Position", pos, "m");
			InputAgent.apply(dEnt, kw);
		}

		// Show the duplicated entity in the editors and viewers
		FrameBox.setSelectedEntity(copiedEntity);
	}
}

static class DeleteMenuItem extends MenuItem {
	private final Entity ent;
	public DeleteMenuItem(Entity ent) {
		super("Delete");
		this.ent = ent;
	}

	@Override
	public void action() {
		ent.kill();
		FrameBox.setSelectedEntity(null);
	}
}

static class GraphicsMenuItem extends MenuItem {
	private final DisplayEntity ent;
	private final int x;
	private final int y;

	public GraphicsMenuItem(DisplayEntity ent, int x, int y) {
		super("Change Graphics");
		this.ent = ent;
		this.x = x;
		this.y = y;
	}

	@Override
	public void action() {
		// More than one DisplayModel(LOD) or No DisplayModel
		if(ent.getDisplayModelList() == null)
			return;

		GraphicBox graphicBox = GraphicBox.getInstance(ent, x, y);
		graphicBox.setVisible( true );
	}
}

static class LabelMenuItem extends MenuItem {
	private final DisplayEntity ent;

	public LabelMenuItem(DisplayEntity ent) {
		super("Add Label");
		this.ent = ent;
	}

	@Override
	public void action() {
		EntityLabel label = InputAgent.defineEntityWithUniqueName(EntityLabel.class, ent.getName() + "_Label", "", true);
		InputAgent.applyArgs(label, "TargetEntity", ent.getName());

		InputAgent.applyArgs(label, "RelativeEntity", ent.getName());
		if (ent.getCurrentRegion() != null)
			InputAgent.applyArgs(label, "Region", ent.getCurrentRegion().getName());

		double ypos = -0.15 - 0.5*ent.getSize().y;
		InputAgent.apply(label, InputAgent.formatPointInputs("Position", new Vec3d(0.0, ypos, 0.0), "m"));
		InputAgent.applyArgs(label, "TextHeight", "0.15", "m");
		label.resizeForText();
	}
}

static class CenterInViewMenuItem extends MenuItem {
	private final DisplayEntity ent;
	private final View v;

	public CenterInViewMenuItem(DisplayEntity ent, View v) {
		super("Center in View");
		this.ent = ent;
		this.v = v;
	}

	@Override
	public void action() {
		// Move the camera position so that the entity is in the centre of the screen
		Vec3d viewPos = new Vec3d(v.getGlobalPosition());
		viewPos.sub3(v.getGlobalCenter());
		viewPos.add3(ent.getPosition());
		v.setCenter(ent.getPosition());
		v.setPosition(viewPos);
	}
}

	private static class JActionMenuItem extends JMenuItem
	implements ActionListener {
		private final MenuItem de;

		public JActionMenuItem(MenuItem item) {
			super(item.menuName);
			de = item;
			this.setEnabled(item.enabled);
			this.addActionListener(this);
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			de.action();
		}
	}

	/**
	 * A miscelaneous utility to populate a JPopupMenu with a list of DisplayEntity menu items (for the right click menu)
	 * @param menu
	 * @param menuItems
	 */
	public static void populateMenu(JPopupMenu menu, Entity ent, int x, int y) {
		ArrayList<MenuItem> menuItems = getMenuItems(ent, x, y);
		for (MenuItem item : menuItems) {
			menu.add(new JActionMenuItem(item));
		}
	}

	private static ArrayList<MenuItem> getMenuItems(Entity ent, int x, int y) {
		ArrayList<MenuItem> list = new ArrayList<>();
		list.add(new InputMenuItem(ent));
		list.add(new OutputMenuItem(ent));
		list.add(new PropertyMenuItem(ent));

		if (!ent.testFlag(Entity.FLAG_GENERATED))
			list.add(new DuplicateMenuItem(ent));

		list.add(new DeleteMenuItem(ent));

		if (ent instanceof DisplayEntity) {
			DisplayEntity dEnt = (DisplayEntity)ent;
			if (RenderManager.isGood())
				list.add(new GraphicsMenuItem(dEnt, x, y));

			if (RenderManager.isGood()) {
				View v = RenderManager.inst().getActiveView();
				if (v != null) {
					LabelMenuItem item = new LabelMenuItem(dEnt);
					if (dEnt instanceof EntityLabel || EntityLabel.getLabel(dEnt) != null)
						item.enabled = false;
					list.add(item);
					list.add(new CenterInViewMenuItem(dEnt, v));
				}
			}
		}

		if (ent instanceof MenuItemEntity)
			((MenuItemEntity)ent).gatherMenuItems(list, x, y);

		return list;
	}

	static class MyMouseListener implements MouseListener {
		private final JPopupMenu menu= new JPopupMenu();

		@Override
		public void mouseClicked(MouseEvent e) {

			if(e.getButton() != MouseEvent.BUTTON3)
				return;

			if(currentEntity == null)
				return;

			// Right mouse click on a movable DisplayEntity
			menu.removeAll();
			ObjectSelector.populateMenu(menu, currentEntity, e.getX(), e.getY());
			menu.show(e.getComponent(), e.getX(), e.getX());
		}
		@Override
		public void mouseEntered(MouseEvent e) {}
		@Override
		public void mouseExited(MouseEvent e) {}
		@Override
		public void mousePressed(MouseEvent e) {}
		@Override
		public void mouseReleased(MouseEvent e) {}
	}

	static class MyKeyListener implements KeyListener {
		@Override
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
		@Override
		public void keyPressed(KeyEvent e) {}
		@Override
		public void keyTyped(KeyEvent e) {}
	}

	static class MyFocusListener implements WindowFocusListener {
		@Override
		public void windowGainedFocus(WindowEvent arg0) {}

		@Override
		public void windowLostFocus(WindowEvent e) {
			// Complete any editing that has started
			ObjectSelector.myInstance.tree.stopEditing();
		}
	}

}
