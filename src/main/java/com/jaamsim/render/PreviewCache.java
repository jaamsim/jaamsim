/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2012 Ausenco Engineering Canada Inc.
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
import com.jaamsim.DisplayModels.ShapeModel;
import com.jaamsim.Graphics.DisplayEntity;
import com.jaamsim.Graphics.View;
import com.jaamsim.basicsim.JaamSimModel;
import com.jaamsim.controllers.RenderManager;
import com.jaamsim.math.Quaternion;
import com.jaamsim.math.Transform;
import com.jaamsim.math.Vec3d;
import com.jaamsim.math.Vec4d;
import com.jaamsim.ui.GUIFrame;

public class PreviewCache {

	private HashMap<DisplayModel, Future<BufferedImage>> _imageCache;

	private DisplayEntity dummyEntity;

	public PreviewCache() {
		_imageCache = new HashMap<>();

		JaamSimModel simModel = GUIFrame.getJaamSimModel();
		if (simModel.getSimState() != JaamSimModel.SIM_STATE_RUNNING) {
			dummyEntity = simModel.createInstance(DisplayEntity.class);
			//FIXME: remove this when models are no longer static
			dummyEntity.setName("");
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
				Future<BufferedImage> ret = new Future<>(null);
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

			ArrayList<RenderProxy> proxies = new ArrayList<>();

			// Collect the render proxies for a dummy version of this DisplayModel,
			// This will all need to be refactored soonish.

			if (dummyEntity == null) {
				JaamSimModel simModel = GUIFrame.getJaamSimModel();
				if (simModel.getSimState() != JaamSimModel.SIM_STATE_RUNNING) {
					dummyEntity = simModel.createInstance(DisplayEntity.class);
					dummyEntity.kill();
				} else {
					// The simulation is running so we can't make the dummy entity
					Future<BufferedImage> ret = new Future<>(null);
					ret.setFailed("Simulation running");
					return ret;
				}
			}

			if (dm == null || !dm.canDisplayEntity(dummyEntity)) {
				Future<BufferedImage> ret = new Future<>(null);
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
			if (dm instanceof ShapeModel) {
				isFlat = true;
			}
			if (dm instanceof ColladaModel) {
				isFlat = false;
			}


			Transform camTrans;
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


			CameraInfo camInfo = new CameraInfo(Math.PI/3, camTrans, null);

			Future<BufferedImage> fi = RenderManager.inst().renderOffscreen(proxies, camInfo, View.OMNI_VIEW_ID, 180, 180, notifier);

			_imageCache.put(dm, fi);

			return fi;
		}
	}
}
