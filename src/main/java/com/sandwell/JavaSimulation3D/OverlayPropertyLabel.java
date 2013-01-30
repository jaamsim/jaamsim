package com.sandwell.JavaSimulation3D;

public class OverlayPropertyLabel extends OverlayTextLabel {
	private PropertyReader propReader;

	{
		propReader = new PropertyReader();

		addInputGroup(propReader);
	}

	@Override
	public String getText(double time) {
		String val = propReader.getPropertyValueString(time);
		if (val.equals("")) {
			return super.getText(time);
		}

		return val;
	}

}
