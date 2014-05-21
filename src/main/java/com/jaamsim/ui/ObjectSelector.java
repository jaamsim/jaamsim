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
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Locale;

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
import com.jaamsim.input.Input;
import com.jaamsim.input.InputAgent;
import com.jaamsim.math.Vec3d;
import com.sandwell.JavaSimulation.Entity;
import com.sandwell.JavaSimulation.ObjectType;
import com.sandwell.JavaSimulation.Simulation;
import com.sandwell.JavaSimulation3D.DisplayEntity;
import com.sandwell.JavaSimulation3D.GUIFrame;
import com.sandwell.JavaSimulation3D.Text;

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

		top = new DefaultMutableTreeNode( "Defined Objects");
		treeModel = new DefaultTreeModel(top);
		tree = new JTree();
		tree.setModel(treeModel);
		tree.getSelectionModel().setSelectionMode( TreeSelectionModel.SINGLE_TREE_SELECTION );
		updateTree();

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

		ArrayList<String> palettes = new ArrayList<String>();
		for (int j = 0; j < ObjectType.getAll().size(); j++) {
			ObjectType type = null;
			try {
				type = ObjectType.getAll().get(j);
			}
			catch (IndexOutOfBoundsException e) {
				break;
			}
			if (!palettes.contains(type.getPaletteName()))
				palettes.add(type.getPaletteName());
		}

		for (int k = 0; k < palettes.size(); k++) {
			String palName = palettes.get(k);
			DefaultMutableTreeNode palNode = getNodeFor_In(palName, top);
			for (int j = 0; j < ObjectType.getAll().size(); j++) {
				ObjectType type = null;
				try {
					type = ObjectType.getAll().get(j);
				}
				catch (IndexOutOfBoundsException e) {
					break;
				}
				if(!palName.equals( type.getPaletteName()))
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
			String newName = ((String)node.getUserObject()).trim();

			if (newName.contains(" ") || newName.contains("\t") || newName.contains("{") || newName.contains("}")) {
				LogBox.format("Error: Entity names cannot contain spaces, tabs, { or }: %s", newName);
				LogBox.getInstance().setVisible(true);
				node.setUserObject(currentEntity);
				return;
			}

			// Check that the name has not been used already
			Entity existingEnt = Input.tryParseEntity(newName, Entity.class);
			if( existingEnt != null ) {
				LogBox.format("Error: Entity name: %s is already in use.", newName);
				LogBox.getInstance().setVisible(true);
				node.setUserObject(currentEntity);
				return;
			}

			// Rename the entity
			currentEntity.setInputName(newName);
			node.setUserObject(currentEntity);
			FrameBox.setSelectedEntity(currentEntity);
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
		InputAgent.processEntity_Keyword_Value(Simulation.getInstance(), "ShowInputEditor", "TRUE");
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
		InputAgent.processEntity_Keyword_Value(Simulation.getInstance(), "ShowPropertyViewer", "TRUE");
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
		InputAgent.processEntity_Keyword_Value(Simulation.getInstance(), "ShowOutputViewer", "TRUE");
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
				ent.getInputName(), "_Copy", true);

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
			InputAgent.processEntity_Keyword_Value(dEnt, "Position", String.format((Locale)null, "%.6f %.6f %.6f m", pos.x, pos.y, pos.z ));
			FrameBox.valueUpdate();
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
		Text label = InputAgent.defineEntityWithUniqueName(Text.class, "Text", "", true);

		InputAgent.processEntity_Keyword_Value(label, "RelativeEntity", ent.getInputName() );
		if (ent.getCurrentRegion() != null)
			InputAgent.processEntity_Keyword_Value(label, "Region", ent.getCurrentRegion().getInputName());

		InputAgent.processEntity_Keyword_Value(label, "Position", "0.0 -0.65 0.0 m" );
		InputAgent.processEntity_Keyword_Value(label, "TextHeight", "0.15 m" );
		InputAgent.processEntity_Keyword_Value(label, "Format", "%s");
		InputAgent.processEntity_Keyword_Value(label, "OutputName", String.format("%s  Name", ent.getInputName()) );

		FrameBox.setSelectedEntity(label);
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
		ArrayList<MenuItem> list = new ArrayList<MenuItem>();
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
