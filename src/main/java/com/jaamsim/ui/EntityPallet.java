/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2011 Ausenco Engineering Canada Inc.
 * Copyright (C) 2019 JaamSim Software Inc.
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

import java.awt.Component;
import java.awt.Cursor;
import java.awt.Point;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DragSource;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseEvent;
import java.util.HashMap;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import com.jaamsim.basicsim.JaamSimModel;
import com.jaamsim.basicsim.ObjectType;
import com.jaamsim.basicsim.Simulation;
import com.jaamsim.controllers.RenderManager;

public class EntityPallet extends OSFixJFrame implements DragGestureListener {

	private static EntityPallet myInstance;  // only one instance allowed to be open

	private final JScrollPane treeView;
	private final JTree tree;

	private final DefaultMutableTreeNode top;
	private final DefaultTreeModel treeModel;

	private EntityPallet() {

		super( "Model Builder" );
		setType(Type.UTILITY);
		setAutoRequestFocus(false);
		// Make the x button do the same as the close button
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		addWindowListener(FrameBox.getCloseListener("ShowModelBuilder"));

		tree = new MyTree();
		tree.setRootVisible(false);
		tree.setShowsRootHandles(true);
		DragSource dragSource = new DragSource();
		dragSource.createDefaultDragGestureRecognizer(tree, DnDConstants.ACTION_COPY, this);

		top = new DefaultMutableTreeNode();
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

		Simulation simulation = GUIFrame.getJaamSimModel().getSimulation();
		GUIFrame gui = GUIFrame.getInstance();
		Point pt = gui.getGlobalLocation(simulation.getModelBuilderPos().get(0),
				simulation.getModelBuilderPos().get(1));
		setLocation(pt);
		setSize(simulation.getModelBuilderSize().get(0), simulation.getModelBuilderSize().get(1));

		addComponentListener(new ComponentAdapter() {

			@Override
			public void componentMoved(ComponentEvent e) {
				Point pt = gui.getRelativeLocation(getLocation().x, getLocation().y);
				Simulation simulation = GUIFrame.getJaamSimModel().getSimulation();
				simulation.setModelBuilderPos(pt.x, pt.y);
			}

			@Override
			public void componentResized(ComponentEvent e) {
				Simulation simulation = GUIFrame.getJaamSimModel().getSimulation();
				simulation.setModelBuilderSize(getSize().width, getSize().height);
			}
		});
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

	public static void update() {
		SwingUtilities.invokeLater(new RunnableUpdater());
	}

	private static class RunnableUpdater implements Runnable {

		@Override
		public void run() {
			EntityPallet.getInstance().updateTree();
		}
	}

	private void updateTree() {

		// Create a tree that allows one selection at a time
		top.removeAllChildren();
		HashMap<String, DefaultMutableTreeNode> paletteNodes = new HashMap<>();
		JaamSimModel simModel = GUIFrame.getJaamSimModel();
		for (ObjectType type : simModel.getObjectTypes()) {
			if (!type.isDragAndDrop())
				continue;

			String pName = type.getPaletteName();
			DefaultMutableTreeNode palNode = paletteNodes.get(pName);
			if (palNode == null) {
				palNode = new DefaultMutableTreeNode(pName, true);
				paletteNodes.put(pName, palNode);
				top.add(palNode);
			}

			DefaultMutableTreeNode classNode = new DefaultMutableTreeNode(type, true);
			palNode.add(classNode);
		}
		treeModel.reload(top);
	}

	public synchronized static EntityPallet getInstance() {

		if (myInstance == null) {
			myInstance = new EntityPallet();
			myInstance.updateTree();
		}

		return myInstance;
	}

	/**
	 * Disposes the only instance of the entity pallet
	 */
	public synchronized static void clear() {
		if (myInstance != null) {
			myInstance.dispose();
			myInstance = null;
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
			this.setText(type.getName());

			if (!RenderManager.isGood())
				return this;

			if (type.getIconImage() == null)
				return this;

			icon.setImage(type.getIconImage());
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
			return GUIFrame.formatToolTip(ot.getName(), ot.getDescription());
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
