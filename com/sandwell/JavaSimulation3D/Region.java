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

import com.sandwell.JavaSimulation.DoubleVector;
import com.sandwell.JavaSimulation.Input;
import com.sandwell.JavaSimulation.InputErrorException;
import com.sandwell.JavaSimulation.StringVector;
import com.sandwell.JavaSimulation.Tester;
import com.sandwell.JavaSimulation.Util;

import java.awt.Color;
import java.awt.FlowLayout;
import java.util.ArrayList;

import javax.media.j3d.BranchGroup;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.vecmath.Point2d;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;
import java.awt.Point;

/**
 * Entity which defines its own locale to add branch groups to. Analogous to
 * stage construct. Abstract class as it does not define the doProcess() method.
 */
public class Region extends DisplayEntity {
	private static final ArrayList<Region> allInstances;

	private Point3d defaultCenter = new Point3d( 0.0, 0.0, 0.0 ); // where the viewer is looking by default
	private Point3d defaultViewer = new Point3d( 0.0, 0.0, 10.0 ); // where the viwer is located by default
	private Point defaultWindowPos = new Point( 0, 110 ); // default, just below GUIFrame (if GUIFrame is at the top of the screen)
	private Point defaultWindowSize = new Point( 1280, 890 );
	private double defaultFieldOfView = OrbitBehavior.DEFAULT_FOV;
	private boolean defaultsSet = false;
	private boolean setCurrentToDefaults = false;
	private boolean boundDefaultToExtent = false;

	private Point3d currentCenter = new Point3d(); // where the viewer is looking currently
	private Point3d currentViewer = new Point3d(); // where the viwer is located currently
	private double currentFieldOfView = defaultFieldOfView; // default angle of viewer's view frustrum
	private Point currentWindowPos = new Point( 25, 150 ); // default position of the window
	private Point currentWindowSize = new Point( 500, 300 ); // default size of the window
	private boolean showOnStartup = false;

	private JPanel statusBar = null;

	private PictureEntity backgroundImage = new PictureEntity();
	private Vector3d bkImagePos = new Vector3d();
	private Point2d bkImageSize = new Point2d( 1.0, 1.0 );
	private double bkImageAngle = 0.0;  // counter-clockwise rotation in degrees of background image about its center
	private Point2d bkImagePosLatLong = new Point2d( 0.0, 0.0 );
	private Point2d bkImageSizeLatLong = new Point2d( 1.0, 1.0 );

	protected boolean showTime = false; // Do not show the status bar and LocalTime

	protected BranchGroup entityGroup;

	//protected BranchGroup collapsedModel;		// alternate appearance when the region is collapsed

	protected int numWindowsAlive; // number of open windows for the region
	private String titleBarText; // Title name for the window

	protected LocalClock localClock;

	static {
		allInstances = new ArrayList<Region>();
	}

	{
		addEditableKeyword( "BKImage",          "", "",           false, "Graphics" );
		addEditableKeyword( "ImageSize",        "", "",           false, "Graphics" );
		addEditableKeyword( "ImageSizeLatLong", "", "",           false, "Graphics" );
		addEditableKeyword( "ImagePos",         "", "",           false, "Graphics" );
		addEditableKeyword( "ImagePosLatLong",  "", "",           false, "Graphics" );
		addEditableKeyword( "ImageAngle",  		"", "",           false, "Graphics" );

		// Creating a Window
		addEditableKeyword( "WindowPos",  "", "", false, "Graphics" );
		addEditableKeyword( "WindowSize", "", "", false, "Graphics" );
		addEditableKeyword( "ShowWindow", "", "FALSE", false, "Graphics" );
		addEditableKeyword( "ShowTime", "", "FALSE", false, "Graphics" );
		// Default View Camera
		addEditableKeyword( "ViewCenter", "", "", false, "Graphics" );
		addEditableKeyword( "Viewer",     "", "", false, "Graphics" );
		addEditableKeyword( "FOV", "", "", false, "Graphics" );

		addEditableKeyword( "TitleBarText",    "", "",           false, "Optional" );

		addEditableKeyword( "ViewerMoveToDuringTime",    "", "", false, "Script" );
		addEditableKeyword( "ViewCenterMoveToDuringTime","", "", false, "Script" );
		addEditableKeyword( "FOVMoveToDuringTime",       "", "", false, "Script" );
		addEditableKeyword( "MoveToFront",               "", "", false, "Script" );
	}

	/**
	 * Constructor creating a new locale in the simulation universe.
	 */
	public Region() {
		allInstances.add(this);

		// Register this region in the simulation
		numWindowsAlive = 0;

		entityGroup = new BranchGroup();
		entityGroup.setCapability( BranchGroup.ALLOW_DETACH );
		entityGroup.setCapability( BranchGroup.ALLOW_CHILDREN_WRITE );
		entityGroup.setCapability( BranchGroup.ALLOW_CHILDREN_EXTEND );
		entityGroup.setCapability( BranchGroup.ALLOW_CHILDREN_READ );

		addScaledBG(entityGroup);

		// register the graphics for the region if there is not currentRegion
		simulation.registerGraphics( this );

		// Define the status bar
		statusBar = new JPanel();
		statusBar.setBorder(BorderFactory.createLineBorder(Color.darkGray));
		statusBar.setLayout(new FlowLayout(FlowLayout.LEFT, 10, 5));
		statusBar.setVisible(false);

		// Define the local clock
		localClock = new LocalClock();
		localClock.setRegion( this );
		localClock.enterRegion();
	}

	public static ArrayList<? extends Region> getAll() {
		return allInstances;
	}

	public void kill() {
		super.kill();
		allInstances.remove(this);
		simulation.unregisterGraphics(this);
		localClock.kill();
	}

	public void render(double time) {
		super.render(time);

		// show the status bar
		if (showTime)
			statusBar.setVisible(true);
		else
			statusBar.setVisible(false);
	}

	BranchGroup getEntGroup() {
		return entityGroup;
	}

	public void incrementWindowCount() {
		numWindowsAlive++;
	}

	public void decrementWindowCount() {
		numWindowsAlive--;
	}

	public String newWindowName() {
		incrementWindowCount();

		String name;
		if (titleBarText != null)
			name = titleBarText;
		else
			name = this.getName();

		if (numWindowsAlive == 1)
			return name;
		else
			return String.format("%s (%d)", name, numWindowsAlive);
	}

	public int getNumWindowsAlive() {
		return numWindowsAlive;
	}

	public void setRegion( Region newRegion ) {
		exitRegion();
		currentRegion = newRegion;
	}

	public LocalClock getLocalClock() {
		return localClock;
	}

	public boolean showTime() {
		return showTime;
	}

	public JPanel getStatusBar() {
		return statusBar;
	}

	public void setDefaultCenter(double x, double y, double z) {
		defaultCenter.set(x, y, z);
		setCurrentToDefaults = true;
		defaultsSet = true;
	}

	public void setDefaultViewer(double x, double y, double z) {
		defaultViewer.set(x, y, z);
		setCurrentToDefaults = true;
		defaultsSet = true;
	}

	public void setDefaultFOV( double in ) {
		defaultFieldOfView = in;
		setCurrentToDefaults = true;
		defaultsSet = true;
	}

	public void setDefaultWindowPos( Point pos ) {
		defaultWindowPos = pos;
		setCurrentToDefaults = true;
		//		defaultsSet = true;
	}

	public void setDefaultWindowSize( Point size ) {
		defaultWindowSize = size;
		setCurrentToDefaults = true;
		//		defaultsSet = true;
	}

	public Point3d getCurrentCenter() {
		return (Point3d)currentCenter.clone();
	}

	public Point3d getCurrentViewer() {
		return (Point3d)currentViewer.clone();
	}

	public double getCurrentFOV() {
		return currentFieldOfView;
	}

	public Point getCurrentWindowPos() {
		return currentWindowPos;
	}

	public Vector3d getVector3dForLatLong( double latitude, double longitude ) {
		double x, y;
		x = bkImagePos.x + (bkImageSize.x / bkImageSizeLatLong.x) * ( longitude - bkImagePosLatLong.x );
		y = bkImagePos.y + (bkImageSize.y / bkImageSizeLatLong.y) * ( latitude - bkImagePosLatLong.y );

		return new Vector3d ( x, y, 0.0 );
	}

	public double getXForLong( double longitude ) {
		return  bkImagePos.x + (bkImageSize.x / bkImageSizeLatLong.x) * ( longitude - bkImagePosLatLong.x );
	}

	public double getYForLat( double latitude ) {
		return   bkImagePos.y + (bkImageSize.y / bkImageSizeLatLong.y) * ( latitude - bkImagePosLatLong.y );
	}

	public double getLongForX( double x ) {
		return  (x - bkImagePos.x)/(bkImageSize.x / bkImageSizeLatLong.x) + bkImagePosLatLong.x ;
	}

	public double getLatForY( double y ) {
		return    (y - bkImagePos.y)/(bkImageSize.y / bkImageSizeLatLong.y) + bkImagePosLatLong.y;
	}

	public Vector3d getVector3dForLatLong( Point2d latLong ) {
		return this.getVector3dForLatLong( latLong.y, latLong.x );
	}

	public Point getCurrentWindowSize() {
		return currentWindowSize;
	}

	public void setCurrentCenter( Point3d in ) {
		currentCenter.set( in );
	}

	public void setCurrentViewer( Point3d in ) {
		currentViewer.set( in );
	}

	public void setCurrentFOV( double fov ) {
		currentFieldOfView = fov;
	}

	public void setCurrentWindowPos( int x, int y ) {
		currentWindowPos = new Point( x, y );
	}

	public void setCurrentWindowSize( int w, int h ) {
		currentWindowSize = new Point( w, h );
	}

	private void setViewerToSeeAll() {
		Vector3d min = new Vector3d(Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE);
		Vector3d max = new Vector3d(Double.MIN_VALUE, Double.MIN_VALUE, Double.MIN_VALUE);

		// default min and max are the bounds of the region's extent
		Vector3d tempExt = this.getSize();
		if( tempExt.x > 0.0 || tempExt.y > 0.0 || tempExt.z > 0.0 ) {
			tempExt.scale(0.5d);
			max.set(tempExt);
			tempExt.scale(-1.0d);
			min.set(tempExt);
		}

		Vector3d temp = new Vector3d();
		if( !boundDefaultToExtent ) {
			for (int i = 0; i < DisplayEntity.getAll().size(); i++) {
				DisplayEntity nextEnt = DisplayEntity.getAll().get(i);
				if (nextEnt.getCurrentRegion() != this)
					continue;

				Vector3d cent = nextEnt.getPositionForAlignment(new Vector3d());
				Vector3d size = nextEnt.getSize();
				size.scale(0.5d);

				temp.sub(cent, size);
				min.x = Math.min(min.x, temp.x);
				min.y = Math.min(min.y, temp.y);
				min.z = Math.min(min.z, temp.z);

				temp.add(cent, size);
				max.x = Math.max(max.x, temp.x);
				max.y = Math.max(max.y, temp.y);
				max.z = Math.max(max.z, temp.z);
			}
		}

		// determine the position of the viewer
		currentFieldOfView = OrbitBehavior.DEFAULT_FOV;

		temp.add(max, min);
		temp.scale(0.5d);
		temp.z = 0.0d;

		currentCenter.set(temp);

		// Determine the orbit radius
		final double VIEW_BORDER_FACTOR = 1.1;
		double radius = max.x - min.x / 2.0 / Math.tan( currentFieldOfView / 2.0 ) * VIEW_BORDER_FACTOR;

		this.setViewerToIsometricForRadius( radius );
	}

	public void setViewerToIsometricForRadius( double radius ) {

		Vector3d temp = new Vector3d();

		// Set the position of the camera for isometric view
		temp.x = currentCenter.x + (radius / Math.sqrt( 3.0 ));
		temp.y = currentCenter.y - (radius / Math.sqrt( 3.0 ));
		temp.z = currentCenter.z + (radius / Math.sqrt( 3.0 ));
		currentViewer.set( temp );
	}

	public void setViewerToDefault() {
		if( defaultsSet ) {
			currentCenter.set( defaultCenter );
			currentViewer.set( defaultViewer );
			currentFieldOfView = defaultFieldOfView;
			setCurrentToDefaults = false;
		}
		else {
			setViewerToIsometricForRadius( defaultViewer.z );
		}

		currentWindowPos = new Point( defaultWindowPos );
		currentWindowSize = new Point( defaultWindowSize );
	}

	public void setupViewer() {
		// if we have defaults, use them
		if( defaultsSet ) {
			if( setCurrentToDefaults ) {
				setViewerToDefault();
			}
		}
		else {
			setViewerToIsometricForRadius( defaultViewer.z );
		}

		currentWindowPos = new Point( defaultWindowPos );
		currentWindowSize = new Point( defaultWindowSize );
	}

	/**
	 * Method to enter region, add self to the entities list.
	 */
	public void enterRegion() {}

	/**
	 * Processes the input data corresponding to the specified keyword. If syntaxOnly is true,
	 * checks input syntax only; otherwise, checks input syntax and process the input values.
	 *
	 * Reads keyword from a configuration file:
	 *   VIEWCENTER	 - default position the viewer looks at
	 *   VIEWER		 - default position of the viewer
	 *   WINDOWPOS   - default position of this region's window on the screen (pixels)
	 *   WINDOWSIZE	 - default size of this region's window on the screen (pixels)
	 *   SHOWWINDOW	 - states whether this region should have a window displayed when the simulation starts
	 *	 BKIMAGE	 - filename for the background image of the region
	 *	 IMAGEPOS	 - location (in region coordinates) of the background image's center
	 *	 IMAGESIZE	 - size (in region coordinates) of the background image
	 */
	public void readData_ForKeyword(StringVector data, String keyword, boolean syntaxOnly, boolean isCfgInput)
	throws InputErrorException {

		if( "VIEWCENTER".equalsIgnoreCase( keyword ) ) {
			Input.assertCount(data, 3);
			DoubleVector temp = Input.parseDoubleVector(data, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
			this.setDefaultCenter(temp.get(0), temp.get(1), temp.get(2));

			Sim3DWindow window = Sim3DWindow.getFirstWindow(this);
			if (window != null) {
				OrbitBehavior ob = window.getBehavior();
				ob.setOrbitCenter(defaultCenter);
				ob.integrateTransforms();
			}
			return;
		}

		if( "VIEWCENTERMOVETODURINGTIME".equalsIgnoreCase( keyword ) ) {
			Input.assertCount(data, 4);
			DoubleVector vec = Tester.parseDouble( data );

			// Determine the end center
			Point3d endCenter = new Point3d( vec.get( 0 ), vec.get( 1 ), vec.get( 2 ) );

			// Determine the time in which to move to the new viewer
			double time = vec.get( 3 );

			this.startProcess( "viewCenterMoveTo_DuringTime", endCenter, time );
			return;
		}

		if( "VIEWER".equalsIgnoreCase( keyword ) ) {
			Input.assertCount(data, 3);
			DoubleVector temp = Input.parseDoubleVector(data, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
			this.setDefaultViewer(temp.get(0), temp.get(1), temp.get(2));

			Sim3DWindow window = Sim3DWindow.getFirstWindow(this);
			if (window != null) {
				OrbitBehavior ob = window.getBehavior();
				ob.setViewer(defaultViewer);
				ob.integrateTransforms();
			}
			return;
		}

		if( "VIEWERMOVETODURINGTIME".equalsIgnoreCase( keyword ) ) {
			Input.assertCount(data, 4);
			DoubleVector vec = Tester.parseDouble( data );

			// Determine the end viewer
			Point3d endViewer = new Point3d( vec.get( 0 ), vec.get( 1 ), vec.get( 2 ) );

			// Determine the time in which to move to the new viewer
			double time = vec.get( 3 );

			this.startProcess( "viewerMoveTo_DuringTime", endViewer, time );
			return;
		}

		if( "FOV".equalsIgnoreCase( keyword ) ) {
			try {
				setDefaultFOV( Tester.parseDouble( data.get( 0 ) ) );
			}
			catch( Exception e ) {
				throw new InputErrorException( (("The value for " + getName()) + " UPVECTOR is invalid") );
			}

			Sim3DWindow window = Sim3DWindow.getFirstWindow(this);
			if (window != null) {
				OrbitBehavior ob = window.getBehavior();
				ob.setFOV(defaultFieldOfView);
				ob.integrateTransforms();
			}
			return;
		}

		if( "FOVMOVETODURINGTIME".equalsIgnoreCase( keyword ) ) {
			Input.assertCount(data, 2);

			// Determine the end FOV
			double endFOV = Tester.parseDouble( data.get( 0 ) );

			// Determine the time in which to move to the new FOV
			double time = Tester.parseDouble( data.get( 1 ) );

			this.startProcess( "FOVMoveTo_DuringTime", endFOV, time );
			return;
		}

		if( "BOUNDTOEXTENT".equalsIgnoreCase( keyword ) ) {
			Input.assertCount(data, 1);
			boundDefaultToExtent = Input.parseBoolean(data.get(0));
			return;
		}

		if( "WINDOWPOS".equalsIgnoreCase( keyword ) ) {
			Input.assertCount(data, 2);
			int x = Input.parseInteger(data.get(0));
			int y = Input.parseInteger(data.get(1));
			setDefaultWindowPos(new Point(x, y));

			Sim3DWindow window = Sim3DWindow.getFirstWindow(this);
			if (window != null) {
				window.setLocation(new Point(x, y));
			}
			return;
		}

		if( "WINDOWSIZE".equalsIgnoreCase( keyword ) ) {
			Input.assertCount(data, 2);
			int x = Input.parseInteger(data.get(0));
			int y = Input.parseInteger(data.get(1));
			setDefaultWindowSize(new Point(x, y));

			Sim3DWindow window = Sim3DWindow.getFirstWindow(this);
			if (window != null) {
				window.setSize(x, y);
			}
			return;
		}
		// --------------- TitleBarText ---------------
		if( "titleBarText".equalsIgnoreCase( keyword ) ) {
			//  data(0) = <String> : text
			Input.assertCount(data, 1);
			titleBarText = data.get( 0 );
			return;
		}
		if( "BKIMAGE".equalsIgnoreCase( keyword ) ) {
			try {
				backgroundImage.setRegion( this );
				backgroundImage.setImage( Util.getStringForData( data ) );
				return;
			}
			catch( Exception e ) {
				throw new InputErrorException( (("The value " + Util.getStringForData( data ) + " for " + getName()) + " BKIMAGE is invalid") );
			}
		}

		if( "IMAGESIZE".equalsIgnoreCase( keyword ) ) {
			Input.assertCount(data, 2);
			double x = Input.parseDouble(data.get(0));
			double y = Input.parseDouble(data.get(1));
			setImageSize( x, y );
			return;
		}

		if( "IMAGEEXTENT".equalsIgnoreCase( keyword ) ) {

			try {
				Vector3d temp = Input.parseVector3d(data, 0.0d, Double.POSITIVE_INFINITY);
				setImageSize( temp.getX(), temp.getY() );
				temp = this.getSize();
				temp.scale(-0.5d);
				setImagePos(temp.x, temp.y, -0.01);
				return;
			}
			catch( Exception e ) {
				throw new InputErrorException( (("The value for " + getName()) + " IMAGEEXTENT is invalid") );
			}
		}

		if( "IMAGEPOS".equalsIgnoreCase( keyword ) ) {
			Input.assertCount(data, 3);
			DoubleVector temp = Input.parseDoubleVector(data, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
			this.setImagePos(temp.get(0), temp.get(1), temp.get(2));
			return;
		}

		if( "IMAGEANGLE".equalsIgnoreCase( keyword ) ) {
			try {
				double inputDouble = Double.parseDouble(data.get(0));
				setImageAngle(inputDouble);
				return;
			}
			catch( Exception e ) {
				throw new InputErrorException( (("The value for " + getName()) + " IMAGEANGLE is invalid") );
			}
		}

		if( "IMAGEPOSLATLONG".equalsIgnoreCase( keyword ) ) {
			Input.assertCount(data, 2);
			double x = Input.parseDouble(data.get(0));
			double y = Input.parseDouble(data.get(1));

			// Reverse x/y  as lat/lon is y/x
			bkImagePosLatLong = new Point2d(y, x);
			return;
		}

		if( "IMAGESIZELATLONG".equalsIgnoreCase( keyword ) ) {
			Input.assertCount(data, 2);
			double x = Input.parseDouble(data.get(0));
			double y = Input.parseDouble(data.get(1));

			// Reverse x/y  as lat/lon is y/x
			bkImageSizeLatLong = new Point2d(y, x);
			simulation.getGUIFrame().enableShowLatLong();
			return;
		}

		if( "IMAGEORIGIN".equalsIgnoreCase( keyword ) ) {
			try {
				Vector3d temp =Input.parseVector3d(data, 0.0d, Double.POSITIVE_INFINITY);
				setImagePos( temp.getX()- this.getSize().x / 2.0 + bkImageSize.x / 2.0, temp.getY() - this.getSize().y / 2.0 + bkImageSize.y / 2.0, -0.01 );
				return;
			}
			catch( Exception e ) {
				throw new InputErrorException( (("The value for " + getName()) + " IMAGEORIGIN is invalid") );
			}
		}

		if( "SHOWWINDOW".equalsIgnoreCase( keyword ) ) {
			Input.assertCount(data, 1);
			showOnStartup = Input.parseBoolean(data.get(0));

			// Is a script running?
			if( getCurrentTime() > 0.0 ) {
				if( showOnStartup ) {
					if( this.getNumWindowsAlive() == 0 ) {
						Sim3DWindow.spawnWindow(this);
					}
				}
				else {
					Sim3DWindow window = Sim3DWindow.getFirstWindow(this);
					if( window != null ) {

						// Close the window
						window.setVisible(false);
					}
				}
			}
			return;
		}

		if( "SHOWTIME".equalsIgnoreCase( keyword ) ) {
			Input.assertCount(data, 1);
			showTime = Input.parseBoolean(data.get(0));
			return;
		}

		if( "MOVETOFRONT".equalsIgnoreCase( keyword ) ) {
			Input.assertCount(data, 1);
			boolean toFront = Input.parseBoolean(data.get(0));
			Sim3DWindow window = Sim3DWindow.getFirstWindow(this);
			if( window != null ) {
				if( toFront ) {
					window.toFront();
				}
				else {
					window.toBack();
				}
			}
			return;
		}

		// keyword is not Region specific, try DisplayEntity
		super.readData_ForKeyword( data, keyword, syntaxOnly, isCfgInput );
	}

	/**
	 *	returns whether a window for this region should be shown when the model is started
	 **/
	public boolean showWindowOnStartup() {
		return showOnStartup;
	}

	public void setShowWindowOnStartup( boolean show ) {
		showOnStartup = show;
	}

	/**
	 *	assigns a size to the background image
	 **/
	public void setImageSize( double x, double y ) {
		bkImageSize = new Point2d( x, y );
		if( backgroundImage != null ) {
			Vector3d imSize = new Vector3d(x, y, 0.0d);
			backgroundImage.setSize(imSize);
		}
	}

	/**
	 *	assigns a position to the background image
	 **/
	public void setImagePos( double x, double y, double z ) {
		bkImagePos.set(x, y, z);
		if( backgroundImage != null ) {
			backgroundImage.setPosition(bkImagePos);
			backgroundImage.setAlignment(new Vector3d());
		}
	}

	/**
	 *	assigns an angle to the background image
	 **/
	public void setImageAngle( double theta ) {
		bkImageAngle = theta;
		if( backgroundImage != null ) {
			backgroundImage.setAngle( theta );
		}
	}

	public double getImageAngle() {
		return bkImageAngle;
	}

	public Vector3d getImagePos() {
		return bkImagePos;
	}

	public void updatePositionViewer(Point3d centerPosition, Point3d viewerPosition, java.awt.Point position, java.awt.Dimension size, double fov) {

		this.setCurrentCenter( centerPosition );
		this.setCurrentViewer( viewerPosition );
		this.setCurrentWindowPos( (int) position.getX(), (int) position.getY() );
		this.setCurrentWindowSize( (int)size.getWidth(), (int)size.getHeight() );
		this.setCurrentFOV( fov );
	}

	public void updateViewCenterInput() {
		// Update the ViewCenter input
		Input<?> in = this.getInput( "ViewCenter" );
		if ( in != null ) {
			InputAgent.processEntity_Keyword_Value(this, in,
					String.format( "%f %f %f", currentCenter.x,
							currentCenter.y, currentCenter.z ) );
		}
	}

	public void updateViewerInput() {
		// Update the Viewer input
		Input<?> in = this.getInput( "Viewer" );
		if ( in != null ) {
			InputAgent.processEntity_Keyword_Value(this, in,
					String.format( "%f %f %f", currentViewer.x,
							currentViewer.y, currentViewer.z ) );
		}
	}

	public void updateFOVInput() {
		// Update the FOV input
		Input<?> in = this.getInput( "FOV" );
		if ( in != null ) {
			InputAgent.processEntity_Keyword_Value(this, in,
					String.format( "%f", currentFieldOfView ));
		}
	}

	public void updateWindowPosInput() {
		// Update the WindowPos input
		Input<?> in = this.getInput( "WindowPos" );
		if ( in != null ) {

			// If the window is still the default position, then do nothing
			if( currentWindowPos.equals( defaultWindowPos ) ) {
				return;
			}

			if( numWindowsAlive > 1 )
				return;

			InputAgent.processEntity_Keyword_Value(this, in,
					String.format( "%d %d", (int) currentWindowPos.getX(),
							(int) currentWindowPos.getY() ) );
		}
	}

	public void updateWindowSizeInput() {
		// Update the WindowSize input
		Input<?> in = this.getInput( "WindowSize" );
		if ( in != null ) {

			// If the window is still the default size, then do nothing
			if( currentWindowSize.equals( defaultWindowSize ) ) {
				return;
			}

			if( numWindowsAlive > 1 )
				return;

			InputAgent.processEntity_Keyword_Value(this, in,
					String.format( "%d %d", (int)currentWindowSize.getX(),
							(int)currentWindowSize.getY() ) );
		}
	}

	/**
	 * Move the viewer to the given viewer during the given time.
	 * This method is called during a script for continuous zooming and panning.
	 */
	public void viewerMoveTo_DuringTime( Point3d endViewer, double time ) {

		// Is a script running?
		if( getCurrentTime() > 0.0 ) {
			Sim3DWindow window = Sim3DWindow.getFirstWindow(this);
			if( window != null ) {
				window.toFront();

				// Determine the number of time increments
				int num = (int)(time / simulation.getCaptureInterval());

				// Determine the time increment between viewer changes
				double dt = time / num;

				// Determine the viewer
				OrbitBehavior ob = window.getBehavior();
				Point3d startViewer = ob.getViewer();
				Point3d interp = new Point3d();
				// Loop through the time increments
				for( int i = 0; i < num; i++ ) {

					scheduleWait( dt );

					// Set the next viewer
					interp.interpolate(startViewer, endViewer, (double)(i + 1) / num);
					ob.setViewer(interp);
					ob.integrateTransforms();
				}
			}
		}
	}

	/**
	 * Move the view center to the given view center during the given time.
	 * This method is called during a script for continuous panning.
	 */
	public void viewCenterMoveTo_DuringTime( Point3d endCenter, double time ) {

		// Is a script running?
		if( getCurrentTime() > 0.0 ) {
			Sim3DWindow window = Sim3DWindow.getFirstWindow(this);
			if( window != null ) {
				window.toFront();

				// Determine the number of time increments
				int num = (int)(time / simulation.getCaptureInterval());

				// Determine the time increment between viewer changes
				double dt = time / num;

				// Determine the view center
				OrbitBehavior ob = window.getBehavior();
				Point3d startCenter = ob.getCenterPosition();
				Point3d interp = new Point3d();
				// Loop through the time increments
				for( int i = 0; i < num; i++ ) {

					scheduleWait( dt );

					// Set the next viewer
					interp.interpolate(startCenter, endCenter, (double)(i + 1) / num);
					ob.setOrbitCenter(interp);
					ob.integrateTransforms();
				}
			}
		}
	}

	/**
	 * Move the FOV to the given FOV during the given time.
	 * This method is called during a script for continuous panning.
	 */
	public void FOVMoveTo_DuringTime( double endFOV, double time ) {

		// Is a script running?
		if( getCurrentTime() > 0.0 ) {
			Sim3DWindow window = Sim3DWindow.getFirstWindow(this);
			if( window != null ) {
				window.toFront();

				// Determine the number of time increments
				int num = (int)(time / simulation.getCaptureInterval());

				// Determine the time increment between viewer changes
				double dt = time / num;

				// Determine the FOV
				OrbitBehavior ob = window.getBehavior();
				double fov = ob.getFOV();

				// Loop through the time increments
				for( int i = 0; i < num; i++ ) {

					scheduleWait( dt );

					// Set the next FOV
					double alpha = 1.0 / (num-i);
					fov = ((1.0 - alpha)*fov) + (alpha * endFOV);
					ob.setFOV(fov);
					ob.updateBehaviour();
				}
			}
		}
	}
}
