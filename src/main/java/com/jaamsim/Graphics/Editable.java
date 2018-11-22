/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2018 JaamSim Software Inc.
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
package com.jaamsim.Graphics;

public interface Editable {

	public static final int ACCEPT_EDITS = 1;
	public static final int CONTINUE_EDITS = 0;
	public static final int CANCEL_EDITS = -1;

	/**
	 * Sets the edit mode.
	 * @param bool - new mode: true = editing, false = not editing
	 */
	public void setEditMode(boolean bool);

	/**
	 * Returns whether edit mode is set.
	 * @return true if in edit mode
	 */
	public boolean isEditMode();

	/**
	 * Accepts any edits that have been made.
	 */
	public void acceptEdits();

	/**
	 * Cancels any edits that have been made.
	 */
	public void cancelEdits();

	/**
	 * Performs the editing action for the specified key on the keyboard is first pressed.
	 * @param keyCode - Newt code for the keyboard key
	 * @param keyChar - Newt character code for the keyboard key
	 * @param shift - true if the Shift key is pressed
	 * @param control - true if the Control key is pressed
	 * @param alt - true if the Alt key is pressed
	 * @return state after the keystroke: ACCEPT_EDITS, CONTINUE_EDITS, or CANCEL_EDITS
	 */
	public int handleEditKeyPressed(int keyCode, char keyChar, boolean shift, boolean control, boolean alt);


	/**
	 * Performs the editing action for the specified key on the keyboard is released.
	 * @param keyCode - Newt code for the keyboard key
	 * @param keyChar - Newt character code for the keyboard key
	 * @param shift - true if the Shift key is pressed
	 * @param control - true if the Control key is pressed
	 * @param alt - true if the Alt key is pressed
	 * @return state after the keystroke: ACCEPT_EDITS, CONTINUE_EDITS, or CANCEL_EDITS
	 */
	public int handleEditKeyReleased(int keyCode, char keyChar, boolean shift, boolean control, boolean alt);

}
