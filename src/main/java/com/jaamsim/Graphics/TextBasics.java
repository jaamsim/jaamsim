/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2015 Ausenco Engineering Canada Inc.
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

import java.util.ArrayList;

import com.jaamsim.DisplayModels.TextModel;
import com.jaamsim.controllers.RenderManager;
import com.jaamsim.input.BooleanInput;
import com.jaamsim.input.ColourInput;
import com.jaamsim.input.Input;
import com.jaamsim.input.InputAgent;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.StringChoiceInput;
import com.jaamsim.input.StringListInput;
import com.jaamsim.input.ValueInput;
import com.jaamsim.input.Vec3dInput;
import com.jaamsim.math.Color4d;
import com.jaamsim.math.Transform;
import com.jaamsim.math.Vec3d;
import com.jaamsim.render.TessFontKey;
import com.jaamsim.ui.GUIFrame;
import com.jaamsim.units.DistanceUnit;
import com.jogamp.newt.event.KeyEvent;

/**
 * The "TextBasics" object displays text within the 3D model universe.
 * @author Harry King
 *
 */
public abstract class TextBasics extends DisplayEntity implements TextEntity, EditableText {

	@Keyword(description = "The font to be used for the text.",
	         exampleList = { "Arial" })
	private final StringChoiceInput fontName;

	@Keyword(description = "The height of the font as displayed in the view window.",
	         exampleList = {"15 m"})
	protected final ValueInput textHeight;

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

	private final EditableTextDelegate editableText;

	{
		displayModelListInput.addValidClass(TextModel.class);

		fontName = new StringChoiceInput("FontName", FONT, -1);
		fontName.setChoices(TextModel.validFontNames);
		fontName.setDefaultText("TextModel");
		this.addInput(fontName);

		textHeight = new ValueInput("TextHeight", FONT, 0.3d);
		textHeight.setValidRange(0.0d, Double.POSITIVE_INFINITY);
		textHeight.setUnitType(DistanceUnit.class);
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

	public TextBasics() {
		editableText = new EditableTextDelegate();
	}

	@Override
	public void updateForInput( Input<?> in ) {
		super.updateForInput( in );

		if (in == fontName || in == textHeight || in == fontColor || in == fontStyle) {
			if (GUIFrame.getInstance() == null)
				return;
			GUIFrame.getInstance().updateTextButtons();
			return;
		}
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
	}

	@Override
	public void cancelEdits() {
		editableText.cancelEdits();
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
		if (result == Editable.ACCEPT_EDITS) {
			acceptEdits();
		}
		else if (result == Editable.CANCEL_EDITS) {
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
	public void handleMouseClicked(short count, Vec3d globalCoord) {
		if (count > 2)
			return;

		// Double click starts edit mode
		if (!isEditMode() && count == 2) {
			setEditMode(true);
		}

		if (!isEditMode())
			return;

		// Position the insertion point where the text was clicked
		int pos = getStringPosition(globalCoord);
		editableText.setInsertPosition(pos, false);

		// Double click selects a whole word
		if (count == 2)
			editableText.selectPresentWord();
	}

	@Override
	public boolean handleDrag(Vec3d currentPt, Vec3d firstPt) {
		if (!isEditMode())
			return false;

		// Set the start and end of highlighting
		int insertPos = getStringPosition(currentPt);
		int firstPos = getStringPosition(firstPt);
		editableText.setInsertPosition(insertPos, false);
		editableText.setNumberSelected(firstPos - insertPos);
		return true;
	}

	@Override
	public void handleSelectionLost() {
		if (isEditMode()) {
			acceptEdits();
		}
	}

	public String getCachedText() {
		return getText();
	}

	/**
	 * Returns the insert position in the present text that corresponds to the specified global
	 * coordinate. Index 0 is located immediately before the first character in the text.
	 * @param globalCoord - position in the global coordinate system
	 * @return insert position in the text string
	 */
	public int getStringPosition(Vec3d globalCoord) {
		double height = getTextHeight();
		TessFontKey fontKey = getTessFontKey();

		// Set up the transformation from global coordinates to the entity's coordinates
		Vec3d textsize = RenderManager.inst().getRenderedStringSize(fontKey, height, getText());
		Transform trans = getEntityTransForSize(textsize);

		// Calculate the entity's coordinates for the mouse click
		Vec3d entityCoord = new Vec3d();
		trans.multAndTrans(globalCoord, entityCoord);

		// Position the insertion point where the text was clicked
		double insert = entityCoord.x + 0.5d*textsize.x;
		int pos = RenderManager.inst().getRenderedStringPosition(fontKey, height, getText(), insert);
		return pos;
	}

	public Vec3d getTextSize() {
		double height = getTextHeight();
		TessFontKey fontKey = getTessFontKey();
		return RenderManager.inst().getRenderedStringSize(fontKey, height, getText());
	}

	public void resizeForText() {
		if (!RenderManager.isGood())
			return;
		Vec3d textSize = getTextSize();
		double length = textSize.x + textSize.y;
		double height = 2.0 * textSize.y;
		Vec3d newSize = new Vec3d(length, height, 0.0);
		InputAgent.apply(this, InputAgent.formatVec3dInput("Size", newSize, DistanceUnit.class));
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

	public TextModel getTextModel() {
		return (TextModel) getDisplayModel();
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
			return getTextModel().getTextHeight();
		}
		return textHeight.getValue();
	}

	@Override
	public String getTextHeightString() {
		if (textHeight.isDefault()) {
			return getTextModel().getTextHeightString();
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

	@Override
	public boolean isBold() {
		return TextModel.isBold(getStyle());
	}

	@Override
	public boolean isItalic() {
		return TextModel.isItalic(getStyle());
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

}
