package com.sandwell.JavaSimulation3D;

import com.sandwell.JavaSimulation.ColourInput;
import com.sandwell.JavaSimulation.Keyword;

public class BooleanIndicator extends DisplayEntity {

	private PropertyReader propReader;

	@Keyword(desc = "The colour of the indicator when the property is true",
	         example = "BinLevel TrueColor { green }")
	private final ColourInput trueColor;

	@Keyword(desc = "The colour of the indicator when the property is false",
	         example = "BinLevel TrueColor { green }")
	private final ColourInput falseColor;

	{
		propReader = new PropertyReader();
		addInputGroup(propReader);

		trueColor = new ColourInput("TrueColour", "Key Inputs", ColourInput.GREEN);
		this.addInput(trueColor, true, "TrueColor");

		falseColor = new ColourInput("FalseColour", "Key Inputs", ColourInput.RED);
		this.addInput(falseColor, true, "FalseColor");
	}

	public BooleanIndicator() {
	}

	@Override
	public void updateGraphics( double time ) {

		String val = propReader.getPropertyValueString(time);
		if(val.equals( "true" ) ) {
			setTagColour(DisplayModelCompat.TAG_CONTENTS, trueColor.getValue());
		}
		else {
			setTagColour(DisplayModelCompat.TAG_CONTENTS, falseColor.getValue());
		}
	}

}
