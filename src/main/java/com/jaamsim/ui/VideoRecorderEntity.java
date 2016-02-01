/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2013 Ausenco Engineering Canada Inc.
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

import java.util.ArrayList;

import javax.swing.JOptionPane;

import com.jaamsim.Graphics.DisplayEntity;
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
import com.jaamsim.input.InputErrorException;
import com.jaamsim.input.IntegerInput;
import com.jaamsim.input.IntegerListInput;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.StringInput;
import com.jaamsim.input.ValueInput;
import com.jaamsim.units.TimeUnit;

public class VideoRecorderEntity extends DisplayEntity {
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

		captureViews = new EntityListInput<>(View.class, "CaptureViews", "Key Inputs", new ArrayList<View>(0));
		this.addInput(captureViews);

		videoName = new StringInput("VideoName", "Key Inputs", "");
		this.addInput(videoName);
	}

	@Override
	public void validate() {
		super.validate();

		if( ( saveImages.getValue() || saveVideo.getValue() ) && captureViews.getValue().size() == 0 )
			throw new InputErrorException( "CaptureViews must be set when SaveImages or SaveVideo is TRUE" );
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

		if (saveVideo.getValue() || saveImages.getValue())
			startProcess(new CaptureNetworkTarget(this));

		this.hasRunStartup = true;
	}

	@Override
    public void updateForInput(Input<?> in) {
		super.updateForInput(in);

		if (in == saveVideo) {
			// Start the capture if we are already running and we set the input
			// to true
			if (hasRunStartup && saveVideo.getValue())
				EventManager.scheduleTicks(0, 10, false, new CaptureNetworkTarget(this), null);
		}
	}

	private static class CaptureNetworkTarget extends ProcessTarget {
		final VideoRecorderEntity rec;

		CaptureNetworkTarget(VideoRecorderEntity rec) {
			this.rec = rec;
		}

		@Override
		public String getDescription() {
			return rec.getName() + ".doCaptureNetwork";
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

		if (!RenderManager.canRenderOffscreen()) {
			JOptionPane.showMessageDialog(null, "Your hardware does not support Video Recording.");
			return;
		}

		int width = captureArea.getValue().get(0);
		int height = captureArea.getValue().get(1);

		ArrayList<View> views = captureViews.getValue();

		String videoFileName = String.format("%s_%s", InputAgent.getRunName(), videoName.getValue());

		VideoRecorder recorder = new VideoRecorder(views, videoFileName, width, height, captureFrames.getDefaultValue(),
		                             saveImages.getValue(), saveVideo.getValue(), videoBGColor.getValue());

		// Otherwise, start capturing
		while (saveVideo.getValue() || saveImages.getValue()) {

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
