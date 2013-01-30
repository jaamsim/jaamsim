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
package com.jaamsim.DisplayModels;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import com.jaamsim.render.DisplayModelBinding;
import com.jaamsim.render.OverlayTextureProxy;
import com.jaamsim.render.RenderProxy;
import com.sandwell.JavaSimulation.ChangeWatcher;
import com.sandwell.JavaSimulation.Entity;
import com.sandwell.JavaSimulation.ErrorException;
import com.sandwell.JavaSimulation.IntegerVector;
import com.sandwell.JavaSimulation.Util;
import com.sandwell.JavaSimulation3D.OverlayImage;

public class OverlayImageModel extends DisplayModel {
	public DisplayModelBinding getBinding(Entity ent) {
		return new Binding(ent, this);
	}

	@Override
	public boolean canDisplayEntity(Entity ent) {
		return ent instanceof OverlayImage;
	}

	private class Binding extends DisplayModelBinding {

		private OverlayImage imageObservee;
		private ChangeWatcher.Tracker observeeTracker;

		private OverlayTextureProxy cachedProxy = null;

		public Binding(Entity ent, DisplayModel dm) {
			super(ent, dm);
			try {
				imageObservee = (OverlayImage)observee;
				if (imageObservee != null) {
					observeeTracker = imageObservee.getGraphicsChangeTracker();
				}
			} catch (ClassCastException e) {
				// The observee is not a display entity
				imageObservee = null;
			}
		}

		@Override
		public void collectProxies(ArrayList<RenderProxy> out) {
			if (imageObservee == null || !imageObservee.getShow()) {
				return;
			}

			if (!observeeTracker.checkAndClear() &&
			    cachedProxy != null) {
				// Nothing changed

				out.add(cachedProxy);
				++_cacheHits;
				return;
			}

			++_cacheMisses;
			registerCacheMiss("OverlayImage");

			String filename = imageObservee.getFileName();
			IntegerVector pos = imageObservee.getScreenPos();
			IntegerVector size = imageObservee.getImageSize();

			boolean alignRight = imageObservee.getAlignRight();
			boolean alignBottom = imageObservee.getAlignBottom();
			boolean transparent = imageObservee.getTransparent();

			try {
				cachedProxy = new OverlayTextureProxy(pos.get(0), pos.get(1), size.get(0), size.get(1),
				                                      new URL(Util.getAbsoluteFilePath(filename)),
				                                      transparent, false,
				                                      alignRight, alignBottom, getVisibilityInfo());

				out.add(cachedProxy);
			} catch (MalformedURLException ex) {
				cachedProxy = null;
			} catch (ErrorException ex) {
				cachedProxy = null;
			}
		}

		@Override
		protected void collectSelectionBox(ArrayList<RenderProxy> out) {
			// No selection widgets for now
		}
	}
}
