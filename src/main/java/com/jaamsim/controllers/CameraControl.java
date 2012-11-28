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

package com.jaamsim.controllers;

import com.jaamsim.input.WindowInteractionListener;
import com.jaamsim.math.Plane;
import com.jaamsim.math.Quaternion;
import com.jaamsim.math.Ray;
import com.jaamsim.math.Transform;
import com.jaamsim.math.Vector4d;
import com.jaamsim.render.CameraInfo;
import com.jaamsim.render.RenderUtils;
import com.jaamsim.render.RenderWindow;
import com.jaamsim.render.Renderer;
import com.jaamsim.ui.FrameBox;
import com.jaamsim.ui.View;
import com.jogamp.newt.event.MouseEvent;
import com.sandwell.JavaSimulation.ChangeWatcher;

public class CameraControl implements WindowInteractionListener {

	private static final double ZOOM_FACTOR = 1.1;
	// Scale from pixels dragged to radians rotated
	private static final double ROT_SCALE_X = 0.005;
	private static final double ROT_SCALE_Z = 0.005;


	private final Object dataLock = new Object();
	// The following 4 variables deterministically determine the camera position
	private double _rotZ; // The spherical coordinate that rotates around Z (in radians)
	private double _rotX; // Ditto for X
	private double _radius; // The distance the camera is from the view center
	private Vector4d _viewCenter;

	private Renderer _renderer;
	private RenderWindow _window;
	private View _updateView;

	private int _windowPosSetsToIgnore = 4;

	private ChangeWatcher.Tracker _viewTracker;


	public CameraControl(Renderer renderer, View updateView) {
		_renderer = renderer;
		_viewCenter = new Vector4d();
		_rotZ = 0;
		_rotX = 0;
		_radius = 10;
		_updateView = updateView;

		_viewTracker = _updateView.getChangeTracker();
	}

	@Override
	public void mouseDragged(WindowInteractionListener.DragInfo dragInfo) {

		synchronized(dataLock) {
			// Give the RenderManager first crack at this
			if (RenderManager.inst().handleDrag(dragInfo)) {
				RenderManager.inst().queueRedraw();
				return; // Handled
			}

			if (dragInfo.controlDown()) {
				return;
			}

			if (dragInfo.shiftDown()) {
				// handle rotation
				handleRotation(dragInfo.x, dragInfo.y, dragInfo.dx, dragInfo.dy, dragInfo.button);
			} else {
				handlePan(dragInfo.x, dragInfo.y, dragInfo.dx, dragInfo.dy, dragInfo.button);
			}

			updateCamTrans(true);
		}
	}

	private void handleRotation(int x, int y, int dx, int dy,
	                            int button) {

		_rotZ -= dx * ROT_SCALE_Z;
		_rotX -= dy * ROT_SCALE_X;

		if (_rotX < 0) _rotX = 0;
		if (_rotX > Math.PI) _rotX = Math.PI;

		if (_rotZ < 0) _rotZ += 2*Math.PI;
		if (_rotZ > 2*Math.PI) _rotZ -= 2*Math.PI;
	}

	private void handlePan(int x, int y, int dx, int dy,
	                            int button) {

		Renderer.WindowMouseInfo info = _renderer.getMouseInfo(_window.getWindowID());
		if (info == null) return;

		//Cast a ray into the XY plane both for now, and for the previous mouse position
		Ray currRay = RenderUtils.getPickRayForPosition(info.cameraInfo, x, y, info.width, info.height);
		Ray prevRay = RenderUtils.getPickRayForPosition(info.cameraInfo, x - dx, y - dy, info.width, info.height);

		double currDist = Plane.XY_PLANE.collisionDist(currRay);
		double prevDist = Plane.XY_PLANE.collisionDist(prevRay);
		if (currDist < 0 || prevDist < 0 ||
		    currDist == Double.POSITIVE_INFINITY ||
		    prevDist == Double.POSITIVE_INFINITY)
		{
			// We're either parallel to or beneath the XY plane, bail out
			return;
		}

		Vector4d currIntersect = currRay.getPointAtDist(currDist);
		Vector4d prevIntersect = prevRay.getPointAtDist(prevDist);

		Vector4d diff = new Vector4d();
		currIntersect.sub3(prevIntersect, diff);

		_viewCenter.subLocal3(diff);

	}

	@Override
	public void mouseWheelMoved(int windowID, int x, int y, int wheelRotation) {

		synchronized (dataLock) {
			int rot = wheelRotation;

			if (rot > 0) {
				for (int i = 0; i < rot; ++i) {
					_radius = _radius / ZOOM_FACTOR;
				}
			} else
			{
				rot *= -1;
				for (int i = 0; i < rot; ++i) {
					_radius = _radius * ZOOM_FACTOR;
				}
			}

			updateCamTrans(true);
		}
	}

	@Override
	public void mouseClicked(int windowID, int x, int y, int button, int modifiers) {
		if (!RenderManager.isGood()) { return; }

		RenderManager.inst().hideExistingPopups();
		if (button  == 3) {
			// Hand this off to the RenderManager to deal with
			RenderManager.inst().popupMenu(windowID);
		}
		if (button == 1 && (modifiers & WindowInteractionListener.MOD_CTRL) == 0) {
			RenderManager.inst().handleSelection(windowID);
		}
	}

	@Override
	public void mouseMoved(int windowID, int x, int y) {
		if (!RenderManager.isGood()) { return; }

		RenderManager.inst().mouseMoved(windowID, x, y);
	}

	@Override
	public void rawMouseEvent(MouseEvent me) {
	}

	@Override
	public void mouseEntry(int windowID, int x, int y, boolean isInWindow) {
		if (!RenderManager.isGood()) { return; }

		if (isInWindow && RenderManager.inst().isDragAndDropping()) {
			RenderManager.inst().createDNDObject(windowID, x, y);
		}
	}

	private void updateCamTrans(boolean updateInputs) {
		// It is possible this gets called before the window is properly set, so just ignore the first call.
		if (_window == null)
			return;

		Vector4d zOffset = new Vector4d(0, 0, _radius);

		Vector4d pos = new Vector4d(_viewCenter);
		Vector4d negCenter = new Vector4d(_viewCenter);
		negCenter.scaleLocal3(-1);

		pos.addLocal3(zOffset);

		Quaternion rot = Quaternion.Rotation(_rotZ, Vector4d.Z_AXIS);
		rot.mult(Quaternion.Rotation(_rotX, Vector4d.X_AXIS), rot);

		Transform finalTrans = new Transform();

		// These 3 represent a rotation around the view center (translate, rotate, negative translate)
		finalTrans.merge(new Transform(_viewCenter), finalTrans);
		finalTrans.merge(new Transform(Vector4d.ORIGIN, rot, 1), finalTrans);
		finalTrans.merge(new Transform(negCenter), finalTrans);

		// And set the position
		finalTrans.merge(new Transform(pos), finalTrans);

		CameraInfo info = _renderer.getCameraInfo(_window.getWindowID());

		info.trans = finalTrans;

		// HACK, manually set the near and far planes to keep the XY plane in view. This will be a bad thing when we go real 3D
		info.nearDist = _radius * 0.1;
		info.farDist = _radius * 10;

		_renderer.setCameraInfoForWindow(_window.getWindowID(), info);

		if (updateInputs) {
			updateViewPos(info.trans.getTransRef(), _viewCenter);
		}

		// Queue a redraw
		RenderManager.inst().queueRedraw();
	}

	public void setRotationAngles(double rotX, double rotZ) {
		synchronized(dataLock) {
			_rotX = rotX;
			_rotZ = rotZ;
			updateCamTrans(true);
		}
	}

	@Override
	public void setWindow(RenderWindow wind) {
		synchronized(dataLock) {
			_window = wind;

			updateCamTrans(false);
		}
	}

	@Override
	public void windowClosing() {
		if (!RenderManager.isGood()) { return; }

		RenderManager.inst().hideExistingPopups();
		RenderManager.inst().windowClosed(_window.getWindowID());
	}

	@Override
	public void mouseButtonDown(int windowID, int x, int y, int button, boolean isDown, int modifiers) {
		if (!RenderManager.isGood()) { return; }

		RenderManager.inst().handleMouseButton(windowID, x, y, button, isDown, modifiers);
	}

	@Override
	public void windowGainedFocus() {
		if (!RenderManager.isGood()) { return; }

		RenderManager.inst().setActiveWindow(_window.getWindowID());
	}

	/**
	 * Set the position information in the saved view to match this window
	 */
	private void updateViewPos(Vector4d viewPos, Vector4d viewCenter) {
		if (_updateView == null) {
			return;
		}

		_updateView.updateCenterAndPos(viewCenter, viewPos);

		FrameBox.valueUpdate();
	}

	@Override
	public void windowMoved(int x, int y, int width, int height)
	{
		// HACK!
		// Ignore the first 4 sets as these are spurious from the windowing system and we don't want to dirty
		// the simulation state. This should die when we have better input change detection
		if (_windowPosSetsToIgnore > 0) {
			_windowPosSetsToIgnore--;
			return;
		}

		// Filter out large negative values occuring from window minimize:
		if (x < -30000 || y < - 30000)
			return;

		_updateView.setWindowPos(x, y, width, height);
	}

	public View getView() {
		return _updateView;
	}

	public void checkForUpdate() {
		if (!_viewTracker.checkAndClear()) {
			return;
		}

		synchronized(dataLock) {

			_viewCenter =_updateView.getGlobalCenter();

			Vector4d camPos = _updateView.getGlobalPosition();

			Vector4d viewDiff = new Vector4d();
			camPos.sub3(_viewCenter, viewDiff);

			_radius = viewDiff.mag3();

			_rotZ = Math.atan2(viewDiff.x(), -viewDiff.y());

			double xyDist = Math.hypot(viewDiff.x(), viewDiff.y());

			_rotX = Math.atan2(xyDist, viewDiff.z());

			// If we are near vertical (within about a quarter of a degree) don't rotate around Z (take X as up)
			if (Math.abs(_rotX) < 0.005) {
				_rotZ = 0;
			}

			updateCamTrans(false);

		}

	}
}