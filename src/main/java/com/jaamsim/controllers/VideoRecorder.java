package com.jaamsim.controllers;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;

import com.jaamsim.math.Vector4d;
import com.jaamsim.render.Future;
import com.jaamsim.render.OffscreenTarget;
import com.jaamsim.ui.View;
import com.jaamsim.video.MediaFactory;
import com.jaamsim.video.MediaWriter;
import com.sandwell.JavaSimulation.IntegerVector;

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
		public int viewID;
		OffscreenTarget renderTarget;
		Vector4d camPos;
		Vector4d viewCenter;

	}

	private ArrayList<ViewInfo> _views;
	private String _filenamePrefix;
	private int _width;
	private int _height;
	private int _sampleNumber = 0;

	private MediaWriter _videoWriter;
	private long _writeTimeMS = 0;
	private static final long STEP_TIME = 30;

	private boolean _isLoaded;

	private boolean _saveImages;
	private boolean _saveVideo;

	public VideoRecorder(ArrayList<View> views, String filenamePrefix, int width, int height,
	                     boolean saveImages, boolean saveVideo) {
		_filenamePrefix = filenamePrefix;
		_width = width;
		_height = height;
		_saveImages = saveImages;
		_saveVideo = saveVideo;

		_views = new ArrayList<ViewInfo>(views.size());

		// Cache the view position information and build the offscreen render targets
		for (View v : views) {
			ViewInfo vi = new ViewInfo();

			IntegerVector windSize = (IntegerVector)v.getInput("WindowSize").getValue();
			IntegerVector windPos = (IntegerVector)v.getInput("WindowPosition").getValue();

			vi.x = windPos.get(0);
			vi.y = windPos.get(1);
			vi.width = windSize.get(0);
			vi.height = windSize.get(1);

			vi.renderTarget = RenderManager.inst().createOffscreenTarget(vi.width, vi.height);

			vi.camPos = v.getGlobalPosition();
			vi.viewCenter = v.getGlobalCenter();
			vi.viewID = v.getID();

			_views.add(vi);
		}

		// Search if the xuggle jar is included
		boolean xuggleFound = false;
		try {
			if (Class.forName("com.xuggle.mediatool.ToolFactory") != null) {
				xuggleFound = true;
			}
		}catch (ClassNotFoundException ex) {}

		if (!xuggleFound) {
			// TODO log error
			_saveVideo = false;
		}

		if (_saveVideo) {
			String videoName = String.format("%s.avi", _filenamePrefix);
			_videoWriter = MediaFactory.makeWriter(videoName);
			_videoWriter.addVideoStream(0, 0, width, height);
		}

		_isLoaded = true;

	}

	public void sample() {
		assert(_isLoaded);

		if (!_saveVideo && !_saveImages) {
			return; // Don't waste the time
		}

		//long start = System.nanoTime();

		ArrayList<Future<BufferedImage>> images = new ArrayList<Future<BufferedImage>>();
		for (ViewInfo vi : _views) {
			images.add(RenderManager.inst().renderScreenShot(vi.camPos, vi.viewID, vi.viewCenter, vi.width, vi.height, vi.renderTarget));
		}

		// Make sure all the renders are queued up before waiting for any of them.
		for (Future<BufferedImage> fi : images) {
			fi.blockUntilDone();
		}

		//long renders = System.nanoTime();

		// Now composite the images based on the views
		BufferedImage img = new BufferedImage(_width, _height, BufferedImage.TYPE_3BYTE_BGR);
		Graphics2D g2 = img.createGraphics();

		g2.setColor(Color.WHITE);
		g2.fillRect(0, 0, _width, _height);

		for (int i = 0; i < images.size(); ++i) {
			ViewInfo vi = _views.get(i);
			boolean drawResult = g2.drawImage(images.get(i).get(), vi.x, vi.y, vi.width, vi.height, null);
			assert(drawResult == true);
		}

		//long composite = System.nanoTime();

		if (_saveVideo) {
			_videoWriter.encodeVideo(0, img, _writeTimeMS, TimeUnit.MILLISECONDS);
		}
		_writeTimeMS += STEP_TIME;
		if (_saveImages) {
			try {
				FileOutputStream out = new FileOutputStream(String.format("%s%04d.png", _filenamePrefix, _sampleNumber++));

				// Finally write the image to disk
				ImageIO.write(img, "PNG", out);

				out.close();

			} catch (FileNotFoundException ex) {
				ex.printStackTrace();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}

		//long writeout = System.nanoTime();

		//double renderTimeMS = (renders - start) * 0.000001;
		//double compositeTimeMS = (composite - renders) * 0.000001;
		//double writeoutTimeMS = (writeout - composite) * 0.000001;

		//System.out.printf("Render: %f Composite: %f Writeout %f\n", renderTimeMS, compositeTimeMS, writeoutTimeMS);
	}

	public void freeResources() {

		if (_saveVideo) {
			_videoWriter.close();
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
