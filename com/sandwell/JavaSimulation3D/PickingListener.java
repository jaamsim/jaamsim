/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2002-2011 Ausenco Engineering Canada Inc.
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

import com.sun.j3d.utils.picking.PickCanvas;
import com.sun.j3d.utils.picking.PickResult;
import com.sun.j3d.utils.picking.PickTool;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.media.j3d.Link;
import javax.media.j3d.Canvas3D;
import javax.media.j3d.CapabilityNotSetException;
import javax.media.j3d.Node;
import javax.media.j3d.SceneGraphPath;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import java.awt.Component;
import java.awt.Point;
import java.util.ArrayList;

/**
 * Class to implement the picking of objects from the simulation world.
 */
public class PickingListener implements MouseListener {

	private PickCanvas pickCanvas;
	private final JPopupMenu menu;

	public PickingListener(Canvas3D target) {
		pickCanvas = new PickCanvas( target, DisplayEntity.simulation.rootLocale );
		// pickCanvas.setMode( PickTool.BOUNDS ); // Pick the object based on its bounding box (Huge for objects which are at an angle)
		// pickCanvas.setMode(PickTool.GEOMETRY); // This one works better, but still picks over too large an area
		pickCanvas.setMode(PickTool.GEOMETRY_INTERSECT_INFO); // Pick the object based on its geometry
		pickCanvas.setTolerance( 4.0f ); // Mouse click can miss object by as much as 4 pixels
		menu = new JPopupMenu();
	}

	public void destroy() {
		pickCanvas = null;
	}

	/** event handler when the mouse is clicked **/
	public void mouseClicked( MouseEvent e ) {
		// verify this is an Info Request -- Right Click
		if( e.getButton() == MouseEvent.BUTTON3 ) {
			processMenuClick( e );
		}
	}

	private void processMenuClick( MouseEvent e ) {
		ArrayList<DisplayEntity> entList = getEntityList(e);

		menu.removeAll();

		// if there is only one entity, display it
		if( entList.size() == 1 ) {
			entList.get(0).addMenuItems(menu, e.getX(), e.getY());
		}
		else {

			// there are multiple entities, select from a menu
			// for each menu, add a menu item that pops up an info box
			for( int i = 0; i < entList.size(); i++ ) {
				final DisplayEntity thisEnt = entList.get( i );
				final Point pt = new Point(e.getX(), e.getY());
				final Component canvas = e.getComponent();

				JMenuItem thisItem = new JMenuItem(thisEnt.getName());
				thisItem.addActionListener( new ActionListener() {

					public void actionPerformed( ActionEvent event ) {
						menu.removeAll();
						thisEnt.addMenuItems(menu, pt.x, pt.y);
						menu.show(canvas, pt.x, pt.y);
					}
				} );

				menu.add( thisItem );
			}
		}

		// display the popup menu
		menu.show(e.getComponent(), e.getX(), e.getY());
	}

	public void mouseEntered( MouseEvent e ) {}

	public void mouseExited( MouseEvent e ) {}

	public void mousePressed( MouseEvent e ) {}

	public void mouseReleased( MouseEvent e ) {}

	/**
	 *	returns the selected DisplayEntities(including TextLabels) for this mouse position.
	 *	If there are no objects, returns null.
	 *	If there are objects, it returns a Vector of those objects
	 * @param e
	 * @return
	 */
	private ArrayList<DisplayEntity> getEntityList(MouseEvent e) {

		// 1) All objects but TextLabels are pickable by their geometry
		ArrayList<DisplayEntity> results = this.getEntityList_ForPickTool(e, PickTool.GEOMETRY_INTERSECT_INFO);

		// 2) TextLabel is only pickable by its bounding box (this picks a lot more objects, but we only need the TextLabels)
		ArrayList<DisplayEntity> textLabels = this.getEntityList_ForPickTool(e, PickTool.BOUNDS);

		// Only add TextLabels from textLabels list to the results
		for (DisplayEntity each : textLabels) {
			if (each instanceof TextLabel && !results.contains(each))
				results.add(each);
		}

		return results;
	}

	private PickResult[] pickResults(MouseEvent e, int pickMode) {
		pickCanvas.setShapeLocation(e);
		pickCanvas.setMode(pickMode);

		// obtain all of the objects that are located under this ray
		try {
			return pickCanvas.pickAllSorted();
		}
		catch (CapabilityNotSetException excep) {
			System.out.println("Missing capabilty: " + excep);
			return null;
		}
		catch (NullPointerException excep) {
			//FIXME: we sometimes cause a pick to occur on a branchgroup that has been
			// detached and we get a null return, return null to the picker and
			// hope the next call goes more smoothly
			return null;
		}
	}

	private DisplayEntity lookupEntity(SceneGraphPath path) {
		if (path.nodeCount() < 2)
			return null;

		/*
		 * BranchGroups are returned in order from the top of the
		 * SceneGraph tree to the bottom.  Links are found after all
		 * Region and DisplayEntity BranchGroups as they will be
		 * lower in the tree.
		 */

		Node node = path.getNode(1);

		// The item before the first Link(if there is any) is DisplayEntity BranchGroup
		for(int i=2; i < path.nodeCount(); i++) {
			if(path.getNode(i) instanceof Link)
				break;
			node = path.getNode(i);
		}

		if (node.getUserData() instanceof DisplayEntity)
			return (DisplayEntity)node.getUserData();
		else
			return null;
	}

	/**
	 *	returns the selected DisplayEntities for this mouse position based o picktool property
	 *	If there are no objects, returns null.
	 *	If there are objects, it returns a Vector of those objects
	 * for all objects but TextLabel use PickTool.GEOMETRY_INTERSECT_INFO
	 * for TextLabel use PickTool.BOUNDS
	 * @param e
	 * @param pickTool
	 * @return
	 */
	private ArrayList<DisplayEntity> getEntityList_ForPickTool( MouseEvent e, int pickTool ) {
		PickResult[] results = this.pickResults(e, pickTool);

		// if there are no objects we are done
		if (results == null)
			return new ArrayList<DisplayEntity>();

		// a list of DisplayEntities
		ArrayList<DisplayEntity> entList = new ArrayList<DisplayEntity>();

		// go through all of the picked objects and obtain the DisplayEntities that they are related to
		for( int i = 0; i < results.length; i++ ) {
			DisplayEntity ent = this.lookupEntity(results[i].getSceneGraphPath());

			if (ent != null && !entList.contains(ent) && ent.isMovable())
				entList.add(ent);
		}

		// return the list of entities
		return entList;
	}

	/**
	 *	returns the selected DisplayEntities for this mouse position only if their showToolTips are true
	 *	If there are no objects, returns null.
	 *	If there are objects, it returns a Vector of those objects
	 * @param e
	 * @return
	 */
	protected ArrayList<DisplayEntity> getEntityListWithToolTip( MouseEvent e ) {
		ArrayList<DisplayEntity> entList = new ArrayList<DisplayEntity>();
		for (DisplayEntity each : this.getEntityList(e)) {
			if (each.showToolTip()) {
				entList.add(each);
			}
		}

		// return the list of entities
		if (entList.size() == 0)
			return null;
		else
			return entList;
	}

	/**
	 * return the closest DisplayEntity to the mouse pointer
	 *
	 * @param e
	 * @return
	 */
	protected DisplayEntity getClosestEntity(MouseEvent e) {

		// 1) All objects but TextLabels are pickable by their geometry
		DisplayEntity result = this.getClosestEntity_ForPickTool( e, PickTool.GEOMETRY_INTERSECT_INFO );
		if ( result != null ) {
			return result;
		}

		// 2) TextLabel is only pickable by its bounding box (this picks a lot more objects, but we only need the TextLabels)
		result = this.getClosestEntity_ForPickTool( e, PickTool.BOUNDS );

		return result;

	}

	/**
	 * return the closest DisplayEntity (except Regions) to the mouse pointer based on the picktool property
	 * for all objects but TextLabel use PickTool.GEOMETRY_INTERSECT_INFO
	 * for TextLabel use PickTool.BOUNDS
	 * @param e
	 * @param pickTool
	 * @return
	 */
	private DisplayEntity getClosestEntity_ForPickTool(MouseEvent e, int pickTool) {
		PickResult[] results = this.pickResults(e, pickTool);

		// if there are no objects we are done
		if( results == null ) {
			return null;
		}

		for( int i = 0; i < results.length; i++ ) {
			DisplayEntity candidate = this.lookupEntity(results[i].getSceneGraphPath());

			if (candidate == null)
				continue;

			// If we are picking based on bounds candidate should be TextLabel
			if( pickTool == PickTool.BOUNDS && !( candidate instanceof TextLabel ) ) {
				continue;
			}

			// Ignore Region
			if( ! ( candidate instanceof Region ) ) {
				if (candidate.isMovable())
					return candidate;
			}
		}
		return null;
	}

	public JPopupMenu getMenu() {
		return menu;
	}
}
