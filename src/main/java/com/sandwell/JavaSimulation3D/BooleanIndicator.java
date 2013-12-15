package com.sandwell.JavaSimulation3D;

import com.jaamsim.input.Keyword;
import com.jaamsim.input.OutputInput;
import com.sandwell.JavaSimulation.ColourInput;

public class BooleanIndicator extends DisplayEntity {

	private final OutputInput<Boolean> boolProp;

	@Keyword(description = "The colour of the indicator when the property is true",
	         example = "BinLevel TrueColor { green }")
	private final ColourInput trueColor;

	@Keyword(description = "The colour of the indicator when the property is false",
	         example = "BinLevel TrueColor { green }")
	private final ColourInput falseColor;

	{
		boolProp = new OutputInput<Boolean>(Boolean.class, "OutputName", "Key Inputs", null);
		this.addInput(boolProp, true);

		trueColor = new ColourInput("TrueColour", "Key Inputs", ColourInput.GREEN);
		this.addInput(trueColor, true, "TrueColor");

		falseColor = new ColourInput("FalseColour", "Key Inputs", ColourInput.RED);
		this.addInput(falseColor, true, "FalseColor");
	}

	public BooleanIndicator() {
	}

	@Override
	public void updateGraphics( double time ) {
		if (boolProp.getValue() == null)
			return;
		Boolean b = boolProp.getOutputValue(time);
		if (b.booleanValue()) {
			setTagColour(DisplayModelCompat.TAG_CONTENTS, trueColor.getValue());
		}
		else {
			setTagColour(DisplayModelCompat.TAG_CONTENTS, falseColor.getValue());
		}
	}

}
