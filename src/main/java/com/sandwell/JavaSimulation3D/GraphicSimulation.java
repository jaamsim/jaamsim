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

import java.util.ArrayList;

import com.jaamsim.controllers.RenderManager;
import com.jaamsim.input.InputAgent;
import com.jaamsim.ui.ExceptionBox;
import com.jaamsim.ui.FrameBox;
import com.jaamsim.ui.View;
import com.sandwell.JavaSimulation.BooleanInput;
import com.sandwell.JavaSimulation.ColourInput;
import com.sandwell.JavaSimulation.DoubleInput;
import com.sandwell.JavaSimulation.Entity;
import com.sandwell.JavaSimulation.EntityListInput;
import com.sandwell.JavaSimulation.Input;
import com.sandwell.JavaSimulation.InputErrorException;
import com.sandwell.JavaSimulation.IntegerInput;
import com.sandwell.JavaSimulation.IntegerListInput;
import com.sandwell.JavaSimulation.IntegerVector;
import com.sandwell.JavaSimulation.Keyword;
import com.sandwell.JavaSimulation.Process;
import com.sandwell.JavaSimulation.Simulation;
import com.sandwell.JavaSimulation.StringVector;
import com.sandwell.JavaSimulation.TimeInput;

/**
 * Abstracts the Java 3D interface out of simulation for those programs which do
 * not require that functionality. <br>
 * Simulation provides simulation functionality. <br>
 * GraphicSimulation provides user interaction functionality. <br>
 * <br>
 * Integrates with GUIFrame and OrbitBehaviour to provide user interaction.
 */
public class GraphicSimulation extends Simulation {
	protected String modelName = "Transportation Logistics Simulator";

	private boolean captureFlag = false;  // true when capturing is in progress
	private boolean startCapturerFlag = false;
	private boolean hasRunStartup = false;

	private int numFramesWritten = 0;

	@Keyword(desc = "Simulated time between screen captures",
	         example = "This is placeholder example text")
	private final TimeInput captureInterval;

	@Keyword(desc = "How long the simulation waits until starting video recording",
	         example = "This is placeholder example text")
	private final TimeInput captureStartTime;

	@Keyword(desc = "jpeg quality from 0.0f to 1.0f",
	         example = "This is placeholder example text")
	private final DoubleInput captureQuality;

	@Keyword(desc = "Number of frames to capture",
	         example = "This is placeholder example text")
	private final IntegerInput captureFrames;

	@Keyword(desc = "If the video recorder should save out PNG files of individual frames",
	         example = "This is placeholder example text")
	private final BooleanInput saveImages;

	@Keyword(desc = "If the video recorder should save out an AVI file",
	         example = "This is placeholder example text")
	private final BooleanInput saveVideo;

	@Keyword(desc = "The size of the video/image in pixels",
	         example = "This is placeholder example text")
	private final IntegerListInput captureArea;

	@Keyword(desc = "The background color to use for video recording",
	         example = "This is placeholder example text")
	private final ColourInput videoBGColor;

	@Keyword(desc = "The list of views to draw in the video",
	         example = "This is placeholder example text")
	private final EntityListInput<View> captureViews;

	protected Process captureThread = null;

	{
		captureStartTime = new TimeInput( "CaptureStartTime", "Key Inputs", 0.0 );
		captureStartTime.setValidRange( 0, Double.POSITIVE_INFINITY );
		captureStartTime.setUnits( "h" );
		this.addInput( captureStartTime, true );

		captureInterval = new TimeInput( "CaptureInterval", "Key Inputs", 1.0 );
		captureInterval.setValidRange( 1e-15d, Double.POSITIVE_INFINITY );
		captureInterval.setUnits( "h" );
		this.addInput( captureInterval, true );
		captureInterval.setHidden(true);

		captureQuality = new DoubleInput( "CaptureQuality", "Key Inputs", 0.9 );
		captureQuality.setValidRange( 0.0d, 1.0d );
		this.addInput( captureQuality, true );
		captureQuality.setHidden(true);

		videoBGColor = new ColourInput("VideoBackgroundColor", "Key Inputs", ColourInput.WHITE);
		this.addInput(videoBGColor, true, "Colour");

		captureFrames = new IntegerInput("CaptureFrames", "Key Inputs", 0);
		captureFrames.setValidRange(0, 30000);
		this.addInput(captureFrames, true);

		saveImages = new BooleanInput("SaveImages", "Key Inputs", false);
		this.addInput(saveImages, true);

		saveVideo = new BooleanInput("SaveVideo", "Key Inputs", false);
		this.addInput(saveVideo, true);

		IntegerVector defArea = new IntegerVector(2);
		defArea.add(1000);
		defArea.add(1000);
		captureArea = new IntegerListInput("CaptureArea", "Key Inputs", defArea);
		captureArea.setValidCount(2);
		captureArea.setValidRange(0, 3000);
		this.addInput(captureArea, true);

		captureViews = new EntityListInput<View>(View.class, "CaptureViews", "Key Inputs", new ArrayList<View>(0));
		this.addInput(captureViews, true);

		addEditableKeyword( "VideoCapture",          "",        		"  ",          false, "Key Inputs" );
		this.getInput("VideoCapture").setHidden(true);
	}

	/**
	 *  Constructor for the Graphic Simulation.
	 *	Establishes the User Interface
	 *  Protected makes this a 'singleton' class -- only one instance of it exists.  is instantiated through 'getSimulation()' method.
	 */
	public GraphicSimulation() {
		// Create main frame
		DisplayEntity.setSimulation( this );
	}

	@Override
	public void startUp() {
		super.startUp();
		if (startCapturerFlag) {

			this.startProcess("doCaptureNetwork");
		}
		this.hasRunStartup = true;
	}

	/**
	 * Processes the input data corresponding to the specified keyword. If syntaxOnly is true,
	 * checks input syntax only; otherwise, checks input syntax and process the input values.
	 */
	@Override
	public void readData_ForKeyword(StringVector data, String keyword, boolean syntaxOnly, boolean isCfgInput)
	throws InputErrorException {

		if( "VideoCapture".equalsIgnoreCase( keyword ) ) {
			Input.assertCount(data, 1);
			boolean bool = Input.parseBoolean(data.get(0));

			if( bool ) {
				// This is a work around for a bug, right now the external process needs to be started
				// after the model has started. If the model has not started, simply defer starting
				// the video recording process until startup()
				// TODO: Review this and the process start code
				if (hasRunStartup) {
					this.startExternalProcess("doCaptureNetwork");
				} else {
					this.startCapturerFlag = true;
				}
			}
			else {
				this.setCaptureFlag( false );
			}
			return;
		}
		super.readData_ForKeyword( data, keyword, syntaxOnly, isCfgInput );
	}

	public void setModelName( String newModelName ) {
		modelName = newModelName;
	}

	public String getModelName() {
		return modelName;
	}

	@Override
	public void clear() {
		super.clear();

		// close warning/error trace file
		InputAgent.closeLogFile();

		ArrayList<FrameBox> boxes = new ArrayList<FrameBox>(FrameBox.getAllFB());
		for (FrameBox each : boxes) {
			each.dispose();
		}

		EntityPallet.clear();

		if (RenderManager.isGood()) {
			RenderManager.inst().closeAllWindows();
		}

		// Kill all entities except simulation
		while(Entity.getAll().size() > 1) {
			Entity ent = Entity.getAll().get(Entity.getAll().size()-1);
			ent.kill();
		}

		GUIFrame.instance().updateForSimulationState();
	}

	@Override
	public void start() {
		try {
			super.start();
			GUIFrame.instance().updateForSimulationState();
		}
		catch( Throwable t ) {
			ExceptionBox.instance().setError(t);
		}
	}

	@Override
	public void pause() {
		try {
			super.pause();
			GUIFrame.instance().updateForSimulationState();
		}
		catch( Throwable t ) {
			ExceptionBox.instance().setError(t);
		}
	}

	@Override
	public void stop() {
		try {
			super.stop();
			GUIFrame.instance().updateForSimulationState();
		}
		catch( Throwable t ) {
			ExceptionBox.instance().setError(t);
		}
	}

	@Override
	public void resume() {
		try {
			super.resume();
			GUIFrame.instance().updateForSimulationState();
		}
		catch( Throwable t ) {
			ExceptionBox.instance().setError(t);
		}
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

		scheduleWait(captureStartTime.getValue(), 10);

		if (!RenderManager.isGood()) {
			RenderManager.initialize();
		}

		int width = captureArea.getValue().get(0);
		int height = captureArea.getValue().get(1);

		ArrayList<View> views = captureViews.getValue();

		RenderManager.inst().resetRecorder(views, width, height, InputAgent.getRunName(), captureFrames.getValue(),
				saveImages.getValue(), saveVideo.getValue(), videoBGColor.getValue());

		// Otherwise, start capturing
		captureFlag = true;
		while( captureFlag) {

			RenderManager.inst().blockOnScreenShot();
			++numFramesWritten;

			if (numFramesWritten == captureFrames.getValue()) {
				break;
			}

			// Wait until the next time to capture a frame
			// (priority 10 is used to allow higher priority events to complete first)
			captureThread = Process.current();
			scheduleWait( this.getCaptureInterval(), 10 );
			captureThread = null;
		}

		RenderManager.inst().endRecording();
	}

	@Override
	public void updateForSimulationState() {
		super.updateForSimulationState();
		GUIFrame.instance().updateForSimulationState();
	}
}
