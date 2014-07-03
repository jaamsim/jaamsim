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

import com.jaamsim.math.Mat4d;
import com.jaamsim.math.MathUtils;
import com.jaamsim.math.Plane;
import com.jaamsim.math.Quaternion;
import com.jaamsim.math.Ray;
import com.jaamsim.math.Transform;
import com.jaamsim.math.Vec3d;
import com.jaamsim.math.Vec4d;
import com.jaamsim.render.CameraInfo;
import com.jaamsim.render.RenderUtils;
import com.jaamsim.render.Renderer;
import com.jaamsim.render.WindowInteractionListener;
import com.jaamsim.ui.FrameBox;
import com.jaamsim.ui.View;
import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.MouseEvent;

public class CameraControl implements WindowInteractionListener {

	private static final double ZOOM_FACTOR = 1.1;
	// Scale from pixels dragged to radians rotated
	private static final double ROT_SCALE_X = 0.005;
	private static final double ROT_SCALE_Z = 0.005;

	private Renderer _renderer;
	private int _windowID;
	private View _updateView;

	private int _windowPosSetsToIgnore = 4;

	private final Vec3d POI = new Vec3d();

	private static class PolarInfo {
		double rotZ; // The spherical coordinate that rotates around Z (in radians)
		double rotX; // Ditto for X
		double radius; // The distance the camera is from the view center
		final Vec3d viewCenter;

		PolarInfo(Vec3d center) {
			viewCenter = new Vec3d(center);
		}

		@Override
		public boolean equals(Object o) {
			if (!(o instanceof PolarInfo)) {
				return false;
			}
			PolarInfo pi = (PolarInfo)o;

			return pi.rotZ == rotZ && pi.rotX == rotX && pi.radius == radius &&
			       viewCenter.equals3(pi.viewCenter);
		}
	}

	private PolarInfo piCache; // The last polar info this view has re-drawn for

	public CameraControl(Renderer renderer, View updateView) {
		_renderer = renderer;
		_updateView = updateView;

		POI.set3(_updateView.getGlobalCenter());
	}

	@Override
	public void mouseDragged(WindowInteractionListener.DragInfo dragInfo) {

		// Give the RenderManager first crack at this
		if (RenderManager.inst().handleDrag(dragInfo)) {
			RenderManager.redraw();
			return; // Handled
		}

		if (!_updateView.isMovable() || _updateView.isScripted()) {
			return;
		}

		if (dragInfo.button == 1) {
			if (dragInfo.shiftDown()) {
				handleExpVertPan(dragInfo.x, dragInfo.y, dragInfo.dx, dragInfo.dy);
			} else {
				handleExpPan(dragInfo.x, dragInfo.y, dragInfo.dx, dragInfo.dy);
			}
		}
		else if (dragInfo.button == 3) {
			if (dragInfo.shiftDown()) {
				handleTurnCamera(dragInfo.dx, dragInfo.dy);
			} else {
				handleRotAroundPoint(dragInfo.x, dragInfo.y, dragInfo.dx, dragInfo.dy);
			}
		}
	}

	private void handleTurnCamera(int dx, int dy) {

		Vec3d camPos = _updateView.getGlobalPosition();
		Vec3d center = _updateView.getGlobalCenter();

		PolarInfo origPi = getPolarFrom(center, camPos);

		Quaternion origRot = polarToRot(origPi);
		Mat4d rot = new Mat4d();
		rot.setRot3(origRot);

		Vec3d rotXAxis = new Vec3d(1.0d, 0.0d, 0.0d);
		rotXAxis.mult3(rot, rotXAxis);

		Quaternion rotX = new Quaternion();
		rotX.setAxisAngle(rotXAxis, dy * ROT_SCALE_X / 4);

		Quaternion rotZ = new Quaternion();
		rotZ.setRotZAxis(dx * ROT_SCALE_Z / 4);


		Mat4d rotTransX = MathUtils.rotateAroundPoint(rotX, camPos);
		Mat4d rotTransZ = MathUtils.rotateAroundPoint(rotZ, camPos);

		center.multAndTrans3(rotTransX, center);
		center.multAndTrans3(rotTransZ, center);

		PolarInfo pi = getPolarFrom(center, camPos);
		updateCamTrans(pi, true);

	}

	private void handleExpPan(int x, int y, int dx, int dy) {

		Renderer.WindowMouseInfo info = _renderer.getMouseInfo(_windowID);
		if (info == null) return;

		//Cast a ray into the XY plane both for now, and for the previous mouse position
		Ray currRay = RenderUtils.getPickRayForPosition(info.cameraInfo, x, y, info.width, info.height);
		Ray prevRay = RenderUtils.getPickRayForPosition(info.cameraInfo, x - dx, y - dy, info.width, info.height);

		double currZDot = currRay.getDirRef().z;
		double prevZDot = prevRay.getDirRef().z;
		if (Math.abs(currZDot) < 0.017 ||
			Math.abs(prevZDot) < 0.017) // 0.017 is roughly sin(1 degree)
		{
			// This is too close to the xy-plane and will lead to too wild a translation
			return;
		}

		Plane dragPlane = new Plane(null, POI.z);
		double currDist = dragPlane.collisionDist(currRay);
		double prevDist = dragPlane.collisionDist(prevRay);
		if (currDist < 0 || prevDist < 0 ||
		    currDist == Double.POSITIVE_INFINITY ||
		    prevDist == Double.POSITIVE_INFINITY)
		{
			// We're either parallel to or beneath the collision plane, bail out
			return;
		}

		Vec3d currIntersect = currRay.getPointAtDist(currDist);
		Vec3d prevIntersect = prevRay.getPointAtDist(prevDist);

		Vec3d diff = new Vec3d();
		diff.sub3(currIntersect, prevIntersect);

		Vec3d camPos = _updateView.getGlobalPosition();
		Vec3d center = _updateView.getGlobalCenter();
		camPos.sub3(diff);
		center.sub3(diff);
		PolarInfo pi = getPolarFrom(center, camPos);
		updateCamTrans(pi, true);

	}

	private void handleExpVertPan(int x, int y, int dx, int dy) {
		Renderer.WindowMouseInfo info = _renderer.getMouseInfo(_windowID);
		if (info == null) return;

		//Cast a ray into the XY plane both for now, and for the previous mouse position
		Ray currRay = RenderUtils.getPickRayForPosition(info.cameraInfo, x, y, info.width, info.height);
		Ray prevRay = RenderUtils.getPickRayForPosition(info.cameraInfo, x - dx, y - dy, info.width, info.height);

		double zDiff = RenderUtils.getZDiff(POI, currRay, prevRay);

		Vec3d camPos = _updateView.getGlobalPosition();
		Vec3d center = _updateView.getGlobalCenter();
		camPos.z -= zDiff;
		center.z -= zDiff;
		PolarInfo pi = getPolarFrom(center, camPos);
		updateCamTrans(pi, true);

	}

	private void handleRotAroundPoint(int x, int y, int dx, int dy) {

		Vec3d camPos = _updateView.getGlobalPosition();
		Vec3d center = _updateView.getGlobalCenter();

		PolarInfo origPi = getPolarFrom(center, camPos);
		if ( camPos.x == center.x &&
		     camPos.y == center.y ) {
			// This is a degenerate camera view, tweak the polar info a bit to
			// prevent view flipping
			origPi.rotX = 0.00001;
			origPi.rotZ = 0;
		}

		Quaternion origRot = polarToRot(origPi);
		Mat4d rot = new Mat4d();
		rot.setRot3(origRot);

		Vec3d origUp = new Vec3d(0.0d, 1.0d, 0.0d);
		origUp.mult3(rot, origUp);

		Vec3d rotXAxis = new Vec3d(1.0d, 0.0d, 0.0d);
		rotXAxis.mult3(rot, rotXAxis);

		Quaternion rotX = new Quaternion();
		rotX.setAxisAngle(rotXAxis, -dy * ROT_SCALE_X);

		Quaternion rotZ = new Quaternion();
		rotZ.setRotZAxis(-dx * ROT_SCALE_Z);

		Mat4d rotTransX = MathUtils.rotateAroundPoint(rotX, POI);
		Mat4d rotTransZ = MathUtils.rotateAroundPoint(rotZ, POI);

		camPos.multAndTrans3(rotTransX, camPos);
		center.multAndTrans3(rotTransX, center);

		camPos.multAndTrans3(rotTransZ, camPos);
		center.multAndTrans3(rotTransZ, center);

		PolarInfo pi = getPolarFrom(center, camPos);

		Quaternion newRot = polarToRot(pi);
		rot.setRot3(newRot);

		Vec3d newUp = new Vec3d(0.0d, 1.0d, 0.0d);
		newUp.mult3(rot, newUp);

		double upDot = origUp.dot3(newUp);
		if (upDot < 0) {
			// The up angle has changed by more than 90 degrees, we probably are looking directly up or down
			// Instead only apply the rotation around Z
			camPos = _updateView.getGlobalPosition();
			center = _updateView.getGlobalCenter();

			camPos.multAndTrans3(rotTransZ, camPos);
			center.multAndTrans3(rotTransZ, center);

			pi = getPolarFrom(center, camPos);
		}

		updateCamTrans(pi, true);
	}


	@Override
	public void mouseWheelMoved(int windowID, int x, int y, int wheelRotation, int modifiers) {
		if (!_updateView.isMovable() || _updateView.isScripted()) {
			return;
		}

		Vec3d camPos = _updateView.getGlobalPosition();
		Vec3d center = _updateView.getGlobalCenter();

		Vec3d diff = new Vec3d();
		diff.sub3(POI, camPos);

		double scale = 1;
		double zoomFactor = (wheelRotation > 0) ? 1/ZOOM_FACTOR : ZOOM_FACTOR;
		for (int i = 0; i < Math.abs(wheelRotation); ++i) {
			scale = scale * zoomFactor;
		}

		// offset is the difference from where we are to where we're going
		diff.scale3(1 - scale);

		camPos.add3(diff);
		center.add3(diff);

		PolarInfo pi = getPolarFrom(center, camPos);
		updateCamTrans(pi, true);
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

	private Quaternion polarToRot(PolarInfo pi) {
		Quaternion rot = new Quaternion();
		rot.setRotZAxis(pi.rotZ);

		Quaternion tmp = new Quaternion();
		tmp.setRotXAxis(pi.rotX);

		rot.mult(rot, tmp);
		return rot;
	}

	private void updateCamTrans(PolarInfo pi, boolean updateInputs) {

		if (pi.rotX == 0) {
			pi.rotZ = 0; // If we're ever looking directly down, which is degenerate, force Y up
		}

		if (piCache != null && piCache.equals(pi) && !updateInputs) {
			return; // This update won't do anything
		}

		piCache = pi;

		Vec4d zOffset = new Vec4d(0, 0, pi.radius, 1.0d);

		Quaternion rot = polarToRot(pi);

		Transform finalTrans = new Transform(pi.viewCenter);

		finalTrans.merge(finalTrans, new Transform(null, rot, 1));
		finalTrans.merge(finalTrans, new Transform(zOffset));


		if (updateInputs) {
			updateViewPos(finalTrans.getTransRef(), pi.viewCenter);
		}

		// Finally update the renders camera info
		CameraInfo info = _renderer.getCameraInfo(_windowID);
		if (info == null) {
			// This window has not been opened yet (or is closed) force a redraw as everything will catch up
			// and the information has been saved to the view object
			RenderManager.redraw();
			piCache = null;
			return;
		}

		info.trans = finalTrans;

		info.skyboxTexture = _updateView.getSkyboxTexture();

		_renderer.setCameraInfoForWindow(_windowID, info);

		// Queue a redraw
		RenderManager.redraw();
	}

	public void setRotationAngles(double rotX, double rotZ) {
		PolarInfo pi = getPolarCoordsFromView();
		pi.rotX = rotX;
		pi.rotZ = rotZ;
		updateCamTrans(pi, true);
	}

	public void setWindowID(int windowID) {
		_windowID = windowID;
	}

	@Override
	public void windowClosing() {
		if (!RenderManager.isGood()) { return; }

		RenderManager.inst().hideExistingPopups();
		RenderManager.inst().windowClosed(_windowID);
	}

	@Override
	public void mouseButtonDown(int windowID, int x, int y, int button, boolean isDown, int modifiers) {
		if (!RenderManager.isGood()) { return; }

		// We need to cache dragging
		if (button == 1 && isDown) {
			Vec3d clickPoint = RenderManager.inst().getNearestPick(_windowID);
			if (clickPoint != null) {
				POI.set3(clickPoint);
				//dragPlane = new Plane(Vec4d.Z_AXIS, clickPoint.z);
			} else {
				// Set the drag plane to the XY_PLANE
				Renderer.WindowMouseInfo info = _renderer.getMouseInfo(_windowID);
				if (info == null) return;

				//Cast a ray into the XY plane both for now, and for the previous mouse position
				Ray mouseRay = RenderUtils.getPickRayForPosition(info.cameraInfo, x, y, info.width, info.height);
				double dist = Plane.XY_PLANE.collisionDist(mouseRay);
				if (dist < 0) {
					return;
				}
				POI.set3(mouseRay.getPointAtDist(dist));
				//dragPlane = Plane.XY_PLANE;

			}
		}

		RenderManager.inst().handleMouseButton(windowID, x, y, button, isDown, modifiers);
	}

	@Override
	public void windowGainedFocus() {
		if (!RenderManager.isGood()) { return; }

		RenderManager.inst().setActiveWindow(_windowID);
	}

	/**
	 * Set the position information in the saved view to match this window
	 */
	private void updateViewPos(Vec3d viewPos, Vec3d viewCenter) {
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

	private PolarInfo getPolarFrom(Vec3d center, Vec3d pos) {
		PolarInfo pi = new PolarInfo(center);

		Vec3d viewDiff = new Vec3d();
		viewDiff.sub3(pos, pi.viewCenter);

		pi.radius = viewDiff.mag3();

		pi.rotZ = Math.atan2(viewDiff.x, -viewDiff.y);

		double xyDist = Math.hypot(viewDiff.x, viewDiff.y);

		pi.rotX = Math.atan2(xyDist, viewDiff.z);

		return pi;

	}

	private PolarInfo getPolarCoordsFromView() {
		return getPolarFrom(_updateView.getGlobalCenter(), _updateView.getGlobalPosition());
	}

	public void checkForUpdate() {
		PolarInfo pi = getPolarCoordsFromView();
		updateCamTrans(pi, false);
	}

	@Override
	public void keyPressed(KeyEvent event) {
		switch (event.getKeySymbol()) {
		case KeyEvent.VK_DELETE:
			RenderManager.inst().deleteSelected();
			break;
		}
	}

	@Override
	public void keyReleased(KeyEvent event) {
		// Empty
	}
}