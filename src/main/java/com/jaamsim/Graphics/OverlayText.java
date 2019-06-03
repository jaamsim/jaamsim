/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2013 Ausenco Engineering Canada Inc.
 * Copyright (C) 2018-2019 JaamSim Software Inc.
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

import java.util.ArrayList;

import com.jaamsim.Commands.KeywordCommand;
import com.jaamsim.DisplayModels.TextModel;
import com.jaamsim.StringProviders.StringProvConstant;
import com.jaamsim.StringProviders.StringProvInput;
import com.jaamsim.basicsim.GUIListener;
import com.jaamsim.controllers.RenderManager;
import com.jaamsim.datatypes.IntegerVector;
import com.jaamsim.input.BooleanInput;
import com.jaamsim.input.ColourInput;
import com.jaamsim.input.EntityInput;
import com.jaamsim.input.Input;
import com.jaamsim.input.InputAgent;
import com.jaamsim.input.IntegerInput;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.KeywordIndex;
import com.jaamsim.input.StringChoiceInput;
import com.jaamsim.input.StringInput;
import com.jaamsim.input.StringListInput;
import com.jaamsim.input.UnitTypeInput;
import com.jaamsim.input.Vec3dInput;
import com.jaamsim.math.Color4d;
import com.jaamsim.math.Vec3d;
import com.jaamsim.render.TessFontKey;
import com.jaamsim.units.DimensionlessUnit;
import com.jaamsim.units.Unit;
import com.jogamp.newt.event.KeyEvent;

/**
 * OverylayText displays written text as a 2D overlay on a View window.
 * @author Harry King
 *
 */
public class OverlayText extends OverlayEntity implements TextEntity, EditableText {

	@Keyword(description = "The fixed and variable text to be displayed. If spaces are included, "
	                     + "enclose the text in single quotes. If variable text is to be "
	                     + "displayed using the DataSource keyword, include the appropriate Java "
	                     + "format in the text, such as %s, %.6f, %.6g.",
	         exampleList = {"'Present speed = %.3f m/s'", "'Present State = %s'"})
	protected final StringInput formatText;

	@Keyword(description = "The unit type for the numerical value to be displayed as "
	                     + "variable text. Set to DimensionlessUnit if the variable text is "
	                     + "non-numeric, such as the state of a Server.",
	         exampleList = {"DistanceUnit", "DimensionlessUnit"})
	protected final UnitTypeInput unitType;

	@Keyword(description = "The unit in which to express an expression that returns a numeric "
	                     + "value.",
	         exampleList = {"m/s"})
	protected final EntityInput<Unit> unit;

	@Keyword(description = "An expression that returns the variable text to be displayed. "
	                     + "The expression can return a number that will be formated as text, "
	                     + "or it can return text directly, such as the state of a Server. "
	                     + "An object that returns a number, such as a TimeSeries, can also "
	                     + "be entered.",
	         exampleList = {"[Queue1].AverageQueueTime", "[Server1].State",
	                        "'[Queue1].QueueLength + [Queue2].QueueLength'",
	                        "TimeSeries1"})
	protected final StringProvInput dataSource;

	@Keyword(description = "The text to display if there is any failure while formatting the "
	                     + "variable text or while evaluating the expression.",
	         exampleList = {"'Input Error'"})
	protected final StringInput failText;

	@Keyword(description = "The font to be used for the text.",
	         exampleList = { "Arial" })
	private final StringChoiceInput fontName;

	@Keyword(description = "The height of the font as displayed in the view window. Unit is in pixels.",
	         exampleList = {"15"})
	private final IntegerInput textHeight;

	@Keyword(description = "The font styles to be applied to the text, e.g. Bold, Italic. ",
	         exampleList = { "Bold" })
	private final StringListInput fontStyle;

	@Keyword(description = "The colour of the text.",
	         exampleList = { "red", "skyblue", "135 206 235" })
	private final ColourInput fontColor;

	@Keyword(description = "If TRUE, then a drop shadow appears for the text.",
	         exampleList = { "TRUE" })
	private final BooleanInput dropShadow;

	@Keyword(description = "The colour for the drop shadow.",
	         exampleList = { "red", "skyblue", "135 206 235" })
	private final ColourInput dropShadowColor;

	@Keyword(description = "The { x, y, z } coordinates of the drop shadow's offset, expressed "
	                     + "as a decimal fraction of the text height.",
	         exampleList = { "0.1 -0.1 0.001" })
	private final Vec3dInput dropShadowOffset;

	private String renderText;
	private final EditableTextDelegate editableText;

	{
		displayModelListInput.addValidClass(TextModel.class);

		formatText = new StringInput("Format", KEY_INPUTS, "%s");
		this.addInput(formatText);

		unitType = new UnitTypeInput("UnitType", KEY_INPUTS, DimensionlessUnit.class);
		this.addInput(unitType);

		unit = new EntityInput<>(Unit.class, "Unit", KEY_INPUTS, null);
		unit.setSubClass(null);
		this.addInput(unit);

		dataSource = new StringProvInput("DataSource", KEY_INPUTS, new StringProvConstant(""));
		dataSource.setUnitType(DimensionlessUnit.class);
		this.addInput(dataSource);
		this.addSynonym(dataSource, "OutputName");

		failText = new StringInput("FailText", KEY_INPUTS, "Input Error");
		this.addInput(failText);

		fontName = new StringChoiceInput("FontName", FONT, -1);
		fontName.setChoices(TextModel.validFontNames);
		fontName.setDefaultText("TextModel");
		this.addInput(fontName);

		textHeight = new IntegerInput("TextHeight", FONT, 0);
		textHeight.setValidRange(0, 1000);
		textHeight.setDefaultText("TextModel");
		this.addInput(textHeight);

		fontColor = new ColourInput("FontColour", FONT, ColourInput.BLACK);
		fontColor.setDefaultText("TextModel");
		this.addInput(fontColor);
		this.addSynonym(fontColor, "FontColor");

		fontStyle = new StringListInput("FontStyle", FONT, new ArrayList<String>(0));
		fontStyle.setValidOptions(TextModel.validStyles);
		fontStyle.setCaseSensitive(false);
		fontStyle.setDefaultText("TextModel");
		this.addInput(fontStyle);

		dropShadow = new BooleanInput("DropShadow", FONT, false);
		dropShadow.setDefaultText("TextModel");
		this.addInput(dropShadow);

		dropShadowColor = new ColourInput("DropShadowColour", FONT, ColourInput.BLACK);
		dropShadowColor.setDefaultText("TextModel");
		this.addInput(dropShadowColor);
		this.addSynonym(dropShadowColor, "DropShadowColor");

		dropShadowOffset = new Vec3dInput("DropShadowOffset", FONT, null);
		dropShadowOffset.setDefaultText("TextModel");
		this.addInput(dropShadowOffset);
	}

	public OverlayText() {
		editableText = new EditableTextDelegate();
	}

	@Override
	public void updateForInput(Input<?> in) {
		super.updateForInput(in);

		if (in == formatText) {
			setText(formatText.getValue());
			return;
		}

		if (in == unitType) {
			Class<? extends Unit> ut = unitType.getUnitType();
			dataSource.setUnitType(ut);
			unit.setSubClass(ut);
			return;
		}

		if (in == fontName || in == textHeight || in == fontColor || in == fontStyle) {
			GUIListener gui = getJaamSimModel().getGUIListener();
			if (gui != null)
				gui.updateControls();
			return;
		}
	}

	@Override
	public void setInputsForDragAndDrop() {
		super.setInputsForDragAndDrop();

		// Set the displayed text to the entity's name
		InputAgent.applyArgs(this, "Format", this.getName());
	}

	@Override
	public void setText(String str) {
		editableText.setText(str);
	}

	@Override
	public String getText() {
		return editableText.getText();
	}

	@Override
	public void acceptEdits() {
		editableText.acceptEdits();
		KeywordIndex kw = InputAgent.formatArgs("Format", getText());
		InputAgent.storeAndExecute(new KeywordCommand(this, kw));
	}

	@Override
	public void cancelEdits() {
		editableText.cancelEdits();
	}

	@Override
	public boolean isEditMode() {
		return editableText.isEditMode();
	}

	@Override
	public void setEditMode(boolean bool) {
		editableText.setEditMode(bool);
	}

	@Override
	public int getInsertPosition() {
		return editableText.getInsertPosition();
	}

	@Override
	public int getNumberSelected() {
		return editableText.getNumberSelected();
	}

	@Override
	public int handleEditKeyPressed(int keyCode, char keyChar, boolean shift, boolean control, boolean alt) {
		return editableText.handleEditKeyPressed(keyCode, keyChar, shift, control, alt);
	}

	@Override
	public int handleEditKeyReleased(int keyCode, char keyChar, boolean shift, boolean control, boolean alt) {
		return editableText.handleEditKeyReleased(keyCode, keyChar, shift, control, alt);
	}

	@Override
	public void setInsertPosition(int pos, boolean shift) {
		editableText.setInsertPosition(pos, shift);
	}

	@Override
	public void selectPresentWord() {
		editableText.selectPresentWord();
	}

	@Override
	public void handleKeyPressed(int keyCode, char keyChar, boolean shift, boolean control, boolean alt) {

		// If F2 is pressed, set edit mode
		if (keyCode == KeyEvent.VK_F2) {
			setEditMode(true);
			RenderManager.redraw();
			return;
		}

		// If not in edit mode, apply the normal action for the keystroke
		if (!isEditMode()) {
			super.handleKeyPressed(keyCode, keyChar, shift, control, alt);
			return;
		}

		// If in edit mode, the apply the keystroke to the text
		int result = handleEditKeyPressed(keyCode, keyChar, shift, control, alt);
		if (result == ACCEPT_EDITS) {
			acceptEdits();
		}
		else if (result == CANCEL_EDITS) {
			cancelEdits();
		}
		RenderManager.redraw();
	}

	@Override
	public void handleKeyReleased(int keyCode, char keyChar, boolean shift, boolean control, boolean alt) {
		if (isEditMode()) {
			handleEditKeyReleased(keyCode, keyChar, shift, control, alt);
			return;
		}
		super.handleKeyReleased(keyCode, keyChar, shift, control, alt);
	}

	@Override
	public void handleMouseClicked(short count, int x, int y, int windowWidth, int windowHeight) {
		if (count > 2)
			return;

		// Double click starts edit mode
		if (!isEditMode() && count == 2) {
			setEditMode(true);
		}

		if (!isEditMode())
			return;

		// Position the insertion point where the text was clicked
		int pos = getStringPosition(x, y, windowWidth, windowHeight);
		editableText.setInsertPosition(pos, false);

		// Double click selects a whole word
		if (count == 2)
			editableText.selectPresentWord();
	}

	@Override
	public boolean handleDrag(int x, int y, int startX, int startY, int windowWidth, int windowHeight) {
		if (!isEditMode())
			return false;

		// Set the start and end of highlighting
		int insertPos = getStringPosition(x, y, windowWidth, windowHeight);
		int firstPos = getStringPosition(startX, startY, windowWidth, windowHeight);
		editableText.setInsertPosition(insertPos, false);
		editableText.setNumberSelected(firstPos - insertPos);
		return true;
	}

	/**
	 * Returns the insert position in the present text that corresponds to the specified global
	 * coordinate. Index 0 is located immediately before the first character in the text.
	 * @param x -
	 * @param y -
	 * @return insert position in the text string
	 */
	public int getStringPosition(int x, int y, int windowWidth, int windowHeight) {
		double height = getTextHeight();
		TessFontKey fontKey = getTessFontKey();
		double length = RenderManager.inst().getRenderedStringLength(fontKey, height, getText());
		IntegerVector pos = getScreenPosition();
		double textStart = getAlignRight() ? windowWidth - pos.get(0) - length : pos.get(0);
		return RenderManager.inst().getRenderedStringPosition(fontKey, height, getText(), x - textStart);
	}

	public String getRenderText(double simTime) {

		// If the object is selected, show the editable text
		if (isEditMode())
			return getText();

		double siFactor = 1.0d;
		if (unit.getValue() != null)
			siFactor = unit.getValue().getConversionFactorToSI();

		// Default Format
		if (formatText.isDefault())
			return dataSource.getValue().getNextString(simTime, siFactor);

		// Only static text is to be displayed
		if (dataSource.isDefault())
			return formatText.getValue();

		// Dynamic text is to be displayed
		try {
			return dataSource.getValue().getNextString(simTime, formatText.getValue(), siFactor);
		}
		catch (Throwable e) {
			return failText.getValue();
		}
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

	@Override
	public void handleSelectionLost() {
		if (isEditMode()) {
			acceptEdits();
		}
	}

	public String getCachedText() {
		return renderText;
	}

	public TextModel getTextModel() {
		return (TextModel) displayModelListInput.getValue().get(0);
	}

	@Override
	public String getFontName() {
		if (fontName.isDefault()) {
			return getTextModel().getFontName();
		}
		return fontName.getChoice();
	}

	@Override
	public double getTextHeight() {
		if (textHeight.isDefault()) {
			return getTextModel().getTextHeightInPixels();
		}
		return textHeight.getValue();
	}

	@Override
	public String getTextHeightString() {
		if (textHeight.isDefault()) {
			return getTextModel().getTextHeightInPixelsString();
		}
		return textHeight.getValueString();
	}

	@Override
	public int getStyle() {
		if (fontStyle.isDefault()) {
			return getTextModel().getStyle();
		}
		return TextModel.getStyle(fontStyle.getValue());
	}

	public TessFontKey getTessFontKey() {
		return new TessFontKey(getFontName(), getStyle());
	}

	@Override
	public Color4d getFontColor() {
		if (fontColor.isDefault()) {
			return getTextModel().getFontColor();
		}
		return fontColor.getValue();
	}

	@Override
	public boolean getDropShadow() {
		if (dropShadow.isDefault()) {
			return getTextModel().getDropShadow();
		}
		return dropShadow.getValue();
	}

	@Override
	public Color4d getDropShadowColor() {
		if (dropShadowColor.isDefault()) {
			return getTextModel().getDropShadowColor();
		}
		return dropShadowColor.getValue();
	}

	@Override
	public Vec3d getDropShadowOffset() {
		if (dropShadowOffset.isDefault()) {
			return getTextModel().getDropShadowOffset();
		}
		return dropShadowOffset.getValue();
	}

	@Override
	public boolean isBold() {
		return TextModel.isBold(getStyle());
	}

	@Override
	public boolean isItalic() {
		return TextModel.isItalic(getStyle());
	}

}
