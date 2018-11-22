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

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;

import com.jogamp.newt.event.KeyEvent;

public class EditableTextDelegate implements EditableText {

	private boolean editMode;     // true if the entity is being edited
	private String text;          // present text including any edits in progress
	private String initText;      // text before any editing is performed
	private int insertPos;        // position in the string where new text will be inserted
	private int numSelected;      // number of characters selected (positive to the right of the insertion position)

	public EditableTextDelegate() {}

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

			case KeyEvent.VK_HOME:
				setInsertPosition(0, shift);
				break;

			case KeyEvent.VK_END:
				setInsertPosition(text.length(), shift);
				break;

			case KeyEvent.VK_ENTER:
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
		}

		// Find the start of the present word
		int start = 0;
		for (int i=insertPos-1; i>=0; i--) {
			if (text.charAt(i) == ' ') {
				start = i + 1;
				break;
			}
		}

		// Set the insert position and selection
		insertPos = end;
		numSelected = start - end;
	}

	private void deleteSelection() {
		if (numSelected == 0)
			return;
		int start = Math.min(insertPos, insertPos+numSelected);
		int end = Math.max(insertPos, insertPos+numSelected);
		StringBuilder sb = new StringBuilder(text);
		text = sb.delete(start, end).toString();
		insertPos = start;
		numSelected = 0;
	}

	private void copyToClipboard() {
		Clipboard clpbrd = Toolkit.getDefaultToolkit().getSystemClipboard();
		int start = Math.min(insertPos, insertPos + numSelected);
		int end = Math.max(insertPos, insertPos + numSelected);
		StringBuilder sb = new StringBuilder(text);
		String copiedText = sb.substring(start, end).toString();
		clpbrd.setContents(new StringSelection(copiedText), null);
	}

	private void pasteFromClipboard() {
		Clipboard clpbrd = Toolkit.getDefaultToolkit().getSystemClipboard();
		try {
			String newText = (String)clpbrd.getData(DataFlavor.stringFlavor);
			StringBuilder sb = new StringBuilder(text);
			text = sb.insert(insertPos, newText).toString();
			insertPos += newText.length();
		}
		catch (Throwable err) {}
	}

	@Override
	public String toString() {
		return String.format("(text=%s, insertPos=%s, numSelected=%s, initText=%s)",
				text, insertPos, numSelected, initText);
	}

}
