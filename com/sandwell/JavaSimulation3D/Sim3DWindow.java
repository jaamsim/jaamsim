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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.util.ArrayList;

import javax.media.j3d.BoundingSphere;
import javax.media.j3d.Locale;
import javax.swing.JFrame;
import javax.vecmath.Tuple3d;


/**
 * Provides a Window for the display of the simulation model It is tied to a
 * region that defines the to be displayed. Plays the role of the view.
 */
public class Sim3DWindow extends JFrame {
	static final ArrayList<Sim3DWindow> allWindows;
	static Sim3DWindow lastActiveWindow;

	/** Branchgroup holding the view information and node */
	private ModelView modelView;
	private Region region;

	public OrbitBehavior behavior;          // maintains viewer for the window's 3D graphics

	private boolean interactiveSizeChange = true;      // the size of the window is updating by user; false => it is changing in codes
	private boolean interactiveLocationChange = true;  // the position of the window is updating by user; false => it is changing in codes
	private boolean resizeTriggered = true;            // true => componentResized has been triggered interactively ( not in the codes )
	private PickingListener picker;

	private static final ControlKeyListener keyListener;
	private static final FocusListener focusListener;

	static {
		allWindows = new ArrayList<Sim3DWindow>();
		keyListener = new ControlKeyListener();
		focusListener = new FocusListener();
	}

	/** Constructor for the 3D window.  Sub-classes internalframe to add custom
	 *  constructor code to register itself with the view manager (to be added)
	 */
	public Sim3DWindow( Region newRegion, Locale locale) {
		super( newRegion.newWindowName() );
		region = newRegion;

		setIconImage(GUIFrame.getWindowIcon());

		if( newRegion.getStatusBar() != null )
			getContentPane().add( newRegion.getStatusBar(), BorderLayout.SOUTH );

		setDefaultCloseOperation( JFrame.DISPOSE_ON_CLOSE );
		newRegion.setupViewer();
		Point size = region.getCurrentWindowSize();
		setSize( size.x, size.y );

		// Create basic objects and connect in universe
		modelView = new ModelView();

		// Initialize behavior
		behavior = new OrbitBehavior(modelView, this);
		BoundingSphere behaviorBounds = new BoundingSphere();
		behaviorBounds.setRadius( Double.POSITIVE_INFINITY );
		behavior.setSchedulingBounds( behaviorBounds );
		modelView.getCanvas3D().setFocusable(false);
		modelView.getCanvas3D().addMouseListener( behavior );
		modelView.getCanvas3D().addMouseMotionListener( behavior );
		modelView.getCanvas3D().addMouseWheelListener( behavior );
		this.getRootPane().setTransferHandler(new EntityTransferHandler());

		modelView.addChild( behavior );

		region.addBranchGroup( modelView );

		// Add picking tool
		picker = new PickingListener(modelView.getCanvas3D(), locale);
		modelView.getCanvas3D().addMouseListener( picker );

		// Add convas to frame
		this.getContentPane().add( modelView.getCanvas3D(), BorderLayout.CENTER );
		setExtendedState( JFrame.NORMAL );
		setLocationRelativeTo( null );

		Point pos = region.getCurrentWindowPos();

		//        newRegion.setupDefaultViewer();
		behavior.positionViewer(newRegion);

		// resize and position the window after the rendering is complete
		setSize( size.x, size.y );
		setLocation( pos.x, pos.y );

		addKeyListener(keyListener);
		addWindowFocusListener(focusListener);

		addComponentListener( new ComponentListener() {

			// The window goes hidden
			public void componentHidden( ComponentEvent e ) {}

			// While the window is moving ( this should trigger after the window moved though; this is a known bug in Java )
			public void componentMoved( ComponentEvent e ) {
				//System.out.println( "componentMoved " );
				if( interactiveLocationChange ) {
					behavior.updateBehaviour();
					behavior.storeUndoSteps();
					region.updateWindowPosInput();
				}
				else {
					interactiveLocationChange = true;
				}
			}

			// After window is resized
			public void componentResized( ComponentEvent e ) {
				//System.out.println( "componentResized " );

				// scale the viewer position to the aspect ratio of the window
				// J3D uses Window width as the determiner for model scale
				// we will apply the aspect ratio to the height to have the view
				// work with changes to the vertical extent of the window

				if( interactiveSizeChange ) {
					behavior.updateBehaviour();
					behavior.storeUndoSteps();
					resizeTriggered = true;
					region.updateWindowSizeInput();
				}

				// program changed the size of the window
				else {
					interactiveSizeChange = true;
				}
//				System.out.println( "FOV:" + behavior.getFOV() + " Viewer: " + behavior.getViewerPosition() + " Center: " + behavior.getCenterPosition() );
			}

			// Window goes visible
			public void componentShown( ComponentEvent e ) {}
		} );
	}

	public void dispose() {
		synchronized (allWindows) {
			// guard against multiple calls to dispose
			if (!allWindows.contains(this))
				return;
			allWindows.remove(this);
		}

		if (lastActiveWindow == this) {
			lastActiveWindow = null;

			// Undo and redo button are disabled
			DisplayEntity.simulation.getGUIFrame().setToolButtonRedoEnable( false );
			DisplayEntity.simulation.getGUIFrame().setToolButtonUndoEnable( false );
		}

		removeWindowFocusListener(focusListener);
		removeKeyListener(keyListener);

		this.getContentPane().removeAll();
		region.decrementWindowCount();
		super.dispose();
		modelView.getCanvas3D().removeMouseListener(picker);
		picker.destroy();
		picker = null;
		modelView.getCanvas3D().removeMouseListener( behavior );
		modelView.getCanvas3D().removeMouseMotionListener( behavior );
		modelView.getCanvas3D().removeMouseWheelListener( behavior );
		behavior.destroy();
		modelView.detach();
		modelView.destroy();
		modelView = null;
		region.setCurrentCenter( behavior.getCenterPosition() );
		region.setCurrentViewer( behavior.getViewer() );
		region.setCurrentFOV( behavior.getFOV() );
		behavior = null;
		java.awt.Dimension dim = getSize();
		region.setCurrentWindowSize( dim.width, dim.height );
		java.awt.Point pos = getLocation();
		region.setCurrentWindowPos( pos.x, pos.y );
		region = null;
		this.getRootPane().setTransferHandler(null);
		//        System.out.println( "Finished dispose in: " + this.getTitle() );
	}

	public PickingListener getPicker() {
		return picker;
	}

	public void setViewerToIsometric() {

		// Position the viewer to the isometric position (preserving orbit radius)
		behavior.setOrbitAngles(Math.acos(Math.tan(Math.PI/6.0d)), Math.PI/4.0d);
		behavior.integrateTransforms();
	}

	public void setViewerToXYPlane() {

		// Position the viewer to the XY-plane position (preserving orbit radius)
		behavior.setOrbitAngles(0.0d, 0.0d);
		behavior.integrateTransforms();
	}

	/**
	 * finds the first window for the specified region
	 * @param region the region to search for
	 * @return Sim3DWindow the first window for the specified region. null if
	 * no window was found for the specified region.
	 */
	static Sim3DWindow getFirstWindow(Region region) {
		synchronized (allWindows) {
			for (Sim3DWindow win : allWindows) {
				if (win.getRegion() == region)
					return win;
			}
		}

		// none were found, signify with null
		return null;
	}

	public Region getRegion() {
		return region;
	}

	public OrbitBehavior getBehavior() {
		return behavior;
	}

	public ModelView getModelView() {
		return modelView;
	}

	public boolean resizeTriggered() {
		return resizeTriggered;
	}

	public void setResizeTriggered( boolean bool ) {
		resizeTriggered = bool;
	}

	public void  setInteractiveSizeChenge( boolean bool ) {
		interactiveSizeChange = bool;
	}

	public void  setInteractiveLocationChenge( boolean bool ) {
		interactiveLocationChange = bool;
	}

	// Return true if the window location is different than the location in the current undoStep
	public boolean moved() {

		// Any step, but the first step
		if( behavior.getUndoStep() > 0 && behavior.getUndoSteps().size() > 0 ) {

			// Moved
			if( ! this.getLocation().equals( behavior.getUndoSteps().get( (behavior.getUndoStep() - 1)*6 + 3 )) ) {
				return true;
			}
		}

		// The first step
		if( behavior.getUndoStep() == 0 && behavior.getUndoSteps().size() > 0 ) {

			// Moved
			if(  ! this.getLocation().equals( behavior.getUndoSteps().get( (behavior.getUndoStep())*6 + 3 )) ) {
				return true;
			}
		}
		return false;
	}

	// Return true if the window size is different than the size in the current undoStep
	public boolean resized() {

		// Any step, but the first step
		if( behavior.getUndoStep() > 0 && behavior.getUndoSteps().size() > 0 ) {

			// Resized
			if( ! this.getSize().equals( behavior.getUndoSteps().get( (behavior.getUndoStep() - 1)*6 + 4 ))  ) {
				return true;
			}
		}

		// The first step
		if( behavior.getUndoStep() == 0 && behavior.getUndoSteps().size() > 0 ) {

			// Resized
			if(  ( ! this.getSize().equals( behavior.getUndoSteps().get( (behavior.getUndoStep())*6 + 4 )) )   ) {
				return true;
			}
		}
		return false;
	}

	private static class FocusListener implements WindowFocusListener {
		public void windowGainedFocus(WindowEvent e) {
			Sim3DWindow.lastActiveWindow = (Sim3DWindow)e.getSource();
		}

		public void windowLostFocus(WindowEvent e) {}
	}

	private static class ControlKeyListener implements KeyListener {
		public void keyReleased(KeyEvent e) {
			if (e.getKeyCode() == KeyEvent.VK_F12) {

				Sim3DWindow source = (Sim3DWindow)e.getSource();
				OrbitBehavior orbit = source.getBehavior();
				String title = source.getTitle();
				Tuple3d temp;

				temp = orbit.getCenterPosition();
				System.out.format("%s ViewCenter { %f %f %f }%n", title, temp.x, temp.y, temp.z);

				temp = orbit.getViewer();
				System.out.format("%s Viewer { %f %f %f }%n", title, temp.x, temp.y, temp.z);

				System.out.format("%s FOV { %f }%n", title, orbit.getFOV());

				Point loc = source.getLocation();
				System.out.format("%s WindowPos { %d %d }%n", title, loc.x, loc.y);

				Dimension d = source.getSize();
				System.out.format("%s WindowSize { %d %d }%n", title, d.width, d.height);
				return;
			}

			if (e.getKeyCode() == KeyEvent.VK_DELETE &&
			    OrbitBehavior.selectedEntity != null) {
				OrbitBehavior.selectedEntity.kill();
				FrameBox.setSelectedEntity(null);
			}
		}

		public void keyPressed(KeyEvent e) {}
		public void keyTyped(KeyEvent e) {}
	}
}
