package com.sandwell.JavaSimulation3D;

public class OverlayPropertyLabel extends OverlayTextLabel {
	private PropertyReader propReader;

	{
		propReader = new PropertyReader();

		addInputGroup(propReader);
	}

	@Override
	public String getRenderText(double time) {
		String val = propReader.getPropertyValueString(time);
		if (val.equals("")) {
			return super.getRenderText(time);
		}

		return val;
	}

}
