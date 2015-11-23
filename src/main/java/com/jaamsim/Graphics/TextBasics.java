/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2015 Ausenco Engineering Canada Inc.
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

import com.jaamsim.DisplayModels.TextModel;
import com.jaamsim.controllers.RenderManager;
import com.jaamsim.input.InputAgent;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.ValueInput;
import com.jaamsim.math.Transform;
import com.jaamsim.math.Vec3d;
import com.jaamsim.units.DistanceUnit;
import com.jogamp.newt.event.KeyEvent;

/**
 * The "TextBasics" object displays text within the 3D model universe.
 * @author Harry King
 *
 */
public abstract class TextBasics extends DisplayEntity {

	@Keyword(description = "The height of the font as displayed in the view window.",
	         exampleList = {"15 m"})
	protected final ValueInput textHeight;

	private boolean editMode = false;  // true if the entity is being edited
	private String savedText = "";     // saved text after editing is finished
	private String editText = "";      // modified text as edited by the user
	private int insertPos = 0;         // position in the string where new text will be inserted
	private int numSelected = 0;       // number of characters selected (positive to the right of the insertion position)

	{
		textHeight = new ValueInput("TextHeight", "Key Inputs", 0.3d);
		textHeight.setValidRange(0.0d, Double.POSITIVE_INFINITY);
		textHeight.setUnitType(DistanceUnit.class);
		this.addInput(textHeight);
	}

	public TextBasics() {}

	public void setSavedText(String str) {
		savedText = str;
		editText = str;
	}

	public String getEditText() {
		return editText;
	}

	private void deleteSelection() {
		if (numSelected == 0)
			return;
		int start = Math.min(insertPos, insertPos+numSelected);
		int end = Math.max(insertPos, insertPos+numSelected);
		StringBuilder sb = new StringBuilder(editText);
		editText = sb.delete(start, end).toString();
		insertPos = start;
		numSelected = 0;
	}

	private void setInsertPosition(int pos, boolean shift) {
		if (shift)
			numSelected -= pos - insertPos;
		else
			numSelected = 0;
		insertPos = pos;
	}

	protected void acceptEdits() {
		savedText = editText;
		editMode = false;
		insertPos = editText.length();
		numSelected = 0;
	}

	protected void cancelEdits() {
		editMode = false;
		editText = savedText;
		insertPos = editText.length();
		numSelected = 0;
	}

	private void selectPresentWord() {

		// Find the end of the present word
		int end = editText.length();
		for (int i=insertPos; i<editText.length(); i++) {
			if (editText.charAt(i) == ' ') {
				end = i + 1;
				break;
			}
		}

		// Find the start of the present word
		int start = 0;
		for (int i=insertPos-1; i>=0; i--) {
			if (editText.charAt(i) == ' ') {
				start = i + 1;
				break;
			}
		}

		// Set the insert position and selection
		insertPos = end;
		numSelected = start - end;
	}

	private void copyToClipboard() {
		Clipboard clpbrd = Toolkit.getDefaultToolkit().getSystemClipboard();
		int start = Math.min(insertPos, insertPos+numSelected);
		int end = Math.max(insertPos, insertPos+numSelected);
		StringBuilder sb = new StringBuilder(editText);
		String copiedText = sb.substring(start, end).toString();
		clpbrd.setContents(new StringSelection(copiedText), null);
	}

	private void pasteFromClipboard() {
		Clipboard clpbrd = Toolkit.getDefaultToolkit().getSystemClipboard();
		try {
			String newText = (String)clpbrd.getData(DataFlavor.stringFlavor);
			StringBuilder sb = new StringBuilder(editText);
			editText = sb.insert(insertPos, newText).toString();
			insertPos += newText.length();
		}
		catch (Throwable err) {}
	}

	@Override
	public void handleKeyPressed(int keyCode, char keyChar, boolean shift, boolean control, boolean alt) {

		if (keyCode == KeyEvent.VK_F2) {
			editMode = true;
			insertPos = editText.length();
			numSelected = 0;
			RenderManager.redraw();
			return;
		}
		if (!editMode) {
			super.handleKeyPressed(keyCode, keyChar, shift, control, alt);
			return;
		}

		switch (keyCode) {

			case KeyEvent.VK_DELETE:
				if (numSelected == 0) {
					if (insertPos == editText.length())
						break;
					StringBuilder sb = new StringBuilder(editText);
					editText = sb.deleteCharAt(insertPos).toString();
					break;
				}
				deleteSelection();
				break;

			case KeyEvent.VK_BACK_SPACE:
				if (numSelected == 0) {
					if (insertPos == 0)
						break;
					StringBuilder sb = new StringBuilder(editText);
					editText = sb.deleteCharAt(insertPos-1).toString();
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
				setInsertPosition(Math.min(editText.length(), insertPos+1), shift);
				break;

			case KeyEvent.VK_HOME:
				setInsertPosition(0, shift);
				break;

			case KeyEvent.VK_END:
				setInsertPosition(editText.length(), shift);
				break;

			case KeyEvent.VK_ENTER:
				acceptEdits();
				break;

			case KeyEvent.VK_ESCAPE:
				cancelEdits();
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
				if (keyChar == KeyEvent.VK_UNDEFINED || control)
					break;
				deleteSelection();
				StringBuilder sb = new StringBuilder(editText);
				editText = sb.insert(insertPos, keyChar).toString();
				insertPos++;
				break;
		}
		RenderManager.redraw();
	}

	@Override
	public void handleKeyReleased(int keyCode, char keyChar, boolean shift, boolean control, boolean alt) {
		if (editMode)
			return;
		super.handleKeyReleased(keyCode, keyChar, shift, control, alt);
	}

	@Override
	public void handleMouseClicked(short count, Vec3d globalCoord) {
		if (count > 2)
			return;

		// Double click starts edit mode
		if (count == 2)
			editMode = true;

		// Set up the transformation from global coordinates to the entity's coordinates
		double height = textHeight.getValue();
		TextModel tm = (TextModel) displayModelListInput.getValue().get(0);
		Vec3d textsize = RenderManager.inst().getRenderedStringSize(tm.getTessFontKey(), height, editText);
		Transform trans = getEntityTransForSize(textsize);

		// Calculate the entity's coordinates for the mouse click
		Vec3d entityCoord = new Vec3d();
		trans.multAndTrans(globalCoord, entityCoord);

		// Position the insertion point where the text was clicked
		double insert = entityCoord.x + 0.5d*textsize.x;
		insertPos = RenderManager.inst().getRenderedStringPosition(tm.getTessFontKey(), height, editText, insert);
		numSelected = 0;

		// Double click selects a whole word
		if (count == 2)
			selectPresentWord();
	}

	@Override
	public boolean handleDrag(Vec3d currentPt, Vec3d firstPt) {
		if (!editMode)
			return false;

		// Set up the transformation from global coordinates to the entity's coordinates
		double height = textHeight.getValue();
		TextModel tm = (TextModel) displayModelListInput.getValue().get(0);
		Vec3d textsize = RenderManager.inst().getRenderedStringSize(tm.getTessFontKey(), height, editText);
		Transform trans = getEntityTransForSize(textsize);

		// Calculate the entity's coordinates for the mouse click
		Vec3d currentCoord = new Vec3d();
		trans.multAndTrans(currentPt, currentCoord);
		Vec3d firstCoord = new Vec3d();
		trans.multAndTrans(firstPt, firstCoord);

		// Set the start and end of highlighting
		double insert = currentCoord.x + 0.5d*textsize.x;
		double first = firstCoord.x + 0.5d*textsize.x;
		insertPos = RenderManager.inst().getRenderedStringPosition(tm.getTessFontKey(), height, editText, insert);
		int firstPos = RenderManager.inst().getRenderedStringPosition(tm.getTessFontKey(), height, editText, first);
		numSelected = firstPos - insertPos;
		return true;
	}

	@Override
	public void handleSelectionLost() {
		if (editMode)
			acceptEdits();
	}

	public String getCachedText() {
		if (editMode)
			return editText;
		return savedText;
	}

	public double getTextHeight() {
		return textHeight.getValue().doubleValue();
	}

	public Vec3d getTextSize() {
		double height = textHeight.getValue();
		TextModel tm = (TextModel) displayModelListInput.getValue().get(0);
		return RenderManager.inst().getRenderedStringSize(tm.getTessFontKey(), height, savedText);
		}

	public void resizeForText() {
		Vec3d textSize = getTextSize();
		double length = textSize.x + textSize.y;
		double height = 2.0 * textSize.y;
		Vec3d newSize = new Vec3d(length, height, 0.0);
		InputAgent.apply(this, InputAgent.formatPointInputs("Size", newSize, "m"));
	}

	public boolean isEditMode() {
		return editMode;
	}

	public int getInsertPosition() {
		return insertPos;
	}

	public int getNumberSelected() {
		return numSelected;
	}

}
