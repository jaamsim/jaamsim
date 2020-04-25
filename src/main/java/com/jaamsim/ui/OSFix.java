/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2017-2020 JaamSim Software Inc.
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
package com.jaamsim.ui;

import java.awt.Point;

/**
 * Provides OS-dependent fixes for bugs in Swing.
 */
public class OSFix {

	private static final String OS_NAME = System.getProperty("os.name");
	private static final Point DEFAULT_ADJUST = new Point();
	private static final String WIN_10 = "Windows 10";
	private static final String MAC_OS_X = "Mac OS X";
	private static final Point WIN_10_LOCATION_ADJUST = new Point(-7, 0);
	private static final Point WIN_10_SIZE_ADJUST = new Point(15, 8);

	public static final boolean isMac() {
		return OS_NAME.equals(MAC_OS_X);
	}

	public static final Point getSizeAdustment() {
		Point ret;
		switch (OS_NAME) {
		case WIN_10:
			ret = WIN_10_SIZE_ADJUST;
			break;
		default:
			ret = DEFAULT_ADJUST;
			break;
		}
		return ret;
	}

	public static final Point getLocationAdustment() {
		Point ret;
		switch (OS_NAME) {
		case WIN_10:
			ret = WIN_10_LOCATION_ADJUST;
			break;
		default:
			ret = DEFAULT_ADJUST;
			break;
		}
		return ret;
	}

}
