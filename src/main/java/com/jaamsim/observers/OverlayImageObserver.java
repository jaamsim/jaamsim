package com.jaamsim.observers;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import com.jaamsim.render.OverlayTextureProxy;
import com.jaamsim.render.RenderProxy;
import com.jaamsim.ui.View;
import com.sandwell.JavaSimulation.ChangeWatcher;
import com.sandwell.JavaSimulation.Entity;
import com.sandwell.JavaSimulation.IntegerVector;
import com.sandwell.JavaSimulation.Util;
import com.sandwell.JavaSimulation3D.OverlayImage;

public class OverlayImageObserver extends RenderObserver {
	private OverlayImage _imageObservee;
	private ChangeWatcher.Tracker _observeeTracker;

	private OverlayTextureProxy cachedProxy = null;

	OverlayImageObserver(Entity observee) {
		super(observee);
		try {
			_imageObservee = (OverlayImage)observee;
			_observeeTracker = _imageObservee.getGraphicsChangeTracker();
		} catch (ClassCastException e) {
			// The observee is not a display entity
			_imageObservee = null;
			// Debug assert, not actually an error
			assert(false);
		}

	}

	@Override
	public void collectProxies(ArrayList<RenderProxy> out) {
		if (_imageObservee == null || !_imageObservee.getShow()) {
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

		String filename = (String)_imageObservee.getInput("File").getValue();
		IntegerVector pos = (IntegerVector)_imageObservee.getInput("ScreenPosition").getValue();
		IntegerVector size = (IntegerVector)_imageObservee.getInput("ImageSize").getValue();

		boolean alignRight = (Boolean)_imageObservee.getInput("AlignRight").getValue();
		boolean alignBottom = (Boolean)_imageObservee.getInput("AlignBottom").getValue();

		boolean transparent = (Boolean)_imageObservee.getInput("Transparent").getValue();

		ArrayList<View> visibleViews = _imageObservee.getVisibleViews();
		ArrayList<Integer> visibleWindowIDs = new ArrayList<Integer>(visibleViews.size());
		for (View v : visibleViews) {
			visibleWindowIDs.add(v.getID());
		}

		try {
			cachedProxy = new OverlayTextureProxy(pos.get(0), pos.get(1), size.get(0), size.get(1),
			                                      new URL(Util.getAbsoluteFilePath(filename)),
			                                      transparent, false,
			                                      alignRight, alignBottom, visibleWindowIDs);

			out.add(cachedProxy);
		} catch (MalformedURLException ex) {
			cachedProxy = null;
		}
	}

	@Override
	protected void collectSelectionBox(ArrayList<RenderProxy> out) {
		// No selection widgets for now
	}

}