/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2016-2026 JaamSim Software Inc.
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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.ArrayList;

import javax.swing.ImageIcon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.KeyStroke;

import com.jaamsim.Commands.Command;
import com.jaamsim.Commands.CoordinateCommand;
import com.jaamsim.Commands.DefineCommand;
import com.jaamsim.Commands.KeywordCommand;
import com.jaamsim.Commands.ListCommand;
import com.jaamsim.Graphics.DisplayEntity;
import com.jaamsim.Graphics.EditableText;
import com.jaamsim.Graphics.EntityLabel;
import com.jaamsim.Graphics.OverlayEntity;
import com.jaamsim.Graphics.PolylineInfo;
import com.jaamsim.Graphics.View;
import com.jaamsim.SubModels.CompoundEntity;
import com.jaamsim.SubModels.SubModel;
import com.jaamsim.basicsim.Entity;
import com.jaamsim.basicsim.GUIListener;
import com.jaamsim.basicsim.JaamSimModel;
import com.jaamsim.basicsim.Log;
import com.jaamsim.basicsim.Simulation;
import com.jaamsim.controllers.RenderManager;
import com.jaamsim.input.InputAgent;
import com.jaamsim.input.KeywordIndex;
import com.jaamsim.math.Vec3d;
import com.jaamsim.units.DistanceUnit;

public class ContextMenu {

	private static final ArrayList<ContextMenuItem> menuItems = new ArrayList<>();

	static {
		ContextMenu.addCustomMenuHandler(new ExportColladaModelHandler());
	}

	private ContextMenu() {}

	public static final void addCustomMenuHandler(ContextMenuItem i) {
		synchronized (menuItems) {
			menuItems.add(i);
		}
	}

	private static class UIMenuItem extends JMenuItem implements ActionListener {
		final ContextMenuItem i;
		final Entity ent;
		final int x;
		final int y;

		UIMenuItem(ContextMenuItem i, Entity ent, int x, int y) {
			super(i.getMenuText());
			this.i = i;
			this.ent = ent;
			this.x = x;
			this.y = y;
			this.addActionListener(this);
		}

		@Override
		public void actionPerformed(ActionEvent event) {
			i.performAction(ent, x, y);
		}
	}

	/**
	 * Adds menu items to the right click (context) menu for the specified entity.
	 * @param ent - entity whose context menu is to be generated
	 * @param menu - context menu to be populated with menu items
	 * @param x - screen coordinate for the menu
	 * @param y - screen coordinate for the menu
	 */
	public static void populateMenu(JPopupMenu menu, final Entity ent, final int nodeIndex,
			Component c, final int x, final int y) {
		// 1) Input Editor
		JMenuItem inputEditorMenuItem = new JMenuItem( "Input Editor" );
		inputEditorMenuItem.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed( ActionEvent event ) {
				GUIFrame.getInstance().clearPresentationMode();
				InputAgent.applyBoolean(ent.getSimulation(), "ShowInputEditor", true);
				FrameBox.setSelectedEntity(ent, false);
			}
		} );
		if (ent == null) {
			inputEditorMenuItem.setEnabled(false);
		}
		menu.add( inputEditorMenuItem );

		// 2) Output Viewer
		JMenuItem outputViewerMenuItem = new JMenuItem( "Output Viewer" );
		outputViewerMenuItem.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed( ActionEvent event ) {
				GUIFrame.getInstance().clearPresentationMode();
				InputAgent.applyBoolean(ent.getSimulation(), "ShowOutputViewer", true);
				FrameBox.setSelectedEntity(ent, false);
			}
		} );
		if (ent == null) {
			outputViewerMenuItem.setEnabled(false);
		}
		menu.add( outputViewerMenuItem );

		// 3) Property Viewer
		JMenuItem propertyViewerMenuItem = new JMenuItem( "Property Viewer" );
		propertyViewerMenuItem.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed( ActionEvent event ) {
				GUIFrame.getInstance().clearPresentationMode();
				InputAgent.applyBoolean(ent.getSimulation(), "ShowPropertyViewer", true);
				FrameBox.setSelectedEntity(ent, false);
			}
		} );
		if (ent == null) {
			propertyViewerMenuItem.setEnabled(false);
		}
		menu.add( propertyViewerMenuItem );
		menu.addSeparator();

		// 4) Copy
		JMenuItem copyMenuItem = new JMenuItem( "Copy" );
		copyMenuItem.setIcon( new ImageIcon(
				GUIFrame.class.getResource("/resources/images/Copy-16.png")) );
		copyMenuItem.setAccelerator(KeyStroke.getKeyStroke(
		        KeyEvent.VK_C, ActionEvent.CTRL_MASK));
		copyMenuItem.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed( ActionEvent event ) {
				GUIFrame.getInstance().copyAction(ent);
			}
		} );
		if (ent == null || ent.isGenerated() || ent == ent.getSimulation()) {
			copyMenuItem.setEnabled(false);
		}
		menu.add( copyMenuItem );

		// 4) Paste
		JMenuItem pasteMenuItem = new JMenuItem( "Paste" );
		pasteMenuItem.setIcon( new ImageIcon(
				GUIFrame.class.getResource("/resources/images/Paste-16.png")) );
		pasteMenuItem.setAccelerator(KeyStroke.getKeyStroke(
		        KeyEvent.VK_V, ActionEvent.CTRL_MASK));
		pasteMenuItem.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed( ActionEvent event ) {
				GUIFrame.getInstance().pasteAction(ent);
			}
		} );
		if (!(ent instanceof EditableText && ((EditableText) ent).isEditMode())
				&& GUIFrame.getInstance().getEntityFromClipboard() == null) {
			pasteMenuItem.setEnabled(false);
		}
		menu.add( pasteMenuItem );

		// 5) Paste Clone
		JMenuItem pasteCloneMenuItem = new JMenuItem( "Paste Clone" );
		pasteCloneMenuItem.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed( ActionEvent event ) {
				GUIFrame.getInstance().pasteCloneAction(ent);
				GUIFrame.getInstance().updateModelBuilder();
			}
		} );
		if (!(ent instanceof EditableText && ((EditableText) ent).isEditMode())
				&& GUIFrame.getInstance().getEntityFromClipboard() == null) {
			pasteCloneMenuItem.setEnabled(false);
		}
		menu.add( pasteCloneMenuItem );

		// 6) Delete
		JMenuItem deleteMenuItem = new JMenuItem( "Delete" );
		deleteMenuItem.setAccelerator(KeyStroke.getKeyStroke(
		        KeyEvent.VK_DELETE, 0));
		deleteMenuItem.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed( ActionEvent event ) {
				GUIFrame.getInstance().deleteAction(ent);
			}
		} );
		if (ent == null) {
			deleteMenuItem.setEnabled(false);
		}
		menu.add( deleteMenuItem );

		// DisplayEntity menu items
		if (ent instanceof DisplayEntity && !(ent instanceof OverlayEntity)) {
			menu.addSeparator();
			ContextMenu.populateDisplayEntityMenu(menu, (DisplayEntity)ent, nodeIndex, c, x, y);
		}

		// CompoundEntity menu items
		if (ent instanceof CompoundEntity) {
			menu.addSeparator();
			ContextMenu.populateCompoundEntityMenu(menu, (CompoundEntity)ent, nodeIndex, c, x, y);
			if (ent instanceof SubModel && !ent.isClone()) {
				ContextMenu.populateSubModelMenu(menu, (SubModel)ent, nodeIndex, c, x, y);
			}
		}

		synchronized (menuItems) {
			for (ContextMenuItem each : menuItems) {
				if (each.supportsEntity(ent)) {
					menu.addSeparator();
					menu.add(new UIMenuItem(each, ent, x, y));
				}
			}
		}
	}

	public static void populateDisplayEntityMenu(JPopupMenu menu, final DisplayEntity ent, final int nodeIndex,
			final Component c, final int x, final int y) {

		if (!RenderManager.isGood())
			return;

		// 1) Change Graphics
		JMenuItem changeGraphicsMenuItem = new JMenuItem( "Change Graphics" );
		changeGraphicsMenuItem.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed( ActionEvent event ) {
				GraphicBox graphicBox = GraphicBox.getInstance(ent, c, x, y);
				graphicBox.setVisible( true );
			}
		} );
		if (ent.getDisplayModelList() == null) {
			changeGraphicsMenuItem.setEnabled(false);
		}
		menu.add( changeGraphicsMenuItem );

		// 2) Show Label
		final EntityLabel label = EntityLabel.getLabel(ent);
		boolean bool = label != null && label.getShowInput();
		final JMenuItem showLabelMenuItem = new JCheckBoxMenuItem( "Show Label", bool );
		showLabelMenuItem.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed( ActionEvent event ) {
				EntityLabel.showLabel(ent, showLabelMenuItem.isSelected());
			}
		} );
		showLabelMenuItem.setEnabled(EntityLabel.canLabel(ent));
		menu.add( showLabelMenuItem );

		// 3) Set Parent and RelativeEntity
		ScrollableMenu setParentMenu = new ScrollableMenu( "Set Parent and RelativeEntity" ) {

			@Override
			public void setPopupMenuVisible(boolean bool) {
				super.setPopupMenuVisible(bool);
				Entity presentParent = ent.getParent();
				if (!bool || presentParent == null)
					return;
				int index = ent.getParentOptions().indexOf(presentParent.getName());
				if (index != -1) {
					ensureIndexIsVisible(index + 1);  // Allows for the option <None>
				}
			}
		};
		ArrayList<String> parentNameList = new ArrayList<>();
		parentNameList.addAll(ent.getParentOptions());
		parentNameList.retainAll(ent.getRelativeEntityOptions());

		JaamSimModel simModel = ent.getJaamSimModel();
		if (ent.getParent() == null || simModel.getEntity(ent.getLocalName()) == null)
			parentNameList.add(0, "<None>");

		String presentParentName = "<None>";
		if (ent.getParent() != null) {
			presentParentName = ent.getParent().getName();
		}
		for (final String parentName : parentNameList) {
			JRadioButtonMenuItem item = new JRadioButtonMenuItem(parentName);
			if (parentName.equals(presentParentName)) {
				item.setSelected(true);
			}
			item.addActionListener( new ActionListener() {

				@Override
				public void actionPerformed( ActionEvent event ) {
					ArrayList<Command> cmdList = new ArrayList<>();
					if (parentName.equals("<None>")) {
						cmdList.add(new KeywordCommand(ent, InputAgent.formatArgs("Parent")));
						cmdList.add(new CoordinateCommand(ent, InputAgent.formatArgs("RelativeEntity")));
					}
					else {
						cmdList.add(new KeywordCommand(ent, InputAgent.formatArgs("Parent", parentName)));
						cmdList.add(new CoordinateCommand(ent, InputAgent.formatArgs("RelativeEntity", "parent")));
					}
					ent.getJaamSimModel().storeAndExecute(new ListCommand(cmdList));
					GUIListener gui = simModel.getGUIListener();
					if (gui != null)
						gui.updateModelBuilder();
				}
			} );
			setParentMenu.add(item);
		}
		if (ent instanceof EntityLabel	|| ent.isGenerated()) {
			setParentMenu.setEnabled(false);
		}
		menu.add( setParentMenu );

		// 4) Set RelativeEntity
		ScrollableMenu setRelativeEntityMenu = new ScrollableMenu( "Set RelativeEntity" ) {

			@Override
			public void setPopupMenuVisible(boolean bool) {
				super.setPopupMenuVisible(bool);
				if (!bool || ent.getRelativeEntity() == null)
					return;
				String presentEntName = ent.getRelativeEntity().getName();
				int index = ent.getRelativeEntityOptions().indexOf(presentEntName);
				if (index != -1) {
					ensureIndexIsVisible(index + 1);  // Allows for the option <None>
				}
			}
		};
		ArrayList<String> entNameList = new ArrayList<>();
		entNameList.add("<None>");
		entNameList.addAll(ent.getRelativeEntityOptions());
		String presentEntName = "<None>";
		if (ent.getRelativeEntity() != null) {
			presentEntName = ent.getRelativeEntity().getName();
		}
		for (final String entName : entNameList) {
			JRadioButtonMenuItem item = new JRadioButtonMenuItem(entName);
			if (entName.equals(presentEntName)) {
				item.setSelected(true);
			}
			item.addActionListener( new ActionListener() {

				@Override
				public void actionPerformed( ActionEvent event ) {
					KeywordIndex kw;
					if (entName.equals("<None>")) {
						kw = InputAgent.formatArgs("RelativeEntity");
					}
					else {
						kw = InputAgent.formatArgs("RelativeEntity", entName);
					}
					ent.getJaamSimModel().storeAndExecute(new CoordinateCommand(ent, kw));
				}
			} );
			setRelativeEntityMenu.add(item);
		}
		if (ent instanceof EntityLabel	|| ent.isGenerated()) {
			setRelativeEntityMenu.setEnabled(false);
		}
		menu.add( setRelativeEntityMenu );

		// 5) Set Region
		ScrollableMenu setRegionMenu = new ScrollableMenu( "Set Region" ) {

			@Override
			public void setPopupMenuVisible(boolean bool) {
				super.setPopupMenuVisible(bool);
				if (!bool || ent.getCurrentRegion() == null)
					return;
				String presentRegionName = ent.getCurrentRegion().getName();
				int index = ent.getRegionOptions().indexOf(presentRegionName);
				if (index != -1) {
					ensureIndexIsVisible(index + 1);  // Allows for the option <None>
				}
			}
		};
		ArrayList<String> regionNameList = new ArrayList<>();
		regionNameList.add("<None>");
		regionNameList.addAll(ent.getRegionOptions());
		String presentRegionName = "<None>";
		if (ent.getCurrentRegion() != null) {
			presentRegionName = ent.getCurrentRegion().getName();
		}
		for (final String regionName : regionNameList) {
			JRadioButtonMenuItem item = new JRadioButtonMenuItem(regionName);
			if (regionName.equals(presentRegionName)) {
				item.setSelected(true);
			}
			item.addActionListener( new ActionListener() {

				@Override
				public void actionPerformed( ActionEvent event ) {
					KeywordIndex kw;
					if (regionName.equals("<None>")) {
						kw = InputAgent.formatArgs("Region");
					}
					else {
						kw = InputAgent.formatArgs("Region", regionName);
					}
					ent.getJaamSimModel().storeAndExecute(new CoordinateCommand(ent, kw));
				}
			} );
			setRegionMenu.add(item);
		}
		if (ent instanceof EntityLabel	|| ent.isGenerated()) {
			setRegionMenu.setEnabled(false);
		}
		menu.add( setRegionMenu );

		// 6) Centre in View
		JMenuItem centerInViewMenuItem = new JMenuItem( "Center in View" );
		centerInViewMenuItem.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed( ActionEvent event ) {
				View v = RenderManager.getActiveView();
				// Move the camera position so that the entity is in the centre of the screen
				Vec3d viewPos = new Vec3d(v.getViewPosition());
				Vec3d viewCenter = new Vec3d(v.getEffViewCenter());
				Vec3d diff = new Vec3d();
				diff.sub3(ent.getGlobalPosition(), viewCenter);
				viewPos.add3(diff);
				viewCenter.add3(diff);

				RenderManager.inst().setPOI(v, viewCenter);
				KeywordIndex posKw = InputAgent.formatVec3dInput(v, "ViewPosition", viewPos, DistanceUnit.class);
				KeywordIndex ctrKw = InputAgent.formatVec3dInput(v, "ViewCenter", viewCenter, DistanceUnit.class);
				v.getJaamSimModel().storeAndExecute(new KeywordCommand(v, posKw, ctrKw));
			}
		} );
		if (RenderManager.getActiveView() == null) {
			centerInViewMenuItem.setEnabled(false);
		}
		menu.add( centerInViewMenuItem );

		// The following menu items apply only to a polyline type DisplayEntity
		if (!ent.usePointsInput())
			return;
		menu.addSeparator();

		// 7) Add Node
		JMenuItem addNodeItem = new JMenuItem( "Add Node" );
		addNodeItem.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed( ActionEvent event ) {
				ArrayList<Vec3d> pts = ent.getPoints();
				Vec3d pos = RenderManager.inst().getPOI();
				if (pos == null)
					return;
				Vec3d localPos = ent.getLocalPosition(pos);
				final Simulation simulation = ent.getSimulation();
				if (simulation.isSnapToGrid()) {
					localPos = simulation.getSnapGridPosition(localPos);
				}
				int ind = PolylineInfo.getInsertionIndex(pts, localPos);
				pts.add(ind, localPos);
				KeywordIndex ptsKw = InputAgent.formatPointsInputs(ent, "Points", pts, new Vec3d());
				ent.getJaamSimModel().storeAndExecute(new KeywordCommand(ent, ind, ptsKw));
			}
		} );
		if (ent.isGenerated() || nodeIndex >= 0) {
			addNodeItem.setEnabled(false);
		}
		menu.add( addNodeItem );

		// 8) Delete Node
		JMenuItem deleteNodeItem = new JMenuItem( "Delete Node" );
		deleteNodeItem.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed( ActionEvent event ) {
				ArrayList<Vec3d> pts = ent.getPoints();
				pts.remove(nodeIndex);
				KeywordIndex ptsKw = InputAgent.formatPointsInputs(ent, "Points", pts, new Vec3d());
				ent.getJaamSimModel().storeAndExecute(new KeywordCommand(ent, nodeIndex, ptsKw));
			}
		} );
		if (ent.isGenerated() || nodeIndex == -1
				|| ent.getPoints().size() <= 2) {
			deleteNodeItem.setEnabled(false);
		}
		menu.add( deleteNodeItem );

		// 9) Split
		JMenuItem spitMenuItem = new JMenuItem( "Split" );
		spitMenuItem.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed( ActionEvent event ) {
				JaamSimModel simModel = ent.getJaamSimModel();
				String name = InputAgent.getUniqueName(simModel, ent.getName(), "_Split");
				simModel.storeAndExecute(new DefineCommand(simModel, ent.getClass(), name));
				DisplayEntity splitEnt = (DisplayEntity) simModel.getNamedEntity(name);

				// Match all the inputs
				splitEnt.copyInputs(ent);

				// If the mouse was not clicked on a node, add one at this location
				ArrayList<Vec3d> pts = ent.getPoints();
				int ind = nodeIndex;
				if (nodeIndex == -1) {
					Vec3d pos = RenderManager.inst().getPOI();
					if (pos == null)
						return;
					Vec3d localPos = ent.getLocalPosition(pos);
					final Simulation simulation = simModel.getSimulation();
					if (simulation.isSnapToGrid()) {
						localPos = simulation.getSnapGridPosition(localPos);
					}
					ind = PolylineInfo.getInsertionIndex(pts, localPos);
					pts.add(ind, localPos);
				}

				// Original entity is left with the first portion of the nodes
				ArrayList<Vec3d> pts0 = new ArrayList<>(ind + 1);
				for (int i = 0; i <= ind; i++) {
					pts0.add(pts.get(i));
				}
				KeywordIndex ptsKw0 = InputAgent.formatPointsInputs(ent, "Points", pts0, new Vec3d());
				simModel.storeAndExecute(new KeywordCommand(ent, ind, ptsKw0));

				// New entity receives the remaining portion of the nodes
				ArrayList<Vec3d> pts1 = new ArrayList<>(pts.size() - ind);
				for (int i = ind; i < pts.size(); i++) {
					pts1.add(pts.get(i));
				}
				KeywordIndex ptsKw1 = InputAgent.formatPointsInputs(splitEnt, "Points", pts1, new Vec3d());
				InputAgent.processKeyword(splitEnt, ptsKw1);

				// Change any other object specific inputs for the split
				ent.setInputsForSplit(splitEnt);

				// Show the split entity in the editors and viewers
				FrameBox.setSelectedEntity(splitEnt, false);
			}
		} );
		if (ent.isGenerated() || nodeIndex == 0
				|| nodeIndex == ent.getPoints().size() - 1) {
			spitMenuItem.setEnabled(false);
		}
		menu.add( spitMenuItem );
	}

	public static void populateSubModelMenu(JPopupMenu menu, final SubModel submodel, final int nodeIndex,
			final Component c, final int x, final int y) {

		if (!RenderManager.isGood())
			return;

		// Select Components
		JMenuItem updateClonesItem = new JMenuItem( "Update Clones" );
		updateClonesItem.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed( ActionEvent event ) {
				submodel.updateClones();
			}
		} );
		menu.add( updateClonesItem );

		// Save SubModel
		JMenuItem saveSubModelItem = new JMenuItem( "Save SubModel" );
		saveSubModelItem.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed( ActionEvent event ) {
				GUIFrame.getInstance().saveEntity(submodel);

				ArrayList<Entity> refList = submodel.getExternalReferences();
				if (!refList.isEmpty()) {
					String msg = String.format("The saved SubModel file contains the following "
							+ "external references: %s", refList);
					Log.logLine(msg);
					GUIFrame.getInstance().invokeErrorDialogBox("Warning", msg);
				}
			}
		} );
		menu.add( saveSubModelItem );
	}

	public static void populateCompoundEntityMenu(JPopupMenu menu, final CompoundEntity ent, final int nodeIndex,
			final Component c, final int x, final int y) {

		if (!RenderManager.isGood())
			return;

		// Show Components
		boolean show = ent.isShowComponents(0.0d);
		final JMenuItem showComponentsMenuItem = new JCheckBoxMenuItem( "Show Components", show );
		showComponentsMenuItem.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed( ActionEvent event ) {
				boolean bool = showComponentsMenuItem.isSelected();
				KeywordIndex kw = InputAgent.formatBoolean("ShowComponents", bool);
				ent.getJaamSimModel().storeAndExecute(new KeywordCommand(ent, kw));
			}
		});
		menu.add( showComponentsMenuItem );
	}

}
