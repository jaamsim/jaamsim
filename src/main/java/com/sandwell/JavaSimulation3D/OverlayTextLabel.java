package com.sandwell.JavaSimulation3D;

import com.sandwell.JavaSimulation.BooleanInput;
import com.sandwell.JavaSimulation.Input;
import com.sandwell.JavaSimulation.IntegerInput;
import com.sandwell.JavaSimulation.IntegerListInput;
import com.sandwell.JavaSimulation.IntegerVector;
import com.sandwell.JavaSimulation.Keyword;
import com.sandwell.JavaSimulation.StringInput;

public class OverlayTextLabel extends DisplayEntity {

	@Keyword(description = "The static text to be displayed.  If spaces are included, enclose the text in single quotes.",
	         example = "TitleLabel Text { 'Example Simulation Model' }")
	private final StringInput text;

	@Keyword(description = "The height of the font as displayed in the view window. Unit is in pixels.",
	         example = "TitleLabel TextHeight { 15 }")
	private final IntegerInput textHeight;

	@Keyword(description = "The position of the label, from the upper left corner of the window to the upper left corner " +
	                "of the label. Value is in pixels",
	     example = "TitleLabel ScreenPosition { 20 20 }")
	private final IntegerListInput screenPosition;

	@Keyword(description = "If this text label should be aligned from the right edge of the window (instead of the left)",
	         example = "TitleLabel AlignRight { TRUE }")
	private final BooleanInput alignRight;

	@Keyword(description = "If this text label should be aligned from the bottom edge of the window (instead of the top)",
	         example = "TitleLabel AlignBottom { TRUE }")
	private final BooleanInput alignBottom;

	private String renderText;

	{
		text = new StringInput("Text", "Key Inputs", "abc");
		this.addInput(text, true, "Label");

		textHeight = new IntegerInput("TextHeight", "Key Inputs", 15);
		textHeight.setValidRange(0, 1000);
		this.addInput(textHeight, true);

		IntegerVector defPos = new IntegerVector(2);
		defPos.add(10);
		defPos.add(10);
		screenPosition = new IntegerListInput("ScreenPosition", "Key Inputs", defPos);
		screenPosition.setValidCount(2);
		screenPosition.setValidRange(0, 2500);
		this.addInput(screenPosition, true);

		alignRight = new BooleanInput("AlignRight", "Key Inputs", false);
		this.addInput(alignRight, true);

		alignBottom = new BooleanInput("AlignBottom", "Key Inputs", false);
		this.addInput(alignBottom, true);

		getInput("position").setHidden(true);
		getInput("alignment").setHidden(true);
		getInput("size").setHidden(true);
		getInput("orientation").setHidden(true);
		getInput("region").setHidden(true);
		getInput("relativeentity").setHidden(true);
		getInput("active").setHidden(true);
		getInput("show").setHidden(true);
		getInput("movable").setHidden(true);
		getInput("tooltip").setHidden(true);

	}

	@Override
	public void updateForInput( Input<?> in ) {
		super.updateForInput(in);

		setGraphicsDataDirty();
	}

	@Override
	public void updateGraphics(double time) {
		super.updateGraphics(time);

		// This is cached because PropertyLabel uses reflection to get this, so who knows how long it will take
		String newRenderText = getText(time);
		if (newRenderText.equals(renderText)) {
			// Nothing important has changed
			return;
		}

		// The text has been updated
		setGraphicsDataDirty();
		renderText = newRenderText;

	}

	protected String getText(double time) {
		return text.getValue();
	}

	public String getCachedText() {
		return renderText;
	}

	public int getTextHeight() {
		return textHeight.getValue();
	}

	public boolean alignRight() {
		return alignRight.getValue();
	}

	public boolean alignBottom() {
		return alignBottom.getValue();
	}

	public IntegerVector screenPosition() {
		return screenPosition.getValue();
	}

}
