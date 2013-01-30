/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2012 Ausenco Engineering Canada Inc.
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
package com.jaamsim.render;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;

import com.jaamsim.DisplayModels.DisplayModel;
import com.jaamsim.controllers.RenderManager;
import com.jaamsim.math.Quaternion;
import com.jaamsim.math.Transform;
import com.jaamsim.math.Vec4d;
import com.jaamsim.ui.View;
import com.sandwell.JavaSimulation3D.DisplayModelCompat;

public class PreviewCache {

	private HashMap<DisplayModel, Future<BufferedImage>> _imageCache;

	public PreviewCache() {
		_imageCache = new HashMap<DisplayModel, Future<BufferedImage>>();
	}

	public void clear() {
		_imageCache.clear();
	}

	/**
	 * Get the preview image for this, optionally a Runnable can be passed in that will be run when this image is ready
	 * @param ot
	 * @param notifier
	 * @return
	 */
	public Future<BufferedImage> getPreview(DisplayModel dm, Runnable notifier) {
		synchronized (_imageCache) {

			Future<BufferedImage> cached = _imageCache.get(dm);
			if (cached != null) {
				return cached;
			}

			// Otherwise we need to load it....

			ArrayList<RenderProxy> proxies = new ArrayList<RenderProxy>();

			// Collect the render proxies for a dummy version of this DisplayModel,
			// This will all need to be refactored soonish.

			if (dm != null) {
				dm.getBinding(null).collectProxies(proxies);
			}

			boolean isFlat = true;
			if (dm instanceof DisplayModelCompat) {
				DisplayModelCompat dmc = (DisplayModelCompat)dm;
				String shapeString = dmc.getShape();
				String extension = shapeString.substring(shapeString.length() - 3, shapeString.length()).toUpperCase();
				if (extension.equals("DAE") || extension.equals("ZIP")) {
					isFlat = false;
				}
			}

			Transform camTrans = new Transform();
			if (!isFlat) {
				// If this model is 3D, switch to an isometric view
				Quaternion cameraRot = new Quaternion();
				Quaternion.Rotation(Math.PI/2, Vec4d.X_AXIS).mult(cameraRot, cameraRot);
				Quaternion.Rotation(3*Math.PI/4, Vec4d.Z_AXIS).mult(cameraRot, cameraRot);
				Quaternion.Rotation(Math.PI/5, new Vec4d(1, -1, 0, 1.0d)).mult(cameraRot, cameraRot);
				camTrans = new Transform(new Vec4d(1.2, 1.2, 1.2, 1.0d), cameraRot, 1);
			} else {
				camTrans = new Transform(new Vec4d(0, 0, 1.2, 1.0d));
			}


			CameraInfo camInfo = new CameraInfo(Math.PI/3, 1, 10, camTrans);

			Future<BufferedImage> fi = RenderManager.inst().renderOffscreen(proxies, camInfo, View.NO_VIEW_ID, 180, 180, notifier);

			_imageCache.put(dm, fi);

			return fi;
		}
	}
}
