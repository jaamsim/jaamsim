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

import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.KeyListener;
import com.jogamp.newt.event.MouseEvent;

public interface WindowInteractionListener extends KeyListener {

	// Flags used as modifiers
	public static int MOD_SHIFT = 	1;
	public static int MOD_ALT = 	2;
	public static int MOD_CTRL = 	4;


	/**
	 *
	 * @author matt.chudleigh
	 *
	 */
	public static class DragInfo {
		public int windowID;
		public int x, y; // the current position
		public int dx, dy; // The change in x, y since the last dragged event
		public int startX, startY; // The point where the mouse first went down
		public int button; // the button that went down
		public int modFlags; // Modifiers

		public boolean shiftDown() {
			return (modFlags & MOD_SHIFT) != 0;
		}
		public boolean altDown() {
			return (modFlags & MOD_ALT) != 0;
		}
		public boolean controlDown() {
			return (modFlags & MOD_CTRL) != 0;
		}
	}
	/**
	 * The mouse was dragged in this window.
	 * @param info - a struct containing all the information for this drag event (this is just cleaner)
	 */
	public void mouseDragged(DragInfo info);

	/**
	 * The mouse wheel was moved
	 * @param windowID
	 * @param x
	 * @param y
	 * @param wheelRotation - the number of mouse wheel 'ticks'
	 */
	public void mouseWheelMoved(int windowID, int x, int y, int wheelRotation, int modifiers);

	/**
	 * A click occurred, registered on mouse button release
	 * @param windowID
	 * @param x
	 * @param y
	 */
	public void mouseClicked(int windowID, int x, int y, int button, int modifiers, short count);

	/**
	 * Basic mouse motion
	 * @param windowID
	 * @param x
	 * @param y
	 */
	public void mouseMoved(int windowID, int x, int y);

	/**
	 * Track mouse entry/exit
	 * @param isInWindow
	 */
	public void mouseEntry(int windowID, int x, int y, boolean isInWindow);

	/**
	 * Callback for mouse button press/release
	 * @param windowID
	 * @param x
	 * @param y
	 * @param button
	 * @param isDown
	 */
	public void mouseButtonDown(int windowID, int x, int y, int button, boolean isDown, int modifiers);

	/**
	 * In the event that the above definitions do not provide enough useful information, the
	 * raw NEWT mouse events can also be hooked
	 * @param me
	 */
	public void rawMouseEvent(MouseEvent me);

	/**
	 * The window is closing
	 */
	public void windowClosing();

	/**
	 * This window gained focus
	 */
	public void windowGainedFocus();
	public void windowMoved(int x, int y, int width, int height);

	@Override
	public void keyPressed(KeyEvent event);
	@Override
	public void keyReleased(KeyEvent event);
}
