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

import com.sandwell.JavaSimulation.ColourInput;
import com.sandwell.JavaSimulation.DoubleInput;
import com.sandwell.JavaSimulation.ErrorException;
import com.sandwell.JavaSimulation.FileEntity;
import com.sandwell.JavaSimulation.FileInput;
import com.sandwell.JavaSimulation.Input;
import com.sandwell.JavaSimulation.InputErrorException;
import com.sandwell.JavaSimulation.Simulation;
import com.sandwell.JavaSimulation.Tester;
import com.sandwell.JavaSimulation.TimeInput;
import com.sandwell.JavaSimulation.Util;
import com.sandwell.JavaSimulation.Process;
import com.sandwell.JavaSimulation.StringVector;

import java.awt.Color;
import java.util.ArrayList;

import javax.imageio.ImageIO;
import javax.media.j3d.AmbientLight;
import javax.media.j3d.Appearance;
import javax.media.j3d.Background;
import javax.media.j3d.BoundingSphere;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.DirectionalLight;
import javax.media.j3d.Light;
import javax.media.j3d.Locale;
import javax.media.j3d.Texture;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.media.j3d.VirtualUniverse;
import javax.swing.JFrame;
import javax.vecmath.Color3f;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;
import javax.vecmath.Vector3f;

import com.sun.image.codec.jpeg.JPEGCodec;
import com.sun.image.codec.jpeg.JPEGEncodeParam;
import com.sun.image.codec.jpeg.JPEGImageEncoder;
import com.sun.j3d.utils.geometry.Primitive;
import com.sun.j3d.utils.geometry.Sphere;
import com.sun.j3d.utils.image.TextureLoader;

import java.awt.image.BufferedImage;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.File;
import java.lang.Exception;
import java.net.MalformedURLException;
import java.net.URL;

import com.sandwell.JavaSimulation.Entity;
import com.sandwell.JavaSimulation3D.util.Shape;

/**
 * Abstracts the Java 3D interface out of simulation for those programs which do
 * not require that functionality. <br>
 * Simulation provides simulation functionality. <br>
 * GraphicSimulation provides user interaction functionality. <br>
 * <br>
 * Integrates with GUIFrame and OrbitBehaviour to provide user interaction.
 */
public class GraphicSimulation extends Simulation {
	private final GUIFrame guiFrame;
	protected String modelName = "Ausenco JaamSim";

	// Java3d base objects
	private final VirtualUniverse universe;
	private final Locale rootLocale;
	private final BranchGroup globalGroup;
	private final Background background;

	/** list of all the active windows */
	private ArrayList<Sim3DWindow> windowList;

	private boolean captureFlag = false;  // true when capturing is in progress
	private final TimeInput captureInterval; // simulated time between screen captures
	private final DoubleInput captureQuality;  // jpeg quality from 0.0f to 1.0f
	private int captureNumber = 0;        // four digit number included in jpg file name
	protected Process captureThread = null;

	private final FileInput skyImage;
	private final Appearance skyAppearance;
	private final ColourInput backgroundColour;

	{
		skyImage = new FileInput( "SkyImage", "Optional", "" );
		this.addInput(skyImage, true);

		backgroundColour = new ColourInput("BackgroundColour", "Optional", Shape.getPresetColor(Shape.COLOR_WHITE));
		this.addInput(backgroundColour, true);

		captureInterval = new TimeInput( "CaptureInterval", "Optional", 1.0 );
		captureInterval.setValidRange( 1e-15d, Double.POSITIVE_INFINITY );
		captureInterval.setUnits( "h" );
		this.addInput( captureInterval, true );

		captureQuality = new DoubleInput( "CaptureQuality", "Optional", 0.9 );
		captureQuality.setValidRange( 0.0d, 1.0d );
		this.addInput( captureQuality, true );

		addEditableKeyword( "VideoCapture",          "",        		"  ",          false, "Script" );
		addEditableKeyword( "RealTime"    ,          "",        		"  ",          false, "Script" );
		addEditableKeyword( "RealTimeFactor",        "",        		"  ",          false, "Script" );
	}

	/**
	 *  Constructor for the Graphic Simulation.
	 *	Establishes the User Interface
	 *  Protected makes this a 'singleton' class -- only one instance of it exists.  is instantiated through 'getSimulation()' method.
	 */
	public GraphicSimulation() {

		// Write Java3D dll files if they are contained in the jar file
		try {
			String[] libraries = new String[] {"j3dcore-d3d.dll",
											   "j3dcore-ogl.dll",
											   "j3dcore-ogl-chk.dll",
											   "j3dcore-ogl-cg.dll" };

			for( int i = 0; i < libraries.length; i++ ) {
				File dllFile = new File(libraries[i]);

				if (dllFile.exists()) {
					continue;
				}

				InputStream inputStream = Simulation.class.getResourceAsStream("/resources/win32/" + libraries[i]);
				if( inputStream != null ) {
					FileOutputStream outputStream = new FileOutputStream(dllFile);
					byte[] array = new byte[8192];
					for (int j = inputStream.read(array); j != -1; j = inputStream.read(array) ) {
						outputStream.write(array, 0, j);
					}
					outputStream.close();
					inputStream.close();
				}
			}
		}
		catch( Throwable t ) {
			new ExceptionBox( t, true );
		}

		// Create main frame
		DisplayEntity.setSimulation( this );
		guiFrame = new GUIFrame(modelName);

		// Universe to hold graphic objects
		universe = new VirtualUniverse();
		rootLocale = new Locale( universe );

		BoundingSphere bounds = new BoundingSphere(new Point3d(), Double.POSITIVE_INFINITY);

		// Graphics update Behavior
		GraphicsUpdateBehavior update = new GraphicsUpdateBehavior();
		update.setSchedulingBounds(bounds);
		globalGroup = new BranchGroup();
		globalGroup.setCapability(BranchGroup.ALLOW_DETACH);
		globalGroup.addChild(update);

		// Background color
		background = new Background(new Color3f(Color.white));
		background.setCapability(Background.ALLOW_COLOR_WRITE);

		// Create skydome sphere
		skyAppearance = new Appearance();
		skyAppearance.setCapability(Appearance.ALLOW_TEXTURE_WRITE);
		Sphere sphere = new Sphere(1f, Sphere.GENERATE_NORMALS |
				Primitive.GENERATE_TEXTURE_COORDS |
				Primitive.GENERATE_NORMALS_INWARD, 45, skyAppearance);

		// Rotate the sphere so the top hemisphere is the sky
		Transform3D rotation = new Transform3D();
		rotation.setEuler(new Vector3d(-Math.PI/2, 0, 0));
		TransformGroup tg = new TransformGroup(rotation);
		tg.addChild(sphere);
		BranchGroup bg = new BranchGroup();
		bg.addChild(tg);
		background.setGeometry(bg);

		background.setApplicationBounds(bounds);
		globalGroup.addChild(background);

		// Set up lighting
		setupLightingForBranchGroup_WithinBounds(globalGroup, bounds);

		// Compile top-level group
		globalGroup.compile();

		windowList = new ArrayList<Sim3DWindow>();

		guiFrame.updateForSimulationState();

		defaultRegion = new Region();
		defaultRegion.setName("ModelStage");
		defaultRegion.setInputName("ModelStage");
		defaultRegion.showTime = true;
		setRegion(defaultRegion);
	}

	public static void setupLightingForBranchGroup_WithinBounds(BranchGroup branchGroup, BoundingSphere bounds) {
		Light light;
		light = new DirectionalLight(new Color3f(0.8156863f, 0.8156863f, 0.8156863f), new Vector3f(1.5f, -1, -0.8f));
		light.setInfluencingBounds(bounds);
		branchGroup.addChild(light);

		light = new DirectionalLight(new Color3f(0.8156863f, 0.8156863f, 0.8156863f), new Vector3f(-1.5f, -1, -0.8f));
		light.setInfluencingBounds(bounds);
		branchGroup.addChild(light);

		light = new DirectionalLight(new Color3f(0.8156863f, 0.8156863f, 0.8156863f), new Vector3f(0, 1, -0.8f));
		light.setInfluencingBounds(bounds);
		branchGroup.addChild(light);

		light = new DirectionalLight(new Color3f(0.57098037f, 0.57098037f, 0.57098037f), new Vector3f(0, 0, 1f));
		light.setInfluencingBounds(bounds);
		branchGroup.addChild(light);

		light = new AmbientLight(new Color3f(0.2f, 0.2f, 0.2f));
		light.setInfluencingBounds(bounds);
		branchGroup.addChild(light);

		light = null;
	}

	public void earlyInit() {
		super.earlyInit();

		Color3f col = new Color3f();
		backgroundColour.getValue().getColor( col );
		this.setBackgroundColor( col );
	}

	/**
	 * Processes the input data corresponding to the specified keyword. If syntaxOnly is true,
	 * checks input syntax only; otherwise, checks input syntax and process the input values.
	 */
	public void readData_ForKeyword(StringVector data, String keyword, boolean syntaxOnly, boolean isCfgInput)
	throws InputErrorException {

		if( "VideoCapture".equalsIgnoreCase( keyword ) ) {
			Input.assertCount(data, 1);
			boolean bool = Input.parseBoolean(data.get(0));

			if( bool ) {
				this.startExternalProcess("doCaptureNetwork");
			}
			else {
				this.setCaptureFlag( false );
			}
			return;
		}
		if( "RealTime".equalsIgnoreCase( keyword ) ) {
			Input.assertCount(data, 1);
			boolean bool = Input.parseBoolean(data.get(0));
			this.setRealTimeExecution( bool );
			return;
		}

		// --------------- RealTimeFactor ---------------
		if( "RealTimeFactor".equalsIgnoreCase( keyword ) ) {
			Input.assertCount(data, 1);
			double factor = Tester.parseDouble( data.get( 0 ) );
			this.setRealTimeFactor( factor );
			return;
		}
		super.readData_ForKeyword( data, keyword, syntaxOnly, isCfgInput );
	}

	protected void setAlwaysOnTop(boolean alwaysOnTop) {
		guiFrame.setAlwaysOnTop(alwaysOnTop);
	}

	public void setMinimized() {
		guiFrame.setExtendedState(JFrame.ICONIFIED);
	}

	public void setRestore() {
		guiFrame.setExtendedState(JFrame.NORMAL );
	}

	public void setModelName( String newModelName ) {
		modelName = newModelName;
	}

	public String getModelName() {
		return modelName;
	}

	public void setBackgroundColor(Color3f col) {
		background.setColor(col);
	}

	public void setSkyImage() {

		// Defining a unit sphere for the sky background
		if(!skyImage.getValue().isEmpty()) {
			URL file=null;
			try {
				file = new URL(Util.getAbsoluteFilePath( skyImage.getValue() ));
			}
			catch (MalformedURLException e) {}
			BufferedImage textureImage=null;
			try {
				textureImage = ImageIO.read(file);
			} catch (IOException e) {
				throw new ErrorException( "Could not load image %s", skyImage.getValue() );
			}
			if (textureImage != null) {
				TextureLoader textureLoader = new TextureLoader(textureImage);
				Texture texture = textureLoader.getTexture();
				skyAppearance.setTexture(texture);
			}
		}
	}

	public void clear() {
		super.clear();

		// close warning/error trace file
		InputAgent.closeLogFile();

		ArrayList<FrameBox> boxes = new ArrayList<FrameBox>(FrameBox.getAll());
		for (FrameBox each : boxes) {
			each.dispose();
		}

		EntityPallet.clear();

		// remove the sky
		skyAppearance.setTexture( null );

		// close all windows
		while( windowList.size() > 0 ) {
			windowList.get(0).dispose();
		}
		windowList.clear();

		// Kill all entities except simulation
		while(Entity.getAll().size() > 1) {
			Entity ent = Entity.getAll().get(Entity.getAll().size()-1);
			ent.kill();
		}

		guiFrame.updateForSimulationState();

		// Set the default region to null so that the new default region won't have a region
		defaultRegion = null;

		// Create a new default region
		defaultRegion = new Region();
		defaultRegion.setName("ModelStage");
		defaultRegion.setInputName("ModelStage");
		defaultRegion.showTime = true;
		setRegion(defaultRegion);
	}

	public void configure(String configFileName) {
		try {
			guiFrame.clear();
			super.configure(configFileName);
			this.setSkyImage();

			// show the present state in the user interface
			guiFrame.setTitle( getModelName() + " - " + InputAgent.getRunName() );
			guiFrame.updateForSimulationState();

			// setup regions and display the default windows
			showStartupWindows();
		}
		catch( Throwable t ) {
			new ExceptionBox(t, true);
		}
	}

	public void start() {
		try {
			super.start();
			guiFrame.updateForSimulationState();
		}
		catch( Throwable t ) {
			new ExceptionBox(t, true);
		}
	}

	public void pause() {
		try {
			super.pause();
			guiFrame.updateForSimulationState();
		}
		catch( Throwable t ) {
			new ExceptionBox(t, true);
		}
	}

	public void stop() {
		try {
			super.stop();
			guiFrame.updateForSimulationState();
		}
		catch( Throwable t ) {
			new ExceptionBox(t, true);
		}
	}

	public void resume() {
		try {
			super.resume();
			guiFrame.updateForSimulationState();
		}
		catch( Throwable t ) {
			new ExceptionBox(t, true);
		}
	}

	/**
	 * Call this function to create a new view.
	 * @param region - the Region that is the basis for the view
	 */
	public Sim3DWindow spawnWindow( Region region ) {
		Sim3DWindow view = new Sim3DWindow( region, rootLocale);

		view.setParentMenu( guiFrame.getWindowList() );
		synchronized (windowList) {
			windowList.add( view );

			// J3D Memory Leak fix - don't lose memory if we aren't looking
			if( windowList.size() == 1 ) {
				rootLocale.addBranchGraph(globalGroup);
			}
		}

		view.setVisible(true);

		return view;
	}

	/** removes the specified window from the simulation
	 *	@param view - the window displaying a Region in the simulation
	 */

	public void removeWindow( Sim3DWindow view ) {
		synchronized (windowList) {
			// it appears that dispose() can be called multiple times on a window,
			// for now, just bail out early to avoid a racing removal between the
			// two calls
			if (windowList.size() == 0)
				return;

			windowList.remove( view );
			if (windowList.size() > 0)
				return;

			rootLocale.removeBranchGraph(globalGroup);
		}
	}

	/**
	 *	finds the first window for the specified region
	 *	@param region - the region to search for
	 *	@return Sim3DWindow - the first window for the specified region.
	 *   null if no window was found for the specified region.
	 */
	public Sim3DWindow getWindowForRegion( Region region ) {
		// go through the list of windows and return the first for the specified region
		synchronized (windowList) {
			for (Sim3DWindow win : windowList) {
				if( win.getRegion() == region )
					return win;
			}
		}

		// none were found, signify with null
		return null;
	}

	/** visually registers the graphics for a region
	 *   @param region - the region to have the graphics displayed
	 */
	void registerGraphics( Region region ) {
		rootLocale.addBranchGraph( region.getBranchGroup() );
	}

	/** visually removes the regions graphics from the simulation
	 *	@param removeRegion - the region for which graphics are removed
	 */
	void unregisterGraphics( Region removeRegion ) {
		rootLocale.removeBranchGraph( removeRegion.getBranchGroup() );
	}

	/** provides a reference to the simulation User Interface controls
	 *   @return GUIFrame - the user interface control window
	 */
	public GUIFrame getGUIFrame() {
		return guiFrame;
	}

	public void setProgress(int percentage) {
		guiFrame.setProgress(percentage);
	}

	public void setProgressText(String text) {
		guiFrame.setProgressText(text);
	}

	/**
	 * Updates the progress bar in the status bar to show the percent completion of the run
	 */
	public void updateRunProgress() {

		long lastSystemTime;
		long currentSystemTime;
		long elapsedSystemTime;
		double lastSimulatedTime;
		double currentSimulatedTime;
		double elapsedSimulatedTime;
		double remainingSimulatedTime;
		double remainingSystemTime;
		double speedUpFactor = 0.0;

		// Determine the system starting time
		lastSystemTime = System.currentTimeMillis();
		lastSimulatedTime = getCurrentTime();

		// Determine the number of hours in run (including initialization)
		double duration = (this.getRunDuration() + this.getInitializationTime());

		if( !(Tester.equalCheckTimeStep( duration, 0.0 )) ) {

			double step = (duration / 100.0);
			int percentComplete = 0;
			this.setProgress( percentComplete );

			while( percentComplete < 100 ) {

				// Wait for 1% of the run time
				scheduleWait( step );

				currentSystemTime = System.currentTimeMillis();
				currentSimulatedTime = getCurrentTime();

				// Update the percent complete
				percentComplete++;
				this.setProgress(percentComplete);

				// Determine the elapsed system time
				elapsedSystemTime = currentSystemTime - lastSystemTime;

				// Determine the elapsed simulated time
				elapsedSimulatedTime = currentSimulatedTime - lastSimulatedTime;

				// Determine the speed-up factor
				speedUpFactor = (elapsedSimulatedTime * 3600.0) / (elapsedSystemTime / 1000.0);
				guiFrame.setSpeedUp( speedUpFactor );

				// Determine the remaining simulated time
				remainingSimulatedTime = duration - currentSimulatedTime;

				// Determine the remaining system time
				remainingSystemTime = remainingSimulatedTime / speedUpFactor * 60.0;
				guiFrame.setRemaining( remainingSystemTime );

				lastSystemTime = currentSystemTime;
				lastSimulatedTime = currentSimulatedTime;
			}
		}
	}

	/**
	 * displays windows that are flagged to show on startup
	 */
	public void showStartupWindows() {
		for (Region each : Region.getAll()) {
			each.setupViewer();
			each.setViewerToDefault();
			if( each.showWindowOnStartup() )
				if( each.getNumWindowsAlive() == 0 )
					spawnWindow( each );
		}
	}

	public void setRealTimeExecution( boolean useRealTime ) {
		super.setRealTimeExecution( useRealTime );
		guiFrame.updateForRealTime();
	}

	public void setRealTimeFactor( double newRealTimeFactor ) {
		super.setRealTimeFactor( newRealTimeFactor );
		guiFrame.updateForRealTime();
	}

	public void setCaptureFlag( boolean flag ) {
		captureFlag = flag;
	}

	public double getCaptureInterval() {
		return captureInterval.getValue();
	}

	/**
	 * Capture JPEG images of the screen at regular simulated intervals
	 */
	public void doCaptureNetwork() {

		// If the capture network is already in progress, then stop the previous network
		if( captureThread != null ) {
			Process.terminate(captureThread);
			captureThread = null;
		}

		// Otherwise, start capturing
		captureFlag = true;
		while( captureFlag ) {

			try {
				java.awt.Robot robot = new java.awt.Robot();
				java.awt.Rectangle rect = new java.awt.Rectangle(java.awt.Toolkit.getDefaultToolkit().getScreenSize() );
				BufferedImage img = robot.createScreenCapture( rect );

				FileOutputStream out = new FileOutputStream(String.format("%s%04d.jpg", InputAgent.getRunName(), captureNumber));
				JPEGImageEncoder encoder = JPEGCodec.createJPEGEncoder( out );
				JPEGEncodeParam param = encoder.getDefaultJPEGEncodeParam( img );
				param.setQuality( (float)captureQuality.getValue().doubleValue(), false ); // default is 90% qualith JPEG
				param.setHorizontalSubsampling(0, 1);
				param.setHorizontalSubsampling(1, 1);
				param.setHorizontalSubsampling(2, 1);
				param.setVerticalSubsampling(0, 1);
				param.setVerticalSubsampling(1, 1);
				param.setVerticalSubsampling(2, 1);
				encoder.setJPEGEncodeParam( param );
				encoder.encode( img );
				out.close();
			}
			catch( Exception e ) {
				System.out.println( e );
			}
			captureNumber++;

			// Wait until the next time to capture a frame
			// (priority 10 is used to allow higher priority events to complete first)
			captureThread = Process.current();
			scheduleWait( this.getCaptureInterval(), 10 );
			captureThread = null;
		}
	}

	/**
	 * create screenshot, only capture the view if view is specified
	 */
	public void createScreenShot( File file, Region region, Sim3DWindow view ){
		try {
			java.awt.Rectangle rect;
			java.awt.Robot robot = new java.awt.Robot();

			if( view == null ){
				throw new ErrorException( "GraphicalSimulation.createScreenShot cannot find window ");
			}

			if ( region != null ){
				rect = new java.awt.Rectangle(view.getLocation().x, view.getLocation().y,
											  view.getSize().width, view.getSize().height);
			}
			else {
				rect = new java.awt.Rectangle( java.awt.Toolkit.getDefaultToolkit().getScreenSize() );
			}

			BufferedImage img = robot.createScreenCapture( rect );

			ImageIO.write(img, "png", file);
		}
		catch( Exception e ) {
			System.out.println( e );
		}
	}

	public void updateTime(double simTime) {
		super.updateTime(simTime);

		// Update the clock display
		guiFrame.setClock(simTime);
		GraphicsUpdateBehavior.simTime = simTime;

		FrameBox.valueUpdate();
	}

	public void validate()
	throws InputErrorException {
		super.validate();

		if( ! skyImage.getValue().isEmpty() && ! (FileEntity.fileExists(skyImage.getValue())) ) {
			throw new InputErrorException("File \"%s\" not found", skyImage.getValue());
		}
	}

	public void startUp() {
		super.startUp();
		this.startProcess("updateRunProgress");
	}
}
