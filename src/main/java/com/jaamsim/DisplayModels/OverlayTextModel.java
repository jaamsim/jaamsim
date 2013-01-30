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
import com.jaamsim.render.DisplayModelBinding;
import com.jaamsim.render.OverlayStringProxy;
import com.jaamsim.render.RenderProxy;
import com.jaamsim.render.TessFontKey;
import com.sandwell.JavaSimulation.ChangeWatcher;
import com.sandwell.JavaSimulation.Entity;
import com.sandwell.JavaSimulation.IntegerVector;
import com.sandwell.JavaSimulation3D.OverlayTextLabel;

public class OverlayTextModel extends DisplayModel {
	public DisplayModelBinding getBinding(Entity ent) {
		return new Binding(ent, this);
	}

	@Override
	public boolean canDisplayEntity(Entity ent) {
		return ent instanceof OverlayTextLabel;
	}

	private class Binding extends DisplayModelBinding {

		private OverlayTextLabel labelObservee;
		private ChangeWatcher.Tracker observeeTracker;

		private String cachedText = "";

		private OverlayStringProxy cachedProxy = null;
		private OverlayStringProxy cachedShadow = null;


		public Binding(Entity ent, DisplayModel dm) {
			super(ent, dm);
			try {
				labelObservee = (OverlayTextLabel)ent;
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

			// We are explicitly caching the text because the fun that is property labels do not have a good place to
			// hook a dirty call. Checking the string every render shouldn't be TOO much of a performance hit (although with
			// property labels that's a big guess)

			double time = labelObservee.getCurrentTime();
			String text = labelObservee.getText(time);

			if (!observeeTracker.checkAndClear() &&
			    cachedProxy != null &&
			    text.equals(cachedText)) {
				// Nothing changed

				if (cachedShadow != null) {
					out.add(cachedShadow);
				}
				out.add(cachedProxy);
				++_cacheHits;
				return;
			}

			cachedText = text;

			++_cacheMisses;
			registerCacheMiss("OverlayText");

			Color4d color = (Color4d)labelObservee.getInput("FontColour").getValue();
			IntegerVector pos = (IntegerVector)labelObservee.getInput("ScreenPosition").getValue();
			int height = (Integer)labelObservee.getInput("TextHeight").getValue();
			String fontName = labelObservee.getFontName();
			int style = labelObservee.getFontStyle();

			boolean alignRight = (Boolean)labelObservee.getInput("AlignRight").getValue();
			boolean alignBottom = (Boolean)labelObservee.getInput("AlignBottom").getValue();

			TessFontKey fk = new TessFontKey(fontName, style);

			boolean dropShadow = (Boolean)labelObservee.getInput("DropShadow").getValue();
			if (dropShadow) {
				Color4d dsColor = (Color4d)labelObservee.getInput("DropShadowColour").getValue();
				IntegerVector dsOffset = (IntegerVector)labelObservee.getInput("DropShadowOffset").getValue();

				cachedShadow = new OverlayStringProxy(text, fk, dsColor, height,
				                                      pos.get(0) + (dsOffset.get(0) * (alignRight ? -1 : 1)),
				                                      pos.get(1) + (dsOffset.get(1) * (alignBottom ? -1 : 1)),
				                                      alignRight, alignBottom, getVisibilityInfo());
				out.add(cachedShadow);
			} else {
				cachedShadow = null;
			}

			cachedProxy = new OverlayStringProxy(text, fk, color, height, pos.get(0), pos.get(1),
			                                     alignRight, alignBottom, getVisibilityInfo());
			out.add(cachedProxy);
		}

		@Override
		protected void collectSelectionBox(ArrayList<RenderProxy> out) {
			// No selection widgets for now
		}


	}
}
