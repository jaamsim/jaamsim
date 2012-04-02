/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2003-2011 Ausenco Engineering Canada Inc.
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

import com.sandwell.JavaSimulation.Entity;
import com.sandwell.JavaSimulation.Vector;
import com.sun.j3d.utils.behaviors.vp.ViewPlatformAWTBehavior;

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

import com.sandwell.JavaSimulation3D.util.Rectangle;
import com.sandwell.JavaSimulation3D.util.Shape;

import javax.media.j3d.Transform3D;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

/**
 * Provides mouse events processing of moving the viewer within a J3D environment.<br>
 * Provides this functionality by providing the viewer location, the point to look at and the up vector.<br>
 * Provides changes of the viewer of: zoom, pan, translate, orbit and field of view angle<br>
 * Provides the position of the locator in model units
 */
public class OrbitBehavior extends ViewPlatformAWTBehavior {
	public static final double DEFAULT_FOV = Math.PI / 3.15789d; // default if 57 degrees (about human FOV)
	public static final double DEFAULT_RADIUS = 2.41d;
	static final double MIN_RADIUS = 0.0000001d;
	static final double MAX_RADIUS = Double.POSITIVE_INFINITY;

	private static final double ORBIT_ANGLE_FACTOR = -0.01d;

	// Temporary place where all the entityTooltip state will be shared
	static MouseEvent tootipEvent = null;
	static OrbitBehavior tooltipBehavior = null;
	static boolean showtooltip = true;
	static EntityToolTip entityToolTip = null;

	// The enumerated mouse behavior modes
	static final int CHANGE_PAN = 2;
	static final int CHANGE_ROTATION = 4;
	static final int CHANGE_TRANSLATION = 8;
	static final int CHANGE_ZOOM_BOX = 64;
	static final int CHANGE_NODE = 128;

	static enum Plane { XY_PLANE, XZ_PLANE, YZ_PLANE };

	private final Sim3DWindow window;
	private final ModelView modelView;

	private final Vector3d orbitCenter;
	private final Vector3d orbitAngles;
	private double orbitRadius;
	private double fov;

	// used for calculations (only create once)
	private Vector3d universeDatumPoint = new Vector3d();

	// Zoom box parameters
	private int mouseX = 0;
	private int mouseY = 0;
	private Rectangle rectangle;

	// Do/undo parameters
	private Vector undoSteps = new Vector( 10, 1 ); // contains zoom steps
	private int undoStep = 0;                       // The position of the current zoom step in undoSteps
	private boolean windowMovedAndResized = false;  // true => window both resized and moved

	private static int mouseMode = OrbitBehavior.CHANGE_TRANSLATION;

	private static boolean positionMode = true;

	private static String currentCreationClass;

	private Vector3d dragEntityStartPosition = new Vector3d();

	// resize
	protected static DisplayEntity selectedEntity = null;
	private final Vector3d selectedStart = new Vector3d();

	// moving in z direction
	private int mouseXForZDragging; // x location needs to be fixed while moving in z direction
	private boolean zDragging = false;

	private static boolean resizeBounds = false;

	public static final int CORNER_BOTTOMLEFT = 1;
	public static final int CORNER_BOTTOMCENTER = 2;
	public static final int CORNER_BOTTOMRIGHT = 3;
	public static final int CORNER_MIDDLERIGHT = 4;
	public static final int CORNER_TOPRIGHT = 5;
	public static final int CORNER_TOPCENTER = 6;
	public static final int CORNER_TOPLEFT = 7;
	public static final int CORNER_MIDDLELEFT = 8;
	public static final int CORNER_ROTATE = 9;
	private Cursor rotation;

	public static int resizeType = 0;

	/**
	 * Creates a new OrbitBehavior
	 *
	 * @param c The Canvas3D to add the behavior to
	 * @param flags The option flags
	 */
	public OrbitBehavior(ModelView v, Sim3DWindow window) {
		super(v.getCanvas3D(), MOUSE_LISTENER | MOUSE_MOTION_LISTENER);
		modelView = v;
		this.window = window;
		setEnable( true );

		orbitCenter = new Vector3d();
		orbitAngles = new Vector3d();
		orbitRadius = DEFAULT_RADIUS;
		fov = DEFAULT_FOV;
		integrateTransforms();

		Toolkit tk = Toolkit.getDefaultToolkit();

		// The middle of the image is the hot spot for rotating cursor
		Dimension dim = tk.getBestCursorSize(32, 32);
		dim.height /= 2;
		dim.width /= 2;

		// Create rotating cursor from image
		Image cursorImage = tk.getImage(
				GUIFrame.class.getResource("/resources/images/rotating.png"));
		try {
			rotation = tk.createCustomCursor(
				cursorImage, new Point(dim.width,dim.height), "Rotating");
		}

		// use the default cursor, do not throw an exception
		catch(Exception exc){
			rotation = null;
		}
	}

	protected synchronized void processAWTEvents( final java.awt.AWTEvent[] events ) {
		motion = false;
		for( int i = 0; i < events.length; i++ ) {
			if( events[i] instanceof MouseEvent ) {
				MouseEvent evt = (MouseEvent)events[i];
				// Check if the next event is the same as this event to suppress
				// a large class of duplicates (dragging/moving)
				if (i + 1 < events.length && events[i + 1] instanceof MouseEvent) {
					MouseEvent nextevt = (MouseEvent)events[i + 1];
					// events have the same ID
					if (evt.getID() == nextevt.getID()) {
						if (evt.getID() == MouseEvent.MOUSE_DRAGGED)
							continue;
						if (evt.getID() == MouseEvent.MOUSE_MOVED)
							continue;
					}
				}

				processMouseEvent(evt);
			}
		}
		GraphicsUpdateBehavior.forceUpdate = true;
	}

	public void destroy() {
		canvases = null;
	}

	static void enableTooltip(boolean enable) {
		if (!enable)
			OrbitBehavior.killToolTip();

		OrbitBehavior.showtooltip = enable;
	}

	static void killToolTip() {
		if( entityToolTip != null ) {
			entityToolTip.abort();
			entityToolTip = null;
		}
	}

	public static void selectEntity(Entity ent) {
		if(ent == selectedEntity)
			return;

		// Unselect previous entity
		resizeObjectDeselected();

		if (ent instanceof DisplayEntity) {
			selectedEntity = (DisplayEntity)ent;
			resizeBounds = true;
			selectedEntity.setResizeBounds( true );
		}
		GraphicsUpdateBehavior.forceUpdate = true;
	}

	protected void processMouseEvent( final MouseEvent evt ) {
		if (evt.getID() == MouseEvent.MOUSE_WHEEL) {
			orbitRadius *= Math.pow(0.9d, -((MouseWheelEvent)evt).getWheelRotation());
			// Cap the radius to a minimum value
			orbitRadius = Math.max(orbitRadius, MIN_RADIUS);

			motion = true;
			return;
		}

		// ToolTip belongs to this window now
		if (evt.getID() == MouseEvent.MOUSE_ENTERED) {
			if (OrbitBehavior.showtooltip && entityToolTip == null) {
				entityToolTip = new EntityToolTip();
				entityToolTip.start();
			}
			tooltipBehavior = this;
			return;
		}

		if (evt.getID() == MouseEvent.MOUSE_EXITED) {
			tooltipBehavior = null;
			tootipEvent = null;

			// display nothing for the position
			if (positionMode)
				DisplayEntity.simulation.getGUIFrame().showLocatorPosition(null, window.getRegion());
			return;
		}

		Vector3d currentMousePosition = getUniversePointFromMouseLoc(evt.getX(), evt.getY(), Plane.XY_PLANE, 0.0d);

		if(evt.getClickCount() >= 2) {
			// Just catch everything and move on
			try {
				DisplayEntity.simulation.getGUIFrame().copyLocationToClipBoard(currentMousePosition);
			}
			catch (Throwable e) {}
		}

		// calculate and display the position in the UI
		if (positionMode)
			DisplayEntity.simulation.getGUIFrame().showLocatorPosition(currentMousePosition, window.getRegion());

		// Select the new entity
		DisplayEntity closest = window.getPicker().getClosestEntity(evt);

		// display the location of the locator when the mouse moves
		if( evt.getID() == MouseEvent.MOUSE_MOVED ) {
			// This is the new mousEvent for the tooltip
			tootipEvent = evt;

			// mouse moves inside the entity with resizeBounds set
			if (selectedEntity == closest && resizeBounds) {
					if( !(selectedEntity instanceof MouseNode) ){
						selectedStart.set(this.getUniversePointFromMouseLoc(
								evt.getX(), evt.getY(), Plane.XY_PLANE,
								selectedEntity.getAbsoluteCenter().z));

						// Pick the right mouse icon
						resizeType = edgeSelected(selectedEntity, selectedStart);
					}
			}
			else {

				// Back to default mouse icon
				edgeSelected(null, null);
			}

			return;
		}

		// Suppress tooltip when not a MOUSE_MOVED event
		tootipEvent = null;

		checkZDragging(evt);


		// A) THE FIRST TIME MOUSE IS PRESSED
		// setup the starting point
		if( evt.getID() == MouseEvent.MOUSE_PRESSED ) {
			this.storeUndoSteps();

			FrameBox.setSelectedEntity( closest );

			mouseX = evt.getX();
			mouseY = evt.getY();
			motion = true;
			universeDatumPoint.set(currentMousePosition);

			// ZOOM BOX INITIALIZATION
			// The first point of the rectangle (Zoom box)
			if( zoomBox( evt ) ) {
				rectangle = new Rectangle(universeDatumPoint.x, universeDatumPoint.y, 0, 0, 0, Shape.SHAPE_OUTLINE);
				rectangle.setLineStyle( Rectangle.LINE_DASH_1PX ); // box style
				rectangle.setLayer( 4 );
				rectangle.setColor( 1 ); // Black colour for the box
				window.getRegion().addShape( rectangle );
			}

			if( drag( evt ) ) {
				closest = window.getPicker().getClosestEntity(evt);
				if (selectedEntity != null) {

					// mouse clicked inside the entity with resizeBounds set
					if (selectedEntity == closest && resizeBounds && !zDragging) {
							selectedStart.set(this.getUniversePointFromMouseLoc(evt.getX(), evt.getY(), Plane.XY_PLANE, selectedEntity.getAbsoluteCenter().z));
					}
					else if(zDragging) {
						selectedStart.set(this.getUniversePointFromMouseLoc(mouseXForZDragging, evt.getY(), Plane.XZ_PLANE, selectedEntity.getAbsoluteCenter().y));
					}
				}
			} else if ( "add Node".equals(currentCreationClass) ) {
				Vector3d posn = new Vector3d(currentMousePosition);
				if (closest != null) {
					closest.addMouseNodeAt(posn);
				}
			}
		}

		// B) MOUSE IS MOVING WHILE IT IS PRESSED
		// handle the motions as they occur
		else if( evt.getID() == MouseEvent.MOUSE_DRAGGED ) {
			int xchange = evt.getX() - mouseX;
			int ychange = evt.getY() - mouseY;

			// 1) ZOOM BOX
			if( zoomBox( evt ) && rectangle != null ) {
				double h = currentMousePosition.y - universeDatumPoint.y;
				double w = currentMousePosition.x - universeDatumPoint.x;
				// System.out.println( "h:" + h + "  w: " + w  );
				rectangle.setSize(w, h);
				rectangle.setCenter( universeDatumPoint.x + w/2, universeDatumPoint.y + h/2, 0 );
				rectangle.updateGeometry();
			}

			// 2) PAN
			// translate the viewer and center (viewer and direction of view are translated)
			else if( translate( evt ) && selectedEntity == null) {
				Vector3d translateDiff = new Vector3d();
				translateDiff.sub(universeDatumPoint, currentMousePosition);
				translateDiff.z = 0.0d;
				orbitCenter.add(translateDiff);
				integrateTransforms();
				return;
			}

			// 3) ROTATION
			// rotate the viewer  (viewer orbits about viewpoint, direction of view changes distance does not)
			if( rotate( evt ) ) {

				double phi = orbitAngles.x + ychange * ORBIT_ANGLE_FACTOR;
				if (phi < -Math.PI)
					phi = -Math.PI;

				if (phi > Math.PI)
					phi = Math.PI;
				orbitAngles.x = phi;

				double theta = orbitAngles.z + xchange * ORBIT_ANGLE_FACTOR;
				theta %= 2.0d * Math.PI;

				orbitAngles.z = theta;
			}

			// 4) POSITION OBJECTS
			// 5) Resize Object
			// 6) Dragging object:
			if( drag( evt ) ) {
				if (selectedEntity != null) {

					// Check if we are resizing
					Vector3d dragEnd = this.getUniversePointFromMouseLoc(evt.getX(), evt.getY(), Plane.XY_PLANE, selectedEntity.getAbsoluteCenter().z);
					if (resizeBounds == true && resizeType > 0 && !zDragging) {
						resizeObject(selectedEntity, selectedStart, dragEnd);
					}

					// Otherwise it is object dragging
					else {
						Vector3d dragdist = new Vector3d();

						// z-axis dragging
						if (zDragging) {
							dragEnd = this.getUniversePointFromMouseLoc(mouseXForZDragging, evt.getY(), Plane.XZ_PLANE, selectedEntity.getAbsoluteCenter().y);
						}

						dragdist.sub(dragEnd, selectedStart);
						selectedEntity.dragged(dragdist);
					}
					// update the start position for the net darg we process
					selectedStart.set(dragEnd);
				}
			}

			// IN ANY OF THE 1 TO 7 CASES
			// reset values for next change
			mouseX = evt.getX();
			mouseY = evt.getY();
			motion = true;
		}

		// C) MOUSE IS RELEASED
		// when the motion has finished
		else if( evt.getID() == MouseEvent.MOUSE_RELEASED ) {
			// entity has been dragged
			if( selectedEntity != null && resizeBounds == false ) {
				// find distance to check if it has been dragged
				Vector3d distanceVec = selectedEntity.getPosition();
				distanceVec.sub(dragEntityStartPosition);

				// completed drag, update entity center
				selectedEntity.postDrag();
				selectedEntity = null;
			}

			if( selectedEntity != null && resizeBounds == true && !zDragging ) {
				selectedEntity.updateGraphics();
			}

			// Zooming to the box and removing the rectangle
			if( zoomBox( evt ) && rectangle != null) {
				Dimension d = modelView.getCanvas3D().getSize();
				double aspect = (double)d.width / d.height;

				// Find the physical width that will be viewed
				// (Note: field of view uses only the horizontal width)
				double rectWidth = Math.abs( rectangle.getWidth() );
				double rectHeight = Math.abs( rectangle.getHeight() );
				double widthOfView = Math.max(rectWidth, rectHeight * aspect);

				// Only if the size of the zoom box is not zero
				if( widthOfView > 0 ) {
					// Distance of the eye point from the model (in z=0)
					double zDistance = (widthOfView / 2) / Math.tan(fov / 2);
					Point3d temp = rectangle.getCenterInDouble();
					temp.z = 0.0d;
					setOrbitCenter(temp);
					temp.z = zDistance;
					this.setViewer(temp);

					// Needs to update the graphics properly
					motion = true;
				}

				window.getRegion().removeShape(rectangle);
				rectangle = null;
			}

			this.updateBehaviour();
			this.storeUndoSteps();

			// Update view inputs
			if(zoomBox( evt ) || translate( evt )) {
				window.getRegion().updateViewCenterInput();
				window.getRegion().updateViewerInput();
				window.getRegion().updateFOVInput();
			}
		}

		this.updateBehaviour();
	}

	private void checkZDragging(MouseEvent evt) {
		if(selectedEntity != null) {
			if ( (evt.getModifiersEx() & MouseEvent.SHIFT_DOWN_MASK) == MouseEvent.SHIFT_DOWN_MASK ) {

				// The first time that shift was pressed (dragging starts in z direction)
				if(!zDragging) {
					mouseXForZDragging = evt.getX();
					selectedStart.set(this.getUniversePointFromMouseLoc(mouseXForZDragging, evt.getY(), Plane.XZ_PLANE, selectedEntity.getAbsoluteCenter().y));
					zDragging = true;
				}
			}

			// Shift just released (dragging or resizing/rotating in xy plane)
			else if(zDragging){
				selectedStart.set(this.getUniversePointFromMouseLoc(evt.getX(), evt.getY(), Plane.XY_PLANE, selectedEntity.getAbsoluteCenter().z));
				zDragging = false;
			}
		}
	}

	protected synchronized void integrateTransforms() {

		Transform3D cameraTransform = new Transform3D();
		Vector3d translation = new Vector3d(0.0d, 0.0d, orbitRadius);

		cameraTransform.setEuler(orbitAngles);
		cameraTransform.transform(translation);
		translation.add(orbitCenter);
		cameraTransform.setTranslation(translation);

		modelView.setViewState(cameraTransform, fov, orbitRadius);
	}

	public void setOrbitCenter(Point3d cent) {
		orbitCenter.set(cent);
	}

	public Point3d getCenterPosition() {
		// make a copy and return the copy
		Point3d ret = new Point3d();
		ret.set(orbitCenter);
		return ret;
	}

	public void setOrbitAngles(double phi, double theta) {
		orbitAngles.set(phi, 0.0d, theta);
	}

	public void setViewer(Point3d view) {
		Vector3d relView = new Vector3d();
		relView.sub(view, orbitCenter);
		double dist1 = Math.hypot(relView.x, relView.y);
		// suppress small-angle errors
		if (dist1 < 0.0001d)
			dist1 = 0.0d;

		double phi = Math.atan2(dist1, relView.z);

		// calculate the theta Eular angle, the Pi/2 addition is to account for
		// the initial rotation towards the negative y-axis when off the z-axis
		double theta = Math.atan2(relView.y, relView.x);
		if (dist1 == 0.0d)
			theta = 0.0d;

		if (phi > 0.0d)
			theta += Math.PI / 2.0d;
		orbitAngles.set(phi, 0.0d, theta);
		orbitRadius = relView.length();
	}

	public Point3d getViewer() {
		Transform3D eular = new Transform3D();
		Vector3d translation = new Vector3d(0.0d, 0.0d, orbitRadius);

		eular.setEuler(orbitAngles);
		eular.transform(translation);
		translation.add(orbitCenter);

		return new Point3d(translation);
	}

	public double getFOV() {
		return fov;
	}

	public void setFOV(double fov) {
		this.fov = fov;
	}

	public void positionViewer(Region aRegion) {
		this.positionViewer(aRegion.getCurrentCenter(),
							aRegion.getCurrentViewer(),
							aRegion.getCurrentFOV());
	}

	private void positionViewer(Point3d centerPosition, Point3d viewerPosition, double fov) {
		this.fov = fov;
		setOrbitCenter( centerPosition );
		setViewer(viewerPosition);
		integrateTransforms();
	}

	Sim3DWindow getWindow() {
		return window;
	}

	boolean rotate(MouseEvent evt) {
		return mouseMode == OrbitBehavior.CHANGE_ROTATION && !evt.isMetaDown() && selectedEntity == null;
	}

	boolean zoomBox(MouseEvent evt) {
		return mouseMode == OrbitBehavior.CHANGE_ZOOM_BOX && !evt.isMetaDown() && selectedEntity == null;
	}

	boolean translate(MouseEvent evt) {
		return mouseMode == OrbitBehavior.CHANGE_TRANSLATION && !evt.isMetaDown() && selectedEntity == null;
	}

	boolean drag(MouseEvent evt) {
		return !evt.isMetaDown() && mouseMode != OrbitBehavior.CHANGE_NODE;
	}

	public static void setCreationBehaviour(String entityTypeToCreate) {
		OrbitBehavior.setViewerBehaviour(OrbitBehavior.CHANGE_TRANSLATION);
		DisplayEntity.simulation.pause();

		if ("add Node".equals(entityTypeToCreate)) {
			OrbitBehavior.setViewerBehaviour(OrbitBehavior.CHANGE_NODE);
		}

		OrbitBehavior.currentCreationClass = entityTypeToCreate;
	}

	public static void setViewerBehaviour(int behaviour) {
		OrbitBehavior.currentCreationClass = null;

		if (mouseMode == OrbitBehavior.CHANGE_NODE) {
			OrbitBehavior.resizeObjectDeselected();
		}

		mouseMode = behaviour;
	}

	public static void showPosition( boolean show ) {
		positionMode = show;
	}

	public Vector3d getUniversePointFromMouseLoc( int x, int y, Plane plane, double planeHeight) {
		// get the location of eye and the locator in the image plate
		Point3d eyePosn = new Point3d();
		modelView.getCanvas3D().getCenterEyeInImagePlate(eyePosn);

		Point3d mousePosn = new Point3d();
		modelView.getCanvas3D().getPixelLocationInImagePlate(x, y, mousePosn);

		// get the transform to convert imageplate to virtual world
		Transform3D locatorTrans = new Transform3D();
		modelView.getCanvas3D().getImagePlateToVworld(locatorTrans);

		// convert the image plate points to the virtual world
		locatorTrans.transform(eyePosn);
		locatorTrans.transform(mousePosn);

		// calculate the view vector for the locator
		Vector3d universePos = new Vector3d();
		universePos.sub(mousePosn, eyePosn);
		universePos.normalize();

		// determine the distance from the plane and scale the vector
		double scale = 0.0d;
		switch (plane) {
			case XZ_PLANE:
				scale = -(eyePosn.y - planeHeight) / universePos.y;
			break;

			case YZ_PLANE:
				scale = -(eyePosn.x - planeHeight) / universePos.x;
			break;
			case XY_PLANE:
				scale = -(eyePosn.z - planeHeight) / universePos.z;
			break;
		}

		universePos.scale(scale);
		universePos.add(eyePosn);

		// If this is a view of another region, then it has a current region
		Region aRegion = window.getRegion();
		if (aRegion.getCurrentRegion() != null) {
			aRegion = aRegion.getCurrentRegion();
		}

		// scale by the Region's position for region coordinates
		universePos.sub( aRegion.getPositionForAlignment(new Vector3d()) );
		Vector3d regionScale = aRegion.getScale();
		universePos.x /= regionScale.x;
		universePos.y /= regionScale.y;
		universePos.z /= regionScale.z;

		// return the Universe position
		return universePos;
	}

	// Updates both Sim3Dwindow and OrbitBehavior
	public void updateViewerAndWindow( Point3d centerPosition, Point3d viewerPosition, java.awt.Point position, java.awt.Dimension size, double fov  ) {

		this.positionViewer(centerPosition, viewerPosition, fov);

		// Window size has been changed by do or undo in the program (not by the user)
		if( ! window.getSize().equals( size )  ) {
			window.setInteractiveSizeChenge( false );
			window.setSize(size);
		}

		// Window location has been changed by do or undo in the program (not by the user)
		if( ! window.getLocation().equals( position ) ) {
			window.setInteractiveLocationChenge( false );
			window.setLocation(position);
		}

		window.getRegion().updatePositionViewer(centerPosition, viewerPosition, position, size, fov);
	}

	public void updateBehaviour() {
		// If this is not the active window
		if (Sim3DWindow.lastActiveWindow != window)
			return;

		window.getRegion().updatePositionViewer( this.getCenterPosition(), this.getViewer(), window.getLocation(), window.getSize(), fov);

		if( undoStep > 1 || ( undoStep == 1 && ( this.viewerChanged() || ( ( window.moved() || window.resized() ) && ! windowMovedAndResized ) ) ) ) {
			DisplayEntity.simulation.getGUIFrame().setToolButtonUndoEnable( true );
		}
		else {
			DisplayEntity.simulation.getGUIFrame().setToolButtonUndoEnable( false );
		}

		if( undoStep == ( undoSteps.size() / 6 ) || ( undoStep == undoSteps.size() / 6 - 1 && ( this.viewerChanged() || ( ( window.moved() || window.resized() ) && ! windowMovedAndResized ) ) ) ) {
			DisplayEntity.simulation.getGUIFrame().setToolButtonRedoEnable( false );
		}
		else {
			DisplayEntity.simulation.getGUIFrame().setToolButtonRedoEnable( true );
		}
	}

	public boolean viewerChanged() {

		// Any step but the first
		if( undoStep > 0 && undoSteps.size() > 0 ) {
			Point3d viewer = this.getViewer();
			// Viewer changed
			if( ! orbitCenter.equals( undoSteps.get( (undoStep - 1)*6 )) || ! viewer.equals( (Point3d) undoSteps.get( (undoStep -1)*6 + 1 ))  ||
				! ( fov ==  ((Double) undoSteps.get( (undoStep - 1)*6 + 5 )).doubleValue() ) ) {
				return true;
			}
		}

		// The first step
		if( undoStep == 0 && undoSteps.size() > 0 ) {
			Point3d viewer = this.getViewer();
			// Viewer changed
			if( ! orbitCenter.equals( undoSteps.get( (undoStep )*6 )) || ! viewer.equals( (Point3d) undoSteps.get( (undoStep )*6 + 1 ))  ||
				! ( fov ==  ((Double) undoSteps.get( (undoStep )*6 + 5 )).doubleValue() ) ) {
					return true;
			}
		}
		return false;
	}

	public int getUndoStep() {
		return undoStep;
	}

	public Vector getUndoSteps() {
		return undoSteps;
	}

	public void storeUndoSteps(  ) {
		//System.out.println( "storeZoomSteps " );

		if( ! ( this.viewerChanged() || window.moved() || window.resized() ) && undoSteps.size() != 0 ) {
			return;
		}

		// TODO: We could have ignored this step if the componentMoved event fired after the window has been moved; this event keeps firing while the window is moving;
		//////////// The above is a known bug in component listener
		// viewer has not changed
		if( ! this.viewerChanged() ) {

			// 1) Window both moved and resized
			if( ( window.moved() && window.resized() ) || windowMovedAndResized ) {
				windowMovedAndResized = true;

				// a) componentResized of the window has been triggered
				if ( window.resizeTriggered() ) {
					windowMovedAndResized = false;
					window.setResizeTriggered( false );
				}

				// b) Wait until componentMoved is triggered
				else {
					return;
				}
			}

			// 2) Window moved only
			else if ( window.moved()  && undoStep > 1 ) {
				undoStep--;

				// The only difference in last two undo steps is the window location
				if ( window.moved() ) {
					undoStep++;

					// Only update the window location in the last undo step
					undoSteps.setElementAt( window.getLocation(), undoSteps.size() -  3 );
					return;
				}
				undoStep++;

			}
			// 3) Window resized only is a normal situation
		}
		////////////

		// Remove the steps which were undid ( when undo several steps and then modify the view or window, the redo steps after the current step should be removed )
		for( int i = undoSteps.size() - 1; i >= undoStep*6 && i >= 6; i-- ){
			undoSteps.removeElementAt( i );
		}

		//System.out.println( "adding a new step: Position:" + window.getWindowPosition() + "  --  Size:" + window.getWindowSize());
		//System.out.println( "center:" + center + "  viewer:" + viewer + "  up:" + up + "   fov:" + fov );

		undoSteps.add(orbitCenter.clone());
		undoSteps.add(this.getViewer());
		undoSteps.add( new Vector3d() );
		undoSteps.add( window.getLocation() );
		undoSteps.add( window.getSize() );
		undoSteps.add( new Double(fov) );
		undoStep++;
		this.updateBehaviour();

	}

	/** Undo the previous zoom step */
	public boolean undoPreviousStep() {


		// the current step is the last step
		if( undoStep == undoSteps.size() / 6  ) {
			storeUndoSteps();
			undoStep--;
		}

		// There is at least one step to undo
		if( undoStep > 0 ) {
			undoStep--;
			this.updateViewerAndWindow(new Point3d((Vector3d)undoSteps.get( undoStep*6 )), (Point3d) undoSteps.get( undoStep*6 + 1), (java.awt.Point) undoSteps.get( undoStep*6 + 3 ), (java.awt.Dimension) undoSteps.get( undoStep*6 + 4 ), ((Double) undoSteps.get( undoStep*6 + 5 )).doubleValue());
		}
		updateBehaviour();

		// There is no more step to undo
		if( undoStep == 0 ) {
			return false;
		}

		// Undo is still possible
		else {
			return true;
		}
	}

	/** Redo the previous step */
	public boolean redoPreviousStep() {

		// The current step is not the last step
		if( undoStep < ( undoSteps.size() / 6 - 1) ) {
			undoStep++;
			this.updateViewerAndWindow(new Point3d((Vector3d)undoSteps.get( undoStep*6 )), (Point3d) undoSteps.get( undoStep*6 + 1), (java.awt.Point) undoSteps.get( undoStep*6 + 3 ), (java.awt.Dimension) undoSteps.get( undoStep*6 + 4 ), ((Double) undoSteps.get( undoStep*6 + 5 )).doubleValue());
		}
		updateBehaviour();

		// This is the last step
		if( undoStep == ( undoSteps.size() / 6 - 1)  ) {
			return false;
		}

		// redo is still possible
		else {
			return true;
		}
	}

	private static void resizeObjectDeselected(){
		if (selectedEntity == null)
			return;

		resizeBounds = false;
		resizeType = 0;
		selectedEntity.setResizeBounds( false );
		selectedEntity = null;
	}

	/**
	 * resize the selected Object
	 */
	private void resizeObject(DisplayEntity ent, Vector3d start, Vector3d end){

		if (ent == null)
			return;

		double resizeAverage = 0;			// used for corners
		Vector3d center = ent.getAbsoluteCenter();
		Vector3d orient = ent.getOrientation();

		// get current point
		Vector3d resizeStartRotated = ent.getCoordinatesForRotationAroundPointByAngleForPosition(center, -orient.z, start);
		Vector3d resizeEndRotated = ent.getCoordinatesForRotationAroundPointByAngleForPosition(center, -orient.z, end);

		Vector3d resizediff = new Vector3d();
		resizediff.sub( resizeEndRotated, resizeStartRotated );

		Vector3d selectedEntitySize = ent.getSize();

		// resize the object according to which corner was selected
		switch( resizeType ){
		case CORNER_BOTTOMLEFT:
			// take the average change (equal horizontal and vertical resize);
			resizeAverage = (resizediff.x + resizediff.y);
			selectedEntitySize.x -= resizeAverage;
			selectedEntitySize.y -= resizeAverage;

			// new size of entity is the old size + dragged amount
			ent.setSize(selectedEntitySize);
			ent.updateInputPosition();
			ent.updateInputSize();
			break;
		case CORNER_BOTTOMCENTER:
			// Adjust only y
			// multiply by 2 to adjust both top and bottom
			resizeAverage = 2 * resizediff.y;
			selectedEntitySize.y -= resizeAverage;
			ent.setSize(selectedEntitySize);
			ent.updateInputPosition();
			ent.updateInputSize();
			break;
		case CORNER_BOTTOMRIGHT:
			resizeAverage = (resizediff.x - resizediff.y);
			selectedEntitySize.x += resizeAverage;
			selectedEntitySize.y += resizeAverage;
			ent.setSize(selectedEntitySize);
			ent.updateInputPosition();
			ent.updateInputSize();
			break;
		case CORNER_MIDDLERIGHT:
			resizeAverage = 2 *  resizediff.x;
			selectedEntitySize.x += resizeAverage;
			ent.setSize(selectedEntitySize);
			ent.updateInputPosition();
			ent.updateInputSize();
			break;
		case CORNER_TOPRIGHT:
			resizeAverage = (resizediff.x + resizediff.y);
			selectedEntitySize.x += resizeAverage;
			selectedEntitySize.y += resizeAverage;
			selectedEntitySize.add(resizediff);
			ent.setSize(selectedEntitySize);
			ent.updateInputPosition();
			ent.updateInputSize();
			break;
		case CORNER_TOPCENTER:
			resizeAverage = 2 * resizediff.y;
			selectedEntitySize.y += resizeAverage;
			ent.setSize(selectedEntitySize);
			ent.updateInputPosition();
			ent.updateInputSize();
			break;
		case CORNER_TOPLEFT:
			// top left
			resizeAverage = ( resizediff.y - resizediff.x );
			selectedEntitySize.x += resizeAverage;
			selectedEntitySize.y += resizeAverage;
			ent.setSize(selectedEntitySize);
			ent.updateInputPosition();
			ent.updateInputSize();
			break;
		case CORNER_MIDDLELEFT:
			// middle left
			resizeAverage = 2 * resizediff.x;
			selectedEntitySize.x -= resizeAverage;
			ent.setSize(selectedEntitySize);
			ent.updateInputPosition();
			ent.updateInputSize();
			break;
		case CORNER_ROTATE:
			Vector3d diff = new Vector3d();
			diff.sub(end, center);

			 // add PI as the resize line points to the left
			double zAngle = Math.atan2(diff.y, diff.x) + Math.PI;
			diff.set(orient.x, orient.y, zAngle);
			ent.setOrientation(diff);
			ent.updateInputOrientation();
			break;
		}
	}

	private boolean calcEdgeDistance(DisplayEntity ent, Vector3d pt, Vector3d align, double dist) {
		Vector3d alignPoint = ent.getAbsolutePositionForAlignment(align);
		alignPoint.sub(pt);
		return alignPoint.lengthSquared() < dist;
	}

	/**
	 * Find the corner that has been selected or mouse over and change the mouse icon
	 * to resize icon if a corner has been selected
	 * @param currentPoint
	 * @return
	 */
	private int edgeSelected(DisplayEntity ent, Vector3d currentPoint) {
		if (ent == null) {
			window.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
			return 0;
		}

		// the square of the radius of the circles at each corner that are
		// used for resizing
		Vector3d tmp = ent.getSize();
		double distSquare = 0.10 * Math.min(tmp.x, tmp.y);
		distSquare = distSquare * distSquare;

		tmp.set(-0.5d, -0.5d, 0.0d);
		if (calcEdgeDistance(ent, currentPoint, tmp, distSquare)) {
			setCursor(1, ent);
			return CORNER_BOTTOMLEFT;
		}

		tmp.set(0.0d, -0.5d, 0.0d);
		if (calcEdgeDistance(ent, currentPoint, tmp, distSquare)) {
			setCursor(2, ent);
			return CORNER_BOTTOMCENTER;
		}

		tmp.set(0.5d, -0.5d, 0.0d);
		if (calcEdgeDistance(ent, currentPoint, tmp, distSquare)) {
			setCursor(3, ent);
			return CORNER_BOTTOMRIGHT;
		}

		tmp.set(0.5d, 0.0d, 0.0d);
		if (calcEdgeDistance(ent, currentPoint, tmp, distSquare)) {
			setCursor(0, ent);
			return CORNER_MIDDLERIGHT;
		}

		tmp.set(0.5d, 0.5d, 0.0d);
		if (calcEdgeDistance(ent, currentPoint, tmp, distSquare)) {
			setCursor(1, ent);
			return CORNER_TOPRIGHT;
		}

		tmp.set(0.0d, 0.5d, 0.0d);
		if (calcEdgeDistance(ent, currentPoint, tmp, distSquare)) {
			setCursor(2, ent);
			return CORNER_TOPCENTER;
		}

		tmp.set(-0.5d, 0.5d, 0.0d);
		if (calcEdgeDistance(ent, currentPoint, tmp, distSquare)) {
			setCursor(3, ent);
			return CORNER_TOPLEFT;
		}

		tmp.set(-0.5d, 0.0d, 0.0d);
		if (calcEdgeDistance(ent, currentPoint, tmp, distSquare)) {
			setCursor(0, ent);
			return CORNER_MIDDLELEFT;
		}

		tmp.set(-1.0d, 0.0d, 0.0d);
		if (calcEdgeDistance(ent, currentPoint, tmp, distSquare)) {
			window.setCursor(rotation);
			return CORNER_ROTATE;
		}

		window.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
		return 0;
	}

	/**
	 * set cursor to match the selected edge
	 * @param cursor
	 * @param degree
	 */
	private void setCursor(int cursor, DisplayEntity ent) {
		int rotate;
		double degree = ent.getOrientation().z % Math.PI;

		// If entity has been rotated, change resize cursor direction
		if( degree > 2.7488 ){
			// rotate 157.5 degree to 180 degree
			rotate = 0;
		} else if ( degree > 1.963495 ){
			// rotate 112.5 degree to 157.5 degree
			rotate = 3;
		} else if( degree > 1.170097225 ){
			// rotate 67.5 degree to 112.5 degree
			rotate = 2;
		} else if ( degree > 0.392699 ){
			// rotate 22.5 degree to 67.5 degree
			rotate = 1;
		} else {
			// no rotation
			rotate = 0;
		}

		cursor = (cursor + rotate)%4;

		switch(cursor) {
		case 0: window.setCursor(new Cursor(Cursor.E_RESIZE_CURSOR));  break;
		case 1: window.setCursor(new Cursor(Cursor.NE_RESIZE_CURSOR)); break;
		case 2: window.setCursor(new Cursor(Cursor.N_RESIZE_CURSOR));  break;
		case 3: window.setCursor(new Cursor(Cursor.NW_RESIZE_CURSOR)); break;
		}

	}
}
