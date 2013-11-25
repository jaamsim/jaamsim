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

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

import javax.imageio.ImageIO;

import com.jaamsim.DisplayModels.ColladaModel;
import com.jaamsim.DisplayModels.DisplayModel;
import com.jaamsim.DisplayModels.ImageModel;
import com.jaamsim.controllers.RenderManager;
import com.jaamsim.math.Quaternion;
import com.jaamsim.math.Transform;
import com.jaamsim.math.Vec3d;
import com.jaamsim.math.Vec4d;
import com.jaamsim.ui.View;
import com.sandwell.JavaSimulation3D.DisplayEntity;
import com.sandwell.JavaSimulation3D.DisplayModelCompat;
import com.sandwell.JavaSimulation3D.GUIFrame;

public class PreviewCache {

	private HashMap<DisplayModel, Future<BufferedImage>> _imageCache;

	private DisplayEntity dummyEntity;

	public PreviewCache() {
		_imageCache = new HashMap<DisplayModel, Future<BufferedImage>>();


		if (GUIFrame.instance().getSimState() != GUIFrame.SIM_STATE_RUNNING) {
			dummyEntity = new DisplayEntity();
			dummyEntity.kill();
		}
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

			// Fast path out for ImageModels
			if (dm instanceof ImageModel) {
				ImageModel im = (ImageModel)dm;
				URI file = im.getImageFile();
				Future<BufferedImage> ret = new Future<BufferedImage>(null);
				try {
					URL imageURL = file.toURL();
					BufferedImage image = ImageIO.read(imageURL);

					// For some weird reason, the resizing that may happen to this image fails silently
					// for other color types, so we'll just hard convert it here
					BufferedImage colored = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
					Graphics2D g2 = colored.createGraphics();
					g2.drawImage(image, new AffineTransform(), null);
					g2.dispose();

					ret.setComplete(colored);

				} catch (Exception ex) {
					ret.setFailed("Bad image");
				}
				_imageCache.put(dm, ret);
				return ret;
			}

			// Otherwise we need to load it....

			ArrayList<RenderProxy> proxies = new ArrayList<RenderProxy>();

			// Collect the render proxies for a dummy version of this DisplayModel,
			// This will all need to be refactored soonish.

			if (dummyEntity == null) {
				if (GUIFrame.instance().getSimState() != GUIFrame.SIM_STATE_RUNNING) {
					dummyEntity = new DisplayEntity();
					dummyEntity.kill();
				} else {
					// The simulation is running so we can't make the dummy entity
					Future<BufferedImage> ret = new Future<BufferedImage>(null);
					ret.setFailed("Simulation running");
					return ret;
				}
			}

			if (dm == null || !dm.canDisplayEntity(dummyEntity)) {
				Future<BufferedImage> ret = new Future<BufferedImage>(null);
				ret.setFailed("Cannot render preview");
				return ret;
			}

			// For ColladaModels maintain the proportions of the imported model
			if (dm instanceof ColladaModel) {
				ColladaModel cm = (ColladaModel)dm;
				MeshProtoKey key = ColladaModel.getCachedMeshKey(cm.getColladaFile());
				Vec3d meshSize = new Vec3d(RenderManager.inst().getMeshBounds(key, true).radius);
				double maxDim = Math.max(Math.max(meshSize.x, meshSize.y), meshSize.z);
				meshSize.scale3(1/maxDim);
				dummyEntity.setSize(meshSize);
			}

			dm.getBinding(dummyEntity).collectProxies(0, proxies);

			boolean isFlat = true;
			if (dm instanceof DisplayModelCompat) {
				isFlat = true;
			}
			if (dm instanceof ColladaModel) {
				isFlat = false;
			}


			Transform camTrans = new Transform();
			if (!isFlat) {
				// If this model is 3D, switch to an isometric view
				Quaternion cameraRot = new Quaternion();
				Quaternion tmp = new Quaternion();

				tmp.setRotXAxis(Math.PI / 2);
				cameraRot.mult(tmp, cameraRot);

				tmp.setRotZAxis(3*Math.PI / 4);
				cameraRot.mult(tmp, cameraRot);

				tmp.setAxisAngle(new Vec3d(1.0d, -1.0d, 0.0d), Math.PI / 5);
				cameraRot.mult(tmp, cameraRot);

				camTrans = new Transform(new Vec4d(1.2, 1.2, 1.2, 1.0d), cameraRot, 1);
			} else {
				camTrans = new Transform(new Vec4d(0, 0, 1.2, 1.0d));
			}


			CameraInfo camInfo = new CameraInfo(Math.PI/3, 1, 10, camTrans, null);

			Future<BufferedImage> fi = RenderManager.inst().renderOffscreen(proxies, camInfo, View.NO_VIEW_ID, 180, 180, notifier);

			_imageCache.put(dm, fi);

			return fi;
		}
	}
}
