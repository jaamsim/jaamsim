package com.jaamsim.observers;

import com.jaamsim.math.Color4d;
import com.sandwell.JavaSimulation.ColourInput;
import com.sandwell.JavaSimulation3D.DisplayModel;

/**
 * This is a data container class for the useful data stored in a DisplayModel.
 * It is a purely transitive class useful for finally deprecating DisplayModels
 * @author matt.chudleigh
 *
 */
public class DisplayModelState {

	public String shape;
	public Color4d fillColour;
	public Color4d outlineColour;
	public boolean filled;
	public boolean transparent;
	public boolean compressedTexture;
	public boolean bold;

	/**
	 * Create a display model state from a display model
	 * @param dm
	 */

	public DisplayModelState() {
		shape = "";
		fillColour = ColourInput.MED_GREY;
		outlineColour = ColourInput.BLACK;
		filled = true;
		transparent = false;
		bold = false;
	}

	public DisplayModelState(DisplayModel dm) {
		shape = dm.getShape();
		fillColour = dm.getFillColor();
		outlineColour = dm.getOutlineColor();
		filled = dm.isFilled();
		bold = dm.isBold();
		transparent = dm.isTransparent();
		compressedTexture = dm.useCompressedTexture();
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof DisplayModelState)) return false;

		DisplayModelState dms = (DisplayModelState)o;

		return dms.shape.equals(shape) &&
		       dms.fillColour.equals(fillColour) &&
		       dms.outlineColour.equals(outlineColour) &&
		       dms.filled == filled &&
		       dms.transparent == transparent;

	}

	@Override
	public int hashCode() {
		/**
		 * I have no idea if this is a good hash code or not
		 */
		return shape.hashCode() ^
		       fillColour.hashCode() * 7 ^
		       outlineColour.hashCode() * 57 ^
		       (filled ? 517 : 0) ^
		       (bold ? 905 : 0) ^
		       (transparent ? 1239 : 0) ^
		       (compressedTexture ? 2569 : 0);
	}

	public boolean isFlatModel() {
		// For now simply show any shape that does not end in .dae or .zip as a 2D object
		String ext = shape.substring(shape.length() - 4, shape.length()).toUpperCase();

		return !(ext.equals(".DAE") || ext.equals(".ZIP"));
	}

}
