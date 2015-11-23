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

import com.jogamp.newt.event.InputEvent;
import com.jogamp.newt.event.MouseAdapter;
import com.jogamp.newt.event.MouseEvent;

/**
 * Basic Mouse interface handler for JaamSim renderer. This is the Mouse side of the 'WindowInteractionListener'
 * framework. This class packages Next mouse events and calls the appropriate WindowInteractionListener handlers
 *
 * @author Matt Chudleigh
 *
 */

public class MouseHandler extends MouseAdapter {

	private static int modsFromEvent(InputEvent e) {
		int flags = 0;
		flags += e.isShiftDown() ? WindowInteractionListener.MOD_SHIFT : 0;
		flags += e.isAltDown() ? WindowInteractionListener.MOD_ALT : 0;
		flags += e.isControlDown() ? WindowInteractionListener.MOD_CTRL : 0;

		return flags;
	}

	private boolean[] _isDown;

	private int _startX, _startY;
	private int _lastX, _lastY;

	private WindowInteractionListener _listener;
	RenderWindow _window;
	int _windowID;

	public MouseHandler(RenderWindow window, WindowInteractionListener listener) {
		_listener = listener;
		_window = window;
		_windowID = _window.getWindowID();

		_isDown = new boolean[5];
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		_window.setMouseIn(true); // If we're clicked the mouse is in the window (some of the events seem to get dropped
		_listener.mouseClicked(_windowID, e.getX(), e.getY(), e.getButton(), modsFromEvent(e), e.getClickCount());
	}

	@Override
	public void mouseEntered(MouseEvent e) {
		//_isDown = false;
		_window.setMouseIn(true);
		_listener.mouseEntry(_windowID, e.getX(), e.getY(), true);
	}

	@Override
	public void mouseExited(MouseEvent e) {
		//_isDown = false;
		_window.setMouseIn(false);
		_listener.mouseEntry(_windowID, e.getX(), e.getY(), false);
	}

	@Override
	public void mousePressed(MouseEvent e) {
		_isDown[e.getButton()] = true;
		_startX = e.getX();
		_startY = e.getY();
		_lastX = e.getX();
		_lastY = e.getY();
		_listener.mouseButtonDown(_windowID, _lastX, _lastY, e.getButton(), true, modsFromEvent(e));
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		_isDown[e.getButton()] = false;
		_listener.mouseButtonDown(_windowID, _lastX, _lastY, e.getButton(), false, modsFromEvent(e));
	}
	@Override
	public void mouseMoved(MouseEvent e) {
		_lastX = e.getX();
		_lastY = e.getY();

		_window.setMouseX(_lastX);
		_window.setMouseY(_lastY);

		_listener.mouseMoved(_windowID, _lastX, _lastY);
	}

	@Override
	public void mouseDragged(MouseEvent e) {
		if (!_isDown[e.getButton()]) {
			return; // This should probably not happen, but just in case...
		}
		int deltaX = e.getX() - _lastX;
		int deltaY = e.getY() - _lastY;

		_lastX = e.getX();
		_lastY = e.getY();

		_window.setMouseX(_lastX);
		_window.setMouseY(_lastY);

		WindowInteractionListener.DragInfo dragInfo = new WindowInteractionListener.DragInfo();
		dragInfo.windowID = _windowID;
		dragInfo.x = _lastX;
		dragInfo.y = _lastY;
		dragInfo.dx = deltaX;
		dragInfo.dy = deltaY;
		dragInfo.startX = _startX;
		dragInfo.startY = _startY;
		dragInfo.button = e.getButton();
		dragInfo.modFlags = modsFromEvent(e);

		_listener.mouseDragged(dragInfo);
	}

	@Override
	public void mouseWheelMoved(MouseEvent e) {
		_listener.mouseWheelMoved(_windowID, e.getX(), e.getY(), (int)e.getRotation()[1], modsFromEvent(e));
	}
}
