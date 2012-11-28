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
package com.jaamsim.observers;

import java.util.ArrayList;

import com.jaamsim.math.Color4d;
import com.jaamsim.math.Transform;
import com.jaamsim.math.Vector4d;
import com.jaamsim.render.RenderProxy;
import com.jaamsim.render.StringProxy;
import com.jaamsim.render.TessFontKey;
import com.sandwell.JavaSimulation.ChangeWatcher;
import com.sandwell.JavaSimulation.Entity;
import com.sandwell.JavaSimulation3D.TextLabel;

public class TextObserver extends RenderObserver {

	private TextLabel _labelObservee;
	private ChangeWatcher.Tracker _observeeTracker;

	private StringProxy cachedProxy = null;
	private StringProxy cachedShadow = null;

	TextObserver(Entity observee) {
		super(observee);
		try {
			_labelObservee = (TextLabel)observee;
			_observeeTracker = _labelObservee.getGraphicsChangeTracker();
		} catch (ClassCastException e) {
			// The observee is not a display entity
			_labelObservee = null;
			// Debug assert, not actually an error
			assert(false);
		}

	}

	@Override
	public void collectProxies(ArrayList<RenderProxy> out) {
		if (_labelObservee == null || !_labelObservee.getShow()) {
			return;
		}

		if (!_observeeTracker.checkAndClear() &&
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

		double simTime = _labelObservee.getCurrentTime();
		Transform trans = _labelObservee.getGlobalTrans(simTime);

		String text = _labelObservee.getCachedText();
		Color4d textColour = _labelObservee.getFontColor();
		double height = _labelObservee.getTextHeight();
		if (trans == null) {
			return;
		}

		String fontName = _labelObservee.getFontName();
		int fontStyle = _labelObservee.getFontStyle();
		TessFontKey fontKey = new TessFontKey(fontName, fontStyle);

		cachedProxy = new StringProxy(text, fontKey, textColour, trans, height, _observee.getEntityNumber());

		if (_labelObservee.getDropShadow()) {
			Vector4d dsOffset = _labelObservee.getDropShadowOffset();
			dsOffset.scaleLocal3(height);
			Transform dsTrans = new Transform(trans);
			dsTrans.getTransRef().addLocal3(dsOffset);

			Color4d dsColor = _labelObservee.getDropShadowColor();

			cachedShadow = new StringProxy(text, fontKey, dsColor, dsTrans, height, _observee.getEntityNumber());
			out.add(cachedShadow);
		} else {
			cachedShadow = null;
		}

		out.add( cachedProxy );

	}

}
