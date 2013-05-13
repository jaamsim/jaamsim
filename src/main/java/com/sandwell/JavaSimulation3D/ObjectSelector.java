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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
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

import com.jaamsim.controllers.RenderManager;
import com.jaamsim.input.InputAgent;
import com.jaamsim.math.Vec3d;
import com.jaamsim.ui.FrameBox;
import com.jaamsim.ui.PropertyBox;
import com.jaamsim.ui.View;
import com.sandwell.JavaSimulation.Entity;
import com.sandwell.JavaSimulation.Input;
import com.sandwell.JavaSimulation.ObjectType;
import com.sandwell.JavaSimulation.Palette;

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

		top = new DefaultMutableTreeNode( "Defined Objects");
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

	@Override
	public void setEntity(Entity ent) {
		if(ent == currentEntity || ! this.isVisible())
			return;

		currentEntity = ent;
		long curSequence = Entity.getEntitySequence();
		if (entSequence != curSequence) {
			entSequence = curSequence;
			updateTree();
		}

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

	/**
	 * Returns the only instance of the Object Selector
	 */
	public static synchronized ObjectSelector getInstance() {
		if (myInstance == null)
			myInstance = new ObjectSelector();

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
			DefaultMutableTreeNode palNode = getNodeFor_In(p.getName(), top);
			for( ObjectType type : ObjectType.getAll() ) {
				if( type.getPalette() != p )
					continue;

				Class<? extends Entity> proto = type.getJavaClass();
				// skip unused classes
				DefaultMutableTreeNode classNode = getNodeFor_In(proto.getSimpleName(), palNode);
				if (!used.contains(proto)) {
					if( classNode != null ) {
						classNode.removeAllChildren();
						classNode.removeFromParent();
					}
					continue;
				}

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
					if(palNode.getIndex(classNode) < 0) {
						palNode.add(classNode);
					}
				}
				else if( palNode.getIndex(classNode) >= 0) {
					palNode.remove(classNode);
				}
			}

			// Palette node is not empty
			if(!palNode.isLeaf()) {
				if(top.getIndex(palNode) < 0)
					top.add(palNode);
			}
			else if(top.getIndex(palNode) >= 0) {
				top.remove(palNode);
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
		@Override
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

		@Override
		public void treeNodesChanged( TreeModelEvent e ) {

			DefaultMutableTreeNode node = (DefaultMutableTreeNode)tree.getLastSelectedPathComponent();
			String newName = (String)node.getUserObject();
			currentEntity.setInputName(newName);
			node.setUserObject(currentEntity);
			FrameBox.valueUpdate();
		}

		@Override
		public void treeNodesInserted(TreeModelEvent e) {}
		@Override
		public void treeNodesRemoved(TreeModelEvent e) {}
		@Override
		public void treeStructureChanged(TreeModelEvent e) {}
	}


public static abstract class DEMenuItem {
	public String menuName;

	public DEMenuItem(String name) {
		menuName = name;
	}

	public abstract void action();
}

static class InputMenuItem extends DEMenuItem {
	private final Entity ent;
	public InputMenuItem(Entity ent) {
		super("Input Editor");
		this.ent = ent;
	}

	@Override
	public void action() {
		EditBox.getInstance().setVisible(true);
		EditBox.getInstance().setExtendedState(JFrame.NORMAL);
		EditBox.getInstance().toFront();
		FrameBox.setSelectedEntity(ent);
	}
}

static class PropertyMenuItem extends DEMenuItem {
	private final Entity ent;
	public PropertyMenuItem(Entity ent) {
		super("Property Viewer");
		this.ent = ent;
	}

	@Override
	public void action() {
		PropertyBox.getInstance().setVisible(true);
		PropertyBox.getInstance().setExtendedState(JFrame.NORMAL);
		PropertyBox.getInstance().toFront();
		FrameBox.setSelectedEntity(ent);
	}
}

static class InfoMenuItem extends DEMenuItem {
	private final Entity ent;
	public InfoMenuItem(Entity ent) {
		super("Info Viewer");
		this.ent = ent;
	}

	@Override
	public void action() {
		InfoBox.getInstance().setVisible(true);
		InfoBox.getInstance().setExtendedState(JFrame.NORMAL);
		InfoBox.getInstance().toFront();
		FrameBox.setSelectedEntity(ent);
	}
}

static class OutputMenuItem extends DEMenuItem {
	private final Entity ent;
	public OutputMenuItem(Entity ent) {
		super("Output Viewer");
		this.ent = ent;
	}

	@Override
	public void action() {
		OutputBox.getInstance().setVisible(true);
		OutputBox.getInstance().setExtendedState(JFrame.NORMAL);
		OutputBox.getInstance().toFront();
		FrameBox.setSelectedEntity(ent);
	}
}

static class DuplicateMenuItem extends DEMenuItem {
	private final Entity ent;
	public DuplicateMenuItem(Entity ent) {
		super("Duplicate");
		this.ent = ent;
	}

	@Override
	public void action() {
		Entity copiedEntity = InputAgent.defineEntityWithUniqueName(ent.getClass(),
				String.format("Copy_of_%s", ent.getInputName()), true);

		// Match all the inputs
		for(Input<?> each: ent.getEditableInputs() ){
			String val = each.getValueString();
			if (val.isEmpty())
				continue;

			Input<?> copiedInput = copiedEntity.getInput(each.getKeyword());
			InputAgent.processEntity_Keyword_Value(copiedEntity, copiedInput, val);
		}
		if (copiedEntity instanceof DisplayEntity) {
			DisplayEntity dEnt = (DisplayEntity)copiedEntity;

			Vec3d pos = dEnt.getPosition();
			pos.x += 0.5d * dEnt.getSize().x;
			pos.y += 0.5d * dEnt.getSize().y;

			dEnt.setPosition(pos);
		}
		FrameBox.setSelectedEntity(copiedEntity);
	}
}

static class DeleteMenuItem extends DEMenuItem {
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

static class GraphicsMenuItem extends DEMenuItem {
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

static class LabelMenuItem extends DEMenuItem {
	private final DisplayEntity ent;

	public LabelMenuItem(DisplayEntity ent) {
		super("Add Label");
		this.ent = ent;
	}

	@Override
	public void action() {
		TextLabel label = InputAgent.defineEntityWithUniqueName(TextLabel.class,
		                  String.format("Label_for_%s", ent.getInputName()), true);

		InputAgent.processEntity_Keyword_Value(label, "RelativeEntity", ent.getInputName() );
		if (ent.getCurrentRegion() != null)
			InputAgent.processEntity_Keyword_Value(label, "Region", ent.getCurrentRegion().getInputName());

		InputAgent.processEntity_Keyword_Value(label, "Position", "0.0 -1.0 0.0 m" );
		InputAgent.processEntity_Keyword_Value(label, "Text", ent.getInputName());

		FrameBox.setSelectedEntity(label);
	}
}

static class CenterInViewMenuItem extends DEMenuItem {
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
		private final DEMenuItem de;

		public JActionMenuItem(DEMenuItem item) {
			super(item.menuName);
			de = item;
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
		ArrayList<DEMenuItem> menuItems = getMenuItems(ent, x, y);
		for (ObjectSelector.DEMenuItem item : menuItems) {
			menu.add(new JActionMenuItem(item));
		}
	}

	private static ArrayList<DEMenuItem> getMenuItems(Entity ent, int x, int y) {
		ArrayList<DEMenuItem> list = new ArrayList<DEMenuItem>();
		list.add(new InputMenuItem(ent));
		list.add(new PropertyMenuItem(ent));
		list.add(new InfoMenuItem(ent));
		list.add(new OutputMenuItem(ent));

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
					list.add(new LabelMenuItem(dEnt));
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
}
