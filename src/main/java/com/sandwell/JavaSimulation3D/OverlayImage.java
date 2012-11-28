package com.sandwell.JavaSimulation3D;

import java.util.ArrayList;

import com.jaamsim.ui.View;
import com.sandwell.JavaSimulation.BooleanInput;
import com.sandwell.JavaSimulation.EntityListInput;
import com.sandwell.JavaSimulation.Input;
import com.sandwell.JavaSimulation.IntegerListInput;
import com.sandwell.JavaSimulation.IntegerVector;
import com.sandwell.JavaSimulation.Keyword;
import com.sandwell.JavaSimulation.StringInput;

public class OverlayImage extends DisplayEntity {
	@Keyword(desc = "The position of the image, from the upper left corner of the window to the upper left corner " +
	                "of the image. Value is in pixels",
	         example = "Logo Position { 20 20}")
	private final IntegerListInput screenPosition;

	@Keyword(desc = "The size of the image. Value is in pixels",
	         example = "Logo Size { 200 100}")
	private final IntegerListInput size;

	@Keyword(desc = "The view objects this overlay image will be visible on",
	         example = "Logo VisibleViews { TitleView DefaultView }")
	private final EntityListInput<View> visibleViews;

	@Keyword(desc = "If this text label should be aligned from the right edge of the window (instead of the left)",
	         example = "Logo AlignRight { TRUE }")
	private final BooleanInput alignRight;

	@Keyword(desc = "If this text label should be aligned from the bottom edge of the window (instead of the top)",
	         example = "Logo AlignBottom { TRUE }")
	private final BooleanInput alignBottom;

	@Keyword(desc = "Indicates the loaded image has an alpha channel (transparency information) that should be used.",
	         example = "Logo Transparent { TRUE }")
	private final BooleanInput transparent;

	@Keyword(desc = "File to be displayed. If spaces are included, enclose the text in single quotes.",
	         example = "Logo FileName { '../Images/logo.png' }")
	private final StringInput filename;

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

		visibleViews = new EntityListInput<View>(View.class, "VisibleViews", "Key Inputs", new ArrayList<View>(0));
		this.addInput(visibleViews, true);

		alignRight = new BooleanInput("AlignRight", "Key Inputs", false);
		this.addInput(alignRight, true);

		alignBottom = new BooleanInput("AlignBottom", "Key Inputs", false);
		this.addInput(alignBottom, true);

		transparent = new BooleanInput("Transparent", "Key Inputs", false);
		this.addInput(transparent, true);

		filename = new StringInput( "File", "Key Inputs", null );
		this.addInput( filename, true);

		getInput("position").setHidden(true);
		getInput("alignment").setHidden(true);
		getInput("size").setHidden(true);
		getInput("orientation").setHidden(true);
		getInput("region").setHidden(true);
		getInput("relativeentity").setHidden(true);
		getInput("levelofdetail").setHidden(true);
		getInput("displaymodel").setHidden(true);
		getInput("active").setHidden(true);
		getInput("show").setHidden(true);
		getInput("movable").setHidden(true);
		getInput("tooltip").setHidden(true);
	}

	@Override
	public void updateForInput( Input<?> in ) {
		setGraphicsDataDirty();
	}

	public ArrayList<View> getVisibleViews() {

		if( visibleViews.getValue() == null )
			return new ArrayList<View>();

		return visibleViews.getValue();
	}

}
