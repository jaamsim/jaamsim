package com.jaamsim.observers;

import java.util.ArrayList;

import com.jaamsim.math.Color4d;
import com.jaamsim.render.OverlayStringProxy;
import com.jaamsim.render.RenderProxy;
import com.jaamsim.render.TessFontKey;
import com.jaamsim.ui.View;
import com.sandwell.JavaSimulation.ChangeWatcher;
import com.sandwell.JavaSimulation.Entity;
import com.sandwell.JavaSimulation.IntegerVector;
import com.sandwell.JavaSimulation3D.OverlayTextLabel;

public class OverlayTextObserver extends RenderObserver {
	private OverlayTextLabel _labelObservee;
	private ChangeWatcher.Tracker _observeeTracker;

	private OverlayStringProxy cachedProxy = null;

	OverlayTextObserver(Entity observee) {
		super(observee);
		try {
			_labelObservee = (OverlayTextLabel)observee;
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

			out.add(cachedProxy);
			++_cacheHits;
			return;
		}

		++_cacheMisses;

		Color4d color = (Color4d)_labelObservee.getInput("FontColour").getValue();
		String text = (String)_labelObservee.getInput("Text").getValue();
		IntegerVector pos = (IntegerVector)_labelObservee.getInput("ScreenPosition").getValue();
		int height = (Integer)_labelObservee.getInput("TextHeight").getValue();
		String fontName = _labelObservee.getFontName();
		int style = _labelObservee.getFontStyle();

		boolean alignRight = (Boolean)_labelObservee.getInput("AlignRight").getValue();
		boolean alignBottom = (Boolean)_labelObservee.getInput("AlignBottom").getValue();

		TessFontKey fk = new TessFontKey(fontName, style);

		ArrayList<View> visibleViews = _labelObservee.getVisibleViews();

		ArrayList<Integer> visibleWindowIDs = new ArrayList<Integer>(visibleViews.size());
		for (View v : visibleViews) {
			visibleWindowIDs.add(v.getID());
		}

		cachedProxy = new OverlayStringProxy(text, fk, color, height, pos.get(0), pos.get(1),
		                                     alignRight, alignBottom, visibleWindowIDs);
		out.add(cachedProxy);
	}

	@Override
	protected void collectSelectionBox(ArrayList<RenderProxy> out) {
		// No selection widgets for now
	}

}
