/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2013 Ausenco Engineering Canada Inc.
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

import java.util.ArrayList;

import com.jaamsim.controllers.RenderManager;
import com.jaamsim.controllers.VideoRecorder;
import com.jaamsim.datatypes.IntegerVector;
import com.jaamsim.events.EventHandle;
import com.jaamsim.events.EventManager;
import com.jaamsim.events.ProcessTarget;
import com.jaamsim.input.BooleanInput;
import com.jaamsim.input.ColourInput;
import com.jaamsim.input.EntityListInput;
import com.jaamsim.input.Input;
import com.jaamsim.input.InputAgent;
import com.jaamsim.input.IntegerInput;
import com.jaamsim.input.IntegerListInput;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.StringInput;
import com.jaamsim.input.ValueInput;
import com.jaamsim.units.TimeUnit;
import com.sandwell.JavaSimulation.Entity;

public class VideoRecorderEntity extends Entity {
	@Keyword(description = "Simulated time between screen captures",
	         example = "This is placeholder example text")
	private final ValueInput captureInterval;

	@Keyword(description = "How long the simulation waits until starting video recording",
	         example = "This is placeholder example text")
	private final ValueInput captureStartTime;

	@Keyword(description = "Number of frames to capture",
	         example = "This is placeholder example text")
	private final IntegerInput captureFrames;

	@Keyword(description = "If the video recorder should save out PNG files of individual frames",
	         example = "This is placeholder example text")
	private final BooleanInput saveImages;

	@Keyword(description = "If the video recorder should save out an AVI file",
	         example = "This is placeholder example text")
	private final BooleanInput saveVideo;

	@Keyword(description = "The size of the video/image in pixels",
	         example = "This is placeholder example text")
	private final IntegerListInput captureArea;

	@Keyword(description = "The background color to use for video recording",
	         example = "This is placeholder example text")
	private final ColourInput videoBGColor;

	@Keyword(description = "The list of views to draw in the video",
	         example = "This is placeholder example text")
	private final EntityListInput<View> captureViews;

	@Keyword(description = "The name of the video file to record",
	         example = "This is placeholder example text")
	private final StringInput videoName;

	@Keyword(description = "Enable video capture",
	         example = "VidRecorder VideoCapture { TRUE }")
	private final BooleanInput videoCapture;

	private boolean hasRunStartup;
	private int numFramesWritten;
	private final EventHandle captureHandle = new EventHandle();

	{
		captureStartTime = new ValueInput("CaptureStartTime", "Key Inputs", 0.0d);
		captureStartTime.setUnitType(TimeUnit.class);
		captureStartTime.setValidRange(0, Double.POSITIVE_INFINITY);
		this.addInput(captureStartTime);

		captureInterval = new ValueInput("CaptureInterval", "Key Inputs", 3600.0d);
		captureInterval.setUnitType(TimeUnit.class);
		captureInterval.setValidRange(0.1d, Double.POSITIVE_INFINITY);
		this.addInput(captureInterval);

		videoBGColor = new ColourInput("VideoBackgroundColor", "Key Inputs", ColourInput.WHITE);
		this.addInput(videoBGColor);
		this.addSynonym(videoBGColor, "Colour");

		captureFrames = new IntegerInput("CaptureFrames", "Key Inputs", 0);
		captureFrames.setValidRange(0, 30000);
		this.addInput(captureFrames);

		saveImages = new BooleanInput("SaveImages", "Key Inputs", false);
		this.addInput(saveImages);

		saveVideo = new BooleanInput("SaveVideo", "Key Inputs", false);
		this.addInput(saveVideo);

		IntegerVector defArea = new IntegerVector(2);
		defArea.add(1000);
		defArea.add(1000);
		captureArea = new IntegerListInput("CaptureArea", "Key Inputs", defArea);
		captureArea.setValidCount(2);
		captureArea.setValidRange(0, 3000);
		this.addInput(captureArea);

		captureViews = new EntityListInput<View>(View.class, "CaptureViews", "Key Inputs", new ArrayList<View>(0));
		this.addInput(captureViews);

		videoName = new StringInput("VideoName", "Key Inputs", "");
		this.addInput(videoName);

		videoCapture = new BooleanInput("VideoCapture", "Key Inputs", false);
		this.addInput(videoCapture);
	}

	@Override
	public void earlyInit() {
		super.earlyInit();

		hasRunStartup = false;
		numFramesWritten = 0;
	}

	@Override
	public void startUp() {
		super.startUp();

		if (videoCapture.getValue())
			startProcess(new CaptureNetworkTarget(this));

		this.hasRunStartup = true;
	}

	@Override
    public void updateForInput(Input<?> in) {
		super.updateForInput(in);

		if (in == videoCapture) {
			// Start the capture if we are already running and we set the input
			// to true
			if (hasRunStartup && videoCapture.getValue())
				this.scheduleProcess(new CaptureNetworkTarget(this));
		}
	}

	private static class CaptureNetworkTarget extends ProcessTarget {
		final VideoRecorderEntity rec;

		CaptureNetworkTarget(VideoRecorderEntity rec) {
			this.rec = rec;
		}

		@Override
		public String getDescription() {
			return rec.getInputName() + ".doCaptureNetwork";
		}

		@Override
		public void process() {
			rec.doCaptureNetwork();
		}
	}
	/**
	 * Capture JPEG images of the screen at regular simulated intervals
	 */
	public void doCaptureNetwork() {

		// If the capture network is already in progress, then stop the previous network
		EventManager.killEvent(captureHandle);
		simWait(captureStartTime.getValue(), 10, captureHandle);

		if (!RenderManager.isGood()) {
			RenderManager.initialize(false);
		}

		int width = captureArea.getValue().get(0);
		int height = captureArea.getValue().get(1);

		ArrayList<View> views = captureViews.getValue();

		String videoFileName = String.format("%s_%s", InputAgent.getRunName(), videoName.getValue());

		VideoRecorder recorder = new VideoRecorder(views, videoFileName, width, height, captureFrames.getDefaultValue(),
		                             saveImages.getValue(), saveVideo.getValue(), videoBGColor.getValue());

		// Otherwise, start capturing
		while (videoCapture.getValue()) {

			RenderManager.inst().blockOnScreenShot(recorder);
			++numFramesWritten;

			if (numFramesWritten == captureFrames.getValue()) {
				break;
			}

			// Wait until the next time to capture a frame
			// (priority 10 is used to allow higher priority events to complete first)
			simWait(captureInterval.getValue(), 10, captureHandle);
		}

		recorder.freeResources();
		recorder = null;
	}

}
