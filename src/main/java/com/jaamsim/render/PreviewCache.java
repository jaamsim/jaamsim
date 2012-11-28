package com.jaamsim.render;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;

import com.jaamsim.controllers.RenderManager;
import com.jaamsim.math.Quaternion;
import com.jaamsim.math.Transform;
import com.jaamsim.math.Vector4d;
import com.jaamsim.observers.DisplayModelObserver;
import com.jaamsim.observers.DisplayModelState;
import com.jaamsim.ui.View;
import com.sandwell.JavaSimulation3D.DisplayEntity;

public class PreviewCache {

	private HashMap<DisplayModelState, Future<BufferedImage>> _imageCache;

	public PreviewCache() {
		_imageCache = new HashMap<DisplayModelState, Future<BufferedImage>>();
	}

	public void clear() {
		_imageCache.clear();
	}

	/**
	 * Get the preview image for this, optionally a Runnable can be passed in that will be run when this image is ready
	 * @param dms
	 * @param notifier
	 * @return
	 */
	public Future<BufferedImage> getPreview(DisplayModelState dms, Runnable notifier) {
		synchronized (_imageCache) {

			Future<BufferedImage> cached = _imageCache.get(dms);
			if (cached != null) {
				return cached;
			}

			// Otherwise we need to load it....

			ArrayList<RenderProxy> proxies = new ArrayList<RenderProxy>();
			DisplayEntity.TagSet tags = new DisplayEntity.TagSet();
			Vector4d scale = new Vector4d(1, 1, 1);
			Transform trans = new Transform();

			// Collect the render proxies for a dummy version of this DisplayModel,
			// This will all need to be refactored soonish.
			DisplayModelObserver.collectProxiesForDisplayModel(dms, tags, trans, scale,
	                                                           0L, proxies);

			Transform camTrans;
			if (!dms.isFlatModel()) {
				// If this model is 3D, switch to an isometric view
				Quaternion cameraRot = new Quaternion();
				Quaternion.Rotation(Math.PI/2, Vector4d.X_AXIS).mult(cameraRot, cameraRot);
				Quaternion.Rotation(3*Math.PI/4, Vector4d.Z_AXIS).mult(cameraRot, cameraRot);
				Quaternion.Rotation(Math.PI/5, new Vector4d(1, -1, 0)).mult(cameraRot, cameraRot);
				camTrans = new Transform(new Vector4d(1.2, 1.2, 1.2), cameraRot, 1);
			} else {
				camTrans = new Transform(new Vector4d(0, 0, 1.2));
			}

			CameraInfo camInfo = new CameraInfo(Math.PI/3, 1, 10, camTrans);

			Future<BufferedImage> fi = RenderManager.inst().renderOffscreen(proxies, camInfo, View.NO_VIEW_ID, 180, 180, notifier);

			_imageCache.put(dms, fi);

			return fi;
		}
	}
}
