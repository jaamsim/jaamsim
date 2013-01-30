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

import java.util.ArrayList;

import com.jaamsim.math.Color4d;
import com.jaamsim.math.Transform;
import com.jaamsim.math.Vec3d;
import com.jaamsim.render.DisplayModelBinding;
import com.jaamsim.render.RenderProxy;
import com.jaamsim.render.StringProxy;
import com.jaamsim.render.TessFontKey;
import com.sandwell.JavaSimulation.ChangeWatcher;
import com.sandwell.JavaSimulation.Entity;
import com.sandwell.JavaSimulation3D.TextLabel;

public class TextModel extends DisplayModel {

	@Override
	public DisplayModelBinding getBinding(Entity ent) {
		return new Binding(ent, this);
	}

	@Override
	public boolean canDisplayEntity(Entity ent) {
		return ent instanceof TextLabel;
	}

	private class Binding extends DisplayModelBinding {

		private TextLabel labelObservee;
		private ChangeWatcher.Tracker observeeTracker;

		private StringProxy cachedProxy = null;
		private StringProxy cachedShadow = null;

		public Binding(Entity ent, DisplayModel dm) {
			super(ent, dm);
			try {
				labelObservee = (TextLabel)ent;
				if (labelObservee != null) {
					observeeTracker = labelObservee.getGraphicsChangeTracker();
				}
			} catch (ClassCastException e) {
				// The observee is not a display entity
				labelObservee = null;
			}
		}

		@Override
		public void collectProxies(ArrayList<RenderProxy> out) {
			if (labelObservee == null || !labelObservee.getShow()) {
				return;
			}

			if (observeeTracker != null && !observeeTracker.checkAndClear() &&
			    cachedProxy != null) {
				// Nothing changed
				if (cachedShadow != null) {
					out.add(cachedShadow);
				}
				out.add(cachedProxy);
				++_cacheHits;
				return;
			}

			++_cacheMisses;
			registerCacheMiss("TextLabel");

			double simTime = labelObservee.getCurrentTime();
			Transform trans = labelObservee.getGlobalTrans(simTime);

			String text = labelObservee.getCachedText();
			Color4d textColour = labelObservee.getFontColor();
			double height = labelObservee.getTextHeight();
			if (trans == null) {
				return;
			}

			String fontName = labelObservee.getFontName();
			int fontStyle = labelObservee.getFontStyle();
			TessFontKey fontKey = new TessFontKey(fontName, fontStyle);

			cachedProxy = new StringProxy(text, fontKey, textColour, trans, height, getVisibilityInfo(), labelObservee.getEntityNumber());

			if (labelObservee.getDropShadow()) {
				Vec3d dsOffset = labelObservee.getDropShadowOffset();
				dsOffset.scale3(height);
				Transform dsTrans = new Transform(trans);
				Vec3d shadowTrans = new Vec3d(dsOffset);
				shadowTrans.add3(dsTrans.getTransRef());
				dsTrans.setTrans(shadowTrans);

				Color4d dsColor = labelObservee.getDropShadowColor();

				cachedShadow = new StringProxy(text, fontKey, dsColor, dsTrans, height, getVisibilityInfo(), labelObservee.getEntityNumber());
				out.add(cachedShadow);
			} else {
				cachedShadow = null;
			}

			out.add( cachedProxy );
		}
	}
}
