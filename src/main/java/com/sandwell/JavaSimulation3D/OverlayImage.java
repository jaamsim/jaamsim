package com.sandwell.JavaSimulation3D;

import com.sandwell.JavaSimulation.BooleanInput;
import com.sandwell.JavaSimulation.Input;
import com.sandwell.JavaSimulation.IntegerListInput;
import com.sandwell.JavaSimulation.IntegerVector;
import com.sandwell.JavaSimulation.Keyword;

public class OverlayImage extends DisplayEntity {
	@Keyword(desc = "The position of the image, from the upper left corner of the window to the upper left corner " +
	                "of the image. Value is in pixels",
	         example = "Logo ScreenPosition { 20 20 }")
	private final IntegerListInput screenPosition;

	@Keyword(desc = "The size of the image. Value is in pixels",
	         example = "Logo ImageSize { 200 100 }")
	private final IntegerListInput size;

	@Keyword(desc = "If this text label should be aligned from the right edge of the window (instead of the left)",
	         example = "Logo AlignRight { TRUE }")
	private final BooleanInput alignRight;

	@Keyword(desc = "If this text label should be aligned from the bottom edge of the window (instead of the top)",
	         example = "Logo AlignBottom { TRUE }")
	private final BooleanInput alignBottom;

	{
		IntegerVector defPos = new IntegerVector(2);
		defPos.add(10);
		defPos.add(10);
		screenPosition = new IntegerListInput("ScreenPosition", "Key Inputs", defPos);
		screenPosition.setValidCount(2);
		screenPosition.setValidRange(0, 2500);
		this.addInput(screenPosition, true);

		IntegerVector defSize = new IntegerVector(2);
		defSize.add(100);
		defSize.add(100);
		size = new IntegerListInput("ImageSize", "Key Inputs", defSize);
		size.setValidCount(2);
		size.setValidRange(0, 2500);
		this.addInput(size, true);

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

	public IntegerVector getScreenPos() {
		return screenPosition.getValue();
	}

	public IntegerVector getImageSize() {
		return size.getValue();
	}

	public boolean getAlignRight() {
		return alignRight.getValue();
	}

	public boolean getAlignBottom() {
		return alignBottom.getValue();
	}
}
