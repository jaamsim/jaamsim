/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2009-2013 Ausenco Engineering Canada Inc.
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
package com.jaamsim.Graphics;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.util.ArrayList;

import com.jaamsim.DisplayModels.TextModel;
import com.jaamsim.controllers.RenderManager;
import com.jaamsim.input.EntityInput;
import com.jaamsim.input.Input;
import com.jaamsim.input.InputAgent;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.KeywordIndex;
import com.jaamsim.input.OutputHandle;
import com.jaamsim.input.OutputInput;
import com.jaamsim.input.StringInput;
import com.jaamsim.input.ValueInput;
import com.jaamsim.math.Transform;
import com.jaamsim.math.Vec3d;
import com.jaamsim.units.DistanceUnit;
import com.jaamsim.units.Unit;
import com.jogamp.newt.event.KeyEvent;

/**
 * The "Text" object displays written text within the 3D model universe.  Both fixed and variable text can be displayed.
 * @author Harry King
 *
 */
public class Text extends DisplayEntity {

	@Keyword(description = "The fixed and variable text to be displayed.  If spaces are included, enclose the text in single quotes.  " +
			"If variable text is to be displayed using the OutputName keyword, include the appropriate Java format in the text, " +
			"e.g. %s, %.6f, %.6g",
	         exampleList = {"'Present speed = %.3f m/s'"})
	protected final StringInput formatText;

	@Keyword(description = "The output value chain that returns the variable text to be displayed. " +
			"If more than one output value is given, all outputs but the last should point to an entity output to query" +
			" for the next output. The example returns the name of the product in a tank",
	         exampleList = {"Tank1 Product Name"})
	protected final OutputInput<Object> outputName;

	@Keyword(description = "The unit in which to express the output value",
	         exampleList = {"m/s"})
	protected final EntityInput<Unit> unit;

	@Keyword(description = "The height of the font as displayed in the view window.",
	         exampleList = {"15 m"})
	protected final ValueInput textHeight;

	@Keyword(description = "The text to display if there is any failure while formatting" +
	                       "the dynamic text, or while reading the output's value.",
	         exampleList = {"'Input Error'"})
	private final StringInput failText;

	protected String renderText = "";

	private boolean editMode = false;  // true if the entity is being edited
	private String editText = "";      // input text entered by the user
	private int insertPos = 0;         // position in the string where new text will be inserted
	private int numSelected = 0;       // number of characters selected (positive to the right of the insertion position)

	{
		formatText = new StringInput("Format", "Key Inputs", "");
		this.addInput(formatText);

		outputName = new OutputInput<>(Object.class, "OutputName", "Key Inputs", null);
		this.addInput(outputName);

		unit = new EntityInput<>( Unit.class, "Unit", "Key Inputs", null);
		this.addInput(unit);

		textHeight = new ValueInput("TextHeight", "Key Inputs", 0.3d);
		textHeight.setValidRange(0.0d, Double.POSITIVE_INFINITY);
		textHeight.setUnitType(DistanceUnit.class);
		this.addInput(textHeight);

		failText = new StringInput("FailText", "Key Inputs", "Input Error");
		this.addInput(failText);
	}

	public Text() {}

	@Override
	public void updateForInput(Input<?> in) {
		super.updateForInput(in);

		if (in == formatText) {
			editText = formatText.getValue();
			insertPos = editText.length();
			numSelected = 0;
			return;
		}

		if (in == outputName) {
			OutputHandle h = outputName.getOutputHandle(0.0);
			if (h != null)
				unit.setSubClass(h.getUnitType());
			return;
		}
	}

	@Override
	public void setInputsForDragAndDrop() {
		super.setInputsForDragAndDrop();

		// Set the displayed text to the entity's name
		ArrayList<String> tokens = new ArrayList<>(1);
		tokens.add(this.getName());
		KeywordIndex kw = new KeywordIndex("Format", tokens, null);
		InputAgent.apply(this, kw);
	}

	public String getRenderText(double simTime) {

		// If the object is selected, show the editable text
		if (editMode)
			return editText;

		if( outputName.getValue() == null )
			return formatText.getValue();

		try {
			OutputHandle out = outputName.getOutputHandle(simTime);
			if( out == null )
				return failText.getValue();

			if (out.isNumericValue()) {
				double d = out.getValueAsDouble(simTime, 0.0d, unit.getValue());
				return String.format(formatText.getValue(), d);
			}
			else {
				Object o = out.getValue(simTime, out.getReturnType());
				return String.format(formatText.getValue(), o);
			}
		}
		catch (Throwable e) {
			return failText.getValue();
		}
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

	private void acceptEdits() {
		ArrayList<String> args = new ArrayList<>();
		args.add(editText);
		InputAgent.apply(this, new KeywordIndex("Format", args, null));
		editMode = false;
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
				editMode = false;
				editText = formatText.getValue();
				insertPos = editText.length();
				numSelected = 0;
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
				if (keyChar == KeyEvent.VK_UNDEFINED)
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
		acceptEdits();
	}

	@Override
	public void updateGraphics(double simTime) {
		super.updateGraphics(simTime);

		// This text is cached because reflection is used to get it, so who knows how long it will take
		String newRenderText = getRenderText(simTime);
		if (newRenderText.equals(renderText)) {
			// Nothing important has changed
			return;
		}

		// The text has been updated
		renderText = newRenderText;

	}

	public String getCachedText() {
		return renderText;
	}

	public double getTextHeight() {
		return textHeight.getValue().doubleValue();
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
