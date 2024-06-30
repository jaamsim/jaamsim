/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2018-2024 JaamSim Software Inc.
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

import com.jaamsim.ui.GUIFrame;
import com.jogamp.newt.event.KeyEvent;

public class EditableTextDelegate implements EditableText {

	private boolean editMode;     // true if the entity is being edited
	private String text;          // present text including any edits in progress
	private String initText;      // text before any editing is performed
	private int insertPos;        // position in the string where new text will be inserted
	private int numSelected;      // number of characters selected (positive to the right of the insertion position)

	public EditableTextDelegate() {
		setText("");
	}

	@Override
	public void setText(String str) {
		editMode = false;
		text = str;
		initText = "";
		insertPos = 0;
		numSelected = 0;
	}

	@Override
	public String getText() {
		return text;
	}

	@Override
	public void setEditMode(boolean bool) {
		if (bool == editMode)
			return;
		editMode = bool;
		if (bool) {
			initText = text;
			insertPos = text.length();
		}
		else {
			initText = "";
			insertPos = 0;
		}
		numSelected = 0;
	}

	@Override
	public boolean isEditMode() {
		return editMode;
	}

	@Override
	public void acceptEdits() {
		setEditMode(false);
	}

	@Override
	public void cancelEdits() {
		text = initText;
		setEditMode(false);
	}

	@Override
	public int getInsertPosition() {
		return insertPos;
	}

	@Override
	public int getNumberSelected() {
		return numSelected;
	}

	public void setNumberSelected(int num) {
		numSelected = num;
	}

	@Override
	public int handleEditKeyPressed(int keyCode, char keyChar, boolean shift, boolean control, boolean alt) {

		int ret = CONTINUE_EDITS;
		switch (keyCode) {

			case KeyEvent.VK_DELETE:
				if (numSelected == 0) {
					if (insertPos == text.length())
						break;
					StringBuilder sb = new StringBuilder(text);
					text = sb.deleteCharAt(insertPos).toString();
					break;
				}
				deleteSelection();
				break;

			case KeyEvent.VK_BACK_SPACE:
				if (numSelected == 0) {
					if (insertPos == 0)
						break;
					StringBuilder sb = new StringBuilder(text);
					text = sb.deleteCharAt(insertPos-1).toString();
					insertPos--;
					break;
				}
				deleteSelection();
				break;

			case KeyEvent.VK_LEFT:
				if (!shift && !(numSelected == 0)) {
					if (numSelected < 0)
						setInsertPosition(insertPos + numSelected, shift);
					else
						setInsertPosition(insertPos, shift);
					break;
				}
				setInsertPosition(Math.max(0, insertPos-1), shift);
				break;

			case KeyEvent.VK_RIGHT:
				if (!shift && !(numSelected == 0)) {
					if (numSelected > 0)
						setInsertPosition(insertPos + numSelected, shift);
					else
						setInsertPosition(insertPos, shift);
					break;
				}
				setInsertPosition(Math.min(text.length(), insertPos+1), shift);
				break;

			case KeyEvent.VK_UP:
				int upPos = getUpPosition(insertPos);
				if (upPos >= 0) {
					setInsertPosition(upPos, shift);
				}
				break;

			case KeyEvent.VK_DOWN:
				int downPos = getDownPosition(insertPos);
				if (downPos >= 0) {
					setInsertPosition(downPos, shift);
				}
				break;

			case KeyEvent.VK_HOME:
				if (control) {
					setInsertPosition(0, shift);
					break;
				}
				setInsertPosition(getLineStart(insertPos), shift);
				break;

			case KeyEvent.VK_END:
				if (control) {
					setInsertPosition(text.length(), shift);
					break;
				}
				setInsertPosition(getLineEnd(insertPos), shift);
				break;

			case KeyEvent.VK_ENTER:
				if (control) {
					StringBuilder sb = new StringBuilder(text);
					text = sb.insert(insertPos, '\n').toString();
					insertPos++;
					break;
				}
				ret = ACCEPT_EDITS;
				break;

			case KeyEvent.VK_ESCAPE:
				ret = CANCEL_EDITS;
				break;

			case KeyEvent.VK_C:
				if (control) {
					copyToClipboard();
					break;
				}

			case KeyEvent.VK_V:
				if (control) {
					deleteSelection();
					pasteFromClipboard();
					break;
				}

			case KeyEvent.VK_X:
				if (control) {
					copyToClipboard();
					deleteSelection();
					break;
				}

			default:
				if (control || !KeyEvent.isPrintableKey((short)keyCode, false))
					break;
				deleteSelection();
				StringBuilder sb = new StringBuilder(text);
				text = sb.insert(insertPos, keyChar).toString();
				insertPos++;
				break;
		}
		return ret;
	}

	@Override
	public int handleEditKeyReleased(int keyCode, char keyChar, boolean shift, boolean control, boolean alt) {
		return CONTINUE_EDITS;
	}

	@Override
	public void setInsertPosition(int pos, boolean shift) {
		if (shift)
			numSelected -= pos - insertPos;
		else
			numSelected = 0;
		insertPos = pos;
	}

	@Override
	public void selectPresentWord() {

		// Find the end of the present word
		int end = text.length();
		for (int i=insertPos; i<text.length(); i++) {
			if (text.charAt(i) == ' ') {
				end = i + 1;
				break;
			}
			if (text.charAt(i) == '\n') {
				end = i;
				break;
			}
		}

		// Find the start of the present word
		int start = 0;
		for (int i=insertPos-1; i>=0; i--) {
			if (text.charAt(i) == ' ' || text.charAt(i) == '\n') {
				start = i + 1;
				break;
			}
		}

		// Set the insert position and selection
		insertPos = end;
		numSelected = start - end;
	}

	@Override
	public void deleteSelection() {
		if (numSelected == 0)
			return;
		int start = Math.min(insertPos, insertPos+numSelected);
		int end = Math.max(insertPos, insertPos+numSelected);
		StringBuilder sb = new StringBuilder(text);
		text = sb.delete(start, end).toString();
		insertPos = start;
		numSelected = 0;
	}

	/**
	 * Returns the index of the first character in the line of text containing the specified index.
	 * @param i - index in the text string
	 * @return index of the first character in the line
	 */
	private int getLineStart(int ind) {
		for (int i = ind - 1; i >= 0; i--) {
			if (text.charAt(i) == '\n') {
				return i + 1;
			}
		}
		return 0;
	}

	/**
	 * Returns the index of the first newline character after the specified index.
	 * @param i - index in the text string
	 * @return index of the first newline
	 */
	private int getLineEnd(int ind) {
		for (int i = ind; i < text.length(); i++) {
			if (text.charAt(i) == '\n') {
				return i;
			}
		}
		return text.length();
	}

	private int getUpPosition(int ind) {
		int start = getLineStart(ind);
		if (start == 0)
			return -1;
		int linePos = ind - start;
		int end = start - 1;
		return Math.min(getLineStart(end) + linePos, end);
	}

	private int getDownPosition(int ind) {
		int end = getLineEnd(ind);
		if (end == text.length())
			return -1;
		int linePos = ind - getLineStart(ind);
		return Math.min(end + 1 + linePos, getLineEnd(end + 1));
	}

	@Override
	public void copyToClipboard() {
		int start = Math.min(insertPos, insertPos + numSelected);
		int end = Math.max(insertPos, insertPos + numSelected);
		StringBuilder sb = new StringBuilder(text);
		String copiedText = sb.substring(start, end);
		GUIFrame.copyToClipboard(copiedText);
	}

	@Override
	public void pasteFromClipboard() {
		String newText = GUIFrame.getStringFromClipboard();
		if (newText == null)
			return;
		StringBuilder sb = new StringBuilder(text);
		text = sb.insert(insertPos, newText).toString();
		insertPos += newText.length();
	}

	@Override
	public String toString() {
		return String.format("(text=%s, insertPos=%s, numSelected=%s, initText=%s)",
				text, insertPos, numSelected, initText);
	}

}
