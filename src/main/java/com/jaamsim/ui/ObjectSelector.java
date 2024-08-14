/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2011 Ausenco Engineering Canada Inc.
 * Copyright (C) 2018-2024 JaamSim Software Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jaamsim.ui;

import java.awt.Point;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;

import javax.swing.JFrame;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.ToolTipManager;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import com.jaamsim.DisplayModels.IconModel;
import com.jaamsim.basicsim.Entity;
import com.jaamsim.basicsim.EntityIterator;
import com.jaamsim.basicsim.ErrorException;
import com.jaamsim.basicsim.JaamSimModel;
import com.jaamsim.basicsim.ObjectType;
import com.jaamsim.basicsim.Simulation;
import com.jaamsim.input.Input;
import com.jaamsim.units.Unit;

public class ObjectSelector extends FrameBox {
	private static ObjectSelector myInstance;

	// Tree view properties
	private final DefaultMutableTreeNode top;
	private final DefaultTreeModel treeModel;
	private final JTree tree;
	private final JScrollPane treeView;
	static Entity currentEntity;

	private long entSequence;

	private final static int MAX_FOR_REALTIME_UPDATE = 10000;

	public ObjectSelector() {
		super( "Object Selector" );
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		addWindowListener(FrameBox.getCloseListener("ShowObjectSelector"));
		addWindowFocusListener(new MyFocusListener());

		top = new DefaultMutableTreeNode();
		treeModel = new DefaultTreeModel(top);
		tree = new JTree() {
			@Override
			public String getToolTipText(MouseEvent e) {
				TreePath path = getPathForLocation(e.getX(), e.getY());
				if (path == null)
					return "";

				DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
				if (node == null)
					return "";

				Object object = node.getUserObject();
				if (!(object instanceof Entity))
					return "";
				Entity ent = (Entity) object;

				return GUIFrame.formatToolTip(ent.getName(), ent.getDescription());
			}

			@Override
			public Point getToolTipLocation(MouseEvent e) {
				TreePath path = getPathForLocation(e.getX(), e.getY());
				if (path == null)
					return null;
				return new Point(treeView.getWidth(), getPathBounds(path).y);
			}
		};
		tree.setModel(treeModel);
		tree.getSelectionModel().setSelectionMode( TreeSelectionModel.SINGLE_TREE_SELECTION );
		tree.setRootVisible(false);
		tree.setShowsRootHandles(true);
		tree.setInvokesStopCellEditing(true);

		treeView = new JScrollPane(tree);
		getContentPane().add(treeView);

		entSequence = 0;

		addComponentListener(FrameBox.getSizePosAdapter(this, "ObjectSelectorSize", "ObjectSelectorPos"));

		tree.addTreeSelectionListener( new MyTreeSelectionListener() );
		treeModel.addTreeModelListener( new MyTreeModelListener(tree) );

		tree.addMouseListener(new MyMouseListener());
		tree.addKeyListener(new MyKeyListener());

		ToolTipManager.sharedInstance().registerComponent(tree);
		ToolTipManager.sharedInstance().setDismissDelay(600000);
	}

	@Override
	public void setVisible(boolean bool) {
		if (bool && !isVisible()) {
			myInstance.treeView.getHorizontalScrollBar().getModel().setValue(0);
			myInstance.treeView.getVerticalScrollBar().getModel().setValue(0);
		}
		super.setVisible(bool);
	}

	@Override
	public void setEntity(Entity ent) {

		if (ent == currentEntity)
			return;
		currentEntity = ent;

		if (tree == null)
			return;

		JaamSimModel simModel = GUIFrame.getJaamSimModel();
		if (simModel == null || simModel.getSimulation() == null)
			return;

		long curSequence = simModel.getEntitySequence();
		if (entSequence != curSequence) {
			entSequence = curSequence;
			updateTree(simModel);
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

		// Entity not found in the tree
		tree.setSelectionPath(null);
		tree.setEditable(false);
	}

	@Override
	public void updateValues(double simTime) {
		GUIFrame gui = GUIFrame.getInstance();
		JaamSimModel simModel = GUIFrame.getJaamSimModel();
		if (simModel == null || simModel.getSimulation() == null)
			return;
		if (!this.isVisible() || gui == null || (simModel.isRunningState()
				&& (!simModel.isRealTime() || simModel.getEntityCount() > MAX_FOR_REALTIME_UPDATE)))
			return;

		long curSequence = simModel.getEntitySequence();
		if (entSequence != curSequence) {
			entSequence = curSequence;
			updateTree(simModel);
		}
	}

	public static void allowUpdate() {
		if (myInstance == null)
			return;
		myInstance.entSequence = 0;
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

	private void updateTree(JaamSimModel simModel) {

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
		Simulation simulation = simModel.getSimulation();
		top.add(new DefaultMutableTreeNode(simulation, false));

		// Create the tree structure for palettes and object types in the correct order
		for (int i = 0; i < simModel.getObjectTypes().size(); i++) {
			try {
				final ObjectType type = simModel.getObjectTypes().get(i);
				if (type == null)
					continue;
				String paletteName = type.getPaletteName();
				String typeName = type.getName();

				// Find or create the node for the palette
				DefaultMutableTreeNode paletteNode = getNodeFor_In(paletteName, top);
				if (paletteNode == null) {
					paletteNode = new DefaultMutableTreeNode(paletteName);
					top.add(paletteNode);
				}

				// Add the node for the Object Type to the palette
				if (typeName == null || typeName.equals(paletteName))
					continue;
				DefaultMutableTreeNode typeNode = new DefaultMutableTreeNode(typeName, true);
				paletteNode.add(typeNode);
			}
			catch (IndexOutOfBoundsException e) {}
		}

		// Prepare a sorted list of entities
		EntityIterator<Entity> entIt = GUIFrame.getJaamSimModel().getClonesOfIterator(Entity.class);
		ArrayList<Entity> entityList = new ArrayList<>();
		for (Entity ent : entIt) {
			try {

				// The instance for Simulation has already been added
				if (ent == simulation)
					continue;

				// Do not include the units
				if (ent instanceof Unit)
					continue;

				// Do not include the icons for objects
				if (ent instanceof IconModel)
					continue;

				// Do not include pool entities
				if (ent.isPooled())
					continue;

				entityList.add(ent);
			}
			catch (IndexOutOfBoundsException e) {}
		}
		try {
			Collections.sort(entityList, selectorSortOrder);
		}
		catch (Throwable t) {}

		// Loop through the entities in the model
		for (int i=0; i<entityList.size(); i++) {
			try {
				final Entity ent = entityList.get(i);

				// Determine the object type for this entity
				final ObjectType type = ent.getObjectType();
				if (type == null)
					continue;
				String paletteName = type.getPaletteName();
				String typeName = type.getName();

				// Find the palette node for this entity
				DefaultMutableTreeNode paletteNode = getNodeFor_In(paletteName, top);
				if (paletteNode == null)
					continue;

				// Find the object type node for this entity
				DefaultMutableTreeNode typeNode = getNodeFor_In(typeName, paletteNode);
				if (typeName != null && typeName.equals(paletteName)) {
					typeNode = paletteNode;
				}
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
		Enumeration<?> paletteEnum = top.children();
		while (paletteEnum.hasMoreElements()) {
			DefaultMutableTreeNode paletteNode = (DefaultMutableTreeNode)paletteEnum.nextElement();
			Enumeration<?> typeEnum = paletteNode.children();
			while (typeEnum.hasMoreElements()) {
				DefaultMutableTreeNode typeNode = (DefaultMutableTreeNode)typeEnum.nextElement();
				if (!typeNode.getAllowsChildren())
					continue;
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
			DefaultMutableTreeNode paletteNode = (DefaultMutableTreeNode)paletteEnum.nextElement();

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

	private static class EntityComparator implements Comparator<Entity> {
		@Override
		public int compare(Entity ent0, Entity ent1) {

			// Put any null entities at the end of the list
			if (ent0 == null && ent1 == null)
				return 0;
			if (ent0 != null && ent1 == null)
				return -1;
			if (ent0 == null && ent1 != null)
				return 1;

			// Otherwise, sort in natural order
			return Input.uiSortOrder.compare(ent0, ent1);
		}
	}
	private static final Comparator<Entity> selectorSortOrder = new EntityComparator();

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
		Enumeration<?> enumeration = parent.children();
		while (enumeration.hasMoreElements()) {
			DefaultMutableTreeNode eachNode = (DefaultMutableTreeNode)enumeration.nextElement();
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
				FrameBox.setSelectedEntity((Entity)userObj, false);
			}
			else {
				FrameBox.setSelectedEntity(null, false);
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
				GUIFrame.getInstance().renameEntity(currentEntity, newName);
			}
			catch (ErrorException err) {
				GUIFrame.showErrorDialog("Input Error", err.getMessage());
			}
			finally {
				node.setUserObject(currentEntity);
				GUIFrame.updateUI();
			}
		}

		@Override
		public void treeNodesInserted(TreeModelEvent e) {}
		@Override
		public void treeNodesRemoved(TreeModelEvent e) {}
		@Override
		public void treeStructureChanged(TreeModelEvent e) {}
	}

	static class MyMouseListener implements MouseListener {
		private final JPopupMenu menu= new JPopupMenu();
		@Override
		public void mouseClicked(MouseEvent e) {}
		@Override
		public void mouseEntered(MouseEvent e) {}
		@Override
		public void mouseExited(MouseEvent e) {}
		@Override
		public void mousePressed(MouseEvent e) {}

		@Override
		public void mouseReleased(MouseEvent e) {
			// Show context menu for right-click
			if (e.getButton() != MouseEvent.BUTTON3 || currentEntity == null)
				return;
			menu.removeAll();
			ContextMenu.populateMenu(menu, currentEntity, -1, e.getComponent(), e.getX(), e.getY());
			menu.show(e.getComponent(), e.getX(), e.getY());
		}
	}

	static class MyKeyListener implements KeyListener {

		@Override
		public void keyReleased(KeyEvent e) {
			if (e.getKeyCode() != KeyEvent.VK_DELETE)
				return;

			try {
				GUIFrame.getInstance().deleteEntity(currentEntity);
				FrameBox.setSelectedEntity(null, false);
			}
			catch (ErrorException err) {
				GUIFrame.invokeErrorDialog("User Error", err.getMessage());
			}
		}

		@Override
		public void keyPressed(KeyEvent e) {
			int keyCode = e.getKeyCode();
			boolean control = e.isControlDown();
			if (control && keyCode == KeyEvent.VK_C) {
				if (currentEntity != null) {
					GUIFrame.getInstance().copyAction(currentEntity);
				}
			}
			if (control && keyCode == KeyEvent.VK_V) {
				GUIFrame.getInstance().pasteAction(currentEntity);
			}
			if (keyCode == KeyEvent.VK_F1) {
				String topic = "";
				if (currentEntity != null)
					topic = currentEntity.getObjectType().getName();
				HelpBox.getInstance().showDialog(topic);
			}
		}

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
