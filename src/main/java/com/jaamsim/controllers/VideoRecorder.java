/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2012 Ausenco Engineering Canada Inc.
 * Copyright (C) 2020 JaamSim Software Inc.
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
package com.jaamsim.controllers;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import javax.imageio.ImageIO;

import com.jaamsim.Graphics.View;
import com.jaamsim.datatypes.IntegerVector;
import com.jaamsim.math.Color4d;
import com.jaamsim.render.Future;
import com.jaamsim.render.OffscreenTarget;
import com.jaamsim.ui.GUIFrame;
import com.jaamsim.ui.LogBox;
import com.jaamsim.video.AviWriter;
import com.jaamsim.video.vp8.Encoder;

/**
 * The VideoRecorder class is used to generate a series of saved images (PNG only for the first implementation) from the renderer.
 * This allows the user to composite several views together. Once the recorder is created, calling sample() will
 * cause the renderer to draw the image and save it to disk. sample() blocks until the image has been written to disk.
 * @author matt.chudleigh
 *
 */
public class VideoRecorder {

	private static class ViewInfo {
		public int x;
		public int y;
		public int width;
		public int height;
		OffscreenTarget renderTarget;
		View view;
	}

	private ArrayList<ViewInfo> _views;
	private String _filenamePrefix;
	private int _width;
	private int _height;
	private int _sampleNumber = 0;

	private AviWriter _aviWriter;
	private Encoder _encoder;
	private boolean _isLoaded;

	private boolean _saveImages;
	private boolean _saveVideo;

	private Color4d _bgColor;

	public VideoRecorder(ArrayList<View> views, String filenamePrefix, int width, int height, int numFrames,
	                     boolean saveImages, boolean saveVideo, Color4d bgColor) {
		_filenamePrefix = filenamePrefix;
		_width = width;
		_height = height;
		_saveImages = saveImages;
		_saveVideo = saveVideo;

		_bgColor = bgColor;

		_views = new ArrayList<>(views.size());

		// Cache the view position information and build the offscreen render targets
		for (View v : views) {
			ViewInfo vi = new ViewInfo();

			IntegerVector windSize = GUIFrame.getInstance().getWindowSize(v);
			IntegerVector windPos = GUIFrame.getInstance().getWindowPos(v);

			vi.x = windPos.get(0);
			vi.y = windPos.get(1);
			vi.width = windSize.get(0);
			vi.height = windSize.get(1);

			vi.renderTarget = RenderManager.inst().createOffscreenTarget(vi.width, vi.height);

			vi.view = v;

			_views.add(vi);
		}

		if (_saveVideo) {
			String videoName = String.format("%s.avi", _filenamePrefix);
			_aviWriter = new AviWriter(videoName, width, height, numFrames);
			_encoder = new Encoder();
		}

		_isLoaded = true;

	}

	public void sample() {
		assert(_isLoaded);

		if (!_saveVideo && !_saveImages) {
			return; // Don't waste the time
		}

//		long start = System.nanoTime();

		ArrayList<Future<BufferedImage>> images = new ArrayList<>();
		for (ViewInfo vi : _views) {
			images.add(RenderManager.inst().renderScreenShot(vi.view, vi.width, vi.height, vi.renderTarget));
		}

		// Make sure all the renders are queued up before waiting for any of them.
		for (Future<BufferedImage> fi : images) {
			fi.blockUntilDone();
		}

//		long renders = System.nanoTime();

		// Now composite the images based on the views
		BufferedImage img = new BufferedImage(_width, _height, BufferedImage.TYPE_INT_RGB);
		Graphics2D g2 = img.createGraphics();

		g2.setColor(new Color((float)_bgColor.r, (float)_bgColor.g, (float)_bgColor.b));

		g2.fillRect(0, 0, _width, _height);

		for (int i = 0; i < images.size(); ++i) {
			ViewInfo vi = _views.get(i);
			boolean drawResult = g2.drawImage(images.get(i).get(), vi.x, vi.y, vi.width, vi.height, null);
			assert(drawResult == true);
		}

//		long composite = System.nanoTime();

		if (_saveVideo) {
			boolean keyFrame = (_sampleNumber % 100) == 0;
			ByteBuffer frame = _encoder.encodeFrame(img, keyFrame);
			_aviWriter.addFrame(frame, keyFrame);
		}

		if (_saveImages) {
			try {
				FileOutputStream out = new FileOutputStream(String.format("%s%04d.png", _filenamePrefix, _sampleNumber));

				// Finally write the image to disk
				ImageIO.write(img, "PNG", out);

				out.close();

			} catch (FileNotFoundException ex) {
				LogBox.renderLogException(ex);
			} catch (IOException ex) {
				LogBox.renderLogException(ex);
			}
		}
		_sampleNumber++;

//		long writeout = System.nanoTime();
//
//		double renderTimeMS = (renders - start) * 0.000001;
//		double compositeTimeMS = (composite - renders) * 0.000001;
//		double writeoutTimeMS = (writeout - composite) * 0.000001;
//
//		LogBox.formatRenderLog("Render: %f Composite: %f Writeout %f\n", renderTimeMS, compositeTimeMS, writeoutTimeMS);
	}

	public void freeResources() {

		if (_saveVideo) {
			_aviWriter.close();
		}

		if (!_isLoaded) {
			return;
		}

		for (ViewInfo vi : _views) {
			RenderManager.inst().freeOffscreenTarget(vi.renderTarget);
		}
		_isLoaded = false;
	}
}
