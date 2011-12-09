/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2002-2011 Ausenco Engineering Canada Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 */
package com.sandwell.JavaSimulation3D;

import javax.swing.JLabel;

/**
 * Class LocalClock - Sandwell Marine Transportation Model (MTM)
 */
public class LocalClock extends Display2DEntity {
	private static final java.awt.Font aFont;
	protected double timeAdjustment; // The difference between the local time and the global time maintained by EventManager (usually GMT)

	static {
		aFont = new java.awt.Font("Verdana", java.awt.Font.BOLD, 12);
	}

	public LocalClock() {
		super();
		timeAdjustment = 0.0;

		displayModel = new JLabel( "------.--", JLabel.RIGHT );
		displayModel.setFont(aFont);
	}

	/**
	 * Sets the Time change from GMT
	 */
	public void setTimeChange( double time ) {
		timeAdjustment = time;
	}

	/**
	 * This method returns the present hour of the day for the port as a float.
	 */
	public double getDecHour() {
		return (Clock.getDecHour(getCurrentTime()) + timeAdjustment) % 24.0;
	}

	public double getDecHourForTime( double time ) {
		return (time + timeAdjustment) % 24.0;
	}

	/**
	 * No Comments Given.
	 */
	public void render(double time) {
		super.render(time);

		String output = Clock.formatDateString(time + timeAdjustment);

		((JLabel)displayModel).setText( output );
	}

	/**
	 * Returns a formatted string to reflect the current simulation time.
	 */
	public String getDateStringForTime(double time) {

		double localTime = time + timeAdjustment;
		return Clock.getDateStringForTime(localTime);
	}
}
